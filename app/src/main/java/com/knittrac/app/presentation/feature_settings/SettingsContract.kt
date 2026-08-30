package com.knittrac.app.presentation.feature_settings
object SettingsContract {
    data class State(val currentLanguage: String = "ru", val isPremium: Boolean = false, val isLoading: Boolean = false)
    sealed class Intent {
        data class ChangeLanguage(val langCode: String) : Intent()
        object BuyPremium : Intent()
    }
    sealed class Effect {
        object RecreateActivity : Effect()
        data class ShowError(val message: String) : Effect()
    }
}
