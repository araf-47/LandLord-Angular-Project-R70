# "Receive payment" shows fake success when tenant has no unpaid invoice

**Found:** 2026-08-30, while testing the tenant "Download receipt" toast for the UI/UX plan v1 verification pass.

**App:** LandLord frontend, `receive-payment.component.ts` (`/landlord/payments/receive`).

## Repro

1. Log in as landlord, register a brand-new walk-in tenant (no monthly bill has run for them yet, so they have zero invoices).
2. Go to Payments → Receive Payment, select that tenant.
3. "Total due: 0" shows (correct — no invoices exist).
4. Enter a payment method + amount (e.g. 5000), click "Save payment, generate receipt".
5. UI shows: **"Payment saved. Marked fully paid."**
6. Checked the database directly: **no row was inserted into `payment`** for this tenant. `max(id)` in `payment` unchanged before/after. Nothing was saved.

## Root cause

`receive-payment.component.ts`, `save()` (around line 113-136):

```ts
async save(): Promise<void> {
  ...
  let remaining = this.amount;
  const oldest = [...this.unpaidInvoices()].sort((a, b) => a.id - b.id);
  for (const invoice of oldest) {
    if (remaining <= 0) break;
    const applied = Math.min(remaining, invoice.balance);
    await this.billingApi.recordPayment(this.tenantId, invoice.id, applied, this.method);
    remaining -= applied;
  }
  ...
  this.saved.set(true);   // <-- set unconditionally, even if `oldest` was empty and the loop body never ran
}
```

If `this.unpaidInvoices()` is empty (tenant has no unpaid invoice — e.g. a brand-new tenant before their first monthly bill run), the `for` loop body never executes, so `recordPayment` is never called and nothing is persisted. But `this.saved.set(true)` runs regardless, so the template unconditionally renders "Payment saved. Marked fully paid." — a false success message for a no-op.

## Impact

- A landlord can believe they recorded a cash/bank/mobile payment from a tenant when nothing was actually saved — no audit trail, no ledger entry, money effectively "lost" from the system's point of view even though the landlord did receive it in person.
- Most likely to bite exactly the walk-in-registration → immediate-payment flow (register a new tenant, immediately try to record their first payment before any bill has been generated for them).

## Suggested fix

Guard the success path: only set `saved.set(true)` (and only report "fully/partially paid") when at least one `recordPayment` call actually happened — e.g. track whether `oldest.length > 0` / whether any payment was applied, and otherwise show an explicit error like "This tenant has no outstanding invoice to apply a payment to — generate a bill first."
