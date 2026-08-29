# UI/UX Design Plan v1 — LandLord & BariVara

Grounded in the actual codebase as of 2026-08-29 (post Phase 16.3, both backends live).
Both apps use plain CSS (no Tailwind/Material/Bootstrap) — one `src/styles.css` design
system per app, imported globally, plus component-scoped styles for one-offs. This plan
extends that existing system rather than replacing it; nothing here proposes a UI
framework migration.

## 0. Where things actually stand

Phase 4 (`project-plan.md`) already did real design work: a deliberate type scale,
a restrained shadow system, hover/press states, a mobile off-canvas drawer, and an
explicit ban on "AI-generated landing page" tells (gradient-everything, glowing
shadows, generic marketing copy, mismatched icons). **Any suggestion below that would
reintroduce those tells is out of scope by your own prior call — flagged where relevant.**

What Phase 4 could not cover, because it predates the backend: real async state. Phases
6–16 wired both apps to live Spring Boot APIs, but the UI layer was never revisited for
what that implies. That gap is the single biggest finding in this audit and shows up
everywhere below.

### The headline gap: loading/error states

Only **1 of 56** feature components in LandLord (`auth/login.component.ts`) has any
loading-state handling. The other 55 — including `ledger.component.ts`, every
`properties/`, `payments/`, `tenants/` list — call real HTTP APIs with plain
`await Promise.all([...])` / service calls and **no try/catch, no loading indicator, no
error branch**. Example, `ledger.component.ts:110-134`: three chained `Promise.all`
calls against `PropertyApiService`, `UnitApiService`, `TenantApiService`, `BillingApiService`,
`MaintenanceApiService` — if any one of those five network calls fails (timeout, 500,
token expiry), the page just... does nothing. No spinner while it loads, no message when
it breaks. Same pattern confirmed in BariVara.

This was invisible during Part 1 because everything ran on synchronous mock data
(Phase 4.5 explicitly notes loading/error was "N/A" then). It's no longer N/A. This is
priority zero, not a nice-to-have — it's the difference between "looks done" and
"works when the network hiccups."

### Other confirmed gaps (grep-verified, not guesses)

- **Native `alert()`/`confirm()` for destructive actions**: `tenant/payments/history.component.ts:50`
  (`alert()` for a generated receipt — should be silent/toast, not a blocking dialog),
  `landlord/properties/unit-list.component.ts:66`, `landlord/properties/property-list.component.ts:66`,
  and BariVara's `owner/properties/unit-list.component.ts:73` (all `confirm('Delete this
  unit? This cannot be undone.')`). Browser-native dialogs can't be styled, don't match
  either brand, and are jarring on mobile.
- **No toast/notification-banner system anywhere** — zero matches for `toast` in either
  codebase. Every success/failure signal today is either silent, an `alert()`, or an
  inline `.error-text` that only some forms have.
- **No modal/dialog component** — zero matches for `Modal`. Anything resembling a
  confirm-step today is either a native `confirm()` or a full page navigation (the
  `steps`/`step-dot` pattern in `styles.css:544-564` is used for wizard-style flows, which
  is fine, but there's no lightweight in-page dialog for quick actions).
- **48 inline `style="..."` attributes** in LandLord's `features/` alone (e.g.
  `ledger.component.ts:57,60,64` — `style="color:var(--success)"` / `style="margin-bottom:1rem"`
  repeated ad hoc instead of a utility class). This is how spacing/color drift creeps in:
  every inline style is a place the design system isn't actually being enforced.
- **No dark mode** — zero `prefers-color-scheme` anywhere. Not necessarily wrong to skip,
  but worth a deliberate call rather than defaulting by omission (see §4).
- **Low ARIA coverage** — 8 total `aria-*` attributes across both apps combined. The
  hamburger `menu-toggle` button (`styles.css:171-179`, both apps) has no visible text and,
  from the grep, likely no `aria-label` either — a screen reader user hears "button,"
  nothing else.
- **Single breakpoint (860px)** in both apps — works for the sidebar-drawer swap but means
  no intermediate tablet treatment; dense tables (`ledger`, `payments/history`) go
  straight from full multi-column to horizontal-scroll with no in-between.
- **Reusable component surface is thin**: only `shared/logo.component.ts` exists per app
  (per Phase 4.7). Buttons/badges/cards are CSS classes applied per-template (`.btn`,
  `.badge`, `.card`) — consistent by convention, but there's no Angular component
  wrapping them, so there's nowhere to add shared behavior (e.g. a loading spinner
  built into a `<app-button [pending]="...">`) without touching every call site.

