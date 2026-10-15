# KnitTrac Coding Standards

Версия: 4.0 (окончательная редакция цикла)
Статус: ОБЯЗАТЕЛЕН для всего кода проекта, включая код, сгенерированный ИИ.
Трекер: только GitHub Issues (Jira нет). Issue создаётся через gh CLI
до коммита, который на него ссылается.

---

## 1. Общие принципы

1. Код пишется для человека. ИИ-генерация обязана производить простой,
   явный, линейный код. Запрещён «умный» код ради компактности.
2. Архитектура важнее локального удобства.
3. Каждая зависимость должна иметь понятную причину существования.
4. Запрещено молчаливое проглатывание ошибок.
5. Запрещён TODO без номера GitHub issue.
6. Комментарии объясняют ПОЧЕМУ, а не ЧТО. ЧТО выражается именами.
7. Лимиты: метод <= 30 строк, класс <= 300 строк, файл <= 400 строк,
   Composable <= 60 строк, параметров <= 5, вложенность <= 3.
8. Идентификаторы — на английском. KDoc и комментарии — на русском.
9. Новый компонент поставляется вместе с тестом. Без теста — не коммитим.
10. Границы слоёв: Presentation -> Service -> Repository -> DAO.
    Domain не знает об Android, Room, Gson, DataStore.

---

## 2. Слои и поток зависимостей

    Presentation (Screen, ViewModel, Contract/MVI)
      | вызывает
      v
    Service (domain/service/*Service) — бизнес-логика, ЗАЧЕМ
      | запрашивает данные (цель: через Provider; сейчас: напрямую, техдолг)
      v
    Provider (domain/provider/*Provider) — порт данных между Service и Repository
      | делегирует хранение
      v
    Repository (domain/repository — интерфейс, data/repository — реализация) — КАК
      | использует
      v
    DAO / DataStore / Gson / файлы (data/local, data/dto, data/mapper)

    Platform (platform/*) — Android-адаптеры: ForegroundService,
    SystemTimeProvider, NotificationPermissionChecker, NoOp-платежи/реклама.
    Core (core/*) — Result, AppError, SyncStatus, TimeProvider (интерфейс),
    квалификаторы DI, локализация-менеджеры.

| Слой | Знает | Не знает |
|---|---|---|
| Presentation | State/Action/Effect, UiModel, сервисы | DAO, SQL, Entity, DTO, Android-хранилища |
| Service | бизнес-правила, контракты Repository/Provider, Domain-модели | как хранятся данные, SQL, JSON, Context |
| Provider | откуда взять данные (кэш/БД/часы), агрегация источников | зачем они бизнесу |
| Repository | как сохранить/загрузить из одного хранилища | бизнес-правила, UI |
| Data | Room, DataStore, Gson, файлы, мапперы | бизнес-правила |
| Platform | Android API, жизненный цикл | бизнес-правила в обход Domain |
| Core | общие контракты, ошибки, время | конкретные фичи |

---

## 3. Service-слой (бизнес-логика)

1. Один сервис = одна бизнес-операция или сценарий использования.
2. Сервис не содержит SQL, Room, DataStore, Gson, File, Cursor, Uri,
   Context, Activity, Fragment и любых Android API.
3. Сервис не принимает и не возвращает Entity, Dto, JSON, Cursor, Uri.
4. Параметры — типобезопасный data class (*Params), результат — Result<T>.
5. Сервис детерминирован. Время — только через TimeProvider.
6. Сервис не мутирует глобальное состояние.
7. Логирование бизнес-ошибок — на границе слоя (Data/Platform), не в сервисе.
8. Валидация входа — через Value Objects (createOrNull -> Result.Error).

Пример:

    class SaveSessionService @Inject constructor(
        private val sessionRepository: SessionRepository,
    ) {
        suspend operator fun invoke(params: SaveSessionParams): Result<Long> {
            val seconds = (params.endTimestamp - params.startTimestamp) / 1000L
            val duration = Duration.createOrNull(seconds)
                ?: return Result.Error(AppError.ValidationError("Пустая сессия"))
            val session = Session(params, duration)
            return sessionRepository.addSessionAtomically(session)
        }
    }

---

## 4. Provider-слой

1. Provider — порт данных МЕЖДУ Service и Repository: решает ОТКУДА взять
   (кэш, БД, сеть, системные часы), может агрегировать несколько источников.
2. Интерфейс — в domain/provider/, реализация — в data/provider/ или
   platform/provider/.
3. Системные источники значений (время, ID, локаль) — тоже провайдеры:
   TimeProvider и подобные.
4. Имя строго содержит суффикс Provider (реализация — ProviderImpl или
   System*/Fake* по контексту).
