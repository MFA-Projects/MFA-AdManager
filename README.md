# MFA-AdManager

[![JitPack](https://jitpack.io/v/MFA-Projects/MFA-AdManager.svg)](https://jitpack.io/#MFA-Projects/MFA-AdManager)

**A lightweight Android advertising library that simplifies ad network integration with Kotlin.**

MFA-AdManager is a modular Android library designed to simplify advertising integration in native Android applications.

It currently provides separate modules for Google AdMob and Unity Ads, with straightforward APIs for initialization, ad loading, ad display, cooldown management, and premium mode.

## Features

- **Modular Architecture:** Integrate AdMob and Unity Ads independently.
- **AdMob Integration:** Support for banner, interstitial, and rewarded ads.
- **Unity Ads Integration:** Support for interstitial ads.
- **AdMob Mediation Compatibility:** Use supported third-party mediation adapters alongside the AdMob SDK.
- **Universal Cooldown:** Configure time-based cooldown policies for individual ad placements.
- **Shared Cooldown:** Allow multiple ad formats to share the same cooldown key.
- **Optional Cooldown:** Disable cooldown restrictions when they are not needed.
- **Premium Mode:** Disable advertising through a single setting.
- **Automatic Debug Test Ads:** Use official test ad unit IDs in debuggable builds for AdMob.
- **Lightweight API:** Built for native Android development with Kotlin.

## Modules

MFA-AdManager currently provides two independent modules.

| Module | Package | Supported Formats |
|---|---|---|
| Admob | `com.mfa.admanager.admob` | Banner, Interstitial, Rewarded |
| Unity | `com.mfa.admanager.unity` | Interstitial |

Each module can be integrated independently. You only need to include the modules required by your application.

## Requirements

- Android SDK 24 or higher.
- Kotlin.
- Android Gradle Plugin compatible with the library.
- Internet access for loading advertisements.

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

If your project already has a `dependencyResolutionManagement` block, add only the JitPack repository entry.

### 2. Add the required modules

In your app module's `build.gradle.kts`, add the dependencies you need.

**AdMob only:**

```kotlin
implementation(
    "com.github.MFA-Projects.MFA-AdManager:Admob:1.0.3"
)
```

**Unity Ads only:**

```kotlin
implementation(
    "com.github.MFA-Projects.MFA-AdManager:Unity:1.0.3"
)
```

**Both modules:**

```kotlin
implementation(
    "com.github.MFA-Projects.MFA-AdManager:Admob:1.0.3"
)

implementation(
    "com.github.MFA-Projects.MFA-AdManager:Unity:1.0.3"
)
```

Replace `1.0.3` with the version you intend to use.

Check the [JitPack build page](https://jitpack.io/#MFA-Projects/MFA-AdManager) to verify that the selected version has been built successfully.

---

# AdMob Integration

The AdMob module provides banner, interstitial, and rewarded ad support.

Package:

```kotlin
import com.mfa.admanager.admob.AdMob
```

## 1. Initialize AdMob

Initialize the library once, preferably from your application's startup flow.

```kotlin
class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        AdMob.initialize(
            context = this,
            bannerAdUnitId = "YOUR_BANNER_AD_UNIT_ID",
            interstitialAdUnitId = "YOUR_INTERSTITIAL_AD_UNIT_ID",
            rewardedAdUnitId = "YOUR_REWARDED_AD_UNIT_ID",
            minDelay = 5,
            isPremium = false
        ) {
            // AdMob initialization completed.
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

Replace the placeholder ad unit IDs with your own AdMob ad unit IDs.

**Important:** The `minDelay` parameter is measured in minutes and configures the default interstitial cooldown. Its default value is `0`, which disables that cooldown.

## 2. Display a banner ad

```kotlin
val bannerAdView = AdMob.getBannerAdView(
    context = this
)

bannerAdView?.let { adView ->
    binding.adContainer.addView(adView)
}
```

To request an inline adaptive banner using a width in pixels:

```kotlin
val bannerAdView = AdMob.getBannerAdView(
    context = this,
    adWidth = availableWidthPx
)
```

The function returns `null` when the manager is not initialized or premium mode is enabled.

Remember to manage the banner view's lifecycle and call `destroy()` when it is no longer needed.

## 3. Display an interstitial ad

```kotlin
AdMob.showInterstitial(
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

## 4. Display a rewarded ad

```kotlin
AdMob.showRewarded(
    activity = this,
    onDismiss = { reward ->
        // The user earned the reward.
        val amount = reward.amount
        val rewardType = reward.type

        // Grant the appropriate in-app reward.
    },
    onFailed = {
        // The ad could not be loaded or displayed.
    },
    onAdDismissed = {
        // The full-screen ad was closed.
    }
)
```

**Reward handling:**

The `onDismiss` callback is retained for source compatibility, but it is called when Google Mobile Ads confirms that the user earned a reward. It does not mean the ad was dismissed.

The `onAdDismissed` callback handles ad closure independently.

Do not grant rewards from `onAdDismissed`. Only grant them through the earned-reward callback.

Rewarded ads are loaded when `showRewarded()` is called in the current implementation, so loading latency may occur before the ad appears.

---

# Unity Ads Integration

The Unity module provides direct Unity Ads integration for interstitial advertisements.

Package:

```kotlin
import com.mfa.admanager.unity.Unity
```

## 1. Initialize Unity Ads

```kotlin
Unity.initialize(
    context = this,
    gameId = "YOUR_UNITY_GAME_ID",
    isPremium = false,
    onInitialized = {
        // Unity Ads initialization completed.
    },
    onFailed = { error ->
        // Handle initialization failure.
    }
)
```

Replace `YOUR_UNITY_GAME_ID` with your Android Game ID from the Unity Dashboard.

The library automatically determines whether to enable Unity Ads test mode based on the application's debuggable build configuration.

## 2. Load an interstitial ad

```kotlin
Unity.loadInterstitial(
    placementId = "YOUR_INTERSTITIAL_PLACEMENT_ID",
    onLoaded = {
        // Interstitial is ready.
    },
    onFailed = { error ->
        // Handle loading failure.
    }
)
```

The placement ID must match the placement configured in your Unity Dashboard.

## 3. Display an interstitial ad

```kotlin
Unity.showInterstitial(
    activity = this,
    placementId = "YOUR_INTERSTITIAL_PLACEMENT_ID",
    onSuccess = {
        // Ad completed successfully.
    },
    onFailed = { error ->
        // Handle display failure.
    }
)
```

The library automatically requests another interstitial after the current ad finishes or fails to display.

An interstitial must be loaded before it can be displayed.

---

# AdMob Mediation

MFA-AdManager's AdMob module can be used with Google AdMob Mediation and supported third-party mediation adapters.

AdMob Mediation allows multiple advertising networks to compete for ad requests through bidding or other supported mediation configurations.

For example, Unity Ads can participate in AdMob Mediation through the official Unity Ads mediation adapter.

## 1. Configure the mediation dashboard

Before using mediation:

1. Create or configure your application in the Unity Dashboard.
2. Configure the supported ad placements.
3. Add Unity Ads as a bidding source in your AdMob Mediation group.
4. Configure the required application and placement mapping.
5. Save and activate the mediation configuration.

Refer to the official documentation:

- [Google AdMob Mediation](https://developers.google.com/admob/android/mediation)
- [Unity Ads Mediation Integration](https://developers.google.com/admob/android/mediation/unity)

## 2. Add the required dependencies

The application must include the Google Mobile Ads SDK and the mediation adapter.

Example dependency configuration:

```kotlin
dependencies {
    implementation("com.google.android.gms:play-services-ads:25.5.0")

    implementation("com.unity3d.ads:unity-ads:4.21.0")

    implementation("com.google.ads.mediation:unity:4.21.0.0")
}
```

**Note:** The versions above are an example configuration. Always check the official compatibility requirements before selecting SDK and adapter versions.

If you already use the MFA-AdManager Admob module, do not add a duplicate Google Mobile Ads SDK dependency unless your dependency configuration specifically requires it.

## 3. Request ads through AdMob

When mediation is configured correctly, request advertisements through the AdMob module as usual.

```kotlin
AdMob.initialize(
    context = this,
    bannerAdUnitId = "YOUR_BANNER_AD_UNIT_ID",
    interstitialAdUnitId = "YOUR_INTERSTITIAL_AD_UNIT_ID",
    rewardedAdUnitId = "YOUR_REWARDED_AD_UNIT_ID"
) {
    // AdMob initialization completed.
}
```

The mediation SDK and configured adapters handle communication with participating advertising networks.

You do not need to manually initialize Unity Ads or call its direct ad-loading methods for advertisements handled through AdMob Mediation.

**Important:**

- Mediation adapters must be included in the application's dependencies.
- Dashboard configuration alone does not install mediation adapters.
- Do not manually initialize the Unity SDK for the same mediated ad integration.
- Use AdMob Ad Units when requesting mediated advertisements.
- Verify adapter initialization and ad responses using Google's Ad Inspector.

Mediation availability and bidding behavior depend on the selected network, adapter version, ad format, and dashboard configuration.

---

# Universal Cooldown System

MFA-AdManager provides a reusable cooldown mechanism that can be configured independently for different ad placements.

A cooldown determines the minimum time between ad presentations associated with a particular key.

Cooldown management is available in both AdMob and Unity modules.

## 1. Configure an AdMob cooldown

Enable a five-minute cooldown for interstitial ads:

```kotlin
AdMob.setCooldown(
    key = "interstitial",
    cooldown = AdMob.Cooldown.Enabled(
        durationMillis = 5 * 60 * 1000L
    )
)
```

Disable a cooldown:

```kotlin
AdMob.setCooldown(
    key = "rewarded",
    cooldown = AdMob.Cooldown.Disabled
)
```

## 2. Configure a Unity cooldown

Unity uses a duration in milliseconds:

```kotlin
Unity.setCooldown(
    key = "interstitial",
    cooldown = 5 * 60 * 1000L
)
```

Set the duration to `0L` to disable the time-based restriction:

```kotlin
Unity.setCooldown(
    key = "interstitial",
    cooldown = 0L
)
```

## 3. Use independent cooldowns

Different keys maintain independent cooldown histories.

For example:

```kotlin
AdMob.setCooldown(
    key = "interstitial",
    cooldown = AdMob.Cooldown.Enabled(
        durationMillis = 300_000L
    )
)

AdMob.setCooldown(
    key = "rewarded",
    cooldown = AdMob.Cooldown.Disabled
)
```

## 4. Share a cooldown across ad formats

Use the same key when displaying different formats within the same module.

```kotlin
AdMob.showInterstitial(
    activity = this,
    cooldownKey = "global"
)

AdMob.showRewarded(
    activity = this,
    cooldownKey = "global",
    onDismiss = { reward ->
        // Grant the earned reward.
    }
)
```

Configure the shared cooldown:

```kotlin
AdMob.setCooldown(
    key = "global",
    cooldown = AdMob.Cooldown.Enabled(
        durationMillis = 60_000L
    )
)
```

The same principle applies to Unity interstitial placements.

**Note:** Cooldown histories are maintained independently by each module. An AdMob cooldown and a Unity cooldown do not automatically share history, even when their keys have the same name.

## 5. Check the remaining cooldown

```kotlin
val remainingMillis =
    AdMob.getCooldownRemainingMillis("interstitial")

if (remainingMillis > 0L) {
    // Show a countdown or disable the ad button.
}
```

The Unity module provides the same method:

```kotlin
val remainingMillis =
    Unity.getCooldownRemainingMillis("interstitial")
```

## 6. Check whether an ad can be shown

```kotlin
if (AdMob.canShowAd("interstitial")) {
    // The cooldown is not active.
}
```

Unity:

```kotlin
if (Unity.canShowAd("interstitial")) {
    // The cooldown is not active.
}
```

These checks only evaluate cooldown restrictions. They do not guarantee that an advertisement is available or ready.

## 7. Reset cooldown history

AdMob:

```kotlin
AdMob.resetCooldown("interstitial")
```

Unity:

```kotlin
Unity.resetCooldown("interstitial")
```

Use this when the application intentionally needs to clear a recorded cooldown.

### Cooldown behavior

| Configuration | Behavior |
|---|---|
| Disabled / `0L` | No time-based restriction |
| Enabled duration | Restricts ad presentations for the configured duration |
| Different keys | Independent cooldown histories |
| Same key within a module | Shared cooldown history |
| No configured policy | Cooldown disabled by default |

Cooldown history is stored in memory. It is not persisted across application process restarts.

Cooldown management is not a session frequency cap or a maximum number of advertisements per day.

---

# Premium Mode

Premium mode disables advertisements through MFA-AdManager.

Both modules provide a premium mode setting.

## 1. Enable premium mode during initialization

AdMob:

```kotlin
AdMob.initialize(
    context = this,
    bannerAdUnitId = "YOUR_BANNER_AD_UNIT_ID",
    interstitialAdUnitId = "YOUR_INTERSTITIAL_AD_UNIT_ID",
    rewardedAdUnitId = "YOUR_REWARDED_AD_UNIT_ID",
    isPremium = true
)
```

Unity:

```kotlin
Unity.initialize(
    context = this,
    gameId = "YOUR_UNITY_GAME_ID",
    isPremium = true
)
```

## 2. Update premium mode later

Enable premium mode:

```kotlin
AdMob.setPremiumMode(true)
Unity.setPremiumMode(true)
```

Disable premium mode:

```kotlin
AdMob.setPremiumMode(false)
Unity.setPremiumMode(false)
```

When premium mode is enabled:

- AdMob banner requests return `null`.
- AdMob interstitial display requests invoke `onSuccess` without displaying an advertisement.
- AdMob rewarded requests invoke `onFailed`.
- Unity interstitial loading is disabled.
- Unity interstitial display requests invoke `onSuccess` without displaying an advertisement.

The host application is responsible for determining the user's premium entitlement, including validating purchases and restoring entitlements.

MFA-AdManager does not implement billing or purchase verification.

---

# Test Ads and Production Ads

## AdMob

In debuggable builds, MFA-AdManager automatically uses Google's official test ad unit IDs.

In non-debuggable builds, it uses the ad unit IDs supplied during initialization.

Always use test advertisements during development.

## Unity Ads

The Unity module automatically determines test mode based on the application's debuggable build configuration.

Always verify the Unity Dashboard configuration and placement IDs before production release.

Never click your own production advertisements for testing.

---

# Error Handling

Advertisements are not guaranteed to load or display successfully.

An advertisement may fail because of network connectivity, unavailable inventory, invalid configuration, cooldown restrictions, or SDK errors.

Always provide a fallback when an advertisement is unavailable.

Example:

```kotlin
AdMob.showInterstitial(
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

Do not make essential application functionality dependent on an advertisement being available.

---

# Important Notes

- Initialize the appropriate advertising module before requesting advertisements.
- Use an Activity when displaying full-screen advertisements.
- Manage banner view lifecycle and destruction.
- Cooldown checks and ad availability are separate concerns.
- Cooldown history is stored in memory.
- Rewarded advertisements are loaded on demand in the current AdMob implementation.
- Unity currently supports direct interstitial integration through its module.
- AdMob Mediation requires the corresponding mediation adapters.
- Do not manually initialize Unity Ads when using its mediation adapter through AdMob.
- Premium entitlement and in-app reward granting remain the responsibility of the host application.
- Follow the latest advertising policies, privacy requirements, and consent requirements of the participating ad networks.

# Roadmap

Potential future improvements include:

- App Open Ads.
- Jetpack Compose banner integration.
- Native Ads.
- Rewarded Interstitial Ads.
- Automatic rewarded ad preloading.
- More advanced frequency capping.
- Google User Messaging Platform integration.
- Additional advertising networks.
- Improved ad diagnostics and lifecycle management.
- Dedicated mediation integration module.

These are planned improvements, not features currently included in the library.

# Contributing

Contributions, bug reports, and suggestions are welcome.

Please include clear reproduction steps when reporting a bug.

# License

Check the repository for the applicable license before using or redistributing this library.
