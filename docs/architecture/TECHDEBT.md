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
| ProjectName.kt | `DEFAULT = "Untitled"` (EN в Domain) — по handover §5 это OK, RU-строка в `values-ru/strings.xml` через `isUntitled` | Оставить; при синхронизации с бэкендом — учитывать, что DEFAULT не User-Visible |

### P2

| Место | Проблема | План |
|---|---|---|
| TimerScreen.kt | «Нет проектов» — только текст ошибки, нет действия «Создать проект» / «К списку» | Добавить кнопку навигации |
| TimerScreen.kt | `formatTime` использует `Locale.US` — неочевидно для RU-локали | `Locale.ROOT` или комментарий, почему US |
| SessionDao / DailyStat | `getDailyStats` возвращает `com.knittrac.app.domain.entity.DailyStat` — утечка Domain-модели в Data | Projection-класс в `data/local/`, маппинг в репозитории |
| TimerViewModelTest | `mockk<Project>` bypasses `init { require(...) }` — ложно-зелёные тесты при будущих правках | Реальный `data class` конструктор в тестах |
