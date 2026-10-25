# ClassBell

ClassBell is a native Android class-reminder app. Add weekly class times and choose which days they repeat; each reminder rings 20 minutes before class. The app also lets you choose an alarm ringtone and an image used as the wallpaper on both the app and ringing screen.

## Build and install without Android Studio

1. Create a GitHub repository and upload this project to it.
2. Open the repository on GitHub, select **Actions**, then **Build Android APK**. If it has not run yet, choose **Run workflow**.
3. When the run finishes successfully, open it and download the `classbell-debug-apk` artifact.
4. Extract the downloaded ZIP, transfer `app-debug.apk` to your Android phone, and open it to install. If Android asks, allow installing apps from that source.
5. Open ClassBell and allow notifications and exact alarms so reminders can ring on time.

This creates an unsigned debug APK for personal testing, not a Play Store release.

## Run with Android Studio

Open this folder in Android Studio with JDK 17 and Android SDK 35 installed, sync the Gradle project, then run the `app` configuration on an Android device or emulator. On Android 12 and newer, allow the app to schedule exact alarms when prompted. Allow notifications so the alarm alert can be shown.
