package com.knittrac.app.presentation.feature_settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.knittrac.app.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()

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
        }
    ) { paddingValues ->
        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Text(text = "Внешний вид", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                
                val options = listOf("SYSTEM" to "Как в системе", "LIGHT" to "Светлая", "DARK" to "Темная")
                options.forEach { (mode, label) ->
                    Row(modifier = Modifier.fillMaxWidth().clickable { viewModel.onAction(SettingsContract.Action.ChangeThemeMode(mode)) }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = state.themeMode == mode, onClick = { viewModel.onAction(SettingsContract.Action.ChangeThemeMode(mode)) })
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = label)
                    }
                }

                HorizontalDivider()

                Text(text = "Уведомления", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { viewModel.onAction(SettingsContract.Action.ToggleNotifications(!state.notificationsEnabled)) }.padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Разрешить уведомления о вязании")
                    Switch(
                        checked = state.notificationsEnabled,
                        onCheckedChange = { viewModel.onAction(SettingsContract.Action.ToggleNotifications(it)) }
                    )
                }
            }
        }
    }
}
