import { Component, inject, signal } from '@angular/core';
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
        <button class="btn btn-primary" (click)="payCash()">Submit — awaiting landlord confirmation</button>
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
    // Balance only clears once the landlord confirms the cash arrived — see pending-cash.component.ts.
    this.recordPayment('pending');
    this.result.set('Saved as pending — awaiting landlord confirmation.');
  }

  private recordPayment(status: 'confirmed' | 'pending'): void {
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
