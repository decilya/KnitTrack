# Стандарты разработки KnitTrac

Этот документ описывает архитектурные и стилистические стандарты проекта KnitTrac.
Все новые фичи и рефакторинги должны строго соответствовать этим правилам.

---

## 1. Архитектура

Проект использует **Clean Architecture** с разделением на слои:
- **Domain**: бизнес-логика, UseCases, Entities, Value Objects, интерфейсы репозиториев. Не знает об Android.
- **Data**: реализация репозиториев, Room (DAO, Entity), DataStore, DTO, мапперы.
- **Presentation**: UI (Jetpack Compose), ViewModels, MVI-контракты (State, Action, Effect).
- **Platform / Core**: специфичные для Android реализации (TimerManager, Notifications, TimeProvider), базовые классы, DI-модули.

Зависимости направлены строго внутрь: `Presentation` -> `Domain` <- `Data`.

## 2. Domain слой

- **Value Objects**: Примитивы (String, Long) запрещены для бизнес-сущностей, если есть инварианты. Используем `@JvmInline value class` с приватным конструктором и фабриками (`create`, `createOrNull`, `createOrDefault`).
- **Entities**: `data class`, содержащие только бизнес-данные. Аннотация `@Keep` для защиты от R8/Gson.
- **UseCases (Services)**: Одиночные классы с `operator fun invoke` или `execute`. Одна задача = один UseCase.
- **Repositories**: Только интерфейсы. Возвращают `Flow<T>` или `Result<T>`.

## 3. Data слой

- **Persistence Ignorance**: Room Entity и DTO не должны проникать в Domain.
- **Mappers**: Отдельные файлы `EntityMappers.kt` и `DtoMappers.kt` с extension-функциями (`toDomain()`, `toEntity()`).
- **DAO**: Возвращают только Entity или примитивы. SQL-запросы в `@Query`.
- **Repositories**: Реализации интерфейсов из Domain. Инжектируют DAO. Оборачивают исключения БД в `AppError.DatabaseError`.

## 4. Presentation слой (MVI)

- **Contract**: `object` с `data class State`, `sealed class Action`, `sealed class Effect`.
- **ViewModel**: Наследуется от `androidx.lifecycle.ViewModel`. Принимает UseCases. Не знает про `Context`, `Uri`, `SharedPreferences`.
- **Composable (Screen)**: Только отрисовка `State` и отправка `Action`. Никакой бизнес-логики.
- **Эффекты**: Одноразовые события (навигация, снакбары) через `Channel` или `SharedFlow`.

## 5. Асинхронность и Coroutines

- **Dispatchers**: Инжектируются через `@IoDispatcher` / `@DefaultDispatcher`. Запрещен хардкод `Dispatchers.IO` в бизнес-логике.
- **Scope**: `viewModelScope` во ViewModel, `serviceScope` в Foreground Service. Запрещен `GlobalScope`.
- **Flow**: `StateFlow` для состояния, `SharedFlow` / `Channel` для эффектов.
- **Время**: Запрещен прямой вызов `System.currentTimeMillis()` в таймерах. Использовать `TimeProvider` (wall clock vs monotonic `elapsedRealtime`).

## 6. Обработка ошибок

- **Result**: Собственный sealed-класс `Result<T>` (`Success`, `Error`).
- **AppError**: Sealed-класс для типизации ошибок (`ValidationError`, `DatabaseError`, `NetworkError`, `NotFoundError`).
- **Логирование**: `Timber.e(e, "message")` во всех `catch` блоках репозиториев.
- **CancellationException**: Всегда пробрасывается (`throw e`) перед обработкой `Exception`.

## 7. Локализация и UI

- **Строки**: Все тексты в `strings.xml`. Fallback-язык — английский (`values/`), русский в `values-ru/`.
- **Темы**: Enum `ThemeMode` (SYSTEM, LIGHT, DARK) вместо магических строк.
- **Compose**: Использование Material 3. Переиспользуемые компоненты (например, `EmptyStateView`, `SettingsSection`) выносятся в `presentation/common/`.

## 8. Тестирование

- **Unit-тесты**: Покрывают Domain (UseCases, Entities, Mappers) и ViewModel.
- **Инструменты**: JUnit 4, MockK, `kotlinx-coroutines-test`.
- **Диспетчеры**: `StandardTestDispatcher` или `UnconfinedTestDispatcher` + `Dispatchers.setMain()`.
- **Время**: `FakeTimeProvider` для тестов таймера.
- **Имена тестов**: Английский язык, camelCase. KDoc на русском.

## 9. Базы данных и Миграции

- **Room**: `exportSchema = true`. Схемы хранятся в `app/schemas/`.
- **Миграции**: Пишутся вручную в `AppDatabase.kt`. Для каждого изменения схемы — инструментальный тест (`MigrationTestHelper`).
- **Constraints**: `CHECK` ограничения добавляются в SQL-миграциях для защиты инвариантов Value Objects.

## 10. DI (Hilt)

- **Модули**: Разделены по слоям (`core/di`, `data/di`, `platform/di`).
- **Scope**: `@Singleton` на `@Binds` или реализациях репозиториев/менеджеров.
- **Context**: Используется только `@ApplicationContext`. `Activity Context` не инжектируется.

---

## 18. Строгие запреты (Anti-patterns)

1. Запрещён `Context` в Domain и Data слоях (кроме `@ApplicationContext` для ресурсов/DataStore).
2. Запрещена бизнес-логика в `Entity`, `Dto`, `ApiResource`.
3. Запрещён доступ к `Dao` из `ViewModel`.
4. Запрещён доступ к `Dao` из `Composable`.
5. Запрещён прямой вызов `System.currentTimeMillis()` в доменной логике и таймерах.
6. Запрещён `GlobalScope`.
7. Запрещён `runBlocking` в `Application`, `Activity`, `Fragment`, `ViewModel`.
8. Запрещены магические строки и числа без константы.
9. Запрещены универсальные `catch (e: Exception)` без логирования или преобразования ошибки.
10. Запрещено глотать `CancellationException`.
11. Запрещены «божественные» классы, объединяющие несколько слоёв.
12. Запрещено смешивать уровни ошибок: инфраструктурные исключения не должны напрямую становиться пользовательскими сообщениями без маппинга.

---

## 19. Целевое состояние проекта

Этот стандарт описывает целевое состояние. Существующие нарушения должны исправляться по мере работы с соответствующими файлами.

Приоритет исправлений:

1. нарушения границ слоёв;
2. отсутствие обработки ошибок;
3. прямые обращения к хранилищу из презентационного слоя;
4. нарушение тестируемости;
5. превышение лимитов методов и классов;
6. устаревшие комментарии и `TODO` без задач.

---

## 20. Чек-лист перед коммитом

Перед коммитом необходимо проверить:

- [ ] код компилируется;
- [ ] тесты проходят;
- [ ] метод не длиннее 30 строк;
- [ ] класс не длиннее 300 строк;
- [ ] нет бизнес-логики в контроллерах;
- [ ] нет прямого доступа к базе из презентационного слоя;
- [ ] сервисы работают только через репозитории;
- [ ] репозитории возвращают доменные типы;
- [ ] внешние данные проходят через DTO/ApiResource;
- [ ] ошибки типизированы;
- [ ] нет комментариев «что делает код», если это ясно из кода;
- [ ] нет `TODO` без ссылки на задачу;
- [ ] код понятен без дополнительного объяснения.
