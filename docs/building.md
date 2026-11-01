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

The Play upload key for this checkout is at `${user.home}/.android/stash-signing/stash-play-upload.p12`, with its credentials in `signing.properties` beside it. Back up **both files** in a secure location. Losing them prevents signing a matching update until Google Play resets the upload key. Do not commit or upload either file to GitHub.

```powershell
.\gradlew.bat testDebugUnitTest :app:lintRelease :app:bundleRelease :app:assembleRelease
```

Play App Bundle: `app/build/outputs/bundle/release/app-release.aab`. The APK at `app/build/outputs/apk/release/app-release.apk` is for local installation and QA. Submit the AAB through Play Console with Play App Signing enabled.

Verify it with Android SDK `apksigner verify --print-certs`, and confirm that `aapt dump badging` does not contain `application-debuggable`. Publish the release APK with a SHA-256 checksum, never a keystore, password file or debug APK.

The current source builds the refreshed version 1.0.0, internal version code 14. **Its new signing key cannot update an installation of the earlier v1.0.0 GitHub APK, even though the displayed version is the same.** Retain the same `com.lockr.app` package ID for the Play listing, and use this local key for future Play uploads.

## Earlier installations

Android will not install a production APK over an APK signed by the old debug key. This is independent of the displayed version number.

Before removing any old installation, create a **portable encrypted backup** if that version offers it, keep its passphrase safe, and verify that the file exists. The earlier v1.0.0 GitHub APK did not include portable backup; on that build, export readable copies of the information and original files you need, then open them to verify the copies. Readable exports are not encrypted. Uninstalling removes Stash's private storage and its Android Keystore key. The older device-bound backup cannot restore after that key has been deleted. There is no automatic cross-signature migration or cloud recovery.

Fresh Play installations and later releases signed through Play App Signing use the normal Android update path. A fresh vault is empty.

## Website

The complete website is in `web/`. Use Node.js 20+, then `npm run dev`, `npm run check` and `npm run build`. It needs no package installation. Static hosting consumes `web/dist/`; that generated directory is ignored by Git. Screenshots in `web/assets/screenshots/` are the single shared set for the website and repository README.
