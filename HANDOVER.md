# HANDOVER — KnitTrac

**Актуально на:** 16.10.2026
**Тесты:** 134, 0 failures

---

## 0. КАК ИСПОЛЬЗОВАТЬ

Полный контекст проекта KnitTrac. Прочитай целиком.

**В первом ответе ИИ:**
1. Подтвердить понимание.
2. Спросить, с какого дня начинать.
3. Не начинать с комплиментов. Не писать «ты абсолютно прав».
4. Верифицировать факты через grep/cat перед действиями.

**Ограничение:** у ИИ нет доступа к файловой системе. Все команды запрашиваются у пользователя.

**Перед чтением — узнать актуальное состояние.** В шапке нет HEAD-хэша:
коммит не может содержать собственный хэш. Актуальный HEAD и рабочее
дерево — в git:

    cd /mnt/c/Users/decilya/projects/KnitTrac
    git log --oneline -1
    git status --short

---

## 1. ПРОЕКТ

**KnitTrac** — Android-приложение для учёта времени вязания. Глобальный счётчик рядов на уровне проекта, статистика, sync между устройствами.

**GitHub:** https://github.com/decilya/KnitTrack
**Remote:** origin, SSH
**Ветка:** main. Feature branches не создаются.
**Локально:** /mnt/c/Users/decilya/projects/KnitTrac

**Окружение:** Windows 10 + WSL2. Сборка в PowerShell. JDK 17. Локаль C.UTF-8.

---

## 2. СТЕК

Kotlin 1.9.22, Compose BOM 2024.02.00, Room 2.6.1, DataStore 1.0.0, Hilt 2.50, KSP 1.9.22-1.0.17, Gson 2.10.1, Timber 5.0.1, YandexAds 8.3.0, ycharts 2.1.0, AGP 8.6.0, Gradle 8.7. compileSdk 35, targetSdk 34, minSdk 26. Тесты: JUnit 4.13.2, MockK 1.13.9, coroutines-test 1.7.3. Detekt 1.23.7.

gradle/libs.versions.toml НЕ существует — версии инлайн.

---

## 3. ЦЕЛЬ v1.0

Релиз в Google Play через 2.5–3 месяца (январь 2027). Scope: таймер, проекты, сессии, статистика, настройки, счётчик рядов, sync через Firebase Firestore.

Backend v2.0 (через 6–12 мес): Symfony. Решение обратимо.

Firebase: Spark бесплатный — 50k чтений/день, 20k записей/день, 1 GiB. Blaze при превышении, budget alert $5. Data Safety Declaration в Play.

---

## 4. АРХИТЕКТУРА

### 4.1. Слои

Presentation — Service — Repository — Provider — DAO/DataStore/Cache.
Gateway — рядом с Repository, для внешних систем (Firestore), используется SyncService.

### 4.2. Три понятия (НЕ ПУТАТЬ)

ServiceProvider = DI-регистрация (Hilt @Module), аналог services.yaml.
Provider = источник данных НИЖЕ Repository, аналог EntityManagerInterface.
Gateway = обёртка над внешней системой, аналог HTTP Client.

### 4.3. Provider

Ниже Repository, не между Service и Repository. RepositoryImpl делегирует Provider-у. При одном источнике Provider не вводится.

### 4.4. Gateway

Обёртка над внешней системой. Живёт рядом с Repository. Используется SyncService. Не вводить сейчас. Появится на Фазе 3.

### 4.5. Маппинг Symfony — KnitTrac

services.yaml — Hilt @Module.
Service — *Service.
Doctrine Repository — *Repository.
EntityManager — *Dao.
Entity — *Entity.
DTO — *Dto.
EventDispatcher — _effect.emit.

### 4.6. Пакеты

core — Result, AppError, TimeProvider, LocaleService, SyncStatus, logging, localization, common.
data — dto, local, mapper, repository, di.
domain — entity, repository, service.
platform — timer, service, notification, di.
presentation — feature_*, common.

---

## 4.5. АРХИТЕКТУРНЫЕ ПРИНЦИПЫ

### Пять источников истины (приоритет при конфликте)

1. Clean Architecture
2. DDD
3. SOLID
4. Symfony-подобность
5. Clean Code

### 4.5.1. Clean Architecture

1. Зависимости направлены внутрь.
2. Domain не знает про Android. В domain/ запрещены import android.*, androidx.*, com.google.gson.*, androidx.room.*. Проверяется архитектурным тестом (День 4).
3. Границы через интерфейсы (domain — интерфейс, data/platform — реализация, Hilt @Binds).
4. Сущности пересекают границы, DTO — нет.
5. Use case = один сценарий.

### 4.5.2. DDD

Entity — Project, Session.
Value Object — Duration, ProjectName, ThemeMode.
Aggregate — Project + Session (FK CASCADE).
Repository — ProjectRepository, SessionRepository.
Domain Service — AddProjectService, SaveSessionService.

