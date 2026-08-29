# ConfirmDialogComponent: no actual focus trap (both apps)

**Found:** 2026-08-30, keyboard-only accessibility pass on the UI/UX plan v1 confirm dialog.

**Apps:** both — identical component in each:
- `LandLord-Angular-Project-R70/LandLord-Angular-Project-R70/src/app/shared/confirm-dialog.component.ts`
- `LandLord-Angular-Project-R70/BariVara-Angular-Project-R70/src/app/shared/confirm-dialog.component.ts`

## What the plan called for

`moving-forward-plan` / the implementation plan explicitly specced `<app-confirm-dialog>` as "focus-trapped, Escape-to-cancel."

## What's actually implemented

- **Escape-to-cancel: works.** `@HostListener('document:keydown.escape')` calls `respond(false)`.
- **Initial autofocus: works.** `effect(() => this.confirmBtn()?.nativeElement.focus())` puts focus on the Confirm button when the dialog opens.
- **Focus trap: not implemented at all.** There is no `keydown.tab` handler, no `focus`/`focusin` listener re-steering focus, nothing constraining Tab to the two buttons inside `.confirm-dialog`.

## Repro

1. Log in as landlord (`localhost:4200`, `landlord` / `Landlord@12345`), go to Property & Units, click any row's **Delete**.
2. Dialog opens, `document.activeElement` is the **Confirm** button (correct).
3. Press **Tab** once.
4. `document.activeElement` becomes the floating dark-mode toggle button (`<button class="theme-toggle">`) sitting behind the dialog in the page's bottom-left corner — i.e. focus left the dialog entirely into the backdrop page content.

Verified via:
```js
document.activeElement.outerHTML
// after opening dialog: <button ... class="btn btn-danger">Confirm</button>
// after one Tab press:   <button ... class="theme-toggle" aria-label="Switch to light mode"> ☀️ </button>
```

Same result confirmed on the BariVara owner dashboard's unit-delete dialog (identical component code).

## Impact

- Keyboard/screen-reader users can Tab straight out of an open, modal (`aria-modal="true"`) confirm dialog into unrelated background page controls (here, the theme toggle) while the backdrop is still up and the dialog still open — the page behind the "modal" remains fully keyboard-reachable.
- Violates the `aria-modal="true"` contract already declared on the dialog's own markup (line 13 in both files) — screen readers are told this is a modal, but focus is not actually constrained to it.

## Suggested fix

Add a `keydown.tab` handler (or a small manual trap) in `ConfirmDialogComponent` that keeps Tab/Shift+Tab cycling between the two buttons (`.btn-ghost` and `#confirmBtn`) only, e.g. querying the two focusable elements inside `.confirm-dialog` and wrapping focus at each end. Apply the same fix to both apps' copies since the component is duplicated, not shared.
