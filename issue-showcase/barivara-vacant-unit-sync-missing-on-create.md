# Vacant Unit Ads Not Appearing on BariVara Homepage

## Symptom
New vacant units posted from LandLord's Marketplace/Leads flow do not show up on BariVara's homepage.

## Root Cause
`UnitController.create()` in `landlord-backend/src/main/java/com/landlord/backend/unit/UnitController.java:58-62` never calls `syncService.postVacancyAd(...)`.

Sync only fires from `update()`, and only on a status *transition* into vacant:

```java
boolean turningVacant = !"vacant".equals(existing.getStatus()) && "vacant".equals(update.getStatus()); // line 67
// ...
if (turningVacant) {
    syncService.postVacancyAd(...); // line 82-84
}
```

New units default to `status: 'vacant'` at creation time (`unit-form.component.ts:57`), so they go through `create()` directly — no status transition ever happens, so the sync call never fires. Only a unit that starts **occupied** and is later edited to **vacant** triggers `BariVaraSyncService.postVacancyAd` (`BariVaraSyncService.java:40-65`).

## Verified Working
- BariVara homepage (`homepage.component.ts:67,79,82`) correctly calls `ListingApiService.search()` → GET `/api/listings`, filtered to `status="active"` (`ListingController.java:35-47`).
- `syncVacancyAd` endpoint (`ListingController.java:83-98`) correctly creates/updates a `landlord-linked` Listing with `status="active"` when actually invoked.

## Secondary Gotcha
`postVacancyAd` silently skips (logs only, no error) if the property is missing `district`/`area`/`propertyType`, or the unit has no `rent` (`BariVaraSyncService.java:41-49`). Properties created before Phase 15 won't sync until edited to fill these fields.

## Fix Direction
Add a `syncService.postVacancyAd(...)` call inside `create()` when the newly created unit's status is `"vacant"`, not only inside `update()`'s transition check.

## Status
Fixed — `create()` now sets `vacantSince` and calls `syncService.postVacancyAd(...)` when a newly created unit's status is `"vacant"` (`UnitController.java:58-66`).
