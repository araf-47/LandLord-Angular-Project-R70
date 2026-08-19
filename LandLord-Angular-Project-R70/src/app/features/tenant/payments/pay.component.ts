import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CURRENT_TENANT_ID, MockDataService, nextId } from '../../../core/mock-data.service';

@Component({
  selector: 'app-tenant-pay',
  standalone: true,
  imports: [FormsModule],
  template: `
    <h1>Payments</h1>
    <div class="card" style="max-width:480px;">
      <p><strong>Current due (invoice):</strong> {{ totalDue() }}</p>

      @if (nextInvoice(); as inv) {
        <div class="stack" style="margin-bottom:1rem;">
          <div class="hint-text" style="display:flex; justify-content:space-between;">
            <span>Rent</span><span>{{ inv.rent }}</span>
          </div>
          @for (u of inv.utilityItems; track u.label) {
            <div class="hint-text" style="display:flex; justify-content:space-between;">
              <span>{{ u.label }}</span><span>{{ u.amount }}</span>
            </div>
          }
          @if (inv.prevUnpaidRolled) {
            <div class="hint-text" style="display:flex; justify-content:space-between;">
              <span>Previous unpaid balance</span><span>{{ inv.prevUnpaidRolled }}</span>
            </div>
          }
        </div>
      }

      <div class="field">
        <label for="amount">Amount to pay</label>
        <input id="amount" type="number" name="amount" [(ngModel)]="amount" />
      </div>

      <div class="field">
        <label for="method">Payment method</label>
        <select id="method" name="method" [(ngModel)]="method">
          <option value="online">Online</option>
          <option value="cash">Cash (offline)</option>
        </select>
      </div>

      @if (method === 'cash') {
        <div class="field">
          <label for="date">Payment date</label>
          <input id="date" type="date" name="date" [(ngModel)]="date" />
        </div>
        <button class="btn btn-primary" (click)="payCash()">Record cash payment</button>
      } @else {
        <button class="btn btn-primary" (click)="payOnline()">Open payment gateway</button>
      }

      @if (result()) {
        <p [class.error-text]="result()!.startsWith('Error')" [class.hint-text]="!result()!.startsWith('Error')" style="margin-top:0.75rem;">
          {{ result() }}
        </p>
      }
    </div>
  `,
})
export class TenantPayComponent {
  private readonly data = inject(MockDataService);

  amount = 0;
  method: 'online' | 'cash' = 'online';
  date = new Date().toISOString().slice(0, 10);
  readonly result = signal('');

  readonly nextInvoice = computed(() =>
    this.data
      .invoicesForTenant(CURRENT_TENANT_ID)
      .filter((i) => i.status !== 'paid')
      .sort((a, b) => a.period.localeCompare(b.period))[0]
  );

  totalDue(): number {
    return this.data
      .invoices()
      .filter((i) => i.tenantId === CURRENT_TENANT_ID && i.status !== 'paid')
      .reduce((sum, i) => sum + i.balance, 0);
  }

  payOnline(): void {
    if (!this.amount) return;
    // Simulated gateway — always succeeds in this frontend-only build.
    this.data.applyPaymentToTenant(CURRENT_TENANT_ID, this.amount);
    this.recordPayment('confirmed');
    this.result.set('Payment successful. Balance updated.');
  }

  payCash(): void {
    if (!this.amount) return;
    // Matches the real backend: payments record straight to confirmed, no pending/confirm step.
    this.data.applyPaymentToTenant(CURRENT_TENANT_ID, this.amount);
    this.recordPayment('confirmed');
    this.result.set('Payment recorded. Balance updated.');
  }

  private recordPayment(status: 'confirmed'): void {
    this.data.payments.update((list) => [
      ...list,
      {
        id: nextId('pay'),
        tenantId: CURRENT_TENANT_ID,
        invoiceId: '',
        amount: this.amount,
        method: this.method,
        status,
        date: this.date,
      },
    ]);
  }
}
