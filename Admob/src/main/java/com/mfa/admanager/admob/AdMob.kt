package com.mfa.admanager.admob

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



import java.util.concurrent.ConcurrentHashMap

object AdMob {

    private const val TAG = "AdMob"

    private const val BANNER_AD_UNIT_TESTER_ID =
        "ca-app-pub-3940256099942544/9214589741"

    private const val INTERSTITIAL_AD_UNIT_TESTER_ID =
        "ca-app-pub-3940256099942544/1033173712"

    private const val REWARDED_AD_UNIT_TESTER_ID =
        "ca-app-pub-3940256099942544/5224354917"

    private const val DEFAULT_INTERSTITIAL_PLACEMENT = "interstitial"
    private const val DEFAULT_REWARDED_PLACEMENT = "rewarded"

    /**
     * Cooldown policy shared by all supported ad formats.
     *
     * Disabled:
     * No time-based cooldown.
     *
     * Enabled:
     * Minimum interval between ads using the same cooldown key.
     */
    sealed class Cooldown {
        data object Disabled : Cooldown()
        data class Enabled(
            val durationMillis: Long
        ) : Cooldown() {
            init {
                require(durationMillis >= 0L) {
                    "Cooldown duration cannot be negative."
                }
            }
        }
    }

    private data class Configuration(
        val isDebugMode: Boolean,
        val bannerAdUnitId: String,
        val interstitialAdUnitId: String,
        val rewardedAdUnitId: String
    )

    private lateinit var configuration: Configuration

    @Volatile
    var isInitialized: Boolean = false
        private set

    @Volatile
    private var isPremium: Boolean = false

    private var initializationStarted = false
    private val initializationLock = Any()
    private val pendingInitializationCallbacks =
        mutableListOf<() -> Unit>()

    private val cooldownLock = Any()

    private val cooldownPolicies =
        ConcurrentHashMap<String, Cooldown>()

    private val lastShownAt =
        ConcurrentHashMap<String, Long>()

    private var interstitialAd: InterstitialAd? = null
    private var interstitialLoading = false

    private var applicationContext: Context? = null

    /**
     * Initializes Google Mobile Ads and configures this manager.
     *
     * minDelay is retained for backward compatibility.
     * Its unit is minutes.
     *
     * minDelay = 0 disables the default interstitial cooldown.
     */
    fun initialize(
        context: Context,
        bannerAdUnitId: String,
        interstitialAdUnitId: String,
        rewardedAdUnitId: String,
        minDelay: Long = 0L,
        isPremium: Boolean = false,
        onInitialized: (() -> Unit)? = null
    ) {
        require(minDelay >= 0L) {
            "minDelay cannot be negative."
        }

        val appContext = context.applicationContext

        synchronized(initializationLock) {
            if (isInitialized) {
                updatePremiumMode(isPremium)

                setCooldown(
                    DEFAULT_INTERSTITIAL_PLACEMENT,
                    if (minDelay == 0L) {
                        Cooldown.Disabled
                    } else {
                        Cooldown.Enabled(
                            Math.multiplyExact(minDelay, 60_000L)
                        )
                    }
                )

                onInitialized?.invoke()
                return
            }

            if (onInitialized != null) {
                pendingInitializationCallbacks.add(onInitialized)
            }

            if (initializationStarted) {
                return
            }

            initializationStarted = true
            applicationContext = appContext

            configuration = Configuration(
                isDebugMode = isDebugMode(appContext),
                bannerAdUnitId = bannerAdUnitId,
                interstitialAdUnitId = interstitialAdUnitId,
                rewardedAdUnitId = rewardedAdUnitId
            )

            this.isPremium = isPremium

            setCooldown(
                DEFAULT_INTERSTITIAL_PLACEMENT,
                if (minDelay == 0L) {
                    Cooldown.Disabled
                } else {
                    Cooldown.Enabled(
                        Math.multiplyExact(minDelay, 60_000L)
                    )
                }
            )
        }

        MobileAds.initialize(appContext) {
            synchronized(initializationLock) {
                isInitialized = true
                initializationStarted = false

                if (!isPremium) {
                    preloadInterstitialInternal()
                }

                val callbacks =
                    pendingInitializationCallbacks.toList()

                pendingInitializationCallbacks.clear()

                callbacks.forEach { callback ->
                    runCatching { callback() }
                        .onFailure {
                            Log.e(
                                TAG,
                                "Initialization callback failed.",
                                it
                            )
                        }
                }
            }
        }
    }

    // ================= PREMIUM =================

    /**
     * Updates premium mode after initialization.
     *
     * Call this when the user's entitlement changes.
     */
    fun setPremiumMode(enabled: Boolean) {
        updatePremiumMode(enabled)
    }

