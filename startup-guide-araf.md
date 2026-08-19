# Startup Guide — LandLord / BariVara local dev

How to bring up the whole stack yourself: database, backend, frontend(s).
Three pieces, three terminals (or three background jobs). Order matters:
**database → backend → frontend.**

## 1. Database (Postgres, via Docker)

Lives in `docker-compose.yml` at repo root
(`/home/araf/git-repo/LandLord-Angular-Project-R70/docker-compose.yml`).

What it is: a single-container Postgres 16, named `landlord_postgres`,
listening on the normal Postgres port `5432`, with a named Docker volume
(`landlord_pg_data`) so your data survives container restarts — it only
disappears if you explicitly delete the volume.

Credentials (already baked into `docker-compose.yml`, matches the backend's
`application.properties`):
- DB name: `landlord_db`
- User: `Araf`
- Password: `47araf47`

### Start it

```bash
cd /home/araf/git-repo/LandLord-Angular-Project-R70
docker compose up -d
```

`-d` = detached, runs in the background. First run pulls the `postgres:16`
image (one-time, needs internet); after that it's instant.

### Check it's running

```bash
docker ps
```

Look for a row with `landlord_postgres` and `Up ...` in the status column.

### Stop it

```bash
docker compose down
```

This stops and removes the *container* but **not** the volume — your data
is safe, next `docker compose up -d` picks up where you left off. Only
`docker compose down -v` would wipe the volume (don't run that unless you
actually want to nuke the database).

### Peek inside the database directly (optional, for debugging)

```bash
docker exec -it landlord_postgres psql -U Araf -d landlord_db
```

Drops you into a `psql` prompt. `\dt` lists tables, `\q` quits.

## 2. Backend (Spring Boot, Java)

Lives in `landlord-backend/`. Needs the database running first — it
connects to Postgres on startup and will fail if it can't reach it.

```bash
cd /home/araf/git-repo/LandLord-Angular-Project-R70/landlord-backend
./mvnw spring-boot:run
```

First run downloads Maven dependencies (one-time, needs internet); after
that it's faster. Takes maybe 10-20 seconds to fully boot. You'll know it's
ready when the log stops scrolling and settles on a line like
`Started BackendApplication in X seconds`.

Runs on **`http://localhost:8080`**. CORS is already opened for
`localhost:4200` and `localhost:4201`, so both frontends can call it
without extra config.

Quick health check from another terminal:

```bash
curl http://localhost:8080/api/properties
```

Should return JSON (a list, possibly empty `[]`), not a connection error.

To stop: `Ctrl+C` in that terminal.

## 3. Frontend — LandLord app (Angular)

```bash
cd /home/araf/git-repo/LandLord-Angular-Project-R70/LandLord-Angular-Project-R70
npm start
```

(`npm start` = `ng serve` under the hood, fixed to port `4200`.)

Runs on **`http://localhost:4200`**. First compile takes a bit; it'll
print a `Local: http://localhost:4200/` line when ready and auto-reloads
on file changes after that.

To stop: `Ctrl+C` in that terminal.

## 4. Frontend — BariVara app (Angular, optional)

Only needed if you're testing the LandLord ↔ BariVara cross-app links.

```bash
cd /home/araf/git-repo/LandLord-Angular-Project-R70/BariVara-Angular-Project-R70
npm start
```

(`npm start` = `ng serve --port 4201`, fixed port so it never collides
with the LandLord app.)

Runs on **`http://localhost:4201`**.

## Full startup, start to finish

```bash
# Terminal 1 — database (stays running in background, no terminal needed after this)
cd /home/araf/git-repo/LandLord-Angular-Project-R70
docker compose up -d

# Terminal 2 — backend
cd /home/araf/git-repo/LandLord-Angular-Project-R70/landlord-backend
./mvnw spring-boot:run

# Terminal 3 — frontend
cd /home/araf/git-repo/LandLord-Angular-Project-R70/LandLord-Angular-Project-R70
npm start

# Terminal 4 (optional) — BariVara
cd /home/araf/git-repo/LandLord-Angular-Project-R70/BariVara-Angular-Project-R70
npm start
```

Then open `http://localhost:4200` in a browser.

## Shutdown order (reverse of startup)

1. `Ctrl+C` the frontend(s).
2. `Ctrl+C` the backend.
3. `docker compose down` (from repo root) — or just leave Postgres running,
   it's harmless idle in the background; only stop it if you want the
   resources back.

## Common gotchas

- **Backend won't start / connection refused to Postgres** — the database
  container isn't up yet, or hasn't finished initializing. Run `docker ps`
  to confirm `landlord_postgres` shows `Up`, then retry.
- **Frontend loads but every page is empty / network errors in console** —
  backend isn't running or isn't done booting yet. Check
  `curl http://localhost:8080/api/properties` first.
- **Port already in use** — something from a previous session is still
  running. Find and kill it: `lsof -i :8080` (or `:4200`/`:4201`/`:5432`),
  then `kill <PID>`.
- **Data looks stale/wrong after a while** — the app is fully backend-real
  now for Property/Unit/Tenant/Billing/Maintenance/Expenses/Ledger, so
  changes persist in Postgres across restarts. If something looks off,
  it's real data now, not a mock reset — check via the `psql` peek above
  before assuming it's a bug.
