# LandLord + BariVara.com — Project Plan

Last updated: 2026-08-06

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

## 2. Current status

**Done (draft 1, commit `0a84e70`):**
- Angular 22 standalone frontend scaffold in `LandLord-Angular-Project-R70/`
- Full page/route structure for LandLord core (auth, landlord area, tenant area),
  derived directly from the 4 activity diagrams
- Role-guarded lazy routing, shared plain design system CSS
- `MockDataService` — in-memory fake backend so pages list/add/edit without a real API
- Build verified clean, dev server smoke-tested

**Not done:** real backend/API, real visual design, BariVara.com app, monthly billing
automation, cross-system sync, auth security, testing, deployment.

This plan covers everything from here to a launchable product.

## 3. Target architecture (proposed)

- **Frontend (LandLord core):** Angular 22 standalone, already scaffolded — extend in place.
- **Frontend (BariVara.com):** separate Angular app (or same app, second route root) —
  decision needed, see Open Decisions.
- **Backend/API:** one service (or two, sharing a DB) exposing REST/GraphQL for both
  frontends. Stack not yet chosen — see Open Decisions.
- **Database:** relational (PostgreSQL recommended — the data is relational:
  properties → units → tenants → agreements → invoices → payments).
- **Scheduled jobs:** monthly bill generation, ad expiry, reminders — needs a real
  scheduler (cron / cloud scheduler), not something the Angular frontend can do alone.
- **File storage:** unit photos, tenant documents, maintenance images — object storage
  (S3-compatible).
- **Auth:** JWT-based session, replacing today's localStorage-stub `AuthService`.

## 4. Phased task sequence

Each phase lists tasks in build order. Phases are mostly sequential; some (8, 9) can
run in parallel once phase 7 lands.

### Phase 1 — Frontend foundation ✅ DONE
1.1. Read and map all 4 activity diagrams to a page/route list
1.2. Scaffold Angular app structure (core, layouts, features)
1.3. Auth pages (login, signup, forgot/reset password)
1.4. Landlord pages (dashboard + 8 modules)
1.5. Tenant pages (dashboard + 7 modules)
1.6. Role guard + lazy routing wiring
1.7. Shared design system CSS
1.8. Mock data service for working demo without a backend
1.9. Build verification

### Phase 2 — Visual design pass
2.1. Decide brand direction (colors, type, logo) — needs your input or a Figma source
2.2. Apply branding to shared design system (`styles.css`) — replace neutral placeholder tokens
2.3. Refine layout components (sidebar, topbar, cards) to match brand
2.4. Pass on responsive/mobile behavior for all pages
2.5. Empty states, loading states, error states for each page

### Phase 3 — Backend foundation
3.1. Choose backend stack + hosting (Open Decision)
3.2. Design relational schema: users, properties, units, tenants, agreements,
     invoices, payments, expenses, maintenance_tickets, conversations, messages,
     marketplace_requests, notifications
3.3. Set up project skeleton, migrations, environment config
3.4. Set up API auth (JWT issue/verify, password hashing, OTP delivery)
3.5. Local dev environment (docker-compose: API + Postgres)

### Phase 4 — Real authentication
4.1. Backend: signup/login/OTP/forgot-password/reset-password endpoints
4.2. Backend: account lockout after failed attempts (per login diagram)
4.3. Frontend: replace `AuthService` stub with real HTTP calls + token storage
4.4. Frontend: replace `roleGuard` localStorage check with token/role from API
4.5. Wire OTP email delivery (transactional email provider)

### Phase 5 — Properties & Units (real data)
5.1. Backend: CRUD endpoints for properties and units
5.2. Frontend: replace `MockDataService` properties/units calls with HTTP client + API
5.3. Unit status transitions (vacant/occupied) enforced server-side
5.4. Unit photo upload (object storage integration)

### Phase 6 — Tenant management & rental agreements
6.1. Backend: tenant CRUD, rental agreement CRUD, move-out flow (balance calc, deposit refund/final bill, archive)
6.2. Frontend: wire tenant register/detail/move-out pages to API
6.3. Backend: enforce one active tenant per unit, unit status side-effects

### Phase 7 — Billing engine (monthly bills)
7.1. Backend: `invoices` schema with `period` (billing month) field
7.2. Backend: bill generation logic — rent + utilities + prior unpaid balance rollover
7.3. Backend: scheduled job (cron) to auto-generate bills on the 1st of each month
7.4. Backend: payment recording — cash (pending → landlord confirms) and online (instant)
7.5. Backend: receipt generation (PDF)
7.6. Frontend: "Monthly Bills" view — current month only, filtered by `period`
7.7. Frontend: billing history view (past periods)
7.8. Frontend: tenant-side pay flow wired to real payment intent/gateway

