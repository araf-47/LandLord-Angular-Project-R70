import { Component, inject } from '@angular/core';
import { MockDataService, nextId } from '../../../core/mock-data.service';

@Component({
  selector: 'app-generate-bills',
  standalone: true,
  template: `
    <h1>Generate Bills</h1>
    <div class="card">
      <p>Automated process: retrieve active tenants, calculate base rent + utilities, add previous unpaid balance, save new invoices as unpaid.</p>
      <button class="btn btn-primary" (click)="generate()">Generate bills</button>
    </div>

    <div class="card">
      <table>
        <thead><tr><th>Tenant</th><th>Amount</th><th>Due date</th><th>Status</th></tr></thead>
        <tbody>
          @for (i of data.invoices(); track i.id) {
            <tr>
              <td>{{ tenantName(i.tenantId) }}</td>
              <td>{{ i.amount }}</td>
              <td>{{ i.dueDate }}</td>
              <td><span class="badge" [class.badge-unpaid]="i.status !== 'paid'" [class.badge-paid]="i.status === 'paid'">{{ i.status }}</span></td>
            </tr>
          }
        </tbody>
      </table>
    </div>
  `,
})
export class GenerateBillsComponent {
  protected readonly data = inject(MockDataService);

  tenantName(tenantId: string): string {
    return this.data.tenants().find((t) => t.id === tenantId)?.name ?? '—';
  }

  generate(): void {
    const activeTenants = this.data.tenants().filter((t) => t.status === 'active' && t.unitId);
    const dueDate = new Date();
    dueDate.setMonth(dueDate.getMonth() + 1);

    const newInvoices = activeTenants.map((t) => {
      const rent = this.data.units().find((u) => u.id === t.unitId)?.rent ?? 0;
      const prevUnpaid = this.data
        .invoices()
        .filter((i) => i.tenantId === t.id && i.status !== 'paid')
        .reduce((sum, i) => sum + i.balance, 0);
      const amount = rent + prevUnpaid;
      return {
        id: nextId('inv'),
        tenantId: t.id,
        amount,
        balance: amount,
        status: 'unpaid' as const,
        dueDate: dueDate.toISOString().slice(0, 10),
      };
    });

    this.data.invoices.update((list) => [...list, ...newInvoices]);
  }
}
