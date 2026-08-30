package com.knittrac.app.platform.ads
interface AdManager {
    fun initialize()
    fun showBanner()
    fun hideBanner()
    fun showInterstitial()
    fun showRewarded()
    val isInterstitialLoaded: Boolean
    val isRewardedLoaded: Boolean
    var isAdsEnabled: Boolean
}
