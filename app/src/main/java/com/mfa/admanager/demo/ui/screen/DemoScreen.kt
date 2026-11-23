package com.mfa.admanager.demo.ui.screen


import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.ViewGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdView
import com.mfa.admanager.admob.AdMob
import com.mfa.admanager.demo.R
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

private const val INTERSTITIAL_COOLDOWN_KEY = "demo_interstitial"
private const val REWARDED_COOLDOWN_KEY = "demo_rewarded"

private enum class DemoCooldown(
    val label: String,
    val durationMillis: Long?
) {
    DISABLED("Off", null),
    ONE_MINUTE("1 min", 60_000L),
    FIVE_MINUTES("5 min", 5 * 60_000L);

    fun toPolicy(): AdMob.Cooldown {
        return durationMillis?.let {
            AdMob.Cooldown.Enabled(it)
        } ?: AdMob.Cooldown.Disabled
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdManagerDemoScreen() {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val unityGameId = stringResource(R.string.unity_game_id)
    val unityBannerId = stringResource(R.string.unity_banner_id)
    val unityInterstitialId = stringResource(R.string.unity_interstitial_id)
    val unityRewardedId = stringResource(R.string.unity_reward_id)
    val admobBannerIdBidding = stringResource(R.string.admob_banner_id_bidding)
    val admobInterstitialIdBidding = stringResource(R.string.admob_interstitial_id_bidding)
    val admobRewardedIdBidding = stringResource(R.string.admob_rewarded_id_bidding)
    val admobBannerId = stringResource(R.string.admob_banner_id)
    val admobInterstitialId = stringResource(R.string.admob_interstitial_id)
    val admobRewardedId = stringResource(R.string.admob_rewarded_id)

    var isPremium by remember { mutableStateOf(false) }
    var isInitialized by remember { mutableStateOf(false) }

    var interstitialCooldown by remember {
        mutableStateOf(DemoCooldown.DISABLED)
    }

    var rewardedCooldown by remember {
        mutableStateOf(DemoCooldown.ONE_MINUTE)
    }

    var interstitialRemaining by remember {
        mutableLongStateOf(0L)
    }

    var rewardedRemaining by remember {
        mutableLongStateOf(0L)
    }

    var statusMessage by remember {
        mutableStateOf("Initializing Google Mobile Ads...")
    }

    // Initialize the manager once. Do not reinitialize it every
    // time the Premium simulation switch changes.
    LaunchedEffect(context) {
        AdMob.setCooldown(
            key = INTERSTITIAL_COOLDOWN_KEY,
            cooldown = interstitialCooldown.toPolicy()
        )

        AdMob.setCooldown(
            key = REWARDED_COOLDOWN_KEY,
            cooldown = rewardedCooldown.toPolicy()
        )

        AdMob.initialize(
            context = context.applicationContext,
//            gameId = unityGameId,
            bannerAdUnitId = admobBannerId,
            interstitialAdUnitId = admobInterstitialId,
            rewardedAdUnitId = admobRewardedId,
            minDelay = 0L,
            isPremium = false
        ) {
            isInitialized = true
            statusMessage = "MFA-AdMob initialized successfully."
        }


//        AdMob.initialize(
//            context = context.applicationContext,
//            gameId = "800448733",
//            isPremium = false,
//            onInitialized = {
//                AdMob.loadInterstitial(
//                    placementId = "BP_Interstitial_Android",
//                    onLoaded = {
//                        AdMob.showInterstitial(
//                            activity = activity!!,
//                            placementId = "BP_Interstitial_Android",
//                            cooldownKey = INTERSTITIAL_COOLDOWN_KEY,
//                            onSuccess = {
//                                Toast.makeText(context, "Interstitial shown", Toast.LENGTH_SHORT).show()
//                                Log.d("AdMobAds", "Interstitial shown")
//
//                            },
//                            onFailed = { error ->
//                                Toast.makeText(context, "Interstitial failed: $error", Toast.LENGTH_SHORT).show()
//                                Log.e("AdMobAds", "Interstitial failed: $error")
//                            }
//                        )
//                    },
//                    onFailed = { error ->
//                        Log.e("AdMobAds", "Interstitial failed to load: $error")
//                        Toast.makeText(context, "Interstitial failed to load: $error", Toast.LENGTH_SHORT).show()
//                    }
//                )
//            },
//            onFailed = { error ->
//                Log.e("AdMobAds", "AdMob failed to initialize: $error")
//                Toast.makeText(context, "AdMob failed to initialize: $error", Toast.LENGTH_SHORT).show()
//            }
//        )
    }

    // Apply cooldown configuration only when it changes.
    LaunchedEffect(interstitialCooldown) {
        AdMob.setCooldown(
            key = INTERSTITIAL_COOLDOWN_KEY,
            cooldown = interstitialCooldown.toPolicy()
        )
    }

    LaunchedEffect(rewardedCooldown) {
        AdMob.setCooldown(
            key = REWARDED_COOLDOWN_KEY,
            cooldown = rewardedCooldown.toPolicy()
        )
    }

    // Premium is an entitlement simulation, not a new initialization.
    LaunchedEffect(isPremium, isInitialized) {
        if (isInitialized) {
            AdMob.setPremiumMode(isPremium)

            statusMessage = if (isPremium) {
                "Premium enabled. Ads are disabled."
            } else {
                "Premium disabled. Ads are enabled."
            }
        }
    }

    // Update the remaining cooldown displayed in the UI.
    LaunchedEffect(isInitialized) {
        while (isInitialized) {
            interstitialRemaining =
                AdMob.getCooldownRemainingMillis(
                    INTERSTITIAL_COOLDOWN_KEY
                )

            rewardedRemaining =
                AdMob.getCooldownRemainingMillis(
                    REWARDED_COOLDOWN_KEY
                )

            delay(1000L.milliseconds)
        }
    }

    val adWidth = rememberAdWidth()

    // Create one banner instance per relevant configuration.
    val bannerAdView = remember(
        isInitialized,
        isPremium,
        adWidth,
        context
    ) {
        if (isInitialized && !isPremium) {
            AdMob.getBannerAdView(
                context = context,
            )
        } else {
            null
        }
    }

    // Release the banner when it leaves this composition.
    DisposableEffect(bannerAdView) {
        onDispose {
            bannerAdView?.destroy()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "MFA-AdMob",
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = "Demo & Testing",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            PremiumSimulationCard(
                isPremium = isPremium,
                onPremiumChanged = { enabled ->
                    isPremium = enabled
                }
            )

            SectionHeading(
                title = "Ad configuration",
                subtitle = "Configure cooldown independently for each placement."
            )

            CooldownConfigurationCard(
                title = "Interstitial",
                description = "Full-screen ad between app content.",
                selected = interstitialCooldown,
                remainingMillis = interstitialRemaining,
                enabled = isInitialized && !isPremium,
                onSelected = {
                    interstitialCooldown = it
                }
            )

            CooldownConfigurationCard(
                title = "Rewarded",
                description = "Ad that grants an in-app reward.",
                selected = rewardedCooldown,
                remainingMillis = rewardedRemaining,
                enabled = isInitialized && !isPremium,
                onSelected = {
                    rewardedCooldown = it
                }
            )

            SectionHeading(
                title = "Advertisement",
                subtitle = "Banner placement preview."
            )

            BannerDemo(
                isPremium = isPremium,
                isInitialized = isInitialized,
                adView = bannerAdView
            )

            SectionHeading(
                title = "Ad tests",
                subtitle = "Test loading, presentation, cooldown and callbacks."
            )

            AdTestButton(
                icon = {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null
                    )
                },
                title = "Show interstitial",
                description = if (interstitialRemaining > 0L) {
                    "Cooldown remaining: ${formatDuration(interstitialRemaining)}"
                } else {
                    "Display a full-screen interstitial ad."
                },
                enabled = isInitialized && !isPremium,
                onClick = {
                    val currentActivity = activity

                    if (currentActivity == null) {
                        statusMessage = "Unable to find an Activity."
                    } else {
                        statusMessage = "Requesting interstitial ad..."

                        AdMob.showInterstitial(
                            activity = currentActivity,
                            cooldownKey = INTERSTITIAL_COOLDOWN_KEY,
                            onSuccess = {
                                statusMessage =
                                    "Interstitial dismissed or skipped in Premium mode."
                            },
                            onFailed = { error ->
                                statusMessage =
                                    "Interstitial unavailable: $error"
                            }
                        )
                    }
                }
            )

            AdTestButton(
                icon = {
                    Icon(
                        imageVector = Icons.Default.CardGiftcard,
                        contentDescription = null
                    )
                },
                title = "Show rewarded",
                description = if (rewardedRemaining > 0L) {
                    "Cooldown remaining: ${formatDuration(rewardedRemaining)}"
                } else {
                    "Display an ad and test reward callbacks."
                },
                enabled = isInitialized && !isPremium,
                onClick = {
                    val currentActivity = activity

                    if (currentActivity == null) {
                        statusMessage = "Unable to find an Activity."
                    } else {
                        statusMessage = "Requesting rewarded ad..."

                        AdMob.showRewarded(
                            activity = currentActivity,
                            cooldownKey = REWARDED_COOLDOWN_KEY,
                            onDismiss = { reward ->
                                statusMessage =
                                    "Reward earned: ${reward.amount} ${reward.type}"
                            },
                            onAdDismissed = {
                                statusMessage =
                                    "Rewarded ad closed."
                            },
                            onFailed = {
                                statusMessage =
                                    "Rewarded ad unavailable or cooldown active."
                            }
                        )
                    }
                }
            )

            StatusCard(
                message = statusMessage,
                isPremium = isPremium,
                isInitialized = isInitialized
            )

            Text(
                text = "Demo only. Use Google's test ad units during development.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun SectionHeading(
    title: String,
    subtitle: String
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PremiumSimulationCard(
    isPremium: Boolean,
    onPremiumChanged: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(30.dp)
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp)
                ) {
                    Text(
                        text = "Premium simulation",
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = if (isPremium) "Premium enabled" else "Free user",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Switch(
                    checked = isPremium,
                    onCheckedChange = onPremiumChanged
                )
            }

            HorizontalDivider()

            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )

                Text(
                    text = "Simulates a premium entitlement. It does not verify a real purchase.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CooldownConfigurationCard(
    title: String,
    description: String,
    selected: DemoCooldown,
    remainingMillis: Long,
    enabled: Boolean,
    onSelected: (DemoCooldown) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Timer,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 10.dp)
                ) {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DemoCooldown.entries.forEach { option ->
                    FilterChip(
                        selected = selected == option,
                        onClick = { onSelected(option) },
                        enabled = enabled,
                        label = {
                            Text(option.label)
                        }
                    )
                }
            }

            Text(
                text = when {
                    !enabled -> "Cooldown controls unavailable."
                    remainingMillis > 0L ->
                        "Remaining: ${formatDuration(remainingMillis)}"
                    selected == DemoCooldown.DISABLED ->
                        "Cooldown disabled."
                    else ->
                        "Ready. No active cooldown."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun BannerDemo(
    isPremium: Boolean,
    isInitialized: Boolean,
    adView: AdView?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            when {
                isPremium -> {
                    BannerPlaceholder(
                        message = "Banner disabled in Premium mode"
                    )
                }

                !isInitialized -> {
                    BannerPlaceholder(
                        message = "Waiting for AdMob initialization..."
                    )
                }

                adView != null -> {
                    AndroidView(
                        modifier = Modifier.fillMaxWidth(),
                        factory = {
                            (adView.parent as? ViewGroup)?.removeView(adView)
                            adView
                        }
                    )
                }

                else -> {
                    BannerPlaceholder(
                        message = "Banner unavailable"
                    )
                }
            }
        }
    }
}

@Composable
private fun BannerPlaceholder(
    message: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(Modifier.height(4.dp))

        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun AdTestButton(
    icon: @Composable () -> Unit,
    title: String,
    description: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    OutlinedButton(
        modifier = Modifier.fillMaxWidth(),
        enabled = enabled,
        onClick = onClick,
        shape = RoundedCornerShape(16.dp)
    ) {
        icon()

        Spacer(Modifier.width(12.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 4.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StatusCard(
    message: String,
    isPremium: Boolean,
    isInitialized: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = if (isPremium) {
                    Icons.Default.Star
                } else {
                    Icons.Default.CheckCircle
                },
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )

            Column(
                modifier = Modifier.padding(start = 10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = when {
                        !isInitialized -> "Initializing"
                        isPremium -> "Premium mode"
                        else -> "Ad status"
                    },
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
private fun rememberAdWidth(): Int {
    val configuration = LocalConfiguration.current
    // AdSize's inline adaptive width is expressed in dp.
    return (configuration.screenWidthDp - 32).coerceAtLeast(1)
}

private fun formatDuration(milliseconds: Long): String {
    val totalSeconds = (milliseconds.coerceAtLeast(0L) + 999L) / 1000L
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L

    return if (minutes > 0L) {
        "%d:%02d".format(minutes, seconds)
    } else {
        "${seconds}s"
    }
}

private fun Context.findActivity(): Activity? {
    var currentContext = this

    while (currentContext is ContextWrapper) {
        if (currentContext is Activity) {
            return currentContext
        }

        val nextContext = currentContext.baseContext
        if (nextContext === currentContext) {
            return null
        }

        currentContext = nextContext
    }

    return currentContext as? Activity
}
