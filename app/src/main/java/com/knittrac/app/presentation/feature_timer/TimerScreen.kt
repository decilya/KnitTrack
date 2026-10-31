package com.knittrac.app.presentation.feature_timer

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.knittrac.app.R
import com.knittrac.app.domain.service.TimerState
import kotlinx.coroutines.flow.collectLatest

/**
 * Экран таймера: время, локальный счётчик рядов сессии, прогресс по схеме,
 * старт/пауза/сброс, сохранение сессии.
 *
 * Два независимых счётчика (KnitCompanion-like UX):
 * - FAB «+1» → [TimerContract.Intent.IncrementRow] (локальный `rowCount`
 *   сессии, идёт в `SaveSessionParams`);
 * - IconButton «→» → [TimerContract.Intent.MarkNextPatternRow] (обновляет
 *   `Project.currentPatternRowId` в БД через `MarkNextRowUseCase`).
 *
 * Бейдж «Ряд схемы: N» показывается, только если у проекта есть привязанная
 * схема и прогресс по ней начат (`currentPatternRowNumber != null`).
 */
@Composable
fun TimerScreen(
    viewModel: TimerViewModel = hiltViewModel(),
    permissionViewModel: PermissionViewModel = hiltViewModel(),
    onNavigateToProjects: () -> Unit = {}
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    // Системный диалог запроса POST_NOTIFICATIONS (Android 13+).
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { /* granted или нет — таймер уже стартовал */ }

    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is TimerContract.Effect.NavigateToProjects -> onNavigateToProjects()
                is TimerContract.Effect.SessionSaved -> {
                    Toast.makeText(
                        context,
                        context.getString(R.string.session_saved),
                        Toast.LENGTH_SHORT
                    ).show()
                }
                is TimerContract.Effect.ShowError -> {
                    Toast.makeText(
                        context,
                        context.getString(effect.messageResId),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (state.projects.isNotEmpty()) {
            val selectedProject = state.projects.find { it.id == state.projectId }
            Text(
                text = selectedProject?.name?.value ?: stringResource(R.string.select_project),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        } else {
            Text(
                text = stringResource(R.string.no_projects_available),
                color = MaterialTheme.colorScheme.error
            )
        }

        Text(
            text = formatTime(state.elapsedTime),
            style = MaterialTheme.typography.displayLarge,
            modifier = Modifier.padding(vertical = 32.dp)
        )

        // Счётчик рядов
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
        ) {
            Text(
                text = state.rowCount.toString(),
                style = MaterialTheme.typography.displayMedium
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
            ) {
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.onIntent(TimerContract.Intent.DecrementRow)
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = stringResource(R.string.decrease_row_content_description)
                    )
                }

                Spacer(modifier = Modifier.width(32.dp))

                FloatingActionButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.onIntent(TimerContract.Intent.IncrementRow)
                    },
                    modifier = Modifier.size(96.dp)
                ) {
                    Text(
                        text = "+1",
                        style = MaterialTheme.typography.headlineMedium
                    )
                }

                Spacer(modifier = Modifier.width(32.dp))

                // Кнопка «следующий ряд схемы» (аналог One-Tap Marker
                // в KnitCompanion). Обновляет Project.currentPatternRowId
                // в БД, в отличие от FAB «+1» (локальный rowCount).
                IconButton(
                    enabled = !state.isMarkingRow && state.projectId != null,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.onIntent(TimerContract.Intent.MarkNextPatternRow)
                    }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = stringResource(R.string.next_pattern_row_content_description)
                    )
                }
            }

            val currentPatternRowNumber = state.currentPatternRowNumber
            if (currentPatternRowNumber != null) {
                Text(
                    text = stringResource(
                        R.string.pattern_row_label_format,
                        currentPatternRowNumber
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            val target = state.targetRowCount
            if (target != null && target > 0) {
                val progress = state.rowCount.toFloat() / target
                LinearProgressIndicator(
                    progress = { progress.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().padding(top = 24.dp)
                )
                Text(
                    text = "${state.rowCount} / $target",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        Row(
            modifier = Modifier.padding(top = 32.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (state.status == TimerState.IDLE || state.status == TimerState.PAUSED) {
                Button(onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    if (!permissionViewModel.isGranted()) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    viewModel.onIntent(TimerContract.Intent.Start)
                }) {
                    Text(stringResource(R.string.start))
                }
            }
            if (state.status == TimerState.RUNNING) {
                Button(onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.onIntent(TimerContract.Intent.Pause)
                }) {
                    Text(stringResource(R.string.pause))
                }
            }

            Button(onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                viewModel.onIntent(TimerContract.Intent.Reset)
            }) {
                Text(stringResource(R.string.reset))
            }

            if ((state.status == TimerState.PAUSED || state.status == TimerState.IDLE) &&
                state.elapsedTime > 0
            ) {
                Button(
                    enabled = !state.isSaving,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.onIntent(TimerContract.Intent.SaveSession)
                    }
                ) {
                    Text(stringResource(R.string.save_session))
                }
            }
        }
    }
}

/**
 * Форматирует секунды в HH:MM:SS.
 *
 * `Locale.US` — стабильный формат без разделителей тысяч (зафиксировано
 * в TECHDEBT P2: рассмотреть `Locale.ROOT` или задокументировать выбор).
 */
fun formatTime(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return String.format(java.util.Locale.US, "%02d:%02d:%02d", h, m, s)
}