    private fun updatePremiumMode(enabled: Boolean) {
        isPremium = enabled

        if (enabled) {
            interstitialAd = null
        } else if (isInitialized) {
            preloadInterstitialInternal()
        }
    }

    // ================= UNIVERSAL COOLDOWN =================

    /**
     * Configures a cooldown for any placement or group.
     *
     * Use the same key across different ad formats to share
     * a cooldown. Use different keys for independent cooldowns.
     */
    fun setCooldown(
        key: String,
        cooldown: Cooldown
    ) {
        require(key.isNotBlank()) {
            "Cooldown key cannot be blank."
        }

        synchronized(cooldownLock) {
            cooldownPolicies[key] = cooldown
        }
    }

    /**
     * Returns the remaining cooldown in milliseconds.
     *
     * Returns 0 when the placement is allowed to show an ad.
     */
    fun getCooldownRemainingMillis(
        key: String
    ): Long {
        require(key.isNotBlank()) {
            "Cooldown key cannot be blank."
        }

        synchronized(cooldownLock) {
            val policy = cooldownPolicies[key]
                ?: Cooldown.Disabled

            if (policy is Cooldown.Disabled) {
                return 0L
            }

            val duration =
                (policy as Cooldown.Enabled).durationMillis

            if (duration == 0L) {
                return 0L
            }

            val lastShown = lastShownAt[key]
                ?: return 0L

            val elapsed =
                (System.currentTimeMillis() - lastShown)
                    .coerceAtLeast(0L)

            return (duration - elapsed).coerceAtLeast(0L)
        }
    }

    /**
     * Checks whether a placement can currently show an ad.
     *
     * This method does not reserve a slot. Call
     * tryMarkAdShown() immediately before displaying the ad.
     */
    fun canShowAd(key: String): Boolean {
        return getCooldownRemainingMillis(key) == 0L
    }

    /**
     * Atomically checks the cooldown and records the start
     * of an ad presentation.
     *
     * Returns false if the cooldown is still active.
     * Call only when an ad is actually about to be shown.
     *
     * This method can also be used by future ad formats,
     * including App Open Ads.
     */
    fun tryMarkAdShown(key: String): Boolean {
        require(key.isNotBlank()) {
            "Cooldown key cannot be blank."
        }

        synchronized(cooldownLock) {
            val policy = cooldownPolicies[key]
                ?: Cooldown.Disabled

            if (policy is Cooldown.Disabled) {
                return true
            }

            val duration =
                (policy as Cooldown.Enabled).durationMillis

            val now = System.currentTimeMillis()
            val lastShown = lastShownAt[key]

            if (lastShown != null && duration > 0L) {
                val elapsed = (now - lastShown).coerceAtLeast(0L)

                if (elapsed < duration) {
                    return false
                }
            }

            if (duration > 0L) {
                lastShownAt[key] = now
            }

            return true
        }
    }

    /**
     * Clears a placement's cooldown history.
     *
     * Use only when the application intentionally needs
     * to reset its cooldown state.
     */
    fun resetCooldown(key: String) {
        require(key.isNotBlank()) {
            "Cooldown key cannot be blank."
        }

        synchronized(cooldownLock) {
            lastShownAt.remove(key)
        }
    }

    // ================= INTERNAL HELPERS =================

    private fun ensureInitialized(): Boolean {
        if (!isInitialized) {
            Log.e(TAG, "AdMob has not been initialized.")
            return false
        }

        return true
    }

    private fun isDebugMode(context: Context): Boolean {
        return (
                context.applicationInfo.flags and
                        ApplicationInfo.FLAG_DEBUGGABLE
                ) != 0
    }

    private fun getBannerAdUnitId(): String {
        return if (configuration.isDebugMode) {
            BANNER_AD_UNIT_TESTER_ID
        } else {
            configuration.bannerAdUnitId
        }
    }

    private fun getInterstitialAdUnitId(): String {
        return if (configuration.isDebugMode) {
            INTERSTITIAL_AD_UNIT_TESTER_ID
        } else {
            configuration.interstitialAdUnitId
        }
    }

    private fun getRewardedAdUnitId(): String {
        return if (configuration.isDebugMode) {
            REWARDED_AD_UNIT_TESTER_ID
        } else {
            configuration.rewardedAdUnitId
        }
    }

    // ================= BANNER =================

