# The "two stacks" wrinkle — is it bad?

A follow-up explanation to `angular-springboot-postgres-workflow.md`, digging
into the "one wrinkle in your project" line from that note.

## What the wrinkle actually is

Normal simple app: one browser app talks to one server talks to one database.
Done.

This project: **two full stacks**, each complete on its own:

```
LandLord Angular (4200) → LandLord Spring Boot (8080) → landlord_db
BariVara Angular (4201) → BariVara Spring Boot (8081) → barivara_db
```

Each stack is independent — BariVara's Angular never talks to LandLord's
Spring Boot, never touches `landlord_db`. Two totally separate worlds, like
two different companies.

But then Phase 15 added one direct wire between the two *servers* (not the
browsers): when a LandLord unit goes vacant, LandLord's Spring Boot itself
makes an HTTP call straight to BariVara's Spring Boot ("hey, post an ad for
this"). Same trick as Angular calling Spring Boot — just server calling server
instead of browser calling server, no user's browser involved in that hop.

So the "wrinkle" is: instead of one clean line (browser → server → database),
there are two of those lines side by side, plus one extra cross-line
connecting the two servers directly.

## Is it really bad?

No — normal, common pattern. Called "microservices" or "multi-service
architecture." Real reasons it's fine here:

- **Two products, two teams' worth of concerns.** LandLord (property
  management) and BariVara (public marketplace) are genuinely different apps
  with different users. Keeping them separate means a bug in one can't crash
  the other.
- **Loose coupling.** Each backend only knows the other exists through one
  small sync file (`shared-contracts.ts` DTOs) and a couple of HTTP
  endpoints. Not tangled together.
- **Already proven working.** Phase 15 tested this live — vacate a unit, ad
  appears on BariVara, book it, request lands back in LandLord. Confirmed
  both directions work, and both are "best-effort" (if one server's down, the
  other doesn't crash, just logs a warning).

Downside, honest: more moving parts to run locally (2 Angular dev servers + 2
Spring Boot + Postgres = 5 processes), more surface area for something to be
down. That's it — not a design flaw, just more pieces than a single-app
project would have.

For the actual goal here (a real product with a landlord tool and a public
rental site), this is the right shape, not over-engineering.
