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



## Ревью 05.11.2026 (Строгий код-ревью, все линзы)

- [x] **P2 (LIN/BIZ):** Санитизация `repeatCode` в Data-слое. **Закрыт 05.11.2026**. `BackupRepositoryImpl` очищен от бизнес-логики, ошибка БД маппится в `ValidationError` в `DataImporterImpl`.
- [x] **P2 (LIN/KOT):** Оркестрация в `PatternEditorViewModel.retry()` — **закрыт 05.11.2026**. Убран блокирующий `first()` из VM. Автозагрузка первой схемы перенесена в реактивный `observePatternSummaries`.
- [x] **P2 (BIZ/UX):** Молчаливое удаление данных при импорте. **Закрыт 05.11.2026**. Теперь при orphan `repeatCode` импорт прерывается с `ValidationError` (данные не теряются молча).
- [x] **P2 (AND/QA):** `PatternEditorViewModelTest` — LargeClass — **закрыт 05.11.2026**. Разбит на `PatternEditorViewModelTestBase` (общие моки + хелперы) + `PatternEditorViewModelTest` (12 lifecycle-тестов) + `PatternEditorViewModelEditingTest` (17 editing-тестов). Все `@Test` получили KDoc. `@Suppress("LargeClass")` снят.
- [ ] **P3 (LIN/DBA):** Дублирование SQL-триггеров (`trg_repeat_code_check_*`). Написаны в двух местах: `MIGRATION_8_9` и `DATABASE_CALLBACK.onCreate`. При изменении правила нужно править оба.
- [x] **P3 (LIN/DBA):** Оптимизация UPDATE-триггера — **закрыт 07.11.2026**. Добавлен `WHEN OLD.repeat_code IS NOT NEW.repeat_code` (`IS NOT` — NULL-safe; `!=` в SQLite вернул бы NULL для `NULL → NULL`, и WHEN пропустил бы). Тело триггера пропускается, если `repeat_code` не изменился. Касается fresh installs и новых v8→v9 миграций; уже-мигрированные v9 сохраняют старый триггер до v10 (P4, не актуально для pre-release).
- [ ] **P3 (UX):** Логика показа туториала. Сейчас он показывается при первом входе в `PatternEditorScreen` без учета контекста (видел ли пользователь другие экраны).
- [ ] **P3 (QA):** Отсутствие тестов на `Channel` события. Не проверено, что `Channel` не теряет события (снекбары/навигацию) при быстрых кликах.
- [ ] **P3 (AND):** Защита от забытых миграций в `DatabaseModule`. Если кто-то добавит миграцию в `AppDatabase`, но забудет в `addMigrations`, тесты миграций могут не поймать это.

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
- [x] **P2-4**: `PatternEditorViewModel.retry()` — **закрыт 05.11.2026**. Добавлен `summariesJob`, `retry()` перезапускает подписку на summaries и сбрасывает `errorRes`. Fallback: savedStateHandle → firstAvailable.
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
- [x] **P2 #9, #10** — закрыты попутно: `Session` VO (P1-D), `Project` VO (P1-E).
- [x] **P2 #12** — `StatsViewModel` через `GetProjectStatisticsService` (не `SessionRepository`). Коммит `bb963c1`.
- [x] **P2 #14** — `SaveSessionService : BaseUseCase<SaveSessionParams, SessionId>` + `@param:IoDispatcher`. Коммит `bb963c1`.
- [x] **P2 #16** — `SettingsViewModel`: `MutableSharedFlow(DROP_OLDEST)` → `Channel(BUFFERED)` + `receiveAsFlow`, `emit`→`send`. Коммит `6643f09`.
- [x] **P2 #17** — `AppDatabase`: `INSTANCE`/`getDatabase`/`@Volatile` удалены; `DatabaseModule.provideAppDatabase` строит `Room.databaseBuilder` напрямую. `[Owner-approved]`. Коммит `54fa33d`.
- [x] **P2 #23** — `collectAsState()` → `collectAsStateWithLifecycle()` на 5 экранах (AddProject, ProjectsList, Settings, Stats, Timer). Коммит `6643f09`.
- [x] **P2 #26** — `SettingsViewModel`: guard `isExporting`/`isImporting` до `launch`, `try/finally`. Коммит `6643f09`.
- [ ] **P2 #11** — `getDirectionForRow` `when` по enum. Сознательное решение ТЗ §4 (`rows-and-patterns.md §4`, TECHDEBT 7b): enum, не sealed — P3.
- [x] **P2 #13** — уже валидируется: `ProjectName.createOrNull` + `RowCount.create` (throws → catch → ValidationError).
- [x] **P2 #19** — `repeat_code` без FK. **Закрыт 05.11.2026** через 2 триггера (`trg_repeat_code_check_insert`/`_update`) в `MIGRATION_8_9` + `DATABASE_CALLBACK.onCreate`. FK невозможен: PK `pattern_repeats` composite `(pattern_id, code)`, `code` глобально не уникален. 3-way JOIN через `pattern_rows.pattern_id`. **DELETE не покрыт** (`deleteRepeat` в app нет); при появлении — 3-й триггер `BEFORE DELETE ON pattern_repeats`. **Факт:** `feat(data): repeat_code defense-in-depth — MIGRATION_8_9 (P2 #19)`.
- [x] **P2 #29** — не применимо: тика, перезаписывающего State, нет. Guard `isMarkingRow`/`isSaving` на месте (P1-C).
- [ ] **P2 #31–#33** — backup edge cases. Детали в handover v5 §5 отсутствуют; `grep TODO/FIXME/HACK/XXX` по backup-файлам = 0. Переклассифицировать в P3.
- [x] **P2 #35–#40** — косметика: `grep TODO/FIXME/HACK/XXX` по main = 0. Закрыто.
- [ ] **P3 #41–#48** — из аудита 31.10.2026, в P3-секции `TECHDEBT`.



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
- [x] D-инвариант (`stitch > 0 → row IS NOT NULL`) — закрыт в 7f-D-5 (MIGRATION_7_8).
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

