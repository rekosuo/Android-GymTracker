package com.rekosuo.gymtracker.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.rekosuo.gymtracker.ui.exercise.ExerciseEditScreen
import com.rekosuo.gymtracker.ui.exercise.ExerciseListScreen
import com.rekosuo.gymtracker.ui.group.GroupContentsScreen
import com.rekosuo.gymtracker.ui.group.GroupEditScreen
import com.rekosuo.gymtracker.ui.home.HomeScreen
import com.rekosuo.gymtracker.ui.performance.PerformanceEntryScreen
import com.rekosuo.gymtracker.ui.graph.ProgressGraphScreen

// Navigation routes
sealed class Screen(val route: String) {
    object Home : Screen("home")
    object ExerciseList : Screen("exercise_list")
    object ExerciseEdit : Screen("exercise_edit/{exerciseId}") {
        fun createRoute(exerciseId: Long = 0) = "exercise_edit/$exerciseId"
    }

    object GroupEdit : Screen("group_edit/{groupId}") {
        fun createRoute(groupId: Long = 0) = "group_edit/$groupId"
    }

    object GroupContents : Screen("group_contents/{groupId}") {
        fun createRoute(groupId: Long) = "group_contents/$groupId"
    }

    object PerformanceEntry : Screen("performance_entry/{exerciseId}/{performanceId}") {
        fun createRoute(exerciseId: Long, performanceId: Long = 0) =
            "performance_entry/$exerciseId/$performanceId"
    }

    object ProgressGraph : Screen("progress_graph/{exerciseId}") {
        fun createRoute(exerciseId: Long) = "progress_graph/$exerciseId"
    }

    object Calendar : Screen("calendar/{exerciseId}") {
        fun createRoute(exerciseId: Long) = "calendar/$exerciseId"
    }
}

@Composable
fun NavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    startDestination: String = Screen.Home.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToExerciseList = {
                    navController.navigate(Screen.ExerciseList.route)
                },
                onExerciseClick = { exerciseId ->
                    navController.navigate(Screen.PerformanceEntry.createRoute(exerciseId))
                },
                onGroupClick = { groupId ->
                    navController.navigate(Screen.GroupContents.createRoute(groupId))
                },
                onNavigateToGraph = { exerciseId ->
                    navController.navigate(Screen.ProgressGraph.createRoute(exerciseId))
                },
                onNavigateToCalendar = { exerciseId ->
                    navController.navigate(Screen.Calendar.createRoute(exerciseId))
                },
                onEditExercise = { exerciseId ->
                    navController.navigate(Screen.ExerciseEdit.createRoute(exerciseId))
                },
                onEditGroup = { groupId ->
                    navController.navigate(Screen.GroupEdit.createRoute(groupId))
                }
            )
        }

        composable(Screen.ExerciseList.route) {
            ExerciseListScreen(
                onNavigateBack = { navController.popBackStack() },
                onExerciseClick = { exerciseId ->
                    navController.navigate(Screen.PerformanceEntry.createRoute(exerciseId))
                },
                onCreateExercise = {
                    navController.navigate(Screen.ExerciseEdit.createRoute())
                },
                onEditExercise = { exerciseId ->
                    navController.navigate(Screen.ExerciseEdit.createRoute(exerciseId))
                },
                onNavigateToGraph = { exerciseId ->
                    navController.navigate(Screen.ProgressGraph.createRoute(exerciseId))
                },
                onNavigateToCalendar = { exerciseId ->
                    navController.navigate(Screen.Calendar.createRoute(exerciseId))
                },
                onCreateGroup = {
                    navController.navigate(Screen.GroupEdit.createRoute())
                },
                onEditGroup = { groupId ->
                    navController.navigate(Screen.GroupEdit.createRoute(groupId))
                },
                onGroupClick = { groupId ->
                    navController.navigate(Screen.GroupContents.createRoute(groupId))
                }
            )
        }

        composable(
            route = Screen.ExerciseEdit.route,
            arguments = listOf(
                navArgument("exerciseId") {
                    type = NavType.LongType
                    defaultValue = 0L
                }
            )
        ) { backStackEntry ->
            val exerciseId = backStackEntry.arguments?.getLong("exerciseId")?.takeIf { it != 0L }
            ExerciseEditScreen(
                exerciseId = exerciseId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.GroupEdit.route,
            arguments = listOf(
                navArgument("groupId") {
                    type = NavType.LongType
                    defaultValue = 0L
                }
            )
        ) { backStackEntry ->
            val groupId = backStackEntry.arguments?.getLong("groupId")?.takeIf { it != 0L }
            GroupEditScreen(
                groupId = groupId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.GroupContents.route,
            arguments = listOf(
                navArgument("groupId") { type = NavType.LongType }
            )
        ) {
            GroupContentsScreen(
                onNavigateBack = { navController.popBackStack() },
                onExerciseClick = { exerciseId ->
                    navController.navigate(Screen.PerformanceEntry.createRoute(exerciseId))
                },
                onEditExercise = { exerciseId ->
                    navController.navigate(Screen.ExerciseEdit.createRoute(exerciseId))
                },
                onNavigateToGraph = { exerciseId ->
                    navController.navigate(Screen.ProgressGraph.createRoute(exerciseId))
                },
                onNavigateToCalendar = { exerciseId ->
                    navController.navigate(Screen.Calendar.createRoute(exerciseId))
                }
            )
        }

        // Performance Entry Screen
        composable(
            route = Screen.PerformanceEntry.route,
            arguments = listOf(
                navArgument("exerciseId") { type = NavType.LongType },
                navArgument("performanceId") {
                    type = NavType.LongType
                    defaultValue = 0L
                }
            )
        ) { backStackEntry ->
            val exerciseId = backStackEntry.arguments?.getLong("exerciseId") ?: 0L

            PerformanceEntryScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToGraph = {
                    navController.navigate(Screen.ProgressGraph.createRoute(exerciseId))
                },
                onNavigateToCalendar = {
                    navController.navigate(Screen.Calendar.createRoute(exerciseId))
                },
                onEditExercise = {
                    navController.navigate(Screen.ExerciseEdit.createRoute(exerciseId))
                }
            )
        }

        // Progress Graph Screen
        composable(
            route = Screen.ProgressGraph.route,
            arguments = listOf(
                navArgument("exerciseId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val exerciseId = backStackEntry.arguments?.getLong("exerciseId") ?: 0L

            ProgressGraphScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToPerformanceEntry = { exId, perfId ->
                    navController.navigate(Screen.PerformanceEntry.createRoute(exId, perfId))
                }
            )
        }

        composable(
            route = Screen.Calendar.route,
            arguments = listOf(
                navArgument("exerciseId") { type = NavType.LongType }
            )
        ) {
            // CalendarScreen will be implemented in Phase 5
            PlaceholderScreen(screenName = "Calendar Screen")
        }
    }
}

@Composable
fun PlaceholderScreen(screenName: String) {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        androidx.compose.material3.Text(
            text = "$screenName\n(To be implemented)",
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}
