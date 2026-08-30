# KnitTrac – Техническое задание и Архитектурная Спецификация (v37.0 Final)

## 1. Общие сведения
- **Название:** KnitTrac
- **Цель:** Android-приложение для учёта времени, проектов и статистики рукоделия (вязание, бисероплетение, макраме) с возможностью переноса данных.
- **Целевая аудитория:** Женщины 25–55 лет (фокус на «процессников», ценящих спокойствие, ASMR-таймер и визуализацию прогресса).
- **Монетизация:**
   - Freemium с ненавязчивой рекламой (баннеры, межстраничные). Реклама не показывается во время активного таймера.
   - Подписка ($5/мес или $50/год) – отключает всю рекламу, даёт неограниченное количество проектов и расширенную статистику.
- **Мультиязычность:** Русский и английский. Легко добавлять новые языки.
- **Платёжная система:** Yandex Pay SDK (замена Google Play Billing).
- **Рекламная сеть:** Yandex Mobile Ads SDK (замена AdMob).

## 2. Технологический стек
- **Язык:** Kotlin 1.9.22+
- **UI:** Jetpack Compose (Material 3), Navigation Compose
- **Архитектура:** Clean Architecture + MVC (View → Controller → Service → Repository → Model) + MVI для UI.
- **DI:** Hilt (через KSP, без kapt).
- **БД:** Room (SQLite) с Flow и агрегациями.
- **Асинхронность:** Coroutines + Flow (внедряемые диспетчеры).
- **Графики:** `co.yml:ycharts`.
- **Сериализация:** Gson.
- **Логирование:** Timber.
- **Хранение настроек:** DataStore Preferences (язык, статус подписки).
- **Локализация:** `strings.xml` (ru/en).
- **Реклама:** Yandex Mobile Ads SDK (8.3.0) – через интерфейс `AdManager`.
- **Платежи:** Yandex Pay SDK – через интерфейс `PaymentManager`.
- **Тестирование:** JUnit 4, MockK, Turbine, kotlinx-coroutines-test.

## 3. Архитектура и слои (MVC + Clean Architecture)
1. **View:** Compose Screen – только отрисовка, все тексты через `stringResource()`.
2. **Controller:** ViewModel (MVI) – принимает интенты, управляет состоянием, делегирует бизнес-логику сервисам. Бизнес-логики не содержит.
3. **Service Layer (Сервисный слой):** `domain/service/` – каждый класс решает ОДНУ бизнес-задачу, наследуется от `BaseUseCase` / `BaseFlowUseCase`, вызывает репозитории, возвращает `Result`.
4. **Repository Layer:** интерфейсы в `domain/repository/`, реализации в `data/repository/` – абстрагируют источники данных.
5. **Model:** `domain/entity/` (Project, Session, Category – enum без `displayName`).
6. **Локализация:** `core/localization/LocaleManager` – управление языком через DataStore.
7. **Реклама:** `platform/ads/AdManager` – интерфейс, реализация `AdManagerImpl` через Yandex Ads. Заглушка `NoOpAdManager`.
8. **Платежи:** `platform/payments/PaymentManager` – интерфейс, реализация `PaymentManagerImpl` через Yandex Pay. Заглушка для MVP.

## 4. Жёсткие архитектурные правила (НЕ НАРУШАТЬ)
1. **SRP и DIP:** В конструктор сервиса внедряются ТОЛЬКО интерфейсы репозиториев и `@IoDispatcher`.
2. **Синтаксис конструктора сервиса:** `class AddProjectService @Inject constructor(private val repo: ProjectRepository, @IoDispatcher dispatcher: CoroutineDispatcher) : BaseUseCase<AddProjectParams, Long>(dispatcher)` – обязательно `super(dispatcher)`.
3. **Синхронизация:** Все сущности имеют `updatedAt: Long` и `syncStatus: SyncStatus`. При insert/update/delete репозитории автоматом ставят `updatedAt = System.currentTimeMillis()` и `syncStatus = SyncStatus.PENDING.name`.
4. **Android Service:** Hilt не поддерживает конструкторную инъекцию – используй `@AndroidEntryPoint` и `@Inject lateinit var`.
5. **TimerForegroundService:** При "START" СНАЧАЛА `startForeground()`, затем корутина. При "STOP" – `stopForeground(true)`, `stopSelf()`. В `onDestroy()` – `serviceScope.cancel()`.
6. **TimerManager:**
   - `start()`: if (job?.isActive == true) return. Если `sessionStartTime == 0L` – сохраняет `System.currentTimeMillis()`. При возобновлении – НЕ меняет.
   - `pause()`: job?.cancel(), НЕ сбрасывает `_timeFlow.value`.
   - `reset()`: job?.cancel(), sessionStartTime = 0L, _timeFlow.value = 0L.
