package com.knittrac.app.presentation.feature_timer

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.knittrac.app.R
import com.knittrac.app.domain.service.TimerState
import kotlinx.coroutines.flow.collectLatest

@Composable
fun TimerScreen(
    viewModel: TimerViewModel = hiltViewModel(),
    onNavigateToProjects: () -> Unit = {}
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is TimerContract.Effect.NavigateToProjects -> onNavigateToProjects()
                is TimerContract.Effect.SessionSaved -> {
                    Toast.makeText(context, context.getString(R.string.session_saved), Toast.LENGTH_SHORT).show()
                }
                is TimerContract.Effect.ShowError -> {
                    Toast.makeText(context, context.getString(effect.messageResId), Toast.LENGTH_SHORT).show()
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
                text = selectedProject?.name ?: stringResource(R.string.select_project),
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

        OutlinedTextField(
            value = if (state.rowCount == 0) "" else state.rowCount.toString(),
            onValueChange = { 
                val count = it.toIntOrNull() ?: 0
                viewModel.onIntent(TimerContract.Intent.UpdateRowCount(count))
            },
            label = { Text(stringResource(R.string.row_count)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.padding(top = 32.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (state.status == TimerState.IDLE || state.status == TimerState.PAUSED) {
                Button(onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); viewModel.onIntent(TimerContract.Intent.Start) }) {
                    Text(stringResource(R.string.start))
                }
            }
            if (state.status == TimerState.RUNNING) {
                Button(onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); viewModel.onIntent(TimerContract.Intent.Pause) }) {
                    Text(stringResource(R.string.pause))
                }
            }
            
            Button(onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); viewModel.onIntent(TimerContract.Intent.Reset) }) {
                Text(stringResource(R.string.reset))
            }
            
            if ((state.status == TimerState.PAUSED || state.status == TimerState.IDLE) && state.elapsedTime > 0) {
                Button(onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); viewModel.onIntent(TimerContract.Intent.SaveSession) }) {
                    Text(stringResource(R.string.save_session))
                }
            }
        }
    }
}

fun formatTime(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return String.format(java.util.Locale.US, "%02d:%02d:%02d", h, m, s)
}