5. Provider не содержит бизнес-правил и Room-аннотаций, не пишет SQL.
6. Терминологическая справка (не смешивать):
   - Symfony/Laravel ServiceProvider = инструкция регистрации в DI-контейнере.
     Аналог в KnitTrac — Hilt @Module, а НЕ классы-*Provider.
   - В части литературы (Angular, Spring) provider = низкоуровневый источник
     НИЖЕ Repository. В KnitTrac принято иное: Provider = порт между Service
     и Repository либо системный источник.
7. Текущее состояние: сервисы вызывают Repository напрямую. Это технический
   долг (раздел 23): Provider-слой вводится там, где нужна агрегация или кэш;
   прямые вызовы до миграции допустимы и не считаются нарушением границ
   Presentation/Data.

---

## 5. Repository-слой

1. Все запросы к БД — только через репозитории.
2. Интерфейс — в domain/repository/, реализация — в data/repository/.
3. ViewModel, Screen, Service не используют DAO напрямую.
4. Репозиторий возвращает доменные модели, Result<T>, Flow<T>.
   Entity, Dto, Cursor, ByteArray наружу не выходят.
5. Маппинг Entity <-> Domain и Dto <-> Domain — только в data/mapper/.
6. Инфраструктурные ошибки преобразуются в AppError (раздел 12).
7. UPDATE проверяет rows affected: 0 строк -> NotFoundError, не Success.
8. DELETE идемпотентен: 0 строк -> Success (защита от гонки повторного удаления).
9. Атомарность — на уровне Data: @Transaction, withTransaction.
   В транзакционных методах DAO rows affected проверяется через check().
10. Каждый catch-блок: первым catch (e: CancellationException) { throw e },
    затем специфичные, затем Exception с Timber.e(cause, "контекст").
11. updatedAt и syncStatus проставляет репозиторий; входные значения
    этих полей игнорируются (зафиксировано в KDoc методов).
12. Репозиторий не принимает бизнес-решений, кроме гарантий целостности.

---

## 6. Data-слой: DAO, Entity, DTO, Mappers

### DAO
1. DAO — низкоуровневый технический интерфейс Room, без бизнес-логики.
2. UPDATE/DELETE возвращают Int (rows affected).
3. Имена колонок — snake_case через @ColumnInfo; Kotlin-поля — camelCase.
4. Строковые литералы статусов в SQL параметризованы (:pendingStatus)
   с default = SyncStatus.PENDING.name — переименование enum ловит компилятор.
5. Сложные запросы объяснены именем метода или KDoc.

### Entity
1. Entity — таблица, а не бизнес-объект: без поведения и бизнес-валидации.
2. Хранит примитивы (String, Long). Value Objects в Entity запрещены.
3. Entity не покидает data/.

### DTO
1. DTO — в data/dto/, без бизнес-логики.
2. @SerializedName фиксирует контракт формата; имена стабильны между версиями.
3. Корневой DTO бэкапа содержит version: String и CURRENT_VERSION.
4. Списки по умолчанию пустые — старый бэкап без поля не падает с NPE.
5. DTO защищены от R8: @Keep плюс правило в proguard-rules.pro.

### Mappers
1. Мапперы — единственная точка конверсии между слоями (data/mapper/).
2. Чтение — с безопасным fallback: createOrDefault, createOrNull ?: ZERO,
   fromNameOrDefault. Повреждённые данные не роняют приложение.
