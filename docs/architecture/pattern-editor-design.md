# Pattern Editor — дизайн (7f-B)

## 1. Назначение и границы

Pattern Editor — экран редактирования схемы вязания. Позволяет просматривать
схему (Canvas с zoom/pan), выбирать ряд/колонку/символ и добавлять операции
(петли) в ряды. Схема может быть привязанной к проекту или выбранной из всех
схем в БД.

### Входит в 7f-B (5 коммитов)

- Два новых метода `PatternRepository` (`getOperationsForRow`, `getAllPatternSummaries`).
- `PatternSummary` (Domain, ISP: id + name).
- `Project.canOpenPatternEditor()` (Domain-extension).
- `GetPatternByIdService`, `GetAllPatternSummariesService`.
- `SymbolLocalizer` (Presentation).
- `PatternEditorContract` + `PatternEditorViewModel`.
- `PatternEditorScreen` с Canvas (zoom/pan).
- Dropdown-селектор схем в `TopAppBar`.
- Icon-toggle режима добавления операции (sticky / sheet).
- Два входа: `TimerScreen`, `ProjectsListScreen`.
- Навигация `pattern_editor/{patternId}`.

### НЕ входит (YAGNI / P3 / 7f-C)

- Создание схемы — **7f-C** (отдельная фича). В 7f-B при `patternId == null`
  иконка Edit и LongPress-пункт скрыты; создание схемы недоступно — это
  осознанное ограничение MVP, не баг.
- Удаление операции — **P3** (`DeleteOperationUseCase`).
- Редактирование существующей операции (изменить символ/span) — **P3**.
- Undo/redo — **P3**.
- Сохранение zoom/pan между сессиями (только process death, не app restart) — **P3**.
- Кнопки `+`/`-` и «Zoom to canvas» — **P3**.
- URL-based switching схем (сейчас — internal state, URL не меняется) — **P3**.
- Мультивыбор операций — **P3**.

## 2. Модель данных

### 2.1. PatternSummary (Domain, новый)

Файл `domain/model/PatternSummary.kt`:

```kotlin
data class PatternSummary(
    val id: PatternId,
    val name: PatternName
)
```

Назначение: селектор схем. ISP: только `id + name`. Полный `Pattern` (с
`legend`, `repeats`) загружается только для выбранной схемы.

### 2.2. PatternRepository +2 метода

```kotlin
interface PatternRepository {
    suspend fun getPatternById(id: PatternId): Result<Pattern>  // есть

    suspend fun getOperationsForRow(
        patternId: PatternId,
        rowId: PatternRowId
    ): Result<List<PatternOperation>>

    fun getAllPatternSummaries(): Flow<List<PatternSummary>>
}
```

- `getOperationsForRow(patternId, rowId)` — для Canvas (полные операции
  с `symbol`/`span`/`color`). `patternId` нужен для загрузки legend схемы:
  `pattern_operations.symbol_code` — FK на `symbols.code`, без legend не
  смапить `symbol_code` → `Symbol`.
- `getAllPatternSummaries` — Flow, симметрично `ProjectRepository.getAllProjectsFlow()`.

### 2.3. Project.canOpenPatternEditor (Domain-extension)

```kotlin
// domain/entity/Project.kt (extension, рядом с markRow)
fun Project.canOpenPatternEditor(): Boolean = patternId != null
```

Единственная точка проверки для двух входов. При расширении условий
(например, `patternId != null && ...`) — править в одном месте.

## 3. Application

### 3.1. GetPatternByIdService (новый, BaseUseCase<PatternId, Pattern>)

```kotlin
class GetPatternByIdService @Inject constructor(
    private val patternRepository: PatternRepository,
    @param:IoDispatcher private val dispatcher: CoroutineDispatcher
) : BaseUseCase<PatternId, Pattern>(dispatcher) {
    override suspend fun execute(params: PatternId): Result<Pattern> =
        patternRepository.getPatternById(params)
}
```

Параметр — **`PatternId`** (VO, D1), не `Long`. Эталон — `GetProjectByIdService`.

### 3.2. GetAllPatternSummariesService (новый, BaseFlowUseCase<Unit, List<PatternSummary>>)

