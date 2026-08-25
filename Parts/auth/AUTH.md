# Authentication & Authorization

Port of the auth flow from `cob/emisolution` (Spring Boot 3, multi-module) onto this
single-module Spring Boot 4 service. Package root `com.idb.auth`; the reference
project's `common` module is flattened into `com.idb.auth.common`.

## The flow

```
POST /api/v3/auth/login  {username, password, otp?}
        │
        ├─ IpBlockingFilter ........ 429 IP_BLOCKED if this source IP is blocked
        ├─ AuthProvider ............ BCrypt check; per-account lockout counter
        ├─ 2FA on & no otp ......... 200 OTP_REQUIRED, code mailed, no token issued
        ├─ 2FA on & wrong otp ...... 401 INVALID_OTP
        └─ ok ...................... 200 SUCCESS {accessToken, refreshToken, expiresInSeconds}

GET /api/v3/<anything>   Authorization: Bearer <access>  [x-refresh-token: <refresh>]
        │
        ├─ AuthFilter → AuthManager
        │      ├─ access token valid ........... authenticate
        │      ├─ access expired + refresh ok .. authenticate AND return a new token
        │      │                                 in the `x-access-token` response header
        │      └─ otherwise ................... 401 via AuthEntryPoint
        └─ AuthorizationFilter ................ 403 unless the caller holds a role
                                                listed for this URL in permissions.json
```

### Tokens are signed per-user with the user's own password hash

There is no global JWT secret. `JwtUtil` HMACs each token with the owner's stored
BCrypt hash. Two consequences the rest of the design leans on:

- Changing a password invalidates every token that user holds, with no blacklist.
- Verifying a token requires loading the user first, which is why
  `AuthUtil.getUsernameFromAccessToken` reads the `sub` claim *without* verifying
  the signature. That unverified subject never grants access on its own — it only
  selects which key to verify against.

### Revocation

`POST /api/v3/user/logout-all` stamps `users.tokens_valid_after`. Any token whose
`iat` is at or before that instant is refused, so one row invalidates every
outstanding session for that user without touching the tokens themselves.

### Authorization is data, not code

`permissions.json` maps URL patterns to role names. `SecurityConfig` registers one
`requestMatchers(...).hasAnyAuthority(...)` per entry at startup, **in file order**,
because matching is first-match-wins. The file is re-imported only when its
`version` is greater than the value recorded in `configurations`.

`Role` *is* the `GrantedAuthority` and carries no `ROLE_` prefix, hence
`hasAnyAuthority` rather than `hasAnyRole`. Roles are not hierarchical: `ADMIN` is
refused by a `USER`-only URL.

### Two independent throttles

| | scope | trigger | state |
|---|---|---|---|
| account lockout | one user | `security.account-lockout.max-attempts` wrong passwords | `users.account_locked`, `locked_until` |
| IP blocking | one source IP | per-`AttemptType` thresholds under `auth.ip.block.*` | `blocked_ip` |

Account lockout clears itself when `locked_until` elapses, or via a password reset,
or via `POST /api/v3/user-block/unblock`. IP blocks clear lazily when `unblock_at`
elapses, or via `POST /api/v3/ip-block/unblock`.

IP blocking is **off** by default (`auth.ip.block.enabled=false`), which selects
`NoOpIpBlockingServiceImpl` and leaves `IpBlockingFilter` out of the chain.

## Running

Needs **JDK 25** (`<java.version>25</java.version>`) and a PostgreSQL database.
PostgreSQL specifically — the permission lookups use `STRING_AGG` and `EXISTS`.

```bash
mvn spring-boot:run          # DB_URL / DB_USERNAME / DB_PASSWORD override the defaults
mvn test                     # 337 unit + integration tests; integration needs Docker
cd apitest && docker compose up -d && npx playwright test   # 66 black-box API tests
```

### Packaging

`package` and `install` produce the **thin jar** - this module's own classes only,
about 176 KB - and that is what goes into the local repository, so a dependent
module resolves a normal library artifact rather than a 62 MB executable.

Boot's parent pom binds `repackage` to the package phase, which would otherwise
replace the module jar with the fat one and demote the real jar to
`auth-<version>.jar.original`. `springboot.repackage.skip` defaults to `true` to
prevent that.

```bash
mvn package                                        # auth-<version>.jar          (thin, ~176 KB)
mvn package -Dspringboot.repackage.skip=false      # + auth-<version>-exec.jar   (runnable, ~62 MB)
```

The runnable jar carries an `exec` classifier, so it is published alongside the
thin jar instead of overwriting it - the main artifact is the thin jar in both
modes. `spring-boot:run` is a separate goal and needs neither.

## Tests

Three tiers, each answering a different question.

### Unit — 249 tests, ~6s, no Spring context, no database

Collaborators are mocked; the security-critical logic is exercised directly.