- [ ] **P3**: `ownerPatternId` в `Symbol` — это `Long?`, а не `PatternId?`. Символы нельзя проверить на принадлежность схеме через VO. Нужно будет отрефакторить при внедрении `PatternId` во все места.

- [ ] **P3**: Правило D (`stitch > 0 → row IS NOT NULL`) дублируется в двух SQL-константах (`CREATE_TRIGGER_PROGRESS_INSERT_V8`, `CREATE_TRIGGER_PROGRESS_UPDATE_V8`). При изменении правила нужно править оба. Причина — SQLite не поддерживает `INSERT OR UPDATE` в одном триггере.
- [ ] **P2**: D-инвариант (`stitch > 0 → row IS NOT NULL`) защищён только в БД (триггер). В Domain (`Project.markRow` / `ProgressService`) проверки нет. Нужно добавить, чтобы инвариант был primary в Domain (DDD). Иначе `ImportDataService` или другие пути записи могут обойти триггер.

- [ ] **P3**: Альтернативный вход для accessibility — иконка `List` в `TopAppBar` PatternEditorScreen (список всех операций для пользователей, которым недоступен long-press). Нужна для WCAG 2.1 (2.5.1 Pointer Gestures). Отложено до 7f-D-6+.

- [ ] **P2**: `PatternEditorViewModelTest` — LargeClass (338 строк, >300). Разбить на `PatternEditorViewModelTest` (базовые) + `PatternEditorViewModelEditingTest` (6 новых кейсов 7f-D-6). После 7f-D-8.

- [ ] **P3**: Широкий аудит `Pair`/`Triple` в `app/src/main/` (grep по сигнатурам и локальным переменным). Заменить на явные DTO где читаемость страдает (как `PendingCell` в `PatternEditorScreen`). Правило — Урок 26 (HANDOVER §9).

- [ ] **P3**: Настройки DataStore (`THEME_MODE`, `NOTIFICATIONS_ENABLED`, `LEGEND_ADD_BUTTON_VISIBLE`) не попадают в `ExportDataDto` (backup v1.2). При импорте на новом устройстве пользователь получает defaults. Решение: включить SettingsKeys в backup v1.3 (или отдельный файл). Не блокер — defaults разумны.

## Открытые P1 после 7f-D (ревью 02.11.2026)

По результатам глубокого код-ревью серии 7f-D (11 коммитов, `90172d6..ab76eb6`). P0 — нет. Новые P1-нумерация — буквы (чтобы не путать с закрытыми `P1 #1..#8`).

### P1-A. `PatternEditorUseCases` — God Bag (10 полей) — **закрыт 02.11.2026**

