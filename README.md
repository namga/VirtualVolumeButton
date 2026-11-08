# Virtual Volume — Fire HD 10 (2021)

Version: 1.0.0 (initial source build)

An Android app source project for a movable floating media-volume control. Designed for Amazon Fire HD 10 (2021 / 11th generation) and compatible Android devices.

## Features in v1.0.0
- In-app media volume slider and mute/restore.
- Movable floating `VOL` bubble displayed above other apps.
- Floating panel with media volume slider, percentage, mute, and close.
- Versioned GitHub Actions debug APK artifact.

## Limitations
- Uses the media-volume steps exposed by Fire OS; it does not create extra hardware volume levels.
- Does not promise sound amplification beyond the tablet's system/hardware limit.
- Overlay permission must be granted manually in Fire OS settings.
- This initial version is a debug APK, not a Play Store release-signed APK. Fire OS may restrict the service in the background; restart it from the app if needed.

## Build APK on GitHub
1. Create a GitHub repository and upload these source files.
2. Open **Actions** and run **Build Virtual Volume APK**, or push a commit to `main`/`master`.
3. Download the `VirtualVolume-v1.0.0-fire-hd10` artifact from the completed workflow run.
4. Transfer the APK to the Fire tablet and allow installation from the source you used, if prompted.

The workflow builds with JDK 17, Gradle 8.9, Android SDK 35. The app's target SDK is 28 to retain compatibility with older Fire OS overlay behavior.
