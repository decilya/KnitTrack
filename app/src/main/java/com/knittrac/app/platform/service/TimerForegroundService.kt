package com.knittrac.app.platform.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.knittrac.app.MainActivity
import com.knittrac.app.R
import com.knittrac.app.core.di.IoDispatcher
import com.knittrac.app.domain.service.TimerManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Foreground-сервис для отображения уведомления с работающим таймером.
 *
 * ## Зачем нужен foreground-сервис
 *
 * Android убивает обычные фоновые процессы через несколько минут после
 * сворачивания приложения. Foreground-сервис (с постоянно видимым
 * уведомлением) защищён от этого: система знает, что пользователь видит
 * работу сервиса, и не убивает его без явного согласия.
 *
 * Это критично для таймера: пользователь может свернуть приложение
 * на 2 часа вязания, и таймер должен продолжать идти.
 *
 * ## Жизненный цикл
 *
 * - [onCreate] — создаёт канал уведомлений (Android 8+ требует канал
 *   до первого notify).
 * - [onStartCommand] — обрабатывает ACTION_START / ACTION_STOP и
 *   случай intent == null при перезапуске системы.
 * - [onDestroy] — отменяет serviceScope, чтобы не утекали корутины.
 *
 * ## START_STICKY и перезапуск после убийства
 *
 * Возвращаем START_STICKY — если система убьёт процесс (низкая память,
 * Doze mode), она попытается перезапустить сервис с intent == null.
 *
 * Важно (Android 8+): если сервис был запущен через
 * startForegroundService(), он обязан вызвать startForeground()
 * в течение 5 секунд после запуска, иначе система убьёт его с ANR.
 * Именно поэтому случай intent == null обязан вызвать
 * startForeground() — иначе будет краш на всех Android 8+.
 *
 * ## Что сервис НЕ делает
 *
 * - Не хранит состояние таймера — это ответственность [TimerManager].
 * - Не пересчитывает elapsedTime — подписывается на готовый StateFlow.
 * - Не пишет в БД — этим занимается SaveSessionService по явному действию.
 */
@AndroidEntryPoint
class TimerForegroundService : Service() {

    /**
     * Менеджер таймера — источник [TimerManager.elapsedTime].
     * Инжектится Hilt через @AndroidEntryPoint (конструкторная инъекция
     * для Android Service не поддерживается).
     */
    @Inject lateinit var timerManager: TimerManager

    /**
     * IO-диспетчер для фоновых операций сервиса.
     *
     * Используется в serviceScope, потому что коллекция
     * [TimerManager.elapsedTime] и обновление уведомления — потенциально
     * блокирующие операции (binder IPC, работа с NotificationManager).
     * На Main-диспетчере это могло бы дёргать UI.
     */
    @Inject @IoDispatcher lateinit var ioDispatcher: CoroutineDispatcher

    /**
     * Scope сервиса — живёт от [onCreate] до [onDestroy].
     *
     * SupervisorJob изолирует падения дочерних корутин: если один
     * collect упадёт, остальные не отменятся.
     *
     * Отменяется в [onDestroy] через serviceScope.cancel().
     */
    private val serviceScope = CoroutineScope(SupervisorJob() + ioDispatcher)

