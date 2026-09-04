import { Component, OnInit, inject, signal } from '@angular/core';
import { ApiPayment, BillingApiService } from '../../../core/billing-api.service';
import { TenantApiService } from '../../../core/tenant-api.service';
import { ToastService } from '../../../shared/toast.service';

@Component({
  selector: 'app-tenant-payment-history',
  standalone: true,
  template: `
    <h1>Transaction history</h1>
    <div class="card">
      <div class="table-scroll">
      <table>
        <thead><tr><th>Date</th><th>Amount</th><th>Method</th><th>Status</th><th></th></tr></thead>
        <tbody>
          @for (p of history(); track p.id) {
            <tr>
              <td>{{ p.date }}</td>
              <td>{{ p.amount }}</td>
              <td>{{ p.method }}</td>
              <td>{{ p.status }}</td>
              <td>
                @if (p.status === 'confirmed') {
                  <button class="btn btn-sm" (click)="download(p.id)">Download receipt</button>
                }
              </td>
            </tr>
          } @empty {
            <tr><td colspan="5" class="hint-text">No transactions yet.</td></tr>
          }
        </tbody>
      </table>
      </div>
    </div>
  `,
})
export class TenantPaymentHistoryComponent implements OnInit {
  private readonly tenantApi = inject(TenantApiService);
  private readonly billingApi = inject(BillingApiService);
  private readonly toast = inject(ToastService);

  readonly history = signal<ApiPayment[]>([]);

  async ngOnInit(): Promise<void> {
    const tenant = await this.tenantApi.me();
    const payments = await this.billingApi.paymentsForTenant(tenant.id);
    this.history.set([...payments].sort((a, b) => b.date.localeCompare(a.date) || b.id - a.id));
  }

  async download(paymentId: number): Promise<void> {
    try {
      await this.billingApi.downloadReceipt(paymentId);
    } catch {
      this.toast.error('Could not download the receipt.');
    }
  }
}
