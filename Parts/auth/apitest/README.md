# Black-box API tests

Playwright's `request` fixture driving the packaged jar over real HTTP. No browser
is involved and there is no access to the Spring context or the database — if
something only works because a test shares the JVM, it fails here.

This is deliberately a different kind of test from `src/test/java/**/it`: those
have `JdbcTemplate` and can seed fixtures directly. Here every user is created
through `POST /api/v3/user/register` as the seeded administrator, and there is no
truncation between tests, so names are made unique per run instead.

## Running

```bash
docker compose up -d           # PostgreSQL on 5462, two databases
mvn -f ../pom.xml package -DskipTests -Dspringboot.repackage.skip=false
npm install
npx playwright test            # starts both service instances itself
```

`-Dspringboot.repackage.skip=false` is required: the default build produces only
the thin jar, which is not runnable. It writes `auth-<version>-exec.jar` alongside
the thin jar rather than replacing it.

`npx playwright test --project=api` runs the main suite;
`--project=ip-blocking` runs only the blocking one. Point the suite at an
already-running deployment with `AUTH_BASE_URL` / `AUTH_IPBLOCK_BASE_URL`, which
also skips the managed `webServer`.

`docker compose down -v` when finished.

## Two service instances

`IpBlockingServiceImpl`, `IpBlockingFilter` and `IpBlockController` are each
`@ConditionalOnProperty(auth.ip.block.enabled)`, so `/api/v3/ip-block/**` does not
exist at all unless the service was started with blocking on. That is a different
bean graph, not a runtime flag, so it needs its own process:

| | port | database | blocking |
|---|---|---|---|
| `start-service.sh` | 18080 | `auth_apitest` | off |
| `start-service-ipblock.sh` | 18081 | `auth_apitest_ipblock` | on, threshold 3 |

The blocking instance runs with `ddl-auto=create` so a block persisted by a
previous run cannot wedge the next one's readiness probe.

## How the OTP is obtained

The service stores OTPs BCrypt-hashed and never returns one, and the default
`LoggingMailService` logs only the recipient and template — never the code. So
`start-service.sh` sets `mail.sink.enabled=true`, which activates `FileMailSink`:
a development mail catcher that writes each mail as JSON under `.mail-sink/`,
the local equivalent of pointing SMTP at MailHog.

**`mail.sink.enabled` must never be true outside local or CI** — it puts one-time
passwords on disk in plaintext. It is off by default and off in the Java tests,
which mock `MailService` instead.

## Layout

| spec | covers |
|---|---|
| `01-auth.spec.ts` | login, JWT shape, bearer auth, forged/malformed tokens, enumeration, statelessness |
| `02-authorization.spec.ts` | the permissions.json matrix, wildcards, `@PreAuthorize`, admin-only writes |
| `03-tokens.spec.ts` | logout-all revocation, tampered and `alg=none` tokens, password-change invalidation |
| `04-two-factor.spec.ts` | OTP challenge, single-use, account binding, generation cap, cache clear |
| `05-lockout-and-reset.spec.ts` | the lockout boundary, admin unblock, forgot-password, change-password |
| `06-user-management.spec.ts` | registration validation, duplicates, role change, malformed input |
| `07-ip-blocking.spec.ts` | thresholds per attempt type, 429 envelope, operator self-rescue (second instance) |

All 21 mapped endpoints are exercised across the two projects.
