# KnitTrac — Технический долг (TECHDEBT)

## Текущий техдолг
| Файл / Место | Проблема | Почему так | План исправления |
|---|---|---|---|
| Migration 2→3 | Не покрыта тестами: instrumented-тест удалён, `2.json` в репозитории нет | Эмулятор крашится на AMD GPU (`QEMU2 CPU0 thread`), USB-устройства нет | **Принятый риск** (18.10.2026). Восстановить через `2.json` + Robolectric или прогон на USB-устройстве. |
| SettingsScreen.kt (327 строк) | Слишком длинный файл | Много UI-состояний | Разбить на подкомпоненты |
| TimerForegroundService.kt (330 строк) | Слишком длинный файл | Много логики сервиса | Вынести логику в хелперы |
| TimerService / TimerForegroundService | Коллизия имен (Service) | Не различаются доменный и Android-сервис | В стандарты добавить *AndroidService для платформенных |
| Package name | `com.knittrac.app` (через «c») не совпадает с брендом KnitTrack (через «k») | Историческое имя | Переименовать через Android Studio перед публикацией в Google Play (Фаза 9) |
| ProjectDto | Добавлены поля totalRows/targetRowCount | Обратная совместимость | Версионирование DTO при синхронизации с бэкендом |


## Уроки 31.10.2026 (сессия 7c-2 + 7f-B)

- **SQLite не поддерживает `INSERT OR UPDATE` в одном `CREATE TRIGGER`.** Нужны два триггера. Проверять grammar до написания миграции.
- **При фиксе кода — сразу фиксить §X документа в том же коммите.** Иначе docs↔code рассинхрон.
- **`Owner-approved: <файл>` — обязателен при правке §1.1.** Ретроактивно не исправить.
- **`getOperationsForRow` требует `patternId`, не только `rowId`.** `pattern_operations.symbol_code` → FK на `symbols.code`.
- **`mapNotNull` скрывает баги.** При маппинге «много → много» — `Result.Error` при потере.
- **`StandardTestDispatcher` требует `advanceUntilIdle()`.** `viewModelScope.launch` не выполняется немедленно.
- **Fresh install не проходит через `MIGRATION_X_Y`.** Триггер/CHECK создаётся только в миграции. Для `onCreate` — `RoomDatabase.Callback`. Регресс P0-1.
- **`Float.toInt()` — усечение к нулю, не floor.** `(-16f/32f).toInt() = 0`, не `-1`. Для координат Canvas после pan — guard `if (coord < 0f) return`, не полагаться на `toInt()`.
- **Тест на `coerceAtLeast`/`coerceIn` — входные данные выше порога.** `maxColumnsOf(5+3) = 10` — это coerce, не формула. `maxColumnsOf(8+3) = 11` — формула.

### Открыто после code review 31.10.2026

- [ ] **P1-5**: `TimerScreen` permission flow (`if (!permissionViewModel.isGranted()) launcher.launch(...)`) не покрыт тестом. Регресс невидим. Robolectric compose test (`createComposeRule`).
- [ ] **P2-4**: `PatternEditorViewModel.retry()` перезагружает только `LoadPattern(currentPatternId)`. Ошибки от `observePatternSummaries` не обрабатываются.
- [ ] **P2-5**: `Migration5To6Test` использует симуляцию v5 через `DROP TRIGGER + PRAGMA user_version=5`, не реальную v5. Заменить на raw SQL `CREATE TABLE` v5 (эталон `Migration4To5Test`).
- [ ] **P2-1 (P3→P2)**: `getOperationsForRow` грузит legend на каждый вызов. На Canvas с N видимыми рядами — N загрузок. Batch-запрос.


## Аудит 31.10.2026 (Grok, полный)

Файла `AUDIT-2026-10-31.md` в репозитории нет. Открытые пункты перечислены здесь.

### Закрыто в этом коммите

