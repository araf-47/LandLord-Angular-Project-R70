import { Component, inject } from '@angular/core';
import { CURRENT_TENANT_ID, MockDataService } from '../../../core/mock-data.service';

@Component({
  selector: 'app-tenant-payment-history',
  standalone: true,
  template: `
    <h1>Transaction history</h1>
    <div class="card">
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
  `,
})
export class TenantPaymentHistoryComponent {
  private readonly data = inject(MockDataService);

  history() {
    return this.data.payments().filter((p) => p.tenantId === CURRENT_TENANT_ID);
  }

  download(paymentId: string): void {
    // PDF generation is a backend concern; frontend stub only.
    alert(`Receipt PDF generated for payment ${paymentId}`);
  }
}
