# Cosmetic UI Design Plan v1 — LandLord & BariVara

Scope: visual polish only — make both apps more "eye-catching." This is separate from
`ui-ux_design_plan-v1.md`, which covers functional UX gaps (loading states, toasts,
accessibility). That plan is about things being *broken*; this one is about things
being *plain*. Not yet mentioned in `project-plan.md` — sits here until you decide
whether/when to schedule it.

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

- **Photo cards**: subtle zoom-on-hover (`transform: scale(1.04)` inside an
  `overflow:hidden` frame, ~250ms ease) — makes the grid feel alive without adding
  any new UI chrome.
- **Consistent aspect ratio + object-fit: cover** across all listing photos so the
  grid reads as a considered layout, not user-uploaded chaos.
- **A real "no photo yet" state** instead of an infinite plain gradient (this echoes
  a finding in the UX plan too, but it's a cosmetic opportunity as much as a
  functional gap) — a subtle line-icon illustration (house outline) on a soft brand
  tint, not just muted text on a gradient.
- **Hero imagery on the public landing/search page** — if there's real inventory,
  a large-format featured-listing hero (one big photo, not a slideshow of gradients)
  gives the homepage an actual "look at what's on here" moment instead of a
  generic hero card.

## 2. Micro-interactions & motion

Phase 4.3 already added hover-lift + shadow-md on cards and a tactile button press
state (`project-plan.md:722-728`) — that's the right instinct, just under-extended.
Cheap additions that compound:

- **Stagger-in on list/grid mount**: rows/cards fade+slide in with a small
  incremental delay (~30-40ms per item, capped at ~8 items) when a list first
  populates — reads as "considered," costs nothing functionally.
- **Number count-up on stat tiles** (ledger totals, dashboard KPIs) — animate from 0
  to the real value over ~500ms on mount. Small, but this is exactly the kind of
  detail that makes financial dashboards feel premium instead of static.
- **Page-level transition** (fade or subtle slide on route change) rather than a
  hard cut — Angular Router supports this natively via route animations, one config
  change, applies everywhere.
- **Skeleton shimmer** instead of blank-then-populate, once the loading-state work
  from the UX plan lands — this is a case where the *functional* fix (add a loading
  state) and the *cosmetic* opportunity (make that loading state good-looking)
  are the same piece of work, worth doing together.

## 3. Typography & "hero moments"

Current type scale (`styles.css` h1-h4) is disciplined but conservative — good for
tables, a little flat for the handful of places that should actually grab attention:

- **One outsized display moment per public page** — the LandLord homepage hero
  headline and BariVara's search-page headline are the two places a bigger, bolder
  weight (not a bigger palette) earns its keep. Everywhere else, keep the current
  restrained scale — this is deliberately not "make all headings bigger."
- **LandLord's stat tiles** could go beyond a bare number — a small sparkline or
  trend indicator next to the figure would let typography/data work together
  instead of the number sitting alone.

## 4. Color — use the accent more intentionally

Both apps already have a two-color brand system (blue/LandLord, orange/BariVara) plus
semantic success/danger/warning tokens. Underused:

- **Accent as a highlight, not a UI-wide tint** — e.g. one bordered/tinted "featured"
  or "recommended" listing card on BariVara using `--accent` as a background wash,
  standing out precisely because everything else stays neutral.
- **Gradient, used once, on purpose** — Phase 4 banned gradient-*everything*, which
  is right, but a single soft brand-color gradient behind one hero section (not
  every card) is a different move — a signature moment instead of a template tell.
  If this feels like it's toeing the banned line, skip it; flagging it as a
  judgment call, not a firm recommendation.
- **Charts/graphs, if any get built later**, should draw from the existing token
  palette rather than default chart-library colors — keeps financial data visuals
  on-brand instead of looking bolted-on.

## 5. Iconography consistency

Grep-worth-doing-later item: confirm icon usage (nav, buttons, status badges) comes
from one consistent set/weight rather than mixed sources — mismatched icon styles
was explicitly one of Phase 4's "AI-generated tells" to avoid, worth re-checking now
that more features have shipped since that pass.

## 6. Suggested rough sequencing

1. **BariVara listing card hover/zoom + real "no photo" state** — cheap, highest
   visual payoff for the app where imagery is the product.
2. **Stagger-in lists + stat-tile count-up** — cheap, compounds with the loading-state
   work already planned in the UX doc.
3. **Route transitions** — one Angular config change, applies everywhere.
4. **One hero display-type moment per app's public landing page** — moderate effort,
   highest visibility (first thing any new visitor sees).
5. **Accent-as-highlight pattern (featured card treatment)** — small effort, use
   sparingly, re-evaluate against the "AI-generated tells" list before shipping.

Nothing here requires a framework change or new dependency — all achievable in plain
CSS/Angular animations, consistent with the existing "no Tailwind/Material" setup.
