# Properties Feature — Android Parity Spec

Reference spec for the "Properties" listing screen and the Property Detail screen(s), reverse-engineered from the iOS app (Maskan-iOS) so the Android app can implement full feature parity. Every field, action, validation rule, and business rule below exists in the shipped iOS app — nothing here is speculative.

Source of truth: iOS codebase at time of writing (branch `development`, after commit `dd8ef2c "Fixed deleted property and tenants history"`).

---

## 0. Terminology

- **Building** — a physical building/property with a name + address. Client-side grouping concept only (not a Firestore document); identified by `buildingKey = "{buildingName}|{address}"`.
- **Flat / Unit** — one rentable unit within a building. **Each flat is its own Firestore document** in the `properties` collection. A building with `propertyType == flat` can contain multiple flats; `house`/`villa`/`shop` buildings always contain exactly one flat/unit (itself).
- **Property** — used loosely for "a flat document." One Firestore `properties` doc = one Property = one Flat.

---

## 1. Data Models

### 1.1 `Property` (Firestore collection: `properties`)

| Field | Type | Required | Notes |
|---|---|---|---|
| `id` | String (doc ID) | — | Firestore document ID |
| `landlordId` | String | yes | UID of the owning landlord |
| `name` | String | yes | Legacy/base name; mirrors building name for single-unit types |
| `address` | String | yes | |
| `unit` | String | no | Flat/unit label; empty for single-unit property types |
| `monthlyRent` | Double | yes | |
| `occupied` | Bool | yes | |
| `propertyIdCode` | String | yes | Permanent tenant-login code, e.g. `"ABCDE123"` (8 chars) |
| `createdAt` | Timestamp (server) | yes | Set on create |
| `propertyType` | enum: `house` \| `flat` \| `villa` \| `shop` | no (nullable) | Nullable for legacy docs |
| `buildingName` | String | no | |
| `electricityAccountNumber` | String | no | Optional |
| `houseTaxNumber` | String | no | Optional |
| `waterTaxNumber` | String | no | Optional |
| `photoUrl` | String (URL) | no | Firebase Storage download URL, shared across all flats in a building |
| `coOwners` | List<String> (UIDs) | no | Max 7 |
| `currency` | String (3-letter ISO code) | no | Stamped from landlord's currency at creation time (see §9) |
| `isDeleted` | Bool | no | Soft-delete flag |
| `deletedAt` | Timestamp (server) | no | Set on soft delete |

**Computed values (implement identically on Android):**
- `buildingKey = "{buildingName ?? name}|{address}"`
- `unitDisplayName = unit.isEmpty ? name : unit`
- `PropertyType.supportsMultipleUnits` → **true only for `flat`**; `house`/`villa`/`shop` are always single-unit.

### 1.2 `PropertyDocument` (Firestore subcollection: `properties/{propertyId}/documents`)

| Field | Type |
|---|---|
| `id` | String (doc ID) |
| `name` | String |
| `downloadURL` | String |
| `storagePath` | String (needed to delete from Storage) |
| `mimeType` | String (`application/pdf`, `image/jpeg`, `image/png`) |
| `fileSize` | Int (bytes, post-compression) |
| `uploadedAt` | Timestamp (server) |

Computed: `isPDF`, `isImage`, `fileExtension`, `formattedSize` (human-readable "B"/"KB"/"MB").

### 1.3 `propertyIdCodes` registry (Firestore collection, doc ID = the code itself)

Used to guarantee global uniqueness of `propertyIdCode` and to permanently retire codes of deleted properties (never recycled). Fields (inferred from usage): claimed on creation, `retired: Bool` + `deletedAt` set when the owning property is soft-deleted.

### 1.4 `Currency`

Static list of 17 supported currency codes (no more, no less — mirror exactly):
`USD, EUR, GBP, AED, SAR, PKR, INR, EGP, QAR, KWD, OMR, BHD, MYR, NGN, BDT, TRY, IDR`

Formatting rule everywhere rent/bills are displayed: **`"{3-letter code} {amount}"`** — whole numbers only (no decimals), grouped digits, code prefix (never a currency symbol). E.g. `"USD 2,500"`.

