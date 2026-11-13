# Карта проекта KnitTrac

Фактическое состояние кода. Составлено по `app/src/main/java/com/knittrac/app`,
проверено по файлам, не по HANDOVER.

---

## 1. Слои

Поток зависимостей направлен строго внутрь: Presentation и Platform знают Domain, Domain не знает ни Android, ни Room, ни Gson (проверяется DomainLayerTest). Core — сквозной слой, от которого зависят все.

    [Внешние слои]
      Presentation (MVI: Screen, ViewModel, Contract)
      Data         (Repositories, Room, Mappers)
      Platform     (Android-адаптеры, Timer, Notifications)
            |
            | (зависят от)
            v
    [Внутренние слои]
      Domain       (Entities, VOs, Services/UseCases)
            |
            v
      Core         (Result, AppError, BaseUseCase)

*Стрелка означает "зависит от". Domain зависит только от Core. Data и Presentation зависят от Domain. Core — сквозной слой.*

| Слой | Пакеты | Содержимое |
|---|---|---|
| Core | core/base, core/common, core/di, core/localization, core/logging, core/preferences | Result, AppError, ErrorCode, ErrorMapper, AppErrorMapper, ErrorStringProvider, SyncStatus, TimeProvider, BaseUseCase, BaseFlowUseCase, LocaleService, FileLoggingTree, SettingsKeys, DI-квалификаторы |
| Domain | domain/entity, domain/model, domain/service, domain/repository | Value Objects, агрегаты, Params, UseCase/Service, интерфейсы репозиториев |
| Data | data/repository, data/mapper, data/local, data/dto, data/di | *RepositoryImpl, мапперы Entity/Dto <-> Domain, Room (DAO, Entity, AppDatabase, миграции), DTO бэкапа |
| Platform | platform/timer, platform/service, platform/notification, platform/ads, platform/payments, platform/di | Android-адаптеры: TimerForegroundService, SystemTimeProvider, уведомления, no-op реклама/платежи |
| Presentation | presentation/feature_*, presentation/common, presentation/navigation | MVI: Screen + ViewModel + Contract/Intent/Effect, Compose-компоненты |

Точки входа: MainActivity.kt, KnitTracApplication.kt.

## 2. Application как отдельный слой отсутствует

**Факт:** слой `Application` физически не вынесен. UseCases лежат в
`domain/service` (рядом с `*Service` и `*Params`), DI-композиции для UI лежат в
`presentation/feature_pattern_editor/` (`PatternEditor*UseCases`).

Следствия:

- `BaseUseCase` / `BaseFlowUseCase` — в `core/base`, но сами UseCase — в Domain.
- `AGENTS.md §1` описывает цепочку `Presentation → Domain (Service/UseCase) → Data`.
- Разделение «Application vs Domain» в коде не поддерживается; при появлении
  второго UseCase-слоя его придётся выделять отдельно.

---

## 3. Value Objects

Все Value Objects — в `domain/entity` (отдельного `domain/vo/` нет). Это
`value class` с приватным конструктором и тремя фабриками:
`create` (throws), `createOrNull`, `createOrDefault`.

| VO | Тип значения | Инвариант |
|---|---|---|
| `ColumnStart` | `Int` | ≥ 0 |
| `Duration` | `Long` (секунды) | ≥ 0 |
| `RowCount` | `Int` | ≥ 0 |
| `RowNumber` | `Int` | ≥ 1 |
| `StitchCount` | `Int` | ≥ 0 |
| `Span` | `Int` | ≥ 1 (`Span.ONE`) |
| `PatternId` | `Long` | > 0 |
| `PatternRowId` | `Long` | > 0 |
| `ProjectId` | `Long` | > 0 |
| `SessionId` | `Long` | > 0 |
| `PatternName` | `String` | непустое, длина |
| `ProjectName` | `String` | непустое, длина |

Enum-типы домена (не VO, но там же): `Category`, `Technique`, `ThemeMode`,
`PatternDirection`, `SymbolCategory`.

### Domain-модели в `domain/model` (не VO и не Entity)

Проекции и объекты-параметры, у которых нет собственного инварианта:

- `OperationSpan` — занятый интервал операции в ряду `[columnStart, endExclusive)`
  для overlap-check. **Не entity и не VO**; инвариант `span ≥ 1` обеспечивают
  `ColumnStart` и `Span`.
- `MarkResult` — результат отметки ряда (`Added`, `AlreadyExists`).
- `PatternSummary` — проекция для списка схем.
- `*Params` — `AddPatternOperationParams`, `UpdatePatternOperationParams`,
  `AddSymbolParams`, `CreatePatternParams`, `DeleteOperationParams`,
  `DeletePatternRowParams`, `DeleteSymbolParams`, `UpdatePatternRowNumberParams`.

---

## 4. Ключевые сущности и агрегаты (`domain/entity`)

| Тип | Роль |
|---|---|
| `Project` | корень агрегата (проект + прогресс) |
| `Pattern` | корень агрегата схемы: `rows`, `legend`, `repeats`, кэш `rowIndexCache` |
| `PatternRow` | ряд схемы, владеет `List<PatternOperation>` |
| `PatternOperation` | операция в ряду; идентичность `(patternRowId, columnStart)` |
| `PatternRepeat` | раппорт |
| `Symbol` | символ легенды (`defaultColumnSpan`, `defaultRowSpan`) |
| `Session` | сессия вязания |
| `DailyStat` | проекция статистики по дню |

---

## 5. Шпаргалка «где что искать»

| Нужно | Путь |
|---|---|
| Бизнес-операция | `domain/service/*Service` |
| Сценарий (UseCase) | `domain/service/*UseCase` |
| Контракт репозитория | `domain/repository/*Repository` |
| Реализация репозитория | `data/repository/*RepositoryImpl` |
| Room DAO / Entity / миграции | `data/local`, `data/local/entity`, `AppDatabaseMigrations` |
| Контракт бэкапа | `data/dto`, версия — `CURRENT_VERSION` |
| Маппинг слоёв | `data/mapper` (единственная точка конверсии) |
| MVI State/Intent/Effect | `presentation/feature_*/[Feature]Contract.kt` |
| DI-модули | `core/di`, `data/di`, `platform/di`, `di/*Module.kt` |
| Архитектурный тест слоёв | `app/src/test/java/com/knittrac/app/architecture/DomainLayerTest.kt` |