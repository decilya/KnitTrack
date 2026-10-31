package com.knittrac.app.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.knittrac.app.presentation.feature_add_project.AddProjectScreen
import com.knittrac.app.presentation.feature_pattern_editor.PatternEditorScreen
import com.knittrac.app.presentation.feature_projects.ProjectsContract
import com.knittrac.app.presentation.feature_projects.ProjectsListNavigator
import com.knittrac.app.presentation.feature_projects.ProjectsListScreen
import com.knittrac.app.presentation.feature_settings.SettingsScreen
import com.knittrac.app.presentation.feature_stats.StatsScreen
import com.knittrac.app.presentation.feature_timer.TimerScreen

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    
    NavHost(
        navController = navController,
        startDestination = Screen.ProjectsList.route
    ) {
        composable(Screen.ProjectsList.route) {
            ProjectsListScreen(
                navigator = ProjectsListNavigator(
                    onAdd = { mode ->
                        when (mode) {
                            ProjectsContract.AddMode.PROJECT ->
                                navController.navigate(Screen.AddProject.route)
                            ProjectsContract.AddMode.PATTERN ->
                                navController.navigate(Screen.PatternEditorNew.route)
                        }
                    },
                    onTimer = { projectId ->
                        navController.navigate("${Screen.Timer.route}/$projectId")
                    },
                    onStats = { projectId ->
                        navController.navigate("${Screen.Stats.route}/$projectId")
                    },
                    onSettings = { navController.navigate(Screen.Settings.route) },
                    onPatternEditor = { patternId ->
                        navController.navigate("${Screen.PatternEditor.route}/$patternId")
                    }
                )
            )
        }
        
        composable(Screen.AddProject.route) {
            AddProjectScreen(
                onNavigateBack = { navController.popBackStack() },
                onProjectAdded = { navController.popBackStack() }
            )
        }
        
        composable(
            route = "${Screen.Timer.route}/{projectId}",
            arguments = listOf(navArgument("projectId") { type = NavType.LongType })
        ) {
            TimerScreen(
                onNavigateToProjects = { navController.popBackStack() },
                onNavigateToPatternEditor = { patternId ->
                    navController.navigate("${Screen.PatternEditor.route}/$patternId")
                }
            )
        }

        composable(
            route = "${Screen.Stats.route}/{projectId}",
            arguments = listOf(navArgument("projectId") { type = NavType.LongType })
        ) {
            StatsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.PatternEditorNew.route) {
            PatternEditorScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = "${Screen.PatternEditor.route}/{patternId}",
            arguments = listOf(navArgument("patternId") { type = NavType.LongType })
        ) {
            PatternEditorScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
