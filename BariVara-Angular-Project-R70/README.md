# BariVara.com

Public rental marketplace frontend. Guests browse and view listings; Tenants search,
favorite, and book; Apartment Owners post and manage ads; a LandLord (core-linked)
role gets a read-only view of ads synced from the LandLord app.

Angular 22, standalone components, no backend. See `../project-plan.md` (repo root)
for the full roadmap and current phase.

## Running it

```bash
npm install
npm start        # http://localhost:4201
```

Fixed to port 4201 so it can run alongside the [LandLord app](../LandLord-Angular-Project-R70)
(port 4200) without a flag — the landlord-linked dashboard and a couple of
LandLord pages link to each other's dev URL (`src/app/core/cross-app.config.ts`).

Demo login: any email/password works. Pick a role from the dropdown (Tenant /
Apartment Owner / LandLord core account) — there's no real auth yet, see below.

## What's mock vs. what's real

Everything in this app runs on `src/app/core/mock-data.service.ts` — an in-memory
signal-based store, no backend, no network calls. That means:

- **Resets on page reload.** Refreshing mid-demo loses all state back to seed data.
- **`AuthService` is a stub.** Login accepts any credentials and just remembers the
  role you picked in the dropdown; there's no real account system yet.
- **The landlord-linked dashboard's ads are hand-seeded to match LandLord's mock
  data**, not actually pulled from it — the two apps have no shared server.
  "Redirect to LandLord core" opens the other app in a new tab; it isn't a deep
  link into a shared session.

## Connection to LandLord

This app and LandLord core are genuinely separate deployments with no shared
server — see `project-plan.md` Part 1 Phase 3 for what "connected" means before
there's a real backend. In short: same field shapes for the concepts that cross the
boundary (`src/app/core/shared-contracts.ts`, hand-kept in sync with LandLord's
copy), consistent seed data (this app's landlord-linked listing matches LandLord's
vacant unit), and dev-only links between the two running apps. Real, live sync — a
booking request here actually landing in LandLord's Marketplace & Leads inbox — is
Phase 15, once both apps share a backend.

## Known gaps

Tracked in `project-plan.md` §3a — things found during review that aren't blocking
but aren't fixed yet (no unit-management page for owners beyond initial creation, no
way to start a new conversation, single hardcoded demo tenant/owner, no listing
images, etc.).
