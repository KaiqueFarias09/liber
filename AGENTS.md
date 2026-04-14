# AGENTS.md

This document is the ultimate source of truth for the **Liber** Android codebase. It supersedes older architectural documentation. If instructions conflict, trust the current code structure (specifically Hilt modules, `AppNavHost.kt`, and `AppDatabase.kt`).

## 1. Scope & Core Architecture

Liber is a single-module Android application (`:app`) built entirely with **Kotlin**, **Jetpack Compose**, **Room**, and **Hilt**. It uses an **MVVM (Model-View-ViewModel)** architecture with a reactive data flow pattern (`Flow`/`StateFlow`).

### The Big Shift (Recent Architectural Changes)

- **Dependency Injection:** We now use **Dagger Hilt** extensively. Manual DI has been removed. All ViewModels use `@HiltViewModel`, and dependencies are provided via `di/DatabaseModule.kt` and `di/RepositoryModule.kt`.
    
- **Navigation:** We now use a **Hybrid Navigation Approach**.
    
    - Tab-level navigation (Home, Library, Settings) uses **Jetpack Navigation Compose** (`AppNavHost.kt`).
        
    - Immersive screens (EPUB Reader, Audiobook Player) use **State-Driven Overlays** managed by `LiberAppViewModel` to render on top of the `NavHost` without losing background state.
        
- **Audiobooks:** A major new subsystem powered by **Media3** has been integrated, requiring new database columns (`mediaType`, `durationMillis`, `narrator`, `tracksJson`).
    
- **Network Layer:** **Retrofit 3** and **Kotlinx Serialization** have been added to the project, setting the foundation for future network/API integrations.
    

---

## 2. Tech Stack & Key Libraries

|**Category**|**Libraries & Versions**|**Notes for Agents**|
|---|---|---|
|**Core UI**|Jetpack Compose (BOM 2026.03.01)|Strictly declarative. No XML layouts except launcher/manifest.|
|**Architecture**|ViewModels, `UiState` wrapper|Always expose state via `StateFlow`. Use `UiState<T>` for async loads.|
|**DI**|Dagger Hilt 2.59.2|Annotate ViewModels with `@HiltViewModel`. Add Repos to `RepositoryModule.kt`.|
|**Navigation**|Navigation Compose 2.9.7|Route definitions live in `AppRoute` object (`AppNavHost.kt`).|
|**Persistence**|Room 2.8.4, Datastore Prefs 1.2.1|DB is at **Version 11**. Always write migrations in `AppDatabase.kt`.|
|**Media/Audio**|AndroidX Media3 1.10.0|ExoPlayer & MediaSession used for Audiobook playback.|
|**EPUB Engine**|Readium Toolkit 3.1.2|Used for parsing, LCP (DRM), and rendering text via `FragmentContainerView`.|
|**Network**|Retrofit 3.0.0, OkHttp 5.3.2|Uses `kotlinx.serialization.json` (no Gson/Moshi).|
|**Images**|Coil Compose 2.7.0|Used for async cover art loading.|
|**Icons**|Phosphor Icons 1.0.0|**DO NOT use Material Icons.** Use `com.adamglin.phosphoricons.*`.|

---

## 3. Data Layer & Persistence Patterns

- **AppDatabase (Singleton):** Room database currently at **v11**. Contains `books`, `annotations`, `bookmarks`, `collections`, `book_collections`, and `scan_sources`.
    
- **Migrations are Mandatory:** Do not rely on destructive migrations in production. If you add a column, write a `MIGRATION_X_Y` block in `AppDatabase.kt` and add it to `.addMigrations()`.
    
- **DAOs & Repositories:** * DAOs (`BookDao`, `CollectionDao`) return `Flow<List<T>>` for reactive streams or `suspend` for one-shot operations.
    
    - Repositories (`BookRepository`, etc.) are thin wrappers injected via Hilt. Do not place heavy business logic in Repositories; keep them focused on data orchestration.
        
- **Datastore:** User preferences (themes, reader settings) are migrated to AndroidX Datastore Preferences (`UserPreferencesRepository`).
    

