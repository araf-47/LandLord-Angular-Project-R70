# Health check — 2026-08-22

A live check of the whole stack (Postgres, both Spring Boot backends, both
Angular frontends), run after Phase 16.1–16.3, not just a compile-time guess.

## Result: healthy, functional, nothing broken

- **Postgres**: up, accepting connections on both `landlord_db` and
  `barivara_db`.
- **Both backends compile clean** (`mvn compile`, zero errors).
- **Both Angular apps build clean** (`ng build`, zero errors).
- **Both backends actually started and served real traffic**: units,
  tenants, invoices, listings all returned real data from the DB, zero
  exceptions in either server log.
- **Notification/reminder system from Phase 16** confirmed clean — no
  leftover test data from the earlier live-verification pass.

## Outstanding, not a break

Phase 16 changes (background jobs, ad reminders) were still **uncommitted**
in git at the time of this check — built, tested, working, just not saved to
a commit yet.

## What was actually checked

1. `docker exec landlord_postgres pg_isready` against both databases.
2. `./mvnw -q -o compile` in `landlord-backend/` and `barivara-backend/`.
3. `npx ng build` in `LandLord-Angular-Project-R70/` and
   `BariVara-Angular-Project-R70/`.
4. Started both backends for real (`spring-boot:run`), waited for them to
   come up, then hit real endpoints:
   - `GET /api/units`, `/api/tenants`, `/api/invoices` (landlord-backend,
     :8080)
   - `GET /api/listings` (barivara-backend, :8081)
   - `GET /api/notifications?audience=landlord`
5. Grepped both server logs for `error`/`exception` — none found.
6. Stopped both backend processes cleanly afterward, confirmed no lingering
   Java processes.
