# Startup Guide v2 — LandLord + BariVara, both backends, local dev

`startup-guide-araf.md` only covers LandLord's backend (from before BariVara
had one). This is the updated version: **one shared Postgres container, two
separate Spring Boot backends, two Angular frontends** — five pieces total.
Order matters: **database → both backends → both frontends.**

## The shape of it

```
Postgres (one container, port 5432)
 ├─ landlord_db   ──►  landlord-backend   (port 8080)  ──►  LandLord app   (port 4200)
 └─ barivara_db   ──►  barivara-backend   (port 8081)  ──►  BariVara app   (port 4201)
```

Two separate Spring Boot projects, two separate databases, but **one Docker
container** does both — `barivara_db` is auto-created inside the same
Postgres instance via `docker-compose-init/init-barivara-db.sh` the first
time the container initializes its data volume.

## 1. Database (Postgres, via Docker) — start once, feeds both backends

Lives in `docker-compose.yml` at repo root
(`/home/araf/git-repo/LandLord-Angular-Project-R70/docker-compose.yml`).

Single container, named `landlord_postgres`, port `5432`, named volume
`landlord_pg_data` (survives restarts, only wiped by `docker compose down -v`).
On first-ever startup (empty volume) it creates `landlord_db` from the
compose file's `POSTGRES_DB`, then runs the init script to also create
`barivara_db` alongside it.

Same credentials for both databases (already baked into both backends'
`application.properties`):
- User: `Araf`
- Password: `47araf47`
- Databases: `landlord_db`, `barivara_db`

### Start it

```bash
cd /home/araf/git-repo/LandLord-Angular-Project-R70
docker compose up -d
```

### Check it's running

```bash
docker ps
```

Look for `landlord_postgres` with `Up ...`.

### Stop it

```bash
docker compose down
```

Removes the container, keeps the volume (both databases' data is safe).
`docker compose down -v` wipes everything, including `barivara_db` — don't
run that unless you actually mean it.

### Peek inside either database (optional, for debugging)

```bash
docker exec -it landlord_postgres psql -U Araf -d landlord_db
docker exec -it landlord_postgres psql -U Araf -d barivara_db
```

`\dt` lists tables, `\q` quits.

## 2. Backend — LandLord (Spring Boot)

```bash
cd /home/araf/git-repo/LandLord-Angular-Project-R70/landlord-backend
./mvnw spring-boot:run
```

Runs on **`http://localhost:8080`**, reads `landlord_db`. Ready when the log
settles on `Started BackendApplication in X seconds`.

Health check:

```bash
curl http://localhost:8080/api/properties
```

## 3. Backend — BariVara (Spring Boot)

Separate project, separate terminal, can start in parallel with the LandLord
backend (both only need Postgres, not each other).

```bash
cd /home/araf/git-repo/LandLord-Angular-Project-R70/barivara-backend
./mvnw spring-boot:run
```

Runs on **`http://localhost:8081`**, reads `barivara_db`. First run
downloads its own Maven dependencies (separate `pom.xml` from
`landlord-backend`, one-time, needs internet). Ready when the log settles on
`Started BackendApplication in X seconds`.

Health check:

```bash
curl http://localhost:8081/api/listings
```

Should return JSON (a list, possibly empty `[]`), not a connection error.

To stop either backend: `Ctrl+C` in its terminal.

## 4. Frontend — LandLord app (Angular)

```bash
cd /home/araf/git-repo/LandLord-Angular-Project-R70/LandLord-Angular-Project-R70
npm start
```

Runs on **`http://localhost:4200`**, fixed port. Talks to the LandLord
backend on `8080`.

## 5. Frontend — BariVara app (Angular)

```bash
cd /home/araf/git-repo/LandLord-Angular-Project-R70/BariVara-Angular-Project-R70
npm start
```

Runs on **`http://localhost:4201`**, fixed port (`ng serve --port 4201`
under the hood). Talks to the BariVara backend on `8081`. Needed any time
you're testing BariVara itself, not just the LandLord↔BariVara cross-app
links.

To stop either frontend: `Ctrl+C` in its terminal.

## Full startup, start to finish

```bash
# Terminal 1 — database (stays running in background, no terminal needed after this)
cd /home/araf/git-repo/LandLord-Angular-Project-R70
docker compose up -d

# Terminal 2 — LandLord backend
cd /home/araf/git-repo/LandLord-Angular-Project-R70/landlord-backend
./mvnw spring-boot:run

# Terminal 3 — BariVara backend
cd /home/araf/git-repo/LandLord-Angular-Project-R70/barivara-backend
./mvnw spring-boot:run

# Terminal 4 — LandLord frontend
cd /home/araf/git-repo/LandLord-Angular-Project-R70/LandLord-Angular-Project-R70
npm start

# Terminal 5 — BariVara frontend
cd /home/araf/git-repo/LandLord-Angular-Project-R70/BariVara-Angular-Project-R70
npm start
```

Then open `http://localhost:4200` (LandLord) and/or `http://localhost:4201`
(BariVara) in a browser.

## Shutdown order (reverse of startup)

1. `Ctrl+C` both frontends.
2. `Ctrl+C` both backends.
3. `docker compose down` (from repo root) — or leave Postgres running, it's
   harmless idle in the background; only stop it if you want the resources
   back.

## Common gotchas

- **A backend won't start / connection refused to Postgres** — the database
  container isn't up yet, or hasn't finished initializing. Run `docker ps`
  to confirm `landlord_postgres` shows `Up`, then retry.
- **BariVara backend specifically fails to find `barivara_db`** — this only
  happens if the Postgres volume was created *before* `barivara_db` existed
  (e.g. an old volume from before Phase 14) and never got the init script
  run against it. Fix: connect via `psql` (see peek command above) against
  `landlord_db` and run `CREATE DATABASE barivara_db;` by hand — the init
  script only runs automatically on a brand-new empty volume, not on an
  existing one.
- **A frontend loads but every page is empty / network errors in console** —
  its backend isn't running or isn't done booting yet. Check the matching
  `curl` health check above (LandLord → `:8080`, BariVara → `:8081`).
- **Port already in use** — something from a previous session is still
  running. Find and kill it: `lsof -i :8080` (or `:8081`/`:4200`/`:4201`/
  `:5432`), then `kill <PID>`.
- **Uploaded photos (unit/listing photos) don't show up** — each backend
  serves its own `uploads/` folder from its own project root
  (`landlord-backend/uploads/`, `barivara-backend/uploads/`) at `/uploads/**`
  on its own port. A LandLord-side photo URL will never resolve through the
  BariVara backend's port or vice versa — check you're hitting the right
  origin (`:8080` vs `:8081`) for the app you're looking at.
- **Data looks stale/wrong after a while** — both backends are real
  Postgres-backed now (see `project-plan.md` §3 for exactly what's migrated
  in each app), so changes persist across restarts. If something looks off,
  it's real data now, not a mock reset — check via the `psql` peek above
  before assuming it's a bug.
