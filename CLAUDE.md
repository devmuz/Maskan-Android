# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

Maskan is a property-management app with two roles sharing one Firebase backend:
**landlords** (manage properties, tenants, and bills) and **tenants** (view their
dues and history). This repo is an Android/Kotlin+Compose port of an existing
SwiftUI/iOS app, built against the **same Firebase project** and the **same
Firestore schema** as that iOS app and an older Flutter app.

Full screen-by-screen/feature-by-feature specs (ported from the iOS app) live in
a sibling directory: `../android-port/*.md`. Read `00-overview.md` there first;
`02-data-models.md` documents the Firestore schema and business rules in detail.
When a doc says a field name, enum raw value, or algorithm "must not change",
that's a hard constraint — the schema and the `tenantLogin` Cloud Function are
shared with the other clients and are not this repo's to redesign.

## Commands

Build and install on a connected device/emulator:
```
./gradlew :app:installDebug
```

Run unit tests (`app/src/test`):
```
./gradlew :app:testDebugUnitTest
```
Run a single unit test class:
```
./gradlew :app:testDebugUnitTest --tests "com.maskan.mobileapp.SomeTest"
```

Run instrumented tests (`app/src/androidTest`, needs a connected device/emulator):
```
./gradlew :app:connectedDebugAndroidTest
```

Lint / assemble without installing:
```
./gradlew :app:lint
./gradlew :app:assembleDebug
```

Launching and viewing crashes on a running emulator:
```
adb shell am start -n com.maskan.mobileapp/.MainActivity
adb logcat -d | grep -A 40 "FATAL EXCEPTION"
```

## Architecture

**Package name is `com.maskan.mobileapp`** — this must match `namespace`/
`applicationId` in `app/build.gradle.kts` exactly. If Kotlin sources ever end up
under a different package (e.g. after a copy/paste or a bad rename), the app
still compiles but crashes instantly on launch with
`ClassNotFoundException` / `Unable to instantiate application`, because the
manifest's `android:name=".MainActivity"` / `".MaskanApplication"` resolve
relative to the namespace, not to whatever package the `.kt` files declare.

**Composition root — one repository instance per app process, not per screen.**
The iOS app had a bug class where each screen owned its own Firestore listener,
so switching tabs re-fetched data and screens disagreed on numbers. This is
deliberately avoided here:
- `di/AppContainer.kt` is a manual DI container instantiated once in
  `MaskanApplication.onCreate()`, holding the Firebase SDK instances and all
  four repositories (`AuthRepository`, `PropertyRepository`, `TenantRepository`,
  `BillingRepository`) plus `RolePreferences`.
- It's exposed down the Compose tree via `di/LocalAppContainer.kt`
  (a `CompositionLocal`) from `MainActivity`.
- Each repository exposes Firestore snapshot-listener data as a `StateFlow`,
  with `startListening(scope, ...)` / `stopListening()` — listeners are started
  once and shared, never per-screen.
- `LandlordViewModel` (`ui/landlord/LandlordViewModel.kt`) is created once per
  landlord session at the `LandlordShellScreen` level (survives tab switches,
  cleared on sign-out) and is what actually calls `startListening` on the three
  landlord-facing repositories. Every landlord screen takes this same
  `LandlordViewModel` instance rather than creating its own. `TenantSessionViewModel`
  plays the equivalent role for the tenant shell.
- Don't add a Firestore listener directly inside a screen composable — add it to
  the relevant repository and consume it through the shared ViewModel.

**Navigation is two-level.** `ui/nav/MaskanNavHost.kt` is the outer graph:
splash (decides landlord shell vs. tenant shell vs. role-select by reading
`AuthRepository.currentUser` + `RolePreferences.roleFlow`) → role select → auth
screens → `LandlordShellScreen` / `TenantShellScreen`. Each shell screen then
owns its *own* inner `NavHost` for its bottom-tab destinations
(`LandlordShellScreen` → Dashboard/Properties/Tenants/Bills/Settings tabs, plus
detail/add/edit routes nested under it).

**Role is separate from Firebase Auth.** A tenant's Firebase Auth user carries
no custom claim distinguishing role, so `"landlord"` / `"tenant"` is persisted
locally via DataStore in `data/prefs/RolePreferences.kt`, independent of the
Firebase Auth session. `RolePreferences` also caches the tenant's
`propertyIdCode` after login, since nothing in Firestore maps a tenant's
Firebase Auth UID back to a `tenants` doc otherwise.

**Auth is two different mechanisms depending on role** (`data/repository/AuthRepository.kt`):
- Landlords: standard Firebase Auth email/password. Their Firebase Auth UID
  *is* `landlordId` throughout the schema.
- Tenants: no Firebase Auth account up front. Login calls the existing callable
  Cloud Function `tenantLogin` with `{propertyId, password}`; the function
  verifies the password against a stored salt+hash and returns a custom token,
  which the client exchanges via `signInWithCustomToken`. Tenant passwords are
  never checked client-side — see `02-data-models.md` for the exact salted-hash
  scheme if you ever need to regenerate credentials.

**Firestore schema constraints worth knowing before touching a repository:**
- `properties`: one doc per rentable flat/unit, not per building. Flats in the
  same building are separate docs that share `buildingName` + `address`;
  grouping is done client-side (see `PropertyGrouping.kt`), not via a Firestore
  query.
- Creating a property writes the `properties` doc and a `propertyIdCodes`
  registry doc (used only to enforce code uniqueness) in the **same batch** —
  see `PropertyRepository.addProperty`. Property ID code generation has a fixed
  algorithm documented in `02-data-models.md`; match it exactly if generating
  codes anywhere else.
- Photo upload is two-phase: the property doc is created with `photoUrl = null`
  immediately, then a compressed image is uploaded to Storage and every flat
  sharing the building gets `photoUrl` patched in afterward
  (`PropertyRepository.uploadPhoto`).
- `bills.status == "overdue"` is not reliable as "is this late" — it only
  flips when a landlord manually taps "Mark as Overdue". Compute lateness from
  `dueDate < today && status != "paid"` instead; this was a real bug on iOS.
- Occupied/vacant state (`properties.occupied`) is only ever mutated by
  assign/move-out/delete flows, never set directly by UI code.

## Firebase project config

`app/google-services.json` is checked in and points at the shared Firebase
project used by iOS/Flutter/Android alike — don't regenerate it from a
different Firebase project. It's a real config file, not a template.