    /**
     * Менеджер уведомлений Android.
     * by lazy — создаётся при первом обращении, не в конструкторе,
     * потому что getSystemService() доступен только после onCreate.
     */
    private val notificationManager by lazy {
        getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    companion object {
        /** Действие: запустить сервис и начать показывать таймер в уведомлении. */
        const val ACTION_START = "ACTION_START"

        /** Действие: остановить сервис и убрать уведомление. */
        const val ACTION_STOP = "ACTION_STOP"

        /**
         * Идентификатор уведомления. Одно и то же значение — чтобы
         * notificationManager.notify обновлял существующее уведомление,
         * а не создавал новое каждый тик.
         */
        const val NOTIFICATION_ID = 1001

        /** Идентификатор канала уведомлений (Android 8+). */
        const val CHANNEL_ID = "knittrac_timer_channel"
    }

    /**
     * Вызывается при создании сервиса.
     *
     * Создаёт канал уведомлений, если его ещё нет. Без канала первый
     * вызов notify на Android 8+ приведёт к тому, что уведомление
     * не покажется (система молча проигнорирует).
     */
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    /**
     * Вызывается при каждом startService() / startForegroundService().
     *
     * Обрабатывает три случая:
     * 1. ACTION_START — пользователь запустил таймер.
     * 2. ACTION_STOP — пользователь остановил таймер.
     * 3. intent == null — система перезапустила сервис после убийства
     *    процесса (START_STICKY). Обязаны вызвать startForeground()
     *    в течение 5 секунд, иначе ANR.
     *
     * @return START_STICKY — просим систему перезапустить сервис, если
     * он будет убит.
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> handleStart()
            ACTION_STOP -> handleStop()
            null -> handleSystemRestart()
        }
        return START_STICKY
    }

    /**
     * Обработка штатного запуска таймера.
     *
     * Порядок действий важен:
     * 1. startForeground() — обязательно первым, до корутин, иначе ANR.
     * 2. timerManager.start() — запускаем тикер.
     * 3. launch { collect } — после start(), чтобы первый тик 0L
     *    не был пропущен. Если бы launch шёл первым, корутина
     *    запланировалась бы, но не выполнилась до конца onStartCommand,
     *    и start() сдвинул бы _elapsedTime раньше, чем подписка.
     */
    private fun handleStart() {
        val notification = createNotification(
            title = getString(R.string.timer_started),
            text = formatTime(0L)
        )
        startForeground(NOTIFICATION_ID, notification)

        timerManager.start()

        serviceScope.launch {
            timerManager.elapsedTime.collect { elapsed ->
                val updatedNotification = createNotification(
                    title = getString(R.string.timer_running),
                    text = formatTime(elapsed)
                )
                notificationManager.notify(NOTIFICATION_ID, updatedNotification)
            }
        }
    }

    /**
     * Обработка остановки таймера.
     *
     * STOP_FOREGROUND_REMOVE — убирает уведомление при остановке.
     * stopSelf() — сервис завершится после onStartCommand, вызовется
     * [onDestroy], где serviceScope будет отменён.
     */
    private fun handleStop() {
        timerManager.pause()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    /**
     * Обработка перезапуска системы после убийства процесса (START_STICKY).
     *
     * Сценарий: пользователь вязал, таймер работал, система убила процесс
     * из-за низкой памяти. Android решает перезапустить сервис — но
     * intent == null, потому что исходный startService() вызов уже
     * не восстановить.
     *
     * Критично: мы обязаны вызвать startForeground() в течение
     * 5 секунд после перезапуска, иначе система убьёт сервис с
     * ForegroundServiceDidNotStartInTimeException.
     *
     * Дальнейшая логика:
     * - Состояние таймера восстановить нельзя (sessionStartElapsed
     *   было в памяти убитого процесса).
     * - Показываем базовое уведомление и сразу останавливаемся.
     * - При следующем открытии приложения пользователь запустит таймер
     *   заново (или мы это сделаем в AppStartupCoordinator — план на будущее).
     */
    private fun handleSystemRestart() {
        val notification = createNotification(
            title = getString(R.string.timer_started),
            text = formatTime(0L)
        )
        startForeground(NOTIFICATION_ID, notification)
        stopSelf()
    }

    /**
     * Вызывается при уничтожении сервиса.
     *
     * Отменяем serviceScope, чтобы:
     * - Прекратилась коллекция [TimerManager.elapsedTime] (иначе утечка).
     * - Отменились все запущенные корутины.
     * - Освободились связанные ресурсы.
     */
    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    /**
     * Сервис не поддерживает binding — используется только через
     * startService() / startForegroundService().
     */
    override fun onBind(intent: Intent?): IBinder? = null

    /**
     * Создаёт канал уведомлений для Android 8+ (API 26).
     *
     * IMPORTANCE_LOW — уведомление показывается в шторке, но не звучит
     * и не вибрирует при обновлении. Важно, потому что уведомление
     * обновляется каждую секунду — при IMPORTANCE_DEFAULT это
     * превратилось бы в непрерывный поток вибраций.
     */
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.timer_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.timer_channel_description)
        }
        notificationManager.createNotificationChannel(channel)
    }

    /**
     * Создаёт объект уведомления для отображения в шторке.
     *
     * @param title Заголовок уведомления (например, «Таймер запущен»).
     * @param text Текст (например, отформатированное время «00:15:30»).
     * @return Настроенный [Notification] для startForeground или notify.
     *
     * Особенности:
     * - PendingIntent.getActivity — открывает [MainActivity] при тапе.
     * - FLAG_IMMUTABLE — обязателен для Android 12+, иначе SecurityException.
     * - setOngoing(true) — уведомление нельзя смахнуть жестом.
     * - Иконка ic_launcher_foreground — стандартная, требуется для
     *   foreground-сервиса (маленькая монохромная).
     */
    private fun createNotification(title: String, text: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    /**
     * Форматирует секунды в строку «HH:MM:SS» для уведомления.
     *
     * @param seconds Количество секунд (из [TimerManager.elapsedTime]).
     * @return Строка вида «01:23:45».
     *
     * Использует [java.util.Locale.US] явно, чтобы избежать проблем с
     * локалями, где цифры пишутся не арабскими символами (например,
     * принудительная арабица), — в уведомлении нужны обычные цифры.
     */
    private fun formatTime(seconds: Long): String {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return String.format(java.util.Locale.US, "%02d:%02d:%02d", h, m, s)
    }
}
