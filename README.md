# Field Incident Reporter

An Android-native prototype for reporting and managing facility maintenance incidents.

## Android app

- App name: Field Incident Reporter
- Application ID: `com.hyejeanmoon.fieldincidentreporter`
- UI: Jetpack Compose with Material 3
- Architecture baseline: AndroidX Lifecycle and ViewModel, with Navigation Compose
- Minimum Android version: Android 8.0 (API 26)

## Open and run

Open this folder in Android Studio and allow Gradle sync to finish. The project uses Android Gradle Plugin 9.4.0 and Gradle 9.6.0. Android Studio must have a compatible JDK 17 and Android SDK Platform 37 installed.

This environment did not have Gradle installed, so the Gradle wrapper launch scripts and wrapper JAR are not included yet. Once Gradle 9.6.0 is available locally, generate them with `gradle wrapper --gradle-version 9.6.0`; then commit `gradlew`, `gradlew.bat`, and `gradle/wrapper/gradle-wrapper.jar`.

Then select the `app` run configuration and launch it on an emulator or Android device.

## Project structure

```text
app/                 Android application module
gradle/              Centralized dependency and plugin versions
```

The app currently includes incident list, create/edit form, and detail screens. Create, edit, and delete actions work against in-memory prototype data and are not connected to the mock API yet. The app starts with an empty incident list; sample data is used only in Compose previews.
