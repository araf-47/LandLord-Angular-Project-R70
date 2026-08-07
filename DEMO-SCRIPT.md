# Full-loop demo script — Phase 5.4

Both apps are frontend-only, each with its own in-memory mock data (no shared
backend yet — that's Phase 15). This script walks the full lifecycle story using
each app's *own* seed data, which was deliberately written to tell one coherent
story: LandLord's unit `A-102, Green View Apartments, ৳14,000/mo` and BariVara's
landlord-linked listing `Green View Apartments — A-102, ৳14,000` are the same
fictional unit, kept narratively in sync by hand. Nothing here is live-synced —
walking through both halves back-to-back is what makes the loop feel connected.

Run both dev servers side by side before starting:
```
cd LandLord-Angular-Project-R70 && npm start   # localhost:4200
cd BariVara-Angular-Project-R70 && npm start   # localhost:4201
```
Login on both apps accepts any email/password — only the role you pick matters.

---

## Part A — LandLord: the vacancy and its ad

1. Go to `localhost:4200`, click **Get Started** → log in with any email, role
   **Landlord**.
2. Dashboard: point out the 5 KPI cards (Occupancy, Collected this month,
   Outstanding this month, Net this month, Pending maintenance) — real numbers,
   derived from the seed data, not placeholders.
3. **Property & Units** → Green View Apartments → Manage units. Unit A-102 is
   already seeded `vacant` — this is the unit BariVara mirrors.
4. **Marketplace & Leads → Ads**: A-102 shows as an active ad. Point out the
   "Live on BariVara.com" link — opens BariVara's dev site in a new tab (real
   navigation, not a dead stub) and the hint text that's honest about the ad not
   being auto-synced yet.

## Part B — BariVara: browse and request

5. Switch to `localhost:4201` (or use the link from step 4). No login needed —
   it's the public marketplace.
6. Homepage → search by District (Dhaka) / Area (Dhanmondi) → **Green View
   Apartments — A-102** appears in results, ৳14,000, same unit.
7. Open the listing detail page. Log in as a prospective tenant (any email,
   role **Tenant**) when prompted, then submit a booking request.
8. Show the tenant dashboard → **Notifications** — confirms the request went in.

## Part C — LandLord: approve and register

9. Back on `localhost:4200` (still logged in as landlord) → **Marketplace &
   Leads → Requests** — the new booking request is listed (BariVara's own mock
   store, not literally the one from step 7 — call this out plainly, it's the
   seeded request that tells the same story).
10. Open the request → approve.
11. **Tenant Management → Register tenant (walk-in)** → fill in the applicant's
    details, National ID (uniqueness check runs live against existing active
    tenants), assign unit A-102 from the property-grouped dropdown. Unit flips
    to occupied.
12. **Payments → Monthly Bills**: A-102 now has an active tenant and shows up
    for next bill generation — full loop closed, notebook-replacement story
    intact.

## Optional side branches worth showing

- **Utility charges**: on a unit with prepaid electricity, the utility list on
  the unit form is landlord-configurable (Water/Gas/Service Charge presets) —
  intentionally not a hardcoded electricity line.
- **Partial payment**: Payments → Monthly Bills, pay less than the full amount
  — status flips to `partial` (amber, distinct from `unpaid` red), total due
  updates to the real remaining balance.
- **Move-out**: Tenant Management → an active tenant → Move out — unit returns
  to vacant, ready to re-list.
- **Mobile**: shrink the browser under ~860px on any dashboard page — sidebar
  becomes a hamburger-triggered drawer, not a horizontal scroll strip.

## Known, deliberate gaps (mention if asked, don't dwell on)

No page reload persistence (in-memory only), no property edit/delete, no
listing photos, "Send" on request-detail is decorative, single hardcoded demo
tenant/owner login target regardless of email typed. All tracked in
`project-plan.md` §3a, not required for Part 1 sign-off.
