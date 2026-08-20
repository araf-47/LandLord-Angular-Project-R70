Phase 14 done. Summary:

Built: barivara-backend/ — separate Spring Boot 4.1 project (Java 21, own barivara_db Postgres schema, port 8081), scaffolded from official Spring Initializr. Entities: OwnerProfile, TenantProfile, OwnerProperty, OwnerUnit, Listing, Favorite, BookingRequest. Real endpoints for public listing search/filter, favorites, booking-request submit/approve/reject (approval flips listing to taken), and owner property/unit/listing CRUD.

Frontend: 6 new API services, 12 BariVara components migrated off MockDataService (homepage, browse, listing-detail, tenant favorites, owner property/listing/request pages). provideHttpClient() added to app.config.ts. ng build clean.

Verified: full curl round-trip against the real backend (register → property → unit → listing → public search → favorite → booking request → approve → listing status flip), then reseeded clean demo data (Nasrin Akhter / Karim Hossain, matching the old mock's story) so ids line up with the new CURRENT_OWNER_ID_REAL/CURRENT_TENANT_ID_REAL stopgap constants. Both dev servers (backend :8081, frontend :4201) are running now — you can click through live.

Deliberately still mock (no backend built for these — out of Phase 14's actual scope): BariVara messaging, notifications, tenant profile page, landlord-linked dashboard (waits on Phase 15's real cross-app sync).

Updated project-plan.md (Phase 14 marked done, §3 log entry, "Not done" line) and BariVara's README.md. docker-compose.yml now provisions barivara_db on fresh volumes too.

Next candidates: Phase 10 leftovers (10.3 cron, 10.5 receipts, 10.8 gateway), or Phase 15 (real cross-system sync, now that both backends exist) — your call.
