# Gym Tracker

<p align="center">
    <img src="docs/pics/app.gif" width="300" alt="Gym Tracker navigation demo"/>
</p>

---
### Purpose

Gym Tracker is an Android app for keeping track of your gym training. 

You can:
* Create your own exercises
* Create custom groups for exercises (e.g. Chest day)
* Favorite exercises and groups to quickly access them from the home screen
* Enter exercise performances, with different amounts of weight and repetitions
* Track exercise progress over time with the Graph view

### Planned features

1. Implement the Calendar view.
* View days for the selected exercise.
* View all exercises performed on a day.

2. Implement drag and drop reordering into Group Edit Screen.
* Drag and drop exercise items to reorder them within the group.

3. Include rep tracking in the Graph view.

### Build

Android Studio Otter 3 (2025.2.3) or newer

* Install Android Studio
* Open the project in Android Studio
* Set Gradle JDK in Settings > Build, Execution, Deployment > Build Tools > Gradle > Gradle JDK
* Build with Android Studio or with CLI

```bash
# Build the APK in CLI
cd project
./gradlew assembleDebug
```

APK will be located in `project/app/build/outputs/apk/debug/`.

### Architecture

See [GymTracker-architecture.png](docs/GymTracker-architecture.png) or [GymTracker-architecture.excalidraw](docs/GymTracker-architecture.excalidraw).
