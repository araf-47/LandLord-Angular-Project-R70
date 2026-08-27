Solid stopping point — backend side (tasks 1–6) is fully wired, tested against real running servers, and cleaned up. Status:

Done and verified live:
- JDK 25 (Temurin, via SDKMAN) installed; both backends bumped to Java 25 / Boot 4.1.1 to match Parts/auth.
- Parts/auth embedded as a library in both landlord-backend and barivara-backend — separate users/roles tables inside each app's own existing database, no shared identity, no third service. Real login/logout, role-gated endpoints, machine-to-machine sync endpoints left open by design.
- Roles: LANDLORD+TENANT in landlord-backend; LANDLORD+TENANT+OWNER in barivara-backend.
- Guest browsing on BariVara (public search/listing-detail) still works with zero login, while writes stay OWNER-gated — needed a small, contained addition to Parts/auth itself (PUBLIC_GET_URLS, method-scoped public routes) since the shipped model could only do "public for every verb" or "role-gated for every verb."
- /api/tenants/me, /api/tenant-profiles/me, /api/owner-profiles/me added, backed by a new authUserId link column on Tenant/TenantProfile/OwnerProfile.
- Walk-in tenant registration now creates real login credentials in the same transaction (landlord sets a temp password, hands it to the tenant in person, per your call). BariVara tenant/owner signup does the same self-service.
- Hardened the obvious tenant-scoped reads (invoices, payments, maintenance tickets, notifications, conversations, favorites) so a TENANT token can't read someone else's data by editing a query param — their own linked id always wins over whatever the client sends.

Two real bugs found and fixed in Parts/auth along the way (worth knowing about if you reuse this module elsewhere):
1. Its global exception handlers were unscoped @RestControllerAdvices — embedding it silently turned every 404/409 in your existing controllers into an HTTP 200 with a generic body. Fixed by scoping them to com.idb.auth only.
2. A subtler one: once fixed, plain ResponseStatusExceptions from host-app controllers triggered a container error-dispatch that re-entered the security chain with no SecurityContext, turning real 404s into misleading 401s — same defect class AUTH.md already documented for its own AccessDeniedHandler, just not covered for host-app exceptions. Fixed with a small ApiExceptionHandler in each backend.

Known residual gaps, not closed in this pass (flagging rather than silently skipping): per-id operations like deleting a favorite, marking a notification read, or fetching a single maintenance ticket/booking-request by numeric id still don't check ownership — same pre-existing trust-the-id pattern as before, just not newly introduced by auth.

Still open: tasks 7 & 8 — replacing both Angular apps' AuthService stubs with real login, adding the Bearer/refresh-token interceptor, making roleGuard read the real role, and deleting the CURRENT_TENANT_ID_REAL/CURRENT_OWNER_ID_REAL stopgaps. That's a full frontend pass across two separate apps — want me to keep going straight into that now, or pause here?

✻ Cooked for 37m 37s
