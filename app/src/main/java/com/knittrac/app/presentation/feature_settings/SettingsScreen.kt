package com.knittrac.app.presentation.feature_settings

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.knittrac.app.R
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.InputStream
import java.io.OutputStream

/**
 * Экран настроек.
 * 
 * ВАЖНО: Весь текст в этом экране (заголовки, опции, кнопки) получен через 
 * stringResource(R.string.*), что гарантирует полную поддержку мультиязычности (RU/EN).
 * Screen полностью отвечает за создание и безопасное закрытие потоков SAF.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var activeOutputStream by remember { mutableStateOf<OutputStream?>(null) }
    var activeInputStream by remember { mutableStateOf<InputStream?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                val outputStream = openOutputStreamSafe(context, it)
                if (outputStream != null) {
                    activeOutputStream = outputStream
                    viewModel.onAction(SettingsContract.Action.RequestExport(outputStream))
                } else {
                    snackbarHostState.showSnackbar(context.getString(R.string.settings_export_error))
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                val inputStream = openInputStreamSafe(context, it)
                if (inputStream != null) {
                    activeInputStream = inputStream
                    viewModel.onAction(SettingsContract.Action.ImportData(inputStream))
                } else {
                    snackbarHostState.showSnackbar(context.getString(R.string.settings_import_error))
                }
            }
        }
    }

    // FIX: используем collect вместо collectLatest, и закрываем поток ДО showSnackbar.
    // Это гарантирует, что close() выполнится даже если showSnackbar приостановит корутину.
    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                SettingsContract.Effect.ExportSuccess -> {
                    activeOutputStream?.close()
                    activeOutputStream = null
                    snackbarHostState.showSnackbar(context.getString(R.string.settings_export_success))
                }
                SettingsContract.Effect.ExportError -> {
                    activeOutputStream?.close()
                    activeOutputStream = null
                    snackbarHostState.showSnackbar(context.getString(R.string.settings_export_error))
                }
                SettingsContract.Effect.ImportSuccess -> {
                    activeInputStream?.close()
                    activeInputStream = null
                    snackbarHostState.showSnackbar(context.getString(R.string.settings_import_success))
                }
                SettingsContract.Effect.ImportError -> {
                    activeInputStream?.close()
                    activeInputStream = null
                    snackbarHostState.showSnackbar(context.getString(R.string.settings_import_error))
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // FIX: Все заголовки и опции теперь используют stringResource
                Text(stringResource(R.string.settings_appearance), style = MaterialTheme.typography.titleMedium)
                listOf(
                    "SYSTEM" to stringResource(R.string.settings_theme_system),
                    "LIGHT" to stringResource(R.string.settings_theme_light),
                    "DARK" to stringResource(R.string.settings_theme_dark)
                ).forEach { (mode, label) ->
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .clickable { viewModel.onAction(SettingsContract.Action.ChangeThemeMode(mode)) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = state.themeMode == mode,
                            onClick = { viewModel.onAction(SettingsContract.Action.ChangeThemeMode(mode)) }
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(label)
                    }
                }

                HorizontalDivider()

                Text(stringResource(R.string.settings_notifications), style = MaterialTheme.typography.titleMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.settings_notifications_enable))
                    Switch(
                        checked = state.notificationsEnabled,
                        onCheckedChange = { viewModel.onAction(SettingsContract.Action.ToggleNotifications(it)) }
                    )
                }

                HorizontalDivider()

                Text(stringResource(R.string.settings_data), style = MaterialTheme.typography.titleMedium)

                Button(
                    onClick = { exportLauncher.launch("knittrac_backup.json") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isExporting
                ) {
                    Icon(Icons.Default.Upload, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.settings_export_data))
                }

                OutlinedButton(
                    onClick = { importLauncher.launch("application/json") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isImporting
                ) {
                    Icon(Icons.Default.Download, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.settings_import_data))
                }
            }
        }
    }
}

/**
 * Безопасно открывает [java.io.OutputStream] по Uri.
 * @return поток или null при ошибке.
 */
private fun openOutputStreamSafe(context: Context, uri: Uri): OutputStream? = try {
    context.contentResolver.openOutputStream(uri)
} catch (e: Exception) {
    Timber.e(e, "Failed to open output stream for uri: $uri")
    null
}

/**
 * Безопасно открывает [java.io.InputStream] по Uri.
 * @return поток или null при ошибке.
 */
private fun openInputStreamSafe(context: Context, uri: Uri): InputStream? = try {
    context.contentResolver.openInputStream(uri)
} catch (e: Exception) {
    Timber.e(e, "Failed to open input stream for uri: $uri")
    null
}
