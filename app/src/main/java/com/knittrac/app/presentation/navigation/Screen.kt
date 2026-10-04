package com.knittrac.app.presentation.navigation

sealed class Screen(val route: String) {
    object ProjectsList : Screen("projects_list")
    object AddProject : Screen("add_project")
    object Timer : Screen("timer")
    object Stats : Screen("stats")
}
