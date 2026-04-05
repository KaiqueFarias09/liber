# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
./gradlew assembleDebug          # Build debug APK
./gradlew assembleRelease        # Build release APK
./gradlew test                   # Run unit tests
./gradlew connectedAndroidTest   # Run instrumented tests (requires device/emulator)
./gradlew lintDebug              # Run lint checks
./gradlew clean                  # Clean build outputs
```

## Architecture

**Liber** is an Android EPUB reader app using MVVM + Jetpack Compose.

### Layers

- **`data/`** — Room database (v4), DAOs, entities (`BookEntity`, `AnnotationEntity`), and the `Book` domain model. `AppDatabase` is a singleton.
- **`ui/`** — All Compose UI: screens, ViewModels, reusable components, and theme.

### Navigation

Navigation lives in `LiberApp.kt` — it uses simple Compose state (`remember`/`mutableStateOf`) to switch between tabs (`AppTab` enum: HOME, LIBRARY) and overlay the reader. There is no Jetpack Navigation library; the Reader screen renders on top as a modal state.

### EPUB Reading

The reader is built on the [Readium Kotlin Toolkit](https://readium.org/kotlin-toolkit/) (v3). Key classes:
- `ReaderViewModel` — opens the publication, manages the `EpubNavigatorFragment`, tracks locator/progress, handles annotation requests.
- `ReaderScreen.kt` — hosts the `FragmentContainerView` (Readium requires a `Fragment`), bridges Compose and the Fragment world.
- Annotations use the Readium **Decorator API** to render highlights in the navigator.

### State Management

- ViewModels expose `StateFlow<T>` for UI state.
- Room DAOs return `Flow<List<T>>` for reactive database queries.
- All DB/IO work runs on `Dispatchers.IO` via `viewModelScope`.

### Dependency Injection

Manual — no DI framework. Instances are created directly in Activities/ViewModels. `ReaderViewModel` uses a custom `Factory` that takes a `BookEntity` argument.

## Key Dependencies

| Library | Purpose |
|---|---|
| Readium Kotlin Toolkit 3.1.2 | EPUB parsing, rendering, DRM |
| Room 2.8.4 | Local SQLite database |
| Jetpack Compose BOM 2026.03.01 | Declarative UI |
| Material 3 | Design system |
| Coil 2.7.0 | Book cover image loading |
| Phosphor Icons 1.0.0 | UI icons |
| KSP 2.1.0-1.0.29 | Annotation processing (Room) |

## Database

Room database at version 4. Migrations are defined inline in `AppDatabase.kt`. When adding new columns or tables, add a migration — the database uses `fallbackToDestructiveMigration` only in development.

**Tables:**
- `books` — metadata, file URI, cover path, reading progress, last locator (JSON)
- `annotations` — highlights/notes with CASCADE delete on book removal
