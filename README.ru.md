# KnitTrac

> Учёт времени для тех, кто вяжет, плетёт и занимается рукоделием.

KnitTrac — Android-приложение для учёта времени, потраченного на вязание спицами, крючком, бисероплетение и макраме. Оно сделано для тех, кто работает над одной вещью неделями или месяцами и хочет понимать, куда уходит время — без таблиц и блокнотов.

Таймер работает в фоне, сессии сохраняются локально, а статистика по дням показывает, как продвигается проект. Когда появится синхронизация, проекты будут доступны на всех устройствах.

[English version](README.md)

## Возможности

**Таймер, который не сбивают переводы часов.**
Таймер основан на монотонном времени (elapsedRealtime), а не на настенных часах. Корректировка по NTP, ручная смена времени и переход на летнее время не искажают прошедшее время. Таймер работает как foreground-сервис и продолжает идти, когда приложение свёрнуто.

**Проекты с категориями.**
У каждого проекта есть название, категория (спицы, крючок, бисер, макраме) и глобальный счётчик рядов. Счётчик привязан к проекту, а не к сессии: можно закрыть приложение и на следующий день продолжить с того же ряда.

**Сессии и статистика по дням.**
Каждый запуск таймера создаёт сессию с временем начала, длительностью и заметками. Сессии агрегируются в статистику по дням с графиком — видно, сколько времени ушло на этой неделе.

**Local-first.**
Все данные лежат в Room (SQLite) на устройстве. Приложение полностью работает офлайн. Экспорт и импорт используют потоковый JSON (JsonWriter / JsonReader), поэтому даже большие бэкапы не приводят к OOM.

**Локализация.**
Русский и английский из коробки. Добавить новый язык — значит отредактировать arrays.xml и один файл строк, без правок Kotlin.

**Синхронизация (в разработке).**
Двусторонняя синхронизация между устройствами одного пользователя через Firebase Firestore запланирована на v1.0. Схема уже подготовлена: во всех таблицах есть client_id, server_updated_at, sync_version и поля для soft-delete.

## Стек технологий

| Слой | Выбор | Почему |
|---|---|---|
| Язык | Kotlin 1.9.22 | Современный Android; корутины встроены. |
| UI | Jetpack Compose (BOM 2024.02.00) | Декларативный UI; меньше boilerplate, чем XML. |
| Локальная БД | Room 2.6.1 | Типобезопасный SQL поверх SQLite. |
| Настройки | DataStore Preferences 1.0.0 | Замена SharedPreferences; на Flow, без ANR. |
| DI | Hilt 2.50 | Единый контейнер; проверка на этапе компиляции через KSP. |
| JSON | Gson 2.10.1 | Зрелая библиотека; DTO версионируются через SerializedName. |
| Логи | Timber 5.0.1 | Tree-based; подмена дерева по типу сборки. |
| Графики | ycharts 2.1.0 | Compose-native графики для статистики. |
| Линтер | Detekt 1.23.7 | Проверки на уровне AST; regex-подхода оказалось недостаточно. |
| Сборка | AGP 8.6.0, Gradle 8.7, JDK 17 | Зафиксировано осознанно. Gradle 8.7 падает на JDK 21+. |

Версии объявлены инлайн в build.gradle.kts. Файла gradle/libs.versions.toml нет.

## Сборка

Требования:

- JDK 17 в Android Studio, Settings, Build, Execution, Deployment, Build Tools, Gradle, Gradle JDK. JDK 21 или 25 сломают Gradle 8.7.
- Android SDK 35.
- Сборка из терминала Android Studio (PowerShell), не из WSL.

Команды:

    cd C:\Users\decilya\projects\KnitTrac
    .\gradlew.bat clean test
    .\gradlew.bat assembleDebug
    .\gradlew.bat detekt

adb не в PATH. Эмулятор запускать через Android Studio, Device Manager.

## Архитектура

Слои и направление зависимостей:

    Presentation (Compose, MVI)
        вниз
    ViewModel (Action / State / Effect)
        вниз
    Domain (UseCase, Result, Value Objects)
        вниз
    Data (Repositories, Room, DataStore, DTO)
        вниз
    Platform (Timer, FGS, Notification)
        вниз
    Core (Result, AppError, Dispatchers, TimeProvider)

Соглашения:

- DIP. Интерфейсы репозиториев в domain/repository, реализации в data/repository. Домен не импортирует Android.
- Value Objects. Duration, ProjectName, ThemeMode — неизменяемые, с приватным конструктором и безопасными фабриками (create, createOrNull, createOrDefault).
- MVI. Однонаправленный поток. Эффекты через _effect.emit, потребляются как Channel или SharedFlow в экране.
- Обработка ошибок. Каждый try ловит CancellationException первым и пробрасывает её. Отмена корутины — сигнал, не ошибка.

Подробный стандарт (Clean Architecture, DDD, SOLID, Symfony-подобная структура, Clean Code) — в файле docs/architecture/coding-standards.md.

## Тесты

134 unit-теста (JUnit 4 + MockK).

    .\gradlew.bat clean test

XML-отчёт: app/build/test-results/testDebugUnitTest/.

Инструментальные тесты (миграции Room, DAO) — в app/src/androidTest, требуют запущенного эмулятора:

    .\gradlew.bat connectedDebugAndroidTest

## Линтер

Detekt 1.23.7.

    .\gradlew.bat detekt

Конфиг: config/detekt/detekt.yml. Существующие нарушения заморожены в config/detekt/baseline.xml. Новые нарушения валят сборку.

## Pre-commit hook

Файл .githooks/pre-commit запускает gradlew.bat test detekt перед каждым коммитом. Активация один раз:

    git config core.hooksPath .githooks

Hook WSL-safe: вызывает Windows gradlew.bat через cmd.exe, а не bash ./gradlew. Причина — Gradle пишет в .gradle/ на /mnt/c через drvfs, Windows-антивирус блокирует файлы → Input/output error.

## Документация

- ROADMAP.md — план v1.0: scope, sync, вехи.
- HANDOVER.md — контекст проекта для передачи между сессиями.
- docs/architecture/coding-standards.md — стандарт кода.

## Участие

Проект пока соло. Issues и pull requests приветствуются, но сначала заводите issue, чтобы согласовать объём до написания кода.

## Лицензия

TBD.
