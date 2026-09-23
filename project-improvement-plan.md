# Project Improvement Plan

Separate from `project-plan.md` (the primary plan). This file tracks
brainstormed improvement ideas for LandLord and their progress, kept apart so
the main plan stays focused on shipped phases.

## Status: 2026-09-14

Brainstormed 8 new feature/design ideas for LandLord (analytics, unit-grid
view, multi-landlord support, rent-health badge, SMS/WhatsApp reminders,
command palette, ledger export, bento dashboard). User picked 3 to build:

1. **Unit-grid "floor plan" view** — ✅ done. View-toggle (grid/table) added
   to `unit-list.component.ts`, defaults to grid. New `unit-card.component.ts`.
   Status color-coded (vacant/occupied/overdue), overdue derived client-side
   via the existing `GET /api/tenants/{id}/outstanding-balance` endpoint — no
   new backend code.
2. **Analytics & Reports module** — ✅ done. New `reports.service.ts`
   (client-side aggregation off existing invoice/payment/expense endpoints,
   no new backend surface) + hand-rolled inline-SVG `bar-chart.component.ts`
   (no charting library added) + `reports.component.ts`, routed at
   `landlord/reports`, new dashboard module tile. Shows monthly income/
   expense/net, collection rate, and a *current* occupancy-by-property
   snapshot (deliberately not a fabricated occupancy trend — no history
   table exists for that).
3. **Bento-grid dashboard redesign** — ✅ done, revised. Bento asymmetric
   layout (one dominant 2x2 tile) applied to the 5 KPI stat tiles only (Net
   this month is the dominant cell). **User feedback (2026-09-14): the
   asymmetric bento layout did not work for the "Manage your property"
   module-nav grid** — reverted that section back to the original uniform
   `.module-grid`. Bento styling now scoped to the stat-tile row only.

All three verified via `ng build` clean; live browser click-through is on the
user (Claude-in-Chrome drives the user's real browser, which isn't reachable
from the sandbox these dev servers run in).

## Status: 2026-09-15

4. **Property-list inline accordion** — ✅ done. Clicking a property row in
   `property-list.component.ts` expands its units inline as an
   `app-unit-card` grid (single-open accordion — expanding one auto-collapses
   any other, re-clicking collapses it), replacing the old separate
   "Manage units" page. `unit-list.component.ts` and its `:propertyId/units`
   route deleted; `unit-form.component.ts` now redirects back to the property
   list on save (was navigating to the now-removed unit-list route). Tenant
   name/overdue-balance derivation moved into `property-list.component.ts`,
   computed portfolio-wide up front (page already eager-loads all properties/
   units). Verified via `ng build` clean; live browser click-through on the
   user.

5. **Landlord Settings tab** — ✅ done. Outside the original 8 ideas above;
   user asked directly for an account-settings surface. New
   `features/landlord/settings/settings.component.ts` (single sectioned
   page), routed at `landlord/settings`, new sidebar nav entry + gear icon.
   Sections:
   - **Profile & Security** — edit email/phone, change password (OTP-gated),
     2FA toggle, "log out of all devices". All wired to `Parts/auth`
     `UserController` endpoints (`update`, `change-password`, `toggle-2fa`,
     `logout-all`) that existed on the backend but had no frontend caller
     before this.
   - **Appearance** — theme toggle relocated alongside the existing floating
     button (kept both; not confirmed with user whether to remove the
     floating one).
   - **Notification preferences** — new. Added 4 boolean columns to
     `com.idb.auth.model.User` (rent-due email/SMS, payment-received email,
     maintenance email) + new `/api/v3/user/notification-prefs` endpoint.
     `/api/auth/me` extended to return `id`, `email`, `phone`,
     `twoFactorEnabled`, and the 4 notification flags so the page can
     prefill.
   - Payment/billing settings excluded — Phase 10.8 gateway is on hold.

   **Bug caught during verification**: the already-running `landlord-backend`
   JVM was serving stale code (started before these edits), so the new
   columns/endpoint weren't live until restarted. On restart, Hibernate's
   `ddl-auto=update` failed to add the 4 new `NOT NULL` boolean columns
   against the existing non-empty `users` table (`contains null values`) —
   fixed by adding `@ColumnDefault(...)` to each field so Postgres can
   backfill existing rows. Verified end-to-end via curl against the real
   backend (login → `/api/auth/me` → update notification prefs → confirmed
   persisted) after the fix; `ng build`/`tsc --noEmit`/`mvn compile` all
   clean. Password-change and 2FA-toggle flows verified via curl but not yet
   click-tested in the browser UI itself.

