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

MVI-style data flow with a 4-layer architecture:

```
UI (Jetpack Compose) → ViewModel (StateFlow) → Repository → Room Database
```

- **`domain/model/`** — Pure Kotlin data classes (`Exercise`, `ExerciseGroup`, `Performance`, `SetEntry`, `WeightRow`, `GraphDataPoint`) plus domain extension functions for data transformations (e.g., `List<SetEntry>.toWeightRows()`, `Performance.toGraphDataPoint()`, `List<GraphDataPoint>.filterByTime()`).
- **`data/local/entity/`** — Room `@Entity` classes that map to SQLite tables.
- **`data/local/dao/`** — Room DAOs exposing `Flow`-based queries and `suspend` mutation functions. `GroupDao` uses a `@Transaction` method (`replaceGroupExercises`) for atomic junction table updates.
- **`data/repository/`** — Maps between entity and domain models; injected into ViewModels via Hilt. `ExerciseRepository` handles both exercises and groups (they are tightly coupled via many-to-many relationships). Repositories also provide domain-level operations like `toggleExerciseFavorite`/`toggleGroupFavorite` (using targeted single-column SQL updates).
- **`ui/<feature>/`** — Each screen has a `*Screen.kt` composable and a paired `*ViewModel.kt`.
- **`di/AppModule.kt`** — Hilt `@Module` providing the `GymDatabase`, DAOs, and repositories as singletons.

### State & Event pattern (MVI)

Every ViewModel follows unidirectional data flow: the composable sends user actions as sealed `*Event` objects through a single `viewModel.onEvent(...)` entry point, the ViewModel processes them and updates an immutable `data class *State(...)`, and the composable re-renders by collecting `StateFlow<*State>`. ViewModels never expose individual mutator methods — all interactions go through the sealed event dispatch.

### Sets storage

`SetEntry` objects are stored as a flat ordered list in `PerformanceEntity.sets` (serialized to JSON via `kotlinx-serialization`). The domain layer provides `List<SetEntry>.toWeightRows()` and `List<WeightRow>.toSets()` for converting between the flat storage format and the grouped UI representation. All mutations go through `sets`; `weightRows` is always recalculated via these domain extensions.

### Navigation

Defined in `ui/navigation/NavGraph.kt` using a `sealed class Screen(val route: String)`. Each screen object provides a `createRoute(...)` helper for type-safe argument passing. Navigation arguments are accessed in ViewModels via `SavedStateHandle`.

### Database relationships

Exercise ↔ Group is many-to-many via `ExerciseGroupCrossRef` junction table (with `orderIndex` for exercise ordering within a group). The repository assembles `GroupWithExercises` manually via `getGroupById()` + `getOrderedExercisesForGroup()`. Room `@Relation` classes exist in `ExerciseRelations.kt` but are not currently used by any DAO queries.

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
