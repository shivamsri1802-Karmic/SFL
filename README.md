# SFL — Save Favorite Location

SFL is an Android app for saving and organizing the places that matter to you —
not just search results, but a personal map of spots worth remembering: a
friend's house, a favorite café, the good parking spot, a place from a trip
you want to find again.

It's built around a simple idea: saving a place should take one tap, and
finding it again later should be just as fast.

## Features

**Capture**
- Map-centric home screen — see all your saved places at a glance, with Home
  and Work highlighted in their own colors
- One-tap save from your current location, with a draggable pin to fine-tune
  the exact spot before confirming
- A home-screen widget for capturing a location without opening the app
- Plus Code support alongside the street address, for places with no clean
  postal address

**Organize**
- Collections — group places into lists like "Weekend Trip" or "Restaurants
  to Try," browsable on their own
- Set a Home and Work location for one-tap access from the map
- Link a saved place to a phone contact ("my friend's house")
- Search by name, address, or type, with distance-from-me and nearest-first
  sorting
- Multi-select batch actions — delete, export, or add several places to a
  collection at once

**Share & back up**
- Share a Collection with another SFL user as a link or QR code
- Export/import your full location list as a JSON file
- Optional, off-by-default backup to your own Google Drive (App Data folder
  — invisible in your regular Drive, readable only by this app)

**Everything else**
- Light, dark, and system theme support
- Undo on delete, so a stray tap never costs you data

## Privacy

SFL doesn't upload your saved locations anywhere or track you in the
background unless you explicitly turn a feature on. Cloud backup is opt-in
and off by default; sharing a collection is something you trigger yourself.
Your data lives on your device unless you choose otherwise.

## Tech stack

- Java, Android SDK (minSdk 23, targetSdk 34)
- Google Maps SDK for Android + Fused Location Provider
- SQLite (local storage, no external database)
- Material 3 components
- Google Sign-In + Drive REST API (for the optional backup feature)
- ZXing (QR code generation for shareable collections)

## Building it yourself

1. Clone the repo and open it in Android Studio.
2. Copy `local.properties.sample` to `local.properties` and add your own
   Google Maps API key (`MAPS_API_KEY=...`). Get one from the
   [Google Cloud Console](https://console.cloud.google.com/) and restrict it
   to this app's package name (`com.shivam.sfl`) and your signing
   certificate's SHA-1 fingerprint.
3. To use the optional Google Drive backup feature, you'll also need to
   enable the Google Drive API and create an Android OAuth client ID in the
   same Cloud Console project (package name + SHA-1, same as above). This
   step is only required if you want that feature working — the rest of the
   app functions without it.
4. Let Gradle sync, then build and run.

## License

No license has been chosen for this project yet.
