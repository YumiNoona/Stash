# Google Play submission package

Stash 1.0.0 (version code 14) uses package `com.lockr.app`, targets Android 16 / API 36, and supports Android 8.0+. Upload `app/build/outputs/bundle/release/app-release.aab` after running the release checks in `docs/building.md`. Enroll in Play App Signing using the local upload certificate. The AAB and signing files are intentionally ignored by Git.

## Listing assets

- `listing/icon.png`: 512 × 512 RGBA icon
- `listing/feature-graphic.png`: 1024 × 500 RGB feature graphic
- `listing/phone/*.png`: six 1080 × 2160 portrait screenshots of the production UI with fictional sample content. A clean install has no seeded records.
- `listing/en-US.md`: title, descriptions, and contact for the Play listing.

The feature graphic uses the Stash vault art style and was generated for this listing. Screenshots come from an isolated emulator and were cropped to Play's accepted 2:1 portrait ratio without stretching.

## Console entries to review

- **Privacy policy URL:** `https://github.com/YumiNoona/Stash/blob/main/docs/privacy.md`. The policy is also available in Settings → About → Privacy policy. Confirm the URL is publicly reachable after pushing this commit.
- **Contact email:** `rushikeshingale2001@gmail.com`.
- **Data safety:** Stash has no account, internet permission, ads, analytics, or remote transfer. Vault data is stored locally in encrypted app-private storage. Users can explicitly export or share data with another app. Review the current Play questionnaire, including any SDK disclosures, before submitting; do not treat this README as a completed declaration.
- **Permissions:** camera is requested only for scanning. NFC is optional and used for compatible card import. File and photo selection uses Android's pickers.
- **Financial features:** the app stores user-supplied card information but does not process payments, lend, transfer money, or connect to a bank. Complete the Financial Features declaration according to the current Console questionnaire.
- **App access:** no account is needed. A clean install begins with an empty vault. Device authentication is used only for selected sensitive actions.
- **Content rating, target audience, ads, country availability, and release tracks:** complete these in Play Console using the actual distribution plan.

## Signing transition

The earlier v1.0.0 GitHub APK and the refreshed v1.0.0 release use different signing certificates. Android cannot install the refreshed APK over the older APK. Keep the new local key and `signing.properties` securely backed up outside this repository, and use the same Play app signing identity for future versions. See `docs/building.md` for user data migration guidance.
