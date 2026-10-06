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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.knittrac.app.R
import kotlinx.coroutines.flow.collectLatest
import timber.log.Timber

/**
 * Экран настроек.
 * Screen отвечает за чтение/запись файлов через ContentResolver (SAF),
 * ViewModel работает только со String (JSON), соблюдая Clean Architecture.
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

    var pendingExportUri by rememberSaveable { mutableStateOf<String?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let {
            pendingExportUri = it.toString()
            viewModel.onAction(SettingsContract.Action.RequestExport)
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val json = readJsonFromUri(context, it)
            if (json != null) {
                viewModel.onAction(SettingsContract.Action.ImportData(json))
            } else {
                viewModel.onAction(SettingsContract.Action.ImportData(""))
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is SettingsContract.Effect.ExportReady -> {
                    val uriStr = pendingExportUri
                    if (uriStr != null) {
                        val ok = writeJsonToUri(context, Uri.parse(uriStr), effect.json)
                        val msgRes = if (ok) R.string.settings_export_success else R.string.settings_export_error
                        snackbarHostState.showSnackbar(context.getString(msgRes))
                        pendingExportUri = null
                    }
                }
                is SettingsContract.Effect.ExportError ->
                    snackbarHostState.showSnackbar(context.getString(R.string.settings_export_error))
                
                SettingsContract.Effect.ImportSuccess ->
                    snackbarHostState.showSnackbar(context.getString(R.string.settings_import_success))
                
                is SettingsContract.Effect.ImportError ->
                    snackbarHostState.showSnackbar(context.getString(R.string.settings_import_error))
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
                Text("Внешний вид", style = MaterialTheme.typography.titleMedium)
                listOf("SYSTEM" to "Как в системе", "LIGHT" to "Светлая", "DARK" to "Темная")
                    .forEach { (mode, label) ->
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

                Text("Уведомления", style = MaterialTheme.typography.titleMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Разрешить уведомления")
                    Switch(
                        checked = state.notificationsEnabled,
                        onCheckedChange = { viewModel.onAction(SettingsContract.Action.ToggleNotifications(it)) }
                    )
                }

                HorizontalDivider()

                Text("Данные", style = MaterialTheme.typography.titleMedium)

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
 * Читает содержимое файла по Uri и возвращает его как String.
 * Возвращает null при ошибке чтения.
 */
private fun readJsonFromUri(context: Context, uri: Uri): String? = try {
    context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
} catch (e: Exception) {
    Timber.e(e, "Failed to read JSON from uri")
    null
}

/**
 * Записывает JSON-строку в файл по Uri.
 * Возвращает true при успехе, false при ошибке записи.
 */
private fun writeJsonToUri(context: Context, uri: Uri, json: String): Boolean = try {
    context.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
    true
} catch (e: Exception) {
    Timber.e(e, "Failed to write JSON to uri")
    false
}
