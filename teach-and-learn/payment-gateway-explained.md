# Payment gateway, explained — for this project specifically

A reference note, not a decision. Phase 10.8 stays on hold — see "Where this
stands" at the bottom.

## Where things actually are today

`POST /api/payments` already records real payments (rent + utilities) and
applies them to invoice balance (Phase 10.4). But it's landlord-entered only:
landlord manually types in "tenant paid me cash," clicks confirm, invoice
flips to `paid`. There's no path for a tenant to pay *through* the app.
That gap is Phase 10.8 — explicitly parked, "sounds complicated."

`pending-cash.component.ts` was retired (2026-08-19) because there's no
`pending → confirm` step server-side to back it — `POST /api/payments`
writes straight to `confirmed`. That detail matters below.

## What a payment gateway actually is

A middleman between your app and real money. Your app never touches card
numbers or bank/wallet PINs directly — the gateway does, and you just get
back a "paid" or "failed" result. That's what saves you from PCI compliance
and fraud-handling work.

Generic flow:
1. Tenant clicks "Pay rent" in your app.
2. Your backend asks the gateway for a payment session/URL (amount, invoice
   ID attached).
3. Tenant is sent to the gateway's own page (or an embedded widget) — enters
   card/bKash PIN/whatever *there*, never on your site.
4. Gateway processes it, redirects the tenant back to your app with a
   success/fail flag.
5. Gateway also calls your backend directly — a **webhook** — to confirm the
   result. This is the trustworthy signal, not the redirect (the redirect
   can be faked or interrupted by closing the browser tab).
6. Your backend only marks the invoice `paid` once the webhook confirms —
   not on the redirect alone.

## The 3 candidates already in project-plan.md (§6 open decisions)

- **bKash / Nagad** — dominant mobile wallets in Bangladesh. If tenants are
  BD-based, this is what they actually carry and use day to day. Integration
  docs exist but historically fiddly — sandbox access is a request/approval
  process, not instant signup.
- **Stripe** — best developer experience, excellent docs, huge ecosystem.
  Weak-to-no direct bKash/Nagad support though — mainly cards. Better fit if
  users are international or card-first.
- **SSLCommerz** — Bangladesh-focused aggregator that bundles bKash + Nagad +
  cards + bank transfer behind *one* integration, so you're not building 3
  separate wallet integrations by hand. Common default pick for BD SaaS for
  exactly that reason.

## Why it's genuinely nontrivial (not just "sounds scary")

- **Webhooks need a public URL.** Localhost can't receive a webhook call —
  this ties directly back to `hosting-explained.md`; hosting has to exist
  first, or at least a tunnel (ngrok-style) for testing.
- **Sandbox/approval friction.** bKash especially requires an approval step
  to even get test credentials — not a same-day signup.
- **Idempotency.** Gateways can call your webhook more than once for the
  same payment (retries on their end). Must not double-credit an invoice if
  the same webhook lands twice.
- **Pending state is currently missing.** `Payment`/`Invoice` go straight to
  `confirmed` today (10.4's simplification). A real gateway needs an actual
  `pending → confirmed/failed` lifecycle, since gateway payment isn't
  instant-and-certain the way "landlord typed in cash received" is. This is
  exactly what `pending-cash.component.ts` used to model before being
  retired for lack of a backend state to back it — it'd need reviving, or a
  new pending-payment concept, once a gateway is in play.

## Realistic sizing

Not "impossible-complicated" — more like: needs hosting with a public URL
first (Phase 19, still unpicked), needs a pending payment state added back to
`Invoice`/`Payment`, needs one webhook endpoint, needs picking one provider
and going through their account/sandbox approval.

Given tenants are presumably Bangladesh-based, SSLCommerz is the natural
default — one integration surface instead of three.

## Where this stands

`project-plan.md` Phase 10.8 stays on hold, your call. §6 open decisions
still lists bKash/Nagad, Stripe, and SSLCommerz as the unpicked options. This
doc exists so the tradeoffs are understood ahead of time — no gateway chosen
yet.
