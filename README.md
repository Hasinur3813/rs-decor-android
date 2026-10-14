# RS Decor Android

RS Decor is an Android catalog app for browsing home-decoration designs, including
wallpaper, ceiling, and epoxy-floor designs. It is built with Kotlin and Jetpack
Compose and uses Firebase Firestore for the remotely managed catalog and app
settings.

## Features

- Browse design categories and individual designs.
- Filter the catalog by category and room, and search by title, category, or
  room.
- View design details and contact RS Decor by phone or WhatsApp.
- Load catalog entries in 24-item Firestore pages. Search uses indexed word
  prefixes rather than downloading the full catalog.

## Requirements

- Android Studio with the Android SDK required by the Gradle project.
- Access to the RS Decor Firebase project for live catalog data.
- A Firebase configuration file for the Android app.

## Set up and run

1. Clone this repository and open the `rs-decor-android` directory in Android
   Studio. Allow Gradle to sync.
2. Download `google-services.json` for the Android application ID configured in
   `app/build.gradle.kts` from the Firebase Console. Place it at
   `app/google-services.json`. This file is excluded from Git; do not force-add
   it to the repository.
3. If you use Gemini API functionality, copy `.env.example` to `.env` in the
   project root and set `GEMINI_API_KEY` to your key. Keep `.env` private.
4. Select an emulator or connected Android device and run the `app`
   configuration in Android Studio.

You can also build and run the unit tests from PowerShell:

```powershell
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:testDebugUnitTest
```

## Firestore indexes

The app's catalog queries require the composite indexes in
`firestore.indexes.json`. The Android client's Firestore rules are public-read
and deny writes; the admin panel changes data through the server-side Firebase
Admin SDK. Deploy both rules and indexes to the Firebase project before
releasing. From a directory whose `firebase.json` points at the Android
project's `firestore.rules` and `firestore.indexes.json`, run:

```shell
firebase deploy --only firestore:rules --project YOUR_FIREBASE_PROJECT_ID
firebase deploy --only firestore:indexes --project YOUR_FIREBASE_PROJECT_ID
```

Wait for the indexes to finish building in the Firebase Console before releasing
the app. When the admin panel's **Designs** page is opened, it backfills search
tokens for existing catalog entries in resumable batches. New and edited
designs maintain their search tokens automatically.

## Release signing

Create an upload key (the configured alias is `upload`) and keep the keystore
and passwords in a secure backup. Enroll the app in Play App Signing when
creating its Play Console listing; the upload key is used to upload bundles.
By default, Gradle looks for `rs-decor-upload.jks` in your Windows user folder
(`%USERPROFILE%`). You can override its location with:

```powershell
$env:KEYSTORE_PATH = "$env:USERPROFILE\rs-decor-upload.jks"
```

Build a signed bundle from PowerShell in this project directory with:

```powershell
.\build-release.ps1
```

The script prompts for the keystore and key passwords without displaying them,
passes them to Gradle for this build only, and clears them afterward. Do not
save passwords in project files or commit the keystore.

The bundle is written to
`app/build/outputs/bundle/release/app-release.aab`. Verify the signing
configuration with `.\gradlew.bat :app:signingReport` before uploading.

## Play Store release checklist

- Confirm the application ID in `app/build.gradle.kts` is the permanent ID for
  this Play listing before its first upload. Changing it later creates a
  different app. The Firebase Android app registration and
  `app/google-services.json` must use the same ID.
- Increment `versionCode` for every uploaded release and update `versionName`
  as appropriate.
- Deploy the Firestore indexes and the read-only client rules from
  `firestore.rules`. The admin panel writes through its server-side Firebase
  Admin SDK; the published Android app does not need Firestore write access.
- Test the release build on supported devices, including catalog loading,
  search, offline/error states, and phone/WhatsApp links.
- In Play Console, provide a public privacy-policy URL, complete the Data
  safety and app-content declarations based on the app and its Firebase
  configuration, set the content rating and target audience, and add the
  required store listing graphics and screenshots.
- Upload the signed Android App Bundle to an internal testing track first and
  review Play Console's pre-launch report before requesting production access.
