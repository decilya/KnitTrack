package com.knittrac.app.presentation.feature_settings
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.knittrac.app.core.common.Result
import com.knittrac.app.core.localization.LocaleManager
import com.knittrac.app.platform.payments.PaymentManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
@HiltViewModel
class SettingsViewModel @Inject constructor(private val localeManager: LocaleManager, private val paymentManager: PaymentManager, private val dataStore: DataStore<Preferences>) : ViewModel() {
    private val _state = MutableStateFlow(SettingsContract.State())
    val state: StateFlow<SettingsContract.State> = _state.asStateFlow()
    private val _effect = MutableSharedFlow<SettingsContract.Effect>()
    val effect: SharedFlow<SettingsContract.Effect> = _effect.asSharedFlow()
    init {
        viewModelScope.launch {
            val lang = localeManager.getCurrentLocaleFlow().first().language
            val isPremium = dataStore.data.first()[booleanPreferencesKey("is_premium")] ?: false
            _state.value = _state.value.copy(currentLanguage = lang, isPremium = isPremium)
        }
    }
    fun onIntent(intent: SettingsContract.Intent) {
        when (intent) {
            is SettingsContract.Intent.ChangeLanguage -> changeLanguage(intent.langCode)
            is SettingsContract.Intent.BuyPremium -> buyPremium()
        }
    }
    private fun changeLanguage(langCode: String) {
        viewModelScope.launch {
            localeManager.setLanguage(langCode)
            _state.value = _state.value.copy(currentLanguage = langCode)
            _effect.emit(SettingsContract.Effect.RecreateActivity)
        }
    }
    private fun buyPremium() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            val result = paymentManager.purchaseSubscription("knittrac_premium_monthly")
            if (result is Result.Success) {
                dataStore.edit { it[booleanPreferencesKey("is_premium")] = true }
                _state.value = _state.value.copy(isPremium = true, isLoading = false)
            } else {
                _state.value = _state.value.copy(isLoading = false)
                _effect.emit(SettingsContract.Effect.ShowError("Ошибка покупки"))
            }
        }
    }
}
