# OTP email, explained — for this project specifically

A reference note, not a decision. Phase 7.5 stays open — see "Where this
stands" at the bottom.

## Where things actually are today

`Parts/auth` already has OTP code — generates codes, has a verify endpoint —
but delivery is stubbed with `LoggingMailService`, which just logs the code
to the console instead of emailing it. So the OTP flow *works* today only if
you're watching server logs, not for a real user. Phase 7.5 is swapping that
stub for a real mail provider. It also blocks Phase 16.4 (the lockout/OTP
cleanup job), which is deferred until 7.5 lands.

## What OTP email actually covers here

Two different uses bundled under "OTP" in this project:
- **Signup/login verification** — BariVara's self-signup wizard has a
  "verify" step (per the Phase 7 log) that presumably sends a code to prove
  the email is real before the account is created.
- **2FA / forgot-password** — a code emailed on login or password reset,
  which the tenant/owner types back in to prove it's them.

Both need the same mechanism: backend sends an email with a short code, user
reads it, types it back within some expiry window.

## How it differs from the payment gateway problem

The payment gateway needed a **public URL** — the gateway calls *your*
server (inbound webhook), which meant hosting had to exist first (see
`payment-gateway-explained.md`). OTP email is the opposite direction: *your*
server calls *out* to an SMTP/email API. Nothing needs to call you back. So
this one doesn't block on hosting at all — it can be wired and fully tested
on localhost right now.

## Real provider options (the ones already in project-plan.md §6)

- **SendGrid** — the common default pick. Generous free tier (roughly 100
  emails/day), simple REST API, good docs. Usual go-to for "just send
  transactional email."
- **AWS SES** — cheapest per-email at scale, but rougher setup: needs an AWS
  account, and starts in a sandbox mode that can only email
  *verified* addresses until you request production access. Overkill unless
  you're already on AWS for other reasons.
- **Twilio** — listed in project-plan.md, but that's for **SMS** OTP
  specifically, not email. Relevant only if "text me a code" is ever wanted
  instead of "email me a code." Skip it for email-only OTP.

For email-only OTP, SendGrid is the low-friction default.

## What wiring it up actually involves

1. Sign up with the provider, verify a sending domain or address — SendGrid
   won't send from an unverified sender.
2. Get an API key and put it in `application.properties`/an env var — not
   hardcoded. Same secrets-hygiene issue already flagged in
   `hosting-explained.md` re: the DB password currently sitting in plain
   text in that file.
3. Replace `LoggingMailService`'s implementation with a real HTTP call to
   the provider's send-email endpoint. Since it's described as a drop-in
   stub, the interface it implements presumably already exists — this is a
   swap, not new plumbing.
4. Decide code expiry (5-10 minutes is typical) and resend/rate-limit rules
   — stopping someone from spamming "resend code" ties directly into the
   lockout mechanism Phase 16.4 is waiting on.

## Realistic sizing

Smaller than the payment gateway problem: no public URL, no webhook, no
pending-payment-style state redesign needed. Mostly: pick a provider, verify
sender identity, swap one stub class, add expiry/rate-limit rules. The bulk
of the OTP *logic* (generate, verify, store) already exists in `Parts/auth`
per the plan notes — this is a delivery-mechanism swap, not new auth
architecture.

## Where this stands

`project-plan.md` Phase 7.5 stays open; §6 open decisions still lists the
email/OTP provider choice as unresolved, on hold alongside real auth. This
doc exists so the tradeoffs are understood ahead of time — no provider
chosen yet.
