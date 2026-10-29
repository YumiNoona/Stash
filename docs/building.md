# Building and installing Stash

## Development

Use JDK 17 or newer, Android SDK Platform 36, and the checked-in Gradle wrapper.

```powershell
.\gradlew.bat testDebugUnitTest :app:lintDebug :app:assembleDebug
```

Development APK: `app/build/outputs/apk/debug/app-debug.apk`. It uses Android's local debug key. Never publish this file as a production release.

## Production signing

The release build enables R8 code optimization, resource shrinking and `debuggable=false`. It requires a production key and fails if signing properties are missing. There is no fallback to a debug key.

By default, Gradle reads `${user.home}/.android/stash-signing/signing.properties`. Alternatively, set `STASH_SIGNING_PROPERTIES` to an absolute properties-file path. Keep this file and the keystore outside the repository with private file permissions.

```properties
storeFile=C:/private/location/stash-release.p12
storePassword=YOUR_PRIVATE_PASSWORD
keyAlias=stash
keyPassword=YOUR_PRIVATE_PASSWORD
```

Create your own key using the JDK's `keytool` or Android Studio's signed APK workflow. Back up both the key and passwords securely: future production updates must retain the same signing identity. Do not commit or upload them to GitHub.

```powershell
.\gradlew.bat testDebugUnitTest :app:assembleRelease :app:lintRelease
```

Production APK: `app/build/outputs/apk/release/app-release.apk`.

Verify it with Android SDK `apksigner verify --print-certs`, and confirm that `aapt dump badging` does not contain `application-debuggable`. Publish the release APK with a SHA-256 checksum, never a keystore, password file or debug APK.

The v1.0.0 production signing certificate SHA-256 is:

```text
fac3948fe1fd202f198b6a7cdecbdcb92e95704a876cfe73d5a121b1cc07534b
```

The public version is reset to 1.0.0. The internal version code is 13, above all earlier test builds. Future releases must continue increasing the internal code.

## Earlier debug-signed installations

Android will not install a production APK over an APK signed by the old debug key. This is independent of the displayed version number.

Before removing a test installation, export readable copies of the information and original files you need, open them to verify the copies, and keep them private. Readable exports are not encrypted. Uninstalling removes Stash's private storage and its Android Keystore key. An encrypted backup alone cannot restore data after that key has been deleted. There is no automatic cross-signature migration or cloud recovery.

Fresh installations and later releases signed with the production key use the normal Android update path. A fresh vault is empty.

## Website

The complete website is in `web/`. Use Node.js 20+, then `npm run dev`, `npm run check` and `npm run build`. It needs no package installation. Static hosting consumes `web/dist/`; that generated directory is ignored by Git. Screenshots in `web/assets/screenshots/` are the single shared set for the website and repository README.
