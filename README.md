# MFA-AdManager

[![](https://www.jitpack.io/v/MFA-Projects/MFA-AdManager.svg)](https://www.jitpack.io/#MFA-Projects/MFA-AdManager)

**A lightweight Android library that simplifies Google Mobile Ads integration with Kotlin.**

MFA-AdManager provides a simple API for displaying banner, interstitial, and rewarded ads in native Android applications. It also provides a reusable cooldown system that can be configured independently for different ad placements or shared across multiple ad formats.

## Features

- **Simple Integration:** Initialize Google Mobile Ads with a single function.
- **Banner Ads:** Display standard banners or inline adaptive banners.
- **Interstitial Ads:** Preload and display full-screen interstitial ads.
- **Rewarded Ads:** Handle earned rewards separately from ad dismissal.
- **Universal Cooldown:** Configure time-based cooldown policies for different ad placements.
- **Shared Cooldown:** Allow multiple ad formats to share the same cooldown key.
- **Optional Cooldown:** Disable time-based cooldown when it is not needed.
- **Premium Mode:** Disable ads through a single premium setting.
- **Automatic Debug Test Ads:** Use Google's test ad unit IDs in debuggable builds.
- **Lightweight API:** Built for native Android development with Kotlin.

## Requirements

- Android SDK 24 or higher.
- Kotlin.
- Google Mobile Ads SDK compatibility.
- Internet access for loading ads.

## Installation

### 1. Add JitPack

In your project's `settings.gradle.kts`, add JitPack to `dependencyResolutionManagement`:

```kotlin
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)

    repositories {
        google()
        mavenCentral()
        maven {
            url = uri("https://jitpack.io")
        }
    }
}
```

If your project already has a `dependencyResolutionManagement` block, add only the JitPack repository entry. Do not create a duplicate block.

### 2. Add the dependency

In your app module's `build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.github.MFA-Projects:MFA-AdManager:1.0.1")
}
```

Check the [JitPack build page](https://www.jitpack.io/#MFA-Projects/MFA-AdManager) to confirm that the version you want to use has built successfully.

## Quick Start

### 1. Initialize MFA-AdManager

Initialize the library once, preferably from your application's startup flow.

```kotlin
import android.app.Application
import com.mfa.admanager.AdManager

class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        AdManager.initialize(
            context = this,
            bannerAdUnitId = "YOUR_BANNER_AD_UNIT_ID",
            interstitialAdUnitId = "YOUR_INTERSTITIAL_AD_UNIT_ID",
            rewardedAdUnitId = "YOUR_REWARDED_AD_UNIT_ID",
            minDelay = 5,
            isPremium = false
        ) {
            // AdManager initialization completed.
        }
    }
}
```

Register your application class in `AndroidManifest.xml`:

```xml
<application
    android:name=".MyApplication"
    ... >
</application>
```

Replace the placeholder ad unit IDs with your own AdMob ad unit IDs before releasing your application.

**Important:** The `minDelay` parameter is measured in minutes and configures the default interstitial cooldown. Its default value is `0`, which disables that cooldown.

### 2. Display a banner ad

```kotlin
val bannerAdView = AdManager.getBannerAdView(
    context = this
)

bannerAdView?.let { adView ->
    binding.adContainer.addView(adView)
}
```

To request an inline adaptive banner using a width in pixels:

```kotlin
val bannerAdView = AdManager.getBannerAdView(
    context = this,
    adWidth = availableWidthPx
)
```

The function returns `null` when the manager is not initialized or premium mode is enabled.

Remember to manage the banner view's lifecycle and call `destroy()` when it is no longer needed.

### 3. Display an interstitial ad

```kotlin
AdManager.showInterstitial(
    activity = this,
    onSuccess = {
        // Continue the application flow.
    },
    onFailed = { error ->
        // Handle cooldown, loading, or display errors.
    }
)
```

The `onSuccess` callback runs when the ad is dismissed, or immediately when premium mode is enabled.

An interstitial must be loaded before it can be displayed. If no ad is ready, the function reports the failure and requests another ad.

### 4. Display a rewarded ad

```kotlin
AdManager.showRewarded(
    activity = this,
    onDismiss = { reward ->
        // The user earned the reward.
        val amount = reward.amount
        val rewardType = reward.type

        // Grant the appropriate in-app reward.
    },
    onAdDismissed = {
        // The full-screen ad was closed.
    },
    onFailed = {
        // The ad could not be loaded or displayed.
    }
)
```

**Reward handling:** In the current API, `onDismiss` is retained for source compatibility, but it is called when Google Mobile Ads confirms that the user earned a reward. It does not mean the ad was dismissed.

`onAdDismissed` handles ad closure independently.

Do not grant rewards from `onAdDismissed`. Only grant them through the earned-reward callback, and validate any valuable server-side entitlement according to your application's security requirements.

Rewarded ads are loaded when `showRewarded()` is called in the current implementation, so loading latency may occur before the ad appears.

## Universal Cooldown System

MFA-AdManager provides a reusable cooldown mechanism that can be applied to different ad placements.

A cooldown determines the minimum time between ad presentations associated with a particular key. It is separate from ad loading and readiness.

### 1. Enable a cooldown

For example, configure a five-minute cooldown for interstitial ads:

```kotlin
AdManager.setCooldown(
    key = "interstitial",
    cooldown = AdManager.Cooldown.Enabled(
        durationMillis = 5 * 60 * 1000L
    )
)
```

The duration is expressed in milliseconds.

### 2. Disable a cooldown

```kotlin
AdManager.setCooldown(
    key = "rewarded",
    cooldown = AdManager.Cooldown.Disabled
)
```

Disabling the cooldown removes the time-based restriction for that key. It does not guarantee that an ad is available or that Google will fill an ad request.

### 3. Use independent cooldowns

```kotlin
AdManager.setCooldown(
    key = "interstitial",
    cooldown = AdManager.Cooldown.Enabled(
        durationMillis = 5 * 60 * 1000L
    )
)

