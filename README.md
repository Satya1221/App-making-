# Cricket Master Manager

An offline Android cricket-management game built with Kotlin and the Android framework. It launches with a populated Harbor Hawks squad and offers a complete playable management loop:

- dashboard, squad browser, detailed player profiles, playing-XI and captain selection;
- deterministic attribute-informed T20 simulation with tactics, scorecards, ball events, form, fatigue, and statistics;
- local JSON save/load using `SharedPreferences`;
- role-aware AI auction opponents, a player auction, budgets, finance upgrades, training, standings, news, and season rollover.

## Run

Open the project in Android Studio with an Android SDK Platform 35 installed, then run the `app` configuration on an Android emulator or device.

## Development environment

The repository includes `./gradlew` as a project-local Gradle launcher and targets Android SDK Platform 35. Build with:

```sh
./gradlew :app:assembleDebug
```

Set `ANDROID_HOME` (or add `sdk.dir` to an untracked `local.properties`) to an SDK installation that contains Platform 35 and Build Tools 35.0.0.
