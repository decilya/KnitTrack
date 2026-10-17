# KnitTrac — Технический долг (TECHDEBT)

## Текущий техдолг
| Файл / Место | Проблема | Почему так | План исправления |
|---|---|---|---|
| TimerManager | Суффикс Manager | Историческое имя | Переименовать в TimerService (неделя 2) |
| ProjectRepositoryImpl | System.currentTimeMillis | Быстрая реализация | Заменить на TimeProvider |
| ProjectName.DEFAULT | "Без названия" в Domain | Локализация не успела | Вынести в strings.xml |
| AppErrorMapper | Мапит IOException в NetworkError | Устарел, добавили IoError | Удалить маппер |
| DataExporter/DataImporter | Нет тестов | Отложено | Написать тесты на неделе 2 |
| LanguageOption | В core/localization вместо domain | Спешка | Перенести в domain/entity |
| Session.updatedAt | Default в конструкторе | Удобство | Убрать default |
| Play Console FGS | Не отправлена форма specialUse | Не дошло до публикации | Отправить при Internal Testing |
