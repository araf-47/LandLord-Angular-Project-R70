# LandLord Android App — Implementation Plan

## Build status (as of 2026-10-02)

**All 12 modules have working functionality. The app is installable and usable end-to-end** (debug APK, sideloaded, LAN-only backend). Phases A–E below are done; Phase F is done except FCM. This section is the honest status — it calls out where the actual build diverged from this plan's original design, not just what got added.

**What matches the plan closely**: offline-first architecture (Section 2) — local-UUID PKs, outbox with `dependsOnOpId` ordering, `IdMapper` server-ID remapping, conflict policy — built exactly as designed and verified working (Property→Unit dependency chain tested live by the user: created both offline, confirmed correct linkage after reconnect). Auth flow (Section 3), including the non-blocking-on-401 behavior, built as designed.

**Deviations from this plan, done deliberately for speed**:
- **No shared `BaseListFragment`/`BaseDetailFragment`/`BaseFormFragment` abstraction** (Section 5) was built. Every screen is bespoke (own Fragment/Adapter/ViewModel/layout, same hand-rolled pattern repeated ~13 times). Works fine, but a real refactor opportunity exists if the app grows further — the repetition is now large enough to be worth collapsing.
- **No "Sync Issues" screen** (planned for Phase B) — failed/blocked outbox ops are retried with capped attempts and flagged `CONFLICT` in Room, but there's no UI surfacing them to the user yet. A real gap if a sync silently fails.
- **Dashboard is minimal** — shows `/api/auth/me` data (username/roles/email), not the planned KPI card grid + activity feed. No `DashboardSummaryEntity` cache was built.
- **No multi-step tenant registration wizard** — single dialog with all fields at once, not a guided flow. No live `active-by-nid` duplicate check during entry (relies on the backend's 409 on conflict).
- **No move-out flow** — `POST /api/tenants/{id}/move-out` exists server-side, never wired up client-side. Tenants can be registered but not offboarded from the app.
- **No ad-pause/ad-repost Unit actions**, no outstanding-balance display, no expandable-accordion Property Detail (flat Units list instead).
- **No product flavors** (local/staging/production), no automated tests (JUnit/Mockito/Espresso), no dynamic color/Material Motion polish pass, no notification swipe-to-dismiss (tap-to-mark-read only).
- **Server address is runtime-editable** (Settings + login screen), not a build-time product flavor as originally planned — turned out to matter more in practice, since the dev machine's LAN IP changes as it moves between networks, which broke login repeatedly until fixed this way (2026-10-02).

**Confirmed net-new backend work still needed, untouched**: FCM push (device-token table, Firebase Admin SDK, dispatch hooks) — **explicitly put on hold by the user (2026-10-02)**, needs a Firebase project + `google-services.json` from them first. Pagination (Phase-2 flag, unchanged). Proper optimistic locking (`updatedAt`/`@Version` + 409, unchanged).

## Context

LandLord web platform (Angular + Spring Boot `landlord-backend`) covers landlord-side property management: properties/units, tenants, rentals, payments, marketplace, expenses, ledger, reports, AI insights, maintenance, messages, settings. No mobile app exists. User wants native Android app (Java, Android Studio), landlord-only, full feature parity with web, reusing `landlord-backend` API as-is, with eye-catching modern Material Design UI/UX. This plan defines architecture, screens, and build phases for that app.

## Tech stack decisions (user-confirmed)

- Native Android, **Java** (not Kotlin/Compose), built in Android Studio, traditional Views/XML + Material Components.
- Scope: **landlord-only**, full parity with web landlord portal (12 feature modules below).
- Backend: reuse `landlord-backend` REST API (port 8080) as-is, same JWT auth.

## Grounded backend facts (from repo)

- Auth flow is **non-standard token rotation**, not classic refresh-endpoint: requests send `Authorization: Bearer <access>` + `x-refresh-token` header; server may transparently rotate the access token and return it in response header `x-access-token`. Reference implementation to port: `LandLord-Angular-Project-R70/src/app/core/auth.interceptor.ts`; server contract in `Parts/auth/src/main/java/com/idb/auth/filter/AuthFilter.java` and `CommonConstants.java`.
- Auth endpoints: `POST /auth/login`, `POST /auth/forgot-password`, `POST /auth/otp`, `GET /api/auth/me`. No separate `/auth/refresh` endpoint confirmed — verify before finalizing Android `Authenticator` fallback.
- No CORS issue for native HTTP clients (CORS is browser-only; confirmed no other origin gate in `WebConfig.java`).
- No pagination on any list endpoint (`/api/properties`, `/api/units`, `/api/tenants`, `/api/marketplace-requests`, `/api/maintenance-tickets`) — full-fetch + local cache/filter for v1; flag server pagination as Phase-2 backend work.
- No FCM/push infrastructure anywhere in backend — push notifications require new backend work (device-token table + endpoint + Firebase Admin SDK + dispatch hooks), not just an Android addition.
- Ledger has no dedicated backend resource — Angular composes it client-side from payments+invoices+expenses; Android must do the same.
- Full controller inventory to mirror as Retrofit interfaces: `PropertyController` (`/api/properties`), `UnitController` (`/api/units`, incl. ad-pause/ad-repost/photo), `TenantController` (`/api/tenants`, incl. register/agreement/move-out/active-by-nid), `BillingController` (`/api/invoices`, `/api/payments`, receipt PDF, outstanding-balance), `MarketplaceController` (`/api/marketplace-requests`), `MaintenanceController` (`/api/maintenance-tickets`, `/api/expenses` — expenses has no edit endpoint, only create/list/delete), `MessagingController` (`/api/conversations`), `NotificationController` (`/api/notifications`), `ReportController` (`/api/reports/*` — JSON/PDF/XLSX variants), `InsightsController` (`POST /api/insights/chat`).

## 1. Project setup

New standalone project `landlord-android/` at repo root (sibling to `landlord-backend`, not nested inside it).

- Min SDK 26, target/compile SDK latest stable.
- Groovy `build.gradle` (standard for a Java-first project).
- Package-by-feature layout (mirrors Angular `features/landlord/*`):
```
com.landlord.android
  core/        network (Retrofit/OkHttp/interceptors), auth (TokenManager, biometric), db (Room), common (Result<T>, BaseFragment)
  feature/     auth, dashboard, properties, tenants, rentals, payments, marketplace,
               expenses, ledger, reports, insights, maintenance, messages, notifications, settings
  ui/          widgets (KpiCardView, EmptyStateView, SkeletonLoaderView), theme
```
- Key libraries: AndroidX + Material Components (M3), Jetpack Navigation (XML + Safe Args), Lifecycle ViewModel/LiveData, Room (annotationProcessor, no KSP), Retrofit2 + OkHttp3 + Gson, Glide (images), `androidx.security:security-crypto` (EncryptedSharedPreferences), `androidx.biometric`, `FileProvider` for report/receipt sharing, MPAndroidChart (dashboard/insights charts), JUnit/Mockito/Espresso for tests.

## 2. Architecture — offline-first with sync

MVVM: Fragment/Activity → ViewModel (LiveData) → Repository → Room (always) + outbox-driven sync layer (Retrofit lives only in sync layer now).

- `MainActivity` hosts single `NavHostFragment` for authenticated app; separate `AuthActivity` for login/OTP/forgot-password (clean task-stack separation on logout).
- One repository per feature, 1:1 with backend controller groups listed above. **Every mutating Repository method is local-first**: write Room immediately (own-assigned local UUID PK), enqueue an outbox op, return success instantly — no direct Retrofit calls in any mutation path.
- Generic `Result<T>` wrapper (Loading/Success/Error via abstract base class, since Java lacks sealed classes) — every ViewModel exposes `LiveData<Result<T>>`, driving consistent loading/error/empty UI everywhere.
- **Decisive backend fact**: checked `@Entity` classes (`Property`, `Unit`, `Tenant`, `Payment`, `Invoice`) — none has `updatedAt` or `@Version`. No cheap way to detect a server-side change since last read → conflict policy below is a deliberate v1 simplification, not CRDT-grade.
- **Room is full source of truth**, not a selective cache. Every syncable entity (`PropertyEntity`, `UnitEntity`, `TenantEntity`, `RentalAgreementEntity`, `PaymentEntity`, `InvoiceEntity`, `MarketplaceAdEntity`, `MarketplaceRequestEntity`, `ExpenseEntity`, `MaintenanceTicketEntity`, `ConversationEntity`, `MessageEntity`, `NotificationEntity`) carries an `@Embedded SyncMetadata`: `localId` (String UUID, **primary key**, generated client-side at creation — never the server's numeric ID, since offline-created rows need a PK before the server assigns one), `serverId` (nullable Long, indexed, sync-correlation only), `syncState` (SYNCED/PENDING_CREATE/PENDING_UPDATE/PENDING_DELETE/CONFLICT), `updatedLocallyAt`, `lastSyncedAt`, `deletedLocally`.
  - **Rule: all local Room queries/joins use `localId`, never `serverId`.** UI/ViewModels/DAOs stay blind to sync status (just a "pending" badge from `syncState`).
  - **Local-ID → server-ID remapping** (e.g. Property created offline, then a Unit under it): `IdMapper` (`core/sync/IdMapper.java`) + `IdMappingEntity` records `localId -> serverId` once a create ack's. `SyncWorker` resolves a child's parent FK from `IdMapper` only at serialization time for the outgoing request — local FK columns stay in local-UUID space, no bulk FK rewrite needed.
  - **Pull-sync reconciliation**: full-list fetch (no pagination exists) matched by `serverId`; new server-side records get a fresh local row; rows in `PENDING_UPDATE`/`PENDING_DELETE` are **not** overwritten by a pull (local pending write wins until actually sent); local rows missing from a fresh pull are tombstoned (`deletedLocally = true`).
- **Outbox / write-queue**: new `PendingOperationEntity` (`core/sync/PendingOperationEntity.java`) + `PendingOperationDao` — `opId`, `entityType`, `entityLocalId`, `opType` (CREATE/UPDATE/DELETE), `payloadJson`, `sequenceNumber` (FIFO order), `dependsOnOpId` (nullable — set when a child's parent isn't yet `SYNCED`, e.g. Unit depends on its Property's create op), `attemptCount`, `lastErrorMessage`.
  - Collapsing: a second local edit before sync mutates the existing pending op's payload in place; deleting a never-synced CREATE just removes both rows.
  - Ordering: `SyncWorker` drains ops by `sequenceNumber`, deferring any op whose `dependsOnOpId` is still present — handles Property-before-Unit, Tenant-before-Agreement, Invoice-before-Payment without a hand-built topo-sort.
  - Capped retries (WorkManager backoff) → past max, entity flagged `CONFLICT`, surfaced on a new **"Sync Issues"** screen under Settings.
  - Retrofit calls for mutations live only in per-entity `EntitySyncHandler` implementations (`pushCreate`/`pushUpdate`/`pushDelete`/`pullAll`), dispatched by `SyncWorker` via a `Map<String, EntitySyncHandler>`.
- **`SyncWorker`** (`core/sync/SyncWorker.java`, WorkManager): drains outbox, then runs `pullAll()` per handler. Triggers: `ConnectivityObserver` (`ConnectivityManager.NetworkCallback`) fires expedited sync on reconnect; periodic WorkManager sync (~15 min, `NetworkType.CONNECTED`); manual pull-to-refresh (routes through `SyncScheduler`, fragments still just observe Room `LiveData`); app-foreground trigger via `ProcessLifecycleOwner` (debounced ~2 min). All converge on one `WorkManager.enqueueUniqueWork("landlord-sync", ...)` chain to avoid overlap.
- **Conflict policy (v1, explicit)**: client-timestamp last-write-wins for updates (matches existing backend behavior — no versioning). Pull-sync never clobbers a locally-pending write. DELETE-vs-concurrent-change collision is server-wins-with-notice: drop local op, tombstone/update row, surface one-line in-app notice — no field-level merge. Proper optimistic locking (`updatedAt`/`@Version` + 409) flagged as **Phase-2 backend recommendation**, same tier as the pagination flag.
- **Stays online-only**: Reports, AI Insights, PDF/XLSX export/receipts — server-computed/generated, can't be produced offline. Shared `RequiresInternetBanner` widget disables these entry points when offline; already-downloaded files remain viewable via existing FileProvider flow regardless of connectivity.

## 3. Auth flow — must not block offline use

- Screens: Login → OTP verification → main app; Forgot Password → Reset OTP → New Password.
- `TokenManager` (EncryptedSharedPreferences, AES256-GCM) stores access/refresh tokens + expiry — now purely for the sync layer, not a gate on app usability.
- `AuthInterceptor` (OkHttp): attaches `Authorization: Bearer <access>` + `x-refresh-token` on every authenticated request; reads response header `x-access-token` and persists rotated token — mirrors `auth.interceptor.ts` exactly, not a classic 401-refresh pattern.
- **Revised**: on unrecoverable 401/403 with no rotation during a background `SyncWorker` run, do NOT force-logout/kick to `AuthActivity`. Instead mark `needsReauth` (`SyncStatusRepository`) and stop retrying (no point hammering a dead token) — outbox stays intact, untouched, ready to flush on re-login. A non-blocking banner ("Changes saved locally — will sync once you sign in again") appears app-wide; tapping it routes to re-login, dismissing it doesn't block other use. On successful re-login: clear `needsReauth`, trigger immediate sync to flush the outbox. Only a true cold-start with zero stored tokens ever (first install, explicit logout) routes to `AuthActivity`.
- Biometric unlock (`BiometricPrompt`) as optional local re-auth gate on cold start — local app-unlock convenience, independent of backend token validity, unaffected by the above.

## 4. Navigation / IA

12 modules won't fit a flat bottom bar (Material caps bottom nav at 3–5 items). Structure:

- **Bottom nav (5)**: Dashboard, Properties, Tenants, Payments, More.
- **More** → grid/drawer grouped by theme: *Operations* (Rentals, Maintenance, Marketplace), *Finance* (Expenses, Ledger, Reports), *Communication* (Messages, Notifications), *Intelligence* (AI Insights), *Account* (Settings).
- Navigation Rail auto-applies on large-screen/foldable configs (Material 3 adaptive layout) sharing the same nav graph as phone bottom-nav.
- Nested XML nav graph per module, multi-backstack via `NavigationUI.setupWithNavController(..., saveState)` so tab switches preserve scroll/back position.

## 5. Screens (shared patterns, then per-module deltas)

Three reusable base patterns built once, reused everywhere:

1. **List** — `BaseListFragment<T>`: RecyclerView + SwipeRefreshLayout + DiffUtil adapter, states driven by `Result<T>` (skeleton shimmer / empty-state+CTA / error+retry), SearchView + Chip filters, FAB for create.
2. **Detail** — `BaseDetailFragment`: collapsing-toolbar header, sectioned MaterialCardView blocks, BottomSheetDialogFragment for quick actions (record payment, change status) instead of full navigation.
3. **Form** — `BaseFormFragment`: TextInputLayout fields with Material validation, sticky bottom Save button, shared `PhotoPickerComponent` (reused by properties/units/maintenance).

Per-module deltas:

| Module | Screens | Notes |
|---|---|---|
| Dashboard | KPI card grid + activity feed | From `DashboardSummaryEntity` cache |
| Properties/Units | List → Property Detail (expandable unit accordion) → Unit Detail → Form | ad-pause/ad-repost as quick-action chips |
| Tenants | List → Detail → Register (multi-step) → Move-out flow | live `active-by-nid` duplicate check during registration |
| Rentals | Agreement view nested in Tenant Detail | no standalone list — matches backend shape |
| Payments | Invoices List/Detail, Payments List, Record Payment form, Receipt PDF view | download via `/api/payments/{id}/receipt`, open via FileProvider/share sheet |
| Marketplace | Ad list/detail/edit, Requests list/detail (approve/reject) | cross-app requests show source badge |
| Expenses | List → Add Form only | no edit endpoint server-side — don't offer edit |
| Ledger | Single composed feed (date-filterable) | client-side join of payments+invoices+expenses, no backend endpoint |
| Reports | Report-type picker (chips) → date range → View (JSON summary) / Export (PDF/XLSX) | **online-only** (`RequiresInternetBanner`), stream via OkHttp to app storage, share sheet |
| Insights | Chat-style RecyclerView | **online-only** (`RequiresInternetBanner`), maps to single `POST /api/insights/chat` |
| Maintenance | Tickets List → Detail (status timeline) → Create (photo) | status change via bottom-sheet |
| Messages | Conversations List → Thread | sent/received bubble view-types, polling until FCM lands |
| Notifications | List, swipe-to-dismiss, tap-to-read | `ItemTouchHelper` swipe |
| Settings | Profile, Security, Appearance, Notification prefs | mirrors existing web settings tab |

## 6. UI/UX design system

- Material 3 theme (`Theme.Material3.DayNight.NoActionBar`), dynamic color via `DynamicColors.applyToActivitiesIfAvailable()` (Android 12+), static brand palette fallback (reuse existing LandLord brand colors/logo assets) on older OS.
- Full M3 type scale defined once, applied via `TextAppearance.Material3.*` styles.
- KPI cards: MaterialCardView, elevation + rounded corners, icon + number + trend delta, grid layout.
- Standardized `StateView` (loading shimmer / empty+CTA / error+retry) swapped via ViewFlipper inside base list/detail fragments.
- Full day/night resource variants, verified against custom-drawn elements (charts, KPI cards).
- Modern Views-idiomatic trend elements: BottomSheetDialogFragment, Chip/ChipGroup filters, ItemTouchHelper swipe actions, SwipeRefreshLayout, CollapsingToolbarLayout with parallax photo headers, Material Motion (`MaterialSharedAxis`, `MaterialContainerTransform`) for list→detail and tab transitions.
- Accessibility: 48dp touch targets, content descriptions, contrast-checked both themes, TalkBack-tested login + payment flows.

## 7. Backend integration touchpoints

- No CORS change needed (browser-only concern).
- Pagination: not required for v1 (client-side fetch+cache), recommend as Phase-2 backend enhancement once portfolios grow.
- PDF/XLSX export: no backend change, endpoints already return binary — purely client-side streaming + FileProvider.
- **Push notifications (FCM): confirmed net-new backend work** — device-token entity + registration endpoint + Firebase Admin SDK integration + dispatch hooks wherever `Notification` entities are created today. Planned as its own workstream in Phase F, not bundled silently.
- Before finalizing the Android token-rotation `Authenticator` fallback, confirm there truly is no `/auth/refresh` endpoint (grep `Parts/auth` once more during Phase A) since silent-rotation-only means a fully expired refresh token has no recovery path except re-login.

## 8. Phased build order — actual outcome (built 2026-10-01/02, compressed into one continuous session rather than the original ~16–20 week estimate)

- **Phase A — DONE.** Gradle scaffold, M3 theme, network layer (`AuthInterceptor` matching `x-access-token`/`x-refresh-token` exactly), Room schema v2 (`SyncMetadata`, `PendingOperationEntity`/Dao, `IdMapper`/`IdMappingEntity`, `EntitySyncHandler`, `SyncWorker`, `ConnectivityObserver`, `SyncScheduler`, `SyncStatusRepository`). Login/token-rotation verified live. Dashboard built minimal (see Build status above), not the full KPI grid originally planned. Biometric gate built, but landed later (after a user-prompted gap-review pass), not in the original A/B window.
- **Phase B — DONE** for Properties/Units/Tenants CRUD + the Property→Unit dependency-ordering case (verified live by the user). **Not done**: shared List/Detail/Form base classes (bespoke per-screen instead), "Sync Issues" screen, tenant registration wizard, live duplicate-NID check, move-out flow.
- **Phase C — DONE** for invoice generation + record-payment + receipt PDF download/share (receipt landed in a later pass, not the same session as the rest of C). **Not done**: outstanding-balance display. Rental agreement view (nested in Tenant Detail, view + edit terms) also landed in a later pass.
- **Phase D — DONE**: Marketplace (pull + approve/reject — the first real use of an UPDATE-type outbox op, not just CREATE), Expenses (create/list, no edit per the confirmed no-PUT-endpoint constraint), Ledger (pure client-side `MediatorLiveData` composition, no new Room table).
- **Phase E — DONE**: Maintenance tickets (create + resolve + photo upload, the last as its own outbox entity type to avoid payload-shape collision with status-update), Messages (conversations + per-thread send, two separate sync handlers since one `EntitySyncHandler` = one entityType), Notifications (pull + mark-read). Reports and AI Insights correctly built online-only with a shared `RequiresInternetBanner`, landed in the same late pass as several Phase C/F items.
- **Phase F — DONE except FCM.** Settings (profile, 2FA, password+OTP change, notification prefs, dark mode, biometric toggle, **runtime-editable server address** — not in the original plan, added reactively after repeated LAN-IP breakage), logout (flagged as a real gap mid-build, then fixed). **FCM explicitly deferred** — needs a Firebase project + `google-services.json` from the user first; backend work (device-token table, Firebase Admin SDK, dispatch hooks) untouched.
- No product flavors, no automated test suite, no Material Motion/dynamic-color polish pass were done.

## 9. Testing / verification

- Emulator: run `landlord-backend` via existing `docker-compose.yml`/`dev-up.sh`, `adb reverse tcp:8080 tcp:8080` to reach host backend at `localhost:8080`.
- Physical device: **actual outcome differs from plan** — server address is runtime-editable (`ServerConfig`, SharedPreferences-backed, field on both the login screen and Settings), not a `BuildConfig.BASE_URL` product flavor. `BaseUrlInterceptor` rewrites every request's scheme/host/port from `ServerConfig` fresh per-call — deliberately not a "rebuild the Retrofit instance" fix, since several repositories/sync handlers cache their `ApiService` for process lifetime and would never see a rebuilt instance otherwise. `network_security_config.xml` permits cleartext broadly (`base-config`, debug-only) rather than pinning one IP, since the user edits the address themselves as their dev machine's LAN IP changes. No Gradle product flavors (`local`/`staging`/`production`) were built.
- Per-phase manual smoke tests against live backend:
  - A: login valid/invalid, OTP resend/expiry, force-kill+relaunch biometric gate, simulate near-expiry token to confirm silent `x-access-token` rotation.
  - B: create/edit/delete property, add units, upload unit photo, register tenant end-to-end (confirm `active-by-nid` check), move-out reverts unit to vacant.
  - C: generate invoice, record payment, download/open receipt PDF, confirm outstanding-balance recalculates.
  - D: publish ad, approve/reject request, add/delete expense, confirm ledger reflects new payment+expense together.
  - E: submit maintenance ticket w/ photo, change status, send/receive message, request each report type (PDF+XLSX), ask AI insights a question.
  - F: dark mode across all screens, biometric toggle, notification prefs, (post-FCM) trigger server-side event → confirm push arrives + deep-links correctly.
- Automated: JUnit/Mockito for ViewModel/Repository (mock ApiService, assert `Result<T>` transitions), Espresso for the 3 base UI patterns (covers all modules via inheritance), Room in-memory DB tests for DAO cache correctness.

## Critical files (reference during implementation)

- `Parts/auth/src/main/java/com/idb/auth/filter/AuthFilter.java` — token rotation contract
- `Parts/auth/src/main/java/com/idb/auth/common/constant/CommonConstants.java` — header name constants
- `LandLord-Angular-Project-R70/src/app/core/auth.interceptor.ts` — reference token-flow implementation to port
- `landlord-backend/src/main/resources/application.properties` — CORS/port config
- `landlord-backend/src/main/java/com/landlord/backend/**/*Controller.java` — authoritative API surface for Retrofit interfaces
- `LandLord-Angular-Project-R70/src/app/features/landlord/` — feature module structure to mirror, incl. `ledger.component.ts` (confirms client-side-only ledger)
- `landlord-android/core/sync/SyncWorker.java`, `PendingOperationEntity.java` + `PendingOperationDao.java`, `IdMapper.java` + `IdMappingEntity.java`, `ConnectivityObserver.java`, `SyncScheduler.java` — offline-sync core (Phase A)
- `landlord-android/core/network/ServerConfig.java` + `BaseUrlInterceptor.java` — runtime-editable backend address (not in original plan, added 2026-10-02)
- `landlord-android/core/auth/LogoutHelper.java`, `BiometricLockPrefs.java` — logout (wipes Room + cancels WorkManager + best-effort server invalidation) and biometric cold-start gate
- `landlord-android/feature/payments/ReceiptDownloader.java`, `feature/reports/ReportFileDownloader.java` — binary PDF/XLSX download + FileProvider open, same pattern reused across receipts and report exports
- `landlord-android/feature/maintenance/TicketPhotoSyncHandler.java` — photo upload as its own outbox entity type, avoids payload-shape collision with ticket status-update
- `landlord-android/ui/widgets/RequiresInternetBanner.java` — shared online-only banner (Reports, AI Insights)

## Offline-first database decision (2026-10-01)

Backend DB (Postgres 16) is local-only (Docker, dev machine), no cloud host. User wants the Android app itself to work fully offline with its own local DB, syncing to `landlord-backend` when online — not a 100% standalone app, local Room DB is source of truth day-to-day. Full offline-first sync architecture (outbox pattern, local-UUID PKs with server-ID remapping, conflict policy, sync triggers) designed and merged into Sections 2/3/5/8 above. See decision record: `/home/araf/.claude/plans/what-about-database-how-gentle-kurzweil.md`.