3. Запись — явное разворачивание VO/enum в примитивы (.value, .seconds, .name).
4. Маппинг чистый и детерминированный, без побочных эффектов.
5. EntityMappers и DtoMappers симметричны по поведению на битых данных.

### Экспорт / импорт
1. Потоковая сериализация JsonWriter/JsonReader — без загрузки JSON в память.
2. Импорт — чанковая вставка (BATCH_SIZE) внутри общей withTransaction:
   падение на чанке откатывает всё, половинчатых состояний нет.
3. Чтение при экспорте — в withTransaction: один снапшот БД.
4. Потоки НЕ закрываются реализацией — ответственность вызывающей стороны.
5. Семантика импорта — merge (REPLACE), озвучена пользователю в UI.

---

## 7. Presentation-слой

### MVI
1. State — неизменяемый data class; обновление только через copy/update.
2. Действия UI входят через единую точку: onAction(action)/onIntent(intent).
3. Одноразовые события — Effect через Channel/SharedFlow, не в State.
4. Загрузка, ошибка и пустое состояние различимы явно:
   isLoading, errorRes (@StringRes Int?), isEmpty-проверки.
5. Ошибки в State хранятся как id строкового ресурса, не как текст.
6. Domain-сущности не попадают в State: только UiModel-проекции.
7. Повторные операции защищены флагами (isExporting, isSaving);
   guard ставится синхронно до launch, снимается в finally.
8. retry() — публичный метод ViewModel для сценария «Повторить».

### Compose
1. Composable декларативны; тяжёлая логика вне композиции.
2. Потоки SAF создаются в Screen (launcher), передаются во ViewModel через
   Action; закрываются после Effect и в DisposableEffect(onDispose)
   с runCatching — IOException не роняет композицию.
3. ViewModel не знает Uri/ContentResolver/Context.
4. Переиспользуемые компоненты: EmptyStateView, SettingsSection,
   hapticClickable. Размер задаётся снаружи через modifier.
5. Тактильный отклик — на осмысленных действиях (LongPress для кнопок и
   карточек, TextHandleMove для RadioButton); стандартные компоненты
   (Switch, back) не дублируют haptic.
6. Весь пользовательский текст — через stringResource.
7. Анимации минимальны и оправданы (animateItemPlacement для списков).

### Старт приложения
1. SplashScreen API: installSplashScreen() до super.onCreate;
   setKeepOnScreenCondition { !localeManager.isInitialized.value }.
2. runBlocking в Application.onCreate запрещён: инициализация локали —
   в applicationScope (SupervisorJob + Dispatchers.Default).
3. LocaleManager.isInitialized сбрасывается в finally: при ошибке лучше UI
   на системном языке, чем вечный splash.

---

## 8. Platform-слой

1. ForegroundService обрабатывает intent == null (перезапуск START_STICKY):
   startForeground() в течение 5 секунд, затем stopSelf(). Иначе ANR на Android 8+.
2. Порядок в handleStart: startForeground() -> timerManager.start() ->
   launch { collect } (иначе первый тик пропускается).
3. Канал уведомлений для ежесекундных обновлений — IMPORTANCE_LOW;
   уведомление обновляется через notify с тем же id; цель — setUsesChronometer
   плюс setOnlyAlertOnce вместо ежесекундного notify.
4. serviceScope отменяется в onDestroy; подписки на Flow не утекают.
5. Сервис не хранит бизнес-состояние: только отображает StateFlow таймера.
6. SystemTimeProvider — единственная точка вызова System.currentTimeMillis
   и SystemClock.elapsedRealtime; лежит в platform/timer.
7. Разрешения: отказ пользователя деградирует мягко (таймер работает,
   уведомление не показывается), запрос — через Effect RequestNotificationPermission.

---

## 9. Dependency Injection

1. DI-контейнер ровно один — Hilt (SingletonComponent и scoped-компоненты).
   Это фабрика объектов, аналог контейнера Symfony/Laravel.
2. @Module-классы — инструкция регистрации зависимостей в контейнере
   (аналог ServiceProvider::register в Laravel). Побочных эффектов не имеют.