**Файл:** `presentation/feature_pattern_editor/PatternEditorUseCases.kt` (удалён)
**Факт:** в 7f-D-6 → 8 полей, в 7f-D-7 → **10 полей** (`getPatternById`, `getAllPatternSummaries`, `addPatternOperation`, `deleteOperation`, `createPattern`, `updatePatternOperation`, `updatePatternRowNumber`, `deletePatternRow`, `addSymbol`, `deleteSymbol`).

**Проблема:**
- **SRP:** группа отвечает за **всё** — чтение, операции, ряды, символы. Одна причина изменения → любая фича Pattern Editor.
- **ISP:** `PatternEditorViewModel` зависит от **всех 10**, даже если использует 2–3.
- **Рост:** каждый новый UseCase → +1 поле. Через 2 фичи → 15–20 полей. God Bag.

**Решение (закрыто 02.11.2026):** разбито на 4 группы + PlatformDeps:
- `PatternEditorPatternUseCases` — `getPatternById`, `getAllPatternSummaries`, `createPattern` (жизненный цикл Pattern).
- `PatternEditorOperationUseCases` — `addPatternOperation`, `updatePatternOperation`, `deleteOperation` (операции в рядах).
- `PatternEditorRowUseCases` — `updatePatternRowNumber`, `deletePatternRow` (ряды).
- `PatternEditorSymbolUseCases` — `addSymbol`, `deleteSymbol` (легенда).
- `PatternEditorPlatformDeps` — `settingsRepository`, `savedStateHandle` (платформенные).

VM: 4 группы + PlatformDeps = **5 параметров** (порог 6, `@Suppress("LongParameterList")` **не нужен**). God Bag устранён.

**Факт:** коммит `refactor(presentation): split PatternEditorUseCases into 4 groups (P1-A)`.

### P1-B. `ProgressService` — 9 методов, 2 ответственности — **закрыт 02.11.2026**

**Файл:** `domain/service/ProgressService.kt`
**Факт:** `markRow`, `markNextRow`, `addRow`, `validateMembership`, `updateProgress`, `selectNextRow`, `pickNextRow`, `findAndAdvance`, `markSelectedRow` — **9 методов**, 2 сценария.
**Проблема:** SRP нарушен. Правка «отметить ряд» рискует сломать «следующий ряд».
**Решение (закрыто 02.11.2026):** `RowMarkService` (markRow + addRow + validateMembership + updateProgress) + `NextRowService` (markNextRow + selectNextRow + pickNextRow + pickFromNonEmpty + findAndAdvance + markSelectedRow). Композиция: `NextRowService` инжектит `RowMarkService` (DRY, markSelectedRow делегирует).
**Факт:** `refactor(domain): split ProgressService into RowMarkService + NextRowService (P1-B)`.

### P1-C. `TimerViewModel` — 6 deps + `@Suppress` — **закрыт 02.11.2026**

**Файл:** `presentation/feature_timer/TimerViewModel.kt`
**Факт:** `timerManager`, `saveSessionService`, `getAllProjectsService`, `markNextRowUseCase`, `permissionChecker`, `savedStateHandle` — **6 параметров** (равенство threshold=6 → detekt валит без `@Suppress`).
**Проблема:** God Object. `@Suppress` — маркер долга.
**Решение:** `RowProgressCoordinator` (markNext + state), `TimerSessionCoordinator` (start/pause/reset/save). VM → 3 deps.
**Оценка:** ~2 ч. Коммит `refactor(presentation): decompose TimerViewModel`.

### P1-D. `Session` VO не мигрированы — **закрыт 02.11.2026**

**Файл:** `domain/entity/Session.kt`
**Факт:** `id: Long`, `projectId: Long`, `rowCount: Int` — примитивы. VO (`SessionId`, `ProjectId`, `RowCount`) **существуют**, но не используются в `Session`.
**Проблема:** primitive obsession, P2 аудита Grok #9.
**Решение (закрыто 02.11.2026):** `SessionId` VO создан. `Session.id: SessionId`, `projectId: ProjectId`, `rowCount: RowCount`. `SaveSessionParams` — VO. `SessionRepository` на границе VO, `SessionRepositoryImpl` разворачивает в примитивы (`.value`). `DtoMappers` обновлён (`Owner-approved §1.1`). Миграции не нужны.
**Факт:** `refactor(domain): Session — VO migration (P1-D)`.

### P1-E. `Project.id: Long` — не VO — **закрыт 02.11.2026**

**Факт:** `refactor(domain): Project — VO migration (P1-E)`.

