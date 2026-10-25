# ClassBell

ClassBell is a native Android class-reminder app. Add weekly class times and choose which days they repeat; each reminder rings 20 minutes before class. The app also lets you choose an alarm ringtone and an image used as the wallpaper on both the app and ringing screen.

## SHM and REV media-sharing demo

This project also builds two separate Android demo apps:

- **SHM** lets a person choose specific photos or videos with Android’s media picker and preview an explicit, local-only demo share.
- **REV** previews a consent request and inbox card for administrators.

The two apps are not connected to a backend: they do not send requests or notifications, report online status, upload/download files, or give REV access to a device gallery. No broad gallery permission is requested. A real multi-device sharing service would need a separately configured backend, authentication, secure storage, and explicit user approval for each share.

## Build and install without Android Studio

1. Open this project’s repository on GitHub and select **Actions**, then **Build Android APK**. Each push starts a build; you can also choose **Run workflow**.
2. When the run finishes successfully, open it and download the `media-share-apks` artifact.
3. Extract the downloaded ZIP. Install `shm-debug.apk` for SHM and `rev-debug.apk` for REV on your Android phone. If Android asks, allow installing apps from that source.

These are unsigned debug APKs for personal testing, not Play Store releases. The existing ClassBell app remains in the project but is not included in the media-sharing APK artifact.

## Run with Android Studio

Open this folder in Android Studio with JDK 17 and Android SDK 35 installed, sync the Gradle project, then run the `app` configuration on an Android device or emulator. On Android 12 and newer, allow the app to schedule exact alarms when prompted. Allow notifications so the alarm alert can be shown.
