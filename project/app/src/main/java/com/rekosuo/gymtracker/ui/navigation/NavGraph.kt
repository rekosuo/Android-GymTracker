package com.rekosuo.gymtracker.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.rekosuo.gymtracker.ui.calendar.CalendarScreen
import com.rekosuo.gymtracker.ui.exercise.ExerciseEditScreen
import com.rekosuo.gymtracker.ui.exercise.ExerciseListScreen
import com.rekosuo.gymtracker.ui.graph.ProgressGraphScreen
import com.rekosuo.gymtracker.ui.group.GroupContentsScreen
import com.rekosuo.gymtracker.ui.group.GroupEditScreen
import com.rekosuo.gymtracker.ui.home.HomeScreen
import com.rekosuo.gymtracker.ui.performance.PerformanceEntryScreen

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

    object Calendar : Screen("calendar?exerciseId={exerciseId}&groupId={groupId}") {
        fun createRoute(exerciseId: Long = 0, groupId: Long = 0): String {
            return if (exerciseId == 0L) {
                "calendar?groupId=$groupId"
            } else {
                "calendar?exerciseId=$exerciseId"
            }
        }
    }
}

private fun NavHostController.safePopBackStack() {
    if (currentBackStackEntry?.destination?.route != Screen.Home.route) {
        popBackStack()
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
        // Home Screen
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
                onNavigateToExerciseCalendar = { exerciseId ->
                    navController.navigate(Screen.Calendar.createRoute(exerciseId = exerciseId))
                },
                onNavigateToAllCalendar = {
                    navController.navigate(Screen.Calendar.createRoute())
                },
                onNavigateToGroupCalendar = { groupId ->
                    navController.navigate(Screen.Calendar.createRoute(groupId = groupId))
                },
                onEditExercise = { exerciseId ->
                    navController.navigate(Screen.ExerciseEdit.createRoute(exerciseId))
                },
                onEditGroup = { groupId ->
                    navController.navigate(Screen.GroupEdit.createRoute(groupId))
                }
            )
        }

        // Exercise List Screen
        composable(Screen.ExerciseList.route) {
            ExerciseListScreen(
                onNavigateBack = { navController.safePopBackStack() },
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
                onNavigateToExerciseCalendar = { exerciseId ->
                    navController.navigate(Screen.Calendar.createRoute(exerciseId = exerciseId))
                },
                onNavigateToAllCalendar = {
                    navController.navigate(Screen.Calendar.createRoute())
                },
                onNavigateToGroupCalendar = { groupId ->
                    navController.navigate(Screen.Calendar.createRoute(groupId = groupId))
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

        // Exercise Edit Screen
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
                onNavigateBack = { navController.safePopBackStack() }
            )
        }

        // Group Edit Screen
        composable(
            route = Screen.GroupEdit.route,
            arguments = listOf(
                navArgument("groupId") {
                    type = NavType.LongType
                    defaultValue = 0L
                }
            )
        ) {
            GroupEditScreen(
                onNavigateBack = { navController.safePopBackStack() }
            )
        }

        // Group Contents Screen
        composable(
            route = Screen.GroupContents.route,
            arguments = listOf(
                navArgument("groupId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val groupId = backStackEntry.arguments?.getLong("groupId") ?: 0L
            GroupContentsScreen(
                onNavigateBack = { navController.safePopBackStack() },
                onExerciseClick = { exerciseId ->
                    navController.navigate(Screen.PerformanceEntry.createRoute(exerciseId))
                },
                onEditExercise = { exerciseId ->
                    navController.navigate(Screen.ExerciseEdit.createRoute(exerciseId))
                },
                onNavigateToGraph = { exerciseId ->
                    navController.navigate(Screen.ProgressGraph.createRoute(exerciseId))
                },
                onNavigateToExerciseCalendar = { exerciseId ->
                    navController.navigate(Screen.Calendar.createRoute(exerciseId = exerciseId))
                },
                onNavigateToGroupCalendar = {
                    navController.navigate(Screen.Calendar.createRoute(groupId = groupId))
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
                onNavigateBack = { navController.safePopBackStack() },
                onNavigateToGraph = {
                    navController.navigate(Screen.ProgressGraph.createRoute(exerciseId))
                },
                onNavigateToExerciseCalendar = {
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
        ) {
            ProgressGraphScreen(
                onNavigateBack = { navController.safePopBackStack() },
                onNavigateToPerformanceEntry = { exId, perfId ->
                    navController.navigate(Screen.PerformanceEntry.createRoute(exId, perfId))
                }
            )
        }

        // Calendar Screen
        composable(
            route = Screen.Calendar.route,
            arguments = listOf(
                navArgument("exerciseId") {
                    type = NavType.LongType
                    defaultValue = 0L
                },
                navArgument("groupId") {
                    type = NavType.LongType
                    defaultValue = 0L
                }
            )
        ) {
            CalendarScreen(
                onNavigateBack = { navController.safePopBackStack() }
            )
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
