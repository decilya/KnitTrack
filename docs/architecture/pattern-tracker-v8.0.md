# ТЗ v8.0 — KnitTrack: Pattern Tracker

**Дата:** 19.10.2026
**Статус:** Утверждено.
**База:** HEAD `7f2bbb5`, Room v4, 147 тестов (294 прогона).

## 1. Общая информация
**KnitTrack** — Android-приложение для учёта времени, рядов и прогресса вязания. Пакет `com.knittrac.app`.

**Стек:** Kotlin, Compose (Material3), Room, Hilt, Coroutines/Flow, KSP. JDK 17. Gradle 8.7. AGP 8.6.0. MinSDK 26.

**Окружение:** Windows 10 + WSL2. Сборка только через `cmd.exe /c "cd C:\Users\decilya\projects\KnitTrac && gradlew.bat test detekt --console=plain"`. Из WSL `./gradlew` не использовать. Без `| tail`. `cat -n`, не `cat -A`.

**Эмулятор неработоспособен** (`552b2ee`). Instrumented-тесты не исполняются. Robolectric — единственный путь для миграционных тестов.

## 2. Состояние проекта (верифицировано)
- `7f2bbb5` SavedStateHandle для `rowCount` и `projectId` (HEAD).
- `aea1a16` Прогресс в списке проектов (3.1d).
- `f1fb9c4` KOT-1, процессные уроки.
- `ab80214` RU-локализация (56 строк).
- `c3d0c21` Ввод `targetRowCount` (3.1c).
- `8a42566` `totalRows`/`targetRowCount`, миграция 3->4.

## 3. Архитектурные решения

### 3.1. Инварианты (DDD + defense-in-depth)
**Прогресс принадлежит схеме.**
- Domain: `Project.markRow(row: PatternRow): Result<MarkResult>` — без `Set<PatternRowId>`.
- Cross-aggregate validation: `ProgressService.markRow(project, row): Result<MarkResult>`. Загружает `Project` и `PatternRow`, проверяет `row.patternId == project.patternId`. Возвращает `MarkResult { Added, AlreadyExists }`.
- Application: `MarkRowUseCase` и `ImportBackupUseCase` вызывают `ProgressService.markRow`. DRY.
- БД: триггер `trg_progress_check` `BEFORE INSERT OR UPDATE`, `IS NOT` для NULL-safety.

**Символ принадлежит легенде схемы.**
- Domain: `AddPatternOperationService.add(row, col, symbol): Result<PatternOperation>`. Проверяет `symbol in legend`, `overlap`, `repeat_code in pattern.repeats`.
- Overlap: Repository `getOperationsForRow(rowId): List<OperationSpan>`, Service проверяет в памяти (O(n), n ≤ 100).
- БД: триггер `trg_check_symbol_owner` `BEFORE INSERT OR UPDATE`.

### 3.2. Immutability
- `patterns`, `pattern_rows`, `pattern_operations`, `pattern_repeats`, `symbols` — immutable blueprints.
- `projects`, `project_progress` — mutable state.
- `Pattern.fork` — **не метод агрегата**, `ForkPatternService` (Application).
- `project_overrides` и `pattern_sync_metadata` — не создаются в v8.0 миграции (YAGNI).

### 3.3. Слоистость
1. Domain — агрегаты, VO, доменные исключения, Domain Services.
2. Application — Use Cases (оркестрация, без бизнес-логики).
3. Infrastructure — Room, repositories, DTO, importers, file storage, TimeProvider.
4. Presentation — ViewModels, UI Mappers, `RenderModel`, Compose UI.

`RenderRowDto` — Infrastructure (данные из SQL JOIN). `RenderModel` — Presentation (пиксельные координаты). UI Mapper — единственная точка конверсии.

### 3.4. Domain Services vs методы агрегатов
- `Pattern.getDirectionForRow(row: PatternRow): PatternDirection` — метод агрегата. Кэш `Map<PatternRowId, Int>`. `require(rows.isNotEmpty())`. Для `ALTERNATING` чётность по индексу.
- `Project.advanceTo(rowId)`, `applySession(session)`, `attachPattern(patternId, resetProgress)` — методы агрегата.
- `ProgressService.markRow`, `AddPatternOperationService.add` — Domain Services.
- `SaveSessionService`, `MarkRowUseCase`, `AttachPatternService`, `CreatePatternService`, `ForkPatternService`, `IncrementRowService`, `ImportBackupService`, `ExportBackupService` — Application Services.

### 3.5. Value Objects
- `ProjectName`, `RowCount`, `StitchCount`, `Duration`, `PatternName`, `ColumnStart`, `RowNumber`, `Span`.
- `PatternOperation` содержит `overrideSpan: Int?` и `symbol: Symbol`. `effectiveColumnSpan: Int get() = overrideSpan ?: symbol.defaultColumnSpan` — getter.
- `RowCount.plus(Int)` — оператор.
- `Project.currentStitch: StitchCount`, `ProjectEntity.current_stitch: Int`, маппер конвертирует.

### 3.6. Единый стандарт ошибок
- Внутри Domain — `require`/`DomainException`.
- На границе Application — перехват и маппинг в `Result<T>`.
- `ProgressService.markRow` возвращает `Result<MarkResult>`.

### 3.7. 3NF
- `pattern_operations` без `pattern_id`. Простой FK `symbol_code -> symbols(code)`.
- `project_progress` без `pattern_id`.
- `symbols(code PK, owner_pattern_id FK patterns NULL, ...)`.
- Триггеры только на `INSERT OR UPDATE`.