None of this contradicts Phase 4's visual language — the palette, type scale, and shadow
system are genuinely solid and worth keeping as-is. This plan is about the *states* and
*interaction layer* on top of that visual system, which Part 1 never had a reason to build.

---

## 1. Cross-cutting recommendations (apply to both apps)

These are shared-system changes — implement once per app's `styles.css` /new shared
components, they benefit every feature page automatically (the same leverage Phase 4.3
already relied on).

### 1.1 Async feedback system (priority: highest)

- Add a small `LoadingState<T>` convention (`{ status: 'idle'|'loading'|'error'|'ready', data?, error? }`)
  used via a signal in every component that calls an API service, replacing the current
  bare `await`.
- Add skeleton-loader CSS (`.skeleton` shimmer block, using existing `--border`/`--bg`
  tokens so it matches the palette, not a generic gray) for table rows and card grids —
  cheaper to build than per-page custom spinners and reads as more "designed" than a
  spinner-in-a-box.
- Add an `.inline-error` component/class for failed loads: icon + message + retry button,
  styled with existing `--danger` token. Reuse the same pattern Phase 4.5's empty-state
  blocks already use (`@empty` blocks with hint text) — error state is really just empty
  state's unhappy sibling, same visual weight.
- Add a **toast/banner system**: a single `<app-toast-host>` mounted once per layout
  (`landlord-layout`, `tenant-layout`, `owner-layout`, `public-layout`), a signal-based
  service to push messages. Use for: save confirmations, delete confirmations, sync
  events (e.g. "Ad reposted to BariVara"). This directly replaces the `alert()` calls
  in §0 and gives Phase 15/16's background-job outcomes somewhere to actually surface
  beyond the notifications panel.

### 1.2 Replace native `confirm()` with an in-app confirm dialog

A small `<app-confirm-dialog>` (title/body/confirm-danger button, focus-trapped,
Escape-to-cancel) replacing the three `confirm('Delete this...')` call sites. Keep the
copy identical ("Delete this unit? This cannot be undone.") — only the delivery
mechanism changes. This also becomes the foundation for any future "are you sure"
moment (move-out, ad pause, marketplace request rejection) instead of reaching for
`window.confirm` again.

### 1.3 Kill inline styles, extend the utility layer

Add a handful of spacing utility classes (`.mt-1`/`.mb-1`/`.gap-1` etc., or simpler:
`--space-xs/sm/md/lg/xl` tokens plus 4-6 margin/gap utilities) to replace the 48+
one-off `style="margin-bottom:1rem"` instances. Small effort, stops the slow drift
where "the design system" and "what's actually on screen" diverge component by
component.

### 1.4 Accessibility pass (concrete, not aspirational)

- Add `aria-label="Toggle navigation"` (or similar) to `.menu-toggle` in both apps'
  layout components — currently an icon-only button with no accessible name.
- Audit color contrast on badge combos (`styles.css` badge block): `.badge-vacant`
  (`#fef3c7`/`#92400e`) and similar pairs look fine at a glance but should be run through
  a contrast checker once, not assumed — cheap to verify, easy to regret.
- Add `:focus-visible` styling distinct from the current `input:focus`/button states —
  right now focus rings exist for form fields (`styles.css:349-352`) but not for
  `.btn`, `.module-tile`, `.listing-card`, or nav links, so keyboard-only navigation
  loses visible focus the moment you tab off a form.
- Ensure every icon-only control (delete buttons in list rows, the hamburger, chat send
  button if icon-only) has an `aria-label`. Grep shows 8 total today — this needs to be
  closer to "every non-text control."

### 1.5 Responsive: add one mid-breakpoint

A second breakpoint around 1024-1100px (matching the existing `.public-content`
max-width) for tablet — collapse `.module-grid`/`.listing-grid` to 2 columns instead of
jumping straight from 4+ columns to the mobile drawer layout. Low effort since the grids
already use `auto-fill, minmax()` — this is a targeted `minmax()` adjustment plus maybe
one more `@media` block, not a rebuild.

### 1.6 Dark mode — make it a deliberate decision, not a gap

Given the token system is already CSS custom properties, dark mode is a genuinely small
lift (define a `[data-theme="dark"]` block remapping the same variable names — no
component changes needed). Recommend scoping this as its own small phase rather than
folding it silently into other work: either commit to it (renters/landlords checking
bills at night is a real use case) or explicitly decide not to and drop it from future
audits.

