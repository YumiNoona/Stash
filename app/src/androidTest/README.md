# Offline file-format checks

Run `gradlew.bat connectedDebugAndroidTest` on a fresh test emulator. The target is the debug app; use a separate emulator from any installation containing real data. API 31+ is needed to run both modern-codec checks; the release QA emulator uses API 35.

The fixtures are synthetic 160 x 96 red/blue images created with Pillow (HEIC fixtures use pillow-heif), a small plain-text file, an SVG rectangle, a minimal DOCX archive and arbitrary binary bytes. They contain no personal information and are not packaged into production APKs. The test generates its own two-page PDF at runtime.

A test-only read-only content provider returns generic MIME labels so imports exercise header/extension detection. Tests check preview bounds, JPEG/HEIC rotation, HEIF/AVIF decoding, PDF paging, corrupt PDF fallback, original-file round trips and mixed-file ZIP preservation. The tests never read or modify the saved vault.
