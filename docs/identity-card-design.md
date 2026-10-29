# Offline identity cards

The Aadhaar, PAN, voter ID and driving licence views are personal wallet layouts, informed by the issuer documents below. They are not newly issued credentials. Stash keeps the original files alongside editable fields. Eight reference-matched raster faces provide the artwork; live names, dates, numbers and addresses are placed over them. All records and image crops use the existing device-bound AES-GCM vault. No network permission or verification service is added.

## References checked on 28 October 2026

- [UIDAI Secure QR reader](https://uidai.gov.in/en/qr-code-reader): demographic fields, portrait and signed QR. This informed the original field schema. The current wallet design instead displays a labelled website QR for https://venusapp.in; it contains no identity data.
- [Income Tax Department PAN reference, page 1](https://www.incometaxindia.gov.in/documents/d/guest/1-permanent-account-number-pan-pdf): blue card, holder/parent names, DOB, photograph, signature and QR placement; PAN is 10 alphanumeric characters.
- Election Commission of India, Manual on Electoral Rolls, March 2023, Document 10, pp. 116-117: elector and relative names, gender, DOB or age, photo on front; address, constituency and QR on back. [Official ECI publication catalogue](https://www.eci.gov.in/manuals).
- [MoRTH common-format design reference, draft 30 October 2018](https://morth.nic.in/sites/default/files/notifications_document/Draft_Notification_no_GSR_1073E_dated_30_October_2018_regarding_common_format_for_the_Driving_License_and_Registration.pdf): licence number, issuing authority, issue and validity dates, DOB, blood group, photo, relative, address and vehicle classes. This is a visual reference, not a claim that all current state licences use one design.

## Design and data handling

- Aadhaar: white face, original-style Indian emblem/Aadhaar logo, tricolour brush header, fingerprint watermark, left portrait, front demographics and large number, reverse address and contact footer.
- PAN: classic blue/lilac fingerprint background, top departmental/Government of India headings, centred emblem and PAN number, left details and right portrait; reverse note/address and website QR.
- Voter ID: ECI/Indian emblem, pale tricolour waves, dotted India map and chakra, left front portrait and right details; reverse EPIC, address, constituency, part, serial and issue date.
- Maharashtra licence: cream/pink face, red heading, Indian emblem/MMVD graphic and central watermark, chip graphic, right photo, date/authorisation and holder details. Reverse keeps the same background and logos with address/classes/validity and website QR.
- Other ID uses a neutral blue information layout without official headings.
- User-supplied reference images guide the generated static artwork. Personal names, numbers, dates, addresses and signatures are not baked into any artwork file. The app labels the result as a personal wallet view, with originals separately accessible.
- Portraits can be selected directly or cropped from originals. Signatures can be cropped too. Photo imports are bounded and respect image orientation.
- Image/PDF originals are retained unless removed by the user before Save. Asset roles remain compatible with older records.
- OCR is bundled Latin recognition with conservative labelled-field suggestions; local-language names remain manually editable. Format validation does not verify authenticity.
- ID numbers are masked in lists. Device credentials guard private details, original files, copying and editing. The website QR is public and contains no personal data.
- Existing vaults and payment cards remain compatible. All identity fields and media stay in encrypted local storage; no runtime internet permission is added.

## Raster artwork and website QR

The selected artwork comes from built-in imagegen reference-image editing calls,
with a targeted repair for the single-card licence reverse. Outputs are losslessly encoded as WebP with identical pixels in app/src/main/res/drawable-nodpi as identity_{aadhaar,pan,voter,licence}_{front,back}_v3.webp.
The exact prompts, reference paths, selected asset paths and generation mode are
recorded in artwork-sources.json. The earlier decorative-paper variants are
not bundled. Static issuer headings may be part of the artwork; all personal data
is rendered separately.

venus_website_qr.png is a deterministic high-error-correction QR with a four-module
quiet zone. Its decoded payload was checked as exactly https://venusapp.in. All
identity types and both sides share it. Previously saved original QR/logo assets
are preserved in the encrypted record for compatibility; there is no original-QR
crop control in the current form. The generic uploaded document viewer is unchanged.
