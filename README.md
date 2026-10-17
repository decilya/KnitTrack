# KnitTrac

> Time tracking for knitters, crocheters, and other fiber crafters.

KnitTrac is an Android app for tracking the time you spend on knitting, crochet, beadwork, and macrame. It is built for people who work on a project over weeks or months and want to know where their time goes - without juggling a spreadsheet.

The timer keeps running in the background, sessions are stored locally, and daily statistics show how a project progresses. When the sync feature lands, your projects will follow you between devices.

[Русская версия](README.ru.md)

## Features

**A timer that survives everything.**
The timer is based on monotonic time (`elapsedRealtime`), not wall clock. NTP adjustments, manual clock changes, and daylight-saving transitions do not distort the elapsed time. It runs as a foreground service, so it keeps going when you leave the app.

**Projects with categories.**
Every project has a name, a category (knitting, crochet, beading, macrame), and a global row counter. The counter is on the project level, not the session level: you can pick up the same scarf tomorrow and continue where you left off.

**Sessions and daily statistics.**
Each timer run creates a session with a start time, duration, and optional notes. Sessions aggregate into per-day statistics with a chart, so you can see how much time you actually spent this week.

**Local-first.**
All data lives in Room (SQLite) on the device. The app works fully offline. Export and import use streaming JSON (`JsonWriter` / `JsonReader`) so even large backups do not blow up memory.

**Localization.**
Russian and English out of the box. Adding a new language means editing `arrays.xml` and one strings file - no Kotlin changes.

**Sync (in progress).**
Two-way synchronization between a user's devices via Firebase Firestore is on the roadmap for v1.0. The local schema is already prepared: `client_id`, `server_updated_at`, `sync_version`, and soft-delete fields exist on every row.

## Screenshots

Screenshots will be added before the public release.

## Tech stack

| Layer | Choice | Why |
|---|---|---|
| Language | Kotlin 1.9.22 | Modern Android; coroutines built in. |
| UI | Jetpack Compose (BOM 2024.02.00) | Declarative UI; less boilerplate than XML. |
| Local DB | Room 2.6.1 | Type-safe SQL over SQLite. |
| Preferences | DataStore Preferences 1.0.0 | Replaces SharedPreferences; Flow-based, no ANRs. |
| DI | Hilt 2.50 | Single container; compile-time checked via KSP. |
| JSON | Gson 2.10.1 | Mature; DTOs are versioned with @SerializedName. |
| Logging | Timber 5.0.1 | Tree-based; swap trees per build type. |
| Charts | ycharts 2.1.0 | Compose-native charts for daily stats. |
| Lint | Detekt 1.23.7 | AST-based rules; regex-only checks were not enough. |
| Build | AGP 8.6.0, Gradle 8.7, JDK 17 | Pinned deliberately. Gradle 8.7 breaks on JDK 21+. |

Versions are declared inline in `build.gradle.kts`. There is no `gradle/libs.versions.toml`.

## Building

Requirements:

- JDK 17 as the Gradle JDK in Android Studio (Settings, Build, Execution, Deployment, Build Tools, Gradle). JDK 21 or 25 will break Gradle 8.7.
- Android SDK 35.
- Build from Android Studio Terminal (PowerShell), not from a WSL shell.

Commands:

    cd C:\Users\decilya\projects\KnitTrac
    .\gradlew.bat clean test
    .\gradlew.bat assembleDebug
    .\gradlew.bat detekt

`adb` is not on PATH. Launch emulators via Android Studio, Device Manager.

## Architecture

Layers and dependency direction:

    Presentation (Compose, MVI)
        down
    ViewModel (Action / State / Effect)
        down
    Domain (UseCase, Result, Value Objects)
        down
    Data (Repositories, Room, DataStore, DTO)
        down
    Platform (Timer, FGS, Notification)
        down
    Core (Result, AppError, Dispatchers, TimeProvider)

Conventions:

- DIP. Repository interfaces in `domain/repository`, implementations in `data/repository`. The domain layer does not import Android.
- Value objects. `Duration`, `ProjectName`, `ThemeMode` are immutable with private constructors and safe factories (`create`, `createOrNull`, `createOrDefault`).
- MVI. One-way flow. Effects are emitted via `_effect.emit` and consumed as `Channel` or `SharedFlow` in the screen.
- Error handling. Every `try` block catches `CancellationException` first and rethrows it. Cancelling a coroutine is a signal, not an error.

A detailed standard (Clean Architecture, DDD, SOLID, Symfony-inspired layout, Clean Code) is in `docs/architecture/coding-standards.md`.

## Testing

134 unit tests (JUnit 4 + MockK).

    .\gradlew.bat clean test

XML report: `app/build/test-results/testDebugUnitTest/`.

Instrumentation tests (Room migrations, DAO behavior) live in `app/src/androidTest` and require a running emulator:

    .\gradlew.bat connectedDebugAndroidTest

## Lint

Detekt 1.23.7.

    .\gradlew.bat detekt

Config: `config/detekt/detekt.yml`. Existing violations are frozen in `config/detekt/baseline.xml`. New violations fail the build.

## Pre-commit hook

`.githooks/pre-commit` runs `gradlew.bat test detekt` before each commit. Enable once:

    git config core.hooksPath .githooks

The hook is WSL-safe: it invokes Windows `gradlew.bat` through `cmd.exe`, not bash `./gradlew`. Reason: Gradle writes into `.gradle/` on `/mnt/c` through drvfs, and a Windows antivirus scanner intermittently blocks files mid-write, producing `Input/output error`.

## Documentation

- `ROADMAP.md` - the v1.0 plan: scope, sync, milestones.
- `HANDOVER.md` - project context passed between sessions.
- `docs/architecture/coding-standards.md` - the code standard.

## Contributing

The project is a solo effort at the moment. Issues and pull requests are welcome, but please open an issue first so we can agree on scope before you write code.

## License

TBD.


## Документация
- [Философия проекта](docs/architecture/PHILOSOPHY.md)
- [Стандарты](docs/architecture/STANDARDS.md)
- [Техдолг](docs/architecture/TECHDEBT.md)
- [Процесс разработки](docs/architecture/CONTRIBUTING.md)
- [План разработки](ROADMAP.md)