---

## 2. Properties List Screen

Entry point: "Properties" tab in the landlord's main tab bar.

### 2.1 Grouping & sorting
- Properties are fetched flat (all flats owned or co-owned by the current landlord) and **grouped client-side** by `buildingKey` into `BuildingGroup { name, address, type, flats, occupiedCount }`.
- Building groups sorted by **newest flat's `createdAt` descending**.
- Flats within a building sorted **alphabetically by `unitDisplayName`**.
- **No search bar. No sort control.** Order is fixed as above.

### 2.2 Status filter (segmented control: "Active" / "Old")
- Only rendered if the archived (soft-deleted) list is non-empty.
- Switching to "Old" auto-resets back to "Active" if the archived list becomes empty (e.g., last archived item is otherwise gone).

### 2.3 Active tab
- **Empty state**: icon (building icon), title "No properties yet", message "Add your first property to start managing rent and bills."
- **Pull to refresh** → forces a server (non-cached) re-fetch.
- Each building renders as a card:
  - **Header row** (tap → Building Detail): lock icon if the building is paywall-locked (see §8) + building name; "X/Y occupied" OR, while a photo upload is in flight for this building, "Uploading photo…" with a spinner instead; address; property type label; chevron.
  - **One row per flat** (tap → Flat Detail): unit display name, Occupied/Vacant badge (green/orange), Property ID code (prefixed with `#`), monthly rent formatted with currency code.
