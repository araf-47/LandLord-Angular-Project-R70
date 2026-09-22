# Moving This Project to a New PC (with a Pendrive)

Beginner guide. No step skip. Written 2026-09-22.

## Big picture first

Think of this project like small factory with 3 kinds of machine:

- **Database machine** (Postgres, running inside Docker) — stores all data, tables, rows
- **Backend machines** (landlord-backend, barivara-backend — Spring Boot/Java) — business logic, talks to database
- **Frontend machines** (LandLord-Angular-Project-R70, BariVara-Angular-Project-R70 — Angular) — what you see in browser

Pendrive not carry the machines themselves. It carries:
- blueprints (source code)
- raw materials that can't regrow automatically (`.env.local` secret files, uploaded files, database backup)

On new PC, you install fresh tools (Docker, Node, Java) and rebuild machines from blueprints. Heavy generated stuff (`node_modules`, `target/`, `dist/`) — skip copying, gets regenerated automatically, saves huge pendrive space and avoid broken-copy bugs.

`fallback-landlord/` folder — old archived copy, not part of live project. Skip it entirely, don't copy.

## What to copy vs. what regenerates

| Copy via pendrive | Regenerated fresh on new PC (don't copy) |
|---|---|
| `LandLord-Angular-Project-R70/` (minus `node_modules`, `dist`, `.angular`) | `node_modules/` (via `npm install`) |
| `BariVara-Angular-Project-R70/` (minus `node_modules`, `dist`, `.angular`) | `dist/`, `.angular/cache` (build output) |
| `landlord-backend/` (minus `target/`) | `target/` (via Maven build) |
| `barivara-backend/` (minus `target/`) | Docker images (Postgres image auto-downloads) |
| root `docker-compose.yml` | |
| `docker-compose-init/` | |
| `dev-up.sh` | |
| `landlord-backend/.env.local` | |
| `barivara-backend/.env.local` | |
| `landlord-backend/uploads/` (if you have uploaded files worth keeping) | |
| `landlord_db_backup.sql`, `barivara_db_backup.sql` (made in Step 1 below) | |

`.env.local` files and `uploads/` folder are git-ignored — meaning git never tracked them, they only exist as real files on disk. Nothing regenerates them automatically. Must copy by hand.

---

## Step 1 — On OLD PC: export the database

Database inside Docker container `landlord_postgres`. Make a backup file (a "dump") of both databases.

Open terminal in project root folder, run:

```bash
docker exec landlord_postgres pg_dump -U Araf landlord_db > landlord_db_backup.sql
docker exec landlord_postgres pg_dump -U Araf barivara_db > barivara_db_backup.sql
```

What happens: `pg_dump` reads every table/row inside `landlord_db`, writes it out as plain SQL text into a `.sql` file on your computer (not inside Docker). That `.sql` file is your database's "snapshot" — safe to copy anywhere, even to a completely different computer.

Check both files got created and aren't empty:

```bash
ls -lh landlord_db_backup.sql barivara_db_backup.sql
```

---

## Step 2 — On OLD PC: copy files to pendrive

Plug pendrive in. Suppose it mounts at `/media/yourname/PENDRIVE` (Linux) or shows as `E:\` (Windows).

**Linux — use `rsync` to skip heavy folders automatically:**

```bash
mkdir -p /media/yourname/PENDRIVE/LandLord-Project

rsync -av --progress \
  --exclude 'node_modules' --exclude 'dist' --exclude '.angular' --exclude 'target' \
  LandLord-Angular-Project-R70 BariVara-Angular-Project-R70 \
  landlord-backend barivara-backend \
  docker-compose.yml docker-compose-init dev-up.sh \
  landlord_db_backup.sql barivara_db_backup.sql \
  /media/yourname/PENDRIVE/LandLord-Project/
```

No `rsync` available? Install: `sudo apt install rsync`.

**Windows (beginner-friendly way):** copy the folders normally via File Explorer (drag-and-drop onto pendrive drive letter), but **first delete these subfolders** inside each project folder before copying (they're huge and pointless to carry):
- `LandLord-Angular-Project-R70/node_modules`, `LandLord-Angular-Project-R70/dist`, `LandLord-Angular-Project-R70/.angular`
- `BariVara-Angular-Project-R70/node_modules`, `BariVara-Angular-Project-R70/dist`, `BariVara-Angular-Project-R70/.angular`
- `landlord-backend/target`
- `barivara-backend/target`

(Deleting them on OLD PC is safe — they just get rebuilt by `npm install` / Maven whenever needed, on either PC.)

**Don't forget these — easy to miss since they're hidden/git-ignored:**
- `landlord-backend/.env.local`
- `barivara-backend/.env.local`
- `landlord-backend/uploads/` folder (if it has files you care about)
- the two `.sql` backup files from Step 1

On Linux, `.env.local` is a "dotfile" — hidden by default in file managers. Show hidden files (Ctrl+H in most Linux file managers) or use terminal `cp` to be sure it's included.

---

## Step 3 — On NEW PC: install prerequisites

Four tools needed. Order doesn't matter.

### Docker (runs the database)

**Linux (Debian/Ubuntu-based):**
```bash
sudo apt update
sudo apt install docker.io docker-compose-plugin
sudo systemctl enable --now docker
sudo usermod -aG docker $USER
```
Log out and back in after the last command (group change needs fresh login).

**Windows:** install [Docker Desktop](https://www.docker.com/products/docker-desktop/), restart PC after install, launch Docker Desktop once before continuing.

### Node.js (runs the Angular frontends)

Project doesn't pin an exact Node version — any recent LTS (20 or newer) works.

**Linux, easiest via nvm:**
```bash
curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.40.1/install.sh | bash
source ~/.bashrc
nvm install --lts
```

**Windows:** download installer from [nodejs.org](https://nodejs.org/) (LTS version), run it.

### JDK 25 (runs the Spring Boot backends)

Both backends require Java 25 specifically (checked their `pom.xml` — `<java.version>25</java.version>`).

**Linux:**
```bash
sudo apt install openjdk-25-jdk
```
If your distro's package list doesn't have `openjdk-25-jdk` yet (it's a newer release), use [Adoptium Temurin 25](https://adoptium.net/temurin/releases/?version=25) installer instead.

**Windows:** download JDK 25 from [Adoptium](https://adoptium.net/temurin/releases/?version=25), run installer.

Verify: `java -version` should print something with `25` in it.

Maven itself — **not needed**, both backends ship a wrapper script (`./mvnw`) that downloads the right Maven version automatically on first run.

### Git — optional

Not required for this pendrive-only move. Handy to have installed anyway for future version control, but skip if you don't want it right now.

---

## Step 4 — On NEW PC: copy project off pendrive

Pick a folder, e.g. `~/git-repo/` (Linux) or `C:\projects\` (Windows). Copy everything from pendrive into it — same folder layout as before.

Then put the two easy-to-miss files back in their exact spot:
- `.env.local` → inside `landlord-backend/` (next to `pom.xml`)
- `.env.local` → inside `barivara-backend/` (next to `pom.xml`)
- `uploads/` → inside `landlord-backend/` (if you copied it)

Linux only — the `mvnw` script needs execute permission, which sometimes gets lost during copy:
```bash
chmod +x landlord-backend/mvnw barivara-backend/mvnw
chmod +x dev-up.sh
```

---

## Step 5 — On NEW PC: start Postgres and restore data

From project root folder:

```bash
docker compose up -d
```

This downloads the Postgres 16 image (first time only, needs internet) and starts a fresh empty `landlord_postgres` container with two empty databases (`landlord_db`, `barivara_db`).

Wait ~10 seconds for it to be ready, then check it's running:
```bash
docker ps
```
You should see `landlord_postgres` in the list.

Now restore your data from the `.sql` backups made in Step 1:

```bash
cat landlord_db_backup.sql | docker exec -i landlord_postgres psql -U Araf -d landlord_db
cat barivara_db_backup.sql | docker exec -i landlord_postgres psql -U Araf -d barivara_db
```

This replays every table-creation and row-insert command from the backup file into the fresh database — end result is identical data to what you had on the old PC.

---

## Step 6 — On NEW PC: start backends

Open two separate terminals (one backend per terminal, they need to stay running).

**Terminal 1:**
```bash
cd landlord-backend
./mvnw spring-boot:run
```

**Terminal 2:**
```bash
cd barivara-backend
./mvnw spring-boot:run
```

First run downloads all Java dependencies — can take a few minutes, needs internet. You'll know it worked when you see a line like `Started LandlordBackendApplication in X seconds` near the bottom of the log, with no red `ERROR` lines above it. landlord-backend listens on port 8080, barivara-backend on port 8081.

---

## Step 7 — On NEW PC: install frontend deps and run

Two more terminals.

**Terminal 3:**
```bash
cd LandLord-Angular-Project-R70
npm install
npm start
```
`npm install` downloads all frontend packages (first time only, few minutes). `npm start` runs `ng serve` on port 4200.

**Terminal 4:**
```bash
cd BariVara-Angular-Project-R70
npm install
npm start
```
Runs on port 4201 (hardcoded in this project's `npm start` script).

---

## Step 8 — Verify everything works

Open browser:
- `http://localhost:4200` — LandLord frontend
- `http://localhost:4201` — BariVara frontend

Try logging in with your restored data's real credentials (whatever accounts existed in your old database — the pg_dump restore brought those over). If you skipped the DB restore and started fresh instead, default seeded logins are:
- LandLord: `landlord` / `Landlord@12345`
- BariVara: `landlord-linked` / `Landlord@12345`

### Troubleshooting

| Problem | Likely cause / fix |
|---|---|
| `docker compose up -d` fails, "Cannot connect to Docker daemon" | Docker not running — start Docker Desktop (Windows) or `sudo systemctl start docker` (Linux) |
| Backend won't start, port 8080/8081 "already in use" | Something else using that port — find and stop it, or check you didn't already start it in another terminal |
| `./mvnw: Permission denied` | Run `chmod +x mvnw` inside that backend folder |
| Backend crashes on startup mentioning database connection refused | Postgres container not up yet — wait longer after `docker compose up -d`, or check `docker ps` |
| `java -version` shows wrong version / backend won't compile | Multiple JDKs installed — make sure JDK 25 is the active one (`update-alternatives --config java` on Linux) |
| Frontend shows blank page / can't reach backend | Backend not started yet, or CORS issue — check backend terminal logs for errors first |
| pendrive copy missing `.env.local` | Emails (OTP, notifications) silently won't send — app still runs fine otherwise, just re-copy that file from old PC if needed |

---

## Optional shortcut: `dev-up.sh`

Once Postgres data is restored (Step 5 done), you can skip Steps 6 and 7 manually and instead run, from project root:

```bash
./dev-up.sh
```

This one script starts Postgres (if not already up), both backends, and both frontends, all in background, with logs saved to `.dev-run/*.log`. It waits and reports whether each service came up successfully, then prints the URLs and login credentials.

Stop everything (except Postgres, which keeps running) with:
```bash
./dev-up.sh down
```

Handy for daily use after this initial migration is done — no need to open 4 separate terminals every time.
