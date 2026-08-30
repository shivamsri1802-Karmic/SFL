# SFL — UX & Product Analysis

Prepared as a UX/PM review of the current app, based on its actual screens, flows and data
model (not a generic template). Goal: identify where the current journey creates friction,
and what would turn SFL from "a place to store GPS pins" into a product people keep coming
back to.

## 1. What the product is today, in one line

A private, offline, single-device notebook for GPS coordinates: capture where you're
standing right now, name it, tag it with a Google-Places-style category, and look it up
later. That's a real, honest niche — but every step in the journey currently assumes the
user is *physically at* the place they want to save, and every safeguard against data loss
is a manual "export to a JSON file and share it somewhere" action. Those two constraints
are the biggest ceiling on how useful this can become.

## 2. Current user journey, as it exists in the code today

1. **Launch** → `MainActivity`. Bottom nav: Home / Save current location / Saved locations
   / Export / Exit. First run shows a 3-step tooltip tour (Favorites → Save current
   location → Export).
2. **Capture a place** → tap "Save Current Location." App checks GPS is on (else redirects
   to system settings with a Toast), requests location + reverse-geocodes via `Geocoder`,
   shows a progress bar until a fix lands, then a live map + address.
3. **Name it** → tap "Save," land on `CompleteSaveLocation`: type a name, pick one category
   from a flat, alphabetically-sorted, ~90-item spinner (`Airport` … `Zoo`), confirm.
4. **Find it again** → `SavedLocationList`: reverse-chronological list, a search box that
   filters by **category substring only**, and per-row Map / Share / Edit / Delete icons.
5. **Back it up** → Export writes a JSON file and opens the Android share sheet; Import
   picks a JSON file from storage and re-populates the local SQLite database.

That's the entire product. Nothing else exists yet — no notes, no photos, no collections,
no reminders, no sync.

## 3. Where the journey breaks down

Ranked by how much everyday friction or lost value each one causes, not by how hard it is
to fix.

**You can only save where you're standing.** There is no "search for an address or place
and save it" path — no Places Autocomplete, no drop-a-pin-on-the-map option. If a friend
texts you an address, or you're planning a trip and want to pre-save a hotel, you cannot do
it in this app at all. For a "save favorite location" app, this is the single biggest gap:
it turns the app into "log where I've been" instead of "keep a book of places that matter
to me," which is a much smaller use case.

**Search only matches the category, not the name or address.** Open `SavedLocationListAdapter`'s
filter and it checks `item.getType().toLowerCase().contains(filterPattern)` — nothing
else. A user who remembers they saved "Grandma's House" cannot type "grandma" and find it;
they'd have to remember they filed it under "Home Goods Store" or whatever category they
picked. This is very likely the single most-hit dead end in the app today, and it's a
one-field fix.

**The category picker fights the user.** Ninety flat, alphabetical, Google-Places-style
options (`Roofing Contractor`, `Locksmith`, `Embassy`) is built for classifying commercial
venues, not for organizing a personal list of "places I care about." Most saves will get
crammed into a handful of categories (`Restaurant`, `Store`, `Park`) or a mismatched one
out of picker fatigue, which then makes the (already narrow) search even less useful.

**Data lives in exactly one place.** There's no cloud backup and no multi-device story —
just local SQLite plus a manual "export a JSON file, then remember to import it on your
next phone" ritual. For an app whose entire value is a growing personal archive, an
uninstall, a factory reset, or a lost phone means the archive is gone. This is the kind of
risk that quietly kills retention: people stop adding "favorites" to something they don't
trust to keep them.

**The core action isn't reachable fast enough.** The whole pitch of a location-saving app
is "capture this before I forget" — but doing that today means: unlock phone → open app →
wait for cold start → tap Save Current Location → wait for GPS fix → tap Save → fill in a
name and wade through the category spinner. There's no home-screen widget, no quick-tile,
no notification action, no voice/Assistant shortcut. The moment of intent (standing
somewhere worth remembering) and the moment of capture are separated by too many taps.

**The list has no sense of "where am I relative to my places."** Everything is sorted
purely by recency. There's no distance-from-current-location, no "nearby" view, no
sort-by-name. For a location app, "what's close to me right now" is one of the most
naturally useful queries, and it doesn't exist.

