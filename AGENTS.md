# AGENTS.md

Android home-screen launcher that recreates the Windows 10 Mobile Start screen (Compose, single module `:app`).

## Critical traps

- **Use JDK 21 to build.** JDK 26 makes AGP's `JdkImageTransform` fail (`jlink` on `core-for-system-modules.jar`). Set `JAVA_HOME` to a 17/21 JDK (e.g. `/opt/homebrew/opt/openjdk@21/...`) and pass `--no-configuration-cache`; the config cache also fails to serialize the Java compile task.
- **Active source is `app/src/main/java/com/ab/**`.** The stale `com.example` duplicate tree was deleted; do not recreate it. The manifest resolves `.MainActivity` against the `com.ab` namespace.
- Application id (`com.aistudio.win10start.xkqmvy`, AI Studio export) differs from namespace (`com.ab`). Tests assert `BuildConfig.APPLICATION_ID`, not the package name.
- Local builds need a gitignored `local.properties` (`sdk.dir`) and `debug.keystore`; CI provides its own.

## Build / test commands

```
./gradlew assembleDebug          # build debug APK
./gradlew testDebugUnitTest      # Robolectric unit tests (no device)
./gradlew connectedAndroidTest   # instrumented tests (device/emulator required)
./gradlew lint
```

- Run one unit test: `./gradlew testDebugUnitTest --tests "com.ab.ExampleRobolectricTest"`.
- Unit tests use Robolectric pinned to `@Config(sdk = [36])`; `isIncludeAndroidResources = true`.
- `gradle.properties` sets `org.gradle.configuration-cache=true` and `kotlin.compiler.execution.strategy=in-process` — do not "fix" these for normal IDE builds; pass `--no-configuration-cache` on the CLI when it conflicts with the Java compile task.

## Toolchain

- AGP 9.1.1, Gradle 9.8.0 (wrapper), Kotlin 2.2.10, KSP, Compose BOM 2024.09.00, `compileSdk 36`/`minSdk 24`/`targetSdk 36`.
- Dependencies come from the version catalog `gradle/libs.versions.toml` — add deps as `libs.*` aliases, not literal coordinates. Many are pre-declared but commented out in `app/build.gradle.kts`; uncomment instead of adding.
- Room and Moshi both use KSP codegen (`ksp(...)`).

## Config / secrets

- `secrets-gradle-plugin` reads `.env` (falls back to `.env.example`); `GEMINI_API_KEY` stays commented out so it is not packaged.
- `googleServices { missingGoogleServicesStrategy = WARN }`, so a missing `google-services.json` does not fail the build.
- Release signing pulls `KEYSTORE_PATH` / `STORE_PASSWORD` / `KEY_PASSWORD` from env (default keystore `my-upload-key.jks`); debug uses gitignored `debug.keystore`.

## Architecture notes

- MVVM + Compose. Entry point `com.ab.MainActivity`; state in `com.ab.ui.viewmodel.LauncherViewModel`; UI under `com.ab.ui` (`screens/`, `components/`, `theme/`).
- Settings: `com.ab.ui.settings` — `SettingsDestination`/`settingsBackStack` drive navigation (root list -> swipeable pivot shell -> nested pages), reusable controls live in `settings/components/MetroSettingsControls.kt`, and `SettingsSearchIndex` powers local search. `LauncherViewModel.handleInternalBack()` handles Back priority.
- Preferences: `com.ab.data.LauncherPreferences` (DataStore, name `win10_launcher_prefs`), with pinned tiles serialized to JSON carrying a `schemaVersion`. Update `parseTilesJson` when changing `TileModel`.
- Live tiles: `com.ab.livetile` — `LiveTileRegistry` maps a component to a `LiveTileProvider`, `LiveTileManager` polls providers and caches `LiveTileState`, `LiveTileScheduler` staggers face flips (interval follows `LiveTileAnimationFrequency`, min 1s).
- Media live tiles rely on `com.ab.media.LauncherNotificationListenerService` (declared in the manifest) plus user-granted notification access; `MediaSessionRepository` bridges `MediaSessionManager` to tile state.
- Launcher needs `QUERY_ALL_PACKAGES` to enumerate apps via `LauncherApps`.

## Repo facts

- Only CI is `.github/workflows/release-apk.yml` (manual `workflow_dispatch`, main-only signed release APK). No pre-commit hooks or lint/format scripts.
- Commit style: Conventional Commits (`feat: ...`).