6. **Tenant list row-click UX** — ✅ done. Outside the original 8 ideas;
   user asked for tenant row click to behave like the "View" button. Whole
   `<tr>` in `tenant-list.component.ts` now navigates to
   `/landlord/tenants/:id` on click or Enter (`role="button"`, `tabindex="0"`,
   mirrors the row-click pattern from idea #4's property-list accordion, but
   navigates instead of expanding since tenants use a separate detail page).
   Actions cell (`View`/`Move out`) stops click propagation so those links
   keep working independently. New `.tenant-row` hover style in
   `styles.css`, same treatment as `.property-row`. Verified via
   `tsc --noEmit` clean; live browser click-through on the user.

7. **Move "Move out" button to tenant detail page** — ✅ done. Removed
   per-row "Move out" link from `tenant-list.component.ts` actions cell
   (list now shows only "View", relying on idea #6's row-click). Added a
   "Move out" button (`btn btn-danger`, active-status only) to
   `tenant-detail.component.ts`'s header info card, next to "Message
   tenant". New `moveOut()` method navigates to the existing
   `:tenantId/move-out` route/component (`tenant-moveout.component.ts`,
   unchanged) — only the entry point moved, not the flow itself. Verified
   via `tsc --noEmit` clean; live browser click-through on the user.

## Status: 2026-09-15 (cont'd)

8. **Tenant search null-phone crash fix** — ✅ done. Outside the original 8;
   user reported Tenant Management search "doing nothing" on keystroke.
   Root cause: `tenant-list.component.ts` filter called
   `t.phone.toLowerCase()` unguarded, but backend `Tenant.phone` has no
   `@NotBlank` (nullable) — any tenant with a null phone threw once a
   non-empty query was typed (empty query short-circuited past it),
   crashing the whole filtered list. Fixed with `(t.phone ?? '')`.

9. **Receive payment: searchable tenant combobox** — ✅ done. User asked for
   name search on the tenant picker in Payments → Receive payment
   (`receive-payment.component.ts`), then asked for it "integrated" rather
   than a separate search box + native `<select>`. Replaced with a
   hand-rolled combobox: single text input filters `tenants()` by name
   (case-insensitive `includes`), matches render as a clickable `<ul>`
   dropdown positioned under the input (new `.combobox`/`.combobox-list`
   CSS in `styles.css`), with a decorative caret icon inside the input
   (`.combobox-caret`). Selecting an item sets `tenantId` + loads that
   tenant's unpaid invoices, same as the old `(change)` handler did.
   Dropdown list is scoped to `status === 'active'` tenants only (you can't
   receive payment against a moved-out tenant). Verified via `ng build`
   clean.

10. **Active-tenant filter on remaining tenant dropdowns** — ✅ done.
    Follow-up to #9: user asked to apply the same active-only filter to
    the two other plain-`<select>` tenant pickers —
    `maintenance/ticket-new.component.ts` ("Log new issue" → Tenant &
    unit) and `expenses/expense-management.component.ts` ("Log expense" →
    Tenant, shown when bearer is tenant). Both got a new `activeTenants`
    computed (`filter((t) => t.status === 'active')` over
    `tenantApi.tenants()`), same shape as receive-payment's filter, wired
    into their `@for` loops. No combobox/search UI added here — just the
    status filter; kept scope to what was asked. Verified via `ng build`
    clean.

## Status: 2026-09-21

11. **Exportable reports (PDF/Excel) + free-tier AI portfolio insights** — ✅
    done. Outside the original 8 ideas; user asked directly for "Jasper-style"
    reports and a free AI feature. Two separate additions:
    - **Reports & Exports** — new `landlord-backend/.../report/` module
      (`ReportController`, `ReportService`, shared `ReportTable` model +
      generic `ReportPdfService`/`ReportExcelService` renderer). Four fixed
      reports — Income Statement, Expense Report, Occupancy Report, Tenant
      Ledger — each servable as JSON/PDF/XLSX with date-range/property/
      tenant/category filters. PDF reuses the OpenPDF pattern from Phase
      10.5's `ReceiptService`; Excel is new (`Apache POI` added to
      `pom.xml`). Frontend: `reports.component.ts` extended with a filter
      bar + download buttons per report, new `report-api.service.ts`.
      Aggregation is in-memory over existing repositories (no new `@Query`
      SUM/COUNT) — fine at current portfolio scale, revisit if that grows.
    - **AI Insights (chat)** — new `landlord-backend/.../insights/` module.
      `GeminiClient` calls Google's free-tier Gemini API (`gemini-3.6-flash`
      — `gemini-2.0-flash`, the original pick, was retired by Google mid-build
      and swapped out), config via `GEMINI_API_KEY` env var following the
      Brevo integration's pattern (never committed to a file).
      `InsightsContextBuilder` is the scoping piece: builds a small,
      pre-aggregated text summary (occupancy, this month's income/expenses,
      top-20 overdue tenants by balance, maintenance ticket counts) and only
      that bounded summary reaches the LLM — never raw entity dumps. New
      "AI Insights" sidebar page, chat-style Q&A. Both features verified
      end-to-end against the live backend and real seed data (all 12
      report endpoints, plus a live Gemini round-trip); `mvn compile` and
      `ng build --configuration production` both clean.

