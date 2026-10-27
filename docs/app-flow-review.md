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

- All 21 JVM unit tests passed with `testDebugUnitTest`.
- `:app:assembleDebug` produced the installable debug APK.
- `git diff --check` passed.

This is a source review, not a completed device certification. Camera, NFC,
credential prompts, and file pickers need checks on a device or emulator.
