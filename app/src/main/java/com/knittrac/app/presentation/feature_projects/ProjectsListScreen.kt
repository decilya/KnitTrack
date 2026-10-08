package com.knittrac.app.presentation.feature_projects

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.knittrac.app.R
import com.knittrac.app.presentation.common.EmptyStateView
import kotlinx.coroutines.flow.collectLatest

/**
 * Экран списка проектов.
 *
 * Состояния:
 * 1. isLoading → CircularProgressIndicator.
 * 2. projects.isEmpty() → EmptyStateView с кнопкой «Добавить».
 * 3. else → LazyColumn с карточками проектов.
 *
 * @param onNavigateToAddProject Лямбда навигации на экран создания проекта.
 * @param onNavigateToTimer Лямбда навигации на таймер с projectId.
 * @param onNavigateToStats Лямбда навигации на статистику с projectId.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectsListScreen(
    viewModel: ProjectsListViewModel = hiltViewModel(),
    onNavigateToAddProject: () -> Unit,
    onNavigateToTimer: (Long) -> Unit,
    onNavigateToStats: (Long) -> Unit
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is ProjectsContract.Effect.OpenTimer -> {
                    onNavigateToTimer(effect.projectId)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    if (state.projects.isNotEmpty()) {
                        IconButton(onClick = { onNavigateToStats(state.projects.first().id) }) {
                            Icon(
                                imageVector = Icons.Default.Assessment,
                                contentDescription = stringResource(R.string.action_stats)
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAddProject
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.add_project)
                )
            }
        }
    ) { paddingValues ->
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
            state.projects.isEmpty() -> {
                EmptyStateView(
                    icon = Icons.Default.Inbox,
                    title = stringResource(R.string.no_projects_yet),
                    description = stringResource(R.string.no_projects_description),
                    actionText = stringResource(R.string.add_project),
                    onActionClick = onNavigateToAddProject,
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
                            onClick = { onNavigateToTimer(projectUi.id) }
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
@Composable
private fun ProjectCard(
    project: ProjectsContract.ProjectUiModel,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = project.name,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = project.localizedCategoryName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
