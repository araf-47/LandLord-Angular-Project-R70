import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApiInvoice, ApiPayment, BillingApiService } from '../../../core/billing-api.service';
import { ApiTenant, TenantApiService } from '../../../core/tenant-api.service';

@Component({
  selector: 'app-receive-payment',
  standalone: true,
  imports: [FormsModule],
  template: `
    <h1>Receive payment</h1>

    @switch (status()) {
      @case ('loading') {
        <div class="card"><p class="hint-text">Loading tenants…</p></div>
      }
      @case ('error') {
        <div class="card">
          <p class="text-danger mb-sm">Couldn't load this page. {{ loadError() }}</p>
          <button type="button" class="btn btn-sm" (click)="ngOnInit()">Retry</button>
        </div>
      }
      @case ('ready') {
        <div class="card stack" style="max-width:520px;">
          <div class="field">
            <label for="tenant">Select tenant</label>
            <select id="tenant" name="tenant" (change)="onTenantChange($event)">
              <option value="">— choose —</option>
              @for (t of tenants(); track t.id) {
                <option [value]="t.id" [selected]="t.id === tenantId">{{ t.name }}</option>
              }
            </select>
          </div>

          @if (tenantId) {
            <p><strong>Total due:</strong> {{ totalDue() }}</p>

            <div class="field">
              <label for="method">Payment method</label>
              <select id="method" name="method" [(ngModel)]="method">
                <option value="cash">Cash</option>
                <option value="bank">Bank</option>
                <option value="mobile">Mobile</option>
              </select>
            </div>

            <div class="field">
              <label for="amount">Amount paid</label>
              <input id="amount" type="number" name="amount" [(ngModel)]="amount" />
            </div>

            <div class="actions-row">
              <button class="btn btn-primary" (click)="save()">Save payment, generate receipt</button>
            </div>

            @if (saved()) {
              <p class="hint-text">Payment saved. {{ amount >= totalDue() ? 'Marked fully paid.' : 'Marked partially paid — remaining ' + (totalDue() - amount) + '.' }}</p>
            }
            @if (error()) {
              <p class="error-text">{{ error() }}</p>
            }
          }
        </div>
      }
    }
  `,
})
export class ReceivePaymentComponent implements OnInit {
  private readonly tenantApi = inject(TenantApiService);
  private readonly billingApi = inject(BillingApiService);

  readonly tenants = signal<ApiTenant[]>([]);
  readonly unpaidInvoices = signal<ApiInvoice[]>([]);

  tenantId: number | '' = '';
  method: ApiPayment['method'] = 'cash';
  amount = 0;
  readonly saved = signal(false);
  readonly error = signal('');

  readonly status = signal<'loading' | 'error' | 'ready'>('loading');
  readonly loadError = signal<string | undefined>(undefined);

  async ngOnInit(): Promise<void> {
    this.status.set('loading');
    try {
      await this.tenantApi.load();
      this.tenants.set(this.tenantApi.tenants());
      this.status.set('ready');
    } catch {
      this.loadError.set('Check your connection and try again.');
      this.status.set('error');
    }
  }

  async onTenantChange(event: Event): Promise<void> {
    const value = (event.target as HTMLSelectElement).value;
    this.tenantId = value ? Number(value) : '';

    this.saved.set(false);
    if (!this.tenantId) {
      this.unpaidInvoices.set([]);
      return;
    }
    const invoices = await this.billingApi.invoicesForTenant(this.tenantId);
    this.unpaidInvoices.set(invoices.filter((i) => i.status !== 'paid'));
  }

  totalDue(): number {
    return this.unpaidInvoices().reduce((sum, i) => sum + i.balance, 0);
  }

  async save(): Promise<void> {
    if (!this.tenantId) {
      this.error.set('Choose a tenant first.');
      return;
    }
    if (!this.amount) {
      this.error.set('Enter an amount.');
      return;
    }

    this.error.set('');
    let remaining = this.amount;
    const oldest = [...this.unpaidInvoices()].sort((a, b) => a.id - b.id);
    for (const invoice of oldest) {
      if (remaining <= 0) break;
      const applied = Math.min(remaining, invoice.balance);
      await this.billingApi.recordPayment(this.tenantId, invoice.id, applied, this.method);
      remaining -= applied;
    }

    const invoices = await this.billingApi.invoicesForTenant(this.tenantId);
    this.unpaidInvoices.set(invoices.filter((i) => i.status !== 'paid'));
    this.saved.set(true);
  }
}