| class | covers |
|---|---|
| `JwtUtilTest` | expiry, silent refresh, per-user keys, the revocation watermark |
| `AuthUtilTest` | unverified subject extraction incl. smuggling attempts, username/password policy |
| `AuthProviderTest` | credential check, lockout counter, bearer pass-through, enumeration parity |
| `AuthManagerTest` | URI routing, attempt classification, IP-block exemption |
| `AuthServiceImplTest` | login orchestration, 2FA branch, remaining-attempt hint, password reset |
| `OtpServiceImplTest` | hashing, single use, both rate limits, cache key eviction |
| `IpBlockingServiceImplTest` | per-`AttemptType` thresholds, shared counters, lazy expiry |
| `UserServiceImplTest`, `PermissionAndRoleServiceTest` | registration validation, import ordering, version upsert |
| `AuthEntryPointTest`, `AccessDeniedAndFilterTest`, `ExceptionHandlerTest` | status mapping, header blanking, response committing |
| `UserModelTest`, `ExceptionUtilTest`, `ServiceExceptionTest`, `ConstantsAndDtoTest`, `StringUtilTest`, `ValidationUtilTest` | the lockout state machine, cause-chain walking, DTO and constant contracts |

### Integration — 88 tests, real Tomcat + real PostgreSQL 16 (Testcontainers)

Real HTTP on a random port via `java.net.http.HttpClient`. Only `MailService` is
mocked, because a plaintext OTP exists nowhere else to observe.

| suite | covers |
|---|---|
| `AuthFlowIT` | login, token use, missing/garbage/forged tokens, enumeration, validation |
| `AuthorizationIT` | the permissions.json matrix, wildcards, `@PreAuthorize` as a separate layer |
| `TokenLifecycleIT` | expiry, silent refresh + rotation header, logout-all, password-change invalidation |
| `TwoFactorIT` | OTP challenge, single-use, account binding, both rate limits |
| `AccountLockoutIT` | the lockout boundary, self-expiry, admin unblock, reset-to-unlock |
| `IpBlockingIT` | per-`AttemptType` thresholds, lazy expiry, operator self-rescue (own context) |
| `UserManagementIT` | registration + validation, change-password, role listing |
| `ConfigurationIT` | cache registration, JPA auditing, CORS preflight, session policy, CSRF, property binding |
| `ControllerContractIT` | malformed JSON, wrong verbs and content types, injection-shaped input, hash non-disclosure |
| `FullAuthWalkIT` | one continuous lifecycle, HTTP only, no JDBC shortcuts |

### Black-box API — 66 tests, Playwright against the packaged jar

See `apitest/README.md`. No Spring context, no database handle, no test hook: the
suite talks to a deployed jar over the network exactly as a client would. Runs two
service instances, because IP blocking is a different bean graph rather than a
runtime flag.

All 21 mapped endpoints are exercised by every tier that can reach them.

## Deliberately not ported

Not part of the auth flow; all from the reference's wider codebase:

- **WORM/crypto/licensing** — `AuthFileUtil`, `CryptoUtil`, `AppGuidUtils` (RSA
  keypair files, `chattr +i`, app GUID). Unused by authentication.
- **Maker–checker review** — `ReviewData`, `ReviewableModel`, `ReviewDao`.
- **Firebase, S3, the JDBC `@Filterable` DAO layer, springdoc/Swagger annotations.**
- **Gson `@SerializedName`** on entities — a wire-format concern of the reference's
  caching, not of auth.

**`MailService` is a seam, not an implementation.** `LoggingMailService` logs
instead of sending. OTP login and password reset are therefore non-functional
against real users until a real SMTP implementation is dropped in.

## Spring Boot 4 / Spring Security 7 migration notes

The reference targets Boot 3. These are the places the newer APIs forced a change,
not stylistic choices:

| reference | here | why |
|---|---|---|
| `AntPathRequestMatcher` | `PathPatternRequestMatcher` | removed in Spring Security 7 |
| `/**.html`, `/**.css` public URLs | dropped | suffix globs are not valid `PathPattern`s; this is an API-only service |
| `com.fasterxml.jackson.databind.ObjectMapper` | `tools.jackson.databind.ObjectMapper` | Boot 4 ships Jackson 3 as the default mapper |
| `SerializationFeature.WRITE_DATES_AS_TIMESTAMPS` | `DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS` | moved in Jackson 3 |
| `JsonNode.fields()` | `JsonNode.properties()` | renamed in Jackson 3 |
| `jjwt-jackson` | `jjwt-gson` | jjwt binds to Jackson 2; the Gson backend avoids two Jackson majors on one classpath |
| `new ContentCachingRequestWrapper(req)` | `(req, 64 * 1024)` | the unbounded constructor is gone in Spring Framework 7 |
| `@MockBean` | `@MockitoBean` | removed in Boot 4 |
| `TestRestTemplate` | `java.net.http.HttpClient` | moved to `spring-boot-restclient-test`; stdlib is simpler here and exposes response headers |
| `org.testcontainers:postgresql` | `org.testcontainers:testcontainers-postgresql` | artifacts renamed in Testcontainers 2.x |

