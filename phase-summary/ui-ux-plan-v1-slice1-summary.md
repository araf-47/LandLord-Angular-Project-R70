UI/UX Design Plan v1 — first slice done. Summary:

Built shared infra in both apps (LandLord + BariVara, each independently — separate Angular workspaces, no shared code path): signal-backed `ToastService`/`<app-toast-host>` and promise-based `ConfirmService`/`<app-confirm-dialog>` (focus-trapped, Escape-to-cancel), both mounted once in `app.html` above the router outlet so every layout gets them for free. Added `--space-xs/sm/md/lg` tokens plus `.text-success`/`.text-danger`/`.mb-*`/`.gap-*` utility classes to both `styles.css` files.

Wired up a representative slice to prove the pattern end-to-end rather than rolling it across all ~55 components at once:

LandLord — `ledger.component.ts` now has real loading/error/ready states (was a bare `Promise.all` with no error handling, silent failure on network hiccup); `properties/unit-list.component.ts` and `properties/property-list.component.ts` delete actions go through the new confirm dialog instead of native `confirm()`; `tenant/payments/history.component.ts` uses a toast instead of `alert()` for the receipt-generated message (kept the existing stub behavior — no real PDF download, that's a backend gap, not this pass's scope).

BariVara — `owner/properties/unit-list.component.ts` delete goes through the confirm dialog; `owner/listings/listing-list.component.ts` delete previously had **no confirmation at all** (a gap found during research, not previously known) — now guarded the same way; `public/listing-detail.component.ts`'s `book()` flow got a loading/disabled state on the button plus success/error toasts (previously no feedback at all while the request was in flight or if it failed).

Both apps verified with `ng build --configuration development` — clean, no compile/template errors.

**Update (same day, second pass):** the `loading`/`error`/`ready` pattern proven on `ledger.component.ts` was then rolled out across every remaining component that fetches on load — 24 more in LandLord, 10 more in BariVara, 34 total. Every one of them had a real API call worth guarding; none were pure-cosmetic skips. Two naming collisions handled by using alternate signal names where a component already had its own `error`/`status` field for form validation (`loadError`, `pageStatus`/`pageError` — see the two components' diffs for which). The two `message-thread.component.ts` files (both apps' Phase 12 polling screens) only wrap the *initial* mount fetch — the 10-second poll interval itself is untouched, so it won't flash "loading" on every tick. Form components used for both create and edit (property-form, unit-form, listing-form, listing-edit) treat "nothing to fetch" (create mode) as immediately `ready`, not stuck loading. Both apps re-verified with `ng build --configuration development` after the rollout — clean.

This closes out the plan's §0 headline gap ("only 1 of 56 LandLord components has loading/error handling") — now essentially all of them do, across both apps.

**Update (same day, third pass): plan complete.** Every remaining item in the doc's §1-§3 is now built, both apps rebuild clean.

§1.3 (kill inline styles) — all 79 literal `style="..."` attributes removed (45 LandLord, 34 BariVara), replaced with utility classes (`.mt-*`, `.max-w-*`, `.flex-between`, `.img-thumb`, `.topbar-plain`, `.sidebar-logout-btn`, a few narrowly-scoped one-offs). Found and fixed a real bug along the way: `style="color:var(--color-danger, #c0392b)"` in 3 places referenced a token, `--color-danger`, that was never defined anywhere — it had always silently fallen back to the hardcoded hex, never actually reading the design system. Now uses the real `.text-danger`/`--danger` token.

§1.4 (accessibility) — badge color-contrast audited by hand (all pairs pass WCAG AA, 6.37:1 to 8.06:1, several pass AAA too — no changes needed). `:focus-visible` ring added to `.btn`/`.module-tile`/`.listing-card`/links/buttons (previously only form inputs had one). Icon-only controls checked — none found beyond the hamburger, which already had `aria-label` (and got `aria-expanded` in the earlier pass).

§1.5 (mid-breakpoint) — 1100px breakpoint added to both apps, `.module-grid`/`.listing-grid` collapse to 2 columns before the 860px mobile-drawer swap kicks in.

§1.6 (dark mode) — built, after confirming with you it should be committed to rather than skipped. `[data-theme="dark"]` token remap in both `styles.css` (same variable names, no component changes needed, badges deliberately kept light in both themes since they're small self-contained chips). Signal-backed `ThemeService` per app (localStorage-persisted) + a floating toggle button mounted in `app.html`.

§2 (LandLord-specific) — `<app-stat-tile>` component built in `shared/`, wired into `dashboard.component.ts`'s 5 stat cards and `ledger.component.ts`'s 3 totals (previously hand-rolled `<h2>` markup per page). Ledger rows now get a left-border income/expense tint in addition to the badge, for at-a-glance scanning. Both `message-thread.component.ts` files (landlord + tenant) got a disabled "Sending…" button state and an error toast on the compose box, without touching the existing 10-second poll loop.

§3 (BariVara-specific) — the 4 "No photo" spots (browse, homepage, favorites, listing-detail) replaced with a house-icon + "No photo yet" label on a brand-tint background, instead of a flat gray gradient. (The underlying cause — `VacancyAdSync` carrying no photo field, so landlord-linked listings show this permanently, not just transiently — is a backend gap, noted but not in this pass's scope.) `browse.component.ts` got active-filter-chips: one chip per set filter (search text/district/area/property type/source), each individually clearable, plus a "Clear all". Dashboard visual parity between BariVara's owner side and LandLord's landlord side was already true structurally (same token system, same `.module-grid`/`.module-tile` pattern) — confirmed, no code change needed.

Still not done: manual browser verification against the live backends (delete-confirm flow, ledger/list error states, booking flow, dark-mode toggle, filter-chips) — plan file has the full checklist and nobody's clicked through it yet.

Plan file: `/home/araf/.claude/plans/shimmering-sprouting-stonebraker.md`. Source doc: `moving-forward-plan/ui-ux_design_plan-v1.md`.

Next candidates: browser-verify all of this against real backends, or move to the cosmetic UI plan (`moving-forward-plan/cosmetic-ui-design_plan-v1.md`) now that stat-tile/loading-state/dark-mode infra exists for it to build on — your call.
