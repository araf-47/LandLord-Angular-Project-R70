Here is Claude's plan:
╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌
 Phase 7.5 — OTP email delivery via Brevo

 Context

 Parts/auth (shared Maven module used by both landlord-backend and
 barivara-backend) already has full OTP logic — generate, BCrypt-hash,
 5-minute expiry, 3-attempt rate limit, validate — but delivery is stubbed:
 LoggingMailService just logs the code to the console. So today's OTP flow
 only works if you're watching server logs, not for a real user. This blocks
 Phase 16.4 (lockout/OTP cleanup job) and is the last piece standing between
 "auth logic done" and "auth usable by a real tenant/owner."

 There's exactly one send call site — OtpServiceImpl.sendOtpEmail, invoked
 from generateOtp(username) — shared by signup verification, login 2FA, and
 forgot-password. So swapping the stub covers all three flows at once; no
 per-flow branching needed.

 Decisions made:
 - Provider: Brevo. SendGrid killed its permanent free tier in 2025 (now a
   60-day trial, then $19.95/mo+). Brevo gives 300 emails/day free forever, no
   card required — fits this project's low OTP volume.
 - Scope: all flows, satisfied automatically by the single stub swap.
 - Add a basic resend cooldown now (separate from the existing 3-per-day
   otp-attempts limit), full lockout system stays deferred to Phase 16.4.

 User prerequisite (outside this plan's code changes)

 Sign up for Brevo, verify a sender identity/domain in their dashboard,
 generate an API key. Set as env vars, never commit:
 MAIL_PROVIDER=brevo, BREVO_API_KEY=..., BREVO_SENDER_EMAIL=....

 Implementation

 1. Config — three application.properties files (no shared/central

 properties file exists; mail.sink.* etc. are already triplicated the same
 way, so follow precedent):
 Parts/auth/src/main/resources/application.properties,
 landlord-backend/src/main/resources/application.properties,
 barivara-backend/src/main/resources/application.properties

 mail.provider=${MAIL_PROVIDER:}
 brevo.api.key=${BREVO_API_KEY:}
 brevo.sender.email=${BREVO_SENDER_EMAIL:}
 brevo.sender.name=${BREVO_SENDER_NAME:LandLord}
 brevo.api.base-url=${BREVO_API_BASE_URL:https://api.brevo.com}

 Extend the existing cache.config.params JSON blob (all three files) with:
 "otp-resend-cooldown":{"ttlMinutes":1,"maxSize":20000,"recordStats":true}
 The cache's own TTL is the cooldown window (same pattern the otp cache
 already uses for expiry) — no separate otp.resend.cooldown-seconds
 property, one source of truth.

 2. BrevoMailService (new)

 Parts/auth/src/main/java/com/idb/auth/common/service/BrevoMailService.java

 - @Service @ConditionalOnProperty(name="mail.provider", havingValue="brevo") —
   mirrors FileMailSink's existing conditional pattern, so LoggingMailService
   stays the safe default when mail.provider is unset.
 - Implements MailService.sendTemplatedEmail(MailInfo). Only templateName="otp"
   is handled today — build a small inline HTML string from templateModel
   (name, otp, expiryMinutes, logoUrl); no template engine exists in the
   project, don't add one for a single template.
 - Uses RestClient (not RestTemplate/WebClient) — this is already
   the established convention for inter-service HTTP calls in this codebase
   (BariVaraSyncService, LandlordSyncService). No new Maven dependency;
   spring-boot-starter-web already provides it.
 - POST {brevo.api.base-url}/v3/smtp/email, header api-key, JSON body
   (sender, to, subject, htmlContent).
 - Fail fast at construction if brevo.api.key/brevo.sender.email are
   blank (IllegalStateException) rather than silently no-op-ing.
 - Wrap the HTTP call: any failure maps to TraceableException (same pattern
   FileMailSink already uses) — never leak raw Brevo error bodies or the
   API key to the client or logs.

 3. BrevoConfig (new)

 Parts/auth/src/main/java/com/idb/auth/common/config/BrevoConfig.java

 - @Configuration @ConditionalOnProperty(name="mail.provider", havingValue="brevo")
 - @Bean RestClient.Builder brevoRestClientBuilder() — plain unconfigured
   builder; BrevoMailService applies .baseUrl()/.defaultHeader() itself
   so tests can bind MockRestServiceServer to this same builder bean.

 4. Resend cooldown in OtpServiceImpl

 Parts/auth/src/main/java/com/idb/auth/service/impl/OtpServiceImpl.java

 - Add CACHE_OTP_RESEND_COOLDOWN = "otp-resend-cooldown" to AuthConstants.
 - In generateOtp(username), before the existing attempts check: if the
   cooldown cache already has an entry for username, throw
   TraceableException ("please wait before requesting another OTP") — same
   style as the existing rate-limit throw.
 - On successful send, cooldownCache().put(username, Boolean.TRUE) — the
   cache's 1-minute TTL clears it automatically.
 - Add cooldownCache() private helper mirroring otpCache()/attemptsCache().
   No constructor signature change (already has CacheManager injected) —
   important because OtpServiceImplTest constructs OtpServiceImpl directly
   with positional args.

 5. Update OtpServiceImplTest

 Parts/auth/src/test/java/com/idb/auth/unit/OtpServiceImplTest.java

 - setUp()'s new CaffeineCacheManager("otp", "otp-attempts") must become
   new CaffeineCacheManager("otp", "otp-attempts", "otp-resend-cooldown") —
   required, not optional, or every existing generateOtp test throws
   IllegalStateException once the cooldown cache lookup is added.
 - Add a test asserting a second generateOtp call for the same user within
   the cooldown window is rejected. Don't sleep out the real TTL in a unit
   test — only assert rejection-while-warm.

 6. BrevoMailServiceTest (new)

 Parts/auth/src/test/java/com/idb/auth/unit/BrevoMailServiceTest.java

 - Bind MockRestServiceServer to the RestClient.Builder, construct
   BrevoMailService with it.
 - Assert: POST to /v3/smtp/email with api-key header and correct JSON
   body (htmlContent contains the OTP code). Assert a simulated server error
   surfaces as TraceableException, not a raw HTTP exception.
 - Follow existing MockitoExtension/AssertJ style from OtpServiceImplTest.

 7. Confirm test isolation

 No edit expected — confirm Parts/auth/src/test/resources/application-test.properties
 does not set mail.provider=brevo (already absent), so integration tests
 keep using LoggingMailService and never hit the real Brevo API.

 Verification

 1. cd Parts/auth && ./mvnw test -Dtest=OtpServiceImplTest,BrevoMailServiceTest —
    cooldown test + new Brevo test pass, no regression from the cache-manager
    constructor change.
 2. cd Parts/auth && ./mvnw test — full suite (Docker/Testcontainers needed
    for ITs) — confirm mail.provider unset keeps LoggingMailService active
    everywhere else.
 3. Real send smoke test: set MAIL_PROVIDER=brevo, BREVO_API_KEY,
    BREVO_SENDER_EMAIL as env vars, run landlord-backend, trigger the
    OTP-generating endpoint, confirm a real email arrives with correct code,
    name, expiry text.
 4. Repeat the same call immediately — confirm the cooldown rejection fires
    instead of sending a second email.
 5. Check logs: confirm BrevoMailService never logs the raw OTP or the API
    key (matching LoggingMailService's existing log line, which only logs
    template/recipient/subject).

 Files touched                                                                                                                                                                                     - Parts/auth/.../common/service/BrevoMailService.java (new)                                      - Parts/auth/.../common/config/BrevoConfig.java (new)
 - Parts/auth/.../service/impl/OtpServiceImpl.java
 - Parts/auth/.../constant/AuthConstants.java
 - Parts/auth/src/test/java/.../unit/OtpServiceImplTest.java
 - Parts/auth/src/test/java/.../unit/BrevoMailServiceTest.java (new)
 - Parts/auth/src/main/resources/application.properties
 - landlord-backend/src/main/resources/application.properties
 - barivara-backend/src/main/resources/application.properties