- **Add button** (top bar, "+"), Active tab only:
  - If landlord is not Pro AND owns ≥ 1 distinct building already (free limit = 1) → show Paywall.
  - Else → open **Add Property** form.
  - "Owned" building count = distinct `buildingKey`s where `landlordId == self` (co-owned buildings don't count against the limit).
- **Photo upload failure**: a dedicated alert ("Photo upload failed") surfaces if a background photo upload fails — this never blocks or fails the property creation itself.

### 2.4 Old / Archived tab
- Fully **read-only**. No add/edit, no occupied badge logic beyond a "Deleted" badge.
- Building cards: name + red "Deleted" badge, address, type.
- Flat rows: unit name, Property ID, rent — tap → **Archived Property Detail** (read-only screen, §6).

---

## 3. Building Detail Screen

Reached by tapping a building's header row from the list.

Sections top to bottom:
1. **Photo hero** (if `photoUrl` set).
2. **Header**: building name, "X/Y occupied" badge, address, property type.
3. **Lock banner** if the building is currently paywall-locked.
4. **Units section** ("UNITS"):
   - "Add Flat" button — **only shown when `propertyType.supportsMultipleUnits`** (i.e., `flat` type only). Tapping while locked → Paywall instead of the Add Flat form.
   - Row per flat (unit name, occupied badge, Property ID, rent) → tap → Flat Detail.
5. **Documents row** → navigates to Property Documents screen, passing `locked` state.
6. **Property Expenses section** — shown only if there are any bills where `paidBy == landlord` across the building's flats. Shows the first 5; "View All" link (only if more than 5 exist) → Property Expenses List screen. Tapping a row → Bill Detail.
7. **Co-owners section** ("CO-OWNERS"):
   - "Add" button, owner-only.
     - Locked → Paywall.
     - Not locked but not Pro and co-owner count ≥ free limit (1) → Paywall.
     - Else → prompt for co-owner email (inline dialog/alert, no navigation).
   - Lists: current owner's email/"Owner" badge (shown to co-owner viewers), then each co-owner with a remove button (owner-only) → confirmation dialog.
8. **Primary action button**:
   - Owner viewing: "Delete Property" (deletes the whole building/all flats) — **disabled unless every flat in the building is vacant**; confirmation dialog: "All N unit(s) and their Property IDs will be permanently removed."; helper text explaining why disabled when not all vacant.
   - Co-owner (non-owner) viewing: "Leave Building" (orange/warning styling) — confirmation dialog → removes self from `coOwners`.
9. **Edit button** (top bar) — locked → Paywall; else → **Edit Property** form, pre-filled from the *first* flat (shared fields cascade to all sibling flats on save).

---

## 4. Flat / Property Detail Screen

Reached by tapping a flat row (from the list directly, or from Building Detail's unit list).

- If the property can no longer be resolved (e.g. deleted elsewhere while viewing) → "Property not found" empty state ("This property may have been removed.").
- Title = the flat's `unitDisplayName`.
- **Edit button** (top bar) — locked → Paywall; else → Edit Property form.
- Content:
  1. Lock banner if locked.
  2. Photo hero if set.
  3. Header: building/property name, Occupied/Vacant badge, unit name (if non-empty), property type label.
  4. **Property ID card**: code shown large/monospaced + **Copy button** — copies to clipboard, shows a transient "Copied" confirmation for ~2 seconds.
  5. **Tenant section** ("TENANT") — shown only if `occupied == true` and an active tenant is found for this property. Shows: tenant name, "Since {move-in date}", call button (opens phone dialer / contact sheet) if a phone number is present, tenant's own Property ID code.
  6. **Details card**: Address; Monthly rent (currency-formatted); Electricity account number / House tax number / Water tax number rows — **each shown only if set**.
  7. **Delete button** — **owner only** (not shown to co-owners on this screen):
     - Disabled + shows "Deleting…" while the delete request is in flight.
     - **Disabled entirely while the flat is occupied**, with helper text: "Occupied properties can't be deleted — move out or remove the tenant first."
     - Confirmation dialog: "Delete this property?" — "Its Property ID {code} will be freed. Past bills, payments, and old tenants are kept as history."
     - On success: soft-delete + navigate back. On failure: error dialog with the failure message.

---

## 5. Archived Property Detail Screen (read-only)

Reached from the "Old" tab.

Shows: photo, header with "Deleted" badge, Property ID card, Details card (same fields as §4.6), **Last Tenant section** — the most recent non-active tenant matched to this property (by property reference), sorted by move-out/move-in date descending: name, In/Out dates, badge = "Removed" (if the tenant record itself was deleted) or "Moved Out" (if just moved out), phone if present. Documents link (always opened in **locked** mode — archived properties are never editable). Bills & Expenses section listing all historical bills tied to this property, tappable → Bill Detail.

**No edit or delete actions anywhere on this screen.**

---

## 6. Add / Edit / Delete Flows

### 6.1 Add Property (from list "+")

Fields, in order:
1. **Property type** — single-select chips: House / Flat / Villa / Shop. Choice determines single-unit vs. multi-unit flow below.
2. **Building name** (multi-unit / Flat type) or **Property name** (single-unit types) — required.
3. **Address** — required.
4. **Single-unit types (house/villa/shop)**:
   - Monthly rent — required, numeric, must be `> 0`.
   - Property ID field (see §6.4).
5. **Multi-unit type (flat)** — repeatable list of flat entries, each with:
   - Unit name — required **only if more than one flat is being added**; optional (blank allowed) if exactly one flat.
   - Monthly rent — required, `> 0`.
   - Property ID field (see §6.4), per flat.
   - Remove button (shown once there are ≥2 flats in the form).
   - "Add another flat" button appends a new blank entry.
6. **Optional details** (apply to the whole building, all sibling flats): Electricity account number, House tax number, Water tax number — free text, all optional.
7. **Photo** (optional) — pick from gallery; shows a preview once chosen; "Remove photo" clears the selection. Placeholder prompt: "Add a photo of the building."
8. Submit button: "Add Property" — disabled while saving or while the form is invalid; label changes to "Saving…" while in flight. Cancel is disabled while saving; the sheet/dialog cannot be dismissed by back-gesture while saving.
9. On submit: creates **one Firestore doc per flat** (batched write); the currently-set landlord currency code is stamped onto every created flat; the photo (if any) uploads **in the background after** the property document(s) are created — creation success does not wait on the photo upload.

**Form validity (all must hold to enable Submit):**
- Name/building-name and address non-empty (trimmed).
- At least one flat entry.
- No Property-ID field currently mid-check or in an error state.
- Single-unit: rent `> 0`.
- Multi-unit: every flat's rent `> 0`; if more than one flat, every flat's unit name non-empty; no duplicate/invalid/taken Property ID across the flats in the form.

### 6.2 Edit Property (from Building Detail or Flat Detail "Edit")

Pre-filled from the selected flat.

- **Building/property name** — edits apply to **all sibling flats** in the building.
- **Address** — applies to all sibling flats.
- **Unit name** (multi-unit only) — applies to **this flat only**.
- **Monthly rent** — this flat only.
- **Electricity / House tax / Water tax numbers** — apply to all sibling flats.
- **No Property ID field** (immutable after creation).
- **No photo re-upload** on this form.
- **No property-type change** on this form.
- Validation: name/address non-empty; rent `> 0`.
- `isMultiUnit` for rendering purposes = `propertyType?.supportsMultipleUnits`, falling back to "`unit` field is non-empty" for legacy docs without a `propertyType`.

### 6.3 Add Flat to Existing Building

Only available when the building's `propertyType == flat` (multi-unit). Reached from Building Detail's "Add Flat" button.

Fields: Unit name (required), Monthly rent (required, `> 0`), Property ID (see §6.4, single entry). On submit, all shared building fields (name, address, utility numbers, photo, currency, co-owners) are copied from an existing sibling flat onto the new flat doc.

### 6.4 Property ID field behavior (Add Property / Add Flat, applies per flat)

- **Auto-generated live** from name+address as the user types: algorithm = take up to 5 letters extracted from `name+address` (uppercase, letters only, non-ASCII stripped), pad with random filler letters from the set `ABCDEFGHJKMNPQRSTUVWXYZ` (deliberately excludes **I, L, O** to avoid visual ambiguity) if short of 5, then append 3 random digits → **8-character code**, e.g. `"ABCDE123"`.
- If the user manually edits the field, auto-generation stops for that field (`codeEdited` flag) — manual input is force-uppercased and restricted to letters/digits.
- **"Reset to suggested"** link reverts to the auto-generated value.
- **Live availability check**, debounced ~600ms after typing stops: checks the code isn't already registered in the `propertyIdCodes` registry (server check), and isn't a duplicate of another code already entered elsewhere in the same form (local check, checked first). UI states: Checking / Available (checkmark) / Already in use (x-mark, blocks submit).
- Minimum length: **4 characters**.

### 6.5 Delete a single Flat/Property

- Owner only.
- **Blocked while `occupied == true`** — must remove/move out the tenant first.
- Confirmation required.
- **Soft delete**: sets `isDeleted = true`, `deletedAt = now` on the doc. The property's `propertyIdCode` registry entry is marked `retired = true` (never reused by any future property). Bills, payments, and past tenant records tied to this property are **preserved**, not deleted.

### 6.6 Delete a whole Building

- Owner only.
- **Blocked unless every flat in the building is vacant.**
- Confirmation required, wording surfaces the exact unit count being removed.
- Soft-deletes every flat doc in the building and retires every one of their registry codes (same semantics as §6.5, applied per flat).

### 6.7 Co-owner management (Building Detail only)

- **Add**: owner enters an email; system looks up an existing landlord account by that email. Failure cases (all must be surfaced as distinct, user-facing errors): no account found for that email; email belongs to the current owner (can't add self); already a co-owner; building already has 7 co-owners (hard cap). On success, the UID is appended to `coOwners` on **every flat** in the building (batch).
- **Remove**: owner removes a specific co-owner, confirmation required, batch-removed from every flat's `coOwners`.
- **Leave**: a co-owner (not the owner) removes themself from the building via a "Leave Building" action on Building Detail, same batch removal, no confirmation-target choice (self only).

---

## 7. Property Documents Screen

Reached from Building Detail (and Archived Property Detail, always locked) via a "Documents" row.

- Lists documents attached to the property (Firestore subcollection), sorted by `uploadedAt` descending.
- **Upload**: choose from Photos (gallery) or Files, presented as a menu with those two options.
  - Files: max size **10 MB**, rejected above that with an error.
  - Images: downscaled to max **1920px** on the long edge, re-encoded JPEG quality 0.75, before upload.
  - PDFs: uploaded as-is (no compression).
  - Photos-picker uploads are auto-named `Photo_{timestamp}.jpg`.
  - Upload progress shown as a percentage indicator.
- **View**: tapping a document previews it in-place (PDF/image viewer, e.g. platform's built-in document preview). First view triggers a download to local cache; subsequent views open the cached copy instantly. Each row shows a distinct icon for "downloaded/cached" vs "needs download."
- **Delete**: via a context/long-press menu on a document row; removes the Storage object, the Firestore doc, and any local cache file.
- **Locked mode** (paywall-gated, when `locked == true` — see §8): the upload entry point becomes a paywall trigger instead of opening the picker; empty-state message changes to: "This property is locked on the free plan. Upgrade to Pro to upload documents."
- **Empty state** (unlocked, no docs yet): standard empty-state styling, appropriate icon/copy for "no documents yet."

---

## 8. Subscription / Pro Paywall Gating

**Free-tier limits (constants, must match exactly):**
- `freePropertyLimit = 1` — number of buildings a free-tier landlord can fully use.
- `freeCoOwnerLimit = 1` — number of co-owners allowed per building on free tier.

**Lock determination algorithm** (`isLocked(property, viewerId, isPro)`), must be reproduced exactly:
```
if isPro OR property.landlordId != viewerId:
    return false   # Pro landlords are never locked; a co-owner is never locked
                    # by the OWNER's plan status — only the owner's own plan matters
                    # when THEY are viewing

owned = all properties where landlordId == viewerId
orderedBuildingKeys = distinct buildingKeys among `owned`,
                      sorted by each building's OLDEST flat's createdAt, ascending

if orderedBuildingKeys.count <= freePropertyLimit:
    return false    # everything still fits inside the free limit

unlockedKeys = first `freePropertyLimit` keys of orderedBuildingKeys   # oldest buildings
return property.buildingKey not in unlockedKeys
```
In plain terms: **the oldest N buildings** (N = free limit = 1) a landlord has ever created remain fully usable forever, even off Pro. Any building created after that becomes locked whenever the landlord isn't currently Pro. A building being locked does **not** depend on when Pro lapsed — it's purely "are you outside your free allotment right now."

**What "locked" restricts** (locked buildings/flats remain fully **viewable** and their tenant/records remain intact):
- Cannot open the Edit form (Edit button routes to Paywall instead).
- Cannot add a new Flat to the building.
- Cannot add a co-owner.
- Cannot upload a new Document.
- **Can still**: view all details, delete the property/building, remove/manage the existing tenant, view existing documents, view bills/expenses.

**Where the lock icon/banner appears:** next to the building name in the list; as a banner at the top of Building Detail and Flat Detail.

**Add-new-building gating** (distinct from per-property locking): the list screen's "+" button checks *before* opening the Add Property form — if not Pro and the landlord already owns ≥ `freePropertyLimit` distinct buildings, it routes straight to Paywall instead of the form (you can't even start creating a 2nd free building).

**Pro status resolution**, in priority order:
1. An admin override flag on the landlord's profile (`proOverride == true`) — grants Pro with no expiry, no purchase needed.
2. An active native in-app subscription entitlement (monthly or yearly SKU) checked directly against the platform's billing library — authoritative on that platform. On a successful check, the resulting expiry date and a `proSource` tag (this platform's identifier) are written back to the landlord's profile so the **other platform** can cross-read it.
3. Fallback: a `proExpiryDate` field on the landlord profile in the future — this is how a purchase made on the **other platform** (e.g., Android reading an iOS purchase, or vice versa) is honored.
- Pro status must update **live** (realtime listener on the landlord profile document), without requiring an app restart — e.g. an admin grant or a purchase on the other platform should unlock instantly.

---

## 9. Currency Handling

- Single source of truth: a `currency` (3-letter ISO code) field on the landlord's own profile document, defaulting to `"USD"`.
- **On property creation**, the landlord's current currency code is copied onto the new property doc(s) (`Property.currency`) — this is required because tenants cannot read the landlord's profile document per security rules, so the property doc is the only place a tenant can learn the currency.
- **Changing currency in Settings** updates the landlord's profile **and** batch-updates the `currency` field on every property the landlord **primarily owns** (not on properties they merely co-own — those keep the primary owner's currency).
- **Display format everywhere** (list rows, detail cards, expense rows): `"{3-letter code} {amount}"`, whole numbers only, grouped thousands, code prefix — **never a currency symbol** (i.e. don't render "$", render "USD").
- Supported currency codes (exactly these 17, no others should appear in an Android currency picker used for this feature): `USD, EUR, GBP, AED, SAR, PKR, INR, EGP, QAR, KWD, OMR, BHD, MYR, NGN, BDT, TRY, IDR`.

---

## 10. Tenant / Property History (soft-delete model)

Both properties and tenants are **soft-deleted**, never hard-deleted, specifically to preserve financial and tenancy history:

- **Property**: `isDeleted = true` + `deletedAt` set (instead of document deletion). The properties listener/query splits results client-side into active (`isDeleted != true`) vs. archived (`isDeleted == true`).
- **Property ID code**: the registry entry is marked `retired = true` + `deletedAt` — retired codes are **never reused**, preventing a new property from accidentally reusing a code a past tenant still associates with their old unit.
- **Tenant**: soft-deleted via a `status: deleted` flag (plus immediately invalidating their login credential) instead of document deletion — preserves all bill/payment history linked to that tenant.
- The "Old Tenants" list (moved-out tenants) **excludes** fully-deleted tenants — deleted tenants only resurface inside a property's own history view (the Archived Property Detail's "Last Tenant" section), where a "Removed" badge distinguishes them from a "Moved Out" (`status: old`, not deleted) tenant.
- Any bill/expense list or dashboard aggregation that resolves a property or tenant name **must continue to resolve names for archived/deleted records** — a bill referencing a since-deleted property must still display a sensible property name, not a broken reference.

**Android requirement**: implement delete-property and delete-tenant as soft-deletes with the same flag semantics, and make sure every place that joins bills → property/tenant names is resilient to the referenced record being archived or deleted.

---

## 11. Images / Photos

### 11.1 Building photo (one per building, shared by all its flats)
- Optional, set only at creation time via the Add Property form (gallery picker, no camera-capture option observed).
- **No re-upload/replace option** in the Edit form — once set, only changeable by... (no path found on iOS; treat as create-time-only for parity).
- Client-side compression before upload: resized to max **300pt width**, JPEG quality **0.7**.
- Storage path pattern: `properties/{landlordId}/{randomId}.jpg`.
- **Upload happens in the background, after** the property document(s) are already created and visible — property creation does not block on photo upload. While uploading, the building's list row shows "Uploading photo…" with a spinner in place of the occupied-count text. On failure, a dedicated alert notifies the user, but the property itself remains created/usable.

### 11.2 Property documents (separate feature, see §7)
- Distinct compression/size rules from the building photo (10MB cap, 1920px/quality-0.75 for images, PDFs untouched).

### 11.3 Image loading/caching (general)
- Photos are displayed via a cached async-image component with a placeholder/spinner while loading — apply the equivalent (e.g. Coil with disk+memory cache) on Android for all property photo displays.

---

## 12. Navigation Map

```
Properties tab (list)
 ├─ [+] → Add Property form            (paywall-gated on landlord's building count)
 ├─ tap building header → Building Detail
 │    ├─ Edit → Edit Property form      (paywall-gated)
 │    ├─ Add Flat → Add Flat form       (paywall-gated; flat-type buildings only)
 │    ├─ Documents row → Property Documents screen
 │    ├─ Expenses "View All" → Property Expenses List → tap row → Bill Detail
 │    ├─ Expense row (inline preview) → Bill Detail
 │    ├─ Add / Remove co-owner → inline dialogs, no navigation
 │    ├─ Delete Property (whole building) → confirm → back to list
 │    ├─ Leave Building (co-owner) → confirm → back to list
 │    └─ tap flat row → Flat Detail
 ├─ tap flat row (also reachable directly from list) → Flat Detail
 │    ├─ Edit → Edit Property form       (paywall-gated)
 │    ├─ Copy Property ID → inline, no navigation
 │    ├─ Tenant call icon → phone dialer / contact sheet
 │    └─ Delete Property (this flat only) → confirm → back
 └─ "Old" tab → archived building sections
      └─ tap archived flat row → Archived Property Detail (read-only)
           ├─ Documents (always locked) → Property Documents screen
           └─ tap bill row → Bill Detail
```

Paywall is reachable as a modal from every gated action above (Add building, Add flat, Add co-owner, Edit, Upload document when locked).

**No deep links / external URL routing** exists for this feature on iOS — everything is in-app stack/modal navigation. Android does not need to support external deep links into a specific property unless a new requirement is introduced.

**Roles**: this entire feature set is **landlord-only**. Tenants never see a "Properties" tab or list — a tenant's app experience is scoped to their own single unit via their Property ID + password login, using a completely separate set of screens.

**Permissions within landlord role:**
- **Owner** (`property.landlordId == currentUid`): sees Edit, Delete, Add Flat, Add/Remove Co-owner, and (on Building Detail) "Delete Property."
- **Co-owner** (`currentUid` in `coOwners`, not the owner): sees view-only + "Leave Building" on Building Detail; **does not** see delete or co-owner management. Note: on iOS, the Edit button is gated only by lock state, not by ownership — a co-owner can currently open the Edit form same as the owner. Confirm with product whether to replicate this on Android or restrict Edit to owners only; documenting as-is since that's current shipped behavior.

---

## 13. Loading / Empty / Error States

| Context | State |
|---|---|
| Properties list, no properties yet | Empty state: building icon, "No properties yet", "Add your first property to start managing rent and bills." |
| Properties list, loading | No explicit spinner tied to a loading flag was found on iOS — list simply populates once the realtime listener delivers data. Android may add a brief loading indicator if desired; not a strict parity requirement. |
| Documents screen, no docs (unlocked) | Standard empty state. |
| Documents screen, no docs (locked) | "This property is locked on the free plan. Upgrade to Pro to upload documents." |
| Expenses list, empty | Icon (tray), "No Expenses", "Property expenses will appear here." |
| Flat Detail, property no longer resolvable | "Property not found", "This property may have been removed." |
| Any delete/save failure | Error dialog showing the underlying error message. |
| Photo upload failure (background) | Dedicated alert: "Photo upload failed" — does not block/undo the property creation. |
| Document upload/delete failure | Error dialog. |
| Co-owner add/remove failure | Error dialog with the specific validation reason (see §6.7). |
| Property ID already taken | Inline field-level error state, blocks form submission. |

---

## 14. Validation Rules — Quick Reference

| Field | Rule |
|---|---|
| Building/property name | Required, non-empty after trimming whitespace |
| Address | Required, non-empty after trimming whitespace |
| Monthly rent | Required, numeric, `> 0` |
| Unit name | Required only when a building has more than one flat; optional/blank allowed for a single flat |
| Property ID code | Min 4 characters; globally unique (server-checked); auto-uppercased; letters+digits only when manually edited |
| Co-owner email | Must match an existing landlord account; not self; not already a co-owner; max 7 co-owners per building |
| Delete single flat | Blocked while `occupied == true` |
| Delete whole building | Blocked unless every flat in the building is vacant |

---

## 15. Explicitly Out of Scope / Not Present (do not build unless newly requested)

- No search bar or sort control on the Properties list.
- No in-app localization system for these screens — all strings are hardcoded English; Android may localize independently but there's no source-of-truth string table to port.
- No analytics events, sharing, or export functionality tied to Properties.
- No deep-linking/universal-link support into a specific property.
- No camera-capture option for the building photo (gallery picker only).
- No photo replace/re-upload from the Edit Property form.