    fun getBannerAdView(
        context: Context,
        adWidth: Int = 0
    ): AdView? {
        if (!ensureInitialized() || isPremium) {
            return null
        }

        return AdView(context).apply {
            adUnitId = getBannerAdUnitId()
            setAdSize(
                if (adWidth <= 0) {
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

    private fun preloadInterstitialInternal() {
        if (!isInitialized || isPremium || interstitialLoading) {
            return
        }

        val context = applicationContext ?: return

        interstitialLoading = true

        InterstitialAd.load(
            context,
            getInterstitialAdUnitId(),
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {

                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialLoading = false
                    interstitialAd = ad

                    Log.d(TAG, "Interstitial loaded.")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialLoading = false
                    interstitialAd = null

                    Log.e(
                        TAG,
                        "Interstitial failed to load: ${error.message}"
                    )
                }
            }
        )
    }

    fun showInterstitial(
        activity: Activity,
        onSuccess: () -> Unit = {},
        onFailed: (String) -> Unit = {},
        cooldownKey: String = DEFAULT_INTERSTITIAL_PLACEMENT
    ) {
        if (!ensureInitialized()) {
            onFailed("AdMob has not been initialized.")
            return
        }

        if (isPremium) {
            onSuccess()
            return
        }

        val remaining =
            getCooldownRemainingMillis(cooldownKey)

        if (remaining > 0L) {
            onFailed("Cooldown active: ${remaining}ms remaining.")
            return
        }

        val ad = interstitialAd

        if (ad == null) {
            onFailed("Interstitial ad is not ready.")
            preloadInterstitialInternal()
            return
        }

        ad.fullScreenContentCallback =
            object : FullScreenContentCallback() {

                override fun onAdDismissedFullScreenContent() {
                    interstitialAd = null

                    onSuccess()
                    preloadInterstitialInternal()
                }

                override fun onAdFailedToShowFullScreenContent(
                    adError: AdError
                ) {
                    interstitialAd = null

                    onFailed(adError.message)
                    preloadInterstitialInternal()
                }
            }

        if (!tryMarkAdShown(cooldownKey)) {
            val wait =
                getCooldownRemainingMillis(cooldownKey)

            onFailed("Cooldown active: ${wait}ms remaining.")
            return
        }

        try {
            interstitialAd = null
            ad.show(activity)
        } catch (exception: Exception) {
            resetCooldown(cooldownKey)

            Log.e(TAG, "Failed to show interstitial.", exception)
            onFailed(
                exception.message ?: "Failed to show interstitial."
            )

            preloadInterstitialInternal()
        }
    }

    // ================= REWARDED =================

    /**
     * Shows a rewarded ad.
     *
     * onDismiss retains its existing parameter name for
     * source compatibility, but now runs only when the
     * Google SDK confirms that a reward was earned.
     *
     * onAdDismissed runs when the full-screen ad is closed,
     * independently of whether a reward was earned.
     */
    fun showRewarded(
        activity: Activity,
        onDismiss: (RewardItem) -> Unit = {},
        onFailed: () -> Unit = {},
        cooldownKey: String = DEFAULT_REWARDED_PLACEMENT,
        onAdDismissed: () -> Unit = {}
    ) {
        if (!ensureInitialized()) {
            onFailed()
            return
        }

        if (isPremium) {
            onFailed()
            return
        }

        if (!canShowAd(cooldownKey)) {
            onFailed()
            return
        }

        preloadRewarded(
            context = activity.applicationContext,
            onLoaded = { ad ->

                if (!tryMarkAdShown(cooldownKey)) {
                    onFailed()
                    return@preloadRewarded
                }

                ad.fullScreenContentCallback =
                    object : FullScreenContentCallback() {

                        override fun onAdDismissedFullScreenContent() {
                            onAdDismissed()
                        }

                        override fun onAdFailedToShowFullScreenContent(
                            adError: AdError
                        ) {
                            resetCooldown(cooldownKey)

                            Log.e(
                                TAG,
                                "Rewarded failed to show: ${adError.message}"
                            )

                            onFailed()
                        }
                    }

                try {
                    ad.show(activity) { rewardItem ->
                        onDismiss(rewardItem)
                    }
                } catch (exception: Exception) {
                    resetCooldown(cooldownKey)

                    Log.e(TAG, "Failed to show rewarded ad.", exception)
                    onFailed()
                }
            },
            onFailed = {
                onFailed()
            }
        )
    }

    private fun preloadRewarded(
        context: Context,
        onLoaded: (RewardedAd) -> Unit,
        onFailed: () -> Unit
    ) {
        if (!isInitialized || isPremium) {
            onFailed()
            return
        }

        RewardedAd.load(
            context,
            getRewardedAdUnitId(),
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {

                override fun onAdLoaded(ad: RewardedAd) {
                    Log.d(TAG, "Rewarded loaded.")
                    onLoaded(ad)
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.e(
                        TAG,
                        "Rewarded failed to load: ${error.message}"
                    )

                    onFailed()
                }
            }
        )
    }
}
