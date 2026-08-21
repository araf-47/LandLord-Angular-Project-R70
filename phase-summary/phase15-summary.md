Phase 15 done and verified live, not just compiled.

What got built: two small RestClient-based sync services, one per backend, wired into the exact trigger points — unit-vacate (both the direct edit path and move-out), ad pause/repost, and marketplace approval — using the VacancyAdSync/BookingRequestSync/UnitStatusSync contracts that had sat unused since Phase 3. Both directions are best-effort: if the other backend's down, the local action still succeeds, just logs a warning.

Real snag hit and fixed along the way: LandLord's Property never had district/area/propertyType (confirmed with you before touching it) — added, plus a hand-crafted Postgres ALTER TABLE ... DROP NOT NULL on listing.owner_id in your actual local DB, since ddl-auto=update adds columns but never relaxes an existing constraint. That fix is already applied to your running Postgres, not just noted in a migration file that hasn't run yet.

Verified live, not just built: ran both backends against your real DB, walked the full loop end to end — vacated a real unit → confirmed a landlord-linked listing appeared on BariVara → submitted a real booking request → confirmed it landed in LandLord's inbox with the message intact → approved it → confirmed the unit flipped occupied and the BariVara ad flipped to taken. Test rows deleted after, your demo seed data is untouched. Both backends are stopped now; project-plan.md updated with full detail.

Left out on purpose, flagged in the plan: synced listings still carry no photo (VacancyAdSync has no photo field yet), and approving a BariVara booking doesn't auto-register a real LandLord Tenant — a landlord still walks that through the registration form by hand.

✻ Cooked for 9m 30s
