# How Angular, Spring Boot, and PostgreSQL connect

A beginner-level explanation of how the three layers of this project talk to
each other, written after checking the real Postgres container's health.

## Postgres health check (2026-08-22)

- Container `landlord_postgres`: up, `accepting connections` (`pg_isready` OK).
- PostgreSQL 16.15, ~8MB total data.
- Two databases live inside the one container:
  - `landlord_db` — 12 tables (property, unit, tenant, rental_agreement,
    invoice, payment, expense, maintenance_ticket, conversation, message,
    notification, marketplace_request)
  - `barivara_db` — 7 tables (owner_property, owner_unit, listing, favorite,
    booking_request, tenant_profile, owner_profile)

Both match what each backend's entities expect. Nothing broken.

## The three layers

```
Angular (browser)  --HTTP-->  Spring Boot (server)  --JDBC-->  PostgreSQL (disk)
   :4200/:4201                  :8080/:8081                    :5432
```

**PostgreSQL** — just a filing cabinet. Stores rows, does nothing on its own,
has no idea "LandLord" or "BariVara" exist as concepts. Two separate cabinets
(databases) inside one building (the Docker container): `landlord_db`,
`barivara_db`.

**Spring Boot** — the clerk. Two separate clerks, one per app
(`landlord-backend` on port 8080, `barivara-backend` on port 8081), each only
opens its own cabinet. A clerk exposes a menu of actions over HTTP —
`GET /api/units`, `POST /api/tenants/register` — called a REST API. When a
request comes in, the clerk translates it into SQL, talks to Postgres,
translates the answer back to JSON.

**Angular** — the customer-facing counter (the browser app). It never touches
Postgres directly — can't, doesn't know SQL, has no permission to. It only
knows how to call the clerk's menu (`HttpClient` making HTTP calls to
`localhost:8080/api/...`).

## Workflow example — landlord marks a unit vacant

1. Click "Save" in Angular's unit-form page.
2. Angular's `UnitApiService` fires `PUT http://localhost:8080/api/units/9`
   with a JSON body.
3. Spring Boot's `UnitController` receives it, runs business logic (e.g. "if
   turning vacant, stamp `vacantSince`"), calls `UnitRepository.save(...)`.
4. Spring's JPA layer turns that into an `UPDATE unit SET ... WHERE id=9` SQL
   statement, sends it to Postgres over JDBC (port 5432).
5. Postgres writes it to disk, returns success.
6. Response flows back up: Postgres → Spring Boot → JSON → Angular → the
   screen updates.

Nothing is shared memory or a direct pipe — every hop is a network call.
That's why each layer can be restarted independently: kill Spring Boot,
Postgres data is still safe; refresh Angular, it just re-asks Spring Boot for
fresh data.

## The wrinkle in this project

Two Angular apps, two Spring Boots, two databases — LandLord and BariVara are
fully separate stacks that additionally talk to *each other's* Spring Boot
(Phase 15 sync: `landlord-backend` calls `barivara-backend`'s API directly,
server-to-server, same HTTP mechanism, just no browser in the middle for that
hop).
