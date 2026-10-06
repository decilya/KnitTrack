package com.knittrac.app.presentation.feature_settings

import android.content.Context
import android.net.Uri
import androidx.activity.ComponentActivity
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
 * Содержит 4 секции:
 * - Внешний вид (тема)
 * - Язык (динамический список из resources)
 * - Уведомления
 * - Данные (экспорт/импорт)
 *
 * Весь текст через stringResource → полная поддержка мультиязычности.
 *
 * Управление потоками SAF:
 * - Потоки создаются в launcher'ах и передаются во ViewModel через Action.
 * - ViewModel НЕ закрывает потоки — это ответственность Screen.
 * - Потоки закрываются после получения Effect (успех/ошибка).
 * - DisposableEffect гарантирует закрытие потоков при уходе с экрана
 *   (например, при навигации назад во время операции) — защита от утечки дескрипторов.
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

    // Ссылки на активные потоки — для гарантированного закрытия.
    var activeOutputStream by remember { mutableStateOf<OutputStream?>(null) }
    var activeInputStream by remember { mutableStateOf<InputStream?>(null) }

    /**
     * Гарантирует закрытие потоков при уходе с экрана (навигация назад, смена конфигурации).
     *
     * Без этого возможна утечка файловых дескрипторов, если пользователь покинет
     * экран во время активной операции экспорта или импорта.
     * 
     * ВАЖНО: close() обёрнут в runCatching, так как IOException в onDispose
     * может привести к крашу приложения при разрыве композиции.
     */
    DisposableEffect(Unit) {
        onDispose {
            runCatching { activeOutputStream?.close() }
                .onFailure { Timber.e(it, "Failed to close output stream on dispose") }
            runCatching { activeInputStream?.close() }
                .onFailure { Timber.e(it, "Failed to close input stream on dispose") }
        }
    }

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
                SettingsContract.Effect.RecreateActivity -> {
                    // Перезапуск Activity для применения нового языка (ресурсы + LayoutDirection)
                    (context as? ComponentActivity)?.recreate()
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
                verticalArrangement = Arrangement.spacedBy(16.dp) // Уменьшено с 24.dp для лучшей интеграции разделителей
            ) {
                // --- Секция: Внешний вид (темы) ---
                SettingsSection(title = stringResource(R.string.settings_appearance)) {
                    listOf(
                        "SYSTEM" to stringResource(R.string.settings_theme_system),
                        "LIGHT" to stringResource(R.string.settings_theme_light),
                        "DARK" to stringResource(R.string.settings_theme_dark)
                    ).forEach { (mode, label) ->
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .clickable { viewModel.onAction(SettingsContract.Action.ChangeThemeMode(mode)) }
                                .padding(vertical = 4.dp),
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
                }

                HorizontalDivider(thickness = 0.5.dp)

                // --- Секция: Язык (динамический список) ---
                SettingsSection(title = stringResource(R.string.settings_language)) {
                    viewModel.supportedLanguages.forEach { lang ->
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .clickable { viewModel.onAction(SettingsContract.Action.ChangeLanguage(lang.code)) }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = state.currentLanguage == lang.code,
                                onClick = { viewModel.onAction(SettingsContract.Action.ChangeLanguage(lang.code)) }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(lang.displayName)
                        }
                    }
                }

                HorizontalDivider(thickness = 0.5.dp)

                // --- Секция: Уведомления ---
                SettingsSection(title = stringResource(R.string.settings_notifications)) {
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
                }

                HorizontalDivider(thickness = 0.5.dp)

                // --- Секция: Данные (экспорт/импорт) ---
                SettingsSection(title = stringResource(R.string.settings_data)) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
    }
}

/**
 * Переиспользуемая секция настроек с заголовком.
 *
 * @param title Заголовок секции (стиль: titleMedium, цвет: onSurfaceVariant по стандарту Material 3).
 * @param content Содержимое секции (список опций, переключатели и т.д.).
 */
@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant // Стандарт Material 3 для второстепенных заголовков
        )
        content()
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