```kotlin
class GetAllPatternSummariesService @Inject constructor(
    private val patternRepository: PatternRepository,
    @param:IoDispatcher private val dispatcher: CoroutineDispatcher
) : BaseFlowUseCase<Unit, List<PatternSummary>>(dispatcher) {
    override fun execute(params: Unit): Flow<List<PatternSummary>> =
        patternRepository.getAllPatternSummaries()
}
```

Эталон — `GetAllProjectsService`. `BaseFlowUseCase` оборачивает в `Flow<Result<...>>`
(`map` + `catch` + `flowOn(dispatcher)`).

### 3.3. AddPatternOperationUseCase (существует, 7e)

```kotlin
class AddPatternOperationUseCase @Inject constructor(
    private val service: AddPatternOperationService,
    @param:IoDispatcher private val dispatcher: CoroutineDispatcher
) : BaseUseCase<AddPatternOperationParams, PatternOperation>(dispatcher)
```

Валидация (legend, repeat, overlap) — в `AddPatternOperationService` (Domain).
UseCase — тонкий оркестратор.

## 4. Presentation — PatternEditorContract

Контракт — **отдельный файл** `PatternEditorContract.kt` (конвенция
`TimerContract.kt`, `SettingsContract.kt`; `ProjectsContract` внутри VM —
историческое исключение, унификация — P3).

### 4.1. State

```kotlin
data class State(
    val availablePatterns: List<PatternSummary> = emptyList(),  // селектор
    val currentPatternId: PatternId? = null,                    // выбранная схема
    val pattern: Pattern? = null,                               // загруженная схема
    val selectedRowId: PatternRowId? = null,                    // выбранный ряд
    val selectedColumn: ColumnStart? = null,                    // выбранная колонка
    val selectedSymbolCode: String? = null,                     // выбранный символ
    val isStickyMode: Boolean = true,                           // режим добавления
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,                              // guard от double-tap
    val errorRes: Int? = null                                   // @StringRes
)
```

### 4.2. Intent

```kotlin
sealed class Intent {
    data class LoadPattern(val patternId: PatternId) : Intent()
    data class SelectPattern(val patternId: PatternId) : Intent()
    data class SelectRow(val rowId: PatternRowId) : Intent()
    data class SelectColumn(val columnStart: ColumnStart) : Intent()
    data class SelectSymbol(val symbolCode: String) : Intent()
    object ToggleMode : Intent()
    data class AddOperation(
        val rowId: PatternRowId,
        val columnStart: ColumnStart
    ) : Intent()
    object Retry : Intent()
}
```

### 4.3. Effect

```kotlin
sealed class Effect {
    data class ShowError(@StringRes val messageResId: Int) : Effect()
    object OperationAdded : Effect()
}
```

## 5. Presentation — PatternEditorViewModel

### 5.1. Зависимости (5 — порог 6, `@Suppress` не нужен)

```kotlin
@HiltViewModel
class PatternEditorViewModel @Inject constructor(
    private val getPatternByIdService: GetPatternByIdService,
    private val getAllPatternSummariesService: GetAllPatternSummariesService,
    private val addPatternOperationUseCase: AddPatternOperationUseCase,
    private val symbolLocalizer: SymbolLocalizer,
    private val savedStateHandle: SavedStateHandle
) : ViewModel()
```

### 5.2. State management — SavedStateHandle

**Переживают process death** (через `SavedStateHandle`):
- `patternId` (Long)
- `selectedRowId` (Long?)
- `selectedColumn` (Int?)
- `selectedSymbolCode` (String?)
- `isStickyMode` (Boolean)

**НЕ в VM** — `zoom`, `pan` в Composable через `rememberSaveable` (UI-only).

### 5.3. Логика

- `init`: `getAllPatternSummariesService(Unit).collect` → `availablePatterns`.
  Если `savedStateHandle.get<Long>("patternId") != null` → `LoadPattern(PatternId)`.
- `LoadPattern(id)` / `SelectPattern(id)`: `getPatternByIdService(id)` →
  Success: `state.pattern`, `state.currentPatternId`; Error: `Effect.ShowError`.
