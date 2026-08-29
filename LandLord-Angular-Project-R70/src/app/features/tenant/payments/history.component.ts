import { Component, OnInit, inject, signal } from '@angular/core';
import { ApiPayment, BillingApiService } from '../../../core/billing-api.service';
import { TenantApiService } from '../../../core/tenant-api.service';

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

  readonly history = signal<ApiPayment[]>([]);

  async ngOnInit(): Promise<void> {
    const tenant = await this.tenantApi.me();
    this.history.set(await this.billingApi.paymentsForTenant(tenant.id));
  }

  download(paymentId: number): void {
    // PDF generation is a backend concern; frontend stub only.
    alert(`Receipt PDF generated for payment ${paymentId}`);
  }
}
