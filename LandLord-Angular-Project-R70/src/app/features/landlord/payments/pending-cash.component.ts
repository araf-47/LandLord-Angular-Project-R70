import { Component, inject } from '@angular/core';
import { MockDataService } from '../../../core/mock-data.service';

@Component({
  selector: 'app-pending-cash',
  standalone: true,
  template: `
    <h1>Pending cash payments</h1>
    <div class="card">
      <table>
        <thead><tr><th>Tenant</th><th>Amount</th><th>Date</th><th></th></tr></thead>
        <tbody>
          @for (p of pending(); track p.id) {
            <tr>
              <td>{{ tenantName(p.tenantId) }}</td>
              <td>{{ p.amount }}</td>
              <td>{{ p.date }}</td>
              <td>
                <button class="btn btn-sm btn-primary" (click)="confirm(p.id, true)">Confirm received</button>
                <button class="btn btn-sm btn-danger" (click)="confirm(p.id, false)">Reject / flag</button>
              </td>
            </tr>
          } @empty {
            <tr><td colspan="4" class="hint-text">No pending cash payments.</td></tr>
          }
        </tbody>
      </table>
    </div>
  `,
})
export class PendingCashComponent {
  protected readonly data = inject(MockDataService);

  pending() {
    return this.data.payments().filter((p) => p.status === 'pending');
  }

  tenantName(tenantId: string): string {
    return this.data.tenants().find((t) => t.id === tenantId)?.name ?? '—';
  }

  confirm(paymentId: string, received: boolean): void {
    const payment = this.data.payments().find((p) => p.id === paymentId);

    this.data.payments.update((list) =>
      list.map((p) => (p.id === paymentId ? { ...p, status: received ? 'confirmed' : 'rejected' } : p))
    );

    // The invoice only clears once the landlord confirms the cash actually arrived.
    if (received && payment) {
      this.data.applyPaymentToTenant(payment.tenantId, payment.amount);
    }
  }
}