---

## 4. Feature Planning: How to Add New Features

When conceptualizing or adding a new feature, follow this strict directory/package structure and separation of concerns:

### Step 1: Data Modeling (`data/`)

- Add Room `@Entity` classes in `data/model/`.
    
- Add queries in `data/local/*Dao.kt`.
    
- Update `AppDatabase.kt` with a new migration and version bump.
    
- Expose the data via a Repository in `data/repository/` and provide it in `di/RepositoryModule.kt`.
    

### Step 2: ViewModel (`feature/your_feature/`)

- Create a `YourFeatureViewModel` annotated with `@HiltViewModel`.
    
- Inject required Repositories via constructor.
    
- Expose a single source of truth for the UI using `MutableStateFlow` or `SharingStarted.WhileSubscribed`.
    
- **Pattern:** Wrap asynchronous data loads in a `UiState` sealed class (`UiState.Loading`, `UiState.Success`, `UiState.Error`). Example seen in `LibraryScreen.kt`.
    

### Step 3: UI Layout (`feature/your_feature/`)

- Create `YourFeatureScreen.kt`.
    
- Follow standard Compose hoisting: the Screen component receives the ViewModel, extracts state, and passes raw data and lambdas down to stateless child `@Composable` components.
    
- Keep complex conditional UI logic (like empty states, loading spinners, grids) in separate private components within the same file or in a `components/` subfolder.
    

### Step 4: Navigation (`core/navigation/`)

- If it's a primary screen, add a new route to `AppRoute` in `AppNavHost.kt` and register the `composable()`.
    
- If it's an immersive/overlay screen (like a PDF reader), add state to `LiberAppViewModel` and overlay it conditionally in `LiberApp.kt`.
    

---

## 5. Coding Patterns & UI Design System

### Handling State (`UiState`)

Always wrap data fetches. `LibraryScreen` is a perfect example of this:

Kotlin

```
when (booksState) {
    is UiState.Loading -> LoadingState()
    is UiState.Error -> ErrorState(booksState.message)
    is UiState.Success -> { /* render grids/lists */ }
}
```

### Design System Rules

- **Typography:** Use `MaterialTheme.typography.*`. Custom fonts (Gambetta for serif/display, Switzer for sans-serif/body) are pre-configured. Never hardcode fonts.
    
- **Colors:** Use `MaterialTheme.colorScheme.*`. For non-standard semantic colors (e.g., specific tags or highlight colors), rely on `ExtendedColorScheme` via `LocalExtendedColors.current`.
    
- **Icons:** Use `PhosphorIcons`. Example: `imageVector = PhosphorIcons.Regular.BookOpen`.
    
- **Components:** Re-use established design system components located in `core/designsystem/` (e.g., `LiberHeader`, `LiberTabBar`, `LiberButton`, `BookGrid`, `EmptyState`).
    

---

## 6. Reader & Audiobook Integration Details

- **Readium (EPUB):** The EPUB reader integrates Readium's Fragment-based navigator inside Compose using AndroidView/FragmentContainerView (in `ReaderScreen.kt`). Annotations are handled via Readium's Decorator API.
    
- **Media3 (Audiobooks):** Audiobooks use `PlaybackService.kt` running in the background. The DB tracks `durationMillis` and `tracksJson`. UI components (`AudioPlayerScreen`, `NowPlayingBar`) connect to the Media3 session to react to playback state changes.
    
- **Importers:** `BookImporter.kt` handles URI parsing, hashing for unique `contentId`, cover art extraction (saving to local cache for Coil to load), and dispatching data to Room.
    

---

## 7. Build & Test Commands

- `./gradlew assembleDebug` — Build standard testing APK.
    
- `./gradlew lintDebug` — Run static analysis (configured to output SARIF and HTML in `build.gradle.kts`).
    
- `./gradlew test` — Run local JUnit unit tests.
    
- `./gradlew connectedAndroidTest` — Run Compose/instrumented tests. _(Note: Test scaffolding is minimal right now, but standard JUnit4/Espresso/Compose rules apply when writing them)._