3. Все зависимости внедряются через конструктор (@Inject constructor).
4. Интерфейсы связываются через @Binds (abstract class); сторонние и
   инфраструктурные объекты — через @Provides (object).
5. Scope (@Singleton) объявляется на @Binds/@Provides, а не на impl-классе:
   реализация не знает, что она синглтон.
6. Диспетчеры — через квалификаторы @IoDispatcher/@DefaultDispatcher;
   хардкод Dispatchers.IO в бизнес-коде запрещён.
7. @ApplicationContext — только там, где контекст реально нужен.
   Activity/Fragment/View в синглтонах запрещены.
8. DataStore единственный: delegate preferencesDataStore объявлен только в
   core/di/DataStoreModule; везде инжектится через конструктор. Дубликат
   delegate = рантайм-краш "multiple DataStores active for the same file".
9. Ручные ServiceLocator и самодельные контейнеры запрещены.
10. Мёртвые биндинги запрещены (пример из истории: provideContext,
    дублировавший встроенный @ApplicationContext).
11. Модули разложены по слоям: core/di, data/di, platform/di.

---

## 10. Именование и суффиксы

| Суффикс | Слой | Пример |
|---|---|---|
| Service | Domain, бизнес-логика | AddProjectService, SaveSessionService |
| Provider | Domain, порт данных / системный источник | TimeProvider |
| ProviderImpl, System* | Data/Platform, реализация провайдера | SystemTimeProvider |
| Repository | Domain, контракт хранилища | ProjectRepository, SettingsRepository |
| RepositoryImpl | Data, реализация | ProjectRepositoryImpl |
| Dao | Data, Room | ProjectDao, SessionDao |
| Entity | Data, таблица | ProjectEntity, SessionEntity |
| Dto | Data, формат бэкапа | ProjectDto, ExportDataDto |
| Mappers | Data, конверсия | EntityMappers, DtoMappers |
| ViewModel | Presentation | ProjectsListViewModel |
| Screen | Presentation, Compose | ProjectsListScreen |
| Contract | Presentation, MVI | TimerContract, SettingsContract |
| Module | DI | RepositoryModule, TimerModule |
| Test | Тесты | SaveSessionServiceTest |

1. Классы — PascalCase; методы и свойства — camelCase; константы — UPPER_SNAKE.
2. Имена тестов — camelCase БЕЗ backtick и пробелов (раздел 17).
3. Enum-ы имеют безопасную фабрику fromString/fromNameOrDefault с fallback.
4. Магические строки запрещены: тема — ThemeMode, статус — SyncStatus,
   категории — Category.
5. Новые классы с именами Manager, Helper, Utils, Handler запрещены
   (исключения-старички — в разделе 23).
6. UI-специфика (R.string.*) живёт в UI-слое: extension labelRes() и подобные,
   а не поля доменных enum.

---

## 11. Value Objects

Полный паттерн — в docs/architecture/vo-pattern.md. Кратко:

1. VO — @JvmInline value class с приватным конструктором и тремя фабриками:
   create (throws), createOrNull (null), createOrDefault (fallback).
2. VO живут только в Domain. Entity, DTO, DAO — на примитивах
   (Persistence Ignorance). Room TypeConverters для VO не используются.
3. Конверсия VO <-> примитив — только в мапперах, на чтении с fallback.
4. Инварианты VO дублируются CHECK-констрейнтами в миграциях БД.
   Перед наложением CHECK старые данные санитизируются.
5. Дефолты VO не содержат пользовательских строк, кроме согласованных с
   миграциями (ProjectName.DEFAULT == строке санитизации MIGRATION_2_3).
6. Новый VO = файл + тесты границ + интеграция в модель + мапперы +
   миграция с CHECK, если меняется схема.

---

## 12. Ошибки и надёжность

1. Таксономия AppError (sealed): ValidationError, NotFoundError, IoError,
   DatabaseError, NetworkError, UnknownError. У всех — cause: Throwable?.
