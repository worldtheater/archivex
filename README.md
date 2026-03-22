# Archivex

English | [简体中文](./README.zh-CN.md)

`archivex` is an offline note-taking app built with Kotlin Multiplatform and Compose Multiplatform, currently targeting Android, iOS, and Desktop (JVM). It is the KMP rewrite of Archive, whose original version has already been published on Google Play.

Its core value is straightforward: local-first and user-controlled by default. Notes can be used without relying on an online service, sensitive content stays on the device as much as possible, and the app aims to provide a consistent experience for writing, viewing, and migrating notes across platforms.

Core capabilities include:

- Local offline notes and folder management
- Markdown editing and preview, plus Mermaid preview
- Sensitive note protection and device authentication
- Backup / restore, plus plain-text import / export

## Module Structure

- `androidApp`
  Android application host module that owns the APK packaging and app manifest.
- `composeApp`
  Shared KMP module with shared UI, business logic, and most platform-specific implementations.
- `iosApp`
  The iOS host project and Xcode entry point.
- `scripts`
  Helper scripts for starting the iOS simulator and running the app.
- `gradle/libs.versions.toml`
  Centralized dependency and plugin version catalog.

Main source sets under `composeApp/src`:

- `commonMain`
  Cross-platform shared UI, navigation, data layer, and domain logic.
- `androidMain`
  Android-specific implementations, platform security features, document pickers, and system integrations.
- `iosMain`
  iOS-specific implementations and the `UIViewController` entry point.
- `jvmMain`
  Desktop-specific implementations, including JavaFX and Mermaid-related support.
- `commonTest`
  Shared tests.

## Development Environment

Recommended environment:

- JDK 17
- Android Studio or IntelliJ IDEA
- Android SDK
- Xcode (required only for iOS development)

The project currently enables:

- Gradle configuration cache
- Gradle build cache
- Gradle version catalog

## Run and Build

### Android

Build the Debug APK:

```bash
./gradlew :androidApp:assembleDebug
```

Compile Android Kotlin:

```bash
./gradlew :composeApp:compileAndroidMain
```

### Desktop

Run the Desktop app:

```bash
./gradlew :composeApp:run
```

Compile Desktop Kotlin:

```bash
./gradlew :composeApp:compileKotlinJvm
```

### iOS

There are two common ways to run the iOS app:

1. Open [`iosApp/iosApp.xcodeproj`](/Users/ppp/repos/archivex/iosApp/iosApp.xcodeproj) in Xcode and run it directly.
2. Use the helper scripts to start a simulator and install / launch the app.

Start an available iPhone simulator:

```bash
./scripts/start_ios_simulator.sh
```

Build and run the iOS app:

```bash
./scripts/run_ios_app.sh
```

You can also pass a device name, for example:

```bash
./scripts/run_ios_app.sh "iPhone 16"
```

## Common Tasks

Run shared tests:

```bash
./gradlew :composeApp:allTests
```

Build the Desktop distribution package:

```bash
./gradlew :composeApp:packageDistributionForCurrentOS
```

## Dependency Management

Dependencies and plugin versions are managed centrally through [`gradle/libs.versions.toml`](/Users/ppp/repos/archivex/gradle/libs.versions.toml). When adding or upgrading dependencies, update the version catalog first, then reference them through `libs.*` in [`composeApp/build.gradle.kts`](/Users/ppp/repos/archivex/composeApp/build.gradle.kts).

## Notes

- This is an evolving KMP project, and some directories and names still reflect its migration history from `Archive`.
- The Android app entry point now lives in `androidApp`, while `composeApp` uses the Android-KMP library plugin to stay aligned with the AGP 9 migration path.