**Файл:** `domain/entity/Project.kt`
**Факт:** `id: Long = 0L` при существующем `ProjectId` VO. `GetProjectByIdService` / `DeleteProjectService` принимают `Long`.
**Проблема:** P2 аудита Grok #10. Асимметрия: `Pattern.id: PatternId`, `Project.id: Long`.
**Решение:** `Project.id: ProjectId`, `ProjectRepository.getProjectById(id: ProjectId)`, `DeleteProjectService(ProjectId)`. Миграции **не нужны**.
**Оценка:** ~1 ч. Коммит `refactor(domain): Project.id — ProjectId VO`.

### P2 после 7f-D

- [x] **P2-1:** `Symbol.ownerPatternId: Long?` → `PatternId?` — **закрыт 02.11.2026**. **Факт:** `refactor(domain): Symbol.ownerPatternId — VO migration (P2-1)`.
- [x] **P2-4:** D-инвариант (`stitch > 0 → row IS NOT NULL`) — **закрыт 04.11.2026**. Domain-проверка в `Project.markRow` (`RowMarkService` не тронут — пишет `ZERO`, проверка там недостижима, §11.7). Defense-in-depth к триггеру v8. **Факт:** `refactor(domain): D-инвариант в Project.markRow (P2-4)`.
- [x] **P2-5:** `PatternRepositoryImpl.TEMP_ROW_NUMBER = 999_999` → `Int.MAX_VALUE` — **закрыт 02.11.2026**. Файл — `PatternRepositoryImpl.kt:398`, не `UpdatePatternRowNumberService` (неточность §24). **Факт:** `refactor(data): TEMP_ROW_NUMBER = Int.MAX_VALUE (P2-5)`.
- **P2-7:** Большие коммиты (`84b3b65` 969 additions, `158add4` 672, `ab76eb6` 736) — нарушение атомарности. Правило: ≤300 строк. Процессный урок.

**Итого P1-долг:** ~10 ч, 5-6 атомарных коммитов. **Не новая фича.**


## Per-stitch tracking (P3) — закрыт 08.11.2026

- [x] **P3 (TD-PT-3, feature):** Глобальный счётчик петель на экране таймера. `UpdateStitchUseCase` (D-инвариант: `currentPatternRowId != null` → иначе `ValidationError`), `ProjectProgressCoordinator.updateStitch`, `TimerContract.currentStitch` + `IncrementStitch`/`DecrementStitch`, UI блок `+1/-1` в `TimerScreen` (только при `currentPatternRowId != null`). Запись через `ProjectRepository.updateProjectProgress(..., currentStitch)`. Триггер D (`stitch > 0 → row IS NOT NULL`) уже в v8. **Факт:** `feat(timer): global stitch counter (P3 per-stitch tracking)`.

## 3NF — отказ от денормализации currentRowNumber (P2) — закрыт 09.11.2026

- [x] **P2 (DB/DDD, 3NF):** `projects.current_row_number` удалена (миграция v9→v10). Поле было транзитивной зависимостью: `currentRowNumber` derived от `current_pattern_row_id` через `pattern_rows.row_number`. §11.2 считал это «примером правильной денормализации» — ошибка (профилирования не было, единственный потребитель — badge). Теперь: `LEFT JOIN pattern_rows` в `ProjectDao.getAllProjectsFlow`/`getAllProjects`/`getProjectById`, маппер `ProjectWithCurrentRowNumber.toDomain`, `Project.currentRowNumber` остаётся в Domain как derived (UI не меняется). Триггеры B и C сняты (инвариант не существует физически), A и D сохранены. `onCreate` переведён на `CREATE_PROJECTS_V10`. Это открывает D&D-reorder: bulk-shift `pattern_rows.row_number` без второго writer-а на `projects`. Факт: `refactor(data): 3NF — drop current_row_number (P2)`.

## Backup DTO 1.3 — currentRowNumber удалён (P2) — закрыт 10.11.2026

