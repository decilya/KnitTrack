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


## Pattern Tracker v8.0 (утверждено 19.10.2026)

Полная спецификация: [docs/architecture/pattern-tracker-v8.0.md](./pattern-tracker-v8.0.md)

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

### Шаг 7c — открытые P2

- [ ] `AppErrorMapper.map(SQLException)` -> `UnknownError` вместо `DatabaseError` (DATA).
  `ProjectRepositoryImpl` / `PatternRepositoryImpl` вынуждены явно создавать
  `AppError.DatabaseError(ex)` в catch-блоках.
- [ ] Timber-политика в Domain не унифицирована (ARCH): `SaveSessionService`
  использует `Timber.e`, `AddProjectService` — нет. Выбрать единый стиль.

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

### P2

| Место | Проблема | План |
|---|---|---|
| TimerScreen.kt | «Нет проектов» — только текст ошибки, нет действия «Создать проект» / «К списку» | Добавить кнопку навигации |
| TimerScreen.kt | `formatTime` использует `Locale.US` — неочевидно для RU-локали | `Locale.ROOT` или комментарий, почему US |
| SessionDao / DailyStat | `getDailyStats` возвращает `com.knittrac.app.domain.entity.DailyStat` — утечка Domain-модели в Data | Projection-класс в `data/local/`, маппинг в репозитории |
| TimerViewModelTest | `mockk<Project>` bypasses `init { require(...) }` — ложно-зелёные тесты при будущих правках | Реальный `data class` конструктор в тестах |
