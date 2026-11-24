# File formats

Use **Add to Stash > Documents > Files** for any local file type. Photos also works for images exposed by the Android photo picker. Imports stay offline, are encrypted with the vault and preserve the original file bytes. Stash corrects missing or incorrect MIME labels using recognized file headers and extensions.

| Format | In-app support |
| --- | --- |
| PNG, JPG/JPEG/JFIF, WebP, BMP | Image preview, zoom, crop/rotate, local OCR and original export |
| GIF | Still-image document preview and OCR; original GIF retained. Custom card artwork supports animation separately. |
| HEIF/HEIC | Image preview and OCR through the device decoder on compatible Android 9+ devices; original retained on every supported Android version |
| AVIF | Image preview and OCR on compatible Android 12+ devices; original retained on older devices |
| PDF | Multi-page preview, zoom, local OCR and original export. Locked or damaged PDFs remain stored/exportable but may not preview. |
| TXT, Markdown, CSV/TSV, JSON, XML, HTML, log | Bounded plain-text preview, UTF-8/UTF-16 BOM handling and text indexing; scripts/HTML are never executed |
| Word, Excel, PowerPoint, OpenDocument, RTF, TIFF, SVG, RAW, ZIP and other files | Encrypted storage, sharing and original download. No built-in Office, RAW or archive editor. |
| `.img` or extensionless files | Recognized image/PDF headers enable preview. A disk image or unknown binary is stored as an original file. |

Several images can be kept together as a swipeable document. Compatible image pages export together as a PDF; mixed files and unsupported image formats export together as a ZIP. Individual original attachments are also available from file information. Editing a page intentionally creates a replacement image; unedited imports are preserved byte for byte.

Limits: 20 files and 24 MB total per document, within the 64 MB encrypted vault limit. Text previews show up to 120,000 characters and identify truncation; the full original remains downloadable. Unsupported codecs never require a network upload or conversion service. Preview availability for HEIF/AVIF depends on Android version, codec/profile and the device; see [Android supported image formats](https://developer.android.com/media/platform/supported-formats#image-formats).
