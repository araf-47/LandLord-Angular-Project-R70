# LandLord + BariVara.com — Project Plan

Last updated: 2026-08-07

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

**Not done:** Phase 5 QA/sign-off, real backend, testing, deployment. See §3a for a
tracked backlog of smaller gaps found during review but deliberately not fixed.

## 3a. Known gaps — reviewed, tracked, not yet fixed

Found during a full review pass across both apps (2026-08-07). Not blocking Phase 3;
listed here so they're not lost, with a note on which phase naturally absorbs each.

- **No property edit or delete, either app.** Add-only. (Candidate: fold into
  whichever phase does the properties/units backend, Phase 8 — or do as a quick
  frontend-only add before then if it starts to hurt demoing.)
- **BariVara owner side has no unit-management page.** LandLord has "Manage units"
  per property (list/add/edit); BariVara can only add one unit bundled into property
  creation, no way to add a second unit or edit rent later. (Phase 4 or earlier —
  frontend-only fix, doesn't need backend.)
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
- **No images anywhere.** Every listing card shows a gray "No photo" placeholder.
  More noticeable now that the homepage is commerce-styled and implies real photos.
  (Needs object storage — Phase 8.4/11.4, or a placeholder-image service as a cheap
  frontend-only stopgap if it bothers you before then.)
- **LandLord's public-layout doesn't check auth state.** BariVara's equivalent shows
  "My Dashboard" instead of Login/Signup when already authenticated; LandLord's
  always shows Login/Signup regardless. An already-logged-in landlord landing on `/`
  sees the marketing page again instead of a way back to their dashboard. Cheap,
  frontend-only — fixable now or foldable into Phase 4.

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
4.4. Responsive pass — **scoped to CSS reflow, not a full interactive mobile nav**:
     sidebar collapses to a horizontal scrolling strip under 860px, hero/search bar
     wrap and resize, `.table-scroll` utility added for wide tables. No hamburger
     menu / off-canvas nav built — call it out if that's wanted, it's a distinct
     follow-up (would need JS state in each layout component, not just CSS) ✅
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

### Phase 5 — Frontend QA & demo readiness
5.1. Manual walkthrough of every branch in all 4 diagrams, both apps
5.2. Cross-browser / cross-device check
5.3. Fix any routing dead-ends or broken links found during walkthrough
5.4. Record or write up the "full loop" demo script (vacate unit → ad appears on
     BariVara mock → booking request → landlord approves → tenant registered) for
     your review
5.5. **Checkpoint: get your sign-off before starting Part 2**

## 5. Part 2 — Backend (starts after Part 1 sign-off)

### Phase 6 — Backend foundation
6.1. Choose backend stack + hosting (Open Decision)
6.2. Design relational schema: users, properties, units, tenants, agreements,
     invoices, payments, expenses, maintenance_tickets, conversations, messages,
     marketplace_requests, notifications, listings
6.3. Set up project skeleton, migrations, environment config
6.4. Set up API auth (JWT issue/verify, password hashing, OTP delivery)
6.5. Local dev environment (docker-compose: API + Postgres)

### Phase 7 — Real authentication (both apps)
7.1. Backend: signup/login/OTP/forgot-password/reset-password endpoints
7.2. Backend: account lockout after failed attempts (per login diagram)
7.3. Frontend (both apps): replace `AuthService` stub with real HTTP calls + token
7.4. Frontend (both apps): replace `roleGuard` localStorage check with real token/role
7.5. Wire OTP email delivery (transactional email provider)

### Phase 8 — Properties & Units (real data)
8.1. Backend: CRUD endpoints for properties and units
8.2. Frontend: replace `MockDataService` properties/units calls with the real API
8.3. Unit status transitions (vacant/occupied) enforced server-side
8.4. Unit photo upload (object storage integration)

### Phase 9 — Tenant management & rental agreements
9.1. Backend: tenant CRUD, rental agreement CRUD, move-out flow (balance calc,
     deposit refund/final bill, archive)
9.2. Frontend: wire tenant register/detail/move-out pages to API
9.3. Backend: enforce one active tenant per unit, unit status side-effects

### Phase 10 — Billing engine (monthly bills)
10.1. Backend: `invoices` schema with `period` (billing month) field
10.2. Backend: bill generation logic — rent + utilities + prior unpaid rollover
10.3. Backend: scheduled job (cron) to auto-generate bills on the 1st of each month
10.4. Backend: payment recording — cash (pending → landlord confirms) and online
10.5. Backend: receipt generation (PDF)
10.6. Frontend: "Monthly Bills" view — current month only, filtered by `period`
10.7. Frontend: billing history view (past periods)
10.8. Frontend: tenant-side pay flow wired to a real payment gateway

### Phase 11 — Expenses & Maintenance
11.1. Backend: expense CRUD tagged by property/tenant
11.2. Backend: maintenance ticket CRUD, status transitions, cost-linked expense
11.3. Frontend: wire both modules to API
11.4. Maintenance image upload (object storage)

### Phase 12 — Messaging & notifications
12.1. Backend: conversations/messages schema + endpoints
12.2. Decide: polling vs. WebSocket/real-time (Open Decision)
12.3. Frontend: wire message list/thread to API (or socket)
12.4. Notifications schema + endpoints, wire tenant notifications page

### Phase 13 — Marketplace & Leads (LandLord side) — real backend
13.1. Backend: ad state per unit (auto-created when unit set vacant)
13.2. Backend: booking request CRUD, accept/reject flow, unit status side-effects
13.3. Frontend: wire ad-management and request pages to real API

### Phase 14 — BariVara.com — real backend
14.1. Backend: public listing search/filter endpoints
14.2. Backend: favorites, booking request submission
14.3. Backend: apartment-owner-only flows (independent landlords, no core account)
14.4. Frontend: replace BariVara's local mock data with real API calls

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
