# App flow review

Stash starts with onboarding, then loads its encrypted local vault. The main
collections lead to an item detail screen, or to a document preview with an
optional detail screen. Add opens a category chooser, an import or manual entry
step, a review screen, and finally the saved item's collection. Settings handles
local exports and backup restore; profile changes stay on the device.

## Fixes in this review

- Apply the saved App Lock preference on every new app instance.
- Hide add and backup confirmation dialogs while the vault is locked.
- Return to Home after activity recreation rather than restore a temporary route
  without its selected item, draft, or running import.
- Keep system Back from leaving an import or an active item write midway.
- Open review immediately after a partial import, before waiting for the warning
  message to disappear.
- Clear the previous card draft after saving.
- Reset item authorization and detail return destinations when opening an item.
- Guard item writes before launching background work to prevent repeated taps
  from starting overlapping add, edit, and delete operations.

## Device checks still required

1. Enable App Lock, close the app, and reopen it. Confirm the lock appears before
   any vault content. Background with an add dialog open and verify it is hidden
   when the lock timeout expires.
2. Import several files including one exceeding the attachment limit. Confirm
   successfully imported files reach review immediately and show the warning.
3. Save a card, then start another card import. Confirm old draft values are absent.
4. Open document preview, open its details, return, then open and delete a card.
   Confirm deletion returns to its originating collection rather than an empty preview.
5. Recreate the activity from a review or detail screen. Confirm Home opens and
   saved items remain available. Unsaved drafts are intentionally not restored.
6. Tap Save repeatedly and confirm a single record is created. Check edit and
   delete while storage is busy.

## Automated verification

- All 31 JVM unit tests passed with `testDebugUnitTest`.
- `:app:assembleDebug` produced the installable debug APK.
- `git diff --check` passed.

This is a source review, not a completed device certification. Camera, NFC,
credential prompts, and file pickers need checks on a device or emulator.

## Onboarding and card appearance follow-up

- Keep onboarding navigation stationary and use a 420 ms eased page slide; ignore
  repeat Next taps during a transition. The third page has no top-right action.
- Use the same 1.586:1 card ratio in lists, review, and saved detail screens.
- Give partner brand and issuer separate fields, keep chip/contactless together,
  and place number, expiry/CVV, and holder/network in consistent rows.
- Replace the two-row finish grid with one swipe row of eight distinct designs.
  Motion is a separate Still, Light sweep, Float, or Glow choice.
- Import custom images, GIFs, and videos into the encrypted attachment store.
  Video playback uses an in-memory media source, without decrypted cache files.
- Keep custom artwork separate from original scans. Do not persist raw OCR text
  on new cards or show the extraction block on existing card detail pages.

Android 15 emulator checks passed for all three onboarding steps, manual card
entry, save and reopen, detail proportions and field order, GIF import, and MP4
import/playback after save. Successive video frames differed and no Android
runtime or media errors were recorded. Tests used fabricated card details and
synthetic media only. Physical-device codec compatibility remains device dependent.

## Theme, tags, and visual refinement

- Onboarding uses a chevron back icon and a tighter continue progress ring.
- The beveled dock has a smooth animated add orb. Reduce motion stops it.
- Light, Dark, and Follow device themes apply to forms, dialogs, previews, and
  collections. Settings has a theme toggle at the top right.
- Cards and Documents share header spacing, raised tag filters, and empty-state
  geometry. Custom card tags and document groups can be added, assigned, filtered,
  searched, and deleted. Deletion removes assignments, preserving item data.
- Fifteen independent bitmap artworks replace the primitive finishes. Aurora and
  Midnight remain; the other thirteen are built-in ImageGen artwork. Prompt and
  asset provenance is saved in card-art-sources.json.
- Ten independently selectable effects plus Still are available. Custom media
  import uses an icon, and the explanatory autoplay text has been removed.
- Card nickname fields are removed. Six detail fields remain, with card type and
  network selectors. Eye/copy actions align with their value row.
- Document review omits OCR controls. File information shows size, type, added
  date, and group. Older records retain their original saved-date label when a
  full timestamp is unavailable.
- The extraction dialog has detected-field choices, an editable value, numbered
  OCR lines, and a fixed Copy/Save footer that respects the keyboard.

Android 15 QA used a separate emulator and fabricated data. It covered theme
switching, custom document tag creation/assignment/deletion, image import, file
information, and extraction/save. The existing user emulator was not cleared.

## Floating glass and illustrated-art follow-up

- Bundle Space Grotesk Regular, Medium, and Bold from the official author repo,
  with its OFL license. Strengthen titles and light-mode text contrast.
- Use a floating translucent rounded dock with inset destination states, a raised
  animated add orb, soft shadows, and a subtle gradient rim.
- Replace near-black fills with charcoal/lavender/teal lighting and glass panels.
- Give both Home and Profile avatars an inset gradient frame that stays visible
  against light and dark surfaces.
- Remove the duplicate Theme row and dialog; retain the top-right toggle.
- Hide artwork/motion headings in edit-card and remove the swipe instruction.
- Keep 15 selectable styles, replacing nine flowing textures with original
  illustrated/poster designs. Prompt and asset provenance is in
  card-art-v3-sources.json. Legacy style IDs still resolve correctly.

Verification: all 31 unit tests and the debug APK build pass. Android 15 emulator
checks cover light/dark switching, the floating dock, profile frame, typography,
card artwork, and edit-card controls using fabricated data in a separate emulator.

## Version 1.5 release validation

- Replace four rejected mascot/geometric finishes with original vector botanical
  card designs. Keep Moon Garden and stable saved style IDs.
- Preview full card layouts in the art chooser, including a detailed silver chip.
- Improve contrast and colored-glass styling for dark Add to Stash tiles.
- Present documents on the full canvas with four floating tools: Edit, Scan,
  Share, Delete. Info is at the top right.
- Show image preview, date/time, MB size, file type, and storage in information;
  remove document-group controls from that screen.
- Bound zoom/pan, preserve PDF paging, and handle malformed previews safely.
- Include the Space Grotesk, profile-frame, softer-theme, and dock refinements
  from the preceding visual review in this release.

Release checks passed: 31 JVM tests, debug APK assembly, and diff whitespace
validation. Emulator checks covered the dark add sheet, full-canvas PNG view,
four tool actions, image information without group controls, two-page PDF
navigation to page 2, and opening a malformed PDF without a crash. Test records
and files were fabricated and confined to the separate QA emulator.