- [x] **P2 (Backup/3NF):** `ProjectDto.currentRowNumber` удалён (формат бэкапа 1.2 → 1.3). Поле было денормализацией, устранённой в v10 (`f989e0a`). При импорте 1.2 и старше Gson игнорирует `"currentRowNumber"`, `DtoMappers.ProjectDto.toDomain` выставляет `currentRowNumber = RowCount.ZERO`; реальное значение восстанавливается через `LEFT JOIN pattern_rows` при первом чтении из БД. `DataImporterImpl.withoutPatternLink()` упрощён (`currentRowNumber = 0` убран, поле исчезло). Тесты: `DtoMappersTest.currentVersion_isOneThree`, `DataExporterImplTest.exportToWritesJSONVersion13WithProjectsSessionsAndPatternTables`, `DataImporterImplTest` (импорт 1.2 с `"currentRowNumber": 4` → 0, обратная совместимость). Факт: `feat(backup): DTO 1.3 — drop currentRowNumber (P2)`.

## Аудит архитектуры (10.11.2026)

- [x] **P1 (Clean Arch):** Парсинг текста исключения в `DataImporterImpl`. **Закрыто 10.11.2026.** Заменено на типизированное `InvalidBackupOperationsException` в Data-слое (`BackupRepositoryImpl`). `DataImporterImpl` ловит его без парсинга строк.
- [x] **P1 (i18n):** Хардкод русского текста в `AppError` — **закрыт 11.11.2026**. Введён `ErrorCode` (enum) в `AppError`. Domain/Data возвращают только код, технические детали — в `details`. В Presentation добавлен `ErrorMapper` (`ErrorCode` → `@StringRes`). Русские тексты удалены из Domain/Data, все строки для UI — в `strings.xml`. Тесты обновлены на проверку `error.code`.
- [ ] **P1 (SRP):** `AppDatabase` — God Class (644 строки). Содержит `@Database`, все 10 миграций, `DATABASE_CALLBACK`, DDL-триггеры и сидеры. Вынести миграции в `AppDatabaseMigrations`, callback в `DatabaseCallback`, триггеры в `DatabaseTriggers` (закрывается в P1-7).
- [x] **P2 (DDD/VO):** `BackupRepository` использует `Pair<Long, T>` — **закрыт 11.11.2026**. Созданы `PatternRowWithId(patternId: PatternId, row: PatternRow)` и `PatternRepeatWithId(patternId: PatternId, repeat: PatternRepeat)`. `BackupRepository` (и `ExportContext`) переведены на эти типы. Обновлены `BackupRepositoryImpl`, `DataExporterImpl`, `DataImporterImpl`.
- [ ] **P2 (DIP):** Gson внутри `DataImporterImpl`/`DataExporterImpl`. Application-слой знает про инфраструктуру парсинга JSON. Вынести в интерфейс `BackupParser` (Data), чтобы можно было заменить Gson на `kotlinx.serialization` без правок Application.
- [ ] **P3 (Kotlin):** `inner class SnapshotContext` в `BackupRepositoryImpl`. Неявная ссылка на внешний класс (потенциальная утечка, если передать наружу). Сделать `private class` и передавать `database` через конструктор.

## Глобальные выводы Большого код-ревью (10.11.2026)

### DDD / Domain
- [ ] **P2 (DDD):** `Pattern` — God Aggregate (13+ полей). Разделить на `PatternMetadata`, `PatternCalibration`, `PatternContent`.
- [ ] **P2 (DDD):** `Project` — God Aggregate. Смешаны прогресс, метаданные и sync. Кандидат на разделение.
- [ ] **P3 (DDD):** `PatternRow` без `patternId` — утечка границ агрегата в Data-слой (`getOperationsForRow` требует оба id).

### Presentation / Android
- [ ] **P2 (SRP):** `PatternEditorViewModel` — God State (15+ полей), `@Suppress("LargeClass")`, `TODO(P2)` без issue.
- [ ] **P2 (Bug):** `TimerViewModel.combine` — race condition: чтение `_state.value` внутри `combine` может терять обновления.
- [ ] **P2 (Compose):** `PatternCanvas` — конфликт жестов (`pointerInput` × 3: tap vs pan vs long-press). Нужен `awaitEachGesture` + `touchSlop`.
- [ ] **P2 (QA):** Нет Compose-тестов (`createComposeRule`). UI не покрыт.

### Стратегия / CTO / PM / SMM
- [ ] **P1 (Strategy):** Scope Creep. Pattern Editor (v1.1) вытеснил Sync (v1.0). Нужен пересмотр приоритетов.
- [ ] **P1 (SMM):** Package name `com.knittrac.app` ≠ бренд `KnitTrack`. Блокер для Google Play (переименование до релиза).
- [ ] **P2 (CTO):** Нет Crashlytics / remote-сбора крэшей.
- [ ] **P2 (CTO):** Нет benchmark-тестов (производительность JOIN на Canvas не измерена).
- [ ] **P2 (SMM):** Нет скриншотов и описания для Google Play.
- [ ] **P2 (PM):** Нет Acceptance Criteria для D&D-reorder рядов.
- [ ] **P3 (PM):** Нет метрик успеха (DAU/MAU, Retention).