2. Соответствие источников ошибок:
   - невалидный вход -> ValidationError;
   - 0 rows affected / null из get -> NotFoundError;
   - файлы, SAF, DataStore -> IoError;
   - SQLite/Room -> DatabaseError;
   - сеть (зарезервировано) -> NetworkError;
   - остальное -> UnknownError.
3. Result<T> — единственная обёртка результата в Domain/Data.
4. Пользователь видит локализованный текст через @StringRes (errorRes),
   а не message ошибки и не стектрейс.
5. Разработчик видит причину: Timber.e(cause, "контекст: id=%d") в каждом catch.
6. AppErrorMapper используется для централизованного маппинга исключений;
   мёртвый код запрещён.
7. Release-наблюдаемость: FileLoggingTree (WARN+ в filesDir/logs, ротация
   1 МБ, 3 файла, I/O-ошибки проглатываются). Debug — Timber.DebugTree.
8. Ошибка сохранения данных не должна выглядеть как пустое состояние.
9. Повторные попытки идемпотентны там, где это возможно.

---

## 13. Корутины и время

1. TimeProvider разделяет два времени:
   - currentTimeMillis() — wall clock, для меток в БД (startTimestamp, updatedAt);
   - elapsedRealtime() — монотонное, для длительностей и таймера.
2. Прямые System.currentTimeMillis()/SystemClock в Domain, Service,
   Presentation и TimerManagerImpl запрещены.
3. endTimestamp сессии = startTimestamp + elapsedTime * 1000, а не wall clock:
   защита от NTP-сдвига и ручного перевода часов.
4. GlobalScope запрещён. runBlocking в Application/Activity/Fragment/ViewModel запрещён.
5. applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default).
   viewModelScope — только во ViewModel. Нужен Main — явно launch(Dispatchers.Main).
6. StateFlow — для состояния; Channel/SharedFlow — для одноразовых Effect.
   combine + update сохраняет флаги операций при изменении любого потока.
7. Долгие задачи поддерживают отмену; CancellationException — не ошибка.
8. Диспетчеры инжектятся квалификаторами; delay вместо busy-wait и Handler-циклов.

---

## 14. Room и миграции

1. exportSchema = true; app/schemas/ коммитится в репозиторий.
2. Любое изменение схемы — версионированная миграция плюс инструментальный тест
   (MigrationTestHelper) в androidTest.
3. SQLite не поддерживает ALTER TABLE ADD CONSTRAINT: таблицы пересоздаются
   через CREATE + INSERT SELECT + DROP + RENAME. CHECK переносится в новую
   CREATE TABLE вручную при КАЖДОЙ миграции — Room их не видит.
4. Структура новой таблицы обязана совпадать с генерируемой Room из @Entity,
   включая индексы.
5. Перед наложением CHECK — санитизация старых данных (пустые имена, отрицательные
   длительности). Иначе миграция падает на constraint.
6. FK ON DELETE CASCADE для дочерних сущностей; каскад проверяется тестом.
7. fallbackToDestructiveMigration запрещён без явной причины и тестов.
8. Коммит миграции включает: AppDatabase (version + MIGRATION), schema json,
   инструментальный тест, KDoc с обоснованием.

---

## 15. Внешние данные и формат бэкапа

1. Формат бэкапа отделён от Domain: изменение доменных моделей не ломает старые файлы.
2. version: String в корневом DTO; неизвестная версия логируется Timber.w
   и читается как текущий формат (forward-compatible).
3. Неизвестные enum-значения -> fallback plus Timber.w, не краш импорта.
4. Отсутствующие поля -> дефолты DTO.
5. Изменение формата = план миграции бэкапов plus тесты round-trip.

---

## 16. Локализация и ресурсы

1. values/ — английский (universal fallback). values-ru/ — русский.
   Русского текста в values/ быть не должно.
2. Весь пользовательский текст — в strings.xml; хардкод строк в коде запрещён.
3. Списки языков читаются из arrays.xml; рассогласование размеров массивов
   codes/names обрабатывается: Timber.e plus emptyList, без IndexOutOfBounds.
4. Доменные enum не содержат displayName: локализация — CategoryLocalizer
   (when по всем константам, полноту гарантирует компилятор).