### Phase 8 — Expenses & Maintenance
8.1. Backend: expense CRUD tagged by property/tenant
8.2. Backend: maintenance ticket CRUD, status transitions, cost-linked expense creation
8.3. Frontend: wire both modules to API
8.4. Maintenance image upload (object storage)

### Phase 9 — Messaging
9.1. Backend: conversations/messages schema + endpoints
9.2. Decide: polling vs. WebSocket/real-time (Open Decision)
9.3. Frontend: wire message list/thread to API (or socket)
9.4. Notifications schema + endpoints, wire tenant notifications page

### Phase 10 — Marketplace & Leads (LandLord side)
10.1. Backend: ad state per unit (auto-created when unit set vacant)
10.2. Backend: booking request CRUD, accept/reject flow, unit status side-effects
10.3. Frontend: wire ad-management and request pages to API

### Phase 11 — BariVara.com app
11.1. Decide: separate Angular project vs. second route-root in same app (Open Decision)
11.2. Scaffold routing from `BariVara_activity_v2.drawio` (guest browse, favorites,
      booking, role dashboards for Tenant/Owner/LandLord-linked)
11.3. Public listing search/filter, listing detail page
11.4. Guest → signup/login handoff (shared auth with LandLord backend)
11.5. Favorites, booking request submission
11.6. Apartment-owner-only flows (independent landlords not using core LandLord)
11.7. LandLord-linked dashboard (read-only synced ads + redirect to core Marketplace & Leads)

### Phase 12 — Cross-system integration
12.1. Backend: unit vacant → auto-create BariVara ad (event or shared DB read)
12.2. Backend: BariVara booking request → sync into LandLord Marketplace & Leads inbox
12.3. Backend: unit filled/ad taken down → propagate back to BariVara listing state
12.4. End-to-end test of the full loop: vacate unit → ad appears → booking request →
      landlord approves → tenant registered → ad removed

### Phase 13 — Background jobs & notifications
13.1. Monthly bill generation job (from 7.3) — verify reliability, retries, logging
13.2. Rent due reminder notifications
13.3. Ad expiry / repost reminders
13.4. Failed OTP / lockout cleanup jobs

### Phase 14 — Testing & QA
14.1. Unit tests for backend business logic (billing calc, move-out calc, lockout)
14.2. API integration tests
14.3. Frontend component/unit tests
14.4. End-to-end tests for critical paths (signup→login, add property→unit→tenant→bill→pay, vacate→ad→booking→approve)
14.5. Manual QA pass against each diagram's decision branches

### Phase 15 — Security review
15.1. Auth flow review (OTP, lockout, password reset token expiry)
15.2. Authorization checks (role guard enforced server-side, not just frontend)
15.3. Input validation / injection review on all endpoints
15.4. File upload validation (type/size limits)
15.5. Rate limiting on auth and payment endpoints

### Phase 16 — Deployment & infra
16.1. Choose hosting (Open Decision)
16.2. CI pipeline: build, test, lint on every push
16.3. Staging environment
16.4. Production environment + database backups
16.5. Domain setup for BariVara.com and LandLord app
16.6. Monitoring/error tracking (logs, uptime, alerts)

### Phase 17 — Launch & post-launch
17.1. Soft launch / pilot with a small set of real landlords
17.2. Collect feedback, fix critical issues
17.3. Public launch
17.4. Post-launch monitoring cadence (weekly bug triage)

## 5. Open decisions (need your input before the relevant phase starts)

| Decision | Needed before | Options |
|---|---|---|
| Backend language/framework | Phase 3 | Node.js/NestJS, Node/Express, Django, Laravel, etc. |
| Hosting/infra | Phase 3, 16 | Self-managed VPS, AWS, GCP, Vercel+Supabase, Firebase |
| Payment gateway | Phase 7 | bKash/Nagad (BD market), Stripe, SSLCommerz |
| Messaging transport | Phase 9 | Polling (simpler) vs. WebSocket (real-time) |
| BariVara app structure | Phase 11 | Separate Angular project vs. shared project, second route root |
| Brand direction | Phase 2 | Provide colors/logo/reference, or have me propose one |
| Email/OTP provider | Phase 3–4 | e.g. SendGrid, SES, Twilio (for SMS OTP if needed) |

## 6. Assumptions

- Diagrams in `Diagram-drawio-project-R70/` remain the source of truth for flow logic
  unless you say otherwise.
- Frontend-first approach (already underway) continues: backend gets built to match
  what the frontend already expects, rather than frontend adapting to a pre-existing API.
- Each phase above is a separate work session/PR — not built all at once.
