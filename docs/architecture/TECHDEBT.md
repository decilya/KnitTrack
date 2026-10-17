# KnitTrac — Технический долг (TECHDEBT)

## Текущий техдолг
| Файл / Место | Проблема | Почему так | План исправления |
|---|---|---|---|
| Migration2To3Test | Тест не запущен на устройстве | Нет эмулятора/USB | Отложено до решения инфраструктуры |
| SettingsScreen.kt (327 строк) | Слишком длинный файл | Много UI-состояний | Разбить на подкомпоненты |
| TimerForegroundService.kt (330 строк) | Слишком длинный файл | Много логики сервиса | Вынести логику в хелперы |
| TimerService / TimerForegroundService | Коллизия имен (Service) | Не различаются доменный и Android-сервис | В стандарты добавить *AndroidService для платформенных |
| KnitTracApplication.onCreate | Timber.plant только под BuildConfig.DEBUG | Debug tree не нужен в проде, Crashlytics/file-tree не заведены | Добавить CrashlyticsTree (или file logger) в release, вне if (DEBUG) |
| ProjectName.DEFAULT = "Без названия" | UI-строка на русском в Domain | Fallback для пустого имени при переносе в strings.xml не успели | Вынести текст в strings.xml, DEFAULT сделать нейтральным (пустое значение + sentinel для UI) |
