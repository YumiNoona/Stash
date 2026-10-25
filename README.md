# Stash for Android

Stash is an offline-first personal vault for cards, identity documents, passwords, notes, and attached files. It is a native Android app built with Kotlin and Jetpack Compose.

## Privacy and storage

- Vault records and selected gallery/files attachments are stored in the app's private storage and encrypted together with AES-GCM.
- The encryption key is held by Android Keystore and is device-bound. An encrypted backup can be restored only on the device that created it.
- Stash has no account, cloud sync, server, or `INTERNET` permission. The installed app does not need a network connection.
- Gallery and file access use Android's system pickers. Stash copies selected files into its encrypted vault; it does not retain a live link to the source file.
- App Lock uses the device's screen-lock credential. Set a PIN, password, or pattern on Android before enabling it.
- The vault starts empty. Stash does not insert demo cards, documents, or passwords.

Gradle may need network access once to download Android build dependencies. This is separate from the app, which runs locally.

## Features

- Add cards, IDs, documents, passwords, and notes by typing details or selecting photos and files.
- Preview image attachments and save a copy of any attachment to a location chosen by the user.
- Reveal protected card/document/password values temporarily.
- Create and restore an encrypted local backup.
- Browse the vault while offline.

## Build

Requirements: JDK 17 and Android SDK Platform 36. From PowerShell in this directory:

```powershell
.\gradlew.bat :app:assembleDebug
```

The installable debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`. Install it on a connected device with:

```powershell
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

The GitHub release includes the APK for Stash `1.0.0`. It is signed with the local Android debug key for direct testing; Play Store distribution requires a private release signing key.
