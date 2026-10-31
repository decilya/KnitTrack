package com.knittrac.app.presentation.navigation

sealed class Screen(val route: String) {
    object ProjectsList : Screen("projects_list")
    object AddProject : Screen("add_project")
    object Timer : Screen("timer")
    object Stats : Screen("stats")
    object Settings : Screen("settings")
    object PatternEditor : Screen("pattern_editor")
    object PatternEditorNew : Screen("pattern_editor_new")
}
