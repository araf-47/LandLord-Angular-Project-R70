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

**Done (draft 1, commit `0a84e70`):**
- Angular 22 standalone frontend scaffold in `LandLord-Angular-Project-R70/`
- Full page/route structure for LandLord core (auth, landlord area, tenant area)
- Role-guarded lazy routing, shared plain design system CSS
- `MockDataService` — in-memory fake backend so pages list/add/edit without a real API
- Build verified clean, dev server smoke-tested

**Done (draft 2.0, since draft 1):**
- LandLord app pushed past initial scaffold: monthly per-tenant billing ledger
  (`period`-keyed invoices, never overwritten), centralized bill generation +
  payment application in `MockDataService`, a landlord-wide Ledger (cash book) with
  property/date filters, tenant financial summary (Total Due / Total Paid /
  Maintenance Cost) with full billing/payment/maintenance history per tenant, and
  navigation fixes (tab sub-nav) for previously undiscoverable sibling routes
- **BariVara.com app scaffolded** — separate Angular 22 project
  `BariVara-Angular-Project-R70/`, same conventions as the LandLord app. Full route
  structure for guest browsing, auth (shared pattern), tenant area, apartment-owner
  area, and landlord-linked area. Own `MockDataService`, seeded so its
  landlord-linked listing narratively matches a vacant unit in the LandLord app's
  seed data. Build verified clean, dev server smoke-tested.

**Done (since BariVara scaffold, still Phase 2 polish):**
- BariVara homepage rebuilt to commerce-marketplace style against a real reference
  (`pics/rental-site-demo.png`) — hero, District/Area/Property-Type search bar,
  recent-listings grid. `district`, `area`, `propertyType` added as real (not
  cosmetic) fields on `Listing`/`OwnerProperty`, wired through every place a listing
  gets created or edited, and into matching filters on `/browse`
- Auth pages (BariVara) got a way back to the homepage — were previously a dead end
- Full review pass across both apps (see §3a) — 3 correctness issues found and fixed:
  BariVara's duplicate-booking bug (booked-state was derived from a local signal
  instead of real data), the one shared-concept field that had drifted between the
  two apps (`MarketplaceRequest` lacked `tenantId`; `BookingRequest` already had it —
  fixed on the LandLord side before Phase 3 locks the contract in), and LandLord's
  dead Repost/Pause buttons on ad-management

**Not done:** visual/brand design pass (Phase 4), real cross-app connection beyond
shared conventions (Phase 3), real backend, testing, deployment. See §3a for a
tracked backlog of smaller gaps found during review but deliberately not fixed yet.

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

### Phase 3 — Frontend "connection" between the two apps
*(Not started. One piece of prep already done: `MarketplaceRequest`/`BookingRequest`
field drift was caught and fixed during the pre-Phase-3 review — see §3a.)*
3.1. Shared TypeScript contract file(s) for cross-app data shapes (Unit, Listing,
     BookingRequest, etc.) — copied into both projects, kept in sync by hand for now
3.2. Seed both mock data sets so a manual demo walkthrough tells one coherent story
     (e.g. the same unit ID/number appears vacant in LandLord and listed on BariVara)
3.3. Shared login pattern: BariVara's login/signup pages visually and structurally
     match LandLord's (per the diagram note "same pattern as LandLord core Login")
3.4. Cross-app navigation stubs: LandLord's "Auto-post ad to BariVara.com" and
     BariVara's "Redirect to LandLord core → Marketplace & Leads" become real links
     once both apps have known dev URLs (still two separate mock worlds underneath)
3.5. Document in each repo's README which pieces are demo-only vs. will need the
     real backend sync from Part 2

### Phase 4 — Visual design pass (both apps)
4.1. Decide brand direction (colors, type, logo) — needs your input, or a Figma
     source, or ask me to propose one
4.2. Apply branding to both apps' shared design system tokens
4.3. Refine layout components (sidebar, topbar, cards, listing cards) to match brand
4.4. Responsive/mobile pass on every page in both apps
4.5. Empty states, loading states, error states for every page in both apps
4.6. Visual consistency check: LandLord and BariVara should read as one family with
     two distinct skins (internal tool vs. public marketplace)

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
| BariVara app structure | Phase 2 (now) | Separate Angular project (recommended) vs. shared project, second route root |
| Brand direction | Phase 4 | Provide colors/logo/reference, or have me propose one |
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
