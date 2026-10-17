# KnitTrac — Стандарты разработки (STANDARDS)

Этот документ содержит жесткие правила, которые проверяются на код-ревью и автоматически (detekt).

## 1. Слои и архитектура
- Presentation -> Service -> Repository -> DAO.
- Domain не знает про Android, Room, Gson. Импорты `android.*` и `androidx.*` (кроме `androidx.annotation`) запрещены.
- Use case = один сценарий (например, `AddProjectService`).
- Provider вводится только если источников данных больше одного (ниже Repository).

## 2. Ошибки и корутины
- Результат всегда возвращается через `Result<T>`.
- Ошибки типизируются через `AppError` (Validation, NotFound, Io, Database, Network, Unknown).
- В `catch` первым всегда идет `CancellationException` (и пробрасывается).
- Все `catch` логируются через `Timber.e(e, "message")`.

## 3. База данных
- UPDATE проверяет `rows affected`. Если 0 — возвращать `NotFoundError`.
- DELETE идемпотентен для UI (0 строк = успех). Для auditable (бэкапы) — возвращать `Result<Int>`.
- Строковые литералы в SQL параметризуются.

## 4. DI
- `@Singleton` только на `@Binds` в модулях. На impl-классах `@Singleton` запрещен.
- **LongParameterList:** Если конструктор превышает 5 параметров, группировать зависимости в Parameter Object (класс с суффиксом `*Dependencies`). Подавлять правило Detekt через `@Suppress` запрещено.
- Внедрение только через конструктор (`@Inject constructor`).
- DataStore delegate ровно один.

## 5. Код и именование
- KDoc на русском к классам и публичным методам. Без markdown (**).
- Имена классов: `*Service`, `*Repository`, `*RepositoryImpl`, `*Dao`, `*Entity`, `*Dto`, `*ViewModel`, `*Screen`.
- Запрещены суффиксы `Manager`, `Helper`, `Utils`, `Handler`.
- System.currentTimeMillis() запрещен вне `TimeProvider`.

## 6. Запреты
- Никакого `GlobalScope`, `runBlocking` в UI/Application.
- Никаких `!!` в production.
- Никаких пустых `catch`.
- Никаких `println`. Только Timber.

## 7. Размеры и God Class

**Диагностика по трём осям. Класс подозрителен, если выполняются 2 из 3:**

1. **Зависимости:** > 5 параметров в `@Inject constructor`.
2. **Методы:** > 10 публичных методов (без `@Composable` и тестов).
3. **Строки:** > 300 строк.

**Если сработало 2 из 3 — разделить класс по ролям.** Не группировать зависимости в Parameter Object: это маскирует проблему, а не решает.

**Правильная реакция на Detekt `LongParameterList`** — разделить класс (извлечь интерфейс), а не обернуть зависимости в `*Dependencies`. Detekt — симптом, причина — нарушение SRP.

**Исключение:** класс-адаптер над внешним API (например, `FirebaseClient`) может превышать лимиты — это нормально, у него одна роль. Проверять «сколько ролей», а не «сколько строк».

**Пример правильного разделения:** `DataExporterImpl` (6 зависимостей, 3 роли: БД, JSON, I/O) → `BackupRepository` (интерфейс в domain) + `DataExporterImpl` (только JSON + I/O, 4 зависимости).