5. Смена языка: сохранение в DataStore plus applyOverides Configuration plus
   Effect RecreateActivity.
6. Fallback-язык — константа FALLBACK_LANGUAGE, покрыта тестом.

---

## 17. Тестирование

### Обязательное покрытие
Сервисы, репозитории (включая rows=0 и rows=1), мапперы (round-trip plus битые данные),
VO (границы, trim, равенство), enum-контракты (fromString, стабильность name),
ViewModel (State, Effect, errorRes, retry, guard), локализация и менеджеры,
миграции БД (instrumented), файловое логирование (ротация, фильтр, устойчивость к I/O).

### Имена тестов
1. Формат: метод_условие_ожидаемыйРезультат, camelCase, английский.
2. Backtick и пробелы в именах запрещены. Имя — валидный идентификатор Kotlin.
3. KDoc на русском к классу и к каждому тесту: что проверяет и почему важно.

### Детерминированность и изоляция
1. Время — через FakeTimeProvider (wall и monotonic двигаются независимо).
2. Диспетчеры — StandardTestDispatcher/UnconfinedTestDispatcher plus setMain/resetMain.
3. Бесконечные циклы таймера: advanceTimeBy/runCurrent, НЕ advanceUntilIdle
   (зависнет); reset() в finally или tearDown.
4. DataStore в тестах — FakePreferencesDataStore (in-memory): edit — extension,
   mockk её не мокает.
5. Android API (Context, Resources) — mockk; Robolectric не требуется.
6. Глобальное состояние (Locale.setDefault) сохраняется и восстанавливается.
7. Тест проверяет поведение, а не реализацию; protected-методы не дёргаются
   reflection-хаками: при необходимости @VisibleForTesting internal.
8. Числа тестов в коммит-сообщении сверяются с XML-отчётами Gradle.

---

## 18. Комментарии, KDoc, TODO

1. KDoc на публичных контрактах: назначение, ограничения, контракт ошибок,
   побочные эффекты, неочевидные требования. Тело метода не дублируется.
2. Комментарии — только WHY. Пример допустимого:
   WHY: elapsedRealtime вместо currentTimeMillis — системные часы могут
   измениться при NTP-синхронизации и сломать расчёт длительности.
3. Запрещены: комментарии WHAT, markdown-разметка (**) внутри KDoc,
   закомментированный код, устаревшие комментарии.
4. TODO только с номером issue:
       // TODO(#123): перенести маппинг категорий в отдельный ресурс
   Issue создаётся до коммита:
       gh issue create --title "Кратко" --body "Подробности и план"
5. FIXME, HACK, XXX запрещены.

---

## 19. Запрещённые паттерны

1. Бизнес-логика в ViewModel, Screen, Receiver, Worker.
2. Доступ к Dao из Presentation или Service.
3. System.currentTimeMillis/SystemClock в Domain, Service, Presentation, TimerManagerImpl.
4. GlobalScope; runBlocking в компонентах приложения.
5. Магические строки и числа без константы.
6. catch (e: Exception) без логирования и маппинга в AppError.
7. Проглатывание CancellationException.
8. «Божественные» классы, объединяющие несколько слоёв.
9. Инфраструктурное сообщение исключения напрямую пользователю.
10. !! в production-коде.
11. Большие бинарные данные в Room как BLOB.
12. Дубликат delegate preferencesDataStore для одного файла.
13. Состояние, переживающее смерть процесса, только в памяти.
14. Новые классы Manager/Helper/Utils/Handler.
15. Backtick-имена тестов, кириллица и пробелы в именах тестов.
16. TODO без номера issue.
17. println в production-коде.
18. Upsert через INSERT REPLACE там, где семантика — UPDATE (молчаливое создание строки).

---

## 20. Лимиты размера кода

| Сущность | Лимит | Действие при превышении |
|---|---|---|
| Метод | 30 строк | декомпозировать на приватные подметоды |
| Класс | 300 строк | выделить отдельные компоненты |
| Composable | 60 строк | вынести приватные @Composable |
| Файл | 400 строк | разделить на несколько файлов |
| Параметры функции | 5 | вынести в data class *Params |
| Вложенность условий | 3 | ранние возвраты, вынос в функции |

