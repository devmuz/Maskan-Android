# Settings Feature — Android Parity Spec

Reference spec for the Settings/Profile screens (landlord and tenant), subscription/paywall, legal screens, and account management — reverse-engineered from the iOS app (Maskan-iOS) so the Android app can implement full feature parity. Every field, action, validation rule, and business rule below exists in the shipped iOS app — nothing here is speculative.

Source of truth: iOS codebase at time of writing (branch `development`). Companion docs: `ANDROID_PROPERTIES_FEATURE_SPEC.md`, `ANDROID_TENANTS_FEATURE_SPEC.md`, `ANDROID_BILLS_FEATURE_SPEC.md`.

> **Important note on source files:** the repository contains **two parallel implementations** of Settings/legal screens. A set under `Views/Common/` (`SettingsView.swift`, plus `AboutView`/`PrivacyPolicyView`/`TermsOfServiceView`) is an unwired **reference implementation** — it is instantiated nowhere in the live app except its own preview, and would crash if wired up as-is (it pushes a tenant-only screen for landlord users). The **real, live screens** are `LandlordProfileView` (landlord) and `TenantProfileView` (tenant). **This spec is built entirely from the live screens** — build Android from this, not from the dead reference files. The native Privacy Policy / Terms of Service content itself (fully detailed in §11) is real, authored copy and should still be ported even though its host screen is currently unwired on iOS.

---

## 1. Settings Screen — Landlord

