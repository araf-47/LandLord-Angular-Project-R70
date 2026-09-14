# Project Improvement Plan

Separate from `project-plan.md` (the primary plan). This file tracks
brainstormed improvement ideas for LandLord and their progress, kept apart so
the main plan stays focused on shipped phases.

## Status: 2026-09-14

Brainstormed 8 new feature/design ideas for LandLord (analytics, unit-grid
view, multi-landlord support, rent-health badge, SMS/WhatsApp reminders,
command palette, ledger export, bento dashboard). User picked 3 to build:

1. **Unit-grid "floor plan" view** — ✅ done. View-toggle (grid/table) added
   to `unit-list.component.ts`, defaults to grid. New `unit-card.component.ts`.
   Status color-coded (vacant/occupied/overdue), overdue derived client-side
   via the existing `GET /api/tenants/{id}/outstanding-balance` endpoint — no
   new backend code.
2. **Analytics & Reports module** — ✅ done. New `reports.service.ts`
   (client-side aggregation off existing invoice/payment/expense endpoints,
   no new backend surface) + hand-rolled inline-SVG `bar-chart.component.ts`
   (no charting library added) + `reports.component.ts`, routed at
   `landlord/reports`, new dashboard module tile. Shows monthly income/
   expense/net, collection rate, and a *current* occupancy-by-property
   snapshot (deliberately not a fabricated occupancy trend — no history
   table exists for that).
3. **Bento-grid dashboard redesign** — ✅ done, revised. Bento asymmetric
   layout (one dominant 2x2 tile) applied to the 5 KPI stat tiles only (Net
   this month is the dominant cell). **User feedback (2026-09-14): the
   asymmetric bento layout did not work for the "Manage your property"
   module-nav grid** — reverted that section back to the original uniform
   `.module-grid`. Bento styling now scoped to the stat-tile row only.

All three verified via `ng build` clean; live browser click-through is on the
user (Claude-in-Chrome drives the user's real browser, which isn't reachable
from the sandbox these dev servers run in).

## Not yet started (from the original 8, deprioritized for now)

- Multi-landlord/portfolio support (`LandlordUser` entity) — bigger
  structural change, not picked this round.
- Rent-health badge for tenants (gamified on-time-payment indicator).
- SMS/WhatsApp reminders (extend Phase 7.5's Brevo-email channel).
- Command palette (Ctrl+K global search/nav).
- Ledger/report export (PDF/Excel), natural extension of Phase 10.5's
  receipt pipeline.
