# MFA AdManager

A simple Android library for managing Google AdMob ads with a centralized `AdManager`.

MFA AdManager provides a simple API for Banner, Interstitial, and Rewarded Ads while handling test ad unit IDs automatically in debug builds.

## Features

* Banner Ads
* Adaptive Banner Ads
* Interstitial Ads
* Rewarded Ads
* Automatic Google test ad unit IDs in debug builds
* Custom ad unit IDs in release builds
* Minimum delay between Interstitial Ads
* Premium mode to disable ads
* Centralized AdMob configuration
* Automatic Interstitial preloading

## Requirements

* Android API 24+
* Kotlin
* Google Mobile Ads SDK

## Installation

**Coming soon.**

The library will be available through a Maven repository.

## Basic Usage

Initialize `AdManager` before displaying ads:

```kotlin
AdManager.initialize(
    context = this,
    bannerAdUnitId = "YOUR_BANNER_AD_UNIT_ID",
    interstitialAdUnitId = "YOUR_INTERSTITIAL_AD_UNIT_ID",
    rewardedAdUnitId = "YOUR_REWARDED_AD_UNIT_ID"
)
```

You can also specify the minimum delay between Interstitial Ads:

```kotlin
AdManager.initialize(
    context = this,
    bannerAdUnitId = "YOUR_BANNER_AD_UNIT_ID",
    interstitialAdUnitId = "YOUR_INTERSTITIAL_AD_UNIT_ID",
    rewardedAdUnitId = "YOUR_REWARDED_AD_UNIT_ID",
    minDelay = 5
)
```

`minDelay` is specified in minutes and defaults to `5`.

### Initialization Callback

If you need to know when AdMob initialization has completed, use `onInitialized`:

```kotlin
AdManager.initialize(
    context = this,
    bannerAdUnitId = "YOUR_BANNER_AD_UNIT_ID",
    interstitialAdUnitId = "YOUR_INTERSTITIAL_AD_UNIT_ID",
    rewardedAdUnitId = "YOUR_REWARDED_AD_UNIT_ID"
) {
    // AdManager is initialized
}
```

## Debug and Release Ad Unit IDs

MFA AdManager automatically detects whether the application is running as a debug build.

### Debug builds

Google test ad unit IDs are used automatically.

You do not need to provide test ad unit IDs yourself.

### Release builds

The ad unit IDs supplied to `initialize()` are used:

```kotlin
AdManager.initialize(
    context = this,
    bannerAdUnitId = "ca-app-pub-XXXXXXXXXXXXXXXX/XXXXXXXXXX",
    interstitialAdUnitId = "ca-app-pub-XXXXXXXXXXXXXXXX/XXXXXXXXXX",
    rewardedAdUnitId = "ca-app-pub-XXXXXXXXXXXXXXXX/XXXXXXXXXX"
)
```

This helps prevent accidentally using production ad unit IDs during development.

## Banner Ads

Create a Banner `AdView` using:

```kotlin
val bannerAdView = AdManager.getBannerAdView(
    context = this
)
```

The returned value is nullable because Banner Ads are automatically disabled when Premium Mode is enabled.

### Adaptive Banner

You can provide the available ad width to create an adaptive banner:

```kotlin
val bannerAdView = AdManager.getBannerAdView(
    context = this,
    adWidth = 320
)
```

If `adWidth` is `0`, the standard `AdSize.BANNER` size is used.

## Interstitial Ads

Show an Interstitial Ad using:

```kotlin
AdManager.showInterstitial(
    activity = this,
    onSucces = {
        // Continue after the ad is dismissed
    },
    onFailed = { message ->
        // Ad could not be shown
    }
)
```

MFA AdManager automatically preloads an Interstitial Ad after initialization.

After an Interstitial Ad is dismissed, the next Interstitial Ad is automatically preloaded.

### Minimum Delay

By default, Interstitial Ads have a minimum delay of 5 minutes.

You can change it during initialization:

```kotlin
AdManager.initialize(
    context = this,
    bannerAdUnitId = "YOUR_BANNER_AD_UNIT_ID",
    interstitialAdUnitId = "YOUR_INTERSTITIAL_AD_UNIT_ID",
    rewardedAdUnitId = "YOUR_REWARDED_AD_UNIT_ID",
    minDelay = 1
)
```

The value is specified in minutes.

If an Interstitial Ad is requested before the minimum delay has passed, `onFailed` is called with a message describing the remaining delay.

## Rewarded Ads

Show a Rewarded Ad using:

```kotlin
AdManager.showRewarded(
    activity = this,
    onDismiss = { rewardItem ->
        // Grant the reward to the user
    },
    onFailed = {
        // Rewarded Ad could not be shown
    }
)
```

The `RewardItem` provided through `onDismiss` contains the reward information returned by Google Mobile Ads.

Example:

```kotlin
AdManager.showRewarded(
    activity = this,
    onDismiss = { rewardItem ->
        val amount = rewardItem.amount
        val type = rewardItem.type

        // Grant the reward
    }
)
```

## Premium Mode

Premium Mode disables advertising.

Set `isPremium = true` during initialization:

```kotlin
AdManager.initialize(
    context = this,
    bannerAdUnitId = "YOUR_BANNER_AD_UNIT_ID",
    interstitialAdUnitId = "YOUR_INTERSTITIAL_AD_UNIT_ID",
    rewardedAdUnitId = "YOUR_REWARDED_AD_UNIT_ID",
    isPremium = true
)
```

When Premium Mode is enabled:

* `getBannerAdView()` returns `null`
* Interstitial Ads are not shown
* Rewarded Ads are not shown

This allows the same application code to support both free and premium users.

## License

License information will be added in a future release.