AdManager.setCooldown(
    key = "rewarded",
    cooldown = AdManager.Cooldown.Disabled
)
```

Each key maintains its own cooldown history.

### 4. Share a cooldown across ad formats

Use the same key when displaying different ad formats:

```kotlin
AdManager.showInterstitial(
    activity = this,
    cooldownKey = "global"
)

AdManager.showRewarded(
    activity = this,
    cooldownKey = "global",
    onDismiss = { reward ->
        // Grant the earned reward.
    }
)
```

Configure the shared policy:

```kotlin
AdManager.setCooldown(
    key = "global",
    cooldown = AdManager.Cooldown.Enabled(
        durationMillis = 60_000L
    )
)
```

Both formats use the same cooldown history when the cooldown is enabled.

A shared cooldown is useful when your application wants to avoid displaying different full-screen ads too close together. Use separate keys when each placement needs independent rules.

### 5. Check the remaining cooldown

```kotlin
val remainingMillis =
    AdManager.getCooldownRemainingMillis("interstitial")

if (remainingMillis > 0L) {
    // Show a countdown or disable the ad button.
} else {
    // The placement has no active cooldown.
}
```

This function returns the remaining time in milliseconds. It does not create a timer or update your UI automatically.

### 6. Check whether an ad can be shown

```kotlin
if (AdManager.canShowAd("interstitial")) {
    // The cooldown is not active.
}
```

This checks the cooldown only. It does not guarantee that an ad has loaded successfully.

### 7. Reset cooldown history

```kotlin
AdManager.resetCooldown("interstitial")
```

Use this only when the application intentionally needs to clear the recorded cooldown for that key.

### Cooldown behavior

| Configuration | Behavior |
|---|---|
| `Cooldown.Disabled` | No time-based restriction |
| `Cooldown.Enabled(300_000L)` | Five-minute cooldown |
| Different keys | Independent cooldown histories |
| Same key | Shared cooldown history |
| No configured policy | Cooldown disabled by default |

Cooldown history is stored in memory. It is not persisted across application process restarts, and it is not a session frequency cap or a maximum number of ads per day.

## Premium Mode

Premium mode disables ad display through MFA-AdManager.

### Enable premium mode during initialization

```kotlin
AdManager.initialize(
    context = this,
    bannerAdUnitId = "YOUR_BANNER_AD_UNIT_ID",
    interstitialAdUnitId = "YOUR_INTERSTITIAL_AD_UNIT_ID",
    rewardedAdUnitId = "YOUR_REWARDED_AD_UNIT_ID",
    isPremium = true
)
```

### Update premium mode later

```kotlin
AdManager.setPremiumMode(true)
```

Disable premium mode:

```kotlin
AdManager.setPremiumMode(false)
```

When premium mode is enabled:

- Banner requests through the manager return `null`.
- Interstitial display requests invoke `onSuccess` without showing an ad.
- Rewarded requests invoke `onFailed`.

The host application is responsible for determining the user's premium entitlement, including validating purchases and restoring entitlements. MFA-AdManager does not implement billing or purchase verification.

## Test Ads and Production Ads

In debuggable builds, MFA-AdManager uses Google's official test ad unit IDs.

In non-debuggable builds, it uses the ad unit IDs supplied during initialization.

Always use test ads during development and testing. Configure valid production ad unit IDs before publishing your application.

Never click your own production ads for testing.

## Error Handling

Handle failures gracefully. An ad may fail to load, may not be ready when requested, or may fail to display.

Example:

```kotlin
AdManager.showInterstitial(
    activity = this,
    onSuccess = {
        navigateToNextScreen()
    },
    onFailed = { error ->
        Log.e("MyApp", "Ad unavailable: $error")
        navigateToNextScreen()
    }
)
```

Do not make essential application functionality dependent on an ad being available. Provide an appropriate fallback when loading or displaying an ad fails.

## Important Notes

- Initialize MFA-AdManager before requesting ads.
- Use an Activity when displaying full-screen ads.
- Manage banner view lifecycle and destruction.
- Cooldown checks and ad availability are separate concerns.
- Cooldown configuration and history are held in memory.
- Rewarded ads are loaded on demand in the current implementation.
- Premium entitlement and in-app reward granting remain the responsibility of the host application.
- Follow Google's current AdMob policies and consent requirements.

## Roadmap

Potential future improvements include:

- App Open Ads.
- Jetpack Compose banner integration.
- Native Ads and Rewarded Interstitials.
- Automatic rewarded ad preloading.
- More advanced frequency capping.
- Google User Messaging Platform integration.
- Improved ad diagnostics and lifecycle management.

These are planned improvements, not features currently included in the library.

## Contributing

Contributions, bug reports, and suggestions are welcome. Please include clear reproduction steps when reporting a bug.

## License

Check the repository for the applicable license before using or redistributing this library.
