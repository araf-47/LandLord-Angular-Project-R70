Phase 7 — Real auth, using Parts/auth, no SSO between apps

Context

Both apps currently run wide-open backends with hardcoded stopgap identities
(CURRENT_TENANT_ID_REAL, CURRENT_OWNER_ID_REAL in each Angular app's
core/current-*.ts). Phase 7 replaces that with real login, using the
existing Parts/auth module (com.idb.auth) as the authentication engine —
not writing a new auth system, wiring in the one you already built and
tested (337 unit/integration tests + 66 Playwright tests, JWT + 2FA/OTP +
lockout + IP blocking + role/permission RBAC via permissions.json).

Explicit product decision driving this design: no single sign-on between
the two apps. A LandLord tenant and a BariVara tenant are different accounts
even if it's the same human — BariVara requires its own signup. This means
Parts/auth gets embedded twice, once per backend, each with its own
users/roles rows living inside that backend's own existing database
(landlord_db, barivara_db). No third auth service, no shared identity
table, no network hop for token verification — each backend verifies its own
tokens in-process, exactly like today's deployment shape (still just two
backends, two ports: 8080 and 8081).

Role sets (per backend, independent)

┌──────────────────┬─────────────────┬─────────────────────────────────────────────────────────┐
│     Backend      │      Roles      │                           Who                           │
├──────────────────┼─────────────────┼─────────────────────────────────────────────────────────┤
│ landlord-backend │ LANDLORD,       │ you (landlord), your registered tenants                 │
│                  │ TENANT          │                                                         │
├──────────────────┼─────────────────┼─────────────────────────────────────────────────────────┤
│ barivara-backend │ LANDLORD,       │ you again (separate BariVara account, landlord-linked   │
│                  │ TENANT, OWNER   │ dashboard), public renters, public apartment owners     │
└──────────────────┴─────────────────┴─────────────────────────────────────────────────────────┘

Dashboards stay exactly as they are today (LandLord core, LandLord tenant,
BariVara tenant, BariVara owner, BariVara landlord-linked) — this phase only
replaces how the app knows who's logged in, not the UI.

Prerequisites

- Install JDK 25 (official Adoptium/Temurin apt repo — Parts/auth's pom
pins java.version=25; this machine only has JDK 21 today). Bump
landlord-backend/pom.xml and barivara-backend/pom.xml to
java.version=25 too, and their Spring Boot parent from 4.1.0 → 4.1.1
to match the version Parts/auth was actually built/tested against.
- mvn install on Parts/auth once, so com.idb:auth:0.0.1-SNAPSHOT
(the thin jar, ~176KB) is in the local Maven repo for both backends to
depend on.

Backend integration (same pattern, done twice — landlord-backend and barivara-backend)

