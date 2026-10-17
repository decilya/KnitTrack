# KnitTrac — Технический долг (TECHDEBT)

## Текущий техдолг
| Файл / Место | Проблема | Почему так | План исправления |
|---|---|---|---|
| AppErrorMapper.kt | Мапит IOException в NetworkError | Устарел, добавили IoError | Заменить NetworkError на IoError |
| TimerManagerImpl.kt | Суффикс Manager | Историческое имя | Переименовать в TimerService |
| AdManagerImpl.kt | Суффикс Manager | Историческое имя | Переименовать в AdService |
| NoOpPaymentManager.kt | Суффикс Manager | Историческое имя | Переименовать в NoOpPaymentService |
| DataExporter/DataImporter | Нет тестов | Отложено | Написать тесты на неделе 2 |
| Migration2To3Test | Тест не запущен на устройстве | Нет эмулятора/USB | Отложено до решения инфраструктуры |
| SettingsScreen.kt (327 строк) | Слишком длинный файл | Много UI-состояний | Разбить на подкомпоненты |
| TimerForegroundService.kt (330 строк) | Слишком длинный файл | Много логики сервиса | Вынести логику в хелперы |
