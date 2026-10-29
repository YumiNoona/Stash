# Privacy and current limits

Stash has no account, server, analytics, cloud sync or internet permission. Its app data stays on your Android device unless you explicitly share or export it.

## Local protection

- Vault records and imported attachments use AES-GCM in app-private storage. Android Keystore holds the device-bound encryption key.
- Writes use atomic replacement. Android pickers grant access to selected files; the app does not request broad storage access.
- Private reveal, copying, original identity scans and sensitive exports use device credentials. App lock and clipboard clearing can be configured in Settings.
- Card verification codes are masked and excluded from readable exports. Number masking is configurable.
- Temporary shared files are removed after ten minutes or on a later launch. Redaction creates a flattened copy and keeps the saved original.

## Backups and exports

Encrypted backups require the original device-bound key. They cannot move a vault to another phone or recover a key deleted by uninstalling. Keep the app installed until you have verified any independent copies you need.

Readable JSON, PDF, Excel and text exports are not encrypted. JSON can include attachments when private fields are explicitly included. Other readable exports list attachments; export an original through its item viewer. Store exported material privately.

## Imports and identity cards

Bundled Latin text recognition works offline. Check suggested names, numbers and dates against the original. PDFs are indexed up to their first 20 pages, while the original remains attached. Document attachment sets are limited to 20 files and 24 MB; the encrypted vault has a 64 MB limit.

The scanner supports manual four-corner correction, rotation, page order and multiple pages. Automatic edge detection is not included. NFC card reading is best effort and needs compatible hardware and readable fields; Stash does not connect to or authenticate with banks.

Identity templates are personal wallet views, with originals kept separately. They do not issue or verify an identity document. The labelled QR encodes only `https://venusapp.in`, not personal details. Local-language names can be entered manually; OCR currently uses Latin recognition.

## Sample screenshots

All six published screenshots contain fictional sample records made in an isolated emulator. Fixture records, documents and the temporary seeding helper are excluded from the production APK. A new installation starts empty.