- [x] **P1 #3**: fresh install без CHECK. `DATABASE_CALLBACK.onCreate` пересоздаёт пустые `projects` и `sessions` с CHECK (тот же SQL, что `MIGRATION_4_5`). `project_progress` не дропается: `PRAGMA foreign_keys = OFF` на время DROP, `ON` до триггеров.
- [x] **P1 #4**: `AddProjectViewModel` — `isSaving` до `launch`, сброс в `finally`.
- [x] **P1 #6**: `Screen.Settings`, маршрут в `AppNavHost`, иконка в TopBar списка проектов.
- [x] **P1 #7**: статистика с карточки своего проекта, не с `projects.first()`.
- [x] **P1 #8**: KDoc `TimerViewModel` — разрешение в `PermissionViewModel`.

### Открыто (P1)

- [x] **P1 #1**: Timber убран из `SaveSessionService`. Лог остаётся в `SessionRepositoryImpl`. Закрыто в этом коммите.
- [x] **P1 #2**: `AppErrorMapper` — `android.database.SQLException` → `DatabaseError`. `Owner-approved`. Закрыто в этом коммите.
- [x] **P1 #5**: `TimerForegroundService` стартует из `TimerScreen.onClick(Start)` через `ContextCompat.startForegroundService` + `ACTION_START`. `Reset` / `SessionSaved` → `ACTION_STOP`. `ServiceCompat.startForeground` с `FOREGROUND_SERVICE_TYPE_SPECIAL_USE` (API 34+). Manifest: +`FOREGROUND_SERVICE_SPECIAL_USE`. Закрыто в этом коммите.
- [ ] **P2**: `TimerForegroundService` не покрыт unit-тестом (Robolectric + `ServiceController` + `ShadowService`). Отложено.
- [ ] **P2 (7g-D2)**: foreground-старт вынести в `ForegroundServiceStarter` после декомпозиции `TimerViewModel` (detekt `LongParameterList=6`). Сейчас сервис стартует `TimerScreen`.

### Открыто (P2/P3)

- [ ] **P2**: Compose-тест `ProjectsListScreen` (иконка stats на карточке). Robolectric `createComposeRule` в проекте нет. Тот же класс, что P1-5 у `TimerScreen`.
- [x] **P2 #24**: `deleteProject` при 0 строк возвращает `NotFoundError`. Контракт изменён осознанно: удаление больше не идемпотентно. Закрыто в этом коммите.
- [x] **P2 #25**: `addSessionAtomically` — нет проекта → `NotFoundError` (`ProjectNotFoundException`). Строки 161 и 169 `SessionDao` по-прежнему `IllegalStateException` → `DatabaseError`. Закрыто в этом коммите.
- [x] **P2 #27**: `ProjectsListViewModel.retry` отменяет предыдущий `collect`. `StatsViewModel.retry` отменяет предыдущий `launch` (`getDailyStats` — не Flow). Закрыто в этом коммите.
- [x] **P2 #28**: `TimerViewModel` на `Result.Error` от `getAllProjects` шлёт `ShowError(error_load_projects)`. Закрыто в этом коммите.
- [x] **P2 #34**: повторный `ACTION_START` отменяет предыдущий `collect` уведомления. `handleStop` тоже отменяет его. Закрыто в этом коммите.
- [ ] **P2**: `PatternEditorScreenTest` не создан: `createComposeRule` в проекте нет. Zoom/pan/tap на устройстве.
- [x] **P2-A**: `PatternCanvas` рисует номера рядов в header через `TextMeasurer.measure` + `DrawScope.drawText`. Закрыто в этом коммите.
- [x] **P2-B**: `PatternCanvas` guard `localY < 0f` в `detectTapGestures` — тап «выше первой строки» после pan не попадает в row 0. Закрыто в этом коммите.
- [x] **P2 #30**: backup `1.2` пишет patterns, patternRows, symbols, patternRepeats, patternOperations. Импорт `1.0`/`1.1` обнуляет patternId, currentPatternRowId и currentRowNumber. Закрыто в этом коммите.
- [ ] **P3-A**: `Reset`/`SessionSaved` шлют `startService(ACTION_STOP)` даже если сервис не запущен — лишний onCreate/onDestroy.
- [ ] **P3-B**: `handleSystemRestart` показывает `00:00:00`, таймер при этом на паузе.
- [ ] **P3**: `project_progress` в бэкап не входит. В коде нет INSERT в эту таблицу.
- [ ] **P2 #9–#23, #26, #29, #31–#33, #35–#40** и **P3 #41–#48** — из аудита 31.10.2026.



