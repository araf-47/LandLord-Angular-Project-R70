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
    this.applyPayment('confirmed');
    this.result.set('Payment successful. Balance updated.');
  }

  payCash(): void {
    if (!this.amount) return;
    this.applyPayment('pending');
    this.result.set('Saved as pending — awaiting landlord confirmation.');
  }

  private applyPayment(status: 'confirmed' | 'pending'): void {
    let remaining = this.amount;
    if (status === 'confirmed') {
      this.data.invoices.update((list) =>
        list.map((i) => {
          if (i.tenantId !== CURRENT_TENANT_ID || i.status === 'paid' || remaining <= 0) return i;
          const applied = Math.min(remaining, i.balance);
          remaining -= applied;
          const newBalance = i.balance - applied;
          return { ...i, balance: newBalance, status: newBalance === 0 ? 'paid' : 'partial' };
        })
      );
    }

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
