# Repository Guidelines

## Project Structure & Module Organization
- `toolkit/`: KMP library module (app, device, clipboard, network, keyboard, haptics, sensors, sharing, navigation, and file APIs). Source in `toolkit/src/*Main` and tests in `toolkit/src/commonTest`, `toolkit/src/androidHostTest`, and `toolkit/src/androidDeviceTest`.
- `composeApp/`: Demo KMP app using the library. Android entry points in `composeApp/src/androidMain`, shared UI in `composeApp/src/commonMain`, iOS entry in `composeApp/src/iosMain`.
- `iosApp/`: Xcode project wrapper for running the iOS app.
- `gradle/` and `gradle/libs.versions.toml`: build logic and dependency versions.

## Build, Test, and Development Commands
- `./gradlew build`: Builds all modules and runs unit tests where applicable.
- `./gradlew :composeApp:assembleDebug`: Builds the Android demo APK.
- `./gradlew :toolkit:testAndroidHostTest`: Runs Android host tests.
- `./gradlew :toolkit:connectedAndroidTest`: Runs Android device tests in `toolkit/src/androidDeviceTest` (requires emulator/device).
- iOS app: open `iosApp/iosApp.xcodeproj` in Xcode and run the scheme.

## Coding Style & Naming Conventions
- Kotlin style is standard/official (`kotlin.code.style=official`). Use 2-space indentation in Gradle Kotlin scripts and 2–4 spaces in Kotlin per IDE defaults.
- Package naming follows reverse-DNS, e.g., `com.airsaid.toolkit` and `com.airsaid.toolkit.demo`.
- Prefer clear, descriptive file/class names that mirror responsibilities (e.g., `NetworkMonitor`, `ClipboardToolkit`).

## Testing Guidelines
- Common tests use `kotlin-test` in `commonTest` source sets.
- Android instrumentation tests live under `toolkit/src/androidDeviceTest` and use AndroidX test + JUnit.
- Name tests with `*Test` suffix (e.g., `DiskLogStrategyBufferTest`).

## Commit & Pull Request Guidelines
- Use an English, imperative, concise subject line.
- Start the subject with a capital letter and do not end it with a period.
- Do not use Conventional Commits prefixes such as `feat:`, `fix:`, or `refactor(scope):`.
- Good examples: “Add file picker save options”, “Remove demo app logo preview component”, “Support iOS keyboard visibility monitoring”.
- Keep commits focused by module and behavior.
- PRs should include: a short description, affected modules, and test results (commands + outcomes). For UI changes, add screenshots from Android and iOS.

## Documentation Guidelines
- When updating `README.md`, sync the same changes in other language variants (e.g., `README.zh.md`) to keep content aligned.

## Security & Configuration Tips
- `local.properties` is intentionally ignored; it contains machine-specific paths like the Android SDK. Do not commit secrets, tokens, or signing configs.
