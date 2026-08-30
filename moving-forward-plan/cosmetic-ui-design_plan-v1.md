# Cosmetic UI Design Plan v1 — LandLord & BariVara

Scope: visual polish only — make both apps more "eye-catching." This is separate from
`ui-ux_design_plan-v1.md`, which covers functional UX gaps (loading states, toasts,
accessibility). That plan is about things being *broken*; this one is about things
being *plain*. Referenced from `project-plan.md` §4.5 follow-up notes (2026-08-29
scoping, 2026-08-30 batch 1+2 progress).

**Status (2026-08-30):** batch 1 (`c5ef6d3`), batch 2 (`a23d967`), and batch 3
(hero typography + BariVara hero imagery) done — see inline ✅ markers below.
Left open on purpose, both judged more costly than a cosmetic tweak: **sparkline**
(no historical/multi-period data exists anywhere in the billing model to drive one —
needs new backend/data plumbing, not just CSS) and **skeleton shimmer** (the loading
markup is inline-repeated in 35 places across both apps, not componentized — doing
it right means touching all 35, not a quick add). Revisit either if/when the
underlying blocker is worth solving on its own.

## 0. The constraint this has to work inside

Phase 4 (`project-plan.md:710-717`) already made a deliberate call: avoid the
"AI-generated landing page" look — gradient-everything, glowing shadows, generic
marketing copy, mismatched icons, empty centered template layouts. That call was
correct and shouldn't be reversed wholesale.

The tension: "eye-catching" and "doesn't look AI-generated" pull in opposite
directions if done lazily. Scattering shiny gradients/glows across every card reads
*more* template-generic, not less. The way out is **selective boldness** — one strong
visual moment per screen, restraint everywhere else — rather than turning the
saturation up uniformly. Everything below is written with that filter already applied.

---

## 1. Photography & imagery (BariVara — highest leverage)

BariVara is the consumer-facing marketplace side; it's the one app where imagery
*is* the product. Right now (`styles.css:510-518`) missing-photo listings get a flat
gradient placeholder — fine as a fallback, but real listing photos deserve better
treatment than a plain `<img>` in a card:

- ✅ **Photo cards zoom-on-hover — done (batch 1).** `transform: scale(1.04)` on
  `.listing-card:hover .listing-card-image.img-cover`, clipped by the card's
  existing `overflow: hidden` — only fires on real photos, not the no-photo state.
- ✅ **Consistent aspect ratio + object-fit: cover — already in place before this
  plan was written**, not a gap: `.listing-card-image` is a fixed 150px height with
  `.img-cover` (`object-fit: cover`) on every listing photo.
- ✅ **"No photo yet" state — already in place before this plan was written**, not
  a gap: `browse.component.ts` already renders a house-outline SVG + "No photo yet"
  label on `--primary-light`, not a plain gradient. (This plan's original text was
  stale on this point — corrected 2026-08-30.)
- ✅ **Hero imagery on the public landing page — done (batch 3), BariVara.**
  `homepage.component.ts` picks the first real listing that has a photo
  (`heroListing` computed off already-fetched data — no invented data), shows it in
  a photo panel beside the hero copy; falls back to the original text-only hero when
  no listing has a photo yet. Responsive: stacks on mobile.

## 2. Micro-interactions & motion

Phase 4.3 already added hover-lift + shadow-md on cards and a tactile button press
state (`project-plan.md:722-728`) — that's the right instinct, just under-extended.
Cheap additions that compound:

- ✅ **Stagger-in on list/grid mount — done (batch 1).** `.listing-grid` (BariVara)
  and `.module-grid` (both apps) fade+slide in, ~30ms incremental delay capped at 8
  items, `prefers-reduced-motion: reduce` disables it.
- ✅ **Number count-up on stat tiles — done (batch 2), LandLord only** (BariVara has
  no stat-tile component). `stat-tile.component.ts` animates numeric values from the
  previous value to the new one over 500ms with ease-out, only when `value` is a
  `number` — the one string-valued tile (Occupancy, "x/y") is left untouched.
  Cancels in-flight animation on rapid updates and respects reduced-motion.
