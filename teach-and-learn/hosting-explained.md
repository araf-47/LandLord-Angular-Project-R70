# Hosting, explained — for this project specifically

A reference note, not a deployment plan. No hosting choice made yet — see
"Where this stands" at the bottom.

## What "hosting" even means

Right now everything runs on your own machine: `localhost:4200`,
`localhost:8080`, Postgres on `localhost:5432`. Only you can reach it.

Hosting = putting the same code and database on a computer that's always on
and connected to the internet, so anyone with the URL can reach it — not just
you, not just when your laptop's open.

## What actually needs hosting in this project

Per `two-stacks-wrinkle-explained.md`, this is two full stacks:

```
LandLord Angular (4200) → LandLord Spring Boot (8080) → landlord_db
BariVara Angular (4201) → BariVara Spring Boot (8081) → barivara_db
```

So five things need a home:
- 2 Angular frontends — these compile down to plain static files (HTML/CSS/JS),
  no server logic of their own.
- 2 Spring Boot backends — these need an actual running JVM process, not just
  static files. Costs more to host than a frontend.
- 1 Postgres database (currently `landlord_db`, `jdbc:postgresql://localhost:5432`
  per `landlord-backend/src/main/resources/application.properties`) —
  BariVara's `barivara_db` too.

Frontend and backend have very different hosting needs — that split matters
for the free-tier question below.

## Free-tier landscape, piece by piece

**Frontend (static files)** — cheap to host because there's no server
process, just files served to browsers. Free tiers are genuinely solid here:
Vercel, Netlify, Cloudflare Pages. No card needed usually.

**Backend (Spring Boot, needs an always-running JVM)** — harder to get free
and always-on:
- Render — free tier exists but the app sleeps after inactivity; first
  request after a nap is slow (cold start).
- Railway — free trial credits, then paid.
- Fly.io — small free allowance.

No provider gives a Java backend a free tier that stays warm 24/7 forever.
That's the honest tradeoff of "free."

**Database (Postgres)**:
- Supabase — free tier, 500MB.
- Neon — free tier.
- Render Postgres — free, but expires after 90 days on the free plan.

## A realistic free combo (for demo/testing, not real users)

Vercel (both frontends) + Render or Railway (both backends) + Supabase or
Neon (both databases). Zero cost, but cold starts and free-tier limits mean
this is for showing people the app, not for paying tenants relying on uptime.

## How you'd actually use hosting (mechanics)

1. Push code to GitHub.
2. Connect that repo to the hosting provider's dashboard.
3. Provider builds and deploys automatically on every push to the branch you
   pick.
4. You get a public URL (e.g. `yourapp.onrender.com`) — optionally point a
   custom domain at it later (domains cost money, hosting-free-tier or not).
5. Secrets (DB password, JWT secret, etc.) get set as environment variables
   in the provider's dashboard — never committed to git, unlike the current
   local `application.properties` which has the dev DB password in plain
   text.

## Where this stands

`project-plan.md` Phase 19 ("Deployment & infra") lists hosting as an open
decision with no pick made yet — options on the table there are self-managed
VPS, AWS, GCP, Vercel+Supabase, or Firebase. This doc exists so the concept
is understood; the actual choice is still pending.
