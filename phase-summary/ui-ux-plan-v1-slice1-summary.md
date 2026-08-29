UI/UX Design Plan v1 — first slice done. Summary:

Built shared infra in both apps (LandLord + BariVara, each independently — separate Angular workspaces, no shared code path): signal-backed `ToastService`/`<app-toast-host>` and promise-based `ConfirmService`/`<app-confirm-dialog>` (focus-trapped, Escape-to-cancel), both mounted once in `app.html` above the router outlet so every layout gets them for free. Added `--space-xs/sm/md/lg` tokens plus `.text-success`/`.text-danger`/`.mb-*`/`.gap-*` utility classes to both `styles.css` files.

Wired up a representative slice to prove the pattern end-to-end rather than rolling it across all ~55 components at once:

LandLord — `ledger.component.ts` now has real loading/error/ready states (was a bare `Promise.all` with no error handling, silent failure on network hiccup); `properties/unit-list.component.ts` and `properties/property-list.component.ts` delete actions go through the new confirm dialog instead of native `confirm()`; `tenant/payments/history.component.ts` uses a toast instead of `alert()` for the receipt-generated message (kept the existing stub behavior — no real PDF download, that's a backend gap, not this pass's scope).

BariVara — `owner/properties/unit-list.component.ts` delete goes through the confirm dialog; `owner/listings/listing-list.component.ts` delete previously had **no confirmation at all** (a gap found during research, not previously known) — now guarded the same way; `public/listing-detail.component.ts`'s `book()` flow got a loading/disabled state on the button plus success/error toasts (previously no feedback at all while the request was in flight or if it failed).

Both apps verified with `ng build --configuration development` — clean, no compile/template errors.

**Update (same day, second pass):** the `loading`/`error`/`ready` pattern proven on `ledger.component.ts` was then rolled out across every remaining component that fetches on load — 24 more in LandLord, 10 more in BariVara, 34 total. Every one of them had a real API call worth guarding; none were pure-cosmetic skips. Two naming collisions handled by using alternate signal names where a component already had its own `error`/`status` field for form validation (`loadError`, `pageStatus`/`pageError` — see the two components' diffs for which). The two `message-thread.component.ts` files (both apps' Phase 12 polling screens) only wrap the *initial* mount fetch — the 10-second poll interval itself is untouched, so it won't flash "loading" on every tick. Form components used for both create and edit (property-form, unit-form, listing-form, listing-edit) treat "nothing to fetch" (create mode) as immediately `ready`, not stuck loading. Both apps re-verified with `ng build --configuration development` after the rollout — clean.

This closes out the plan's §0 headline gap ("only 1 of 56 LandLord components has loading/error handling") — now essentially all of them do, across both apps.

Deliberately deferred (per the plan's own sequencing, to keep this reviewable): §1.3 inline-style cleanup beyond `ledger.component.ts` (48+ instances remain), §1.5 mid-breakpoint (1024px), §1.6 dark mode, §2/§3's feature-specific items (stat-tile component, active-filter-chips, BariVara's permanent "no photo" placeholder, dashboard visual parity). Also not yet done: manual browser verification against the live backends (delete-confirm flow, ledger/list error states, booking flow) — plan file has the full checklist.

Plan file: `/home/araf/.claude/plans/shimmering-sprouting-stonebraker.md`. Source doc: `moving-forward-plan/ui-ux_design_plan-v1.md`.

Next candidates: browser-verify all this against real backends, then move to the remaining hygiene/polish items above, or the cosmetic UI plan (`moving-forward-plan/cosmetic-ui-design_plan-v1.md`) — your call.
