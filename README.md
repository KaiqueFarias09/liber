# Liber Project Documentation

## 1. Project Overview & Goals

**Liber** is a modern, single-module Android application (`:app`) built to serve as a comprehensive
local digital library and media player. It is developed entirely using **Kotlin**, **Jetpack Compose
**, **Room**, and **Dagger Hilt**.

**What it aims to achieve:**
Liber strives to provide a unified, seamless user experience for managing, reading, and listening to
digital literature. By leveraging robust underlying engines like the Readium Toolkit for eBooks and
AndroidX Media3 for audio, the app is designed to parse complex local media (including directories
containing audio files), manage reading progress, handle metadata extraction, and render media
through a highly customized, declarative UI without relying on legacy Android XML layouts.

---

## 2. Supported File Formats

Liber acts as a robust media scanner and importer, specifically targeting eBooks and various audio
encodings.

| Category                             | Formats / Extensions                            | MIME Type / Recognition                                                                               |
|:-------------------------------------|:------------------------------------------------|:------------------------------------------------------------------------------------------------------|
| **eBooks**                           | `.epub`                                         | `application/epub+zip`                                                                                |
| **Audiobooks (High-Fidelity)**       | `.flac`, `.wav`                                 | `audio/flac`, `audio/wav`                                                                             |
| **Audiobooks (Standard/Compressed)** | `.mp3`, `.m4a`, `.m4b`, `.aac`, `.ogg`, `.opus` | `audio/mpeg`, `audio/mp4`, `audio/aac`, `audio/ogg`                                                   |
| **Audiobooks (Mobile/Voice)**        | `.amr`, `.awb`, `.3gp`, `.mka`                  | `audio/amr`, `audio/amr-wb`, `audio/3gpp`, `audio/x-matroska`                                         |
| **Directory Structures**             | `Folders/Directories`                           | The app can scan entire directories for supported audio formats to create unified audiobook entities. |

---

## 3. Architecture

Liber utilizes an **MVVM (Model-View-ViewModel)** architecture paired with a reactive data flow
pattern via Kotlin's `Flow` and `StateFlow`.

* **Dependency Injection:** The app strictly uses **Dagger Hilt** (`v2.59.2`). Manual DI is
  prohibited. Dependencies are resolved through dedicated modules like `DatabaseModule.kt` and
  `RepositoryModule.kt`.
* **Hybrid Navigation:** * **Tab-Level Navigation:** Handled by Jetpack Navigation Compose for
  primary screens like Home, Library, and Settings.
    * **Immersive Overlays:** Core reading and listening experiences (EPUB Reader, Audiobook Player)
      bypass standard routing. They use State-Driven Overlays managed by `LiberAppViewModel` to
      render on top of the navigation host, preserving background UI state seamlessly.
* **Data Layer:** Powered by **Room** database (currently at v11). DAOs return reactive
  `Flow<List<T>>` streams, while Repositories serve as thin, injected wrappers to orchestrate data.
  User preferences are managed through AndroidX Datastore.

---

## 4. Key Features & Implementation

* **EPUB Engine (Readium):** Uses Readium Toolkit (`v3.1.2`) for parsing EPUBs, managing LCP (DRM),
  and rendering text via a fragment-based navigator integrated into Compose through `AndroidView`/
  `FragmentContainerView`.
* **Audiobook Subsystem (Media3):** Relies on AndroidX Media3 (`v1.10.0`) utilizing an ExoPlayer and
  `MediaSession` inside a background `PlaybackService.kt`. UI components connect directly to the
  session to track playback states.
* **Media Parsing & Hashing:** The `BookImporter` utilizes `MediaMetadataRetriever` to extract
  embedded album/artist tags and cover art. It assigns unique content IDs using SHA-256 hashing for
  data consistency.
* **Network Foundations:** Set up for potential API consumption using Retrofit (`v3.0.0`), OkHttp (
  `v5.3.2`), and Kotlinx Serialization (Gson/Moshi are avoided).

---

## 5. Common Design & Coding Patterns

### State Wrapping

Data fetches and asynchronous operations must be wrapped in a sealed `UiState` class (
`UiState.Loading`, `UiState.Success`, `UiState.Error`). This forces UIs (like the `LibraryScreen`)
to reactively handle loading states and error messages gracefully.

### Compose UI Hoisting

Screens receive the ViewModel directly, extract state via `StateFlow`, and pass raw data and
functional lambdas downward to completely stateless `@Composable` child components. Complex
conditional logic is delegated to separate private files or `components/` subfolders.

### Strict Design System

* **No XML:** The app relies entirely on Compose declarative UI (excluding launcher/manifest XML).
* **Typography:** Hardcoded fonts are banned. Developers must use `MaterialTheme.typography.*` which
  implements the customized *Gambetta* (display/serif) and *Switzer* (body/sans-serif) typefaces.
* **Iconography:** Material Icons are prohibited. The app mandates the use of **Phosphor Icons** (
  e.g., `PhosphorIcons.Regular.BookOpen`).
* **Colors:** Semantic and custom colors must be routed through `ExtendedColorScheme` via
  `LocalExtendedColors.current` rather than using direct hex values.

---

## 6. Key Dependencies

Below are the primary core libraries that drive the Liber project framework:

| Dependency              | Version      | Purpose                          |
|:------------------------|:-------------|:---------------------------------|
| **Kotlin**              | `2.3.20`     | Core language                    |
| **Jetpack Compose BOM** | `2026.03.01` | Declarative UI framework         |
| **Dagger Hilt**         | `2.59.2`     | Dependency Injection             |
| **Room**                | `2.8.4`      | Local SQLite Persistence         |
| **Readium Toolkit**     | `3.1.2`      | EPUB / LCP DRM Engine            |
| **AndroidX Media3**     | `1.10.0`     | Background Audiobook Playback    |
| **Retrofit**            | `3.0.0`      | Network API Layer                |
| **Coil Compose**        | `2.7.0`      | Asynchronous image/cover loading |