Reached via the "Settings" tab (5th tab in the landlord's tab bar). Title: "Settings".

**Profile header**: avatar circle (generic person icon, no photo upload capability), displays the landlord's **email only** — no name or phone field exists or is editable anywhere in landlord Settings.

**APPEARANCE section**
- "Theme" — segmented control: **System / Light / Dark**. Stored as a **device-local preference** (not synced to the account/Firestore) — shared storage key with the tenant side, so switching devices or reinstalling resets to "System".

**PREFERENCES section**
- "Currency" row → navigates to a **currency picker** screen: a flat list of all 17 supported currencies (§3), checkmark next to the current selection. Selecting one persists it to the landlord's account **and** batch-updates the `currency` field on every property the landlord primarily owns (so tenants — who can't read the landlord's own profile — still see the correct currency via their property doc; see the Properties spec §9). On failure: "Couldn't Update Currency" error dialog.

**NOTIFICATIONS section**
- "Push Notifications" toggle — this is **not a persisted preference flag**; it directly mirrors the OS-level notification permission state, refreshed each time the screen appears.
  - Turning ON requests OS notification permission (alert/badge/sound); if granted, registers for remote notifications.
  - Turning OFF, or if the OS permission is denied, shows a "Notifications Blocked" dialog with an "Open Settings" action that deep-links to the OS app-settings page for this app, plus "Cancel".
  - **There is no granular per-category notification preference** (e.g., "bills only" vs "maintenance only") — a single on/off switch only.

**SUBSCRIPTION section** (full pricing/gating detail in §4–§5)
- Crown icon (gold if Pro, gray if Free), label "Maskan Pro" / "Free Plan", subtitle:
  - Pro: "All features unlocked" + green "Active" badge.
  - Free: "1 property · 1 co-owner per building".
- **Pro**: "Manage Subscription" row → opens the platform's native subscription-management surface (Play Store subscriptions page, for Android). **There is no in-app cancel/change-plan flow** — subscription management is always delegated to the store.
- **Free**: "Upgrade to Pro" row (visually highlighted) → opens the Paywall screen.

**ABOUT section**
- "Contact Support" → opens an email composer addressed to the support inbox.
- "Rate Maskan" → opens the app's store listing review flow.
- "Privacy Policy" → navigates to the **native, in-app** Privacy Policy screen (not a web link — see §11).
- "Version" row (non-interactive) — shows the app's version number (no build number on this particular row).
- **No "Terms of Service" link and no "About" screen link exist on this live screen** — both `TermsOfServiceView` and `AboutView` exist as native screens in the codebase but are currently unreachable from any live navigation path on iOS. Flag to product whether Android should surface them (recommended — see §15) even though iOS currently doesn't.

**Log Out** — full-width destructive row below ABOUT → confirmation dialog ("Log Out? / You will be signed out of your account.") → "Log Out" (destructive) / "Cancel". Confirmed action signs out of the auth session and clears the locally-stored role, returning to role selection.

**Version footer** — small "Maskan {version}" text at the very bottom of the screen (a duplicate, shorter presentation of the Version row above).

**No account-deletion UI exists anywhere on this screen** (see §6). **No in-Settings change-password option for landlords** — landlord password reset only exists as a **pre-login** "Forgot Password" flow from the sign-in screen (email entered, a reset-link email is sent by the auth backend; the confirmation copy is intentionally non-committal — "If an account exists for {email}, a reset link is on its way." — regardless of whether the account exists, to avoid leaking account existence). **No data-export feature** exists despite the Privacy Policy text claiming a right to export data — that claim is currently unbacked by any UI or API. **No co-owner management appears in Settings** — co-owner invites/limits live on the property/building screens, not here.

---

## 2. Settings Screen — Tenant

Reached via a gear icon on the tenant's Dashboard. Title: "Settings".

**Profile header** — avatar circle showing the tenant's **name-initial letter** (not a generic icon), with the tenant's **name and contact** displayed below. This is the only Settings screen (of the two) that shows a name — landlords' Settings only shows their email.

**PROPERTY section** (tenant-only, no landlord equivalent) — read-only info rows: Property ID, Move-in Date, and (only if set) "Rent Due — Day N of each month". No edit capability on any of these.

**APPEARANCE section** — identical System/Light/Dark theme picker, same shared device-local preference key as the landlord screen.

**NOTIFICATIONS section** — identical push-notification permission toggle/dialog pattern as the landlord screen.

**SECURITY section** (tenant-only, no landlord equivalent) — "Change Password" row → dedicated Change Password screen (full breakdown in §8).

**ABOUT section**
- "Rate Maskan" → same store-review deep link pattern as landlord.
- "Privacy Policy" → **opens a web view of an external URL** (not the native screen). **This is inconsistent with the landlord screen**, which uses the native in-app Privacy Policy screen. This is a real inconsistency in the source app — recommend Android **unify on one native, in-app legal-content pattern for both roles** rather than replicate this split (see §11 for the full native content to reuse for both).
- "Version" row — app version only (no build number).
- **No Terms of Service link exists for tenants at all.**
- **No currency setting for tenants** — currency is entirely read-only, inherited from the landlord's property record; there is no UI anywhere in tenant Settings to view or change it.
- **No subscription/Pro section** — Pro is a landlord-only concept; tenants never see subscription UI.

**Log Out** — identical destructive-confirm pattern and behavior as the landlord screen.

**Version footer** — identical short "Maskan {version}" text.

**No account-deletion UI** exists here either.

---

## 3. Profile / Currency Data

### 3.1 Landlord profile/currency state
- A single published field: **currency code** (default `"USD"`), backed by a realtime listener on the landlord's account document's `currency` field. **No name, phone, or photo field exists anywhere in this profile service** — those simply aren't modeled for landlords.
- **Setting currency** writes the new code to the landlord's account doc **and** batch-updates the `currency` field on every property the landlord **primarily owns** (not properties they merely co-own) — this is how tenants, who can't read the landlord's own account document per security rules, still see the correct currency (stamped on the property they're linked to).
- The initial currency value is seeded **once**, at landlord account creation, by best-guessing from the device's locale (falling back to USD if that fails) — it never overwrites a currency the landlord has since explicitly chosen.

### 3.2 Supported currencies (exactly these 17 — mirror precisely, no more, no fewer)
| Code | Symbol | Code | Symbol | Code | Symbol |
|---|---|---|---|---|---|
| USD | $ | QAR | ر.ق | BDT | ৳ |
| EUR | € | KWD | د.ك | TRY | ₺ |
| GBP | £ | OMR | ر.ع. | IDR | Rp |
| AED | د.إ | MYR | RM | | |
| SAR | ر.س | NGN | ₦ | | |
| PKR | ₨ | | | | |
| INR | ₹ | | | | |
| EGP | ج.م | | | | |

