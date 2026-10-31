# Ряды и схемы в KnitTrack

## 1. Назначение

Документ фиксирует семантику понятий «ряд» и «схема». При изменении кода
документ обновляется в том же коммите (§7.4 `.cursorrules`).

## 2. Ключевое определение

`PatternRow.rowNumber: RowNumber` — **идентификатор позиции в definition
схемы** (1-based). Уникальность — composite UNIQUE `(pattern_id, row_number)`
в БД (миграция 4→5, `index_pattern_rows_pattern_id_row_number`) и инвариант
`Pattern.init` в Domain (`rows.map { it.rowNumber }.distinct().size == rows.size`).
`PatternRow` не содержит `patternId` в Domain — владеет `Pattern` aggregate.

Физическое вязание может проходить один `PatternRow` несколько раз
(укороченные ряды, повторы раппортов). Это моделируется через `PatternRepeat`
(справочник раппортов) и `PatternOperation` (операции внутри ряда), а не
дублированием `PatternRow`.

## 3. Техники

### 3.1. Спицы, прямое (поворотное)

- Нечётные (лицевые) ряды: справа налево. Чётные (изнаночные): слева направо.
- Чётные ряды часто не рисуют — вяжут «по рисунку».
- Кромочные петли — вне узора.
- `PatternDirection.ALTERNATING` — модель для этой техники: чётные индексы
  LTR, нечётные RTL (см. §4).

### 3.2. Спицы, круговое

- Все ряды читаются справа налево, только лицевая сторона.
- Кромочных нет. Маркер — начало ряда.
- «По спирали» — частный случай: `PatternRow` не имеет физической границы,
  `rowNumber` — номер витка в definition.
- `PatternDirection` = `LTR` (правши) / `RTL` (левши). **Не** `ALTERNATING`.

### 3.3. Крючок, поворотное

- Чередование направления. Петли подъёма (1–3 ВП).
- `PatternDirection.ALTERNATING` — модель.

### 3.4. Крючок, круговое (с соединением)

- Соединительный столбик в конце. Все ряды в одну сторону.
- `PatternDirection` = `LTR` / `RTL` (не `ALTERNATING`).

### 3.5. Крючок, спираль

- Без соединительного. Маркер — граница. `rowNumber` — номер витка.
- `PatternDirection` = `LTR` / `RTL`.

### 3.6. Бисер

- Мозаика (пейот): сдвиг на полбисерины, чётные/нечётные смещены.
- Кирпичный стежок: подвешивание за нитки, базовый ряд.
- Крестик: пары бисерин.
- Подтехники внутри `Technique.BEADING`; не значения enum. При потребности
  различения — `subTechnique` (P3).

## 4. Направление чтения (`PatternDirection`)

`PatternDirection { LTR, RTL, ALTERNATING }` — **три значения**, каждое
означает правило чтения операций, а не физическое вращение полотна:

- **LTR** — все ряды читаются слева направо. Круговое вязание (правши),
  бисерные техники, любые техники без поворотов.
- **RTL** — все ряды читаются справа налево. Круговое (левши), зеркальные
  схемы.
- **ALTERNATING** — чётность 0-based индекса ряда: чётные индексы → LTR,
  нечётные → RTL. Поворотные техники (спицы прямое, крючок поворотное).

### 4.1. Круговое/спираль — не отдельное направление

Отдельного `PatternDirection.CIRCULAR` **нет** и не будет. Круговое вязание
и спираль — это свойство `Technique` + отсутствие поворотов:

- `Technique` = `KNITTING` или `CROCHET`.
- `Pattern.direction` = `LTR` (правши) / `RTL` (левши), **не** `ALTERNATING`.
- `Pattern.getDirectionForRow(row)` (контракт 7b-fix) для `LTR`/`RTL`
  возвращает базовое направление для всех рядов; для `ALTERNATING` —
  инвертирует по чётности индекса.

Различие «круговое vs поворотное» определяется `Technique` + выбором
`direction`, а не отдельным значением enum.

## 5. Модель данных (упрощённо; полная — `Pattern.kt`)