- `SelectRow` / `SelectColumn` / `SelectSymbol`: обновление `state` + `savedStateHandle`.
- `ToggleMode`: `state.isStickyMode = !isStickyMode` + `savedStateHandle`.
- `AddOperation(rowId, columnStart)`: guard `isSaving`; вызов
  `addPatternOperationUseCase(AddPatternOperationParams(...))`;
  Success → `Effect.OperationAdded(operation)` + **reload** `pattern`
  (`getPatternByIdService(currentPatternId)`) — VM единый source of truth,
  без мутации Domain в Presentation; Error → `Effect.ShowError`.

### 5.4. Guard от double-tap (`isSaving`)

По эталону `TimerViewModel.handleMarkNextPatternRow` (7g-B): флаг ставится
синхронно, снимается в `finally`.

## 6. Presentation — PatternEditorScreen

### 6.1. Структура

```
Scaffold
├── TopAppBar
│   ├── title: Dropdown селектор схем (текущая → меню всех)
│   └── actions:
│       └── IconButton (TouchApp / AdsClick) — sticky/sheet
├── body:
│   ├── Row picker (LazyRow, номера рядов)
│   ├── Canvas (zoom/pan, сетка + операции)
│   └── Symbol picker (LazyVerticalGrid, если sticky mode)
```

### 6.2. Canvas — zoom/pan

```kotlin
var scale by rememberSaveable { mutableStateOf(1f) }
var offset by rememberSaveable { mutableStateOf(Offset.Zero) }

Canvas(
    modifier = Modifier
        .fillMaxSize()
        .pointerInput(Unit) {
            detectTransformGestures { _, pan, zoom, _ ->
                scale = (scale * zoom).coerceIn(0.5f, 4f)
                offset += pan
            }
        }
) {
    // draw grid, operations с учётом scale и offset
}
```

Стандарт индустрии (KnitCompanion, Pattern Keeper, Stitchart, İlmek, YarnFit —
все имеют pinch-zoom). Полировка (кнопки `+`/`-`, «Zoom to canvas») — **P3**.

### 6.3. Ячейка — tap detection

`detectTapGestures` на Canvas — вычислить `(row, column)` по координатам с учётом
`scale`/`offset`. Поведение:
- **Sticky**: если `selectedSymbolCode != null` → `AddOperation(row, column)`.
- **Sheet**: открыть bottom sheet с Symbol picker → после выбора → `AddOperation`.

### 6.4. Symbol picker (sticky mode)

`LazyVerticalGrid` с символами схемы. Клик → `SelectSymbol(code)`. Выбранный
подсвечен.

### 6.5. SymbolLocalizer

```kotlin
@Singleton
class SymbolLocalizer @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun localize(symbol: Symbol): String = symbol.textGlyph
}
```

MVP: `textGlyph` (уже в `Symbol`). Расширение (локализация `displayName`) — **P3**.
Эталон — `CategoryLocalizer`.

## 7. Навигация

### 7.1. Screen

```kotlin
sealed class Screen(val route: String) {
    // существующие
    object PatternEditor : Screen("pattern_editor")
}
```

### 7.2. AppNavHost

```kotlin
composable(
    route = "${Screen.PatternEditor.route}/{patternId}",
    arguments = listOf(navArgument("patternId") { type = NavType.LongType })
) {
    PatternEditorScreen(
        onNavigateBack = { navController.popBackStack() }
    )
}
```

### 7.3. Два входа (G2 — predicate)

**TimerScreen:**
```kotlin
if (project.canOpenPatternEditor()) {
    IconButton(onClick = { onNavigateToPatternEditor(project.patternId!!.value) }) {
        Icon(Icons.Default.Edit, contentDescription = "Редактор схемы")
    }
}
```

**ProjectsListScreen:** LongPress на карточке → `showMenu("Открыть схему")` →
`onNavigateToPatternEditor(project.patternId!!.value)`, при `project.canOpenPatternEditor()`.

### 7.4. Back stack (G4)

`popBackStack()` — возврат к предыдущему экрану в стеке. `origin`-параметр **не
нужен**: если пришли из TimerScreen — вернёт туда, из ProjectsList — туда. Схема
проекта не меняется при редактировании другой схемы в редакторе (URL не меняется,
internal state).

