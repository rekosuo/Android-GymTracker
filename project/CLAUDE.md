# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Run unit tests
./gradlew test

# Run instrumented tests (requires connected device/emulator)
./gradlew connectedAndroidTest

# Clean build
./gradlew clean build

# Compile Kotlin (triggers Hilt/Room code generation)
./gradlew compileDebugKotlin
```

## Architecture

MVVM with a 4-layer architecture:

```
UI (Jetpack Compose) → ViewModel (StateFlow) → Repository → Room Database
```

- **`domain/model/`** — Pure Kotlin data classes with no framework dependencies (`Exercise`, `ExerciseGroup`, `Performance`, `SetEntry`, `WeightRow`).
- **`data/local/entity/`** — Room `@Entity` classes that map to SQLite tables.
- **`data/local/dao/`** — Room DAOs exposing `Flow`-based queries and `suspend` mutation functions.
- **`data/repository/`** — Maps between entity and domain models; injected into ViewModels via Hilt.
- **`ui/<feature>/`** — Each screen has a `*Screen.kt` composable and a paired `*ViewModel.kt`.
- **`di/AppModule.kt`** — Hilt `@Module` providing the `GymDatabase`, DAOs, and repositories as singletons.

### State & Event pattern

Every ViewModel defines an immutable `data class *State(...)` and a `sealed class *Event`. The composable collects `StateFlow<*State>` and calls `viewModel.onEvent(...)` for user interactions.

### Sets storage

`SetEntry` objects are stored as a flat ordered list in `PerformanceEntity.sets` (serialized to JSON via `kotlinx-serialization`). The ViewModel derives `weightRows: List<WeightRow>` — a grouped UI view — from this flat list. All mutations go through `sets`; `weightRows` is always recalculated.

### Navigation

Defined in `ui/navigation/NavGraph.kt` using a `sealed class Screen(val route: String)`. Each screen object provides a `createRoute(...)` helper for type-safe argument passing. Navigation arguments are accessed in ViewModels via `SavedStateHandle`.

### Database relationships

Exercise ↔ Group is many-to-many via `ExerciseGroupCrossRef` junction table. Room `@Relation` classes (`ExerciseWithGroups`, `GroupWithExercises`) handle the join queries.

## Key Technologies

| Concern | Library |
|---|---|
| UI | Jetpack Compose (BOM 2025.12.01), Material 3 |
| DI | Hilt 2.57.2 + KSP |
| Database | Room 2.8.4 |
| Navigation | Navigation Compose 2.9.6 |
| Async | Kotlin Coroutines 1.10.2 |
| Serialization | kotlinx-serialization-json 1.9.0 |

All library versions are centralized in `gradle/libs.versions.toml`.

## Unimplemented Features

- **Calendar screen** — route exists in `NavGraph.kt` as a placeholder but the screen is not yet built.
