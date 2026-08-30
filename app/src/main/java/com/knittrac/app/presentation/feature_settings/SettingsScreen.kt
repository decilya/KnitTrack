package com.knittrac.app.presentation.feature_settings
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.knittrac.app.R
import kotlinx.coroutines.flow.collectLatest
@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is SettingsContract.Effect.RecreateActivity -> (context as? ComponentActivity)?.recreate()
                is SettingsContract.Effect.ShowError -> { }
            }
        }
    }
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(text = stringResource(R.string.settings_title), style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = if (state.isPremium) "Premium" else stringResource(R.string.settings_premium_status))
        Button(onClick = { viewModel.onIntent(SettingsContract.Intent.BuyPremium) }, enabled = !state.isLoading && !state.isPremium, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.settings_buy_premium))
        }
        Spacer(modifier = Modifier.height(24.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            Button(onClick = { viewModel.onIntent(SettingsContract.Intent.ChangeLanguage("ru")) }) { Text(stringResource(R.string.settings_language_ru)) }
            Button(onClick = { viewModel.onIntent(SettingsContract.Intent.ChangeLanguage("en")) }) { Text(stringResource(R.string.settings_language_en)) }
        }
    }
}