## 8. Два режима добавления операции

### 8.1. Sticky (по умолчанию)

1. Тап на символ в палитре → `selectedSymbolCode`.
2. Тап по ячейке на Canvas → `AddOperation(row, column)`.
3. Символ не сбрасывается после добавления — быстрое серийное заполнение.

### 8.2. Sheet

1. Тап по пустой ячейке на Canvas → открывается bottom sheet.
2. В sheet — Symbol picker (`LazyVerticalGrid`).
3. Выбор символа → `AddOperation(row, column)` → sheet закрывается.

### 8.3. Переключатель

Icon-toggle в `TopAppBar`:
- `Icons.Default.TouchApp` — sticky
- `Icons.Default.AdsClick` — sheet

Состояние — в `SavedStateHandle` (переживает process death). Tooltip — **P3**.

## 9. Тесты

### 9.1. Application (7f-B-1)

- `GetPatternByIdServiceTest` (2: success, error)
- `GetAllPatternSummariesServiceTest` (2: success, error из Flow)

### 9.2. Data (7f-B-1)

- `PatternRepositoryImplTest` +3: `getOperationsForRow` success / notFound /
  sqlException; `getAllPatternSummaries` маппинг
- `PatternMappersTest` +1: `PatternEntity.toSummary`

### 9.3. Presentation (7f-B-2, 7f-B-4)

- `PatternEditorViewModelTest` (≥6):
  - `init_loadsAvailablePatterns`
  - `loadPattern_success_setsPattern`
  - `selectPattern_reloads`
  - `toggleMode_persistsInSavedState`
  - `addOperation_success_emitsEffect`
  - `addOperation_error_emitsShowError`
  - `restore_fromSavedStateHandle`

### 9.4. Screen (7f-B-3)

- `PatternEditorScreenTest` (Robolectric, smoke):
  - `composition_rendersTopBar`
  - `composition_rendersCanvas`

## 10. Этапы (коммиты)

| # | Коммит | Что | Оценка |
|---|---|---|---|
| 1 | `feat(7f-B-1)`: `getOperationsForRow` + `GetPatternByIdService` + `GetAllPatternSummariesService` + `PatternSummary` + `Project.canOpenPatternEditor` + `pattern-editor-design.md` | Domain + Data + Application + docs | 2.5 ч |
| 2 | `feat(7f-B-2)`: `PatternEditorContract` + `PatternEditorViewModel` + `SymbolLocalizer` | Presentation VM | 1.5 ч |
| 3 | `feat(7f-B-3)`: `PatternEditorScreen` + Canvas (zoom/pan) + nav + 2 входа | Presentation UI | 2.5 ч |
| 4 | `feat(7f-B-4)`: sticky + sheet режимы + column picker | Presentation UI + VM | 2 ч |
| 5 | `docs(7f-B-5)`: `TECHDEBT` + `HANDOVER` | docs | 30 мин |

**Итого ~9 часов, 2–3 сессии.**

## 11. Риски

- **R1.** `PatternOperationEntity.toDomain()` — проверить в `PatternOperationMappers.kt`.
- **R2.** `PatternEntity.toSummary()` — новый маппер в `PatternMappers.kt`.
- **R3.** `getAllPatternsFlow` в `PatternRepositoryImpl` — `map` без `flowOn`
  (обёртка в `BaseFlowUseCase`).
- **R4.** `PatternId.createOrDefault(id)` — safe-fallback для id=0 (в БД всегда > 0).
- **R5.** `PatternSummary` — лёгкая модель; расширение полей (`technique`, `direction`) — **P3**.
- **R6.** Zoom/pan — `rememberSaveable` через process death. Дольше (app restart) — **P3**.
- **R7.** `Project.canOpenPatternEditor` — extension в Domain, не в Presentation.
- **R8.** Создание схемы — **7f-C**. В 7f-B при `patternId == null` кнопки скрыты.

## 12. Ссылки

- ТЗ v8.0 §3.1 — инварианты прогресса
- `docs/architecture/rows-and-patterns.md` — семантика рядов
- `.cursorrules §1.2` (VO), §7.4 (docs)
- `HANDOVER.md §3.10` — TimerViewModel (эталон MVI)