## Status: 2026-09-23

12. **Combined "Full report" export (PDF/Excel)** — ✅ done. Follow-up to
    #11: user wanted one combined document instead of downloading the 4
    fixed reports separately. Scope: bundles Income Statement + Expense
    Report + Occupancy Report only (Tenant Ledger excluded — it's
    per-tenant, doesn't fit a portfolio-wide doc); PDF + Excel only, no
    JSON endpoint.
    - Backend: `ReportService.fullReport()` composes the 3 existing report
      builds into a new `FullReport` record (no new aggregation logic —
      pure composition). `ReportTables.of(FullReport)` converts it to
      `List<ReportTable>`. `ReportPdfService`/`ReportExcelService` each
      got a `List<ReportTable>` overload (single-table `render()` kept
      unchanged — its body was extracted into a private
      `addTableSection`/`writeSheet` helper reused by both overloads). PDF:
      one document, one shared logo header/page-footer (`ReportPageEvent`
      was already document-scoped), page break between each of the 3
      sections. Excel: one workbook, 3 sheets (named per `table.title()`,
      same as today just looped). New endpoints
      `GET /api/reports/full-report.pdf` / `.xlsx` on `ReportController`,
      `category` filter intentionally left out (not meaningful across a
      combined doc).
    - Frontend: 5th tab "Full report" on `reports.component.ts` — date
      range + property filter, PDF/Excel download buttons only (no
      "Run"/results-table, per the JSON-less scope). New
      `downloadFullReportPdf/Excel()` on `report-api.service.ts` mirroring
      the existing `download()` helper pattern.
    - Gotcha hit during verification: the running `landlord-backend`
      Spring Boot process (started via `dev-up.sh`, no devtools hot-reload)
      kept serving the pre-change compiled classes, so the new endpoints
      404'd under the hood — masked by the auth filter returning 401 for
      *any* path under `/api/reports/**` regardless of whether the route
      exists, so `curl` couldn't distinguish "not authed" from "route
      doesn't exist." Fixed by killing the port-8080 process and
      relaunching `spring-boot:run` to pick up the rebuild. **Takeaway:**
      after backend code changes, the dev server needs a restart to take
      effect — it does not hot-reload.
    - `mvn compile` and `ng build --configuration production` both clean.

## Not yet started (from the original 8, deprioritized for now)

- Multi-landlord/portfolio support (`LandlordUser` entity) — bigger
  structural change, not picked this round.
- Rent-health badge for tenants (gamified on-time-payment indicator).
- SMS/WhatsApp reminders (extend Phase 7.5's Brevo-email channel).
- Command palette (Ctrl+K global search/nav).
