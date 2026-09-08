# Dashboard Feature — Android Parity Spec

Reference spec for the landlord's Dashboard (home tab) and the tenant's Dashboard (home screen) — reverse-engineered from the iOS app (Maskan-iOS) so the Android app can implement full feature parity. Every card, metric, computation, and navigation link below exists in the shipped iOS app — nothing here is speculative.

Source of truth: iOS codebase at time of writing (branch `development`). Companion docs: `ANDROID_PROPERTIES_FEATURE_SPEC.md`, `ANDROID_TENANTS_FEATURE_SPEC.md`, `ANDROID_BILLS_FEATURE_SPEC.md`, `ANDROID_SETTINGS_FEATURE_SPEC.md`.

> **Headline finding:** there is **no dedicated Dashboard service, view-model, or server-computed rollup document.** The Dashboard is 100% **client-side derived** from the exact same live Firestore listener arrays (`properties`, `tenants`, `bills`, `payments`) that back the Properties/Tenants/Bills tabs. Android must replicate the same filter/reduce logic locally over its own equivalent local/live data — do not build a separate aggregation endpoint or cache; the numbers must always agree with what the other tabs show, by construction.

---

## 1. App Shell / Navigation Root

- On launch, after a brief splash screen, the app root switches on the signed-in role: landlord → landlord tab shell; tenant → tenant single-stack shell; signed out → role selection/login.
- Core services (Auth, Purchase/subscription) are instantiated once at the very top of the app and shared everywhere via dependency injection — they are **not** re-created per tab or per screen.
- **There is no app-level banner, update-prompt, or global paywall banner rendered above the tab shell anywhere.** All subscription/lock messaging is expressed through the same per-property `isLocked` filtering used throughout the Properties/Tenants/Bills features — never a persistent top-level banner.

---

## 2. Landlord Tab Shell

5 tabs, in this fixed order:
1. **Dashboard**
2. **Properties**
3. **Tenants**
4. **Bills**
5. **Settings**

**Services owned at the tab-shell level** (created once, injected into every tab, persisting across tab switches — not re-fetched per tab visit): Property service, Tenant service, Billing service, Service-Request service, Landlord-Profile/currency service.

**Lifecycle**:
- On the shell appearing (i.e., once, at landlord login — not on every tab switch): starts realtime listeners on all 5 services simultaneously for the current landlord.
- Whenever the set of accessible property IDs changes (including co-owned properties becoming visible), the Billing service is told to attach additional listeners scoped to those property IDs too — this is how bills/payments on **co-owned** properties become visible (see the Bills spec §2.1).
- On the shell disappearing (logout): all listeners are torn down.

No tab-shell-level banners/alerts exist — it is a pure tab container.

---

## 3. Landlord Dashboard Screen

Title: **"Dashboard"**. Structure top to bottom: Welcome/hero card → Quick actions row → Revenue chart card → Stat card grid → Overdue card (conditional) → Upcoming Bills card (always present, has its own empty state).

Toolbar: a bell icon (top-trailing) → navigates to the Service Requests screen, showing a red numeric badge (capped display at 99) for the count of pending service requests when greater than zero.

### 3.1 Welcome/hero card
- **Static copy only — no personalized greeting and no date-based content on the landlord side.** Title: **"Everything, at a glance"**. Subtitle: **"Occupancy, dues, and activity across every property you manage."**
- Brand-gradient background, light/white text, rounded corners with a drop shadow.
- (Note: a "YOUR PORTFOLIO" eyebrow label and a name-based greeting exist as disabled/commented-out code in the source — i.e., they were built but deliberately not shipped. Do not add a personalized greeting here for Android parity; the tenant side has the personalized greeting instead, see §5.)

### 3.2 Quick actions row
Three equal-width buttons, each opens a **modal form** (not a full-screen push):

