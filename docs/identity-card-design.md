# Offline identity cards

The Aadhaar, PAN, voter ID and driving licence views are personal wallet layouts, informed by the issuer documents below. They are not newly issued credentials. Stash keeps the original files alongside editable fields. All records and image crops use the existing device-bound AES-GCM vault. No network permission or verification service is added.

## References checked on 28 October 2026

- [UIDAI Secure QR reader](https://uidai.gov.in/en/qr-code-reader): demographic fields, portrait and signed QR. Preserve the original QR bitmap; Stash does not generate, decode or verify its digital signature.
- [Income Tax Department PAN reference, page 1](https://www.incometaxindia.gov.in/documents/d/guest/1-permanent-account-number-pan-pdf): blue card, holder/parent names, DOB, photograph, signature and QR placement; PAN is 10 alphanumeric characters.
- Election Commission of India, Manual on Electoral Rolls, March 2023, Document 10, pp. 116-117: elector and relative names, gender, DOB or age, photo on front; address, constituency and QR on back. [Official ECI publication catalogue](https://www.eci.gov.in/manuals).
- [MoRTH common-format design reference, draft 30 October 2018](https://morth.nic.in/sites/default/files/notifications_document/Draft_Notification_no_GSR_1073E_dated_30_October_2018_regarding_common_format_for_the_Driving_License_and_Registration.pdf): licence number, issuing authority, issue and validity dates, DOB, blood group, photo, relative, address and vehicle classes. This is a visual reference, not a claim that all current state licences use one design.

## Design and data handling

- Aadhaar: warm ivory, saffron/white/green rule, portrait left, QR right, number below, address on back.
- PAN: pale blue/lilac, issuer heading, portrait left, holder/parent/DOB in the middle, original QR right.
- Voter ID: ivory/sage, elector photo and family details in front; constituency, address and original QR on back.
- Driving licence: cream/burgundy, date grid left and portrait right; address, classes and transport validity on back.
- Other ID uses a neutral lavender layout.
- Issuer names and compact identifiers are built in. An authentic issuer logo and signature can be cropped from the user's original. No generated emblems, holograms or security codes are bundled.
- Crop tools support image files and PDF page selection. A crop is a separate PNG attachment with an explicit role. Original attachments are retained unless the user removes them before Save.
- OCR is bundled Latin recognition with conservative labelled-field suggestions. Local-language names can be entered manually. Format validation does not verify authenticity.
- ID numbers are masked in lists. Device credentials guard private details, QR, original files, copying and editing. Private readable exports retain the existing explicit opt-in and credential check.
- Existing card and vault formats remain compatible. Identity records use metadata keys and normal encrypted attachments; payment cards keep their existing form and artwork.