### QA
- [ ] **P2 (QA):** Нет теста на полный путь миграций (v5 → v10).
- [ ] **P2 (QA):** `ProjectRepositoryImplRoomTest` неполный: нет тестов на `getAllProjects` (без Flow) и `updateProjectProgress` с `rowId = null`.

## Архитектурные рекомендации (08.11.2026)

- [ ] **P2 (SOLID/OCP):** Нарушение OCP в `Pattern.getDirectionForRow`. `when` по enum `PatternDirection`. При добавлении нового направления (например, по спирали) придется менять код агрегата. Заменить на sealed class с полиморфным методом (паттерн Стратегия).
- [ ] **P2 (Clean Arch):** `Result<T>` без комбинаторов (`map`, `fold`, `onError`). Приводит к дублированию `when (result) { is Success -> ... }`. Добавить комбинаторы и отрефакторить UseCase/ViewModel.
- [ ] **P3 (QA):** Отсутствие инфраструктуры Compose-тестов (`createComposeRule`). UI-компоненты (ColorPicker, диалоги, Long-press) не покрыты тестами. Настроить `androidx.compose.ui:ui-test-junit4` + Robolectric.

## Открытые P3 после сессии 07.11.2026

- [ ] **P3 (LIN/DBA):** Дублирование SQL-триггеров `trg_repeat_code_check_*` в `MIGRATION_8_9` и `DATABASE_CALLBACK.onCreate`. При изменении правила — править оба места. См. `rows-and-patterns.md §6.1.1`.
- [ ] **P3 (LIN/DBA):** Оптимизация UPDATE-триггера. Добавить `WHEN OLD.repeat_code != NEW.repeat_code` в `trg_repeat_code_check_update`, чтобы не делать 3-way JOIN при каждом UPDATE операции (даже если цвет/символ не меняются).
- [x] **P3 (AND):** Защита от забытых миграций в `DatabaseModule` — **закрыт 07.11.2026**. Интеграционный тест `DatabaseModuleMigrationsTest.provideAppDatabaseMigratesFromPreviousVersion`: создаёт БД на `CURRENT-1`, открывает через production-путь `DatabaseModule.provideAppDatabase`, проверяет `version == CURRENT`. Reflection на `@Database.version` недоступен (аннотация `CLASS` retention), поэтому `CURRENT_DB_VERSION` hardcoded в тесте — при bump version обновлять константу.
- [x] **P3 (QA):** Тест на `Channel` без потери событий — **закрыт 08.11.2026**. 3 теста в `SettingsViewModelTest`: (а) guard `isExporting` не запускает второй `exportDataService`, (б) два последовательных `RequestExport` дают 2 `ExportSuccess` (Channel не теряет повторный эмит), (в) `RequestExport` + `ImportData` параллельно — оба эффекта доходят. Паттерн: `CompletableDeferred` для suspend `coAnswers` + `effect.take(N).toList()`.
- [x] **P3 (UX):** Контекстная логика показа туториала — **закрыт 08.11.2026**. Решение: не угадывать «контекст» (по отзывам на KnitCompanion — пользователи не хотят автоматики, хотят доступ к помощи в любой момент). Иконка `?` (`Icons.Default.Help`) в `TopAppBar` `PatternEditorScreen` → `Intent.ShowEditTutorial`. `State.tutorialManuallyRequested` защищает ручной показ от затирания следующим эмитом `editTutorialShownFlow`. `DismissEditTutorial` сбрасывает флаг + `setEditTutorialShown(true)`. 2 теста: `showEditTutorialManually_overridesStoredFlag`, `dismissEditTutorial_resetsManualFlag`.
- [ ] **P3 (UX):** Drag-and-drop для перемещения ряда (сейчас только ввод числа в `RowEditorSheet`).
- [ ] **P3 (Accessibility):** Иконка `List` в `TopAppBar` `PatternEditorScreen` как альтернатива long-press (WCAG 2.1, 2.5.1 Pointer Gestures).
- [ ] **P3 (Performance):** Lazy-load операций на Canvas. Сейчас `getPatternById` грузит все операции всех рядов.