### 3.8. Бисероплетение
**`Technique` += `BEADING`.**
Схемы: `loom work` и `square stitch` — сетка. Остальные (peyote, brick, RAW, herringbone, netting, chevron, circular, triangle, tubular) — 80% не ложатся в сетку.
**Решение:** фон + калибровка + счётчик рядов + счётчик бисерин. `pattern_operations` пуста. Текст-режим disabled. Семантика — v9.0.

## 4. Domain
**Агрегаты:**
- `Pattern` — `id`, `metadata`, `legend: Set<Symbol>`, `rows: List<PatternRow>`. Immutable. `getDirectionForRow(row)`. Кэш `Map<PatternRowId, Int>`.
- `Project` — `id`, `metadata`, `patternId: PatternId?`, `currentRowNumber: Int`, `currentPatternRowId: PatternRowId?`, `currentStitch: StitchCount`, `totalRows: RowCount`, `targetRowCount: RowCount?`, `totalTime: Duration`, `syncStatus`.

**Domain Services:** `ProgressService`, `AddPatternOperationService`.

**Entities:** `PatternRow`, `PatternOperation`, `Symbol`, `PatternRepeat`.

**VO:** `ProjectName`, `RowCount`, `StitchCount`, `Duration`, `PatternName`, `ColumnStart`, `RowNumber`, `Span`.

**Enums:** `Technique` (`KNITTING`, `CROCHET`, `CROSS_STITCH`, `BEADING`), `PatternDirection`, `SymbolCategory`.

## 5. Application
- `SaveSessionService`
- `MarkRowUseCase`
- `AttachPatternService`
- `CreatePatternService`
- `ForkPatternService`
- `IncrementRowService` (два режима: с `pattern_rows` и без)
- `ImportBackupService` (`MutableMap<Long, Long>` для маппинга)
- `ExportBackupService` (DTO 1.1)

## 6. Infrastructure (Room v5)
**Таблицы (3NF):**
- `symbols` (code PK, owner_pattern_id FK, display_name, text_glyph, category, default_column_span, default_row_span)
- `patterns` (id PK, name, technique, direction, is_reversible, background_image_uri, image_offset_x, image_offset_y, image_scale)
- `pattern_rows` (id PK, pattern_id FK, row_number, UNIQUE(pattern_id, row_number))
- `pattern_operations` (pattern_row_id PK, column_start PK, symbol_code FK, back_symbol_code FK, color_hex, column_span_override, row_span_override, repeat_code)
- `pattern_repeats` (pattern_id PK, code PK, stitch_count, row_count)
- `projects` (id, name, category, created_at, updated_at, total_time_seconds, total_rows, target_row_count, pattern_id FK, current_row_number, current_pattern_row_id FK, current_stitch, sync_status)
- `project_progress` (project_id PK, pattern_row_id PK, UNIQUE, Index(pattern_row_id))

**Триггеры:** `trg_progress_check`, `trg_check_symbol_owner` (`BEFORE INSERT OR UPDATE`, `IS NOT`).
**Seed `symbols`:** `onOpen INSERT OR IGNORE`.
**Файлы:** `filesDir/patterns/<id>.img`. PdfRenderer: `ParcelFileDescriptor` без копирования.
**Backup 1.1:** `ProjectDto` += новые поля. Round-trip тесты. Дефолты для 1.0.

## 7. Presentation
**MVI:** `PatternViewerContract`, `PatternViewerViewModel`.
**Экраны:** `PatternViewerScreen`, `PatternCreationScreen`, `PatternOperationEditorScreen`.
**Canvas:** `PatternCanvas`, `CalibrationOverlay`, `CounterPanel`, `TextModePanel`.
**Координаты:** `remember` внутри `PatternCanvas`. Инверсия матрицы для тапов.
**Process death:** `rememberSaveable` (listSaver/mapSaver) для матрицы трансформации.
**Текст-режим:** только спицы. Крючок/вышивка/бисер — disabled.

## 8. Accessibility
- Контраст WCAG AA. Тапы 48dp. Шрифты 16sp+.
- TalkBack: `Modifier.semantics` на Canvas, `customActions`.
- Кнопки зума `+`/`-`.

## 9. Миграция 4 -> 5 + rollback
- Шаги: CREATE TABLE -> Триггеры -> Backup sessions -> Пересоздание projects -> DROP sessions -> DROP projects -> RENAME -> Restore sessions.
- Rollback: auto-export JSON перед `onOpen`. При падении — `MigrationErrorScreen`.

## 10. Монетизация
- Free: Базовые счётчики, калибровка, 1-страничный PDF.
- Premium: Синхронизация, многостраничный PDF, экспорт отчётов.

## 11. Технический долг (планируемый)
- См. таблицу `TD-PT-N` в TECHDEBT.md.

## 12. Тест-план
- Pre-v7 PR: `test.assets`, Coil, Backup DTO 1.1, VO (6-8 часов).
- Domain: `ProgressService`, `AddPatternOperationService`, `Pattern`.
- Migration: `Migration4To5Test` (Robolectric + `RAISE(ABORT)`).
- Repository: overlap, `getOperationsForRow`, round-trip backup.
- ViewModel: process-death, конкурентность.

## 13. Критерии приёмки
- Все тесты зелёные. Detekt 0. KDoc-парность. Ручной прогон.

## 14. Milestone-план
- Pre-v7 PR (16-20 ч).
- v8.0-alpha (1 нед).
- v8.0-beta (2 нед).
- v8.0-rc (1 нед).
- v8.0 (финал).

## 15. DoD
- Все критерии, rollback, обновление HANDOVER, скриншоты для стора.

## 16. Правила процесса
- `cat -n`, патчи через `python3`.
- Сборка через `cmd.exe`.
- `git add` явным списком.
