# Tenants Feature — Android Parity Spec

Reference spec for the landlord-side Tenants list/detail screens and the tenant's own self-service app, reverse-engineered from the iOS app (Maskan-iOS) so the Android app can implement full feature parity. Every field, action, validation rule, and business rule below exists in the shipped iOS app — nothing here is speculative.

Source of truth: iOS codebase at time of writing (branch `development`, after commit `dd8ef2c "Fixed deleted property and tenants history"`). Companion doc: `ANDROID_PROPERTIES_FEATURE_SPEC.md` (properties/flats referenced heavily here).

---

## 1. Data Model

### 1.1 `Tenant` (Firestore collection: `tenants`, top-level, not nested)

| Field | Type | Notes |
|---|---|---|
| `id` | String (doc ID) | Also equals the tenant's Firebase Auth UID (see §7) |
| `propertyId` | String | Human-readable Property ID code (legacy field name, kept for the login Cloud Function's query field) — currently always equal to `propertyIdCode` |
| `name` | String | |
| `contact` | String | Phone number, freeform |
| `moveInDate` | Date | |
| `propertyIdCode` | String | Same code as `propertyId`, duplicated for direct display |
| `passwordHash` | String | SHA-256(salt + password), hex |
| `passwordSalt` | String | 16-byte random hex |
| `createdAt` | Timestamp (server) | |
| `landlordId` | String? | Nullable on legacy docs; scopes security rules/queries |
| `propertyDocumentId` | String? | Firestore doc ID of the linked property; nullable on legacy docs |
| `status` | enum: `active` \| `old` \| `deleted`, nullable | `nil` == active (legacy docs) |
| `moveOutDate` | Date? | |
| `rentDueDay` | Int? | 1–28; nil until landlord sets it |

Computed: `isActive = (status ?? .active) == .active`.

**No email, photo, lease-end-date, deposit, or lease-document field exists on Tenant** — deliberately minimal model. Do not add these on Android unless newly requested.

### 1.2 Credential helpers (must match server-side verification exactly)
- `hashPassword(password, salt) = SHA256(salt + password)`, hex-encoded.
- `generateSalt()` — 16 random bytes, hex.
- `generateTempPassword()` — 6 characters: 5 random digits + 1 random lowercase letter, shuffled.

---

## 2. Landlord-side Service — Tenant CRUD & Status Transitions

### 2.1 Live state
- `tenants` — all tenants belonging to the landlord (owned + co-owned properties), realtime listener, sorted `createdAt` desc.
- `activeTenants` = `tenants.filter(isActive)`.
- `oldTenants` = `tenants.filter { !isActive && status != .deleted }` — **excludes soft-deleted tenants**.

### 2.2 Methods
- **`assignTenant(property, landlordId, name, contact, moveInDate) → (propertyIdCode, tempPassword)`** — Add Tenant:
  - Generates temp password + salt + hash.
  - Batch write: creates the tenant doc (`status = active`) + sets the property's `occupied = true`.
  - Returns the plaintext temp password — **the only moment it's ever available**; never persisted or shown again.
- **`updateTenant(name, contact, moveInDate)`** — partial update, these 3 fields only.
- **`updateRentDueDay(day)`** — updates `rentDueDay` only.
- **`markMovedOut(tenant, moveOutDate)`** — Move-out flow (§6.1).
- **`deleteTenant(tenant)`** — Soft-delete flow (§6.2).
- **`resetPassword(for: tenant) → newPlaintextPassword`** — Reset Login Password: generates new temp password + salt + hash, writes directly to Firestore (landlord's own write, passes security rules). Returns plaintext once.
- **`resolvePropertyRef(for: tenant)`** (internal) — uses `propertyDocumentId` if present, else looks up `properties` by `propertyIdCode == tenant.propertyId` (legacy-doc fallback).

### 2.3 Tenant-side service (self session)
Backing the tenant's own app (see §7). Live state: `tenant`, `property`, `bills`, `payments`, `requests`, `isLoading`, `billsError`.

- **`start(tenantId)`** — attaches listeners: `serviceRequests` where `tenantId == uid` (starts immediately); `tenants/{tenantId}` doc listener drives `tenant`; when `propertyDocumentId` resolves (direct field, or one-time `propertyIdCode` lookup for legacy docs), attaches `properties/{id}` listener → `property`, `bills` where `propertyId == id` **filtered to exclude `paidBy == landlord` bills** sorted by dueDate desc, and `payments` where `propertyId == id` sorted by paidDate desc.
- **`refreshBills()`** — pull-to-refresh, server-forced re-fetch with the same filter.
- **`changePassword(current, new)`** — verifies `current` locally against stored hash (throws "Current password is incorrect." on mismatch); tenant cannot write their own Firestore doc directly (security rules require `auth.uid == landlordId`, but the tenant's UID is their own doc ID) so this **calls a server-side Cloud Function** (`changeTenantPassword`) with the new hash+salt.
- **`submitRequest(title, category, description)`** — creates a `ServiceRequest` doc; optimistic local insert.
- **`stopListening()`** — tears down all listeners, clears state.

`pendingBills` = bills where `status != paid`, sorted dueDate asc. `paidBills` = bills where `status == paid`, sorted paidAt/dueDate desc. `totalDue` = sum of pendingBills amounts. `currencyCode` = `property.currency ?? "USD"`.

### 2.4 Auth
- **`signInTenant(propertyId, password)`** — trims + uppercases `propertyId`, calls Cloud Function `tenantLogin` with `{propertyId, password}`; response must contain a Firebase custom-auth `token`; signs in with it, then sets the local role to `.tenant`.
- **`signOut()`** — Firebase Auth sign-out + clears local role.
- **Architectural rule**: a signed-in tenant's Firebase Auth UID **equals the Firestore `tenants` document ID** (the login Cloud Function mints the custom token from the doc ID) — this is how the tenant app knows which tenant doc belongs to it.

---

## 3. Landlord Tenants List Screen

Entry point: "Tenants" tab in the landlord's main tab bar.

- **Segmented control "Active" / "Old"** — shown only if `oldTenants` is non-empty; auto-resets to "Active" if the old list becomes empty while selected.
- List source: `activeTenants` or `oldTenants` per filter. **No search bar, no sort control** — fixed `createdAt` descending order.
- **Empty states**:
  - Active, empty: "No active tenants" / "Assign a tenant to one of your vacant properties to get started."
  - Old, empty: "No old tenants" / "Tenants you mark as moved out will appear here."
  - Both wrapped in pull-to-refresh even while empty.
- **List item (tenant card)**: name, property (`unit · buildingName` or fallback), overflow menu (Edit / Remove — destructive), footer row showing Property ID code (`#` prefix) and either "Since {moveInDate}" (active) or "Moved out {date}" (old, warning-colored).
- Tapping the card body (not the overflow menu) → **Tenant Detail** screen.
- **Add button (top bar "+")** → **Add Tenant** form (sheet).
- **Remove confirmation dialog**, message depends on state:
  - Active: "{name} will no longer be able to log in, and {propertyIdCode} will be marked vacant."
  - Old: "{name} will be removed from the tenant list. Payment history will remain accessible in Bills."
  - Buttons: "Remove" (destructive) / "Cancel".
- Separate error alert "Couldn't remove tenant" on failure.
- Pull-to-refresh refreshes both properties and tenants services in parallel.
- **Paywall interaction**: a tenant's row/edit access is gated if the tenant's property is currently paywall-locked (see §8) — this doesn't hide the tenant, it restricts what Edit can do.

---

## 4. Tenant Detail Screen

If the tenant can't be resolved: empty state "Tenant not found" / "This tenant may have been removed."

Sections top to bottom:
1. **Tenant info card** — name, property (`unit · buildingName`), call/contact icon button (opens Call/WhatsApp action sheet, shown only if `contact` is non-empty), Property ID code, "Since {moveInDate}".
2. **Rent card** (active tenants only, "RENT"):
   - Monthly rent — read-only, sourced from the property's `monthlyRent`, currency-formatted.
   - Payment due day — picker, Day 1–28; paywall-locked if the plan lapsed.
3. **Current month card** (active tenants only, gradient hero):
   - Period label (e.g. "SEPTEMBER 2026") + Paid/Pending badge.
   - Current amount = the current month's rent **Bill** if one exists for this period, else falls back to `property.monthlyRent` (see §5 Bills spec for why bills are lazily created — the same "virtual until touched" rule applies here).
   - Status subtext: "Received this month — nothing pending." or "Due on day {N} of the month" / "Pending this month".
   - **Record Payment** button → payment form sheet (paywall-locked if applicable).
   - **Remind** button (shown only if not paid this month) — writes a `notifications` Firestore doc (see §9); shows "Reminder sent" confirmation.
   - **Adjust pending amount** link (hidden once paid) — inline dialog with a numeric field to override this month's pending amount; paywall-locked if applicable.
4. **Moved-out banner** (inactive tenants) — "Moved out on {date}" (warning color), replaces the Rent/Current-month cards entirely.
5. **Recent Payments section** — up to 5 previewed rows (icon, bill type, method, amount, date, edit/delete overflow menu); **"View All"** → full Transaction History screen (all payments tied to this tenant's property, excluding landlord-expense bills and payments whose bill was deleted).
6. **Account section** (active tenants only) — **"Reset Login Password"** row → confirmation dialog → generates new credentials → **one-time reveal screen**: Property ID + new password with copy-to-clipboard, and the note "This is the only time the new password is shown. The previous password stopped working immediately."
7. **Edit** button (top bar, disabled for inactive tenants) → Edit Tenant form (sheet).

**Record Payment sheet** (from this screen) fields: Bill Type (chip picker — all bill types, default Rent; amount auto-suggests the tenant's current rent only when type is Rent), Amount (decimal), Payment Date (date picker, capped at today), Method (chip picker: Cash / Bank Transfer / UPI / Cheque / Other).

**Lock semantics on this screen**: viewing, reminders, password reset, and moving the tenant out **remain available even when locked**; Record Payment, editing tenant info, and rent-due-day settings are paywall-gated.

---

## 5. Add / Edit Tenant Forms

### 5.1 Add Tenant (sheet, from Tenants list "+")

Fields:
- **Property** — picker over vacant properties (`!occupied` AND not paywall-locked). Label: `"{unit} · {buildingName} ({propertyIdCode})"`.
- **Tenant name** — required.
- **Contact** — required, phone-oriented keyboard.
- **Move-in date** — date picker, no min/max constraint.

**Validation**: property selected AND name non-empty (trimmed) AND contact non-empty (trimmed). No phone-format regex, no email field, no lease-term/deposit field.

**Empty-vacant-properties messaging**:
- All vacant properties locked (free plan lapsed): "Your other vacant properties are locked on the free plan. Resubscribe to Pro to assign a new tenant there." + "Upgrade to Pro" button.
- No vacant properties at all: "All your properties are occupied. Add a property first, or remove an existing tenant."

**On save** → success screen (credentials shown once): checkmark, "Tenant added", "Share these credentials with your tenant. The password is shown only once — it can't be viewed again.", Property ID + Temporary password rows, "Copy Both" button (copies `"Property ID: X\nPassword: Y"`), "Done" button. **This screen cannot be dismissed by back-gesture while credentials are showing** — force the user to tap Done.

### 5.2 Edit Tenant (sheet, from Tenants list or Tenant Detail)

Editable fields: Tenant name, Contact, Move-in date. **Property ID shown read-only** (permanent, never editable).

**Validation**: name & contact non-empty (same as Add).

**Locked state**: if the tenant's property is paywall-locked, edits are disabled with the note "This property is locked on the free plan — tenant details can't be updated. You can still mark the tenant as moved out." — Save routes to Paywall instead.

**Move-out subsection** (shown only if the tenant is currently active): "TENANT LEFT THE PROPERTY?" header, Move-out date picker (bounded to `≥ moveInDate`), "Mark as Moved Out" button (warning-styled) → confirmation: "Mark {name} as moved out? Their login will stop working and {propertyIdCode} will become available for a new tenant. Their record moves to Old Tenants." → triggers §6.1.

If the tenant is already inactive, this subsection is replaced by the static "Moved out on {date}" banner. **No credential fields are editable in this form** (password reset only lives on Tenant Detail's Account section).

---

## 6. Move-Out and Delete/Remove Flows — Exact Semantics

### 6.1 Move-out
Triggered from Edit Tenant's "Mark as Moved Out".

Batch write:
- Tenant doc: `status = old`, `moveOutDate = <chosen date>`, `passwordHash = ""` (login revoked instantly — an empty hash can never match a computed hash).
- Property doc: `occupied = false` (if resolvable).

The tenant document is **not deleted** — it moves to the "Old" tab. Bills/payment history is untouched and remains fully queryable. The property becomes selectable again in the Add-Tenant vacant-property picker.

### 6.2 Delete/Remove — **soft delete only**

Triggered from the Tenants list row's overflow menu → "Remove", confirmed via dialog.

- If the tenant `isActive` at delete time, its property is freed (`occupied = false`) in the same batch. If already "old", the property is **not** touched (it may already have a newer tenant).
- Batch write: tenant doc → `status = deleted`, `passwordHash = ""`. **The document is never deleted from Firestore.**
- Deleted tenants are excluded from both Active and Old lists — they vanish from all list UI.
- Login is revoked instantly (empty hash).
- Payment/bill history tied to that tenant's property remains fully visible in Bills.

> **This is a recent behavior change (commit `dd8ef2c`).** Prior to that fix, delete was a hard delete, which broke historical bill/payment display. Android must implement the current soft-delete semantics — never hard-delete a tenant document.

### 6.3 Related property rule
A property cannot itself be deleted while `occupied == true` — the landlord must move out or remove the tenant first (see `ANDROID_PROPERTIES_FEATURE_SPEC.md` §6.5). Soft-deleted properties still resolve their **last tenant** (most recent tenant matched by property, whether moved-out or deleted) on the read-only Archived Property Detail screen, with a "Moved Out" or "Removed" badge depending on the tenant's status.

---

## 7. Tenant-Side (Self-Service) App

The tenant experience is a **completely separate, single-stack app** (no bottom tab bar) reached only via Property ID + password login. Tenants never see the landlord's tab bar or the Tenants/Properties/Bills tabs directly.

### 7.1 Login
Fields: **Property ID** (auto-capitalized characters, placeholder "e.g. ABCDE123"), **Password** (secure/masked). Validation: both non-empty only (no client-side format regex). On submit, calls `signInTenant` (§2.4).

### 7.2 Shell
Single navigation stack rooted at the tenant Dashboard. One tenant-data-service instance is created for the session, started on appear (`start(tenantId: currentUid)`) and torn down on disappear.

### 7.3 Dashboard (tenant home)
- Title: personalized greeting — "Hello, {FirstName}" (or "Hi!" if name unknown).
- Toolbar: `+` (New Request), gear icon (Settings/Profile).
- **Hero card**: total due amount (large) + "No pending dues" / "{N} bill(s) outstanding".
- **Property card**: photo banner (or placeholder), name/unit, address, 3 metric cells (Monthly Rent, Property ID, Property Type) — or a lightweight placeholder row (icon + Property ID + move-in date) if the property hasn't loaded yet.
- **Outstanding Bills card** (shown only if pending bills exist) — count badge, ALL pending bills listed (no cap/"View All" on this section), tap → Bill Detail sheet.
- **Recent Payments card** (shown only if paid bills exist) — top 5, "View All" (only if more than 5) → full Payment History screen.
- **My Requests card** (compact, shown only if requests exist) — top 3, "View All" (only if more than 3) → Requests screen.
- **Error card**: "Couldn't load bills" + error text, if the bills listener errored.
- **Global empty state**: "All caught up" / "No pending bills or dues at the moment." — shown only when pending bills, paid bills, AND requests are all empty (and no error).
- Loading: a spinner replaces the bill/payment/request sections while the tenant document is loading — note the property/hero cards are **not** gated by this loading flag, so they can render before bills finish loading.
- **Pull-to-refresh is cosmetic only** (a ~400ms delay animation, no real re-fetch) — data is realtime via listeners already; Android should treat this as optional polish, not a required re-fetch mechanism.

**Bill Detail sheet** (tap any bill row): amount, status badge (Paid/Verifying/Overdue/Pending — "Overdue" computed client-side as `status not paid/verifying AND dueDate < today`, not from a stored flag), Period, Due Date, Paid On (if paid), Frequency, Notes. **"I've Paid This Bill"** button (pending/overdue only) → payment-verification submission sheet. If `verifying`: shows "Your payment is being verified by your landlord." with no action button.

### 7.4 Payment History screen
All paid bills, **grouped by calendar month** (of paidAt, falling back to dueDate), newest month first. Empty state: "No history yet" / "Your paid bills and rent payments will appear here." Pull-to-refresh here **is** a real server-forced refetch (unlike the dashboard's cosmetic one).

### 7.5 Requests screens
- List: status badge per request (Pending / In Progress / Resolved). Empty state: "No requests yet" / "Tap + to submit a maintenance or service request."
- Add Request (sheet): Category grid (Plumbing / Electrical / Appliance / General / Other — icon+label chips, 3-column), Issue Title (required), Details (optional, multi-line). Submit creates the request.

### 7.6 Settings/Profile (tenant)
- Header: avatar (initial letter), name, contact.
- **PROPERTY** (read-only): Property ID, Move-in Date, Rent Due (Day N of each month, only if set).
- **APPEARANCE**: theme picker (System/Light/Dark), device-local preference shared with the landlord side.
- **NOTIFICATIONS**: push-notification permission toggle (mirrors OS permission state; denied → dialog directing to system Settings).
- **SECURITY**: "Change Password" → dedicated screen (§7.7).
- **ABOUT**: Rate app (store listing), Privacy Policy (in-app), Version.
- **Log Out** → confirmation dialog → sign out.
- Tenants have **no editable name/contact/photo fields** — only the landlord can change those (via Edit Tenant).

### 7.7 Change Password (tenant, authenticated)
Fields: Current Password, New Password, Confirm New Password (all masked).

**Validation**: new password ≥ 6 characters; new must equal confirm; new must differ from current. Save disabled until all fields non-empty and valid; current-password check happens **locally first** (hash comparison) before any network call — throws "Current password is incorrect." on mismatch without a round trip.

Because a tenant cannot write to their own document directly (security rules require the writer's UID to equal `landlordId`, which a tenant's UID never does), this flow **must go through a server-side function** rather than a direct Firestore write. Success → confirmation dialog → dismiss.

### 7.8 What tenants explicitly cannot do
- Cannot view/upload property documents (documents are landlord-only, at the property level).
- Cannot see other tenants, other properties, or the landlord's own expense-only bills.
- Cannot edit their own name/contact/move-in date.
- No lease-document, deposit tracking, or lease-end-date feature exists anywhere.
- No in-app read of the landlord's "Remind" notifications — see §9 (a real gap worth flagging to backend, not just Android).

---

## 8. Subscription / Pro Paywall Gating Touching Tenants

There is **no tenant-count limit** — gating is entirely **property-count-based** (the same "oldest N buildings stay free" rule from the Properties spec; N = 1 free building). This cascades into tenant flows as follows:

- **Add Tenant**: the vacant-property picker excludes locked properties; if the only vacant properties are locked, an upsell message + "Upgrade to Pro" button is shown instead of a hard block.
- **Edit Tenant**: on a locked property, tenant-info edits (name/contact/move-in date) are disabled, but **move-out remains available regardless of lock state**.
- **Tenant Detail**: when locked — Record Payment, editing tenant info, and the "Payment due day"/"Adjust pending amount" controls route to Paywall. **Viewing, sending reminders, resetting the login password, and marking moved out stay available while locked.**
- **Dashboard "Record Payment" quick action**: its tenant picker excludes tenants whose property is locked.
- Only one active tenant per property at a time is modeled — enforced implicitly through the property's `occupied` boolean, not a count field.

---

## 9. Notifications/Reminders

- **"Remind" button** (Tenant Detail, current-month card) writes a Firestore `notifications` doc: `{tenantId, type: "reminder", message, sentAt: <server time>, read: false}`. Message: `"Rent of {amount} for {period} is pending (due day {N}). Please arrange the payment."` (due-day clause omitted if unset).
- **Known gap, flag to backend/product**: no tenant-side screen in the current app actually reads/displays this `notifications` collection, and no push notification (APNs/FCM) is sent from client code for this event. The landlord-facing copy claims "The tenant will see it in their app's notifications," which is **not currently backed by any UI**. Android should confirm with the backend team whether a server-side function is expected to turn this into a push notification before building anything new here — don't assume Android needs to build a notifications inbox to match iOS, since iOS doesn't have one either.
- `rentDueDay` (1–28) is purely informational — it does not drive any scheduled/local reminder; it only affects bill-due-date computation, dashboard display, and the reminder message text.

---

## 10. Navigation Map

```
Landlord app
 ├─ Tenants tab (list)
 │    ├─ [+] → Add Tenant form (sheet)
 │    ├─ tap tenant row → Tenant Detail
 │    │    ├─ Edit (toolbar) → Edit Tenant form (sheet)
 │    │    │    └─ Mark as Moved Out → confirm → back
 │    │    ├─ Record Payment → payment form (sheet)
 │    │    ├─ Remind → inline, no navigation
 │    │    ├─ Adjust pending amount → inline dialog
 │    │    ├─ Reset Login Password → confirm → one-time credential reveal screen
 │    │    └─ "View All" payments → Transaction History screen
 │    └─ row overflow menu: Edit → Edit Tenant form; Remove → confirm → soft-delete
 ├─ Properties tab → Property Detail → "Tenant" section shows info + contact button only
 │    (no direct link INTO Tenant Detail from here — must go via Tenants tab)
 ├─ Archived Property Detail → "Last Tenant" section, read-only, no navigation
 └─ Dashboard → "Add Tenant" quick action → Add Tenant form (sheet)
             → "Record Payment" quick action → tenant-picker payment form (sheet)

Tenant app (separate login, single stack, no tab bar)
 Login (Property ID + Password) → Dashboard (root)
   ├─ [+] → Add Request screen
   ├─ gear icon → Settings/Profile
   │    └─ Change Password → dedicated screen
   ├─ bill row → Bill Detail sheet → "I've Paid" → payment-verification sheet
   ├─ "View All" payments → Payment History screen
   └─ "View All" requests → Requests screen
        └─ [+] → Add Request screen
```

---

## 11. Loading / Empty / Error States

| Screen | Loading | Empty | Error |
|---|---|---|---|
| Tenants list | none explicit (listener-driven) | Per-tab empty states (§3) | "Couldn't remove tenant" on delete failure |
| Tenant Detail | none explicit | "Tenant not found" if unresolvable | Generic failure alert for rent-day update / reminder / adjust-amount / password reset |
| Add Tenant | "Adding…" button state | Vacant-property messaging (§5.1) | Inline error text |
| Edit Tenant | "Saving…" button state | n/a | Inline error text |
| Tenant Dashboard | Spinner while tenant doc loads | "All caught up" | "Couldn't load bills" card |
| Payment History (tenant) | Spinner | "No history yet" | Silent fail (no dedicated UI) |
| Requests (tenant) | — | "No requests yet" | — |
| Change Password (tenant) | "Saving…" button state | n/a | Inline error text + live validation hint |

---

## 12. Validation Rules — Quick Reference

| Field | Rule |
|---|---|
| Tenant name (Add/Edit) | Required, non-empty after trim |
| Contact (Add/Edit) | Required, non-empty after trim; no format/regex validation |
| Move-in date | No min/max constraint |
| Move-out date | Must be ≥ tenant's move-in date |
| Rent due day | Integer 1–28 only |
| Record Payment amount | Must parse numeric and be `> 0`; date capped at today (not future) |
| Adjust pending amount | Must parse numeric and be `> 0` |
| Tenant new password | ≥ 6 characters; must differ from current; must equal confirm field |
| Property ID login field | Trimmed + uppercased before submit; no client-side format regex |

---

## 13. Business Rules Summary an Android Implementation Must Replicate

1. **One active tenant per property at a time** — `property.occupied` is the single source of truth, kept in sync via batched writes on every tenant status transition (assign, move-out, delete).
2. **Login credential = Property ID Code + password** (salted SHA-256, verified server-side by a login function that mints a custom auth token equal to the tenant's document ID).
3. **Plaintext password is shown exactly once** — on assignment, and again on each reset. Never stored or re-displayed.
4. **Soft delete everywhere** — tenants (and properties) are never hard-deleted; a status flag is set and the record is excluded from active queries, preserving bill/payment history indefinitely.
5. **Move-out ≠ Delete**: move-out keeps the tenant visible under "Old"; delete additionally hides them from both tabs (but the Firestore doc still exists, `status: deleted`).
6. **Revoking access = writing an empty password hash**, not deleting the auth account or document — this instantly invalidates login for both move-out and delete.
7. A property must be vacated (moved out or removed tenant) **before** the property itself can be deleted.
8. Tenant-visible bills are always **filtered to exclude landlord-expense bills** (`paidBy == landlord`), queried by the property's Firestore document ID.
9. Pro/paywall gating is **per-property** (oldest 1 free building), never per-tenant-count; it blocks financial/edit writes on a locked property's tenant but always allows move-out, viewing, reminders, and password reset.

---

## 14. Explicitly Out of Scope / Not Present (do not build unless newly requested)

- No search bar or sort control on the Tenants list.
- No email field, lease document, deposit tracking, or lease-end date on the Tenant model.
- No in-app tenant notifications inbox (the "Remind" feature writes a Firestore doc no UI currently reads — flag to backend before building).
- No landlord-contact info surfaced on the tenant's dashboard (no landlord name/phone shown to tenants anywhere).
- No multi-tenant-per-property support.
- No biometric/PIN security on either side.