Display format everywhere an amount is shown app-wide: **`"{CODE} {whole-number, grouped amount}"`** (e.g. `"INR 1,200"`) — always the ISO code prefix, **never** the currency symbol, despite the symbol being modeled and available.

### 3.3 Tenant profile data
There is no separate "tenant profile service" — tenant profile fields (name, contact, Property ID code, move-in date, rent-due day, password hash/salt) live directly on the `Tenant` record itself (see the Tenants spec §1) and are exposed to the tenant's own session as part of that same live document. There is **no tenant-editable name/contact/photo/currency field anywhere** — only the landlord can change a tenant's name/contact (via Edit Tenant).

---

## 4. Subscription / Pro (Purchase Service)

### 4.1 Product identifiers (SKUs)
- Monthly: `com.maskan.mobileapp.pro.monthly`
- Yearly: `com.maskan.mobileapp.pro.yearly`

**Android must define equivalent Play Billing product IDs** and coordinate naming with backend/product — these exact string IDs are iOS-App-Store-specific and should not be copied verbatim onto Android's Play Console configuration, but the monthly/yearly *tiering* and *feature gating* must match exactly.

### 4.2 Free-tier limits (hard constants — must match exactly across platforms)
- **1** free property (building) — the oldest building a landlord ever created (by earliest flat `createdAt`) stays fully usable forever; anything created after that is locked whenever the landlord isn't currently Pro. Full algorithm in the Properties spec §8.
- **1** free co-owner per building.

### 4.3 Pro status resolution — priority order (must reproduce exactly)
1. **Admin override** — a boolean flag on the landlord's account document, checked first (fastest path, no store call needed). Grants Pro indefinitely with no expiry, purely an ops/support tool (set via backend console access, not any in-app UI).
2. **Active native store entitlement** — checked directly against the platform's billing library (StoreKit on iOS, Play Billing on Android); authoritative for whichever platform confirms it. On a successful check, the resulting **expiry date** and a **source tag** (identifying which platform confirmed the purchase — iOS writes `"ios"`) are written back to the landlord's account document, specifically so **the other platform can cross-read it**.
3. **Fallback: a stored expiry-date field in the future** — this is exactly how a purchase made on the *other* platform (e.g., Android reading an iOS-confirmed purchase, or the reverse) gets honored without either platform needing to talk to the other's billing API directly.

**Android must write its own purchase confirmations back to this same shared schema** (expiry timestamp + a source tag of `"android"`) so iOS can cross-read Android purchases exactly as this priority chain expects. This cross-platform sync contract is the single most important piece of subscription-related parity — getting the field names/types wrong breaks entitlement recognition on both platforms, not just one.

### 4.4 Real-time status updates
Pro status is kept live via a **realtime listener** on the landlord's own account document — an admin override toggle, or a cross-platform purchase confirmation, is reflected **instantly, without requiring an app restart**. Android must implement the equivalent (a Firestore snapshot listener or push-driven refresh), not a polling/manual-refresh-only approach.

### 4.5 Purchase / Restore actions
- **Purchase**: initiates the platform billing flow for the selected product; on success, re-runs the full Pro-status resolution and syncs the result back to the shared document.
- **Restore Purchases**: re-syncs the store's known entitlements (`AppStore.sync()`-equivalent on iOS; Android should use Play Billing's `queryPurchases`) then re-runs status resolution. Presented as a distinct, always-visible action on the Paywall screen (§5), separate from the purchase buttons.
- Purchase errors surface as a dedicated error dialog; user-cancelled and pending-approval outcomes are treated as no-ops (no error shown).

---

## 5. Paywall Screen

Presented as a modal sheet, reachable from: the landlord Settings "Upgrade to Pro" row, and every paywall-gated action elsewhere in the app (see the Properties/Tenants/Bills specs — Add Property beyond the free limit, Add Flat/Edit/Add Co-owner on a locked building, Record Payment/Edit on a locked tenant, Add Bill against a locked property, uploading a Document on a locked property).

- **Dismiss**: a close (X) button, top-right.
- **Hero section**: brand-gradient banner, crown icon, title "Maskan Pro", subtitle "Manage unlimited properties and tenants with no restrictions."
- **Feature comparison table** ("FEATURES", Free vs. Pro columns), exactly 5 rows:

