# SFL improvement tasks

Tracking the improvements identified during a code review of the SFL (Save Favorite
Location) Android app, and which ones have been applied. Last updated 2026-08-29.

## Done this pass

- [x] **Fix: "Show on map" button silently did nothing.** `SaveLocation.mLocation` was
      declared but never assigned, so `showMap()` always exited early. Now set from the
      location callback.
- [x] **Fix: saved-locations list could crash on open.** Timestamps were stored via
      locale/timezone-dependent `Date.toString()` and re-parsed with a fixed-pattern
      `SimpleDateFormat`, which throws `ParseException` on many locales. Timestamps are now
      written with a fixed-locale format (`yyyy-MM-dd HH:mm:ss`, `Locale.US`) and the list
      sort does a plain string compare instead of re-parsing.
- [x] **Fix: dead code after navigating away.** `CompleteSaveLocation.saveLocation()` used
      to disable fields / set a status label *after* already starting the next Activity.
      Reordered so the UI updates happen first, then a `Toast` + navigation.
- [x] **Fix: deleting a saved location while a search filter was active left the item
      re-appearing once the filter cleared.** The delete handler only removed the item from
      the filtered/display list, not the adapter's backing list. Now removes from both.
- [x] **Fix: Geocoder returning no result would crash.** `SaveLocation`'s location callback
      always assumed `addressList.get(0)` existed; a real device can legitimately get zero
      results back. Now guarded.
- [x] **Hardening: SQL built by string concatenation.** `DatabaseHandler.addLocation()`'s
      existence-check query is now parameterized instead of splicing the id into the SQL
      string directly.
- [x] **Hardening: Maps API key no longer hard-coded in the manifest.** Moved to
      `local.properties` (`MAPS_API_KEY=...`) via `manifestPlaceholders` in
      `app/build.gradle`; added `local.properties.sample` and a `.gitignore` so it can't
      land in version control. **You still need to rotate the actual key value in Google
      Cloud Console** - the one that shipped in the original APK is already public and must
      be treated as compromised. Update `local.properties` once you have the new one.
- [x] **Cleanup: dropped the unused `google_maps_key` placeholder string** in
      `strings.xml` (superseded by the manifest placeholder above).
- [x] **Cleanup: removed the checked `XmlPullParserException` clutter.** Every
      `exportLocation()` method declared `throws XmlPullParserException` and every caller
      wrapped it in a no-op `try/catch (XmlPullParserException e) { e.printStackTrace(); }` -
      it's never actually thrown anywhere in this code path. Removed across
      `MainActivity`, `SaveLocation`, `CompleteSaveLocation`, `SavedLocationList`, and
      `LocationImportAndExport`.
- [x] **Validation: block saving a location with a blank name.** `CompleteSaveLocation`
      now shows a field error and refuses to save if the name is empty.
- [x] **Threading: reverse-geocoding moved off the main thread.** `Geocoder.getFromLocation()`
      can block on network/DNS; it now runs on a background thread (new `AppExecutors`
      helper) with the UI update posted back to the main thread.
- [x] **Threading: saving/deleting a location no longer blocks the UI thread.**
      `CompleteSaveLocation`'s DB write and `SavedLocationListAdapter`'s DB delete both now
      run on `AppExecutors`'s background thread; the UI updates optimistically/immediately
      where it's safe to do so (field disabling, list removal) and the DB call catches up
      behind it.
- [x] **Performance: search filtering uses `DiffUtil` instead of `notifyDataSetChanged()`.**
      Keeps item animations and avoids re-binding every row on each keystroke in the search
      box.
- [x] **Tests: added a first unit test.** `SavedLocationEntityTest` covers
      `SavedLocationEntity.fromJson()` (the logic `LocationImportAndExport`'s import feature
      depends on) - a full/partial round trip and the missing-field case. Added
      `junit:junit` and `org.json:json` (the real implementation, since Android's own
      `org.json` is a compile-only stub on the JVM test classpath) as `testImplementation`
      dependencies.

## Deliberately not automated this pass

These are real improvements but are either large, cross-cutting refactors or need a
compiler/emulator in the loop to verify safely - flagging them rather than guessing:

- [ ] **Collapse the bottom-navigation / badge-count / export-import-menu boilerplate**
      that's currently hand-duplicated (with small inconsistencies) across all four
      Activities into a shared base Activity. High value for maintainability, but touches
      every screen at once - worth doing as its own reviewed change.
- [ ] **Migrate `startActivityForResult`/`onActivityResult` to the Activity Result API**
      (`registerForActivityResult`) for the file-import and GPS-settings flows. Deprecated
      but functional as-is; migrating touches the same four Activities and is easiest to
      verify with a build in the loop.
- [ ] **Move the remaining synchronous DB reads off the main thread** - the per-screen
      badge-count query and `SavedLocationList`'s initial "load all locations to populate
      the RecyclerView" query still run synchronously in `onCreate`. Left as-is because a
      local SQLite read of a small table is fast in practice and restructuring
      `SavedLocationList`'s `onCreate` (adapter creation + search-listener wiring both
      depend on the loaded list) has more moving parts to get right without a compiler
      to check against.
- [ ] **Handle "permission permanently denied"** in `SaveLocation` - currently just shows
      a Toast and leaves the user stuck if they've denied location access and checked
      "don't ask again". Needs a rationale/settings-redirect path.
- [ ] **Consider Room** for the data layer if this app keeps growing - would remove the
      manual cursor-mapping boilerplate in `DatabaseHandler` and catch query mistakes like
      the SQL-concatenation issue above at compile time.
- [ ] **Broader test coverage** - `DatabaseHandler` (needs Robolectric or an instrumented
      test since it touches `SQLiteOpenHelper`) and the import/export round trip in
      `LocationImportAndExport` are the next-highest-value targets after the entity test
      added here.

## Reminder

Rotate the Google Maps API key in Google Cloud Console (restrict it to this app's package
name + SHA-1 fingerprint), then update `MAPS_API_KEY` in `local.properties`.