## Открытые P3 после 7f-B/7f-C (ноябрь 2026)

### Pattern Editor

- [ ] **P3**: `getPatternById` грузит operations всех рядов. Отложенная загрузка видимых рядов — P3.
- [ ] **P3**: `getPatternById` мапит `PatternRowEntity` дважды (`attachOperations` и `PatternEntity.toDomain`). Оптимизация: передать уже собранные ряды в `toDomain`.
- [ ] **P3**: подписи техники и направления в диалоге создания схемы — `enum.name`. Локализация отдельно.
- [ ] **P3**: `CreatePatternDialog` без compose-теста (`createComposeRule` в проекте нет).
- [ ] **P3**: одинаковые `PatternName` разрешены.
- [ ] **P3**: после создания схемы маршрут остаётся `pattern_editor_new`. Позже `navigate("pattern_editor/{id}")` с `popUpTo(pattern_editor_new) { inclusive = true }`.
- [ ] **P3**: undo — стек 25. Redo и сохранение стека через process death позже: `SavedStateHandle` не хранит `PatternOperation`.
- [ ] **P3**: sheet (`ModalBottomSheet`) без compose-теста: `createComposeRule` в проекте нет.
- [ ] **P3**: номера рядов масштабируются с zoom/pan (MVP). Фиксированный размер шрифта — отдельный `Canvas` в `Row`, синхронизация `offset`/`scale` вручную.

---

## Pattern Tracker v8.0 (утверждено 19.10.2026)

Полная спецификация: [docs/architecture/pattern-tracker-v8.0.md](./pattern-tracker-v8.0.md)
Семантика рядов и схем: [docs/architecture/rows-and-patterns.md](./rows-and-patterns.md)
Дизайн Pattern Editor: [docs/architecture/pattern-editor-design.md](./pattern-editor-design.md)

### Выполнено (Pre-v7)
- [x] Шаг 5: Добавлены таблицы symbols, pattern_operations, pattern_repeats и PatternDao.
- [x] Backup DTO 1.1 (добавлены новые поля и списки, обновлены тесты).
- [x] Миграция БД v4->v5 (схемы, прогресс, внешние ключи).
- [x] Исправлен баг потери данных (totalRows/targetRowCount) в мапперах Entity и DTO.
- [x] Шаг 6: Внедрены Value Objects RowCount и StitchCount (устранен Primitive Obsession).

### Шаг 7a — открытые P2 (Domain-модель схем)
- [ ] `BackgroundImage` VO: 4 поля `Pattern.backgroundImageUri: String?`,
  `imageOffsetX: Float`, `imageOffsetY: Float`, `imageScale: Float` — кандидаты
  на единый VO, если появится калибровка с инвариантами (сейчас YAGNI).
- [ ] `Symbol.displayName` — не локализован. Локализация через
  `SymbolLocalizer` (Presentation), появится в 7d+.
- [x] `PatternRepeat` — закрыто в 7b-prep: `.value` заменено на сравнение с
  `StitchCount.ZERO` / `RowCount.ZERO`.
- [x] `TechniqueTest`, `PatternDirectionTest` — закрыто в 7b-prep.

### Шаг 7b-prep — открытые P2 (после fix(domain))
- [x] `PatternRow.id: Long` -> `PatternRowId`, `PatternOperation.patternRowId: Long`
  -> `PatternRowId` — закрыто в de1d4b0 (7b-prep, P1-3). SOLID приоритет 1
  (STANDARDS §11 п.6).
- [x] `Project.patternId: Long?` -> `PatternId?` — закрыто в 4f9baab (7c-prep).
- [x] `Project.currentPatternRowId: Long?` -> `PatternRowId?` — закрыто в 4f9baab (7c-prep).

### Fix-очередь 7b-prep (закрыто в 99336af)
- [x] `PatternTest.kt`: `create_withInfiniteOffsetY_throws` -> `create_withNaNOffsetY_throws`.
- [x] `PatternTest.kt`: добавлен `create_withPositiveInfiniteOffsetX_throws`.
- [x] `PatternIdTest.kt`: добавлен `create_withMaxLong_returnsValue`.
- [x] `PatternRowIdTest` — включён в коммит 0188029 (не 7d).

