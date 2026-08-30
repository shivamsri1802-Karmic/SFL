# SFL — Consolidated Feature Activities

This reconciles two independent analyses sitting in this project — `UX_PRODUCT_ANALYSIS.md`
(mine) and `ux_pm_analysis.artifact.md` (found alongside it) — into one execution-ready
backlog. Where they overlap, they're merged into a single activity. Where they genuinely
add different value, both are kept. Where they conflict, that's called out explicitly with
a recommended resolution rather than silently picking one.

## Where the two analyses agree

Both independently flagged: "Exit" wasting a bottom-nav slot, Export being misplaced as a
nav destination, the category picker causing choice fatigue, the save flow having too many
steps, the list being effectively unsearchable by name, cloud backup being the fix for the
single-point-of-failure export/import ritual, and geofenced reminders / photo attachments /
shareable links as the next tier of real utility. That agreement across two independent
passes is a good signal these are the right priorities, not just one reviewer's taste.

## The one place they conflict, and the call

`ux_pm_analysis.artifact.md` proposes **"automated background sync to Google Drive."**
`UX_PRODUCT_ANALYSIS.md` insists sync must be **strictly opt-in**, because the app's own
`strings.xml` already promises the user "we neither upload your saved locations without
your intervention nor track you in background" — silently auto-syncing location data would
directly break that promise and undercut the app's actual differentiator against Google
Maps' own favorites feature.

**Resolution:** build it as *automatic once explicitly turned on* — a single clear opt-in
toggle in Settings ("Back up my locations to Google Drive"), off by default, with a visible
"last synced" state. That gets the reliability the artifact doc is right to want, without
the silent-background-upload the app has promised users it won't do. Activity 3.1 below is
written to this resolution.

## Reconciled bottom navigation

Merging both proposals (the artifact doc's map-first IA is the stronger structure; folding
in where Export/Settings actually belong):

| Slot | Destination | Replaces | Why |
|---|---|---|---|
| 1 | **Explore** (map view, saved pins) | Home dashboard | Spatial recall — people remember places by where they are, not by reading a list. |
| 2 | **Quick Save** (FAB, not a nav tab) | Save Current Location | Central, always-reachable capture action instead of a peer destination. |
| 3 | **Saved** (searchable list) | Saved Locations | Precise text search/filter, complements the map. |
| 4 | **Collections** | Export | Trip/theme groupings; Export moves into Settings as a utility action. |
| 5 | **Settings** | Exit | Sync toggle, backup status, dark mode, import/export live here. |

---

## Verification note — Phase 1 core loop + the adjustable-pin request