**Information architecture mixes navigation with one-off actions.** The bottom nav —
normally reserved for top-level destinations a user revisits — includes "Export" and
"Exit." Export is a rare, occasional action (it belongs in an overflow/settings menu, which
it *also* already lives in, so it's duplicated); "Exit" as a nav destination is unusual on
Android, where the back button/gesture already does this, and it eats a slot that could
hold something people actually want at a glance — like a category/nearby view.

**No undo, no forgiveness.** Delete has a confirmation dialog, which is good, but a
confirmation dialog is still a wall people click through habitually. A "Snackbar with Undo"
pattern is both friendlier and, paired with the destructive-action caution most users have
around data they can't get back, more trustworthy.

**Editing is capture-only, not correction-only.** GPS fixes are often 5–20 meters off,
especially indoors, but there's no way to nudge the pin on the map before saving — you get
exactly the coordinate the phone reported.

## 4. A redesigned journey (to-be)

1. **Capture, from anywhere.** *(The "Search & save a place" entry point was dropped from
   scope — see §5. "Save where I am," today's flow, stays the only capture entry point.)*
2. **Fast capture path shortened.** A home-screen widget/quick-tile that captures GPS +
   reverse-geocodes in the background and drops straight into the naming screen — skipping
   the "open app, wait, tap" sequence entirely for the common case.
3. **Naming step becomes lighter and richer at once.** Free-text tags/collections instead
   of (or alongside) the 90-item spinner — type-ahead against past tags so the list doesn't
   sprawl. Category becomes a suggestion, not a forced single choice from a huge fixed list.
4. **Finding a place becomes a real search.** One search box matches name, address, and
   tags together, plus a toggle to sort "Nearest to me" vs. "Recently added" vs. "A–Z."
   Empty search shows a "Nearby" section by default — turning the list from a log into
   something you'd actually open when you need a place, not just when you're filing one
   away.
5. **Trust the archive.** Backup becomes automatic and quiet (opt-in cloud sync — see
   §5) with the existing JSON export kept as a manual/offline fallback for the
   privacy-first users this app already attracts. A visible "last backed up" state replaces
   the current all-or-nothing manual export ritual.
6. **Act on a saved place, not just view it.** From a row: Directions (already present),
   Share, Edit — plus new: for a place tied to a contact, "Call/Message." *(The geofenced
   "Remind me when I'm nearby" action was dropped from scope — see §5.)*

## 5. Feature roadmap

Grouped by how soon they're worth tackling, weighing effort against how directly each one
addresses the friction above. This assumes the small-team reality of a project like this —
"Now" is scoped to be shippable without new backend infrastructure.

**Now (highest impact, lowest lift — mostly UI/logic changes to what already exists)**
- Fix search to match name + address + type together, not type alone.
- Replace the flat 90-item spinner with a searchable/autocomplete picker, or free-text tags
  with autocomplete against previously-used values.
- Add "distance from current location" to each row and a Nearest-first sort option.
- Move Export out of the bottom nav (keep it in the overflow menu only); replace the
  "Exit" nav slot with something people will actually tap repeatedly, e.g. a "Nearby" or
  "Categories" view.
- Delete via Snackbar-with-Undo instead of (or alongside) the confirm dialog.
- Let the user drag the marker to correct the pin before confirming a save.
- Empty states with a clear call to action (first-run "Save your first location" instead
  of a blank list).

**Next (real feature work, still single-device-friendly)**
- Home-screen widget / quick settings tile for one-tap capture.
- Collections/lists (e.g., "Weekend trip," "Restaurants to try") that a location can belong
  to, browsable as their own filtered views — this is likely the single feature most
  likely to turn an occasional user into a regular one, since it gives people a reason to
  keep organizing rather than just accumulating.
- Batch actions in the list (multi-select → delete/export/move to collection).

**Later (bigger investment, but where the product's real differentiation lives)**
- Opt-in encrypted cloud backup/sync (Google Drive App Data folder is a good low-lift
  first step — no custom backend needed — before considering a full account system).
  Framed and built as strictly opt-in, so the app keeps the "we don't upload your data
  without your intervention" promise it already makes in its own strings.xml — this is a
  real trust asset worth protecting, not just a nice line of copy.
- Shareable collections — a link or QR code a user can send so someone else can view (and
  optionally import) a curated list, e.g. a trip itinerary.
- Contacts integration (attach a saved place to a person).
- Home/Work quick-set, the way most navigation apps let you pin two special places.

**Removed from scope (2026-08-29):** Photo/notes on a location, Places search-and-save,
Geofenced reminders, and Voice/App Actions shortcut were cut by explicit request. Don't
resurface these without being asked again.

**Re-added to scope (2026-08-29, earlier this session):** opt-in cloud backup/sync,
shareable collections (link/QR), contacts integration, and Home/Work quick-set were dropped
then put back by explicit request. Not implemented yet.

## 6. Design principles worth protecting while building the above

- **Privacy is the brand, not a footnote.** The app already tells users it doesn't upload
  or track them in the background. Any sync/cloud feature should be opt-in, clearly
  explained, and ideally end-to-end encrypted — a location history is unusually sensitive
  data, and "off by default" is both the right call and the app's actual competitive edge
  against Google Maps' own favorites feature, which assumes an always-signed-in account.
- **Capture friction is the enemy.** Every feature added to the naming/save step (tags,
  photos, notes) should be optional and skippable in one tap — the fast path ("just save
  it, I'll fill in details later") has to stay fast, or people stop using the app at the
  exact moment it's supposed to be most useful.
- **Personal categorization, not commercial categorization.** The Places-style type list
  models venues from a business's point of view. A personal archive is organized by
  relationship to the user ("my places," "trip 2026," "want to go") more often than by
  venue type — lean the taxonomy that direction.

## 7. Metrics worth watching once these ship

- **Time-to-capture**: taps/seconds from intent ("I should save this") to a saved entry —
  the home-screen widget should move this down significantly.
- **Search success rate**: searches that lead to opening a result vs. searches with zero
  matches — today's type-only search almost certainly has a bad number here.
- **D7/D30 return rate** and **saves per active user per month** — the two numbers most
  sensitive to whether collections/tags give people a reason to organize, not just dump,
  locations.
- **Backup adoption rate**, once opt-in sync ships — a proxy for how much people trust the
  app with data they don't want to lose.
