package com.knittrac.app.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.knittrac.app.presentation.feature_add_project.AddProjectScreen
import com.knittrac.app.presentation.feature_projects.ProjectsListScreen
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
                onNavigateToAddProject = { navController.navigate(Screen.AddProject.route) },
                onNavigateToTimer = { projectId -> navController.navigate("${Screen.Timer.route}/$projectId") },
                onNavigateToStats = { projectId -> navController.navigate("${Screen.Stats.route}/$projectId") }
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
                onNavigateToProjects = { navController.popBackStack() }
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
    }
}