### Шаг 7b — открытые P2 (из ревью adb730b)
- [ ] `Symbol.ownerPatternId: Long?` -> `PatternId?` (LIN, Primitive Obsession).
- [x] `Project.patternId: Long?` -> `PatternId?` (LIN, 7c) — закрыто в 4f9baab (7c-prep).
- [x] `Project.currentPatternRowId: Long?` -> `PatternRowId?` — закрыто в 4f9baab (7c-prep).
- [ ] `RowCount.isPositive()` / `StitchCount.isPositive()` (BIZ, 7e+):
  убрать прямые сравнения с `ZERO` в `init` блоках.
- [ ] Pre-commit hook: проверка `.git/index.lock` (PROC).
- [ ] `Pattern` — God Class (SOLID, 7e+): разделение `PatternMetadata`,
  `PatternCalibration`, `PatternContent`.
- [ ] `PatternDirection` — enum, `getDirectionForRow` — `when` (SOLID, OCP, 7e+).

### Шаг 7f-A (UI) — открытые P2

- [x] `TimerViewModelTest`: `MarkNextPatternRow` покрыт 4 кейсами (success,
  error->ShowError, double-tap guard, no-project guard). Закрыто в 7g-B.
  God Object ViewModel: закрыто в 7g-D (PermissionViewModel, TimerViewModel - 5 зависимостей, @Suppress снят).
- [x] Hilt-граф `MarkNextRowUseCase` → `ProgressService` → `PatternRepository`
  → `PatternDao`/`SymbolDao`/`PatternRepeatDao` теперь reachable из
  `TimerViewModel`. Runtime-проверка — `@HiltAndroidTest` (7g-A, `HiltGraphTest`).

### Шаг 7f-A — открытые P2

- [ ] `ProgressService.markNextRow` = 3x `getPatternById` на клик:
  `MarkNextRowUseCase` (1) + `markNextRow.selectNextRow` (1) + `markRow` (1).
  Оптимизация — переделать `markRow` и `markNextRow` на `pattern` параметр
  (7g), не половину в 7f-A.

### Шаг 7e — открытые P2

- [ ] `MarkRowUseCase` дублирует `getPatternById` с `ProgressService.markRow`:
  `Pattern` загружается дважды (2 SQL-запроса на одну отметку ряда).
  Оптимизация — переделать `ProgressService.markRow(projectId, rowId)` (внутренняя
  загрузка) и убрать двойной вызов.

### Шаг 7d — открытые P2

- [x] `AddPatternOperationService.validateOverlap` использовал
  `params.symbol.defaultColumnSpan` вместо `legendSymbol.defaultColumnSpan` —
  overlap мог быть пропущен при stale span из UI-кеша. Закрыто в 7g-C (D8).
- [ ] `AddPatternOperationService.persist` использует `params.symbol` без
  нормализации по legend: при stale `defaultColumnSpan` сохранится неверный
  `PatternOperation.effectiveColumnSpan`. Тот же класс, что D8, но в слое
  хранения. Исправить в 7g-D или отдельным коммитом.

- [ ] `PatternOperation.toEntity` теряет `rowSpanOverride`: Domain
  `PatternOperation` имеет один `overrideSpan: Span?`, а Entity — два
  (`columnSpanOverride`, `rowSpanOverride`). `rowSpanOverride` всегда null.
  Исправить при появлении вертикального span в Domain (7e+).

### Шаг 7d-prep — открытые P2

- [ ] `PatternRepositoryImpl.getPatternById` выполняет 5 запросов
  (`pattern` + `rows` + `operations` + `symbols` + `repeats`). Операции — один SQL, не N+1.
  Для единичного вызова — ок; при загрузке списка patterns потребуется batch/projection.
- [ ] `pattern_repeats` без CHECK-констрейнта на `stitch_count > 0` и
  `row_count > 0` (в `PatternRepeat.init` — `require`, но БД не защищает).
  При повреждённых данных маппинг упадёт в init. Добавить CHECK в миграции v5->v6.

### Шаг 7c-2 — закрыто (trg_progress_check)