## Defects in the reference, fixed here

Each of these is a behavioural change, not a port artifact.

1. **`AuthEntryPoint` was unreachable.** `AuthFilter` sits upstream of Spring
   Security's `ExceptionTranslationFilter`, so a thrown `AuthenticationException`
   escaped the security chain entirely and `GlobalExceptionFilter` flattened it to
   **HTTP 200 with a generic ERROR body**. The configured entry point never ran, so
   the status codes were wrong and the stale-token headers were never blanked.
   `AuthFilter` now invokes the entry point directly.

2. **`403` was reported as `401`.** The default `AccessDeniedHandler` calls
   `sendError(403)`, which makes the container run an ERROR dispatch to `/error`.
   That re-enters the security chain, but `AuthFilter` is a `OncePerRequestFilter`
   and skips error dispatches — so the re-entered chain found an empty
   `SecurityContext` and overwrote the honest 403 with a misleading 401.
   `AuthAccessDeniedHandler` commits the response itself, so no error dispatch
   happens.

3. **"Log out everywhere" left a usable token behind.** A JWT `iat` has whole-second
   resolution while `tokens_valid_after` has sub-second precision, so the
   reference's `iat < validAfter` compared `12:00:00 < 12:00:00` — false — and a
   token minted in the same second as the revoke survived it. Now `<=`, i.e. fail
   closed.

4. **The OTP rate limits never fired.** The reference annotates its counter helpers
   with `@CachePut`/`@Cacheable` and then calls them from `validateOtp` on `this`.
   Self-invocation bypasses the Spring proxy, so the counters were never persisted.
   `OtpServiceImpl` now reads and writes the cache directly.

5. **`clearCache` never cleared the counters.** It evicted `otp-attempts` by bare
   username, but that cache is keyed `otp_gen_<user>` / `otp_val_<user>`. Both keys
   are now evicted explicitly.

6. **The IP-block exemption was cancelled one layer down.** `IpBlockingFilter`
   exempts the unblock endpoints so an administrator whose own IP got blocked can
   recover — but `AuthManager` re-checked `isIpBlocked` unconditionally, making the
   exemption useless. The exemption now lives in
   `AuthConstants.isIpBlockExempt(...)` and both call sites consult it.

7. **`permissions.json` was re-imported on every start.** The version was persisted
   with a bare `UPDATE`, which affects zero rows when no row exists yet, so the
   recorded version stayed null forever. Now an upsert (`Configuration` entity).

8. **Token subject parsed by string splitting.** The reference does
   `payload.split("\"sub\":\"")[1]`. Now parsed as JSON, so a crafted payload cannot
   smuggle a different subject. Same for the username extracted for audit logging in
   `AuthManager`.

9. **Registration ignored the username policy.** `USERNAME_PATTERN` existed but
   `validateUserRegistration` only checked for blankness. It is now enforced.

10. **`AuthEntryPoint` read a thread-local instead of the request it was given.**
    Its logging call used `RequestContextHolder`, which only resolves because Boot
    registers `RequestContextFilter` ahead of the security chain. Invoked anywhere
    else it throws an NPE out of `commence`, which escapes to
    `GlobalExceptionFilter` and turns a clean 401 into a generic error. It now uses
    the `HttpServletRequest` parameter it already had.

11. **`RequestLogUtil` could throw out of an error path.** `reader.lines()` wraps
    read failures in an *unchecked* `UncheckedIOException`, which the old
    `catch (IOException | IllegalStateException)` did not cover. This helper is
    called from inside the exception handlers, so it would mask the very failure it
    was invoked to describe.

12. **The `jwt-secret-keys` cache was dead config.** `JwtUtil.generateKey` is
    `@Cacheable` but only ever self-invoked, so the proxy never applied — the same
    bug class as the OTP counters. Removed rather than fixed: `hmacShaKeyFor` only
    length-checks and wraps the bytes, so caching bought nothing and kept derived
    key material in a second place.

13. **`/actuator/health` was advertised public with no actuator on the classpath,
    and was not exempt from IP blocking.** A liveness probe gated by a security
    counter means a blocked address makes the instance look dead to its load
    balancer. The starter was added and `IpBlockingFilter` now exempts
    `/actuator/**` alongside the docs paths.

## Known limitation

**A blocked operator with no live token cannot self-rescue.** `IpBlockingFilter`
exempts `/api/v3/ip-block/unblock**`, but not `/api/v3/auth/login` — exempting
login would gut the brute-force protection the feature exists for. So recovery
needs a token obtained *before* the block, a different source address, or waiting
out `auth.ip.block.block.duration.hours`. This is asserted, not just documented:
see `login is NOT exempt` in `apitest/tests/07-ip-blocking.spec.ts`.

`/api/v3/ip-block/list` is likewise not exempt; only unblocking is.
