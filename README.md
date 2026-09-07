# Driftly: Baby Sleep Sounds

Driftly is a native Android app that plays white noise, nature sounds, womb/heartbeat sounds, and
lullabies to help babies (and anyone else) fall asleep and stay asleep. Sounds can be layered into
custom mixes, saved for one-tap replay, and played on a sleep timer that keeps working in the
background via a foreground media playback service.

## Features

- **Sound library** across four categories: White Noise, Nature, Womb & Heartbeat, and Lullabies,
  each generated on-device by a DSP audio engine (no streaming, no bundled audio files).
- **Mixer** — layer multiple sounds together, control each one's volume independently, and stop
  everything with one tap.
- **Saved mixes** — save a layered mix by name and reapply it later in one tap (Premium).
- **Sleep timer** — auto-stop playback after 15 minutes up to a full 8-hour night (longer timer
  options are Premium).
- **Favorites** — star sounds for quick access.
- **Background playback** — a foreground service with a media-style notification keeps sounds
  playing when the app isn't in the foreground.
- **Light/dark/system theming** with Material 3 dynamic color (Android 12+ wallpaper-based
  theming), plus static fallback palettes on older versions.
- **Premium subscription** (weekly/monthly/yearly/lifetime) and a one-time Remove Ads purchase via
  Google Play Billing, with local purchase verification and restore support.

## Requirements

- Android 7.0 (API 24) or higher (`minSdk 24`, `targetSdk`/`compileSdk 34`)
- Android Studio (Ladybug or newer recommended) with the Android SDK 34 platform installed
- JDK 17
- Gradle 8.7 / Android Gradle Plugin 8.5.2 (via the Gradle wrapper)

## Build instructions

1. Open the project in Android Studio and let it sync, **or** build from the command line:
   ```
   ./gradlew assembleDebug
   ```
2. Run `./gradlew installDebug` (or use Android Studio's Run button) to install on a connected
   device or emulator running API 24+.

> **Note:** this project's folder name contains a literal colon (`Driftly: Baby Sleep Sounds`),
> which breaks the Unix classpath separator used by the `gradlew` script on macOS/Linux. If
> `./gradlew` fails with `ClassNotFoundException: org.gradle.wrapper.GradleWrapperMain` or
> `Unresolved reference: R`, either build from Android Studio directly, or copy/checkout the
> project into a path without a colon before invoking Gradle from the command line.

Release builds additionally require:
- A real RSA licensing key in `billing/PurchaseVerifier.kt` (from Play Console > Monetization
  setup > Licensing) for purchase signature verification.
- Published Terms of Service and Privacy Policy URLs in `ui/paywall/PaywallScreen.kt`.
- Billing product IDs in `billing/BillingProductIds.kt` configured to match Play Console.

## Project structure

```
app/src/main/java/com/factory/driftlybabysleepsounds/
├── audio/            DSP sound generators (white/pink/brown noise, rain, ocean, wind, heartbeat,
│                     lullabies) and the AudioEngine mixer that drives them
├── billing/          Google Play Billing integration, product IDs, and purchase verification
├── data/
│   ├── local/        Room database (favorites, saved mixes) and DataStore-backed preferences
│   ├── model/        Sound catalog and domain enums (category, generator type, sleep timer)
│   └── repository/   SoundRepository — single source of truth for sounds/favorites/mixes
├── premium/          Premium entitlement state and paywall trigger/display coordination
├── service/          Foreground PlaybackService + media-style notification
├── ui/
│   ├── home/         Sound library grid (Sounds tab)
│   ├── player/        Mixer, saved mixes, and sleep timer (Mixer tab)
│   ├── settings/      Theme, premium status, restore purchases, about (Settings tab)
│   ├── paywall/       Premium upgrade / remove-ads screen
│   ├── navigation/    Bottom navigation destinations
│   └── theme/         Material 3 color scheme, dynamic color, and typography
├── DriftlyApplication.kt  App-wide singletons (database, repository, premium/billing managers)
└── MainActivity.kt        Splash screen, theme selection, notification permission request
```
