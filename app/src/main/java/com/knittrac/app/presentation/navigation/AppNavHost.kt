package com.knittrac.app.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.knittrac.app.presentation.feature_timer.TimerScreen
import com.knittrac.app.presentation.feature_projects.ProjectsListScreen
import com.knittrac.app.presentation.feature_add_project.AddProjectScreen

/**
 * Главный граф навигации приложения.
 * Определяет маршруты и переходы между экранами.
 */
@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    
    NavHost(
        navController = navController,
        startDestination = Screen.ProjectsList.route
    ) {
        // Экран списка проектов
        composable(Screen.ProjectsList.route) {
            ProjectsListScreen(
                onNavigateToAddProject = {
                    navController.navigate(Screen.AddProject.route)
                },
                onNavigateToTimer = { projectId ->
                    navController.navigate(Screen.Timer.route)
                }
            )
        }
        
        // Экран добавления проекта
        composable(Screen.AddProject.route) {
            AddProjectScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onProjectAdded = {
                    navController.popBackStack()
                }
            )
        }
        
        // Экран таймера
        composable(Screen.Timer.route) {
            TimerScreen(
                onNavigateToProjects = {
                    navController.popBackStack()
                }
            )
        }
    }
}