| Button | Opens |
|---|---|
| "Add Tenant" | Add Tenant form (same form as the Tenants tab's "+") |
| "Add Bill" | Add Bill form, with the property picker **pre-filtered to exclude paywall-locked properties** |
| "Record Payment" | A dedicated "Record Payment" form (tenant picker → bill type → amount/date/method) — described fully in §3.9 |

### 3.3 Revenue / finance chart card
Title: **"Collected vs. Expense"**. Subtitle: **"Last 6 months, across your whole portfolio"**.

- **Grouped bar chart**, 2 bars per month (Collected, Expense), legend below, gridlines, fixed height (~220pt).
- Colors: Collected uses the app's "gold" accent; Expense uses the primary brand-gradient start color.
- **Empty state**: if all 6 months have zero for both series, replace the chart with a bar-chart icon + **"No rent or bills recorded yet."**

**Computation — exact rules, must be reproduced identically:**
- Covers the **last 6 calendar months**, oldest to newest, each labeled with a 3-letter month abbreviation.
- **Collected (per month)** = sum of `Payment.amount` where:
  1. the payment's `paidDate` falls within that calendar month, **and**
  2. the payment's parent `Bill` still exists (a payment whose bill was deleted is **excluded**, not counted as income), **and**
  3. the parent bill's `paidBy` is `tenant` (or unset, defaulting to tenant) — i.e. **any bill type** can count toward "Collected" as long as it's a tenant-owed bill, not just rent.
- **Expense (per month)** = sum of `Bill.amount` where the bill's `paidBy == landlord` ("Property Expense") **and** the bill's `dueDate` falls within that calendar month — bucketed by **due date**, regardless of whether the bill has actually been paid yet.
- **Critical asymmetry to replicate exactly**: Collected is sourced from the **payments** collection and bucketed by **paid date**; Expense is sourced from the **bills** collection and bucketed by **due date**. These are genuinely different data sources and bucketing rules, not a simplification — do not "fix" this into a symmetric computation, it must match iOS's numbers exactly.

### 3.4 Stat card grid
**Exactly 3 cards currently shown** (a 4th-row-wrapping layout exists in the underlying grid logic to support more cards, but only 3 are active):

| Card | Value | Icon/tint intent |
|---|---|---|
| Properties | Count of all non-deleted properties the landlord owns or co-owns | primary brand tint |
| Tenants | Count of tenants where `isActive == true` (not moved out) | success/green tint |
| Vacant | Total properties minus properties where `occupied == true` | warning/amber tint |

Each card: icon in a tinted rounded-square badge, a large bold numeric value, a small label below. **Not tappable — purely informational, no navigation on tap.**

**Two additional stat computations exist in the codebase but are currently disabled/not rendered as cards** — flagging for awareness, not for Android to build unless product decides to revive them:
- "Occupied" (count of `occupied == true` properties).
- "Pending Dues" (sum of the `amount` of every unpaid bill across the portfolio).

Since the underlying values are still used elsewhere (feeding the Overdue/Upcoming card filters), Android may keep the computations available internally but should **not surface them as visible stat cards** unless explicitly asked — that would not be parity, it would be a net-new addition.

### 3.5 Overdue card
- **Conditionally rendered — entirely absent from the layout (not even an empty placeholder) when there are zero overdue bills.** This is different from the Upcoming card below, which always renders with its own empty state.
- Header: "Overdue" / "Needs immediate attention", warning-triangle icon badge (red), a left accent bar for visual emphasis.
- Lists **up to 4** overdue bills; each row tap → navigates to that Bill's detail screen.
- If more than 4 exist: a **non-tappable** footer line, "+ N more overdue" (not a "View All" link — just a count, no further navigation from here).

**Computation — must be reproduced identically, and is the single most important business rule on this screen:**
- Source: all bills where `status != paid`.
- Filter: `dueDate < start of today`.
- Sort: ascending by due date (oldest/most-overdue first).
- **This uses the due date, not the stored `overdue` status field**, because in practice bills mostly just sit at `pending` past their due date rather than being manually re-flagged by the landlord (see the Bills spec §6 for the full explanation, including how this deliberately differs from the Bills tab's own "Overdue" filter, which *does* rely on the stored status). Android must implement this date-based check exactly as described — do not substitute the stored status field here.

### 3.6 Upcoming Bills card
- **Always rendered**, with its own dedicated empty state (unlike the Overdue card).
- Header: "Upcoming Bills" / "Due in the next 7 days", calendar-clock icon badge.
- **Empty state**: green checkmark icon + "Nothing due in the next 7 days."
- Non-empty: lists **up to 4** upcoming bills, same tap-to-bill-detail behavior as Overdue.
- Overflow: "+ N more due soon" footer text (not a link) if more than 4 exist.

**Computation**: from the same unpaid-bills set, filtered to `today ≤ dueDate ≤ today + 7 days`, sorted ascending by due date.

### 3.7 Shared row component (Overdue & Upcoming cards)
Each row shows: bill-type icon, bill type display name, resolved property name (building name + unit if present, falling back to a generic "Property" label if the property record can't be resolved), amount formatted with the **landlord's account-level currency code**, and a relative due-date label:
- Overdue (days < 0) → "Overdue" (red)
- Due today (days == 0) → "Due today" (amber)
- Due tomorrow (days == 1) → "Due tomorrow"
- Otherwise → "Due in {N}d"
- Color rule: red if the bill is date-overdue **or** its stored status is `overdue`; amber otherwise.

### 3.8 Pull-to-refresh
Triggers a **real, parallel, server-forced refresh** of the Property, Tenant, and Billing services simultaneously (bypassing any local cache) — **not** the Service-Request service, which is left untouched by this pull-to-refresh (its bell-badge count is realtime-listener-driven only).

### 3.9 "Record Payment" quick-action form (from §3.2)
A modal form:
- **Tenant picker**: lists only **active** tenants whose property is **not** paywall-locked; empty message "No active tenants." if none qualify. Selecting a tenant, if the amount field is still empty, pre-fills it from that tenant's property's monthly rent.
- **Property info** (read-only line, once a tenant is selected): unit · building name.
- **Bill Type** — chip row of **all** bill types (not rent-only): selecting Rent re-pre-fills the amount from monthly rent; selecting any other type clears the amount field instead.
- **Amount** (decimal input, placeholder = the property's monthly rent).
- **Payment Date** — date picker, **capped at today** (no future dates).
- **Method** — chip row: Cash / Bank Transfer / UPI / Cheque / Other.
- Submit disabled unless a tenant is selected and amount is `> 0`. On success, records the payment via the same "lazy bill materialization" path described in the Bills spec (§2.4 #10) — i.e., if no bill document exists yet for that period, one is created on the fly, already marked paid.

### 3.10 Loading / empty / error states
- **No screen-level loading spinner or skeleton state** — the Dashboard renders immediately from whatever the shared, already-live listener data currently holds. There is no dedicated Dashboard loading flag consulted anywhere in this screen.
- **No screen-level error banner** exists on the landlord Dashboard at all.
- Empty states exist **only per-section**, as already described: chart → "No rent or bills recorded yet."; Overdue → simply not rendered; Upcoming → "Nothing due in the next 7 days."
- **There is no dedicated "brand-new landlord with zero properties" onboarding screen on the Dashboard tab.** With zero properties, the Dashboard just degrades gracefully: all three stat cards show 0, the chart shows its no-data message, the Overdue card is absent, and the Upcoming card shows its empty message. The **only** genuine first-run empty state in the whole landlord app lives on the **Properties tab** ("No properties yet" / "Add your first property to start managing rent and bills."), not the Dashboard — Android should treat the Dashboard's zero-state purely as the normal layout under degenerate data, not as a distinct designed onboarding screen.

### 3.11 Subscription/Pro status representation
**There is no visible Pro badge, upsell banner, or "you have N locked properties" messaging rendered anywhere on the landlord Dashboard.** Subscription state only affects this screen *functionally*, never visually:
- The property picker inside "Add Bill" (quick action) excludes locked properties.
- The tenant picker inside "Record Payment" (quick action, §3.9) excludes tenants whose property is locked.
- No stat card, hero card, or badge communicates plan status on this screen at all. **If Android wants to surface plan status more prominently on the Dashboard, that would be a net-new enhancement beyond iOS parity — flag it as a product decision, don't build it as if it were an iOS feature being ported.**

---

## 4. Tenant Shell

Structurally much simpler than the landlord side: **no bottom tab bar at all** — a single navigation stack rooted at the tenant's Dashboard, with all other tenant screens (Requests, History, Settings, etc.) reached by pushing onto that one stack.

- One tenant-data-service instance is created for the whole session, started when the shell appears (scoped to the signed-in tenant's ID — which, per the Tenants spec §7, equals their Firebase Auth UID), and torn down when the shell disappears.

---

## 5. Tenant Dashboard Screen

**Title is a personalized, dynamic greeting**: **"Hello, {FirstName}"** (first token of the tenant's name) once loaded, falling back to **"Hi!"** before the tenant record has loaded. **This is the only personalized/greeting element in the entire Dashboard feature set** — the landlord side has none (its equivalent greeting code exists but is deliberately disabled, see §3.1).

Toolbar: a "+" icon (top-trailing) → pushes the Add Request screen; a gear icon (top-leading) → pushes the tenant Settings screen.

### 5.1 Hero card
- Large bold number: the tenant's total amount currently owed across all pending bills (or `"{currency code} 0.00"` if nothing is owed).
- Below it: `"{N} bill(s) outstanding"` if any are pending, else a green checkmark + **"No pending dues"**.
- Same brand-gradient visual treatment as the landlord's hero card.
- (Note: an additional name-based greeting line and a "PENDING DUES" eyebrow label exist as disabled/commented-out code here too — the live personalization is solely the nav-title greeting, not anything inside this card.)

**Total-due computation**: sum of the `amount` field across all bills where `status != paid`, for this tenant's property, **already pre-filtered to exclude landlord-expense bills** at the listener level (tenants never see bills the landlord pays themselves — see the Bills spec §8).

### 5.2 Property card
Shown once the tenant's linked property has loaded:
- Optional photo banner at the top (with a placeholder graphic if no photo, or if the image fails to load).
- Title: `{buildingName} · {unit}` (or just the building name, or the property's base name, whichever is available in that priority order).
- Address line (with a location icon), shown only if non-empty.
- A divider, then a **3-cell metric row**: Monthly Rent (currency-formatted), Property ID, and Property Type (only shown if the property has a type set).
- **No landlord contact information is surfaced anywhere on the tenant Dashboard** — no landlord name, phone, or email appears on this screen. Do not add a "contact your landlord" affordance here unless explicitly requested as a new feature; it doesn't exist on iOS.

**Fallback placeholder** (shown if the tenant record has loaded but the property hasn't resolved yet): a lightweight card — house icon, the tenant's Property ID code, and "Move-in: {date}" (full month-name format).

### 5.3 Content sections (each independently conditional)
1. **Outstanding Bills card** — shown only if pending bills exist. Header "Outstanding Bills" with a red count-badge pill. **Lists ALL pending bills with no cap and no "View All" link** (this is a deliberate difference from the landlord Dashboard's Overdue/Upcoming cards, which cap at 4 — the tenant side shows everything). Tapping a row opens a Bill Detail sheet.
2. **Recent Payments card** — shown only if paid bills exist. Header styled as a small-caps section label ("RECENT PAYMENTS", visually distinct from the Outstanding Bills header). Shows the **top 5 only**; a **"View All" link appears only when more than 5 exist**, navigating to the full Payment History screen. Tapping a row opens the same Bill Detail sheet.
3. **My Requests card** — shown only if the tenant has any service requests. Header "MY REQUESTS". Shows the **top 3 only**; "View All" appears only when more than 3 exist, navigating to the Requests screen.
4. **Error card** — shown if the bills listener has errored: warning-triangle icon, "Couldn't load bills" title, the underlying error message. This card takes priority display placement immediately after the three sections above when present.
5. **Global empty state** — shown only when pending bills, paid bills, **and** requests are **all** simultaneously empty (and there's no error): a checkmark icon, **"All caught up"**, **"No pending bills or dues at the moment."**
6. **Loading state** — while the tenant's own document is still loading, a centered spinner **replaces sections 1–5 entirely**. Important nuance: the **hero card and property card/placeholder are NOT gated by this loading flag** — they can appear before the loading spinner clears, and the loading flag itself flips to "done" as soon as the tenant document loads, **independent of whether bills/property have finished loading yet** (bills/property load asynchronously afterward with no separate loading indicator of their own). Android should replicate this exact sequencing rather than gating everything behind one unified loading flag, to avoid a UX regression where the whole screen waits unnecessarily.

### 5.4 Pull-to-refresh — cosmetic only, not a real re-fetch
The tenant Dashboard's pull-to-refresh performs **no actual network re-fetch** — it's a short (~400ms) artificial delay purely so the pull gesture has a visible completion animation, because the underlying data is already realtime via Firestore listeners and is therefore always current. **This is different from the landlord Dashboard**, whose pull-to-refresh performs a real, parallel, server-forced refresh (§3.8). Android should replicate this distinction: tenant Dashboard pull-to-refresh can be a pure UI affordance with a brief delay, while the landlord Dashboard's must trigger a genuine forced re-fetch. (Note: the tenant's dedicated Payment History screen, by contrast, **does** perform a real server-forced refetch on pull — only the Dashboard's own pull-to-refresh is cosmetic.)

### 5.5 Tenant Bill Detail sheet (shared with Payment History)
Tapping any bill row on the Dashboard opens a modal sheet: large icon, amount (currency-formatted), a status pill (**Paid** green / **Verifying…** amber / **Overdue** red / **Pending** amber — "Overdue" is computed the same date-based way as elsewhere: `status not in {paid, verifying} AND dueDate < today`, not from a separately-stored flag), then detail rows (Period, Due Date, Paid On if applicable, Frequency if set, Notes if present). If the bill is pending or overdue, a **"I've Paid This Bill"** button opens the payment-verification submission sheet (see the Bills spec §5.3). If the bill is in `verifying` status, an info banner replaces the button: "Your payment is being verified by your landlord."

### 5.6 Tenant bill row (shared component)
Icon, bill type name, a subtext line ("Paid {date}" or "Due {date}"), the amount (currency-formatted), and a colored status label (Paid=green, Verifying=amber, Overdue=red, Pending=amber).

---

## 6. Business Logic / Aggregation Reference Table

All of the following are computed **entirely client-side**, over the same live listener arrays the rest of the app uses — replicate the exact formulas, not an approximation:

| Metric | Formula |
|---|---|
| Properties count (stat card) | Count of all non-deleted properties owned or co-owned by the landlord |
| Tenants count (stat card) | Count of tenants where `isActive == true` |
| Vacant count (stat card) | Total properties − properties where `occupied == true` |
| Occupied count (computed, not currently shown as a card) | Count of properties where `occupied == true` |
| Pending dues total (computed, not currently shown as a card) | Sum of `amount` for every bill where `status != paid` |
| Overdue bills list | Unpaid bills where `dueDate < start of today`, sorted ascending by due date — **date-based, ignores the stored status field** |
| Upcoming bills list | Unpaid bills where `today ≤ dueDate ≤ today + 7 days`, sorted ascending |
| Collected (per month, chart) | Sum of `Payment.amount` where `paidDate` falls in that month AND the parent bill still exists AND `paidBy` is tenant (or unset) |
| Expense (per month, chart) | Sum of `Bill.amount` where `paidBy == landlord` AND `dueDate` falls in that month (regardless of paid status) |
| Tenant total due (tenant hero card) | Sum of the tenant's own bills' `amount` where `status != paid` (landlord-expense bills already excluded upstream) |
| Locked property (feeds pickers on both dashboards) | Not Pro AND owned (not co-owned) by the viewer AND the property's building isn't among the oldest 1 (free-limit) building(s) the landlord owns, by earliest `createdAt` |
| Pro status | Priority: admin override flag → active native store entitlement → stored expiry-date-in-future (cross-platform fallback) — see the Settings spec §4.3 |

---

## 7. Navigation Map

```
Landlord Dashboard
 ├─ bell icon (toolbar) → Service Requests screen
 ├─ "Add Tenant" quick action → Add Tenant form (modal)
 ├─ "Add Bill" quick action → Add Bill form (modal, properties pre-filtered to exclude locked)
 ├─ "Record Payment" quick action → Record Payment form (modal, §3.9)
 ├─ any row in Overdue card → Bill Detail screen
 ├─ any row in Upcoming Bills card → Bill Detail screen
 ├─ stat cards → not tappable
 └─ chart → not tappable

Tenant Dashboard
 ├─ "+" toolbar → Add Request screen
 ├─ gear toolbar → tenant Settings screen
 ├─ any bill row (Outstanding Bills / Recent Payments) → Bill Detail sheet
 │        └─ "I've Paid This Bill" → payment-verification submission sheet
 ├─ Recent Payments "View All" (only if > 5 paid bills) → Payment History screen
 ├─ My Requests "View All" (only if > 3 requests) → Requests screen
 └─ Property card → not tappable, no dedicated property-detail screen reachable from the tenant Dashboard
```

---

## 8. Loading / Empty / Error States — Consolidated

| Screen/section | Loading | Empty | Error |
|---|---|---|---|
| Landlord Dashboard (overall) | None — renders immediately from live data | No dedicated empty screen; degrades gracefully per-section | None — no error banner exists anywhere on this screen |
| Landlord Dashboard → chart | — | "No rent or bills recorded yet." | — |
| Landlord Dashboard → Overdue card | — | Card is entirely absent (not a placeholder) | — |
| Landlord Dashboard → Upcoming card | — | "Nothing due in the next 7 days." | — |
| Properties tab (reference, not Dashboard) | — | "No properties yet" / "Add your first property to start managing rent and bills." | — |
| Tenant Dashboard (overall) | Centered spinner, gated only on the tenant document (not on bills/property) | "All caught up" / "No pending bills or dues at the moment." — only when bills, payments, and requests are all empty | Dedicated card: "Couldn't load bills" + the underlying error message |

---

## 9. Explicitly Out of Scope / Not Present (do not build unless newly requested)

- No dedicated Dashboard backend service, aggregation endpoint, or server-computed rollup document — everything is client-derived from the same live data the other tabs use.
- No personalized greeting on the landlord Dashboard (it exists in source but is deliberately disabled — replicate the *absence*, not the dead code).
- No Pro/plan-status badge, upsell banner, or "N locked properties" messaging visible anywhere on either Dashboard.
- No landlord-contact information surfaced on the tenant Dashboard.
- No dedicated first-run/onboarding empty state specific to the Dashboard tab (that only exists on the Properties tab).
- No "Occupied" or "Pending Dues" stat cards currently shown (the computations exist internally but are not rendered — do not add them as new visible cards without a product decision).
- No real network refresh behind the tenant Dashboard's pull-to-refresh gesture (it's cosmetic only — see §5.4 for the one screen where a real refresh *is* expected).
