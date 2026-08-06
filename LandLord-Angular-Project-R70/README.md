# LandLord

Property-management frontend. Two roles: **Landlord** (owner) and **Tenant** —
properties/units, tenant lifecycle, rental agreements, monthly billing, a cash-book
ledger, expenses, maintenance, and messaging.

Angular 22, standalone components, no backend. See `../project-plan.md` (repo root)
for the full roadmap and current phase.

## Running it

```bash
npm install
npm start        # http://localhost:4200
```

Fixed to port 4200 so it can run alongside the [BariVara app](../BariVara-Angular-Project-R70)
(port 4201) without a flag — several pages link to the other app's dev URL
(`src/app/core/cross-app.config.ts`).

Demo login: any email/password works. Pick a role from the dropdown (Landlord /
Tenant) — there's no real auth yet, see below.

## What's mock vs. what's real

Everything in this app runs on `src/app/core/mock-data.service.ts` — an in-memory
signal-based store, no backend, no network calls. That means:

- **Resets on page reload.** Refreshing mid-demo loses all state back to seed data.
- **`AuthService` is a stub.** Login accepts any credentials and just remembers the
  role you picked in the dropdown; there's no real account system yet.
- **Bill generation, payment application, and the ledger are all real logic** —
  just running against in-memory data instead of a database. The business rules
  (oldest-unpaid-month-first payment application, rollover balances, etc.) are
  meant to carry over unchanged once a backend exists.

## Connection to BariVara

This app and BariVara are genuinely separate deployments with no shared server —
see `project-plan.md` Part 1 Phase 3 for what "connected" means before there's a
real backend. In short: same field shapes for the concepts that cross the
boundary (`src/app/core/shared-contracts.ts`, hand-kept in sync with BariVara's
copy), consistent seed data (the vacant unit here matches BariVara's
landlord-linked listing), and dev-only links between the two running apps. Real,
live sync — a vacated unit actually appearing on BariVara — is Phase 15, once both
apps share a backend.

## Known gaps

Tracked in `project-plan.md` §3a — things found during review that aren't blocking
but aren't fixed yet (no property edit/delete, no way to start a new conversation,
single hardcoded demo tenant, etc.).
