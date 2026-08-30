# Save Favorite Location (SFL)

**Package:** `com.shivam.sfl` · **Platform:** Android (Java, min SDK 23 / target SDK 34) · **Type:** Native Android app, reconstructed via decompilation (jadx) from `SFL.apk`

## What it is

SFL is a simple offline-first Android app for saving, organizing, and sharing places the user cares about. Instead of relying on Google Maps' built-in favorites, it lets the user capture their current GPS location, tag it with a name and category, and keep a personal, searchable list of those places — stored locally in a SQLite database on the device. The app's own tagline, pulled from its strings resource, sums up its privacy stance: *"We neither upload your saved locations without your intervention nor track you in background."*

## Core capabilities

**Capture the current location.** The Save Location screen requests fine/coarse location permissions, then uses Google Play Services' Fused Location Provider to get a GPS fix (10s update interval, 5s fastest interval, high-accuracy priority). The coordinates are reverse-geocoded via Android's `Geocoder` into a human-readable address, and both are shown live on an embedded Google Map with a marker.

**Save and categorize a place.** From the capture screen, the user names the location and assigns it a type from a long predefined list of ~90 place categories (Restaurant, Park, Hospital, Gas Station, Temple, Hardware Store, etc. — modeled on Google Places types). Saving writes a row into a local SQLite table (`locations`) with id, name, latitude, longitude, address, type, and a timestamp. Re-saving with an existing id updates that row instead of duplicating it, so the same flow serves both "add new" and "edit."

**Browse and search saved locations.** The Favorite Locations list shows every saved place, most-recent first, in a scrollable card list (RecyclerView) with name, coordinates, address, type, and save time. A live search box filters the list by category/type as the user types.

**Act on a saved location.** Each entry in the list has inline actions:
- **Map** — opens the coordinates directly in the Google Maps app.
- **Share** — shares the name, address, and a Google Maps link via Android's standard share sheet (SMS, email, WhatsApp, etc.).
- **Edit** — reopens the entry in the save/edit screen, pre-filled, so any field can be changed.
- **Delete** — removes it after a confirmation dialog.

**View a location on an internal map.** A dedicated in-app map screen (separate from the Google Maps handoff) shows a single saved point with zoom controls and a "my location" button, without leaving the app.

**Import and export the whole list.** The user can export all saved locations to a JSON file (`favorite_location.json`) and share it via any app on the device (e.g. to back it up or hand it to another device), and import a previously exported JSON file back in — useful for migrating data between devices or restoring a backup, since everything otherwise lives only in the local database.

**Guided first-run tutorial.** On first use, a showcase/tooltip overlay (via the ShowcaseView library) walks the user through the bottom navigation: viewing favorites, saving the current location, and exporting data. This only shows once, tracked via a `SharedPreferences` flag.

**GPS-off handling.** If the device's location provider is disabled when the user tries to save a location, the app prompts them to enable it via the system location settings screen rather than failing silently.

## Screens (Activities)

| Screen | Purpose |
|---|---|
| `MainActivity` | Home screen / launcher; bottom navigation hub |
| `SaveLocation` | Captures current GPS position + address, shows it on a live map |
| `CompleteSaveLocation` | Name + categorize the captured point, commit to the database (also used for editing) |
| `SavedLocationList` | Searchable list of all saved locations with per-item actions |
| `MapsFragment` | Standalone full map view for a single saved point |

## Data & storage

- **Local SQLite database** (`sfl_location`, via `SQLiteOpenHelper`) — the single source of truth; no backend server or account system.
- **`SavedLocationEntity`** — the plain data model (id, name, lat/long, address, type, timestamp), with JSON (de)serialization for import/export.
- **`SharedPreferences`** — small bits of app state (whether the onboarding tutorial has been shown).
- **JSON file export/import** via `FileProvider`, so favorites can be backed up or moved between installs without any network dependency.

## Tech stack

- Java, AndroidX (AppCompat, RecyclerView, ConstraintLayout, CardView, Material Components)
- Google Play Services: Maps SDK + Fused Location Provider
- Android `Geocoder` for reverse geocoding (coordinates → address)
- `com.github.amlcurran:ShowcaseView` (via JitPack) for the onboarding tooltip overlay
- No networking/backend libraries, no analytics SDK, no third-party crash reporting — consistent with the app's "we don't track you" claim

## Known issues / things to be aware of

- **This codebase is decompiled, not original source** — it was reconstructed with jadx from a compiled APK, so exact original naming/comments are lost even though behavior should be equivalent (see the project's own `README.md`).
- **A live-looking Google Maps API key is hard-coded in `AndroidManifest.xml`.** It was found embedded in the original APK and should be treated as compromised — rotate/restrict it in Google Cloud Console before shipping or running this build against real quota.
- **Minor SQL-string-building in `DatabaseHandler`** (an `id`-lookup query is built via string concatenation rather than parameter binding) — low risk here since the value is an internal integer id, but worth tightening if the code is extended.
- The project has not been build-verified (no network access to Google's Maven/JitPack repos in the environment that reconstructed it) — expect to resolve minor dependency-version issues on first Gradle sync in Android Studio.
