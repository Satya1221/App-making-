# Cricket Master Manager

An offline Android cricket-management game built with Kotlin and the Android framework. It launches with a populated Harbor Hawks squad and offers a complete playable management loop:

- dashboard, squad browser, detailed player profiles, playing-XI and captain selection;
- deterministic attribute-informed T20 simulation with tactics, scorecards, ball events, form, fatigue, and statistics;
- local JSON save/load using `SharedPreferences`;
- role-aware AI auction opponents, a player auction, budgets, finance upgrades, training, standings, news, and season rollover.

## Run in Android Studio

Open the project in Android Studio with an Android SDK Platform 35 installed, then run the `app` configuration on an Android emulator or device.

The project intentionally does not commit a debug keystore. Android Studio can use its normal local debug signing configuration.

## Command-line build

The repository's `gradlew` is a lightweight launcher that delegates to an installed Gradle executable. Gradle 8.14.4 is the expected version for this project.

```sh
gradle :app:assembleDebug
```

Set `ANDROID_HOME` (or add `sdk.dir` to an untracked `local.properties`) to an SDK installation that contains Platform 35 and Build Tools 35.0.0.

## Continuous integration

GitHub Actions installs JDK 17, Android SDK Platform 35 / Build Tools 35.0.0, and Gradle 8.14.4 before running the debug build. This means the repository can be checked on a clean runner without a committed keystore or a machine-specific Gradle installation.

## Project configuration

This project uses explicit plugin versions in the root `build.gradle.kts`; it does not depend on a `gradle/libs.versions.toml` version catalog. The app module likewise uses explicit Android/Kotlin plugins and does not reference `libs.*` aliases.