---

## 2. LandLord-specific suggestions

LandLord is data-forward (tables, ledgers, financial figures) — the design priority here
is scanability and trust, not visual flourish. Phase 4.6 already made this call
deliberately ("data-forward tables" vs. BariVara's photo cards) — keep that.

- **Ledger/payments tables need row-level status color-coding beyond badges.** Right now
  income/expense rows in `ledger.component.ts` differentiate only via a `type` field in
  data, not a visual left-border or subtle row tint — a quick scan for "what went out
  this month" currently requires reading every row's badge instead of pattern-matching
  on color at a glance.
- **Numbers need consistent formatting treatment.** `ledger.component.ts:60-68` uses
  ad hoc inline-styled `<h2>` for Total in/out/Net — worth a `<app-stat-tile>` component
  (label, value, trend arrow optional, semantic color) so financial dashboards across
  `landlord/dashboard`, `ledger`, `expenses`, `tenant/payments` render figures
  identically instead of each page reinventing the stat-card markup.
- **Tenant-facing payment history (`tenant/payments/history.component.ts`)** currently
  uses `alert()` for "Receipt PDF generated" (§0) — beyond the toast fix, consider
  whether that flow should actually trigger a file download rather than a message
  claiming one happened; worth confirming with backend behavior.
- **Messages (`landlord/messages/message-thread.component.ts`)** — verify it has a
  proper empty/loading state for "no messages yet" and a distinct "sending..." state
  for the compose box, since this is now backed by Phase 12's real polling transport,
  not mock data — same async-state gap as §0 applies here specifically because it's the
  most latency-visible feature in the app.

## 3. BariVara-specific suggestions

BariVara is the consumer-facing marketplace side — photo-forward listing cards
(Phase 4.6), public search/browse flows, less "professional dashboard," more
"can a stranger use this in 30 seconds."

- **Listing cards have no placeholder-image treatment for missing photos.**
  `.listing-card-image` (`styles.css:510-518`) renders a gradient placeholder with
  muted text when there's no image — reasonable, but per project-plan's Phase 15 notes,
  landlord-linked listings *never* have a photo (`VacancyAdSync` carries no photo field
  yet). That means every synced listing shows the same gradient placeholder forever, not
  just transiently — worth either a more deliberate "no photo yet" state (icon + short
  label, not just a blank gradient) or fast-tracking the photo-sync gap itself.
- **Search bar (`.search-bar`, `styles.css:299-310`)** wraps on mobile but has no
  "clear filters" / active-filter-chip treatment — once a user has district + area +
  property type + price range set, there's no compact way to see or clear what's
  applied without opening the form fields again.
- **Booking request flow** — confirm there's a clear "request sent" confirmation state
  distinct from the browse view; per Phase 15.2 this posts cross-backend to LandLord's
  Marketplace inbox, so a tenant needs unambiguous feedback that the request actually
  went through (this is exactly what the toast system in §1.1 is for).
- **Owner dashboard vs. LandLord dashboard visual parity** — since some landlords use
  both apps (landlord-linked ads), the module-grid/dashboard-tile pattern is shared
  structurally (Phase 4.6 confirms same token system) — good. Keep enforcing that as
  new features land so BariVara's owner side and LandLord's landlord side don't visually
  diverge over time despite serving overlapping users.

---

## 4. Suggested rough sequencing

Not a formal phase number (that's your call, same as the rest of the backend order) —
just grouped by effort/impact so you can slot it wherever it fits:

1. **Toast system + confirm dialog + loading/error states for API calls** — highest
   impact, addresses the gap most likely to look broken in front of a real user, touches
   every feature page but each individual change is mechanical once the shared
   components exist.
2. **Spacing utilities to kill inline styles, focus-visible pass, aria-labels on icon
   buttons** — cheap, no visual risk, pure hygiene.
3. **Stat-tile component (LandLord), active-filter-chips (BariVara), mid-breakpoint** —
   moderate effort, visible polish.
4. **Dark mode** — scope as its own explicit decision, moderate effort given the token
   system already exists, but touches every screen so worth doing as a dedicated pass
   rather than piecemeal.

None of this blocks Phase 17 (testing) — if anything, building the loading/error state
system now makes Phase 17.2's API integration tests and 17.4's E2E tests easier to write
against, since there'll be an actual error state to assert on instead of silent failure.
