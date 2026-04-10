# AGENTS.md

## Scope + source of truth

- This repo is a single-module Android app (`:app`) using Kotlin + Compose + Room; see
  `settings.gradle.kts` and `app/build.gradle.kts`.
- If guidance conflicts, trust current code first:
  `app/src/main/java/com/example/liber/ui/LiberApp.kt`,
  `app/src/main/java/com/example/liber/data/AppDatabase.kt`, then `CLAUDE.md`.

## Big picture architecture

- App entry is `MainActivity`; incoming `ACTION_VIEW` / `ACTION_SEND` intents are routed to
  `HomeViewModel.importAndOpenBook(...)` for direct-open flows.
- App-level navigation is state-driven (no Jetpack Navigation): `LiberAppViewModel` tracks
  `activeTab`, `activeBook`, `activePublication`, and reader overlay state.
- Tabs are `HOME`, `LIBRARY`, `SETTINGS` in `ui/navigation/AppTab.kt` (note: older docs mentioning
  only HOME/LIBRARY are outdated).
- Reader mode: `LiberApp.kt` handles EPUB => `ReaderScreen`.

## Data layer + persistence patterns

- `AppDatabase` is a singleton Room DB at **version 8** with inline migrations (`MIGRATION_1_2` ...
  `MIGRATION_7_8`) in `data/AppDatabase.kt`.
- Core entities now include `books`, `annotations`, `bookmarks`, `collections`, `book_collections`,
  and `scan_sources`.
- Repositories are thin DAO wrappers (`BookRepository`, `CollectionRepository`,
  `ScanSourceRepository`); UI state is `StateFlow`, DB streams are `Flow`.
- DB and file work run on `Dispatchers.IO` via `viewModelScope` (see `HomeViewModel`,
  `BookScanService`).

## Reader + annotation integration details

- EPUB reader uses Readium (`readium-shared/streamer/navigator/lcp`) and embeds
  `EpubNavigatorFragment` inside Compose (`ReaderScreen.kt`).

## Importing and scan workflows

- Manual import path: document picker in `LiberApp.kt` -> `HomeViewModel.loadBooksFromUris(...)` ->
  `BookImporter.parseBook(...)`.
- Folder scan path: Settings screen -> `BookScanService.buildIntent(...)` -> foreground scan with
  notification progress.
- Scan dedup is two-stage in `BookScanService`: URI check first, then `contentId` check after parse.
- `BookImporter` treats all non-audio books as EPUB-like parse path, computes a SHA-256
  hash fallback content ID, and generates/loads cover art into app cache.

## UI/system conventions specific to this repo

- Use **Phosphor Icons** only (`com.adamglin.phosphoricons.*`), not Material icons; see
  `ui/navigation/AppTab.kt` and `ui/components/*`.
- Use `MaterialTheme.typography` tokens from `ui/theme/Type.kt` (Gambetta for
  display/headline/titleLarge; Switzer for body/labels).
- Use semantic `MaterialTheme.colorScheme.*`; for badges/chips use `MaterialTheme.extendedColors.*`
  from `ui/theme/ExtendedColorScheme.kt`.
- Dependency wiring is manual (no DI framework); ViewModels/repositories are instantiated directly.

## Build/test commands used in this project

- `./gradlew assembleDebug`
- `./gradlew assembleRelease`
- `./gradlew test`
- `./gradlew connectedAndroidTest`
- `./gradlew lintDebug`
- `./gradlew clean`

## Current testing reality

- Test scaffolding exists, but only template tests are present:
  `app/src/test/.../ExampleUnitTest.kt` and `app/src/androidTest/.../ExampleInstrumentedTest.kt`.