| Feature | Free | Pro |
|---|---|---|
| Properties | 1 building | Unlimited |
| Co-owners | 1 per building | Up to 7 |
| Tenants | Unlimited | Unlimited |
| Bills & Payments | ✓ | ✓ |
| Notifications | ✓ | ✓ |

(Pro-column cells for "Unlimited"/"Up to 7" are visually emphasized/highlighted.)

- **Plan picker** — two selectable rows, **Annual pre-selected by default**:
  - **Annual**: live store price (fallback display if the store hasn't loaded pricing yet: **≈ $19.99/yr**), badge "Best Value", detail text "Billed yearly · save ~44%".
  - **Monthly**: live store price (fallback: **≈ $2.99/mo**), no badge, detail text "Billed monthly · cancel anytime".
  - The "~44% savings" framing implies the reference pricing intent is **$2.99/month, $19.99/year** (≈ $1.67/mo effective on the annual plan vs. $2.99 × 12 = $35.88 monthly-equivalent). **Android's Play Billing SKUs should target this same price point**, subject to Play's own per-region localization — exact displayed prices will vary by store/region on both platforms, that's expected and fine; the *relationship* between the two tiers (roughly 44% cheaper effective monthly rate on annual) is what must be preserved.
- **Call-to-action section**:
  - "Subscribe Now" — primary button, initiates purchase of the selected plan; shows a loading spinner and disables itself while a purchase is processing; the sheet auto-dismisses if Pro status becomes true afterward.
  - "Restore Purchases" — secondary text-button action.
  - Fine print: "Subscription auto-renews. Cancel anytime in [Settings › Account › Subscriptions]." (platform-appropriate phrasing — iOS says "Settings › Apple ID › Subscriptions"; Android should say the Play Store equivalent). **No tappable Terms/Privacy links exist inside the paywall itself** — only this fine-print sentence.
  - Purchase errors surface as a dedicated error dialog.

---

## 6. Account Deletion — Does Not Exist (Gap, Not a Missing Android Feature)

There is **no account-deletion flow anywhere in the iOS app** — no UI, no backend method, nothing beyond prose in the Privacy Policy claiming "When you delete your account, we will delete or anonymize your personal information" and "You may request deletion of your data" as a listed right. In practice this presumably means a user must **email support** to request deletion manually — there's no explicit in-app path even to initiate that request.

**This is a compliance gap on iOS itself**, not a feature Android is missing relative to iOS. Both platforms' respective app stores (Apple App Store Review Guideline 5.1.1(v), and Google Play's Account Deletion policy) generally require an in-app account-deletion path for apps with account creation. **Flag this to the product team as a cross-platform requirement**, not an Android-specific parity task — building it only on Android without iOS having it would leave the products inconsistent, and the underlying backend deletion/anonymization logic likely needs to be shared regardless of which platform ships the entry point first.

---

## 7. Sign Out / Logout Flow

Identical pattern on both landlord and tenant Settings screens:
- A single destructive-styled "Log Out" row/button.
- Tapping shows a confirmation dialog: **"Log Out?"** / **"You will be signed out of your account."**, with "Log Out" (destructive) and "Cancel".
- Confirmed action: signs out of the authentication session, clears the locally-persisted role (landlord vs. tenant), and returns to role selection / login.
- **No "sign out of all devices" option.** **No explicit push-token cleanup on sign-out** — the device's push token remains cached locally and on the account document even after logout on iOS (a known staleness gap worth flagging, though it isn't necessarily something Android must replicate — recommend Android actually clear its push token on sign-out to avoid the same bug, and raise it as a fix candidate for iOS too).

---

## 8. Change Password Flow

**This is asymmetric by design between the two roles — do not build one shared flow.**

### 8.1 Landlord — pre-login only, no in-Settings option
Accessible only from the **login screen** ("Forgot Password"), never from within Settings while authenticated. Flow: enter email → triggers the auth backend's standard password-reset email flow. The confirmation UI is intentionally **generic regardless of outcome** — "If an account exists for {email}, a reset link is on its way." — this is a deliberate privacy choice to avoid revealing whether a given email has an account; replicate this non-committal messaging exactly, don't reveal existence either way.