---

## 21. Чек-лист перед коммитом

- [ ] ./gradlew test зелёный; число тестов сверено с XML-отчётом.
- [ ] scripts/check_standards.py завершается с кодом 0.
- [ ] Метод <= 30 строк, класс <= 300, файл <= 400.
- [ ] Нет бизнес-логики в Presentation; нет DAO в Presentation/Service.
- [ ] Цепочка слоёв соблюдена; новые зависимости обоснованы.
- [ ] Имена классов имеют правильный суффикс; имён-запрещёнок нет.
- [ ] Ошибки типизированы и залогированы с cause; CancellationException проброшен.
- [ ] UPDATE проверяет rows affected; DELETE идемпотентен.
- [ ] Имена тестов — camelCase без backtick; KDoc на русском парный.
- [ ] Нет TODO без issue; нет FIXME/HACK; нет markdown ** в KDoc.
- [ ] Heredoc-маркеры не попали в файлы; head/tail файлов проверены.
- [ ] git status не содержит лишних файлов; staging перечислен явно.
- [ ] Коммит-сообщение: тип(scope): тема plus блоки Проблема/Решение/Результат.

---

## 22. Процесс коммитов и issue

1. Один коммит = одно логическое изменение.
2. Переименования файлов — через git mv (сохранение истории).
3. В staging перечисляются ЯВНО все файлы изменения: коммит только rename
   без содержимого — известный баг процесса (см. историю 679e283).
4. Формат сообщения: тип(scope): краткая тема; тело — Проблема, Решение,
   Результат (с числом тестов). Язык — русский.
5. Push только после зелёных тестов и проверок чек-листа.
6. Issue в GitHub заводится ДО коммита с TODO-ссылкой; создаёт ИИ или
   разработчик через gh CLI.

---

## 23. Известный технический долг

| Класс / место | Сейчас | Должно быть |
|---|---|---|
| TimerManager / TimerManagerImpl | суффикс Manager | TimerService / TimerServiceImpl |
| LocaleManager | суффикс Manager | LocaleProvider |
| CategoryLocalizer | суффикс Localizer | CategoryLocalizationProvider |
| DataExporter / DataImporter | без суффикса Repository | DataExportRepository / DataImportRepository |
| Service -> Repository напрямую | без Provider | ProjectProvider / SessionProvider где нужна агрегация |
| AddProjectService, репозитории | System.currentTimeMillis() | TimeProvider |
| Session.updatedAt | default = System.currentTimeMillis() | без default, время извне |
| targetSdk | 34 | 35 plus edge-to-edge после визуальной проверки |
| Migration2To3Test | собран, не запущен | прогон на эмуляторе (connectedDebugAndroidTest) |
| Backtick-имена в части тестов | TimerManagerImplTest, ProjectsListViewModelTest, SaveSessionServiceTest, StatsViewModelTest, LocaleManagerTest, CategoryLocalizerTest, ThemeModeTest | camelCase без backtick |
| AdManager / PaymentManager | NoOp-заглушки | реализация при включении монетизации |

Долг закрывается по мере касания файлов; приоритет — нарушения границ слоёв,
потерянные ошибки, прямые обращения к хранилищу из Presentation.

---

## 24. История документа

- 1.0: первая фиксация правил (Clean Architecture, SOLID, DDD, лимиты).
- 2.0: добавлены Provider-слой, суффиксы, Symfony-аналогии, camelCase-тесты.
- 3.0: ASCII-схема вместо box-drawing, терминологическая справка Provider.
- 4.0: окончательная консолидация после критического анализа диалога:
  восстановлены потерянные разделы (DI, именование, тесты, TODO, запреты,
  чек-лист, процесс, техдолг); добавлен автоматический валидатор
  scripts/check_standards.py; учтены правила rows affected, IoError,
  CancellationException, FileLoggingTree, единственный DataStore, splash.