- ✅ **Page-level route transition — done (batch 2), both apps.** Used the native
  browser View Transition API via `provideRouter(routes, withViewTransitions())` —
  **not** `@angular/animations` (that package isn't installed in either app and
  wasn't needed; adding it would've been a real new dependency, not the "one config
  change" this bullet originally implied). Degrades to an instant cut on browsers
  without View Transition support. `prefers-reduced-motion` guard added in both
  stylesheets.
- 🔴 **Skeleton shimmer — left open, judged too costly for a cosmetic pass.** The
  loading-state work from the UX plan is real (see `ui-ux_design_plan-v1.md`), but
  it's the same `<p class="hint-text">Loading…</p>` idiom repeated inline 35 times
  across both apps, not one shared component. Adding shimmer properly means either
  touching all 35 sites or componentizing them first — bigger than this plan's scope.

## 3. Typography & "hero moments"

Current type scale (`styles.css` h1-h4) is disciplined but conservative — good for
tables, a little flat for the handful of places that should actually grab attention:

- ✅ **One outsized display moment per public page — done (batch 3), both apps.**
  Hero `h1` bumped to `font-weight: 800` with tighter letter-spacing, and sized up
  (LandLord 2.5rem, BariVara 2.2rem, up from base 1.75-2.1rem) — mobile breakpoints
  re-checked and given a matching override so the desktop size doesn't leak onto
  small screens. Everywhere else keeps the original restrained scale.
- 🔴 **LandLord's stat-tile sparkline — left open, judged too costly for a cosmetic
  pass.** Count-up animation is done (batch 2), but a sparkline needs a real
  historical/multi-period series and none exists: `collected`/`outstanding`/`net`
  are single current-month numbers everywhere in the billing model. Building this
  for real means new backend/data plumbing, not a CSS addition — out of scope here.

## 4. Color — use the accent more intentionally

Both apps already have a two-color brand system (blue/LandLord, orange/BariVara) plus
semantic success/danger/warning tokens. Underused:

- 🟡 **Accent as a highlight — CSS ready, not wired (batch 1).** `.listing-card.featured`
  (accent-tinted background wash + badge) exists in BariVara's `styles.css`, but no
  template applies it — there's no `featured` field on the listing data model, and
  wiring one in would mean inventing fake data rather than a real signal. Needs a
  real "featured" concept (landlord-paid promotion? highest-view listing?) before
  this gets used.
- **Gradient, used once, on purpose** — Phase 4 banned gradient-*everything*, which
  is right, but a single soft brand-color gradient behind one hero section (not
  every card) is a different move — a signature moment instead of a template tell.
  If this feels like it's toeing the banned line, skip it; flagging it as a
  judgment call, not a firm recommendation.
- **Charts/graphs, if any get built later**, should draw from the existing token
  palette rather than default chart-library colors — keeps financial data visuals
  on-brand instead of looking bolted-on.

## 5. Iconography consistency

✅ **Audited 2026-08-30 (batch 1) — clean, no fix needed.** Both apps use inline SVG
exclusively for icons — no `material-icons`, Font Awesome, Feather, Lucide, or
Bootstrap Icons mixed in. Nothing to change here.

## 6. Suggested rough sequencing

1. ✅ **BariVara listing card hover/zoom + real "no photo" state** — hover/zoom done
   (batch 1); "no photo" state and aspect-ratio turned out to already exist before
   this plan was written.
2. ✅ **Stagger-in lists + stat-tile count-up** — done (batch 1 + batch 2).
3. ✅ **Route transitions** — done (batch 2), via native View Transitions API, not
   `@angular/animations`.
4. ✅ **One hero display-type moment per app's public landing page** — done (batch 3),
   both the typography bump and BariVara's hero imagery.
5. 🟡 **Accent-as-highlight pattern (featured card treatment)** — CSS shipped
   (batch 1), not wired in — blocked on a real "featured" data signal, not effort.

Remaining open items: sparkline and skeleton shimmer (both explicitly left open,
2026-08-30 — judged more costly than a cosmetic tweak, see §2/§3 above) and
featured-card wiring (blocked on data, not effort — see §4). All three still
require no framework change or new dependency when their blocker is eventually
resolved — achievable in plain CSS/Angular, consistent with the existing
"no Tailwind/Material" setup.