1. VO только в Domain. Приватный конструктор, три фабрики: create (throws), createOrNull, createOrDefault.
2. Persistence Ignorance. Entity не знает про VO. ProjectEntity.totalTimeSeconds: Long, не Duration.
3. Пример: SessionEntity.durationSeconds: Long — OK. SessionEntity.duration: Duration — нет, Room не умеет.
4. Инварианты в VO.
5. ProjectName (VO) vs String name в Entity — подтверждающий пример.

### 4.5.3. SOLID

S — одна причина изменения. ProjectRepositoryImpl меняется только при изменении хранения проектов.
O — новый AppError приводит к ошибке компиляции в when.
L — ProjectRepositoryImpl полностью заменяет ProjectRepository. Пример нарушения: updateTotalTime возвращал Success при rows == 0, исправлено в спринте 2c.
I — дробить интерфейс, если уже видно, что клиенты используют часть.
D — Service инжектит интерфейс, не impl. В конструкторе @Inject только интерфейсы и VO.

### 4.5.4. DI

Hilt — единственный контейнер.

1. Всё через @Inject constructor.
2. Scope на @Binds. Не на impl.
3. @Provides для внешних (Gson, DataStore, AppDatabase).
4. Квалификаторы для одинаковых типов (@IoDispatcher, @DefaultDispatcher).
5. @ApplicationContext только где реально нужен.
6. DataStore delegate один. Дубликат = рантайм-краш.

### 4.5.5. Clean Code

Имена: спецификация, произносимо, без типов. Глагол для метода, существительное для класса.
Функции: максимум 30 строк, один уровень абстракции, максимум 5 параметров, ранний возврат.
Комментарии: только WHY. KDoc на русском. TODO только с issue.
Формат: 4 пробела, без trailing whitespace.

### 4.5.6. Symfony-подобность

Совпадает: Controller — ViewModel+Screen. Service — *Service. Repository. EntityManager — *Dao. Entity — *Entity. DTO — *Dto. services.yaml — Hilt @Module. Form — VO + ValidationError. EventDispatcher — _effect.emit.

Отличается (Android-специфика): Compose, MVI, Flow, Room.

При конфликте Android-идиома побеждает.

Запреты:
- Нет Manager (исключение LocaleService до Дня 4).
- Нет Helper, Utils, Handler.
- Нет ServiceProvider в коде приложения.
- Нет Provider между Service и Repository.

### 4.5.7. Что проверяется автоматически

Clean Architecture — Detekt ForbiddenImport + DomainLayerTest.
SOLID (SRP) — Detekt LongMethod, LargeClass.
SOLID (DIP) — ручной review + architecture test.
DI (scope) — кастомное @Singleton на impl.
Clean Code (имена) — Detekt NamingConvention.
Clean Code (функции) — Detekt LongParameterList, ReturnCount.
Clean Code (комментарии) — Detekt ForbiddenComment (отключено).
Symfony (суффиксы) — Detekt ClassNaming.

Не проверяется автоматически: VO vs примитив, границы агрегатов DDD, уровень абстракции.

### 4.5.8. Приоритеты при конфликте

1. VO vs примитив в Entity: Persistence Ignorance.
2. Дробить класс: SOLID.
3. Symfony-паттерн vs Android-идиома: Symfony-запрет, если идиома не улучшает читаемость.
4. Функция более 30 строк: лимит с оговоркой.
5. Ручной код vs инструмент: инструмент.

---

## 5. ЖЁСТКИЕ ПРАВИЛА КОДА

1. KDoc на русском к классу и публичному методу.
2. Имена — английский camelCase. Не backtick-русские.
3. Комментарии внутри методов — русский.
4. Без markdown в KDoc.
5. В commit не упоминать KDoc, не упоминать BUILD SUCCESSFUL.
6. KDoc-парность через grep -o: количество /** должно равняться */.
7. Никаких file-level @Singleton на impl — только на @Binds.

---

## 6. cat И HEREDOC

Когда cat с heredoc: только для файлов без кириллицы. Порог по строкам не важен.

Проблема: heredoc портит многострочные вставки с русским. История: EmptyStateView.kt, TimerForegroundService.kt, TimerManagerImplTest.kt.

Когда python3 + patch(): всегда для файлов с кириллицей.

def patch(path, old, new, expected, label): читает файл, считает old, если не равно expected — падает, иначе заменяет и пишет.

def write_new(path, content, label): если файл существует — падает, иначе создаёт.

Свойства: expected падает до записи. write_new падает, если файл существует. Кириллица корректно в UTF-8.

Проверка после записи (для созданного или изменённого .kt-файла): head/tail, KDoc-парность через grep -o, отсутствие маркера завершения heredoc внутри файла.

---

## 7. GIT WORKFLOW

Все коммиты в main. Feature branches не создаются.
git push origin main без --dry-run.
Force-push только --force-with-lease.
Никаких merge commits.
git status --short до git add.
git add явный список, не git add .
Один коммит = одна логическая операция.

Commit:
type(scope): краткое

Проблема:
- ...

Решение:
- ...

Результат: N тестов, 0 failures.