### 8.2 Tenant — authenticated, in-Settings
Reached from Settings → SECURITY → "Change Password". Fields: **Current Password**, **New Password**, **Confirm New Password** (all masked).

**Validation rules**:
- New password must be **at least 6 characters**.
- New password must **equal** the confirm field.
- New password must **differ** from the current password.
- Save is disabled until all three fields are non-empty and no validation error is present; validation hints appear live as the user types the new/confirm fields.

**Submission flow**:
1. **Client-side check first**: the entered "current" password is locally re-hashed (same salted-hash scheme as login) and compared against the tenant's stored hash — **before any network call**. Mismatch throws "Current password is incorrect." immediately, with no round trip.
2. If the local check passes, a **new salt and hash are generated locally**, and the new credentials are sent to a **server-side function** (not a direct Firestore write) — because a tenant's own auth UID does not satisfy the security-rule requirement that only the owning landlord's UID may write to a tenant document. Android must replicate this server-mediated write, not attempt a direct database write from the tenant's own client credentials.
3. On success: confirmation dialog ("Password Changed" / "Your password has been updated successfully.") → dismiss back to Settings.

---

## 9. Notification Settings

Both Settings screens expose **exactly one control**: a single Push Notifications on/off toggle that mirrors the OS-level notification permission — it is **not** a true persisted app preference and has **no per-category granularity** (no separate toggle for "bill reminders" vs. "maintenance requests", etc.). Denying/blocking at the OS level surfaces the same "Notifications Blocked" dialog with a deep link to the OS's app-settings page, on both roles.

**Known infrastructure gap worth flagging (not a Settings-screen issue specifically, but relevant context)**: on iOS, the push token is only ever written to the **landlord's** account document — **tenants' push tokens are never persisted anywhere**, meaning tenants likely cannot currently receive any targeted push notification at all, regardless of what this toggle shows. Android should decide with the backend/product team whether to fix this gap (tenants having working push) rather than silently replicate the same limitation — this is a functional gap, not an intentional design choice worth preserving for parity's sake.

---

## 10. Theme / Appearance Settings

- A single **System / Light / Dark** 3-way choice, stored as a **device-local preference** (not synced to the account) under one shared preference key used identically by both the landlord and tenant Settings screens and applied at the app's root view.
- Because it's device-local, reinstalling the app or switching to a second device resets the preference back to "System" — this is expected/intended behavior, not a bug to "fix" by syncing it to the account on Android.

---

## 11. Legal Screens — Content