```kotlin
enum class Technique { KNITTING, CROCHET, BEADING }
enum class PatternDirection { LTR, RTL, ALTERNATING }

data class Pattern(
    val id: PatternId,
    val name: PatternName,
    val technique: Technique,
    val direction: PatternDirection,
    val isReversible: Boolean,
    val backgroundImageUri: String? = null,
    val imageOffsetX: Float = 0f,
    val imageOffsetY: Float = 0f,
    val imageScale: Float = 1f,
    val legend: Set<Symbol> = emptySet(),
    val rows: List<PatternRow> = emptyList(),
    val repeats: List<PatternRepeat> = emptyList()
)

data class PatternRow(
    val id: PatternRowId,
    val rowNumber: RowNumber,
    val operations: List<PatternOperation> = emptyList()
)

data class PatternRepeat(
    val code: String,
    val stitchCount: StitchCount,
    val rowCount: RowCount
)
```

**Инвариант (`Pattern.init`):** `rows` упорядочены по `rowNumber` по
возрастанию; `rows.map { it.id }.distinct().size == rows.size`;
`repeats.map { it.code }.distinct().size == repeats.size`.

`Project.currentRowNumber: RowCount` — денормализация для UI (badge без JOIN
на `pattern_rows`). Constraint: `currentRowNumber = pattern_rows.row_number`
для `currentPatternRowId`.

## 6. Инварианты и защита

| # | Инвариант | Domain | Trigger | Presentation |
|---|---|---|---|---|
| A | `row IS NOT NULL → row принадлежит pattern` | `ProgressService.markRow` | `trg_progress_check` | — |
| B | `row IS NOT NULL → currentRowNumber = row.rowNumber` | — (денорм.) | `trg_progress_check` | UI |
| C | `row IS NULL → currentRowNumber = 0` | — | `trg_progress_check` | UI |

D-инвариант (`stitch > 0 → row IS NOT NULL`) — **P3**, включается вместе с
per-stitch tracking (7f-B). В текущем коде `ProgressService.updateProgress`
всегда пишет `currentStitch = StitchCount.ZERO`, `TimerViewModel` не пишет
`current_stitch` в БД — триггер D был бы мёртвым кодом.

### 6.1. Триггер `trg_progress_check`

```sql
CREATE TRIGGER trg_progress_check
BEFORE INSERT OR UPDATE OF
    current_pattern_row_id, current_row_number, current_stitch, pattern_id
ON projects
FOR EACH ROW
BEGIN
    SELECT RAISE(ABORT, 'Ряд не принадлежит схеме проекта')
    WHERE NEW.current_pattern_row_id IS NOT NULL
      AND NOT EXISTS (
        SELECT 1 FROM pattern_rows
        WHERE id = NEW.current_pattern_row_id
          AND pattern_id IS NEW.pattern_id
      );

    SELECT RAISE(ABORT, 'Номер ряда не соответствует денормализации')
    WHERE NEW.current_pattern_row_id IS NOT NULL
      AND NOT EXISTS (
        SELECT 1 FROM pattern_rows
        WHERE id = NEW.current_pattern_row_id
          AND row_number = NEW.current_row_number
      );

    SELECT RAISE(ABORT, 'Прогресс без ряда должен иметь номер 0')
    WHERE NEW.current_pattern_row_id IS NULL
      AND NEW.current_row_number != 0;
END;
```

**Gotchas SQLite (зафиксировать явно):**

- `UPDATE OF col1, col2, ...` фильтрует только UPDATE-события. На INSERT
  триггер срабатывает всегда, независимо от `OF`-списка.
- `FOR EACH ROW` в SQLite — no-op (синтаксис валиден, семантика row-based
  по умолчанию). Отличие от PostgreSQL.
- `IS` (не `=`) в подзапросе A — NULL-safe. При `NEW.pattern_id IS NULL`
  подзапрос даёт 0 строк (FK `pattern_rows.pattern_id` NOT NULL), триггер
  корректно отбивает.

### 6.2. Порядок `MIGRATION_5_6`

