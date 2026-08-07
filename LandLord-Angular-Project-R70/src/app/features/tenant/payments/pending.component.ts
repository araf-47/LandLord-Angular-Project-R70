import { Component, inject } from '@angular/core';
import { CURRENT_TENANT_ID, MockDataService } from '../../../core/mock-data.service';

@Component({
  selector: 'app-tenant-payment-pending',
  standalone: true,
  template: `
    <h1>Pending cash payments</h1>
    <div class="card">
      <div class="table-scroll">
      <table>
        <thead><tr><th>Date</th><th>Amount</th><th>Status</th></tr></thead>
        <tbody>
          @for (p of pending(); track p.id) {
            <tr>
              <td>{{ p.date }}</td>
              <td>{{ p.amount }}</td>
              <td><span class="badge badge-pending">{{ p.status }}</span></td>
            </tr>
          } @empty {
            <tr><td colspan="3" class="hint-text">No pending cash payments.</td></tr>
          }
        </tbody>
      </table>
      </div>
    </div>
  `,
})
export class TenantPaymentPendingComponent {
  private readonly data = inject(MockDataService);

  pending() {
    return this.data.payments().filter((p) => p.tenantId === CURRENT_TENANT_ID && p.method === 'cash');
  }
}