### 11.1 Privacy Policy (native content — port this text)
Sections: Introduction; Information We Collect (Personal info, Tenant-specific info, Automatically-collected info — explicitly mentions Google Sign-In profile data, push notification tokens, and crash reports); How We Use Your Information; Data Storage & Retention (names the cloud backend used — Firestore/Cloud Storage — and repeats the account-deletion promise noted in §6); Information Sharing (explicitly calls out that **co-owners on a shared property can see that property's tenant and billing data** — an important disclosure to keep, since co-owner data-sharing is a real feature); Your Privacy Rights (access / correct / delete / object / **export** / withdraw consent — note the "export" right is **not backed by any actual feature**, same gap noted in §1); Security; Children's Privacy (service not intended for users under 13); Changes to This Policy; Contact Us.

### 11.2 Terms of Service (native content — port this text)
Sections: Acceptance of Terms; Service Description (explicit "we are a platform, not a party to the landlord-tenant relationship" disclaimer); User Accounts (18+ age requirement, account-holder responsibility for their own credentials/actions); Landlord Obligations; Tenant Obligations; Payments & Billing (subscription purchases are **non-refundable except where required by law**, and are processed entirely through the relevant app store's in-app-purchase system — not a custom payment processor); Intellectual Property; Prohibited Activities; Disclaimer of Warranties; Limitation of Liability; Termination; Governing Law; Changes to Terms; Contact Us.

### 11.3 About screen (native content, currently unreachable on iOS — still worth porting)
App branding block ("Maskan", tagline "Property Management Made Simple"); a Legal section linking to the native Privacy Policy and Terms of Service screens; a Support section (Contact Support, Rate the app, and a "Help & FAQs" entry that is currently a **no-op placeholder** on iOS — not wired to anything); a version/build footer plus a copyright line.

### 11.4 Recommendation for Android
Unify on **one native, in-app rendering** of Privacy Policy and Terms of Service for **both** roles (landlord and tenant) — do not replicate iOS's inconsistency of "native for landlord, external web view for tenant." Also recommend actually wiring the About screen and a Terms of Service link into both Settings screens on Android, even though iOS currently leaves them orphaned — this is a case where full "parity" would mean intentionally doing slightly *more* than the current iOS build, since the content clearly exists and was authored to be shown.

---

## 12. Biometric / PIN Security

**None exists.** No Face ID / Touch ID / PIN-lock capability anywhere in the app, on either role. Not a feature to port — do not add device-lock/biometric gating to Android unless it's a new, explicitly-requested feature rather than a parity item.

---

## 13. Navigation Map

```
Landlord app
 └─ Settings tab
      ├─ Theme picker (System/Light/Dark) — inline, no navigation
      ├─ Currency row → Currency picker screen
      ├─ Push Notifications toggle — inline + OS permission dialog
      ├─ Subscription row
      │    ├─ if Pro → "Manage Subscription" → platform's native subscription-management surface (external)
      │    └─ if Free → "Upgrade to Pro" → Paywall (sheet)
      ├─ Contact Support → email composer (external)
      ├─ Rate Maskan → store listing (external)
      ├─ Privacy Policy → native Privacy Policy screen (in-app)
      ├─ Version (static, no navigation)
      └─ Log Out → confirm dialog → sign out → role selection / login

Tenant app
 └─ Settings screen (reached via Dashboard gear icon)
      ├─ Property info (read-only: ID, move-in date, rent-due day)
      ├─ Theme picker (System/Light/Dark) — inline
      ├─ Push Notifications toggle — inline + OS permission dialog
      ├─ Change Password → dedicated screen (in-app)
      ├─ Rate Maskan → store listing (external)
      ├─ Privacy Policy → [iOS: external web view — Android should use native screen instead, see §11.4]
      ├─ Version (static, no navigation)
      └─ Log Out → confirm dialog → sign out → role selection / login
```

---

## 14. Validation Rules — Quick Reference

| Field/Flow | Rule |
|---|---|
| Tenant new password | ≥ 6 characters |
| Tenant new/confirm password | Must match |
| Tenant new vs. current password | Must differ |
| Tenant current-password check | Performed locally (hash comparison) before any network call |
| Currency change | No client-side validation; write failures surface as an error dialog |
| Theme selection | Constrained to exactly 3 values (System/Light/Dark), no free text |
| Push notification toggle | Fully governed by OS permission state, not a stored app preference |
| Landlord forgot-password email | Non-empty check only, no format/regex validation |

---

## 15. Explicitly Out of Scope / Not Present (do not build unless newly requested)

- No in-app account-deletion flow (a real gap on iOS too — flag to product, don't silently build it only on Android without discussion; see §6).
- No in-Settings password change for landlords (pre-login "Forgot Password" only).
- No per-category notification preferences.
- No biometric/PIN app-lock.
- No data-export feature (despite Privacy Policy prose claiming the right exists).
- No "sign out of all devices" option.
- No in-app subscription cancel/change-plan flow — always delegates to the platform's native store subscription management.
- No co-owner management inside Settings (that lives on the property/building screens instead).

## 16. Recommended Deviations From Current iOS Behavior (flag to product before building)

These are places where exact iOS parity would mean **replicating a bug or inconsistency** — call these out explicitly rather than silently copying them:
1. Tenant's Privacy Policy uses an external web view while the landlord's uses a native screen — recommend Android unify on native for both (§11.4).
2. Terms of Service and the About screen are fully built but unreachable from any live navigation on iOS — recommend Android actually wires these in.
3. Push tokens are only persisted for landlords, never for tenants — tenants likely can't receive push at all currently; recommend fixing this rather than replicating it.
4. Push token is never cleared on sign-out — recommend Android clears it, and flag the same fix for iOS.
