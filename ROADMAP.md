# ROADMAP — KnitTrac

**Актуально на:** 13.10.2026
**HEAD:** 7319705
**Тесты:** 132, 0 failures

## ЦЕЛЬ v1.0

Android-приложение для учёта времени вязания с глобальным счётчиком
рядов на уровне проекта и двусторонней синхронизацией между
устройствами одного пользователя.

Релиз в Google Play — через 2.5–3 месяца.

## ЗАФИКСИРОВАННЫЕ РЕШЕНИЯ

### Sync
- Backend v1.0: Firebase Firestore.
- Backend v2.0 (через 6–12 мес): свой Symfony.
- Тип: двусторонняя, single-user multi-device.
- Конфликты: last-write-wins + custom merge.
- Удаления: soft delete.

### Счётчик рядов
- row_count в ProjectEntity, не в SessionEntity.
- Sync с debounce 2–3 секунды.
- Отмена ряда — редактирование проекта.
- Удаление сессии не влияет на row_count.

### Миграция 3->4
Добавить в SessionEntity: client_id, server_updated_at, sync_version, deleted_at.
Добавить в ProjectEntity: client_id, server_updated_at, sync_version, deleted_at, row_count.
Убрать из ProjectEntity: total_time_seconds (derived, считать через SUM).

## АНТИПАТТЕРНЫ

- KMP/Desktop до v1.0.
- Свой backend для sync до v1.0.
- AI-парсер PDF, соц. функции, агрессивный пейволл.
- Gson -> kotlinx.serialization до v1.5.
- GitHub Actions CI.
- Новые фичи (паттерны, заметки) — в v1.1.
- Смена scope v1.0.
- Пересмотр архитектуры чаще раза в 2 недели.

## ФАЗЫ

### Фаза 1 — Подготовка (5 сессий, ~12–15 ч)
| Сессия | Тема | Оценка |
|---|---|---|
| N+1 | FileLoggingTree -> Channel, PII redaction, backup rules, 2 DI-фикса | 2–2.5 ч |
| N+2 | WHPX + миграция 2->3 на эмуляторе | 2 ч |
| N+3 | ProGuard, signing, FGS type, POST_NOTIFICATIONS, DTO nullability | 3 ч |
| N+4 | Тесты DataExporter / DataImporter | 3–4 ч |
| N+5 | ARCHITECTURE.md / HANDOVER.md, pre-commit hook | 2 ч |

### Фаза 2 — Миграция 3->4 + счётчик (3 сессии, ~9–10 ч)
| Сессия | Тема | Оценка |
|---|---|---|
| N+6a | Схема + Migration3To4 | 3 ч |
| N+6b | SQL + Domain + тесты | 3–4 ч |
| N+7 | UI счётчика рядов | 2–3 ч |

### Фаза 3 — Firebase интеграция (2 недели)
Firebase setup, Firestore, Auth, коллекции, SDK, mapper.

### Фаза 4 — Sync engine Android (2 недели)
Local sync state, push/pull, конфликты, UI, тесты на 2 устройствах.

### Фаза 5 — Финал (2 недели)
Bug fixing, Play Console, closed testing, production.

## P0-БЛОКЕРЫ

| Проблема | Сессия |
|---|---|
| PII в логах + autobackup | N+1 |
| FileLoggingTree блокирует поток | N+1 |
| Миграция не проверена на эмуляторе | N+2 |
| ProGuard без правил | N+3 |
| FGS type dataSync неверен | N+3 |
| DataExporter/Importer не покрыт | N+4 |
| @Singleton на Project/Session impl | N+1 |

## P1 — до релиза

- TimeProvider не используется.
- ProjectName.DEFAULT — вынести в strings.xml.
- Session.updatedAt в конструкторе — инжектить время.
- LanguageOption — в domain/entity/.
- DELETE в SessionDao — soft delete.

## ТЕКУЩИЙ ФОКУС

Сессия N+1:
- FileLoggingTree: Channel + consumer на Dispatchers.IO.
- Redaction PII (ProjectRepositoryImpl:92, SettingsScreen:300,311, TimerForegroundService:237).
- res/xml/backup_rules.xml + data_extraction_rules.xml.
- Снять @Singleton с ProjectRepositoryImpl, SessionRepositoryImpl.
- Решить по AppErrorMapper.

## МЕТРИКИ

- Тесты до релиза: >= 210.
- Crash-free rate: >= 99.5%.
- Data loss: 0.


### Gateway — план появления
- Gateway (обёртка над внешней системой) появится на Фазе 3 (Firestore Sync).
- До этого момента SyncService не пишется, модель синхронизации не фиксируется.
- Gateway живет в `domain/gateway` (интерфейс) и `data/gateway` (реализация).
