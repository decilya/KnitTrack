# KnitTrac — Технический долг (TECHDEBT)

## Текущий техдолг
| Файл / Место | Проблема | Почему так | План исправления |
|---|---|---|---|
| Migration2To3Test | Тест не запущен на устройстве | Нет эмулятора/USB | Отложено до решения инфраструктуры |
| SettingsScreen.kt (327 строк) | Слишком длинный файл | Много UI-состояний | Разбить на подкомпоненты |
| TimerForegroundService.kt (330 строк) | Слишком длинный файл | Много логики сервиса | Вынести логику в хелперы |
| DataExporterImpl / DataImporterImpl | По 5 ответственностей | Смешаны Reader/Serializer | Разделить на BackupReader (JSON) и BackupRepository (БД) |
| TimerService / TimerForegroundService | Коллизия имен (Service) | Не различаются доменный и Android-сервис | В стандарты добавить *AndroidService для платформенных |
