# Bills Feature — Android Parity Spec

Reference spec for rent bills, property expenses, and payments — landlord side and tenant side — reverse-engineered from the iOS app (Maskan-iOS) so the Android app can implement full feature parity. Every field, action, validation rule, and business rule below exists in the shipped iOS app — nothing here is speculative.

Source of truth: iOS codebase at time of writing (branch `development`). Companion docs: `ANDROID_PROPERTIES_FEATURE_SPEC.md`, `ANDROID_TENANTS_FEATURE_SPEC.md`.

> **Headline finding — read this before implementing anything:** there is **no server-side scheduled job** that auto-creates monthly rent/utility bills. No Cloud Functions directory, no cron, no background task exists anywhere in this repo for bill generation. All "recurring" bill behavior is a **client-side, lazy-materialization** pattern — see §6. Do not build a proactive monthly bill-generation job on Android; it would create data iOS never produces and the two platforms would disagree.

---

## 1. Data Model

Comment in the source model itself: *"Existing field names and enum raw values must not change"* — this is Firestore-mirrored data; Android must use the exact same raw string values.

### 1.1 `BillType` (string enum)
| case | raw value | display name | default frequency |
|---|---|---|---|
| rent | `rent` | Rent | monthly |
| electricity | `electricity` | Electricity | monthly |
| water | `water` | Water Tax | annual |
| houseTax | `houseTax` | House Tax | annual |
| sewerage | `sewerage` | Sewerage Tax | annual |
| maintenance | `maintenance` | Maintenance | monthly |
| other | `other` | Other | monthly |

Each type has an associated icon and tint color used as the icon-tile background in list rows (rent = purple, electricity = orange, water = blue, houseTax = brown, sewerage = teal, maintenance = indigo, other = grey).

### 1.2 `BillFrequency` (string enum)
`monthly` ("Monthly"), `annual` ("Annual"), `oneTime` ("One Time").

### 1.3 `BillStatus` (string enum)
| case | raw value | display | meaning |
|---|---|---|---|
| pending | `pending` | Pending | not yet paid |
| paid | `paid` | Paid | fully paid |
| overdue | `overdue` | Overdue | manually flagged overdue |
| verifying | `verifying` | Verifying… | tenant marked "paid", landlord confirmation pending |