1. Санитизация A+B: `UPDATE projects SET current_pattern_row_id = NULL,
   current_row_number = 0 WHERE current_pattern_row_id IS NOT NULL AND
   (pattern_id IS NULL OR NOT EXISTS (SELECT 1 FROM pattern_rows WHERE
   id = current_pattern_row_id AND pattern_id = projects.pattern_id))`.
2. Санитизация B-fix: `UPDATE projects SET current_row_number = (SELECT
   row_number FROM pattern_rows WHERE id = current_pattern_row_id) WHERE
   current_pattern_row_id IS NOT NULL AND EXISTS (SELECT 1 FROM pattern_rows
   WHERE id = current_pattern_row_id AND pattern_id = projects.pattern_id)
   AND current_row_number != (SELECT row_number FROM pattern_rows WHERE
   id = current_pattern_row_id)`. Не теряет row.
3. Санитизация C: `UPDATE projects SET current_row_number = 0 WHERE
   current_pattern_row_id IS NULL AND current_row_number != 0`.
4. `CREATE TRIGGER trg_progress_check ...`.
5. Room при сборке генерирует `schemas/com.knittrac.app.data.local.AppDatabase/6.json`.

**Порядок обязателен:** триггер после всех UPDATE. Иначе санитизация упадёт
на RAISE.

`schemas/6.json` идентичен `5.json` — Room не экспортирует триггеры в schema
JSON. Тест проверяет триггер через `SELECT name FROM sqlite_master WHERE
type='trigger' AND name='trg_progress_check'`, не через schema validation.

### 6.3. INSERT-путь

`ProjectEntity` defaults: `patternId = null, currentRowNumber = 0,
currentPatternRowId = null, currentStitch = 0`. `ProjectDao.insertProject`
(`@Insert(onConflict = REPLACE)`) пишет эти дефолты. Триггер A/B/C не
срабатывают. Raw INSERT вне Room — только `INSERT INTO projects_new` в
`MIGRATION_2_3` и `MIGRATION_4_5` (новые таблицы, триггер ещё не создан).

### 6.4. Смена `pattern_id`

Если `pattern_id` меняется без сброса `current_pattern_row_id` /
`current_row_number` → Trigger A отбивает (row старой схемы не принадлежит
новой). Ответственность UI/Repository: сбросить progress перед сменой.

### 6.5. `WHEN`-оптимизация (P3)

`WHEN NEW.current_row_number != OLD.current_row_number OR
NEW.current_pattern_row_id != OLD.current_pattern_row_id` пропускает
подзапросы при неизменных progress-полях. В SQLite `WHEN` применяется
до `BEGIN` — экономит и подзапросы, и `UPDATE`. Не «микрооптимизация»,
но P3: на текущих объёмах не критично.

## 7. Что не покрыто (YAGNI)

- **D: `stitch > 0 → row IS NOT NULL`** — P3. Включить вместе с per-stitch
  tracking (7f-B). В текущем коде `current_stitch` всегда 0.
- **«Длина ряда = current_stitch»** — дорогой JOIN `pattern_operations` +
  `symbols` на каждый UPDATE `projects`. Domain (`StitchCount` +
  `ProgressService`) ограничивает.
- **`pattern_id` в `pattern_operations`** — отдельный триггер
  `trg_check_symbol_owner` (ТЗ §3.1, строка 36) — отдельная задача.
- **Укороченные ряды как отдельная сущность** — `PatternRepeat` +
  `PatternOperation` покрывают.
- **Импорт схем** — будущая фича; `rowNumber` остаётся идентификатором.

## 8. Ссылки

- ТЗ v8.0 §3.1 — инварианты прогресса.
- `.cursorrules §1 п.2` — VO вместо примитивов.
- `HANDOVER.md §24` — статус проекта.
- `docs/architecture/vo-pattern.md` — VO-паттерн (создан ранее).
- Профильные руководства по чтению схем (DROPS, DMC и др.) — общая практика;
  в KnitTrack семантика `rowNumber` как идентификатора definition-позиции
  согласуется с ней.
