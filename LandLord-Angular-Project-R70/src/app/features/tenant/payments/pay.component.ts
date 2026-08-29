import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApiInvoice, BillingApiService } from '../../../core/billing-api.service';
import { TenantApiService } from '../../../core/tenant-api.service';

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
          <div class="hint-text" style="display:flex; justify-content:space-between;">
            <span>Utilities</span><span>{{ inv.utilitiesTotal }}</span>
          </div>
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
export class TenantPayComponent implements OnInit {
  private readonly tenantApi = inject(TenantApiService);
  private readonly billingApi = inject(BillingApiService);

  private tenantId: number | null = null;
  readonly invoices = signal<ApiInvoice[]>([]);

  amount = 0;
  method: 'online' | 'cash' = 'online';
  date = new Date().toISOString().slice(0, 10);
  readonly result = signal('');

  readonly nextInvoice = computed(() =>
    this.invoices()
      .filter((i) => i.status !== 'paid')
      .sort((a, b) => a.period.localeCompare(b.period))[0]
  );

  async ngOnInit(): Promise<void> {
    const tenant = await this.tenantApi.me();
    this.tenantId = tenant.id;
    this.invoices.set(await this.billingApi.invoicesForTenant(tenant.id));
  }

  totalDue(): number {
    return this.invoices()
      .filter((i) => i.status !== 'paid')
      .reduce((sum, i) => sum + i.balance, 0);
  }

  async payOnline(): Promise<void> {
    // Real payment gateway integration is on hold (Phase 10.8) — records
    // straight to confirmed, same as cash, just tagged 'online'.
    await this.pay('online');
    this.result.set('Payment successful. Balance updated.');
  }

  async payCash(): Promise<void> {
    await this.pay('cash');
    this.result.set('Payment recorded. Balance updated.');
  }

  private async pay(method: 'online' | 'cash'): Promise<void> {
    const invoice = this.nextInvoice();
    if (!this.amount || !invoice || this.tenantId === null) return;
    await this.billingApi.recordPayment(this.tenantId, invoice.id, this.amount, method);
    this.invoices.set(await this.billingApi.invoicesForTenant(this.tenantId));
  }
}
