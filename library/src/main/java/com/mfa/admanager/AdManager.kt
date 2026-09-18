package com.mfa.admanager

import android.app.Activity
import android.content.Context
import android.content.pm.ApplicationInfo
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardItem
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback


object AdManager {
    private lateinit var configuration: Configuration
    private const val TAG = "AdManager"
    private const val BANNER_AD_UNIT_TESTER_ID = "ca-app-pub-3940256099942544/9214589741"
    private const val INTERSTITIAL_AD_UNIT_TESTER_ID = "ca-app-pub-3940256099942544/1033173712"
    private const val REWARDED_AD_UNIT_TESTER_ID = "ca-app-pub-3940256099942544/5224354917"
    private var lastInterstitialAd = System.currentTimeMillis()
    private var isPremium: Boolean = false
    var isInitialized: Boolean = false
    private var interstitialAd: InterstitialAd? = null

    fun initialize(
        context: Context,
        bannerAdUnitId: String,
        interstitialAdUnitId: String,
        rewardedAdUnitId: String,
        minDelay: Long = 5,
        isPremium: Boolean = false,
        onInitialized: (() -> Unit)? = null
    ) {
        AdManager.isPremium = isPremium

        if (isInitialized) {
            onInitialized?.invoke()
            return
        }

        MobileAds.initialize(context) {

            configuration = Configuration(
                isDebugMode(context),
                bannerAdUnitId,
                interstitialAdUnitId,
                rewardedAdUnitId,
                minDelay * 60 * 1000L,
                isPremium
            )

            isInitialized = true

            if (!AdManager.isPremium) {
                preloadInterstitial(context)
            }

            onInitialized?.invoke()
        }
    }
    private fun isAdsTime(message: (String) -> Unit) : Boolean {
        val elapsed = System.currentTimeMillis() - lastInterstitialAd
        val result = elapsed >= configuration.minDelay
        if (!result) {
            message("Wait ${configuration.minDelay - elapsed} ms")
        }
        return result
    }
    private fun isDebugMode(context: Context): Boolean {
        return 0 != context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE
    }
    private data class Configuration(
        val isDebugMode: Boolean,
        val bannerAdUnitId: String,
        val interstitialAdUnitId: String,
        val rewardedAdUnitId: String,
        val minDelay: Long,
        val isPremium: Boolean
    )
    private fun getBannerAdUnitID(): String = if (configuration.isDebugMode)
        BANNER_AD_UNIT_TESTER_ID else configuration.bannerAdUnitId

    private fun getInterstitialAdUnitID(): String = if (configuration.isDebugMode)
        INTERSTITIAL_AD_UNIT_TESTER_ID else configuration.interstitialAdUnitId

    private fun getRewardedAdUnitID(): String = if (configuration.isDebugMode)
        REWARDED_AD_UNIT_TESTER_ID else configuration.rewardedAdUnitId

    // ================= BANNER =================

    fun getBannerAdView(
        context: Context,
        adWidth: Int = 0
    ): AdView? {
        if (isPremium) return null

        return AdView(context).apply {
            adUnitId = getBannerAdUnitID()
            setAdSize(
                if (adWidth == 0) {
                    AdSize.BANNER
                } else {
                    AdSize.getCurrentOrientationInlineAdaptiveBannerAdSize(
                        context,
                        adWidth
                    )
                }
            )
            loadAd(AdRequest.Builder().build())
        }
    }

    // ================= INTERSTITIAL =================

    private fun preloadInterstitial(context: Context) {
        if (isPremium) return
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            context,
            getInterstitialAdUnitID(),
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    Log.d(TAG, "Interstitial loaded")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                    Log.e(TAG, "Interstitial failed: ${error.message}")
                }
            }
        )
    }

    fun showInterstitial(
        activity: Activity,
        onSuccess: () -> Unit = {},
        onFailed: (String) -> Unit = {}
    ) {

        if (isPremium) {
            onSuccess()
            return
        }
        if (!isAdsTime { onFailed(it) }) return
        interstitialAd?.let { ad ->
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    onSuccess()
                    lastInterstitialAd = System.currentTimeMillis()
                    interstitialAd = null
                    preloadInterstitial(activity)
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    onFailed(adError.message)
                    interstitialAd = null
                    preloadInterstitial(activity)
                }
            }
            ad.show(activity)
        } ?: run {
            onFailed("Interstitial ad is null")
            preloadInterstitial(activity)
        }
    }

    // ================= REWARDED =================

    private fun preloadRewarded(
        context: Context,
        onLoaded: (RewardedAd) -> Unit = {},
        onFailed: () -> Unit = {}
    ) {
        if (isPremium) return
        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(
            context,
            getRewardedAdUnitID(),
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    Log.d(TAG, "Rewarded loaded")
                    onLoaded(ad)
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.e(TAG, "Rewarded failed: ${error.message}")
                    onFailed()
                }
            }
        )
    }

    fun showRewarded(
        activity: Activity,
        onDismiss: (RewardItem) -> Unit = {},
        onFailed: () -> Unit = {}
    ) {
        if (isPremium) {
            onFailed()
            return
        }
        fun showRewardedAd(ad: RewardedAd) {
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    onDismiss(ad.rewardItem)
                }
                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Log.e(TAG, "Rewarded failed: ${adError.message}")
                    onFailed()
                }
            }
            ad.show(activity) {}
        }
        preloadRewarded(activity, onLoaded = {
            showRewardedAd(it)
        }, onFailed = { onFailed() })
    }
}
