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
- `./gradlew :toolkit:lint --no-parallel`: Runs Android lint for the library module. Use `--no-parallel` to avoid intermittent AGP/KMP lint classpath races.
- `./gradlew :composeApp:lint --no-parallel`: Runs Android lint for the demo app.
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
- After any code, manifest, resource, Gradle, or demo UI change, run the affected tests and the relevant lint tasks before committing. For `toolkit/` changes run `./gradlew :toolkit:lint --no-parallel`; for `composeApp/` changes run `./gradlew :composeApp:lint --no-parallel`. If a change touches both modules, run both lint tasks.
- Run KMP/AGP verification tasks such as tests, lint, assemble, and compile serially. Do not launch multiple Gradle processes against this workspace in parallel, because they can race on shared build outputs and produce misleading missing-class or unresolved-reference failures.

## Commit & Pull Request Guidelines
- Use Conventional Commits so Release Please can determine release versions.
- `fix:` triggers a patch release, `feat:` triggers a minor release, and `!` or a `BREAKING CHANGE:` footer triggers a breaking release.
- `docs:`, `test:`, `ci:`, and `chore:` do not trigger a release by default.
- Use a `Release-As: x.y.z` footer when a PR must force a specific release version.
- Keep each PR focused and ensure the squash merge title keeps the Conventional Commits prefix.
- Keep commits focused by module and behavior.
- PRs should include: a short description, affected modules, and test results (commands + outcomes). For UI changes, add screenshots from Android and iOS.

## Documentation Guidelines
- When updating `README.md`, sync the same changes in other language variants (e.g., `README.zh.md`) to keep content aligned.

## ComposeApp Text Internationalization
- All new user-visible text in `composeApp/` must be extracted to Compose Multiplatform string resources.
- Use English as the default text in `composeApp/src/commonMain/composeResources/values/strings.xml`.
- Add the matching Chinese text in `composeApp/src/commonMain/composeResources/values-zh/strings.xml`.
- Resolve text through `stringResource(Res.string...)` or `pluralStringResource(Res.plurals...)`; do not add manual language switching or override the system locale.
- Use placeholders or plurals for dynamic text instead of concatenating localized sentence fragments in Kotlin.
- Keep technical constants in code when they are not user-facing, such as routes, URLs, MIME types, package IDs, file extensions, enum names, and sample identifiers.
- If Android system-level labels are added or changed, keep `composeApp/src/androidMain/res/values/strings.xml` and `composeApp/src/androidMain/res/values-zh/strings.xml` aligned.

## Security & Configuration Tips
- `local.properties` is intentionally ignored; it contains machine-specific paths like the Android SDK. Do not commit secrets, tokens, or signing configs.
