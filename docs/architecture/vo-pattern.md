# Паттерн работы с Value Objects

Дата фиксации: 11 октября 2026
Статус: обязательный для всех новых VO

## Зачем

KnitTrac расширяется: заметки, схемы, счётчики рядов, инвентарь пряжи.
Каждая новая фича приносит 1-3 VO. Если для каждого изобретать подход —
архитектура разъедется. Этот документ фиксирует единый паттерн.

## Три уровня защиты

| Уровень | Где | Что защищает |
|---|---|---|
| 1. UI-валидация | ViewModel | Мгновенная обратная связь пользователю |
| 2. VO в Domain | Фабрики create / createOrNull / createOrDefault | Инварианты на уровне типа, компилятор |
| 3. CHECK в SQLite | MIGRATION_N_M | Последний барьер: БД не примет мусор |

## Где живут VO

- Domain: VO. Единственное место, где тип значим.
- Data (Entity, DTO): примитивы (String, Long). Persistence Ignorance.
- DAO: примитивы. Запросы не знают про VO.
- Mappers: единственная точка конверсии VO ↔ примитив.

## Фабрики VO

Каждый VO предоставляет три фабрики:

- create(value): throws на невалидном. Для мест, где валидация уже пройдена.
- createOrNull(value): null на невалидном. Для сервисов с Result.Error.
- createOrDefault(value): fallback. Для чтения из БД/DTO, где данные могут быть битые.

## Room TypeConverters

НЕ используем для VO. Причины:

- Room 2.6.1 нестабильно работает с @JvmInline value class.
- Gson 2.10.1 не сериализует value class без @JsonAdapter на каждый VO.
- Persistence Ignorance: Domain не должен зависеть от возможностей Room.

## CHECK constraints в SQLite

Добавляются ТОЛЬКО в миграции (Room не поддерживает их декларативно).

Пример для ProjectName (длина 1-100, не пусто):
    name TEXT NOT NULL
        CHECK(length(trim(name)) > 0 AND length(name) <= 100)

Пример для Duration (>= 0):
    duration_seconds INTEGER NOT NULL
        CHECK(duration_seconds >= 0)

КРИТИЧНО: при каждой новой миграции version N -> N+1 переносить CHECK
в новую CREATE TABLE вручную. Room их не видит и не сохранит.

## Шаблон для нового VO

1. Domain/entity/{VOName}.kt — value class с приватным конструктором + 3 фабрики.
2. Тесты VONameTest.kt — границы, trim, структурное равенство.
3. Domain-модель: заменить примитив на VO.
4. Mappers (Entity и Dto): VO <-> примитив с createOrDefault на чтении.
5. Сервис: использовать createOrNull -> Result.Error на входе.
6. Презентация: .value или .seconds для отображения.
7. Миграция: если меняется схема БД — CHECK constraint для нового поля.

## История изменений

- 11.10.2026: паттерн зафиксирован после интеграции ProjectName и Duration.