- [x] Триггеры `trg_progress_check_insert` / `trg_progress_check_update`
  (`BEFORE INSERT` / `BEFORE UPDATE OF current_pattern_row_id, current_row_number,
  current_stitch, pattern_id ON projects`): A (row ∈ pattern), B (number =
  row_number), C (row IS NULL → number = 0). Миграция 5→6 с санитизацией
  (A+B → B-fix → C). `Migration5To6Test` — 8 кейсов. Семантика —
  `rows-and-patterns.md`.
- [ ] D-инвариант (`stitch > 0 → row IS NOT NULL`) — P3. Не входил в 7c-2.
  Включить с per-stitch tracking (7f-D). Сейчас `current_stitch` в БД всегда 0
  (`ProgressService.updateProgress` → `StitchCount.ZERO`).

### Шаг 7c — открытые P2

- [x] `AppErrorMapper.map(android.database.SQLException)` -> `DatabaseError` (P1 #2, `e13e255`).
  Suspend-репозитории явно создают `AppError.DatabaseError(ex)` в catch-блоках.
- [x] Timber убран из `SaveSessionService` (P1 #1, `e13e255`). `Timber` в
  `SessionRepositoryImpl` (Data).

### Шаг 7b — открытые P2 (SOLID)
- [ ] `Pattern` — God Class (13 полей + 1 метод). Кандидат на разделение (7e+):
  `PatternMetadata` (id, name, technique, direction, isReversible),
  `PatternCalibration` (backgroundImageUri, imageOffsetX/Y/Scale),
  `PatternContent` (legend, rows, repeats). Сейчас — ТЗ §3.4 требует
  метод агрегата (`getDirectionForRow`), поэтому не разделяем.
- [ ] `PatternDirection` — enum, `getDirectionForRow` — `when`. При добавлении
  нового направления — менять `Pattern` (OCP). Кандидат на sealed class (7e+).
  Сейчас — ТЗ §4 требует enum.

### Запланированный техдолг (Post-v8.0)

| ID | Раздел ТЗ | Срок | Описание |
|---|---|---|---|
| TD-PT-1 | [§3.2](./pattern-tracker-v8.0.md#32-immutability) | v7.2 | UI для таблицы `project_overrides` |
| TD-PT-2 | [§3.2](./pattern-tracker-v8.0.md#32-immutability) | v8 | Синхронизация `pattern_sync_metadata` |
| TD-PT-3 | [§7](./pattern-tracker-v8.0.md#7-presentation) | После v8.0 | Счётчик петель (`current_stitch`) для спиц в UI |
| TD-PT-4 | [§3.8](./pattern-tracker-v8.0.md#38-бисероплетение) | v9.0 | Векторная семантика крючка |
| TD-PT-5 | [§6](./pattern-tracker-v8.0.md#6-infrastructure-room-v5) | v7.2 | Overlap операций на уровне триггера БД |
| TD-PT-6 | [§9](./pattern-tracker-v8.0.md#9-миграция-4--5--rollback) | v7.2 | Zip-бэкап с изображениями |
| TD-PT-7 | [§6](./pattern-tracker-v8.0.md#6-infrastructure-room-v5) | v7.2 | Иконки-файлы для кастомных символов |
| TD-PT-8 | [§6](./pattern-tracker-v8.0.md#6-infrastructure-room-v5) | v8 | JIS-контент-пак (Nihon Vogue) |
| TD-PT-9 | [§3.8](./pattern-tracker-v8.0.md#38-бисероплетение) | v9.0 | Семантика бисероплетения (peyote, brick, RAW и др.) |
| TD-PT-10 | [§8](./pattern-tracker-v8.0.md#8-accessibility) | v8 | Интеграция с Ravelry |
| TD-PT-11 | [§8](./pattern-tracker-v8.0.md#8-accessibility) | v9 | Wear OS и голосовое управление |
| TD-PT-12 | [§10](./pattern-tracker-v8.0.md#10-монетизация) | После v8.0 | Аналитика premium-конверсии |
| TD-PT-13 | [§10](./pattern-tracker-v8.0.md#10-монетизация) | TBD | RU-платежи (вне GP Billing) |
| TD-PT-14 | [§1](./pattern-tracker-v8.0.md#1-общая-информация) | До релиза | FGS type `specialUse`: property для Play Console |
| TD-PT-15 | [§10](./pattern-tracker-v8.0.md#10-монетизация) | TBD | Включение рекламы + позиционирование |

## Техдолг по ревью сессии 18.10.2026

### P0 — блокеры шага 3.1c

| Место | Проблема | План |
|---|---|---|
| TimerScreen.kt | `rowCount` не в `SavedStateHandle` — теряется при process death (свернул, система убила) | Передать в `SavedStateHandle`, восстановить в init |
| TimerScreen.kt | `FloatingActionButton.size(96.dp)` вместо `LargeFloatingActionButton` | Заменить на `LargeFloatingActionButton` (Material3) |
| TimerScreen.kt | `collectAsState()` — продолжает собирать `state`, когда экран в фоне (секундный Flow таймера) | `collectAsStateWithLifecycle()` из `lifecycle-runtime-compose` |
| TimerScreen.kt | `HapticFeedbackType.LongPress` на всех кнопках (Start/Pause/Reset/Save и `+1`) — тяжёлая отдача | Оставить haptic только на `+1`, использовать `TextHandleMove` или `VirtualKey` |

### P1

| Место | Проблема | План |
|---|---|---|
| TimerScreen.kt | `contentDescription = "Decrease row count"` — хардкод EN, UI локализован на RU | Вынести в `strings.xml` (`decrease_row_count`) |
| ProjectDao / SessionDao | `incrementProjectTotals` продублирован в двух DAO | Свести к одному (делегирование или единственное место) |
| TimerViewModel | `handleSelectProject` не сбрасывает `rowCount` при смене проекта | Продуктовое решение: сбрасывать / спрашивать / оставить. Тест `selectProject_setsTargetRowCount` фиксирует текущее поведение |
| core/common/Result.kt | `Result<T>` без `map`/`fold`/`onError` — риск молчаливого игнорирования `Error` | Добавить комбинаторы; заменить `if (result is Success)` на `fold` |
| ProjectRepositoryImpl | `@Deprecated updateTotalTime` без `ReplaceWith` и без плана удаления | Добавить `ReplaceWith("updateProjectTotals(..., rows = 0)")`, мигрировать call sites, удалить |
| TimerContract | `ShowError(messageResId = 0)` — `getString(0)` крашит `Resources.NotFoundException` | `require(messageResId != 0)` в конструкторе |
| AddProjectViewModel | `ShowError("Ошибка сохранения проекта")` — generic, не использует `result.error.message`; `"Цель не может быть отрицательной"` — хардкод вместо strings.xml | `ShowError(@StringRes Int)` или `result.error.message`; строку в XML |

### P3

Пусто после сессии 31.10.2026. Открытые P3 — в секции «Открытые P3 после 7f-B/7f-C (ноябрь 2026)».

### P2

| Место | Проблема | План |
|---|---|---|
| TimerScreen.kt | «Нет проектов» — только текст ошибки, нет действия «Создать проект» / «К списку» | Добавить кнопку навигации |
| TimerScreen.kt | `formatTime` использует `Locale.US` — неочевидно для RU-локали | `Locale.ROOT` или комментарий, почему US |
| SessionDao / DailyStat | `getDailyStats` возвращает `com.knittrac.app.domain.entity.DailyStat` — утечка Domain-модели в Data | Projection-класс в `data/local/`, маппинг в репозитории |
| TimerViewModelTest | `mockk<Project>` bypasses `init { require(...) }` — ложно-зелёные тесты при будущих правках | Реальный `data class` конструктор в тестах |

- [ ] **P2 #7 (rowSpanOverride)**: `UpdatePatternOperationService` (7f-D-1) работает в текущей модели (`overrideSpan` -> `columnSpanOverride`), `rowSpanOverride` = `null`. Не трогаем до появления вертикального span в Domain.
- [ ] **P3**: Перемещение ряда использует временный номер `999999` (`TEMP_ROW_NUMBER`) для обхода `UNIQUE(pattern_id, row_number)` при сдвиге. Альтернатива: `MAX(row_number) + 1` или удаление+вставка. Текущий вариант быстрый и работает, но магическое число.
