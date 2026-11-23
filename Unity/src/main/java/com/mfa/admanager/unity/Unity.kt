
package com.mfa.admanager.unity

import android.app.Activity
import android.content.Context
import android.content.pm.ApplicationInfo
import android.util.Log
import com.unity3d.ads.AdExpiredListener
import com.unity3d.ads.InitializationConfiguration
import com.unity3d.ads.InitializationListener
import com.unity3d.ads.InterstitialAd
import com.unity3d.ads.InterstitialShowListener
import com.unity3d.ads.LoadConfiguration
import com.unity3d.ads.LoadListener
import com.unity3d.ads.ShowConfiguration
import com.unity3d.ads.ShowFinishState
import com.unity3d.ads.UnityAds
import com.unity3d.ads.UnityAdsError
import com.unity3d.ads.UnityAdsExperimental
import java.util.concurrent.ConcurrentHashMap

object Unity {

    private const val TAG = "Unity"

    private const val DEFAULT_INTERSTITIAL_PLACEMENT =
        "interstitial"

    private data class Configuration(
        val isDebugMode: Boolean,
        val bannerPlacementId: String,
        val interstitialPlacementId: String,
        val rewardedPlacementId: String
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

    /**
     * Initializes Unity Ads and configures this manager.
     *
     * minDelay is retained for API consistency with AdMob.
     * Its unit is minutes.
     *
     * minDelay = 0 disables the default interstitial cooldown.
     *
     * Unity Ads currently uses the interstitial placement
     * configured here. Banner and Rewarded implementations
     * will use their respective placement IDs when added.
     */
    @OptIn(UnityAdsExperimental::class)
    fun initialize(
        context: Context,
        gameId: String,
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

        require(interstitialAdUnitId.isNotBlank()) {
            "Unity Ads Interstitial Placement ID cannot be blank."
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

            configuration = Configuration(
                isDebugMode = isDebugMode(appContext),
                bannerPlacementId = bannerAdUnitId,
                interstitialPlacementId = interstitialAdUnitId,
                rewardedPlacementId = rewardedAdUnitId
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

        val unityConfiguration =
            InitializationConfiguration.Builder(gameId)
                .withContext(appContext)
                .withTestMode(isDebugMode(appContext))
                .build()

        // The SDK requires the Game ID, not a placement ID.
        // Use the configured Game ID supplied to initialize.
        UnityAds.initialize(
            unityConfiguration,
            InitializationListener { error ->

                synchronized(initializationLock) {

                    initializationStarted = false
                    isInitialized = error == null

                    val callbacks =
                        pendingInitializationCallbacks.toList()

                    pendingInitializationCallbacks.clear()

                    if (error == null) {

                        Log.d(
                            TAG,
                            "Unity Ads initialized successfully."
                        )

                        if (!this.isPremium) {
                            preloadInterstitialInternal()
                        }

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

                    } else {

                        Log.e(
                            TAG,
                            "Unity Ads initialization failed: ${error.message}"
                        )
                    }
                }
            }
        )
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

                val elapsed =
                    (now - lastShown).coerceAtLeast(0L)

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
            Log.e(TAG, "Unity Ads has not been initialized.")
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

    private fun getInterstitialPlacementId(): String {
        return configuration.interstitialPlacementId
    }

    // ================= INTERSTITIAL =================

    private fun preloadInterstitialInternal() {

        if (
            !isInitialized ||
            isPremium ||
            interstitialLoading ||
            interstitialAd != null
        ) {
            return
        }

        interstitialLoading = true

        val loadConfiguration =
            LoadConfiguration.Builder(
                getInterstitialPlacementId()
            ).build()

        InterstitialAd.load(
            loadConfiguration,
            LoadListener {  ad, error ->

                interstitialLoading = false

                if (ad == null) {

                    interstitialAd = null

                    Log.e(
                        TAG,
                        "Interstitial failed to load: " +
                                (error?.message ?: "Unknown error.")
                    )

                    return@LoadListener
                }

                ad.onAdExpired = AdExpiredListener {
                    if (interstitialAd === ad) {
                        interstitialAd = null
                    }

                    Log.d(TAG, "Interstitial expired.")

                    preloadInterstitialInternal()
                }

                interstitialAd = ad

                Log.d(TAG, "Interstitial loaded.")
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
            onFailed("Unity Ads has not been initialized.")
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

        if (!tryMarkAdShown(cooldownKey)) {

            val wait =
                getCooldownRemainingMillis(cooldownKey)

            onFailed("Cooldown active: ${wait}ms remaining.")
            return
        }

        interstitialAd = null

        val showConfiguration = ShowConfiguration.Builder().build()

        try {
            ad.show(
                activity,
                showConfiguration,
                object : InterstitialShowListener {

                    override fun onStarted(
                        unityAd: InterstitialAd
                    ) {
                        Log.d(TAG, "Interstitial started.")
                    }

                    override fun onClicked(
                        unityAd: InterstitialAd
                    ) {
                        Log.d(TAG, "Interstitial clicked.")
                    }

                    override fun onCompleted(
                        unityAd: InterstitialAd,
                        state: ShowFinishState
                    ) {
                        Log.d(
                            TAG,
                            "Interstitial completed: $state"
                        )

                        onSuccess()
                        preloadInterstitialInternal()
                    }

                    override fun onFailed(
                        unityAd: InterstitialAd,
                        error: UnityAdsError
                    ) {
                        Log.e(
                            TAG,
                            "Interstitial failed to show: ${error.message}"
                        )

                        resetCooldown(cooldownKey)

                        onFailed(error.message)
                        preloadInterstitialInternal()
                    }
                }
            )

        } catch (exception: Exception) {

            resetCooldown(cooldownKey)

            Log.e(
                TAG,
                "Failed to show interstitial.",
                exception
            )

            onFailed(
                exception.message
                    ?: "Failed to show interstitial."
            )

            preloadInterstitialInternal()
        }
    }
}
