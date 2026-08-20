# LandLord + BariVara.com — Project Plan

Last updated: 2026-08-21 (Phase 14 done — BariVara.com has its own real
backend now, separate Spring Boot project from LandLord's, public
listing search/favorites/booking + owner property/unit/listing management
all real end to end)

## 1. Product summary

**LandLord** is a property-management platform with two roles: Landlord (owner) and
Tenant. It covers properties/units, tenant lifecycle, rental agreements, billing and
payments, expenses, maintenance tickets, and in-app messaging.

**BariVara.com** is a separate, public-facing rental marketplace where anyone can
browse vacant units and submit booking requests. It is interconnected with LandLord:
vacant units in LandLord auto-post as ads on BariVara, and booking requests on
BariVara sync back into LandLord's Marketplace & Leads module.

Source of truth for flows: `Diagram-drawio-project-R70/*.drawio` (4 activity diagrams —
login, landlord core, tenant core, BariVara).

## 2. Revised build order

Per your direction: **finish the frontend for both LandLord and BariVara.com first**,
including the visual/behavioral connection between them, before starting any backend
work. The plan is now split into two parts:

- **Part 1 — Frontend** (current focus): both apps, fully clickable, mock-data-driven.
- **Part 2 — Backend**: starts only after Part 1 is complete and reviewed.

One honest caveat on "connected": while both apps are frontend-only, they cannot be
*live*-linked at runtime — they're two separate sites with no shared server. Part 1
makes them **contractually connected** (same field names/shapes for units, ads,
requests, shared login pattern, matching design language) and demos the full loop
using each app's own mock data. **Real, live sync (a vacancy in LandLord actually
appearing on BariVara) only becomes real in Part 2**, once there's a shared backend.
This is called out again at the point it matters below.

## 3. Current status