1. Add dependency: com.idb:auth:0.0.1-SNAPSHOT + spring-boot-starter-security
to each backend's pom.xml.
2. Scanning: each backend's main class (LandlordBackendApplication,
BarivaraBackendApplication) lives in a different package root than
com.idb.auth, so Boot's default component/entity scan won't see the
auth classes. Add explicit @ComponentScan(basePackages = {"com.landlord.backend", "com.idb.auth"}),
@EntityScan(basePackages = {..., "com.idb.auth.model"}), and
@EnableJpaRepositories(basePackages = {..., "com.idb.auth.dao"}) on each
main class (mirror for barivara).
3. Call AuthApplication.initPublicUrls() explicitly at the top of each
backend's own main(), before SpringApplication.run(...). This is
required — Parts/auth's own AuthApplication.main() never runs (we're
using it as a library, not launching it), and without this call
PUBLIC_URLS stays empty and AuthFilter treats /api/v3/auth/login
itself as protected, locking everyone out including first login.
4. application.properties additions per backend: JWT expiry envs,
credentials.default.* (seed one initial account per app — LANDLORD role
for both), permissions.file.path, cache config — copy the block from
Parts/auth/src/main/resources/application.properties, values unchanged
except per-app secrets/seed identity. ddl-auto=update already set in
both, so Parts/auth's tables (users, roles, permissions,
role_permissions, user_roles, blocked_ip, configurations) get
created inside the existing landlord_db/barivara_db automatically —
no new database needed.
5. Write a real permissions.json per backend (the shipped one only
covers Parts/auth's own user-management URLs). Walk every
@RestController in each backend's package tree (listed below) and
assign required role(s) per URL. Tenant-facing read endpoints get
TENANT, landlord-facing management endpoints get LANDLORD, BariVara's
owner endpoints get OWNER.
6. Replace ad hoc @CrossOrigin annotations with Parts/auth's central
CORS properties (cors.allowed-origins etc., already wired into
SecurityConfig) — remove the per-controller annotations once the real
security chain owns CORS, so there's one source of truth instead of two.

Existing controllers to classify into permissions.json — landlord-backend:
TenantController, PropertyController, UnitController, BillingController,
MaintenanceController, MarketplaceController, MessagingController,
NotificationController. barivara-backend: ProfileController,
PropertyController, UnitController, ListingController,
FavoriteController, BookingController.

Authorization hardening (new work these controllers don't have today)

permissions.json only checks "does this role have access to this URL" —
it does not stop a TENANT from passing someone else's tenantId in a query
param, which is exactly how every tenant-scoped endpoint works right now
(GET /api/invoices?tenantId=, GET /api/payments?tenantId=, etc. all trust
the caller-supplied id). Once real tenant logins exist, these must instead
derive the tenant/owner id from the authenticated principal, not the
request param:
- landlord-backend: add GET /api/tenants/me (resolves Tenant by
authUserId from the security context) and use it as the sole source of
"which tenant" on the tenant-side pages — the tenantId query param stays
for landlord-side use (landlord can look up any tenant) but a TENANT
caller's own id is never taken from the request.
- barivara-backend: same shape — GET /api/tenant-profiles/me,
GET /api/owner-profiles/me.

Bridge fields (identity → business record)

- landlord-backend: add Long authUserId (nullable) to
Tenant (com/landlord/backend/tenant/Tenant.java).
- barivara-backend: add Long authUserId (nullable) to TenantProfile and
OwnerProfile (com/barivara/backend/profile/).

Provisioning, per your decision (temp password at registration):
- LandLord walk-in registration (POST /api/tenants/register,
tenant-register.component.ts) gains a password field. On submit, backend
creates the Tenant row and an auth User (role TENANT, username =
phone or NID) with that password in the same transaction, stores the new
authUserId on Tenant. Landlord relays the password to the tenant in
person; tenant can change it via Parts/auth's existing
MANAGE_PASSWORD endpoint.
- BariVara tenant/owner signup (currently POST /api/tenant-profiles /
POST /api/owner-profiles, seed-only stubs from Phase 14.3) becomes real
self-service: signup form creates the auth User (role chosen at signup)
and the matching profile row together, linked by authUserId.
- BariVara's landlord-linked dashboard: you create a second, separate
BariVara account (role LANDLORD) — no link back to landlord-backend's
own login, by design (no SSO). It's the same synced-ads data either way,
just gated by a real login instead of an open endpoint.

Frontend (both Angular apps, same pattern, done twice)

- Replace each app's AuthService stub with real calls to its own backend's
/api/v3/auth/login (+ forgot-password/OTP where applicable).
- New HttpInterceptor per app: attaches Authorization: Bearer <access> and
x-refresh-token headers, reads a rotated x-access-token response header
back into storage (silent refresh, per Parts/auth's documented flow).
- roleGuard swaps its localStorage-string check for the real role claim
decoded off the stored access token.
- Delete core/current-tenant.ts / core/current-owner.ts stopgaps; tenant
dashboard pages call the new /me endpoints instead of a hardcoded id.

Verification

- Parts/auth's own test suite still passes standalone after the Boot
4.1.1/JDK 25 pom bump (mvn test).
- Per backend: log in as the seeded account, hit one previously-open
endpoint with no token (expect 401), with a wrong role (expect 403), with
the right role (expect 200) — proves permissions.json is wired, not just
present.
- Full walk-through: landlord registers a tenant with a temp password →
tenant logs into LandLord app with it → tenant dashboard loads via
/api/tenants/me (not a hardcoded id). Repeat independently on BariVara:
self-signup as tenant and as owner, confirm each lands on the right
dashboard and BariVara login is fully separate from the LandLord one just
used (no shared session).
- Confirm a TENANT token from tenant A cannot read tenant B's invoices by
editing the tenantId query param (the authorization-hardening fix above).
