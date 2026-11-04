package com.knittrac.app.presentation.feature_projects

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.knittrac.app.R
import com.knittrac.app.presentation.common.EmptyStateView
import kotlinx.coroutines.flow.collectLatest

/**
 * Переходы со списка проектов. Отдельный тип, чтобы сигнатура экрана
 * не превысила порог LongParameterList.
 */
class ProjectsListNavigator(
    val onAdd: (ProjectsContract.AddMode) -> Unit,
    val onTimer: (Long) -> Unit,
    val onStats: (Long) -> Unit,
    val onSettings: () -> Unit,
    val onPatternEditor: (Long) -> Unit
)

/**
 * Экран списка проектов.
 *
 * Состояния:
 * 1. isLoading → CircularProgressIndicator.
 * 2. projects.isEmpty() → EmptyStateView с кнопкой «Добавить».
 * 3. else → LazyColumn с карточками проектов.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ProjectsListScreen(
    navigator: ProjectsListNavigator,
    viewModel: ProjectsListViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is ProjectsContract.Effect.OpenTimer -> {
                    navigator.onTimer(effect.projectId)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        navigator.onSettings()
                    }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.settings)
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            var addMenu by remember { mutableStateOf(false) }
            Box {
                FloatingActionButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        addMenu = true
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(R.string.add_project)
                    )
                }
                DropdownMenu(
                    expanded = addMenu,
                    onDismissRequest = { addMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.add_mode_project)) },
                        onClick = {
                            addMenu = false
                            navigator.onAdd(ProjectsContract.AddMode.PROJECT)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.add_mode_pattern)) },
                        onClick = {
                            addMenu = false
                            navigator.onAdd(ProjectsContract.AddMode.PATTERN)
                        }
                    )
                }
            }
        }
    ) { paddingValues ->
        val errorRes = state.errorRes
        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            errorRes != null -> {
                EmptyStateView(
                    icon = Icons.Default.Warning,
                    title = stringResource(errorRes),
                    description = stringResource(R.string.error_load_projects_description),
                    actionText = stringResource(R.string.error_retry),
                    onActionClick = { viewModel.onAction(ProjectsContract.Action.Retry) },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            }
            state.projects.isEmpty() -> {
                EmptyStateView(
                    icon = Icons.Default.Inbox,
                    title = stringResource(R.string.no_projects_yet),
                    description = stringResource(R.string.no_projects_description),
                    actionText = stringResource(R.string.add_project),
                    onActionClick = { navigator.onAdd(ProjectsContract.AddMode.PROJECT) },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.projects, key = { it.id }) { projectUi ->
                        ProjectCard(
                            project = projectUi,
                            modifier = Modifier.animateItemPlacement(),
                            onClick = { navigator.onTimer(projectUi.id) },
                            onStatsClick = { navigator.onStats(projectUi.id) },
                            onOpenPattern = navigator.onPatternEditor
                        )
                    }
                }
            }
        }
    }
}

/**
 * Карточка проекта в списке.
 *
 * Отображает название и локализованную категорию, полученные из
 * [ProjectsContract.ProjectUiModel].
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ProjectCard(
    project: ProjectsContract.ProjectUiModel,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onStatsClick: () -> Unit,
    onOpenPattern: (Long) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var menuExpanded by remember { mutableStateOf(false) }
    Box {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onClick()
                },
                onLongClick = {
                    val patternId = project.patternId
                    if (project.canOpenPatternEditor && patternId != null) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        menuExpanded = true
                    }
                }
            )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            val displayName = if (project.isUntitled) {
                androidx.compose.ui.res.stringResource(com.knittrac.app.R.string.project_untitled)
            } else {
                project.name
            }
            Text(
                text = displayName,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = project.localizedCategoryName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            val target = project.targetRowCount
            if (target != null && target > 0) {
                Text(
                    text = stringResource(R.string.project_rows_progress, project.totalRows, target),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
                LinearProgressIndicator(
                    progress = { (project.totalRows.toFloat() / target).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                )
            } else if (project.totalRows > 0) {
                Text(
                    text = stringResource(R.string.project_rows_count, project.totalRows),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
            IconButton(onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onStatsClick()
            }) {
                Icon(
                    imageVector = Icons.Default.Assessment,
                    contentDescription = stringResource(R.string.action_stats)
                )
            }
        }
    }
        DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false }
        ) {
            val patternId = project.patternId
            if (patternId != null) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.pattern_editor_open)) },
                    onClick = {
                        menuExpanded = false
                        onOpenPattern(patternId)
                    }
                )
            }
        }
    }
}
