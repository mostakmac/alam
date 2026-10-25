# MediaShare and ClassBell

ClassBell is a native Android class-reminder app. Add weekly class times and choose which days they repeat; each reminder rings 20 minutes before class. The app also lets you choose an alarm ringtone and an image used as the wallpaper on both the app and ringing screen.

## SHM and REV cross-device sharing

This project also builds two separate Android companion apps:

- **SHM** lets a signed-in user choose specific photos or videos with Android’s photo picker, review the recipient, confirm the upload, and revoke their server copy later.
- **REV** signs in approved administrators and displays approved user shares as they arrive. An admin can download and open a file.

SHM and REV can exchange approved shares across different phones after both apps are built with the same Firebase project configuration. The apps do not monitor online status, request broad gallery access, or silently access a user’s media. The user must select and confirm each upload. REV access is limited to Firebase Auth accounts explicitly added to the admin allowlist. Revoking a share deletes the cloud copy, but cannot remove a copy an admin has already downloaded.

### Configure Firebase

1. Create a Firebase project and enable **Authentication → Email/Password**, **Cloud Firestore**, and **Cloud Storage**. Set up a storage bucket. Firebase may ask you to enable billing for Storage.
2. Register two Android apps in the Firebase project. Use package name `com.mediashare.shm` for SHM and `com.mediashare.rev` for REV.
3. In **Project settings → General**, copy the Web API key, Project ID, Storage bucket, and Cloud Messaging sender ID. Also copy each registered Android app’s Firebase App ID.
4. In the GitHub repository, open **Settings → Secrets and variables → Actions → Variables** and create these repository variables:
   - `FIREBASE_API_KEY`
   - `FIREBASE_PROJECT_ID`
   - `FIREBASE_STORAGE_BUCKET`
   - `FIREBASE_SENDER_ID`
   - `FIREBASE_APP_ID_SHM`
   - `FIREBASE_APP_ID_REV`

   These are Firebase client-configuration values, not service-account credentials. Never put a service-account key or Storage admin key in the app, Git repository, or Actions variables.
5. In the Firebase Console, open **Firestore → Rules**, replace the rules with [`firestore.rules`](./firestore.rules), and publish. Open **Storage → Rules**, replace the rules with [`storage.rules`](./storage.rules), and publish. The rules limit uploads to signed-in users and approved image/video files; only enabled admins can read approved shares.
6. Create each REV administrator in **Authentication → Users → Add user**. Copy that account’s UID. In **Firestore → Data**, create a collection named `admins`, then a document whose ID is the administrator’s UID, with the Boolean field `enabled` set to `true`. Repeat only for people you authorize. The app does not allow self-registration as an admin.
7. Push a commit or run **Actions → Build Android APK → Run workflow**. Install the new `shm-debug.apk` and `rev-debug.apk` on the separate phones. SHM users create their accounts in SHM; admins sign in with their manually created, allowlisted REV accounts.

The Firebase API configuration is embedded in each APK, as is normal for Android client apps. Access control is enforced by the published Firebase rules and admin allowlist—not by hiding client config. Review Firebase usage and storage costs, and revoke admin allowlist entries when access should end.

If those GitHub variables are not set yet, the workflow can still compile-check both apps but skips publishing APKs. Add all six variables and run the workflow again to produce connected APKs.

## Build and install without Android Studio

1. In the GitHub repository, select **Actions**, then **Build Android APK**. A push starts the workflow; you can also choose **Run workflow**.
2. When the run finishes successfully, open it and download the `media-share-apks` artifact.
3. Extract the downloaded ZIP. Install `shm-debug.apk` on the user’s phone and `rev-debug.apk` on the admin’s phone. If Android asks, allow installing apps from that source.

These are unsigned debug APKs for testing, not Play Store releases. The existing ClassBell app remains in the project but is not included in the media-sharing APK artifact.

## Run with Android Studio

Open this folder in Android Studio with JDK 17 and Android SDK 35 installed, sync the Gradle project, then run the `app` configuration on an Android device or emulator. On Android 12 and newer, allow the app to schedule exact alarms when prompted. Allow notifications so the alarm alert can be shown.
