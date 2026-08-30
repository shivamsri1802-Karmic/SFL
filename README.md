# SFL (Save & Find Location) — Reconstructed Android Studio Project

This project was reconstructed by decompiling `SFL.apk` (package `com.shivam.sfl`)
with **jadx**. It's meant as a starting point for inspecting or continuing work
on the app — please read the caveats below before you build or ship it.

## What's here
- `app/src/main/java/com/shivam/sfl/` — the app's own 9 source files
  (decompiled from `classes.dex`/`classes2.dex`). `R.java` and `BuildConfig.java`
  were removed since Gradle regenerates those automatically.
- `app/src/main/res/` — resources recovered from the compiled `resources.arsc`.
- `app/src/main/AndroidManifest.xml` — cleaned up (removed build-injected
  entries like `appComponentFactory` and the merged-in `GoogleApiActivity`,
  which Gradle/manifest-merger will re-add automatically from the Play
  Services dependency).
- `build.gradle` / `settings.gradle` — dependencies inferred from the actual
  `import` statements in the source (AndroidX AppCompat/RecyclerView/Material,
  Play Services Maps + Location, and the `ShowcaseView` tutorial-highlight
  library via JitPack).

## Important caveats (please read)

1. **Decompiled code, not original source.** jadx reconstructs readable
   Java from bytecode, but variable/class names surviving obfuscation,
   exact original formatting, and comments are gone. Logic should be
   equivalent, but treat this as a reverse-engineered approximation.

2. **Resources are merged, not split by library.** A compiled APK's
   `resources.arsc` bakes every dependency's resources (AppCompat's `abc_*`
   layouts, Material's `mtrl_*`/`design_*` files, etc.) into one table
   alongside the app's own. I left the full recovered set in `res/` rather
   than guessing which entries to delete — Android's build system lets a
   module's own resources silently take precedence over a library's
   same-named resources, so this shouldn't cause build failures, just some
   bloat/duplication versus a hand-authored project.

3. **I could not run an actual Gradle build to verify compilation.** My
   sandbox doesn't have network access to Google's Maven repo or JitPack, so
   this project has *not* been build-tested. Open it in Android Studio and
   let it sync — you'll likely need to resolve small things like the exact
   available versions of the AndroidX/Play Services libraries, or the AGP/
   Gradle version compatibility with your local Android Studio.

4. **⚠️ Exposed Google Maps API key.** The manifest contains a live-looking
   Maps API key (`AIzaSyCmB9...`) that was hard-coded in the original APK.
   Treat it as compromised — anyone can extract it the same way I just did.
   Rotate/regenerate it in the Google Cloud Console and restrict the new key
   (by package name + SHA-1 fingerprint) before using or redistributing this
   project.

5. **Third-party library.** `MainActivity` uses `com.github.amlcurran:ShowcaseView`
   for onboarding tooltips, pulled from JitPack — confirm the version/license
   still suits your needs.

## Getting it running
1. Open the project root in Android Studio (it'll offer to generate the
   Gradle wrapper jar if missing — accept it, since I couldn't fetch the
   wrapper binary in my sandbox).
2. Let Gradle sync and resolve dependencies.
3. Fix any resource-duplication or version warnings Android Studio surfaces.
4. Rotate the Maps API key (see above) before running against real Maps
   quota.