**There is no partial-payment status** — a bill is binary paid/unpaid at the status level (a tenant can submit an arbitrary amount for verification, but the bill itself doesn't track "50% paid").

### 1.4 `BillPaidBy` (string enum)
`tenant` ("Tenant"), `landlord` ("Property Expense"). `nil` on legacy docs is always treated as `.tenant`.

### 1.5 `Bill` (Firestore collection: `bills`)

| Field | Type | Notes |
|---|---|---|
| `id` | String (doc ID) | |
| `propertyId` | String | The **Firestore document ID** of the property (not the human-readable code) |
| `landlordId` | String | Owner's UID |
| `type` | BillType | |
| `period` | String | `"2026-07"` monthly, `"2026"` annual, `"2026-07-15"` one-time (see §2.2) |
| `amount` | Double | |
| `status` | BillStatus | |
| `dueDate` | Date | |
| `frequency` | BillFrequency? | Nullable on legacy docs → treated as `.monthly` |
| `notes` | String? | Optional |
| `paidAt` | Date? | Set when marked paid |
| `paidBy` | BillPaidBy? | Nullable → treated as `.tenant` |
| `createdAt` | Timestamp (server) | |

### 1.6 `Payment` (Firestore collection: `payments`)

| Field | Type | Notes |
|---|---|---|
| `id` | String (doc ID) | |
| `billId` | String | FK to `bills` |
| `propertyId` | String | Denormalized, for query filtering |
| `landlordId` | String | Denormalized |
| `amount` | Double | Can differ from `bill.amount` — no cross-validation exists |
| `paidDate` | Date | |
| `method` | String | Free string in the model; UI always constrains to a fixed 5-option set (§7) |
| `notes` | String? | Optional |

**Design point**: payments are a separate collection; a bill's `status`/`paidAt` is denormalized onto the bill doc for fast list reads. Deleting a payment **reverts the bill to `pending`** (§2.2).

---

## 2. Service — Bill/Payment CRUD & Business Logic

### 2.1 Live state & listeners
- `bills`, `payments` — realtime Firestore listeners.
- **Landlord-owned scope**: `bills`/`payments` where `landlordId == uid`.
- **Co-owned scope**: whenever the set of properties the landlord can access changes (including co-owned properties they don't own), additional listeners are attached on `bills`/`payments` `where propertyId in [chunk]`, chunked by 30 (Firestore `in`-query limit) — this lets co-owners see bills/payments on properties they don't directly own. Two caches are merged (owned takes precedence on ID collisions) so listeners never clobber each other.
- `bills` sorted `dueDate` descending; `payments` sorted `paidDate` descending.
- **`refresh()`** — pull-to-refresh; forces a server-source (non-cached) re-read for the landlord-scoped bills/payments (property-chunk listeners are not force-refreshed).
- Stopping listeners **retains** the last-known arrays in memory (no flash-to-empty on listener restart).

### 2.2 Period helpers (must reproduce exactly)
- Monthly period key: `"YYYY-MM"`.
- Annual period key: `"YYYY"`.
- One-time period key: `"YYYY-MM-DD"`.
- `periodDisplayName(period)` — 4-char string → shown as the year as-is; 10-char string → localized abbreviated date; 7-char (`YYYY-MM`) → `"July 2026"` style (wide month name + year).

### 2.3 Read/filter helpers
- `bills(for: propertyId)`, `payments(for: billId)`.
- `currentRentBill(for: property)` — finds a Bill where `type == rent && period == <current month key>` for that property. **Returns null if no such bill doc exists** — this is the crux of the lazy-materialization design (§6).

### 2.4 Create / mutate methods

1. **`createBill(property, landlordId, type, frequency, amount, dueDate, referenceDate, notes, paidBy)`** — manual creation from the Add Bill form. `period` computed from `referenceDate` + `frequency`. Always starts `status = pending`.
2. **`recordPayment(bill, landlordId, amount, paidDate, method, notes)`** — batched: sets `bill.status = paid`, `bill.paidAt = paidDate`; creates a new `Payment` doc. Used against an **existing** bill (from Bill Detail).
3. **`updatePayment(payment, amount, paidDate, method)`** — edits an existing payment's amount/date/method only; does **not** touch the bill's status.
4. **`deletePayment(payment)`** — batched: deletes the payment AND reverts the linked bill to `status = pending`, clears `paidAt`.
5. **`markAsOverdue(bill)` / `markAsPending(bill)`** — direct, **manual-only** status field toggles. Nothing in the system automatically flips a bill to `overdue` when its due date passes (see §6 for the real overdue-detection rule used by the Dashboard).
6. **`submitPaymentForVerification(bill, amount, paidDate, method, notes)`** — tenant-side flow: batched write sets `bill.status = verifying` and **also overwrites `bill.amount`** with the tenant-submitted value, plus creates a pending `Payment` doc. (Note: the displayed amount changes immediately upon submission, *before* landlord approval — replicate this exact quirk.)
7. **`approvePayment(bill)`** — landlord action, valid only if `status == verifying`. Finds the associated payment, sets `status = paid`, `paidAt = payment.paidDate`.
8. **`rejectPayment(bill)`** — landlord action, valid only if `status == verifying`. Deletes the associated payment, reverts to `pending`.
9. **`deleteBill(bill)`** — deletes the bill doc **and** every `payments` doc with a matching `billId`, in the same batch (explicit anti-orphan design — don't let stale payments linger in history/charts).
10. **`recordBillPayment(property, landlordId, type, amount, paidDate, method, rentDueDay)`** — **the "quick record any bill type" entry point, and the actual mechanism behind recurring bills**:
    - Computes `targetPeriod` from `paidDate` + `type.defaultFrequency`.
    - Searches for an existing bill matching `propertyId + type + period == targetPeriod + status != paid + (paidBy == tenant or unset)`.
    - **Found** → updates that bill in place (`status = paid`, `amount`, `paidAt`).
    - **Not found** → **creates a brand-new Bill doc, already `status = paid`** (skips `pending` entirely). `dueDate` = the computed rent-due date (for rent type) or the payment date itself (other types).
    - Always creates a matching `Payment` doc.
    - **This is the only mechanism that produces a rent bill for a month with no pre-existing bill doc.** Rent bills are materialized only at the moment a payment is recorded against them — never proactively.
11. **`setPendingAmount(property, landlordId, amount, rentDueDay)`** — "Adjust pending amount" (from Tenant Detail). If a current-month rent bill already exists, patches its `amount` only; else **creates a new `pending` rent Bill doc** for the current period. This and #10 are the **only two places** a rent Bill doc gets created without the landlord using the full Add Bill form.
12. **`sendReminder(tenantId, message)`** — writes to a `notifications` Firestore collection (see the Tenants spec §9 — this is a documented gap, not a push notification).
13. **`rentDueDate(day, referenceDate)`** — clamps `day` to `[1, 28]` (default 5 if unset), builds a Date for that day-of-month in the reference month.

### 2.5 "Current amount due" fallback chain — critical business rule
```
currentAmount = currentRentBill(for: property)?.amount ?? property.monthlyRent ?? 0
```
The landlord-visible "amount due this month" is **real bill if one exists for this period, else the property's flat monthly-rent field**. There is no batch job populating the bill doc at the start of each month — it only comes into existence when the landlord records a payment, adjusts the pending amount, or manually adds a bill. **Android must replicate this exact "virtual until touched" behavior.**

---

## 3. Landlord-side Bills List Screens

### 3.1 Main Bills tab (list)
- **Status filter (segmented control)**: All / Pending / Overdue / Paid.
  - "Pending" matches both `pending` AND `verifying` statuses.
  - "Overdue" matches **only the stored `overdue` status** — it does **not** auto-include past-due `pending` bills (that date-based check only happens on the Dashboard — see §6). This is an intentional inconsistency between screens; replicate it exactly.
- **Type filter (toolbar menu)**: "All Types" + each BillType — icon fill state changes when a filter is active.
- **Add button (toolbar "+")** → Add Bill form, passed only the properties that are **not** paywall-locked.
- **Grouping**: bills grouped by property into sections (property display name + bills sorted `dueDate` desc within the group); groups themselves sorted alphabetically by property name.
- **Row content**: type icon (tinted tile), type name + "Expense" pill badge (only if `paidBy == landlord`), period display name, "Due {date}", amount (currency-formatted), status badge.
- **Empty states** (contextual combinations):
  - Type filter active, no matches: "No {type} bills" / "No {type} bills match the current filter."
  - "All" filter, zero bills anywhere: "No bills yet" / "Tap + to add electricity, water tax, house tax, and other bills."
  - Other status filter, no matches: "No {filter} bills" / "Bills with {filter} status will appear here."
- Pull-to-refresh refreshes bills+properties in parallel.
- **No search field. No sort control** beyond the fixed due-date-desc grouping order.

### 3.2 Property Expenses list (scoped to a building)
- Filters bills to `paidBy == landlord` only, across a given building's flat(s), sorted `dueDate` desc.
- Flat list, no grouping/filter/sort UI, inside a single card.
- Empty state: "No Expenses" / "Property expenses will appear here."
- Entry points: Building Detail's "PROPERTY EXPENSES" section "View All" link (shown only when more than 5 expense bills exist); same pattern for archived/soft-deleted buildings (read-only, still shows bill history).

### 3.3 Inline expense preview (Building Detail / Archived Property Detail)
"PROPERTY EXPENSES" section shows the first 5 landlord-paid bills for the building's flats, each tappable → Bill Detail. This preview section is **not itself paywall-gated** (existing expense records remain viewable on a locked building — only *creating new* bills against a locked property is blocked, by excluding it from the Add Bill property picker).

---

## 4. Bill Detail Screen

Reused everywhere a bill row is tappable (Bills list, Property Expenses, Building Detail, Archived Property Detail, Dashboard's overdue/upcoming rows). Takes a bill ID and a pre-resolved property display name (not looked up internally).

- If the bill can't be found (deleted): "Bill not found" / "This bill may have been removed."
- **Header card**: type icon tile, status badge (top-right), large amount (currency-formatted), then a detail-row list:
  - Property (passed-in name)
  - Period
  - Frequency (`frequency ?? monthly`)
  - Paid By (`paidBy ?? tenant` → "Tenant" / "Property Expense")
  - Due Date
  - Paid On (only if set)
  - Notes (only if non-empty)
- **Action buttons, status-dependent**:
  - `verifying` → "Approve Payment" (primary) + "Reject Payment" (destructive outline).
  - not `paid`, not `verifying` → "Record Payment" (opens a payment form). Additionally: `pending` → "Mark as Overdue"; `overdue` → "Mark as Pending" (manual toggle, both directions).
  - `paid` → no action buttons; the Payment History section is the record.
- **Payment History section** (shown only if payments exist): rows (icon, type, method, amount, paid date, overflow menu with Edit/Delete). Edit opens an edit-payment sheet; Delete opens a confirmation sheet warning that deleting "will revert the bill to pending."
- **Delete Bill** button (bottom, destructive, full-width) → confirmation "Delete this bill? This will permanently remove the bill record." → deletes → navigate back.
- Errors surfaced via a dialog with the underlying error message.
- Currency: always the **landlord's account-level currency code**, format `"{CODE} {amount, 0 decimals, grouped}"` e.g. `"USD 1,234"`.

---

## 5. Add/Edit Bill & Payment Forms

### 5.1 Add Bill (landlord form, sheet)
Fields, in order:
1. **Property** — picker over the pre-filtered (non-locked) property list. Label: `"{buildingName} – {unit}"` or `name`. Empty message: "No properties found. Add a property first."
2. **Bill Type** — horizontal chip row, one per BillType. Selecting a type **auto-sets frequency to that type's default**; selecting `rent` **force-resets Paid By to Tenant**.
3. **Frequency** — 3 chips: Monthly / Annual / One Time (user can override the type's default).
4. **Paid By** — 2 chips: "Tenant's Bill" / "Property Expense". **Disabled/non-interactive when Bill Type is Rent** (rent is always tenant-paid — enforced only at the UI layer, not server-side).
5. **Amount** — decimal input, placeholder "0".
6. **Reference Date** — date picker; live-shows the computed "Period: {periodDisplayName}" below it based on frequency.
7. **Due Date** — a **separate, independent** date picker (no cross-validation against Reference Date — the two can be inconsistent and the app allows it).
8. **Notes (Optional)** — multi-line text, placeholder "e.g. Paid govt portal, receipt #...".
9. Submit "Add Bill", disabled unless: property selected AND amount parses as a number AND amount `> 0`. Button label "Saving…" while in flight.
10. Error dialog "Couldn't save bill" on failure.
11. Auto-selects the first available property on open if none chosen yet.

**There is no attachments/receipt/photo upload field anywhere in bill creation or payment recording.** Do not add a file/image picker to this feature unless explicitly requested — it does not exist on iOS.

### 5.2 Record Payment (landlord, against an existing Bill — from Bill Detail)
Fields: Amount (pre-filled with the bill's amount), Payment Date (date picker — **no future-date restriction on this specific form**, an inconsistency vs. the other payment forms below; flag to the team, replicate as-is unless asked to fix both platforms), Method (picker: Cash / Bank Transfer / UPI / Cheque / Other), Notes (optional). Read-only summary: Type, Period, Bill Amount. Save disabled unless amount valid & `> 0`.

### 5.3 Submit Payment Verification (tenant — "I've Paid This Bill")
Header: type icon, name, period, explanatory text "Record that you've paid this bill. Your landlord will verify and approve the payment." Fields: Amount Paid (pre-filled with the bill's amount), Payment Date (**capped at today, cannot be future-dated**), Payment Method (chip row, default "Bank Transfer"), Notes (optional, placeholder "Transaction ID, reference number, etc."). Submit "Submit for Verification", disabled unless amount `> 0`.

### 5.4 Edit Payment (edit an existing payment, from Bill Detail or Transaction History)
Fields: Amount, Payment Date (capped at today), Method (same 5-option chips). Does **not** touch the bill's status or amount.

### 5.5 Dashboard "Record Payment" quick action
Tenant picker (restricted to active tenants whose property isn't paywall-locked) → read-only property info line → Bill Type chips (any type, not just rent — selecting Rent pre-fills amount from `property.monthlyRent`; other types clear the amount field) → Amount, Payment Date (capped at today), Method. Calls the "quick record, lazily-creates-bill-if-needed" path (§2.4 #10) — usable for **any** bill type, not just rent.

### 5.6 Tenant Detail "Record Payment" sheet
Same shape as §5.5 but scoped to one known property; type chips default to Rent; amount pre-fills from the tenant's current-month suggested amount (§2.5).

**Payment method values (centralize these 5 exactly, they're duplicated across 5 different iOS forms):** `Cash, Bank Transfer, UPI, Cheque, Other`.

---

## 6. Recurring Bill Generation — No Server Job, Lazy Client-Side Materialization

There is **no automatic/scheduled bill generation** anywhere in the system — no Cloud Function, no cron, no `WorkManager`-equivalent on iOS. Monthly/annual bill instances only ever come into existence through exactly three code paths (already detailed above): `recordBillPayment` (#10), `setPendingAmount` (#11), and manual `createBill` via the Add Bill form.

Until one of those fires for a given period, "this month's rent due" is a **purely derived value** (`property.monthlyRent`), never a stored document. `BillFrequency` only determines what `period` string gets stamped on the eventual bill doc — it does **not** drive any repeat-scheduling, "next occurrence" concept, series/template object, or bulk-generate-for-all-tenants capability.

**Do not build a server cron that proactively inserts a `bills` doc on the 1st of each month.** That would create bill documents Android produces that iOS never would (and vice versa for anything iOS creates), and the two platforms' bill lists/dashboards would permanently disagree. Replicate the lazy pattern exactly: compute "current period due" from `monthlyRent` when no bill exists, and only persist a `Bill` doc when the landlord actually acts.

**Overdue detection is two different, non-reconciled mechanisms — replicate both exactly:**
- `markAsOverdue`/`markAsPending` are **manual, landlord-triggered only** — nothing auto-flips `status` when a due date passes.
- The **Dashboard** independently recomputes "is this actually overdue" by comparing `dueDate < today`, ignoring the stored `status` field entirely — because in practice most bills just sit at `pending` past their due date rather than being manually flagged.
- The **Bills tab's "Overdue" filter**, by contrast, matches **only** the stored `overdue` status. So a bill can be date-overdue and simultaneously **not** appear under the Bills tab's Overdue filter unless a landlord manually flagged it. This is a genuine inconsistency in the source app — Android should replicate it identically rather than "fixing" it unilaterally, unless product explicitly signs off on reconciling both platforms' behavior together.

---

## 7. Payment Recording — Rules

- **No true partial-payment/installment model.** A payment's amount can technically differ from the bill's amount (no cross-validation exists), but recording any payment (via `recordPayment` or the lazy `recordBillPayment` path) **immediately sets the bill's status to `paid`** — functionally full-payment-only from a status standpoint, even though dollar amounts aren't enforced to match.
- **Payment method**: a free-form string in the data model, but every UI surface constrains input to the same fixed 5 values: `Cash, Bank Transfer, UPI, Cheque, Other`. Centralize this into one shared enum on Android rather than duplicating it per screen (iOS duplicates it across 5 different views — don't replicate that duplication).
- **Payment date**: **inconsistently restricted across forms** — most tenant/landlord quick-record sheets forbid future dates (capped at today); the Bill-Detail-originated "Record Payment" form has no such cap. Replicate as-is, and flag the inconsistency to the team as a decision point for a potential unified fix across both platforms.
- **Editing** a payment only patches amount/date/method — never touches the linked bill's status.
- **Deleting** a payment always reverts its linked bill to `pending` — the UI must warn the user of this before confirming.
- **Tenant-submitted payments** go through a **different status path** (`verifying`, not `paid` directly) requiring landlord approval or rejection. Approving uses the already-recorded payment's date as `paidAt`; rejecting deletes the payment and reverts to `pending`.

---

## 8. Tenant-Side Bill Screens

Powered by the tenant's own data service (see Tenants spec §2.3/§7) — a completely separate listener/service instance from the landlord's, scoped only to that tenant's own property.

- **Bills visible to a tenant are always filtered to exclude landlord-expense bills** (`paidBy == landlord`) — tenants only ever see bills they themselves owe.
- `pendingBills` (status != paid, sorted dueDate asc), `paidBills` (status == paid, sorted paidAt/dueDate desc), `totalDue` (sum of pendingBills amounts), `currencyCode` (read from the property's stamped `currency`, default `"USD"` — read-only for tenants, they cannot change it).

### Tenant Dashboard bill sections
- **Hero card**: total due (or `"{code} 0.00"` if none), "{N} bill(s) outstanding" / "No pending dues".
- **Outstanding Bills card** (shown only if pending bills exist): count badge, ALL pending bills listed (no cap), tap → Bill Detail sheet.
- **Recent Payments card** (shown only if paid bills exist): top 5, "View All" (only if more than 5) → full Payment History.
- **Error card**: "Couldn't load bills" + error text, if the listener errored.
- **Empty state**: "All caught up" / "No pending bills or dues at the moment." — only when pending, paid, and requests are all empty.

### Tenant Bill Detail sheet
Icon, amount, status pill; detail rows (Period, Due Date, Paid On if paid, Frequency if set, Notes if any). "I've Paid This Bill" button (pending/overdue only) → submission sheet (§5.3). If `verifying`: info banner "Your payment is being verified by your landlord." — no action button.

### Payment History screen (tenant)
Paid bills grouped by calendar month (of `paidAt`, falling back to `dueDate`), newest month first, newest-first within each group. Empty state: "No history yet" / "Your paid bills and rent payments will appear here."

**Tenants cannot**: create bills, edit bills, delete bills, edit/delete payments, mark bills overdue/pending, or see any landlord-expense bill. Their only write action anywhere in this feature is submitting a payment for verification.

---

## 9. Currency Handling

Format everywhere: **`"{3-letter code} {amount, whole number, grouped}"`**, e.g. `"USD 1,234"` — never a currency symbol, always the ISO code prefix.

- **Landlord side**: currency code always comes from the landlord's own account-level currency setting.
- **Tenant side**: currency code comes from the property's stamped `currency` field (read-only, defaults `"USD"` if unset on legacy properties).
- **Exception**: editable amount **input fields** (Record Payment, Add Bill, etc.) show the plain number ungrouped/uncoded while being edited — the currency-code-prefixed format is a **display-only** treatment, never used for text a user needs to type into or that must be parsed back into a number.

Full supported list (17 codes, shared across the whole app): `USD, EUR, GBP, AED, SAR, PKR, INR, EGP, QAR, KWD, OMR, BHD, MYR, NGN, BDT, TRY, IDR`.

---

## 10. Subscription / Pro Paywall Gating

Gating is applied at the **property level**, not the bill level — but it cascades into every bill-creation flow:

- A property is locked if the viewer is not Pro AND owns more than 1 distinct building — the **oldest** building stays free/unlocked; every other building is locked (identical rule to the Properties spec).
- **Bills tab's "+" / Add Bill**: the property picker only ever shows non-locked properties — a locked property is simply **absent** from the list, not shown-and-disabled. A landlord literally cannot create a new bill against a locked property.
- **Dashboard's "Add Bill" quick action**: same filtered property list.
- **Dashboard's "Record Payment" quick action**: its tenant picker excludes tenants whose property is locked.
- **Tenant Detail**: "Record Payment" and "Adjust pending amount" both route to Paywall instead of acting, when the tenant's property is locked. Viewing existing bills/payment history remains available regardless of lock state.
- **Building Detail / Property Expenses preview**: existing expense records on a locked building remain fully viewable — only *creating new* bills against it is blocked.
- **Important**: this gating is enforced **entirely client-side by pre-filtering which properties/tenants are offered in pickers** — none of the bill/payment service methods themselves check lock state internally, and no Firestore security-rule enforcement for this was found in this repo. Android must replicate the same UI-layer filtering, and the team should separately verify whether server-side (security-rule) enforcement exists or is desired, since a determined client could otherwise bypass it.

---

## 11. Notifications/Reminders for Due/Overdue Bills

- **No push notifications (APNs/FCM) are sent from client code for bills, ever.** The only mechanism is the "Remind" button on Tenant Detail, which writes a plain Firestore `notifications` doc (see Tenants spec §9) — this is **not** currently read by any tenant-facing UI in the app, despite the landlord-facing copy implying it will appear "in their app's notifications." Treat this as a known gap to flag to the backend/product team, not a feature Android needs to newly build to match iOS.
- **There is no automatic/scheduled overdue reminder anywhere** — every reminder is a manual, one-off landlord tap.
- No local notification scheduling tied to bill due dates exists on iOS.

---

## 12. Navigation Map

```
Landlord app
 ├─ Bills tab (list) → tap row → Bill Detail
 │    └─ [+] → Add Bill (sheet)
 ├─ Dashboard
 │    ├─ "Add Bill" quick action → Add Bill (sheet, pre-filtered properties)
 │    ├─ "Record Payment" quick action → Record Payment (sheet, tenant/type/amount/date/method form)
 │    └─ Overdue / Upcoming Bills row tap → Bill Detail
 ├─ Tenants tab → Tenant Detail
 │    ├─ "Record Payment" → Record Payment sheet (scoped to that tenant)
 │    ├─ "Remind" → inline, no navigation
 │    ├─ "Adjust pending amount" → inline dialog
 │    └─ "View All" (payments) → Transaction History → row menu → Edit / Delete payment
 ├─ Properties tab → Building Detail
 │    ├─ "PROPERTY EXPENSES" section rows → Bill Detail
 │    └─ "View All" (if > 5) → Property Expenses List → row → Bill Detail
 ├─ Archived Property Detail (read-only) → bill rows → Bill Detail
 └─ Bill Detail
      ├─ "Record Payment" → Record Payment form
      ├─ "Approve Payment" / "Reject Payment" (verifying only) → inline
      ├─ Payment History row menu → Edit Payment / Delete Payment (confirmation)
      └─ "Delete Bill" → confirm → back

Tenant app
 └─ Dashboard
      ├─ Outstanding Bills / Recent Payments row → Tenant Bill Detail sheet
      │    └─ "I've Paid This Bill" → Submit Payment Verification sheet
      └─ "View All" (payments, if > 5) → Payment History screen → row → same Bill Detail sheet
```

There is no cross-link from a Bill back into editing the linked Tenant record, and no deep-link/URL-scheme handling specific to bills.

---

## 13. Loading / Empty / Error States

| Screen | Empty | Error |
|---|---|---|
| Bills tab, All filter, zero bills | "No bills yet" / "Tap + to add electricity, water tax, house tax, and other bills." | — |
| Bills tab, type/status filter, no matches | "No {type/filter} bills" (contextual message, see §3.1) | — |
| Property Expenses list | "No Expenses" / "Property expenses will appear here." | — |
| Bill Detail, bill not found | "Bill not found" / "This bill may have been removed." | — |
| Bill Detail, action failures | — | Dialog with `error.localizedDescription` |
| Add Bill | — | "Couldn't save bill" dialog |
| Tenant Dashboard, all empty | "All caught up" / "No pending bills or dues at the moment." | "Couldn't load bills" card if the listener errors |
| Tenant Payment History | "No history yet" / "Your paid bills and rent payments will appear here." | Silent fail, no dedicated error UI |

---

## 14. Validation Rules — Quick Reference

| Field | Rule |
|---|---|
| Add Bill: Property | Required (must select one) |
| Add Bill: Amount | Must parse numeric, must be `> 0` |
| Add Bill: Reference Date / Due Date | Independent fields, no cross-validation between them |
| Record Payment (all variants): Amount | Must parse numeric, must be `> 0` |
| Payment Date | Capped at today (not future) on most forms — **except** Bill Detail's Record Payment form, which has no cap (replicate this inconsistency) |
| Adjust pending amount | Must parse numeric, must be `> 0` |
| Notes (any form) | Always optional; empty string is coerced to null on save |
| Bill Type = Rent | Forces Paid By = Tenant at the UI layer only (not enforced server-side) |

---

## 15. Recent Git History Context

Currency formatting, co-owner bill visibility, and Pro-lock gating on bills are all **recent** additions (within the last several weeks of the iOS project's history relative to this doc). Treat all three as load-bearing, current behavior — not optional/legacy — when building Android parity; they are not separable milestones.

---

## 16. Explicitly Out of Scope / Not Present (do not build unless newly requested)

- No server-side/scheduled recurring bill generation (§6) — do not add a cron job on Android.
- No attachments/receipts/photo upload anywhere in bill or payment creation.
- No partial-payment/installment tracking at the data-model level.
- No search bar or sort control on any bills list screen.
- No push notifications for bill reminders or due dates (§11) — the only reminder mechanism writes a Firestore doc no UI currently reads.
- No landlord-configurable "days before due" reminder scheduling.