7. **SaveSessionService:**
   - `durationSeconds = (end - start) / 1000L`. Если <= 0 – `Result.Error(ValidationError)`.
   - Иначе создаёт Session, вызывает `addSession()`.
   - Если Success – вызывает `updateTotalTime()`. Если `updateTotalTime` вернёт Error – логируем через Timber, но возвращаем `Result.Success(id)` (частичный успех для MVP).
   - Если addSession вернул Error – НЕ вызывает updateTotalTime и возвращает ошибку.
8. **Тесты:** Для КАЖДОГО сервиса – юнит-тест, наследующий `BaseTest`. JUnit 4 (`@Before`/`@After`). Для suspend – `coEvery`/`coVerify`, для обычных – `every`/`verify`. В SaveSessionServiceTest проверяй корректность durationSeconds и что updateTotalTime НЕ вызывается при ошибке addSession.
9. **Compose:** Все `rememberLauncherForActivityResult` – в начале функции, ДО условий.
10. **Room Агрегация:** `DailyStat` в `domain/entity/`. Формула: `(startTimestamp / 86400000) * 86400000 AS dayTimestamp`. Фильтр по `projectId`.
11. **DI:** Для `@Provides` – object с `@Singleton`. Для `@Binds` – abstract class. `@Singleton` – на классе реализации.
12. **Локализация:**
   - Все UI-строки – в `res/values/strings.xml` (ru) и `res/values-en/strings.xml` (en).
   - `Category` – enum БЕЗ `displayName`. Названия через `LocaleManager.getLocalizedCategoryName()`.
   - Переключение языка – через экран настроек, вызывающий `LocaleManager.setLanguage()` и `recreate()`.
13. **Реклама (Yandex Ads):**
   - `AdManager` – интерфейс с методами `initialize`, `showBanner`, `hideBanner`, `showInterstitial`, `showRewarded`, флаги загрузки.
   - Реализация – через Yandex Mobile Ads SDK.
   - Если `isPremium == true` – реклама отключается (`AdManager.isAdsEnabled = false`).
   - Во время работы таймера реклама НЕ показывается.
14. **Платежи (Yandex Pay):**
   - `PaymentManager` – интерфейс с методами `isReady()`, `purchaseSubscription(productId)`, `checkSubscriptionStatus()`.
   - Реализация – через Yandex Pay SDK.
   - На Этапе 1 – заглушка, на Этапе 4 – реальная интеграция.

## 5. Структура проекта
```
com.example.knittrac
├── core/
│ ├── common/
│ ├── di/
│ ├── base/
│ └── localization/ # LocaleManager
├── domain/
│ ├── entity/
│ ├── repository/
│ └── service/
├── data/
│ ├── local/ # Room
│ ├── mapper/
│ └── repository/
├── platform/
│ ├── service/ # TimerManager, TimerForegroundService
│ ├── di/ # TimerModule
│ ├── ads/ # AdManager, AdManagerImpl, NoOpAdManager
│ └── payments/ # PaymentManager, PaymentManagerImpl, NoOpPaymentManager
└── presentation/
├── theme/
├── navigation/
├── feature_timer/
├── feature_projects/
├── feature_stats/
└── feature_settings/ # язык + подписка
```


## 6. Этапы разработки и критерии приемки (Definition of Done)

| Этап | Задачи | Критерии готовности |
|------|--------|----------------------|
| **1. Фундамент** | Gradle (KSP, Hilt, DataStore, Yandex Ads, Yandex Pay), Core, Domain, Data/Room, сервисы + тесты, локализация, AdManager (Yandex Ads), PaymentManager (заглушка), настройки (язык, подписка). | Проект компилируется. Все юнит-тесты проходят. AdManager и PaymentManager работают (заглушки). |
| **2. Таймер** | TimerManager, TimerForegroundService, MVI для таймера (TimerContract, TimerViewModel, TimerScreen). | Таймер работает в фоне >10 мин, уведомление обновляется, пауза не сбрасывает время. |
| **3. Статистика и бэкап** | Сервисы статистики, YCharts, SAF экспорт/импорт JSON. | График отображается, экспорт/импорт работают, ошибки логируются. |
| **4. Навигация, проекты и подписка** | AppNavHost, Projects экран, выбор проекта, настройки (подписка), интеграция реальной рекламы и платежей. | Реклама отображается для бесплатных пользователей, отключается при подписке. Платежи работают. |
| **5. Тестирование и релиз** | Инструментальные тесты, ручное QA, релизный AAB. | Приложение стабильно, все тесты проходят. 