Types: feat, fix, refactor, test, chore, docs.

---

## 12.5. САМОПРОВЕРКА ПОСЛЕ ПАТЧА

Уровень 1. Файл не испорчен. KDoc-парность, нет маркеров heredoc.
Уровень 2. Изменения соответствуют. grep новой и старой сигнатуры.
Уровень 3. Git status корректен. Ровно N файлов M/A.
Уровень 4. Кодировка. file = UTF-8, не UTF-16.

Если хоть один не прошёл — git checkout -- файл.

---

## 12.6. ЧЕК-ЛИСТЫ ПЕРЕД PUSH

### 12.6.1. До коммита (5 пунктов)

1. Сборка зелёная, число тестов сверено с XML-отчётом.
2. git status --short — ровно N файлов.
3. KDoc-парность не сломана.
4. Нет маркеров heredoc в .kt.
5. Ручной прогон grep-проверок из 12.5.

XML-отчёт: app/build/test-results/testDebugUnitTest/*.xml.

### 12.6.2. До релиза / важного коммита (10 пунктов)

1. Нет бизнес-логики в Presentation.
2. Нет DAO в Presentation/Service.
3. Цепочка слоёв соблюдена.
4. Ошибки типизированы через AppError.
5. CancellationException первым catch.
6. Timber.e с cause во всех catch.
7. UPDATE проверяет rows affected: 0 приводит к NotFoundError (ручная).
8. DELETE идемпотентен для UI, для auditable приводит к Result с Int, 0 равно NotFoundError.
9. Имена тестов — английский camelCase без backtick-кириллицы.
10. KDoc парный, русский, без markdown.

### 12.6.3. Код-ревью фичи (структурное)

Архитектура: границы слоёв, зависимости обоснованы, нет god-классов, нет методов более 30 строк.
DI: всё через конструктор, нет @Singleton на impl, scope на @Binds, один DataStore delegate.
Ошибки: все catch логируют, CancellationException пробрасывается, нет голых catch без тела.
Тесты: покрытие, camelCase-имена, без Thread.sleep, диспетчеры через Dispatchers.setMain.
Git: один коммит = одна операция, сообщение по стандарту, число тестов указано.

### 12.6.4. Общее ревью проекта

Триггер: перед коммитом в domain, data/repository, di, перед релизным тэгом.

Скрипт в WSL проверяет: файлы более 400 строк, известный техдолг (P1), блокеры (TODO без issue, FIXME, HACK, XXX, println, double-bang, UTF-8, KDoc-парность, DataStore delegate), git status, git log.

После Дня 1 удалить из скрипта пункты, покрытые detekt.yml: @Singleton, AppErrorMapper, TODO, FIXME, HACK, println, KDoc-парность.

### 12.6.5. Порядок перед push

1. Сборка PowerShell: .\gradlew.bat clean test.
2. Сверить число тестов с XML.
3. Чек-лист 12.6.1 (5).
4. Скрипт 12.6.4 (если триггер).
5. git status --short.
6. git add явно, не git add .
7. git commit по стандарту.
8. git log --oneline -3.
9. git push origin main.

Запрещено: push без сборки, git add ., коммит без числа тестов, force-push без --force-with-lease.

---

## 12.7. САМОПРОВЕРКА ПОСЛЕ PUSH

git log --oneline -3.

Проверить визуально, что HEAD совпадает с ожидаемым коммитом. При соло-разработке достаточно — push через SSH подтверждается выводом git push.

Если push не прошёл (rejected, error) — разбираться по выводу git push, без дополнительных git fetch.

---

## 12.8. DEFINITION OF DONE

Фича готова, если:

1. Проходит .\gradlew.bat clean test, число сверено с XML.
2. Новые ветви логики покрыты тестами.
3. Пройден чек-лист 12.6.2.
4. Пройден чек-лист 12.6.1.
5. В commit message указано, какие принципы из 4.5.8 конфликтовали и почему выбран приоритет. Если конфликтов не было — явно написать «конфликтов не было».

---

## 12.9. WORKFLOW НОВОЙ ФИЧИ

ИИ не пишет код новой фичи, пока не пройдены фазы 1–3 и пользователь не подтвердил план.

Триггер: новая функциональность.

Не триггер: KDoc, форматирование, переименование локальной переменной, опечатка, правка одного теста. Для сжатого workflow — одна строка плана и «делаю, ок?».

Фаза 1. Разведка. Прочитать HANDOVER.md. grep по сущностям. Прочитать все файлы, которых фича коснётся. Проверить похожие фичи, техдолг, ROADMAP, TECHDEBT. Критерий выхода: ИИ назвал ВСЕ файлы и обосновал полноту. Если фича триггерит изменение в domain, data/repository, di, AndroidManifest.xml, схеме БД или public API — минимум один из названных файлов в этих пакетах.

Фаза 2. Проектирование. Влияние. Архитектура по слоям. SOLID. DDD. Идемпотентность. Безопасность. Надёжность. Экономия батареи. Для UI-only фичи часть пунктов N/A с обоснованием.

Фаза 3. План и схема (STOP). Схема потоков данных (ASCII или mermaid). Последовательность шагов. Оценка объёма. Риски. STOP.

Фаза 4. Реализация. Точно по плану. Патч через python3 и patch(expected=N). Самопроверка после каждого патча.

Фаза 5. Self-review (до сборки). SOLID. DDD. Безопасность (grep новых Timber, double-bang, System.currentTimeMillis, catch). Идемпотентность. Надёжность. Экономия батареи. Соответствие плану. Чек-листы 12.6.1 п. 3, 4, 5 и 12.6.2 п. 1, 2, 3, 4, 5, 6, 10.

Фаза 6. Сборка, тесты, коммит. PowerShell .\gradlew.bat clean test. Чек-листы 12.6.1 п. 1, 2 и 12.6.2 п. 7, 8, 9. Коммит через python (heredoc с кириллицей запрещён). Push.

### 12.9.1. Итоговый flow

разведка, проектирование, план плюс схема, STOP, реализация, self-review, СБОРКА, commit, push. Если фейл — обратно в реализацию.

### 12.9.2. Что запрещено

Начинать код без фаз 1–3. Пропускать STOP. Коммитить без зелёной сборки. Коммитить без self-review. Расширять scope за пределы плана. «Потом починим». Писать код до подтверждения плана. Heredoc для commit message с кириллицей.

---

## 12.10. ВЕТКИ

Все коммиты в main. Feature branches не создаются.

1. Соло-проект, нет PR-ревью.
2. Нет параллельной работы, merge commits запрещены.
3. Pre-commit hook работает одинаково.
4. Откат на main — git revert или git reset --hard HEAD~1.
5. Крупная экспериментальная переработка — git stash или отдельный коммит.

Правило пересматривается при появлении второго разработчика.

---

## 12.11. САМОПРОВЕРКА ДОКУМЕНТАЦИИ (docs)

Docs-коммиты проверяются так же строго, как код. Файл — markdown, но класс
дефектов тот же: self-reference, расхождения описания и содержимого,
непроверенные утверждения.

Правила:

П1. Документ не описывает своё состояние. Никаких HEAD-хэшей, «последнего
коммита», «(этот)». Только диапазоны и отсылка к git log.

П2. Диапазон A..B записывается только после git log --oneline A..B. Каждый
коммит из вывода либо в списке, либо явно сгруппирован с другими в один
пункт. Пропускать коммиты из описания нельзя. Объединение мелких fix'ов
в одну строку допустимо (пример: «Документация: fix placeholder, fix 21.5»
— три коммита, один пункт).

П3. Перед git commit для docs — обязательные проверки:
- grep на self-reference (HEAD:, «этот», «последний коммит»);
- git log --oneline A..B для каждого диапазона в патче;
- парность двойных звёздочек (markdown emphasis) в тексте;
- отсутствие маркеров завершения heredoc внутри документа
  (само-референция правила);
- git status --short совпадает с ожидаемым списком файлов.

П4. Один fix-коммит на всё найденное. Не по одному на каждый дефект.

П5. amend и force-with-lease — только по явному согласию в обе стороны.
Никаких standing authorizations, никакого молчаливого согласия «по умолчанию».
Каждый раз отдельно, каждый раз вслух.

П6. Docs-синхронизация сессии — один коммит в конце. Исключение: добавление
новой документации (новый раздел, новый файл) — отдельный коммит, в
fix-очередь не накапливается.

П7. Fix-очередь. Дефект docs найден, но на работу не влияет — записать в
очередь, закрыть одним коммитом в конце сессии. Не чинить по ходу.

Метрика успеха: за сессию docs-коммитов не больше одного (без учёта
добавления нового содержания).

---

## 13. СОСТОЯНИЕ — P0-БЛОКЕРЫ

### P0-1. PII в логах

**Статус:** ЗАКРЫТ в коммите c96017b.

Файл: data/repository/ProjectRepositoryImpl.kt:92.

Строка Timber.e(e, "addProject failed: name=%s category=%s", project.name.value, project.category) пишет user input в файл на диске. При allowBackup уезжает в Google autobackup.

Fix: убрать project.name.value. Плюс SettingsScreen:300,311 — убрать uri. Плюс TimerForegroundService:237 — проверить. Плюс SaveSessionService:53 — русский на английский.

### P0-2. FileLoggingTree синхронный ввод-вывод

**Статус:** ЗАКРЫТ в коммите 022e94f.

file.appendText блокирует поток. Jank и износ eMMC.

Fix: Channel<LogEntry>(UNLIMITED) плюс consumer на Dispatchers.IO, trySend из log().

### P0-3. Миграция 2 к 3 не прогонялась

**Статус:** ОТКРЫТ, День 5.

connectedDebugAndroidTest не запускался.

Fix: WHPX, reboot, эмулятор Pixel 6 API 33.

PowerShell от админа: Enable-WindowsOptionalFeature -Online -FeatureName HypervisorPlatform -All.

### P0-5. FGS type dataSync неверен

Manifest строка 40 (была): android:foregroundServiceType="dataSync".

**Статус:** ЗАКРЫТ (chore(build) 5aadf5e). dataSync → specialUse, property PROPERTY_SPECIAL_USE_FGS_SUBTYPE добавлен. Обоснование в Play Console — задача TECHDEBT (День 6).

Fix: specialUse с обоснованием в Play Console.

### P0-4. ProGuard — закрыт как не блокер

app/proguard-rules.pro содержит правила для Gson (DTO плюс domain), Coroutines, attributes. isMinifyEnabled = true стоит.

**Статус:** ЗАКРЫТ (chore(build) 5aadf5e). isShrinkResources = true, -keep class com.knittrac.app.data.local.entity.** { *; } добавлены. assembleRelease успешен (5m 21s, R8 + resource shrink без ошибок).

### P0-6. @Singleton на impl-классах

ProjectRepositoryImpl.kt:32 и SessionRepositoryImpl.kt:31 были аннотированы @Singleton.

Правило 4.5.4: scope на @Binds, не на impl. RepositoryModule уже ставил @Singleton на @Binds — двойной scope.

**Статус:** ЗАКРЫТ (refactor(di), 16.10.2026). @Singleton и import javax.inject.Singleton сняты с обоих impl-классов. Скоуп остался только на @Binds в RepositoryModule.

---

## 14. СОСТОЯНИЕ — P1

6. @Singleton на ProjectRepositoryImpl, SessionRepositoryImpl.
7. System.currentTimeMillis в репозиториях вместо TimeProvider.
8. ProjectName.DEFAULT = "Без названия" — русская строка в Domain.
9. AppErrorMapper — НЕ мёртвый. Используется в BaseUseCase.kt и BaseFlowUseCase.kt. Плюс конфликт: мапит IOException к NetworkError, а в 2b-1 добавлен IoError и применён в репозиториях. Два маппинга одного исключения.
10. DataExporter и DataImporter без тестов.
11. LanguageOption в core/localization вместо domain/entity. LocaleManager переименован в LocaleService (refactor(locale), День 4).
12. Session.updatedAt default в конструкторе.
13. SaveSessionService:53 — русский текст в логе.

---

## 15. ЗАКРЫТО В СЕССИЯХ

Полная история — `git log --oneline`. Сводка по сессиям; диапазоны
`<A>..<B>` пригодны для `git log <A>..<B>` в терминале. Одиночный
хэш — сессия из одного коммита.

Подготовка (спринты 2b-1...2d, docs-работа, fix check_standards) — 2f91e72..7f4ebf3:
- 2b-1: Result-API Settings/Locale, IoError, ShowError.
- 2b-2: IoError и CancellationException в exporter/importer.
- 2c: ProjectRepository.updateSyncStatus, чистка DAO.
- 2d: FileLoggingTree для release.
- docs(roadmap) — scope v1.0 (be47221).
- docs(readme) — замена черновика (d4f407b).
- docs(architecture) — манифест coding-standards.md (5c57644).
- docs(standards) — coding-standards.md v4.0 + валидатор (13a0384).
- fix check_standards.py (7f4ebf3).

День 1 (detekt, hook, git infrastructure) — c6c9a7a..b3c624a:
- detekt 1.23.7, config/detekt/detekt.yml, baseline.
- pre-commit hook через Windows gradlew.bat (WSL-safe).
- .gitattributes, HANDOVER.md в репозитории.

День 2 (PII, backup rules) — c96017b:
- PII redaction: ProjectRepositoryImpl, SettingsScreen, SaveSessionService.
- backup_rules.xml, data_extraction_rules.xml, манифест.

День 3 (FileLoggingTree) — 022e94f:
- Channel-based FileLoggingTree, consumer на Dispatchers.IO.
- log_doesNotBlockCaller — реальный регрессионный тест.

День 4 (release, DI, locale, DomainLayerTest, docs-fixes) — 5aadf5e..6a9c737:
- release-readiness: isShrinkResources, Room entity keep, FGS specialUse.
- Снятие @Singleton с impl-репозиториев.
- LocaleManager -> LocaleService (9 файлов, git mv).
- DomainLayerTest (архитектурный тест domain/).
- README.md (EN), README.ru.md (RU).
- Документация: fix placeholder в разделе 15, fix 21.5,
  синхронизация расхождений после Дня 4.

День 6 — разбиение стандартов, Provider-модель, TECHDEBT.md:
- Созданы STANDARDS.md, TECHDEBT.md, CONTRIBUTING.md.
- coding-standards.md переименован в PHILOSOPHY.md.
- Скрипт check_standards.py в архиве.
- ROADMAP.md: добавлен Gateway.

День 5 — Статический анализ миграции 2->3:
- Код MIGRATION_2_3 и Migration2To3Test проверены вручную, ошибок нет.
- Запуск connectedDebugAndroidTest отложен из-за отсутствия устройства (в TECHDEBT).

День 7 — README по папкам, ретроспектива:
- Созданы 10 README.md для ключевых пакетов (core, domain, data, presentation и т.д.).
- Обновлен корневой README.md (добавлены ссылки на документацию).
- TECHDEBT.md дополнен точными путями из код-ревью (System.currentTimeMillis, Manager-суффиксы).

Неделя 2, День 1 — TimeProvider:
- Создан FakeTimeProvider для тестов.
- AddProjectService, ProjectRepositoryImpl, SaveSessionService, SessionRepositoryImpl и DataExporterImpl переведены на TimeProvider.
- Убран default System.currentTimeMillis() из Session.kt.
- Время во всем проекте теперь управляется через TimeProvider (0 прямых вызовов в коде).

Неделя 2, День 2 — Рефакторинг Manager-классов:
- TimerManager -> TimerService.
- AdManager -> AdService.
- PaymentManager -> PaymentService (NoOpPaymentService).
- Все упоминания в DI, тестах и UI обновлены.

Неделя 2, День 3 — AppErrorMapper:
- IOException теперь мапится в IoError (дисковые операции).
- NetworkError удален как мертвый код (вернется на Фазе 3 с Firebase).

Неделя 2, День 4 — Тесты экспорта/импорта:
- Написаны Unit-тесты для DataExporterImpl и DataImporterImpl (3 теста).
- Покрыт сценарий успешной сериализации/десериализации JSON и обработки битого JSON.
- Для мока транзакции Room использован mockkStatic и secondArg.

Неделя 2, День 5 — Исправление P0 из код-ревью:
- BaseUseCase и BaseFlowUseCase теперь пробрасывают CancellationException (structured concurrency).
- NetworkError возвращен с @Deprecated для Фазы 3.
- DataImporterImpl мапит IllegalStateException (битый JSON) в IoError.
- pre-commit hook теперь блокирует коммит при падении сборки (exit 1).
- Detekt LongMethod игнорирует тесты.

Неделя 2, День 6 — Симметрия DI:
- Создан DataImportDependencies (Parameter Object).
- DataImporterImpl сокращен до 2 параметров.

Закоммичено: ROADMAP.md, README.md (UTF-8), docs/architecture/coding-standards.md (535 строк, будет разбит в Дне 6), config/detekt/detekt.yml, config/detekt/baseline.xml, .gitattributes, .githooks/pre-commit, HANDOVER.md.

Мусорный 8f601dc откачен через git reset --hard и git push --force-with-lease.

---

## 16. КОНТЕКСТ ОБСУЖДЕНИЯ (15-16.10.2026)

Попытка ввести стандарты в другом чате: docs/architecture/coding-standards.md v4.0 (535 строк) и scripts/check_standards.py (regex-валидатор).

Проблемы стандартов:

1. Валидатор не работает. Regex без AST даёт false positives. Правильно: detekt.
2. Раздел 4 (Provider) переписан: Provider — ниже Repository.
3. Запрет backtick-тестов — неверное прочтение. Правило: английский, camelCase, backtick при 5 плюс словах.
4. DI-Logger — абстракция при 3 плюс использованиях и тестах.
5. Противоречия: AppErrorMapper, суффиксы, лимит 400 против стандарт 535.
6. Лимиты без оговорок.
7. Чек-лист 15 пунктов разбить на 5 плюс 10.
8. DELETE-идемпотентность — оговорка.

Ключевой сдвиг: 535-строчный стандарт — не защита, а намерение. Защита: компилятор и типы, detekt/ktlint, архитектурные тесты, README по папкам, pre-commit, PHILOSOPHY.md.

Разбиение стандарта на 5 артефактов (День 6):

docs/architecture/PHILOSOPHY.md — около 500 строк — справочник «почему».
docs/architecture/STANDARDS.md — около 60 строк — enforcement.
docs/architecture/TECHDEBT.md — около 40 строк — техдолг.
docs/architecture/CONTRIBUTING.md — около 30 строк — процесс.
detekt.yml — около 100 строк — автоправила.
README по папкам — 5 на 10 — навигация.

coding-standards.md удалить в Дне 6.

---

## 17. ПЛАН НА 7 ДНЕЙ

### День 1 (3 ч). Detekt и pre-commit и инфраструктура. ЗАКРЫТ.

Detekt 1.23.7. Конфиг config/detekt/detekt.yml. Baseline 9 записей.
Rules: LongMethod 30, LargeClass 300, LongParameterList 5, ReturnCount 2, naming, WildcardImport, UnusedImports, UnusedPrivateMember. FunctionNaming игнорирует Composable и Test.
.gitattributes, .gitignore дополнены.
.githooks/pre-commit и git config core.hooksPath .githooks.
Удалены 2 unused import.
Коммит c6c9a7a.

### День 2 (3 ч). PII и backup rules. ЗАКРЫТ.

ProjectRepositoryImpl:92 — убрать project.name.value.
SettingsScreen:300,311 — убрать uri.
TimerForegroundService:237 — проверить.
SaveSessionService:53 — русский на английский.
res/xml/backup_rules.xml и data_extraction_rules.xml.
Манифест: android:dataExtractionRules (API 31 плюс) и android:fullBackupContent (API меньше 31).

Deliverable: fix(privacy) PII redaction и backup rules.

### День 3 (3 ч). FileLoggingTree в Channel. ЗАКРЫТ.

Channel<LogEntry>(UNLIMITED) плюс consumer на Dispatchers.IO.
trySend из log(), close() при остановке.
Адаптировать 5 тестов, плюс 1 новый.

Deliverable: perf(logging) Channel-based FileLoggingTree.

### День 4 (2-2.5 ч). DI, architecture test, LocaleService, release config

Снять @Singleton с ProjectRepositoryImpl:32, SessionRepositoryImpl:31.
AppErrorMapper: заменить NetworkError на IoError (см. п.9 раздела 14 — конфликт маппингов).
LocaleService в LocaleService: 9 файлов.
Архитектурный тест DomainLayerTest — не существует, создать в app/src/test.
ProGuard: isShrinkResources = true, Room entity keep, assembleRelease.
FGS type: AndroidManifest.xml:40, dataSync в specialUse плюс property PROPERTY_SPECIAL_USE_FGS_SUBTYPE.

Deliverable: refactor(di) убрать @Singleton, architecture test, release config.

### День 5 (3 ч). WHPX и миграция 2 к 3. ОТЛОЖЕН (нет USB/эмулятора).

PowerShell от админа: Enable-WindowsOptionalFeature -Online -FeatureName HypervisorPlatform -All.
Reboot. Эмулятор Pixel 6 API 33.
.\gradlew.bat connectedDebugAndroidTest.

Deliverable: test(db) прогон Migration2To3Test.

Fallback: WHPX не ставится — TECHDEBT, перенос.

### День 6 (4-5 ч). Разбиение стандартов, Provider-модель. ЗАКРЫТ.

Шаг 1 (1.5 ч). Раздел 4 coding-standards.md — модель Provider.

Шаг 2 (2.5 ч). Разбиение на 5 артефактов. Удалить coding-standards.md. Переименовать scripts/check_standards.py в scripts/_archive/check_standards_legacy.py.

Шаг 3 (0.5 ч). В PHILOSOPHY.md и ROADMAP.md — раздел «Gateway — план появления».

Правки контента: убрать запрет backtick, DI-Logger при 3 плюс, оговорки для when-по-sealed и Composable, чек-лист 5 плюс 10, DELETE-идемпотентность, раздел 12.6 AppErrorMapper убрать.

Deliverable: docs(architecture) ревизия v5.0.

### День 7 (3 ч). README по папкам, ретроспектива. ЗАКРЫТ.

README по 10 папкам, 5 строк каждый.
Корневой README — ссылки на PHILOSOPHY, STANDARDS, CONTRIBUTING, ROADMAP.
Ретроспектива и план на неделю 2.

Deliverable: docs(readme) README по папкам.

---

## 18. МЕТРИКА УСПЕХА ЧЕРЕЗ 7 ДНЕЙ

PII в логах: есть — нет.
FileLoggingTree блокирует: да — нет.
Миграция 2 к 3 прогнана: нет — да.
@Singleton на impl: 2 — 0.
AppErrorMapper: есть конфликт — устранён.
LocaleService: Manager — LocaleService.
coding-standards.md: 535 — 0, разбит.
Detekt: нет — есть, готово.
Pre-commit hook: нет — есть, готово.
Архитектурных тестов: 0 — 1 плюс.
README по папкам: 1 — 1 плюс 10.
.gitattributes: нет — есть, готово.
.githooks/pre-commit: нет — есть, готово.
HANDOVER.md в репо: нет — есть, готово.
ProGuard rules: есть — есть, не блокер.
FGS type: dataSync — specialUse.

---

## 19. ЧТО НЕ ДЕЛАТЬ

Firebase не начинать (Фаза 3, 4-6 недель).
Gateway не вводить (даже тонкий).
Provider в код не вводить — источников пока один.
TimerManager в TimerService не рефакторить (техдолг).
Backtick-тесты не переписывать (спорное правило).
Тесты DataExporter и DataImporter — неделя 2.
Feature flags, Security, Performance — после релиза.

---

## 20. ПРАВИЛА РАБОТЫ С ИИ

1. Не доверять памяти — верифицировать grep или файлом.
2. Сборка до коммита.
3. Python плюс patch с expected. cat с heredoc — только без кириллицы.
4. Heredoc-маркеры проверять.
5. Кириллица — только UTF-8 через python.
6. git status --short до git add.
7. Коммит — одна операция.
8. Проверка после патча — KDoc, сигнатуры, git status.

Правила для ИИ:

Строгий review, без комплиментов.
Полные файлы, не diff.
Обоснование решений.
Реальное состояние (cat, grep, git status) перед правками.
Не «ты абсолютно прав» без обоснования.
Не соглашаться со спорным — аргументировать.
Не давать код без разведки фактов.
Не путать ServiceProvider, Provider, Gateway.
Не начинать без факт-блока.

---

## 21. ПРОФИЛЬ ПОЛЬЗОВАТЕЛЯ

decilya — backend-разработчик (Symfony, Laravel), пишет Android впервые, думает как архитектор. Стремится к подобию Symfony/Laravel. Работает в WSL и PowerShell. Проект соло, цель — читаемость через год и защита от ИИ-дрейфа.

Ценит: конкретику, критику без комплиментов, обоснование, честное «не знаю», аргументированный спор.

Не любит: «ты абсолютно прав» без обоснования, код без разведки, обещания «за 5 минут», игнорирование PII и Security.

---

## 21.5. СОХРАНЕНИЕ HANDOVER В РЕПОЗИТОРИЙ

Этот документ — HANDOVER.md в корне репозитория. Обновляется в конце каждой сессии.

Workflow:

1. Начало сессии: открыть HANDOVER.md из репозитория.
2. Работа по плану.
3. Конец: обновить HANDOVER.md — HEAD, тесты, что закрыто, что перенесено, новые P0/P1, фокус следующей сессии.
4. Если в сессии был код — обновить HANDOVER.md в том же коммите,
   что и код (шапка: HEAD, тесты; раздел 15; при необходимости раздел 14).
   Если сессия — только верификация без правок кода (прогон тестов,
   migration-тест) — отдельный коммит docs(handover). Push.
5. Передача: скопировать актуальный HANDOVER.md из репозитория.

Почему: восстановление из истории чата ненадёжно. HANDOVER.md — единственный источник истины.

---

## 22. СТРУКТУРА ФАЙЛОВ

Корень репозитория:

README.md — описание (UTF-8).
ROADMAP.md — план v1.0.
HANDOVER.md — этот документ.
.gitattributes — LF/CRLF (День 1).
.gitignore — дополнен (День 1).
.githooks/pre-commit — (День 1).

docs/architecture:

coding-standards.md — 535 строк, будет разбит (День 6).
vo-pattern.md — паттерн VO.

config/detekt:

detekt.yml — (День 1).
baseline.xml — 9 записей (День 1).

scripts:

check_standards.py — будет архивирован (День 6).

app/src/main/java/com/knittrac/app:

KnitTracApplication.kt.
MainActivity.kt.
core — base (BaseUseCase, BaseFlowUseCase), common (AppError, Result, SyncStatus, AppErrorMapper), di (DataStoreModule, DispatchersModule), localization (LocaleService, CategoryLocalizer), logging (FileLoggingTree).
data — dto, local (entity, dao, AppDatabase), mapper, repository, di.
domain — entity, repository, service.
platform — timer (SystemTimeProvider), service (TimerForegroundService), notification, di.
presentation — feature_projects, feature_settings, feature_stats, feature_timer, common.

app/src/test — 132 теста.

app/proguard-rules.pro — Gson, Coroutines, attributes (не пуст).

app/build.gradle.kts.
build.gradle.kts — root, detekt plugin.
settings.gradle.kts.

gradle/libs.versions.toml — НЕ существует.

---

## 23. ОТКРЫТЫЕ ВОПРОСЫ И ТЕХДОЛГ

1. Detekt и ktlint — оба (detekt это AST, ktlint это форматирование).
2. Структура PHILOSOPHY.md — 500 строк верхняя оценка.
3. coding-standards.md — удалить или архивировать.
4. Firebase project — создавать на Фазе 3, не раньше.
5. Signing config JKS — День 5 или 7.
6. Play Console account — проверить до Фазы 5.

Техдолг (расширение detekt.yml, P2 после релиза):

Включить ForbiddenComment (TODO без issue).
Включить TooGenericExceptionCaught.
Включить SwallowedException.
Каждое правило — отдельный коммит с анализом нарушений.

---

## 24. ЧТО ДЕЛАТЬ СЕЙЧАС

Следующая сессия: Неделя 2, День 2 — рефакторинг суффиксов Manager (TimerManagerImpl -> TimerService).

Первый шаг — факт-блок. Один прогон в WSL:

cd /mnt/c/Users/decilya/projects/KnitTrac
echo "---LOG---"; git log --oneline -3
echo "---STATUS---"; git status --short
echo "---BUILD-RELEASE---"; sed -n "/buildTypes/,/^    }/p" app/build.gradle.kts
echo "---PROGUARD-HEAD---"; head -5 app/proguard-rules.pro
echo "---MANIFEST-SVC---"; grep -n -A 4 "<service" app/src/main/AndroidManifest.xml
echo "---SINGLETON-IMPL---"; grep -rn -B 1 "@Singleton" app/src/main/java/com/knittrac/app/data/repository/
echo "---LOCALE---"; grep -rln "LocaleService" app/src/
echo "---END---"

После вывода — День 4 собирается за один заход.

Без факт-блока — не начинать. Не выдумывать содержимое. Не доверять памяти.

---

КОНЕЦ HANDOVER.

Составлен 16.10.2026. HEAD f8d1d1c. Тесты 133, 0 failures.