You'd already built most of Phase 1's structural centerpiece (1.1 + 1.2) yourself: a
map-centric `MainActivity`, a bottom-sheet capture flow, a searchable category field, and —
as a bonus not in the original backlog — Plus Code (Open Location Code) support alongside
the street address. It's solid work; the DB migration (`DATABASE_VERSION` 1→2, `ALTER
TABLE` for the new `plus_code` column) is done correctly, and the JSON export/import,
delete, and search paths were all updated consistently to match.

**On "adjust the pin / multiple pins when I zoom":** this is already implemented, and it's
the right pattern — a fixed reticle at the screen center (`iv_center_pin`) while the map
itself pans underneath it, re-geocoding via `onCameraIdle` as it settles. That's the same
approach Google Maps/Uber use for "confirm your exact location," and it structurally can't
produce the old "multiple pins" symptom, because no `Marker` is ever added for the
in-progress capture — only real saved locations get markers, and `loadSavedLocationsOnMap()`
clears and redraws all of them together rather than appending. The multiple-pins behavior
you were seeing traces back to the *old* `SaveLocation` screen: its location callback fired
every 5-10 seconds and called `mMap.addMarker(...)` on **every** fix without ever clearing
the previous one — so a stack of pins would visibly appear the longer that screen stayed
open (zooming in just made the accumulated cluster obvious). That screen is now fully
unreachable — nothing in the app still navigates to it — so I removed it rather than leave
dead code that reintroduces confusion later (see 1.2 above).

What I did find and fix in the new flow: the address text next to the pin is fetched with a
background geocode on every `onCameraIdle`, and rapid panning/zooming could fire several of
those in a row. They all share one background thread, but nothing guaranteed they'd
*finish* in the order they started — so a slow lookup for an earlier pin position could
occasionally land after a faster one and briefly show the wrong address for where the pin
actually was. (The saved coordinates themselves were never affected - only the on-screen
address text could flicker.) Fixed with a request-generation counter in
`updateCapturedInfo()` that drops any geocode result that isn't for the most recent pin
position.

---

## Phase 0 — Foundation activities (do before the phases below; small, unblocks everything)

- [x] **0.1 Fix search to match name + address + type**, not type alone. *Done — the
      list's `Filter` now matches name and type together (address next if wanted).*
      *Touches:* `SavedLocationListAdapter`'s `Filter`. *Source: mine.*
- [ ] **0.2 Add distance-from-current-location to each row + "Nearest" sort option.**
      *Touches:* `SavedLocationList`, `SavedLocationListAdapter`, `SavedLocationEntity`.
      *Source: mine.*
- [ ] **0.3 Remove "Exit" and duplicate "Export" from the bottom nav** per the reconciled
      table above; keep Export as a Settings/overflow action only.
      *Touches:* `activity_main.xml` + equivalent menu/nav XML in all four Activities,
      `menu_navigation_list.xml`. *Source: both.*
- [ ] **0.4 Snackbar-with-Undo on delete**, replacing (or backing up) the confirm dialog.
      *Touches:* `SavedLocationListAdapter`'s delete `OnClickListener`. *Source: mine.*
- [ ] **0.5 Empty-state screen** ("No locations yet — tap the map to save your first
      spot") instead of a blank list on first run; retire the UI-blocking `ShowcaseView`
      tour in favor of this plus lightweight inline hints.
      *Touches:* `activity_saved_location_list.xml`, `MainActivity`'s showcase logic.
      *Source: both (artifact doc names this explicitly; mine flagged onboarding gaps).*

## Phase 1 — Core loop rework (highest impact, the "double-hop save" + discoverability fixes)

- [x] **1.1 Map-centric home screen ("Explore").** Full-screen map as the landing
      destination, all saved locations rendered as pins, camera centers on them (or on the
      user's last-known location when the list is empty).
      *Done — built directly into `MainActivity` rather than a separate Fragment; no
      clustering yet for dense areas, worth adding once the pin count grows.*
- [x] **1.2 Collapse the save flow into a bottom sheet over the map**, replacing the
      `SaveLocation` → `CompleteSaveLocation` two-screen hop with one sheet that slides up
      without losing map context (name, tags, category-suggestion all in one place).
      *Done — `bottom_sheet_save_location.xml` + the capture logic in `MainActivity`. The
      old `SaveLocation` Activity is now fully unreachable dead code; removed it and its
      layout (moved to `_to_delete_review/` rather than deleted outright — please review
      and delete that folder once you've confirmed you don't need it) and its manifest
      entry, since nothing referenced it anymore. `CompleteSaveLocation` is kept and still
      wired up, now serving the Edit flow from the Saved list.* **This activity also
      directly delivers the "adjustable pin" feature requested separately — see note
      below.**
- [ ] **1.4 Replace the flat 90-item category spinner** with searchable/autocomplete tags,
      pre-seeded by Smart Categorization (1.5) rather than always requiring manual entry.
      *Touches:* `arrays.xml` types list becomes a suggestion source, not a forced enum;
      `SavedLocationEntity.type` stays a string field so this is schema-compatible.
      *Source: both.*
- [~] **1.5 Smart categorization** — partially done: the category field is now a
      searchable autocomplete against the existing type list (no more scrolling a flat
      90-item spinner), but it doesn't yet auto-suggest a category from what's actually at
      that coordinate (would need the Places API, not just `Geocoder`). Name still has to
      be typed. *Source: artifact doc.*
- [ ] **1.6 App shortcuts** (long-press launcher icon → "Save current location") and a
      home-screen widget/quick-settings tile for the same action, for capture without a
      full app open.
      *Touches:* new `AndroidManifest.xml` shortcuts entry + `AppWidgetProvider`.
      *Source: both (artifact doc: shortcuts; mine: widget/tile) — kept as one activity
      since they're the same underlying "capture without opening the app" intent action.*
- [ ] **1.7 Location cache for instant map display**: show last-known location immediately
      while a high-accuracy fix warms up, instead of a blocking spinner.
      *Touches:* `SaveLocation`/new Explore screen's location-acquisition logic; use
      `FusedLocationProviderClient.getLastLocation()` as the first paint, then upgrade.
      *Source: artifact doc.*
- [x] **1.8 Battery-aware location priority** — effectively satisfied: the app only
      requests `PRIORITY_HIGH_ACCURACY` for the single fix taken at the start of a capture
      (`setMaxUpdates(1)`, immediately removed after landing), and uses one-shot
      `getLastLocation()` rather than a continuous stream for browsing the map.
      *Source: artifact doc.*

## Phase 2 — Organization & proactive utility (retention)

- [ ] **2.1 Collections** (user-named groups a location can belong to — "Paris Trip,"
      "Client Sites"), browsable as their own filtered views; replaces the Export bottom-nav
      slot per the reconciled IA.
      *Touches:* new `collections` + join table in `DatabaseHandler`'s schema (DB v2/v3),
      new `CollectionsActivity`/screen. *Source: both.*
- [ ] **2.3 Batch actions** in the Saved list — multi-select → delete / export / add to
      collection.
      *Touches:* `SavedLocationListAdapter` selection-mode support. *Source: mine.*
- [ ] **2.4 Home/Work quick-set locations**, pinned above the regular list/map for one-tap
      access. *Touches:* `SavedLocationEntity` "isPinned"/"role" flag or reuse Collections
      (2.1) with two reserved system collections. *Source: mine.*

## Phase 3 — Ecosystem & trust (bigger investment, real differentiation)

- [ ] **3.1 Opt-in cloud backup/sync** (Google Drive App Data folder as the low-lift first
      step) — **built per the resolution above: off by default, one explicit toggle, visible
      "last synced" status, never silent.** Existing local JSON export/import stays as the
      offline/manual fallback for privacy-first users.
      *Touches:* new sync module, Settings screen, `LocationImportAndExport` extended
      rather than replaced. *Source: both, reconciled.*
- [ ] **3.2 Dynamic sharing / deep links**: share a location as an "SFL link" another
      installed copy of the app can open and one-tap import, instead of a plain text
      Maps URL.
      *Touches:* `LocationImportAndExport`, an `AndroidManifest.xml` deep-link intent
      filter, a small resolver Activity. *Source: both.*
- [ ] **3.3 Shareable collections**: a link/QR code for an entire Collection (2.1), for
      trip itineraries and similar group use cases.
      *Touches:* builds on 3.2's deep-link mechanism. *Source: mine.*
- [ ] **3.4 Contacts integration**: attach a saved place to a contact ("my friend's
      house"). *Touches:* `SavedLocationEntity` optional contact-lookup-key field, contacts
      picker permission + UI. *Source: mine.*

**Re-added to scope (2026-08-29):** 2.4, 3.1, 3.3, and 3.4 above were dropped earlier this
session and have now been put back by explicit request. Not implemented yet. Note: with
Settings now an actual screen (built during the look-and-feel pass, not tracked as its own
numbered activity here), 3.1's "Settings screen" touch-point already exists to build on.

**Removed from scope (2026-08-29):** 1.3 (search-and-save any place), 1.9/1.10
(photo attachment / free-text notes), 2.2 (geofenced proximity reminders), and 3.5
(voice/App Actions shortcut) were cut by explicit request. Don't resurface these without
being asked again.

---

## Suggested execution order

Phase 0 first — it's small, de-risks the rest, and each item stands alone (any one can ship
independently). Within Phase 1, sequence **1.1 → 1.2 → 1.4/1.5** — the map-centric home
and the bottom-sheet capture flow are the structural change everything else in that phase
sits on top of, so building smart categorization against the *old* two-screen flow would
mean redoing that wiring once 1.2 lands. 1.6–1.8 can run in parallel with each other and
with the tail of Phase 1's core flow work.
