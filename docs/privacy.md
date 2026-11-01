# Privacy policy and current limits

Stash is made by Veil. Updated 1 November 2026. For privacy questions, email [rushikeshingale2001@gmail.com](mailto:rushikeshingale2001@gmail.com).

Stash has no account, server, analytics, ads, cloud sync or internet permission. Its app data stays on your Android device unless you explicitly share or export it. You choose when to use the camera, selected files, NFC, profile photo and offline text recognition.

## Local protection

- Vault records and imported attachments use AES-GCM in app-private storage. Android Keystore holds the device-bound encryption key. Portable backups use AES-GCM with a passphrase-derived key; Veil cannot recover a forgotten passphrase.
- Profile name, photo and display settings are kept separately in app-private storage. They are not included in the encrypted vault or portable backup.
- Writes use atomic replacement. Android pickers grant access to selected files; the app does not request broad storage access.
- Private reveal, copying, original identity scans and sensitive exports use device credentials. App lock and clipboard clearing can be configured in Settings.
- Card verification codes are masked and excluded from readable exports. Number masking is configurable.
- Temporary shared files are removed after about ten minutes or on a later launch. Redaction creates a flattened copy and keeps the saved original.
- Android automatic cloud and device-transfer backups are excluded. You control the destination of manual exports.

## Backups and exports

Portable encrypted backups can restore on another device or after reinstall when you know the passphrase. Keep both the file and passphrase safe. The older device-bound backup format still requires the original Android Keystore key and cannot move a vault to another phone or recover a key deleted by uninstalling.

Readable JSON, PDF, Excel and text exports are not encrypted. Summary exports include item types, saved dates and file counts; they omit user names, titles, free-form details, card digits and original file names. Full readable exports require device authentication. Full JSON includes attachments; other full exports list attachment names. Card verification codes are never exported. Store exported material privately and remove it from any receiving app or destination yourself when no longer needed.

Vault data stays on the device until you delete it or uninstall Stash. Uninstalling deletes the local vault and its device-bound key. Exports or shared files outside Stash must be deleted separately.

## Imports and identity cards

Bundled Latin text recognition works offline. Check suggested names, numbers and dates against the original. PDFs are indexed up to their first 20 pages, while the original remains attached. Document attachment sets are limited to 20 files and 24 MB; the encrypted vault has a 64 MB limit.

The scanner supports manual four-corner correction, rotation, page order and multiple pages. Automatic edge detection is not included. NFC card reading is best effort and needs compatible hardware and readable fields; Stash does not connect to or authenticate with banks.

Identity templates are personal wallet views, with originals kept separately. They do not issue or verify an identity document. The labelled QR encodes only `https://venusapp.in`, not personal details. Local-language names can be entered manually; OCR currently uses Latin recognition.

## Sample screenshots

All six published screenshots contain fictional sample records made in an isolated emulator. Fixture records, documents and the temporary seeding helper are excluded from the production APK. A new installation starts empty.