**Phase 1, 2, and 3 are done.** Both apps are functionally complete per the source
diagrams, cross-referenced against each other, and self-reviewed twice (see §3a for
what those reviews found and didn't fix). Chronological detail:

- **Draft 1** (`0a84e70`): LandLord scaffolded — Angular 22 standalone, full
  page/route structure from the 3 LandLord-side diagrams, role-guarded lazy routing,
  `MockDataService` for a working demo with no backend.
- **Draft 2.0**: LandLord pushed past scaffold — monthly per-tenant billing ledger
  (`period`-keyed invoices, never overwritten), centralized bill generation +
  payment application, a landlord-wide Ledger (cash book) with property/date
  filters, tenant financial summary with full billing/payment/maintenance history,
  and nav fixes for previously undiscoverable sibling routes.
- **Phase 2**: BariVara scaffolded as a separate Angular 22 project
  (`BariVara-Angular-Project-R70/`), same conventions. Full route structure for
  guest/tenant/owner/landlord-linked areas, own `MockDataService`. Then rebuilt the
  homepage to commerce-marketplace style against a real reference
  (`pics/rental-site-demo.png`) with real district/area/property-type filtering, and
  gave auth pages a way back to the homepage.
- **Pre-Phase-3 review**: found and fixed 3 correctness bugs — BariVara's
  duplicate-booking bug, `MarketplaceRequest` missing the `tenantId` field
  `BookingRequest` already had, LandLord's dead Repost/Pause buttons.
- **Phase 3**: shared contract file (`shared-contracts.ts`, hand-synced in both
  projects) with boundary DTOs for the future real sync; both apps on fixed dev
  ports (LandLord 4200, BariVara 4201) with three dead stubs turned into real
  cross-app links; both READMEs rewritten; seed data and login pattern checked
  against each other, no drift.
- **Phase 3 self-review**: found and fixed 3 more issues — LandLord's `Unit` gained
  `propertyType` (the new contract required it but nothing could supply it), the two
  cross-app "go look at listings" links now consistently land on `/browse`, and
  Repost/Pause now says plainly it doesn't touch BariVara's copy of the data yet.
- **LandLord marketing homepage** (`bff390d`, ad-hoc addition outside the formal
  phase list — same treatment as BariVara's earlier commerce-homepage rebuild): `/`
  now sells the product instead of redirecting straight to login — hero with two
  prominent CTAs, a feature grid grounded in what's actually built, a dedicated
  callout for the BariVara auto-post reach pitch, repeat CTA banner. Uses real
  styling decisions (blue hero gradient, CTA sizing, orange callout) made ahead of
  an actual Phase 4.1 brand decision, so expect this page to get touched again once
  that happens.

- **Phase 4**: design tokens refreshed in both apps (Inter, tightened palette, real
  shadow/type scale), layout components get hover/press polish, LandLord's homepage
  hero rebuilt with a grounded product-preview instead of decoration, CSS-reflow
  responsive pass (not a full interactive mobile nav — see 4.4 notes), both apps
  confirmed as one family/two skins. Deliberately built against an "avoid looking
  AI-generated" constraint — restrained gradients, no glowing shadows, grounded
  content over abstract decoration.
- **Real logos** (both apps, after you supplied the SVGs): reusable `<app-logo>`
  component per app picks the light or dark-background mark depending on where it's
  placed, wired into every topbar/auth-card/sidebar spot in both apps. LandLord uses
  Nunito for its badge-style mark, BariVara uses Poppins for its pin+house mark —
  both loaded alongside each app's Inter body font.
- **Utility charges** (LandLord): stress-tested the app against a concrete scenario
  (a landlord with one 12-unit building doing this by hand in a notebook) and found
  a real hole — bills had no way to include water/gas/service-charge amounts,
  `utilities` was hardcoded to 0. Fixed with a per-unit configurable list
  (`UtilityItem[]`, label + usual monthly amount, landlord's choice of what applies)
  instead of a fixed set of fields — deliberately not a hardcoded "electricity"
  line, since most city units now have prepaid meters (tenant recharges directly,
  not the landlord's bill) while older buildings or other charges still vary.
  Snapshotted onto each invoice at generation time (`InvoiceUtilityLine[]`) so
  editing a unit's defaults later never rewrites past bills; the current month's
  amounts stay correctable up until the tenant pays anything against that bill,
  then lock like the rest of the invoice.
- **LandLord deep-dive** (`9bd228a`, `5f627e0`, `8c470e9`) — kept stress-testing
  against the "one landlord, real monthly bookkeeping" scenario and fixed what it
  found:
  - **Total due is dynamic now.** Monthly Bills and tenant Billing history were
    showing the frozen original `amount` even after a partial payment landed;
    both now show `balance` (or `remaining/original` while partial), and the
    `partial` status badge got its own yellow/amber instead of sharing `unpaid`'s
    red — the two were visually indistinguishable before.
  - **Unit dropdown on tenant registration shows the property name** (`Prop >
    Unit > Rent/mo`) — was unit-number-only, ambiguous the moment a landlord has
    more than one building.
  - **`TenantRecord.nationalId` added as the real unique identifier**, with
    registration blocking a duplicate NID against any other *active* tenant
    (inactive/moved-out tenants don't block re-registration). Name and phone
    alone don't reliably distinguish tenants past a handful of them.
  - **Tenant Management gained search** (name/NID/phone, reactive) and a status
    filter (Active/Inactive/All, defaults to Active) — directly enabled by NID
    finally being a real thing to search by; closes the vague "no search" item
    that had been sitting in §3a.
  - Fixed a real CSS bug along the way: `.search-bar` was never defined in
    LandLord's own stylesheet (only in BariVara's) — the class name got reused
    without porting the rule, so the search box and filter dropdown had no gap
    between them at all.
  - **Landlord dashboard gained 5 KPI stat cards** (Occupancy, Collected this
    month, Outstanding this month, Net this month, Pending maintenance) above
    the module grid — all derived from data already tracked, added as reusable
    `MockDataService` methods rather than computed inline. Deliberately no chart
    yet — not enough historical data in the seed to make a trend meaningful;
    revisit once the app has real usage history behind it.
  - Dashboard copy: "Choose a module" → "Manage your property" (sounded like
    software jargon, not something a landlord would think). Tenant dashboard's
    matching "Choose an action" heading is still open — flagged, not yet decided.
- **Professional footer, both apps** — public pages only (homepage/browse/
  listing-detail; deliberately not inside the sidebar dashboards, which don't
  carry marketing footers). Logo + tagline, real links only (no dead links to
  pages that don't exist — LandLord: Log in/Sign up/BariVara.com; BariVara:
  Browse/Log in/Sign up/a line back to LandLord for owners), copyright line,
  and a "Built by Araf" creator credit (plain text, no link, by your choice).
- **Sidebar layout bug fixed, both apps (all 5 sidebar layouts).** `.app-shell`
  used `min-height: 100dvh` instead of `height: 100dvh; overflow: hidden`, so it
  grew to match tall page content instead of staying pinned to the viewport —
  the sidebar (and the Logout button inside it) scrolled along with the page.
  Fixed, plus a `min-height: 0` flexbox fix on `.main-area` so the content pane
  can actually shrink and scroll on its own instead of forcing the whole page
  to grow.
- **Mobile nav rebuilt as a hamburger/off-canvas drawer**, replacing the
  horizontal-scrolling strip from 4.4. Found along the way that the old mobile
  nav hid the logo *and the Logout button* entirely (`display:none`) — no way
  to log out from a phone. Now: topbar shows a hamburger icon at ≤860px,
  tapping it slides in the full vertical nav (logo, links, logout) as an
  overlay with a dismissible backdrop; tapping a link auto-closes it. Same
  shared CSS classes as the desktop sidebar, so one CSS fix per app covers all
  5 sidebar layouts (landlord, tenant ×2, owner, landlord-linked).

- **Phase 5 QA pass** (route-link audit + static responsive audit, both apps):
  zero dead links/broken routerLinks found across either app; found and fixed
  one real gap — a `.table-scroll` CSS utility existed but was never applied,
  so all 20 tables across both apps (16 LandLord + 4 BariVara) would have
  overflowed with no scroll affordance on narrow screens, now wrapped. Wrote
  `DEMO-SCRIPT.md` (repo root) — a 12-step full-loop walkthrough script using
  the two apps' matching seed data, for your review. Caveat: this QA pass was
  done by static code/route audit, not a live browser click-through (no
  browser tool in this environment) — see Phase 5.1/5.2 notes below.

**Part 2 backend work has started ahead of formal Phase 6/7/8 order** (Property
slice only, as a proof-of-pipe before committing to the full schema):
- **Local Postgres via Docker** — official `postgres:16` image, `docker-compose.yml`
  at repo root, `landlord_db` database, persisted named volume. Docker Engine
  installed via the official `download.docker.com` apt repo (Debian 13/trixie).
- **`landlord-backend/`** — new Spring Boot 4.1 project (Java 21, Maven wrapper),
  generated via the official Spring Initializr (`start.spring.io`), deps: Web,
  Data JPA, PostgreSQL driver, Validation. Runs on `:8080`, CORS opened for
  `:4200`/`:4201`.
- **`Property` entity, real end to end** — full CRUD (`GET/POST/PUT/DELETE
  /api/properties`) backed by an actual `property` table in `landlord_db`
  (`ddl-auto: update` for now, no Flyway yet). LandLord's Properties page
  (`property-list.component.ts`, `property-form.component.ts`) now calls this
  API via a new `PropertyApiService` (`HttpClient`) instead of `MockDataService`
  — add/edit/delete a property and it survives a server restart and browser
  refresh, proven by direct `psql` check against the container.
- **Property edit + delete added** (closes part of the §3a "no property
  edit/delete" gap) — real backend-backed for Property.
- **`Unit` entity, real end to end** — full CRUD (`GET/POST/PUT/DELETE
  /api/units`, `GET /api/units?propertyId=`) backed by a real `unit` table,
  FK'd to `property` via `propertyId`. `unit-list.component.ts` and
  `unit-form.component.ts` now call a new `UnitApiService` instead of
  `MockDataService`. Property and Units are now on the same real DB, so
  "units per property" counts on the Properties page work again (reads from
  `UnitApiService`, not the mock). Kept deliberately minimal like the
  Property slice — dropped `propertyType` and per-unit utility-charge line
  items from the form for now (backend entity is just
  `propertyId`/`unitNumber`/`rent`/`status`); can be added back in a later
  pass if needed.
- **`Tenant` + `RentalAgreement`, registration slice real end to end** — real
  `tenant` and `rental_agreement` tables. `POST /api/tenants/register` does
  the full walk-in flow atomically (create tenant, create agreement, flip the
  unit to `occupied` via the real `unit` table), `GET /api/tenants/active-by-nid`
  backs the duplicate-active-NID check server-side (409 on conflict, matches
  the old client-side check). `tenant-list.component.ts` (search/filter) and
  `tenant-register.component.ts` now call a new `TenantApiService` plus the
  existing `PropertyApiService`/`UnitApiService` for the vacant-unit picker.
  **Deliberately scoped narrow**: `tenant-detail.component.ts` and
  `tenant-moveout.component.ts` stay on `MockDataService` for now — they pull
  billing/payment/maintenance history, none of which has a backend yet
  (Phase 10/11), so migrating just the tenant record there would leave those
  tables silently empty. Your call when we get there: migrate detail/moveout
  now (empty history tables) or wait until billing has a backend too.
- **`Invoice` + `Payment`, minimal billing engine, real end to end** —
  `POST /api/invoices/generate` (rent from the real `unit` table + a passed-in
  utilities total + prior-unpaid rollover pulled from that tenant's own
  non-paid invoices), `GET /api/invoices?tenantId=`, `POST /api/payments`
  (applies to invoice balance, flips status unpaid → partial → paid),
  `GET /api/payments?tenantId=`, `GET /api/tenants/{id}/outstanding-balance`.
  Deliberately simplified vs. the mock version: no per-line itemized utility
  snapshot (`InvoiceUtilityLine[]`), just a single `utilitiesTotal` number —
  matches the earlier call to drop itemized utility charges from the real
  `Unit` entity too. No scheduled auto-generation yet (10.3), no receipt PDF
  (10.5), no payment gateway (10.8) — this is bill math + ledger state, not
  the full billing UX.
- **Move-out flow, real end to end** — `POST /api/tenants/{id}/move-out` sums
  that tenant's outstanding invoice balances, flips their unit back to
  `vacant`, sets the tenant `inactive` and clears `unitId`.
  `tenant-moveout.component.ts` now calls this plus the new
  `BillingApiService` for the live outstanding-balance figure and
  `TenantApiService.agreementFor()` for the deposit; refund/final-bill math
  stays client-side (unchanged from before). Damage-deduction reconciliation
  and deposit refund are still just a displayed number, not an actual
  transaction of any kind (no payment-gateway integration exists to make a
  "refund" real). Closes Phase 9.1/9.2's move-out gap.
- **`tenant-detail.component.ts` migrated off `MockDataService`** — tenant
  record, agreement (with a new `PUT /api/tenants/{id}/agreement` for editing
  terms), billing history, and payment history all now hit the real
  `TenantApiService`/`BillingApiService`. Total due/paid computed client-side
  from the real invoice/payment lists. Maintenance cost/history stays on
  `MockDataService` — no maintenance backend exists yet (Phase 11) — and is
  keyed by the mock string tenant IDs, so it'll just render empty for real
  (numeric-ID) tenants until that slice is built.
- **Billing views migrated** — `generate-bills.component.ts` ("Monthly
  Bills") now lists real invoices for the current period only (matches the
  backend's current-month-only generation, no cron yet) and bulk-generates
  for any active, unit-assigned tenant missing one, via a new
  `GET /api/invoices` with optional `period` filter (previously required
  `tenantId`). Dropped the itemized utility-line edit UI — real `Invoice`
  only has `utilitiesTotal`, no per-line breakdown, matching the earlier
  simplification. `receive-payment.component.ts` now calls the real
  `POST /api/payments`, applying the entered amount across that tenant's
  unpaid invoices oldest-first (backend has no single "pay whatever's owed"
  endpoint). `pending-cash.component.ts` stays on mock — the real payment
  flow has no pending→confirm step (10.4 already simplified to
  straight-to-confirmed), so there's nothing on the backend for it to call.
- **`pending-cash.component.ts` retired** — real `Payment` always writes
  straight to `confirmed` (no pending state exists server-side, matches
  10.4's simplification), so the landlord "Pending Cash" tab/route/page had
  no backend to call and was deleted outright (not migrated). Tenant-side
  `pay.component.ts`'s "Pay with cash" button updated to match: records
  straight to `confirmed` instead of a `pending` status with no confirm UI
  left anywhere to clear it.
- **`Maintenance` + `Expense`, real end to end** — new backend package
  (`MaintenanceTicket`: unitId/tenantId/description/status/**cost**;
  `Expense`: propertyId/**ticketId**/category/description/amount/bearer/
  tenantId/date — the real `ticketId` back-reference the mock never had).
  `GET/POST /api/maintenance-tickets` (+`?tenantId=`/`?unitId=`),
  `PUT .../{id}/status` (resolve, auto-creates a linked `Expense` when a
  cost is given, resolving `propertyId` server-side via the ticket's unit),
  `GET/POST/DELETE /api/expenses`. New `MaintenanceApiService` wired into
  all 6 mock-era components: landlord ticket-list/ticket-new/ticket-detail,
  tenant ticket-list/ticket-new, `expense-management.component.ts`. Landlord
  dashboard's pending-maintenance count and net-this-month tile now read
  real data too. Closes the exact gap flagged earlier: `tenant-detail.component.ts`'s
  maintenance-cost/history cards now call the real API instead of matching
  mock string IDs against real numeric tenants (previously always empty).
  **Tenant-side "current user" stopgap**: real auth (Phase 7) still isn't
  built, so tenant-side maintenance pages needed a real numeric tenant id to
  act as — added `CURRENT_TENANT_ID_REAL = 3` (`core/current-tenant.ts`),
  same role as the mock's `CURRENT_TENANT_ID`, explicitly commented as
  throwaway until Phase 7 lands.
  **Deliberately not touched**: `ledger.component.ts` — its `ledgerEntries()`
  fuses expenses with payments *and* properties in one mock method; swapping
  only the expense side would leave it half-real, half-mock inside a single
  computed. Needs its own pass once Payments/Property are wired into the
  Ledger view together, not a Maintenance-scoped fix.
  **Ledger migrated, real end to end (2026-08-19)**: `GET /api/payments`'s
  `tenantId` param made optional (falls back to `findAll()`, same pattern as
  `GET /api/invoices`), plus a new `BillingApiService.allPayments()`.
  `ledger.component.ts` no longer touches `MockDataService` — loads
  properties/units/tenants/payments/expenses via the real APIs in `ngOnInit`,
  resolves each payment's property through tenant→unit→property (falling
  back to `TenantApiService.agreementFor()` per tenant whose `unitId` is
  currently null, e.g. after move-out, matching the mock's old fallback
  logic), and builds the same income/expense merge client-side.
  **Landlord dashboard KPI tiles migrated too (2026-08-20)**:
  `dashboard.component.ts` no longer injects `MockDataService` at all —
  Occupancy now reads `UnitApiService`, Collected reads real confirmed
  `Payment`s for the period, Outstanding reads real unpaid/partial
  `Invoice` balances for the period (`BillingApiService.invoicesForPeriod`,
  already existed), Net stays derived from Collected minus the
  already-real expenses total. Only kept `periodKey`/`periodLabel` (pure
  date-formatting helpers) from `mock-data.service.ts`.
  **Real bug found and fixed (2026-08-20), caught live by you**: a payment
  recorded the same day didn't show in the Ledger even though it showed
  fine under the tenant's Payment history. Root cause: `Payment.date` is
  an `Instant` (serializes with a full timestamp,
  `"2026-08-20T13:00:00Z"`), but `Expense.date` is a `LocalDate`
  (date-only, `"2026-08-20"`) — the date-range filter did a plain string
  comparison against `toDate()` (also date-only), and a same-day
  timestamp lexically sorts *after* the bare date, so it silently
  dropped out of range. Fixed by truncating `p.date` to `yyyy-MM-dd` in
  `ledger.component.ts` before building the income entry.
- **Real bug found and fixed: numeric `<select [(ngModel)]>` silently failed
  to sync the picked option back to the bound property.** Surfaced when you
  screenshotted "Log new issue" — dropdown visually showed a tenant selected,
  but the button did nothing because the bound `tenantId` was still `null`,
  so the (at-the-time-silent) guard clause in `save()` just returned. Root
  cause: `<option [value]="t.id">` inside a `[(ngModel)]`-bound `<select>`
  wasn't reliably round-tripping the numeric value on the `change` event.
  Fixed in all three forms built with this exact pattern by replacing
  two-way `ngModel` on the `<select>` with an explicit `(change)` handler
  that reads `event.target.value` and does the `Number()` conversion itself
  — `ticket-new.component.ts` (landlord maintenance),
  `expense-management.component.ts` (both the property and tenant
  dropdowns), and `tenant-register.component.ts` (unit-assignment dropdown
  — this one had no placeholder option either, so the browser was
  auto-showing the first vacant unit as selected while `unitId` stayed
  `null`, meaning **real tenant registrations were likely silently failing**
  whenever a landlord didn't happen to manually reselect the unit). All
  three also gained a visible error message instead of a silent no-op when
  validation fails.
  **Full sweep done (2026-08-19)**: grepped every `<select [(ngModel)]>` in
  both apps. Found a 4th real instance — `receive-payment.component.ts`'s
  tenant dropdown, same numeric-`[value]` pattern, same fix applied
  (explicit `(change)` handler + visible error on the silent guard clause).
  Every other select in both apps binds to a string-typed model (category,
  payment method, bearer, status, role, district/area/property-type,
  BariVara's `selectedUnitId`) — those compare natively as strings and
  aren't affected; only numeric-id selects wired to the real backend carry
  this bug. Confirmed fixed via rebuild; not yet re-verified in a live
  browser click-through on your end.
- **Real bug found and fixed: Expenses' free-text Category field silently broke
  `tenant-detail.component.ts`'s Maintenance cost history.** You confirmed "Log
  new issue" worked, then found a logged expense wasn't showing under a
  tenant's Maintenance cost history — root cause: the Category input on
  `expense-management.component.ts` was free text, but the tenant-detail page
  only matches `category === 'Maintenance'` exactly (case-sensitive). An
  expense typed as "Repair" (or any other spelling) could never show up.
  Fixed: Category is now a dropdown (Maintenance/Repairs/Utilities/Other,
  defaults to Maintenance), plus visible errors on the save guard clauses.
  Also corrected one real pre-existing expense row in the DB (id 4, "He broke
  the switch.", tenant 4) from `category: 'Repair'` to `'Maintenance'` so it
  retroactively shows up. Confirmed working live in the browser on tenant
  Atiqur Rahman's detail page (400 tenant-borne maintenance cost, correct).
- **Messaging + notifications (Phase 12), LandLord only, real end to end
  (2026-08-20)** — `Conversation` gained `tenantId`, `Message` gained
  `senderRole` (landlord/tenant) + `read`, `Notification` gained `tenantId`
  + `type` (entities existed bare-bones since Phase 6.2, now filled in). New
  `MessagingController` (`GET/POST /api/conversations`, `GET/POST
  /api/conversations/{id}/messages`) and `NotificationController`
  (`GET/POST /api/notifications`, `PUT .../{id}/read`, `DELETE .../{id}`).
  New `MessagingApiService`; landlord + tenant message-list/thread and
  tenant notifications all migrated off `MockDataService`. Thread pages
  poll every 10s (chosen over WebSocket — light two-user demo traffic
  doesn't justify socket infra yet, revisit at Phase 16 if that changes).
  Landlord's `tenant-detail.component.ts` gained a **"Message tenant"**
  button (finds or creates that tenant's conversation, navigates to it) —
  **closes the §3a "no start a new conversation anywhere" gap.**
  **Scope call, your decision**: BariVara messaging left on mock — no
  BariVara backend exists yet (Phase 14 not started), wiring it now would
  mean building a whole separate backend early, out of order. BariVara's
  and the LandLord marketplace's decorative "Send" chat stubs are
  unaffected, still stubs.
  **Verified live**: curl round-trip (create conversation → send both
  directions → mark notification read → delete) plus a real message sent
  landlord→tenant and confirmed visible on the tenant dashboard — but only
  after finding out *why* the first test message didn't show (see gap
  note below).
  **Gap surfaced, not a bug**: first test message was sent to tenant id 5,
  but tenant-side pages are hardcoded to `CURRENT_TENANT_ID_REAL = 3`
  (`core/current-tenant.ts`, Phase 7 auth stopgap, same known issue already
  tracked in §3a) — so it never appeared for whoever "logged in" as tenant.
  Not a messaging bug; expected until real auth lands. Resent to tenant 3
  ("T1") to confirm the feature itself works, and it does.
- **Unit photo upload (Phase 8.4), real end to end (2026-08-20)** — same
  local-disk pattern as Maintenance ticket photos (Phase 11.4). Real `Unit`
  gained `photoUrl`; `POST /api/units/{id}/photo` (multipart) saves to
  `uploads/units/{id}/`, served back at `/uploads/**` (existing generic
  `WebConfig` handler reused as-is, no changes needed there). `unit-form.
  component.ts` gained a file picker (uploads right after create/update,
  same create-then-upload sequencing as the tenant maintenance-ticket-new
  flow); `unit-list.component.ts` gained a photo thumbnail column instead
  of "No photo" text. Verified live: uploaded a real PNG to a real unit via
  curl, confirmed it persisted (`GET /api/units/3` returned the new
  `photoUrl`) and served back correctly (`/uploads/units/3/...` → 200).
  Test photo removed afterward so demo data stays clean.
  **Scope note**: this is the Unit/LandLord side only. The §3a "listing/
  property cards show no images" gap was really about BariVara's browse/
  listing cards — those still show the placeholder because BariVara has no
  backend yet (Phase 14) to read this `photoUrl` from. `Property` itself
  (as opposed to `Unit`) still has no photo field either — LandLord's own
  property list is a data-forward table, not a photo card, by the Phase 4.6
  design decision, so no placeholder existed there to begin with.
- **BariVara.com now has its own real backend too (Phase 14, 2026-08-21)** —
  `barivara-backend/`, a separate Spring Boot 4.1 project from
  `landlord-backend/` (own `barivara_db` schema in the same Postgres
  container, port 8081). Real: public listing search/filter, favorites,
  booking requests (submit/approve/reject, approval flips listing to
  `taken`), and the owner's property/unit/listing management. Still mock on
  BariVara's side: messaging, notifications, tenant profile, landlord-linked
  dashboard (no backend built for any of these yet — see Phase 14.4 note).
  Full detail in Phase 14 below.
- **Everything else still on `MockDataService`** (LandLord app) — Property,
  Units, Tenant, Billing/Move-out, tenant-detail/billing-views,
  Maintenance/Expenses, Ledger, landlord dashboard KPI tiles, Messaging/
  Notifications (LandLord only), and Marketplace are all real; the rest of
  the tenant-side payment pages are not. Auth is still a stub (both backends'
  APIs wide open), no Flyway migrations yet, nothing deployed anywhere
  (localhost only).

**Not done:** Phase 5.5 sign-off (yours to give), the rest of the real backend
(auth, real cross-system sync, BariVara's own messaging/notifications, etc. —
Phase 7, 15-20), testing, deployment. See
§3a for a tracked backlog of smaller gaps found during review but deliberately
not fixed (property/unit edit-delete entry below is now resolved).

## 3a. Known gaps — reviewed, tracked, not yet fixed

Found during a full review pass across both apps (2026-08-07). Not blocking Phase 3;
listed here so they're not lost, with a note on which phase naturally absorbs each.

- ~~**No property edit or delete, either app.**~~ **Resolved for LandLord**
  (2026-08-16), ahead of schedule, alongside the Property backend slice —
  real edit/delete now backed by Postgres. BariVara's owner side still add-only
  (unaffected, no backend there yet).
- ~~**BariVara owner side has no unit-management page.**~~ **Resolved (2026-08-21)**
  — frontend-only, backend (`OwnerUnitController`) already had `GET/{id}`, `PUT`,
  `DELETE`; `OwnerUnitApiService` only exposed `create`/`loadForProperties`. Added
  `get`/`update`/`delete`, plus new `unit-list.component.ts` /
  `unit-form.component.ts` under `features/owner/properties/`, routed at
  `/owner/properties/:propertyId/units[/new|/:unitId/edit]`, mirroring LandLord's
  own unit-management pattern (no photo/status fields — real `OwnerUnit` entity
  doesn't have them, unlike LandLord's `Unit`). Property list gained a
  "Manage units" link per row. Property creation's bundled first-unit flow
  (`property-form.component.ts`) left untouched.
- **No "start a new conversation" anywhere.** Messaging only works by replying
  inside conversations that already exist in seed data — no compose/new-thread entry
  point from a listing, tenant record, or applicant profile. (Natural fit: Phase 12,
  when messaging gets wired to a real backend anyway — but could be done sooner if
  it undermines demos.)
- **Single hardcoded demo user per role.** `CURRENT_TENANT_ID` / `CURRENT_OWNER_ID`
  mean login always lands on the same mock person regardless of email typed — can't
  demo "two different tenants" without editing code. (Resolved naturally by Phase 7,
  real auth.)
- **No data persistence across page reload.** In-memory signals reset on F5. Known
  demo trap — mention before any live walkthrough. (Resolved by Phase 6/8+, real
  backend.)
- **LandLord's "Send" chat stub (Marketplace & Leads → request detail) and
  BariVara's equivalent are still decorative — no handler.** Left as-is because
  fixing it properly means building "start a new conversation" first (see above),
  not just wiring a button.
- **BariVara listing/property cards still show no images** — every listing card
  shows a gray "No photo" placeholder. LandLord's real `Unit` now has a real
  `photoUrl` (Phase 8.4, done 2026-08-20), but BariVara has no backend yet
  (Phase 14) to read it from, so its cards stay on mock data with no photo
  field at all. Resolved once Phase 14 wires BariVara to the real API (or
  Phase 15 syncs the photo across via `VacancyAdSync`, which doesn't carry a
  photo field yet either — add it then).
- **Tenant-side real-backend pages now depend on a hardcoded `CURRENT_TENANT_ID_REAL`**
  (`core/current-tenant.ts`, set to tenant id `3`) instead of a real login session —
  same shape as the older mock `CURRENT_TENANT_ID` gap, just now also true for the
  real API. (Resolved by Phase 7, real auth — replace every usage then.)
- ~~**Tenant id 3's `unitId` (6) doesn't match any real `unit` row**~~ **Fixed
  (2026-08-19)** — direct DB correction: reassigned tenant 3 to real unit 3
  (`A2`, same property 6), flipped that unit's status to `occupied`. Was
  orphaned data from earlier property/unit CRUD testing, not caused by the
  Maintenance slice but surfaced by it (expenses would resolve `propertyId`
  to `null`).

## 4. Part 1 — Frontend (both apps)

### Phase 1 — LandLord core frontend foundation ✅ DONE
1.1. Read and map all 4 activity diagrams to a page/route list
1.2. Scaffold Angular app structure (core, layouts, features)
1.3. Auth pages (login, signup, forgot/reset password)
1.4. Landlord pages (dashboard + 8 modules)
1.5. Tenant pages (dashboard + 7 modules)
1.6. Role guard + lazy routing wiring
1.7. Shared design system CSS (neutral placeholder)
1.8. Mock data service for a working demo without a backend
1.9. Build verification

*(Since Phase 1 "done": monthly billing ledger, cash-book Ledger page, tenant
financial summary, maintenance-cost tracking, and nav fixes were added — this app is
now functionally deeper than a route scaffold.)*

### Phase 2 — BariVara.com app decision + scaffold ✅ DONE
2.1. ~~Decision needed~~ **Decided: separate Angular project**,
     `BariVara-Angular-Project-R70/`, sibling to the LandLord app.
2.2. `ng new` scaffold for the BariVara app (same Angular version/config conventions
     as LandLord app) ✅
2.3. Map `BariVara_activity_v2.drawio` to a full page/route list (guest browse,
     login/signup reusing the login pattern, favorites, booking, three role
     dashboards: Tenant / Apartment Owner / LandLord-linked) ✅
2.4. Build routing: guest area, auth area (mirrors LandLord's), tenant dashboard,
     apartment-owner dashboard, landlord-linked dashboard ✅
2.5. Guest pages: homepage, search/filter listings, listing detail ✅
2.6. Tenant-role pages: search, favorites, booking request, notifications, messages,
     profile ✅
2.7. Apartment-owner-role pages: add property/unit, post ad (manual or auto-fill),
     manage listings, requests inbox, messages ✅
2.8. LandLord-linked dashboard: read-only synced-ads view + "manage in LandLord core"
     redirect stub ✅
2.9. Own local `MockDataService` for BariVara, field-for-field matching the LandLord
     one where concepts overlap (unit, rent, status, applicant, request) — so the two
     mock stores are structurally identical even though not runtime-linked yet ✅
2.10. Build verification for the BariVara app ✅ (clean build, dev server smoke-tested
      on a representative route per area)

### Phase 3 — Frontend "connection" between the two apps ✅ DONE
3.1. `src/app/core/shared-contracts.ts` added to both projects, hand-kept in sync.
     Moved the concepts already identical (`Conversation`, `AppNotification`) there;
     added the boundary-crossing DTOs Phase 15 will actually need
     (`VacancyAdSync`, `BookingRequestSync`, `UnitStatusSync`) so that phase maps
     onto an already-defined wire shape instead of inventing one under pressure ✅
3.2. Seed data checked — already told one coherent story from the original scaffold
     (LandLord's vacant unit A-102/Green View Apartments/৳14,000 matches BariVara's
     landlord-linked listing exactly); no drift found ✅
3.3. Login pattern checked — both apps' login/signup are structurally identical
     (same form, error handling, layout), differences are only the expected
     per-app role sets; no drift found ✅
3.4. Cross-app navigation is real now: BariVara fixed to port 4201, LandLord stays
     on default 4200, so `npm start` in both runs them side by side with no flags.
     `cross-app.config.ts` in each holds the other's dev URL. Three stubs now real
     links (open the other app's dev site in a new tab): LandLord's ad-management
     "Live on BariVara.com", tenant browse-transfer's "Search all listings on
     BariVara.com", BariVara's landlord-linked "Redirect to LandLord core" ✅
3.5. Both READMEs rewritten from ng-new boilerplate: what the app is, how to run it
     alongside the other one, what's mock vs. real, pointer to §3a known gaps ✅

### Phase 4 — Visual design pass (both apps) ✅ DONE (scoped — see notes)
4.1. Brand direction decided: blue (LandLord) / orange (BariVara) — formalized from
     what was already in place, not arbitrary (matches the source diagrams' own
     color-coding). Inter for type, both apps. ~~Text wordmark only, no logo
     icon~~ — superseded: real SVG logos were supplied later and integrated (4.7).
     Explicit constraint carried through the whole pass: avoid the common
     AI-generated-landing-page tells (gradient-everything, generic marketing copy,
     glowing shadows, mismatched icons, empty centered "template" layouts) ✅
4.2. Design tokens refreshed in both `styles.css`: Inter loaded via `index.html`,
     tightened color/border palette, a real shadow scale (`--shadow-sm`/`--shadow-md`,
     soft and single-direction, not glowing), a type scale with deliberate
     size/weight/letter-spacing per heading level instead of browser defaults ✅
4.3. Layout components refined: sidebar/topbar/tab hover transitions, buttons get a
     tactile press state, cards/module-tiles/listing-cards get hover-lift +
     shadow-md. LandLord's homepage hero rebuilt as a two-column layout with a
     grounded product-preview card (a stylized real ledger snippet — actual numbers,
     actual badges — instead of abstract decoration). Because both apps share one
     CSS file per project, this cascades to nearly every page automatically rather
     than needing per-component edits ✅
4.4. Responsive pass — CSS reflow: hero/search bar wrap and resize, `.table-scroll`
     utility added for wide tables ✅. Mobile sidebar nav originally shipped as a
     horizontal scrolling strip (not a full interactive nav); later replaced
     ad-hoc (post-Phase-4, see §3 log) with a proper hamburger/off-canvas drawer
     once it was flagged as a bad mobile experience ✅
4.5. Empty states — already existed page-by-page (`@empty` blocks with hint text)
     from earlier phases, now inherit the refreshed styling for free. Loading/error
     states are **not applicable yet** — all data is synchronous mock data, there is
     no async operation that could show a loading spinner or a network error. Real
     candidates once Phase 6+ wires up an actual API ✅ (empty states) / N/A (loading/error)
4.6. Visual consistency check: both apps now share the exact same token structure,
     type scale, shadow system, and component shapes — same family, distinct skins
     (blue/professional vs. orange/marketplace) purely through the accent color and
     BariVara's photo-forward listing cards vs. LandLord's data-forward tables ✅
4.7. Real logos integrated (both apps), supplied by you after the initial "text
     wordmark only" call in 4.1. Reviewed all 3 first-round SVGs against the design
     system (found: Poppins dependency not yet loaded, BariVara initially had no
     dark-background variant — both since resolved). Built a reusable `<app-logo
     theme="light"|"dark">` component per app (inlined SVG, not an asset file — both
     under 2KB) so each layout just declares which background it sits on instead of
     duplicating markup. LandLord: badge-style mark (Nunito 800), wired into
     public-layout topbar + auth-card (light) and landlord/tenant sidebars (dark).
     BariVara: pin+house icon mark (Poppins), wired into public-layout topbar +
     auth-card (light) and tenant/owner/landlord-linked sidebars (dark) — the
     dark-variant gap from the review is closed now that the asset exists ✅

### Phase 5 — Frontend QA & demo readiness ✅ DONE (5.1–5.4) — 5.5 needs you
5.1. **Done, but by static audit, not a live click-through** — I have no browser
     tool in this environment, so instead of clicking every branch by hand I
     extracted the full route tree from both apps' `*.routes.ts` files and
     cross-referenced every `routerLink`, `[routerLink]`, `router.navigate(By)Url`
     call, and role-guard redirect against it. Zero dead links, zero unmatched
     targets, in either app. This catches broken *wiring*; it does not catch
     things only a real click-through would (visual glitches, an interaction
     that silently does nothing). Worth a manual pass on your end before 5.5.
5.2. **Done, scoped to a static CSS audit** (same browser-tool limitation as
     5.1): confirmed viewport meta tag present both apps, single 860px
     breakpoint covers the sidebar drawer + hero, `.module-grid`/`.listing-grid`
     use `repeat(auto-fill, minmax(...))` so they reflow at any width without
     needing extra breakpoints. Found one real gap this way: a `.table-scroll`
     utility class existed in both stylesheets but was never applied to a
     single `<table>` — all 16 tables (LandLord) + 4 (BariVara) would have
     overflowed with no scroll affordance on narrow screens. Fixed: every
     `<table>` in both apps now wrapped in `.table-scroll`. Real
     cross-*browser*-engine testing (Chrome vs. Safari vs. Firefox rendering
     differences) is still unverified — no tool access to actually run one.
5.3. **Done** — nothing to fix beyond the table-scroll gap above; the routing
     audit in 5.1 came back clean.
5.4. **Done** — `DEMO-SCRIPT.md` (repo root) written: a 12-step walkthrough
     using the two apps' matching seed data (LandLord's unit A-102/Green View
     Apartments/৳14,000 ↔ BariVara's landlord-linked listing of the same unit)
     covering vacancy → ad → booking request → approval → tenant registration,
     plus optional side branches (utility charges, partial payment, move-out,
     mobile drawer) and a short list of known gaps to mention if asked.
5.5. **Checkpoint: get your sign-off before starting Part 2** — open. Suggest:
     skim `DEMO-SCRIPT.md`, run through Part A–C yourself in an actual browser
     once (covers the click-through gap noted in 5.1/5.2), then say go.

## 5. Part 2 — Backend (starts after Part 1 sign-off)

### Phase 6 — Backend foundation
6.1. ~~Choose backend stack + hosting~~ **Decided: Java + Spring Boot, PostgreSQL.
     No hosting yet — localhost-only for now.**
6.2. ✅ **Design relational schema: `properties`, `units` (Phase 8), `tenants`,
     `rental_agreements` (Phase 9), `invoices`, `payments` (Phase 10), `expenses`,
     `maintenance_tickets` (Phase 11), `conversations`, `messages`,
     `marketplace_requests`, `notifications` — all entities + repositories now in
     `landlord-backend/` (`messaging`, `marketplace`, `notification` packages added
     this pass, no controllers yet — those land with Phase 12/13). `users` skipped
     (tied to held Phase 6.4/7 auth work), `listings` skipped (belongs to Phase 14's
     separate BariVara backend, not this repo).**
6.3. Set up project skeleton, migrations, environment config — **skeleton done
     (`landlord-backend/`, Spring Boot 4.1); migrations still ad hoc
     (`ddl-auto: update`), Flyway on hold — no deploy planned, low payoff for
     now, revisit right before Phase 19 deployment. **Known `ddl-auto` gotcha
     hit during Phase 13**: adding a new `NOT NULL` boolean column
     (`unit.ad_paused`) to a table with existing rows fails outright —
     Hibernate emits `ADD COLUMN ... NOT NULL` with no default, Postgres
     rejects it against populated rows, and the column silently never gets
     added (transaction rolls back, app still boots). Fixed by hand this once
     (`ALTER TABLE unit ADD COLUMN ad_paused boolean NOT NULL DEFAULT false;`
     via psql). Same manual fix will be needed for any future non-nullable
     column added to a populated table until Flyway lands.**
6.4. Set up API auth (JWT issue/verify, password hashing, OTP delivery) — **on
     hold: no online deployment planned right now, so a wide-open localhost-only
     API is an acceptable risk for the time being. Revisit before any real
     deployment (Phase 19). Same hold applies to Phase 7.**
6.5. ✅ **Local dev environment: `docker-compose.yml` at repo root, official
     `postgres:16` image, `landlord_db`, port 5432, named volume**

### Phase 7 — Real authentication (both apps) — **ON HOLD, see Phase 6.4 note**
7.1. Backend: signup/login/OTP/forgot-password/reset-password endpoints
7.2. Backend: account lockout after failed attempts (per login diagram)
7.3. Frontend (both apps): replace `AuthService` stub with real HTTP calls + token
7.4. Frontend (both apps): replace `roleGuard` localStorage check with real token/role
7.5. Wire OTP email delivery (transactional email provider)

### Phase 8 — Properties & Units (real data)
8.1. Backend: CRUD endpoints for properties and units — **Done. Property
     (`GET/POST/PUT/DELETE /api/properties`) and Unit (`GET/POST/PUT/DELETE
     /api/units`, `GET /api/units?propertyId=`) both real, FK'd via `propertyId`.**
8.2. Frontend: replace `MockDataService` properties/units calls with the real
     API — **Done. Property and Units both wired (`PropertyApiService`,
     `UnitApiService`); "units per property" count on the Properties page
     reads real data again.**
8.3. Unit status transitions (vacant/occupied) enforced server-side —
     **partial**: `vacant → occupied` now happens server-side as a side
     effect of tenant registration (Phase 9.1). Still possible to set status
     directly via `PUT /api/units/{id}` with no tenant behind it — no
     move-out flow yet to reverse `occupied → vacant`.
8.4. ✅ **Unit photo upload — Done.** Local-disk, same pattern as Maintenance
     (11.4): `photoUrl` on `Unit`, `POST /api/units/{id}/photo`, thumbnail
     in `unit-list.component.ts`, upload field in `unit-form.component.ts`.
     Property (as opposed to Unit) photos and BariVara's own display of
     these photos are still separate, still-pending gaps (see §3 log).

### Phase 9 — Tenant management & rental agreements
9.1. Backend: tenant CRUD, rental agreement CRUD, move-out flow — **Done**
     (`Tenant`, `RentalAgreement` entities, `/api/tenants/register` atomic
     walk-in flow, `/api/tenants/{id}/move-out` balance calc + archive).
     Deposit refund/final-bill is a displayed number only, not a real
     transaction — no payment gateway to make it real yet.
9.2. Frontend: wire tenant register/detail/move-out pages to API — **Done**.
     register, list, move-out, and now `tenant-detail.component.ts` all call
     real APIs. Only maintenance cost/history on the detail page stays mock
     (Phase 11 has no backend yet).
9.3. Backend: enforce one active tenant per unit, unit status side-effects —
     **done for the flows that exist**: register flips `vacant → occupied`
     and blocks a duplicate active National ID; move-out flips
     `occupied → vacant`. Nothing yet stops a unit being directly reassigned
     via `PUT /api/units/{id}` while still `occupied` by someone else — no
     dedicated guard for that edge case.

### Phase 10 — Billing engine (monthly bills)
10.1. Backend: `invoices` schema with `period` (billing month) field —
      **Done** (`Invoice` entity). Simplified: single `utilitiesTotal`
      number, not itemized `InvoiceUtilityLine[]` (matches the real `Unit`
      entity, which also dropped itemized utility defaults).
10.2. Backend: bill generation logic — rent + utilities + prior unpaid
      rollover — **Done** (`POST /api/invoices/generate`, manually
      triggered per tenant).
10.3. Backend: scheduled job (cron) to auto-generate bills on the 1st of each
      month — not started (10.2 is manual-trigger only so far)
10.4. Backend: payment recording — **Done, simplified**: `POST /api/payments`
      records straight to `confirmed` status (no pending → landlord-confirms
      step yet), applies to invoice balance, flips unpaid/partial/paid.
10.5. Backend: receipt generation (PDF) — not started
10.6. Frontend: "Monthly Bills" view — current month only, filtered by
      `period` — **Done** (`generate-bills.component.ts`, real
      `GET /api/invoices?period=`, bulk-generate for tenants missing one).
      No itemized utility edit anymore (backend has no per-line field).
10.7. Frontend: billing history view (past periods) — **Done**
      (`tenant-detail.component.ts`, real invoice/payment lists). No
      period picker for past months — only current-month generation exists
      server-side (10.3 not started), so history is just "whatever's been
      generated to date."
10.8. Frontend: tenant-side pay flow wired to a real payment gateway — not
      started, no gateway chosen yet (see §6 open decisions). Landlord-side
      `receive-payment.component.ts` is real (applies to oldest unpaid
      invoice first via `POST /api/payments`). `pending-cash.component.ts`
      **retired (2026-08-19)** — no pending→confirm step exists server-side
      to back it (10.4 already simplified to straight-to-confirmed), so the
      page/tab/route were deleted rather than migrated; tenant-side cash-pay
      now records straight to `confirmed` too, matching the real endpoint.

### Phase 11 — Expenses & Maintenance ✅ DONE (11.1–11.4)
11.1. Backend: expense CRUD tagged by property/tenant — **Done**
      (`Expense` entity, `GET/POST/DELETE /api/expenses`, filterable by
      `propertyId`/`tenantId`).
11.2. Backend: maintenance ticket CRUD, status transitions, cost-linked
      expense — **Done** (`MaintenanceTicket` entity, `GET/POST
      /api/maintenance-tickets`, `PUT .../{id}/status` auto-creates the
      linked `Expense` with a real `ticketId` back-reference when resolved
      with a cost — closes a gap the mock version never had).
11.3. Frontend: wire both modules to API — **Done**. All 6 components
      (landlord ticket-list/ticket-new/ticket-detail, tenant
      ticket-list/ticket-new, `expense-management.component.ts`) plus the
      landlord dashboard's maintenance/net tiles and
      `tenant-detail.component.ts`'s maintenance history now call the real
      `MaintenanceApiService`. `ledger.component.ts` migrated separately,
      2026-08-19 (see §3 note) once Payments/Property/Expenses were all real.
11.4. ✅ **Maintenance image upload — Done.** Local-disk object storage (no
      cloud, matches localhost-only stance): `MaintenanceTicket` gained
      `photoUrl`; `POST /api/maintenance-tickets/{id}/photo` (multipart) saves
      to `uploads/maintenance/{id}/` and is served back at `/uploads/**`
      (`WebConfig` resource handler). Tenant `ticket-new` now actually uploads
      the picked file (was decorative before — filename-only); landlord
      `ticket-detail` renders the photo when present. Scope is maintenance
      tickets only — Phase 8.4 (property/unit listing photos) is a separate,
      still-pending gap.

### Phase 12 — Messaging & notifications ✅ DONE (LandLord only)
12.1. ✅ **Backend: conversations/messages schema + endpoints.** `Conversation`
      (`tenantId`, `withName`) and `Message` (`conversationId`, `senderRole`,
      `text`, `read`) real, `MessagingController`
      (`GET/POST /api/conversations`, `GET/POST
      /api/conversations/{id}/messages`).
12.2. ✅ **Decided: polling** (10s refresh on thread pages), not WebSocket —
      light two-user demo traffic doesn't justify socket infra yet; revisit
      at Phase 16 if real usage needs push.
12.3. ✅ **Frontend: wire message list/thread to API.** LandLord
      landlord-side and tenant-side message-list/thread components on
      `MessagingApiService`. BariVara messaging deliberately left on mock —
      no BariVara backend yet (Phase 14).
12.4. ✅ **Notifications schema + endpoints, wired tenant notifications page.**
      `Notification` gained `tenantId`/`type`; `NotificationController`
      (`GET/POST /api/notifications`, `PUT .../{id}/read`, `DELETE
      .../{id}`); `notifications.component.ts` (tenant) migrated off mock.

### Phase 13 — Marketplace & Leads (LandLord side) — real backend ✅ DONE
13.1. ✅ **Backend: ad state per unit.** `Unit` gained `adPaused` (boolean,
      default false). `PUT /api/units/{id}/ad-pause` / `.../ad-repost` toggle it;
      `PUT /api/units/{id}` auto-resets `adPaused` to false when a unit's status
      transitions into `vacant` (ad "auto-created"/reposted on vacancy, no
      separate ad entity needed — state derives from `status` + `adPaused`,
      matching the frontend's existing model).
13.2. ✅ **Backend: booking request CRUD, accept/reject flow, unit status
      side-effects.** New `MarketplaceController` (`GET/POST
      /api/marketplace-requests`, filterable by `unitId`/`status`; `PUT
      .../{id}/status`) using the `MarketplaceRequest` entity/repository from
      Phase 6.2. Approving a request sets the linked unit's status to
      `occupied`; rejecting leaves the unit untouched. (Full tenant
      registration on approval is a separate concern, same shape as the
      existing internal-transfer gap — out of scope here.)
13.3. ✅ **Frontend: wire ad-management and request pages to real API.**
      `marketplace-api.service.ts` added; `ad-management.component.ts`,
      `request-list.component.ts`, `request-detail.component.ts` now use
      `MarketplaceApiService`/`UnitApiService` instead of `MockDataService`.
      Verified end-to-end against the real backend: create request → approve
      → unit flips to `occupied`; pause/repost ad toggles `adPaused` and
      persists.

### Phase 14 — BariVara.com — real backend ✅ DONE
14.1. ✅ **Backend: public listing search/filter endpoints.** New
      `barivara-backend/` (separate Spring Boot 4.1 project, own `barivara_db`
      Postgres schema, port 8081) — deliberate architecture call, not shared
      with `landlord-backend`: mirrors the existing frontend split (two
      products, not one monolith) and lets `shared-contracts.ts`'s sync DTOs
      (`VacancyAdSync`/`BookingRequestSync`/`UnitStatusSync`) do their
      intended job at the Phase 15 boundary instead of a shared-schema join.
      `Listing` entity denormalizes address/district/area/rent off the
      owner's property/unit at creation (same simplification style as
      LandLord's invoice snapshotting). `GET /api/listings` filters by
      district/area/propertyType, always `status=active`-only unless
      `ownerId` is passed (owner's own "manage listings" view sees
      everything).
14.2. ✅ **Backend: favorites, booking request submission.** `Favorite`
      (`GET/POST/DELETE /api/favorites`) and `BookingRequest`
      (`GET/POST /api/booking-requests`, `PUT .../{id}/status`) real.
      Approving a request flips the listing to `taken` (mirrors LandLord's
      marketplace-approval → unit-occupied side effect).
14.3. ✅ **Backend: apartment-owner-only flows.** `OwnerProperty`
      (`GET/POST/PUT/DELETE /api/owner-properties`) and `OwnerUnit`
      (`GET/POST/PUT/DELETE /api/owner-units`) CRUD. No real signup yet
      (Phase 7 on hold, same call as LandLord) — `TenantProfile`/`OwnerProfile`
      got minimal `POST /api/tenant-profiles` / `POST /api/owner-profiles`
      registration endpoints instead, seeded once with the same demo
      identities the old mock used (Nasrin Akhter id 1, Karim Hossain id 1)
      so `CURRENT_TENANT_ID_REAL`/`CURRENT_OWNER_ID_REAL` line up.
14.4. ✅ **Frontend: replace BariVara's local mock data with real API calls.**
      New API services (`listing-api`, `owner-property-api`, `owner-unit-api`,
      `favorite-api`, `booking-api`, `profile-api`) wired into: homepage,
      browse, listing-detail (public search + save/book), tenant favorites,
      owner property-list/property-form, owner listing-list/listing-form/
      listing-edit, owner request-list/request-detail. Verified end to end
      via curl (register owner/tenant → create property/unit → post listing
      → public search finds it → favorite → booking request → approve →
      listing flips to `taken`) and a clean `ng build`.
      **Deliberately left on mock** (no backend built for these — matches
      what Phase 14 actually scoped): messaging, notifications, tenant
      profile page, and the landlord-linked dashboard's synced-ads view
      (stays mock until Phase 15's real cross-app sync exists to feed it).

### Phase 15 — Real cross-system integration (this is where "connected" becomes real)
15.1. Backend: unit vacant → auto-create BariVara ad (event or shared DB read)
15.2. Backend: BariVara booking request → sync into LandLord Marketplace & Leads inbox
15.3. Backend: unit filled/ad taken down → propagate back to BariVara listing state
15.4. End-to-end test of the full loop, now for real: vacate unit → ad appears →
      booking request → landlord approves → tenant registered → ad removed

### Phase 16 — Background jobs & notifications
16.1. Monthly bill generation job — verify reliability, retries, logging
16.2. Rent due reminder notifications
16.3. Ad expiry / repost reminders
16.4. Failed OTP / lockout cleanup jobs

### Phase 17 — Testing & QA
17.1. Unit tests for backend business logic (billing calc, move-out calc, lockout)
17.2. API integration tests
17.3. Frontend component/unit tests (both apps)
17.4. End-to-end tests for critical paths
17.5. Manual QA pass against each diagram's decision branches

### Phase 18 — Security review
18.1. Auth flow review (OTP, lockout, password reset token expiry)
18.2. Authorization checks (role guard enforced server-side, not just frontend)
18.3. Input validation / injection review on all endpoints
18.4. File upload validation (type/size limits)
18.5. Rate limiting on auth and payment endpoints

### Phase 19 — Deployment & infra
19.1. Choose hosting (Open Decision)
19.2. CI pipeline: build, test, lint on every push, both apps
19.3. Staging environment
19.4. Production environment + database backups
19.5. Domain setup for BariVara.com and the LandLord app
19.6. Monitoring/error tracking (logs, uptime, alerts)

### Phase 20 — Launch & post-launch
20.1. Soft launch / pilot with a small set of real landlords
20.2. Collect feedback, fix critical issues
20.3. Public launch
20.4. Post-launch monitoring cadence (weekly bug triage)

## 6. Open decisions (need your input before the relevant phase starts)

| Decision | Needed before | Options |
|---|---|---|
| Backend language/framework | Phase 6 | Node.js/NestJS, Node/Express, Django, Laravel, etc. |
| Hosting/infra | Phase 6, 19 | Self-managed VPS, AWS, GCP, Vercel+Supabase, Firebase |
| Payment gateway | Phase 10 | bKash/Nagad (BD market), Stripe, SSLCommerz |
| Messaging transport | Phase 12 | Polling (simpler) vs. WebSocket (real-time) |
| Email/OTP provider | Phase 6–7 | e.g. SendGrid, SES, Twilio (for SMS OTP if needed) |

## 7. Assumptions

- Diagrams in `Diagram-drawio-project-R70/` remain the source of truth for flow logic
  unless you say otherwise.
- Frontend-first approach continues through all of Part 1 before Part 2 starts.
- "Connected" in Part 1 means contractually/visually consistent and demoable, not
  live-synced — live sync is Phase 15, once a backend exists.
- Each phase above is a separate work session/PR — not built all at once.
