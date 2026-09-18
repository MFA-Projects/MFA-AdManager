package com.mfa.admanager.demo

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdView
import com.mfa.admanager.AdManager

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                AdManagerDemoScreen()
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AdManagerDemoPreview() {
    AdManagerDemoScreen()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdManagerDemoScreen() {
    val adManager = AdManager
    val context = LocalContext.current
    val activity = LocalActivity.current
    var isPremium by remember { mutableStateOf(false) }
    var isInitialized by remember { mutableStateOf(false) }

    LaunchedEffect(isPremium) {
        isInitialized = false
        adManager.initialize(
            context = context,
            bannerAdUnitId = "ca-app-pub-your-banner-ad-unit-id",
            interstitialAdUnitId = "ca-app-pub-your-interstitial-ad-unit-id",
            rewardedAdUnitId = "ca-app-pub-your-rewarded-ad-unit-id",
            minDelay = 1,
            isPremium = isPremium
        ) {
            isInitialized = true
        }
    }
    var statusMessage by remember {
        mutableStateOf("Ready to test MFA AdManager")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "MFA AdManager",
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
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            Spacer(modifier = Modifier.height(4.dp))

            PremiumSimulationCard(
                isPremium = isPremium,
                onPremiumChanged = {
                    isPremium = it
                    statusMessage = if (it) {
                        "Premium mode enabled. Ads are disabled."
                    } else {
                        "Premium mode disabled. Ads are enabled."
                    }
                }
            )

            Text(
                text = "Advertisement",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            if (isInitialized) {
                BannerDemo(
                    isPremium = isPremium,
                    adView = adManager.getBannerAdView(
                        context = context,
                        adWidth = rememberAdWidth()
                    )
                )
            }
            Text(
                text = "Ad Tests",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            AdTestButton(
                icon = {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null
                    )
                },
                title = "Interstitial Ad",
                description = "Show a full-screen interstitial advertisement",
                enabled = !isPremium,
                onClick = {
                    statusMessage = "Interstitial test triggered."
                    adManager.showInterstitial(
                        activity = activity?: context as Activity,
                        onSucces = {
                            statusMessage = "Interstitial test completed."
                        },
                        onFailed = {
                            statusMessage = "Interstitial test failed: $it"
                        }
                    )
                }
            )

            AdTestButton(
                icon = {
                    Icon(
                        imageVector = Icons.Default.CardGiftcard,
                        contentDescription = null
                    )
                },
                title = "Rewarded Ad",
                description = "Show a rewarded advertisement",
                enabled = !isPremium,
                onClick = {
                    adManager.showRewarded(
                        activity = activity?: context as Activity,
                        onDismiss = {
                            statusMessage = "Rewarded ad test completed."
                            statusMessage += "\nReward item: ${it.type} - ${it.amount}"
                        },
                        onFailed = {
                            statusMessage = "Rewarded ad test failed."
                        }
                    )
                    statusMessage = "Rewarded ad test triggered."
                }
            )

            StatusCard(
                message = statusMessage,
                isPremium = isPremium
            )
        }
    }
}

@Composable
private fun PremiumSimulationCard(
    isPremium: Boolean,
    onPremiumChanged: (Boolean) -> Unit
) {

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
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
                    modifier = Modifier.size(28.dp)
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp)
                ) {

                    Text(
                        text = "Premium Simulation",
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = if (isPremium) {
                            "Premium user"
                        } else {
                            "Free user"
                        },
                        fontSize = 13.sp,
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
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top
            ) {

                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = "When Premium mode is enabled, advertisements are automatically disabled.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun BannerDemo(
    isPremium: Boolean,
    adView: AdView?
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(
                MaterialTheme.colorScheme.surfaceVariant
            ),
        contentAlignment = Alignment.Center
    ) {

        if (isPremium) {

            Column(
                modifier = Modifier.height(80.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {

                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = "Banner disabled for Premium",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }

        } else {
             adView?.let { ad ->
                 AndroidView(
                     factory = { ad }
                 )
             } ?: run {
                Column(
                    modifier = Modifier.height(80.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "BANNER AD",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "AdMob Banner placement",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
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
        shape = RoundedCornerShape(14.dp)
    ) {

        icon()

        Spacer(modifier = Modifier.size(12.dp))

        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.Start
        ) {

            Text(
                text = title,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = if (enabled) {
                    description
                } else {
                    "Disabled because Premium mode is active"
                },
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StatusCard(
    message: String,
    isPremium: Boolean
) {

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp)
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Icon(
                imageVector = if (isPremium) {
                    Icons.Default.Star
                } else {
                    Icons.Default.CheckCircle
                },
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Text(
                modifier = Modifier.padding(start = 10.dp),
                text = message,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
fun rememberAdWidth(): Int {
    val density = LocalDensity.current
    val containerSize = LocalWindowInfo.current.containerSize

    return with(density) {
        (containerSize.width.toDp().value.toInt() - 16)
    }
}