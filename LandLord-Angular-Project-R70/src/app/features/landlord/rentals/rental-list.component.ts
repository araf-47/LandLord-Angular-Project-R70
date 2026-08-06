import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MockDataService } from '../../../core/mock-data.service';

@Component({
  selector: 'app-rental-list',
  standalone: true,
  imports: [RouterLink],
  template: `
    <h1>Rental Agreements</h1>
    <div class="card">
      <table>
        <thead><tr><th>Tenant</th><th>Unit</th><th>Start date</th><th></th></tr></thead>
        <tbody>
          @for (a of data.agreements(); track a.id) {
            <tr>
              <td>{{ tenantName(a.tenantId) }}</td>
              <td>{{ unitLabel(a.unitId) }}</td>
              <td>{{ a.startDate }}</td>
              <td><a class="btn btn-sm" [routerLink]="['/landlord/rentals', a.id]">View</a></td>
            </tr>
          }
        </tbody>
      </table>
    </div>
  `,
})
export class RentalListComponent {
  protected readonly data = inject(MockDataService);

  tenantName(tenantId: string): string {
    return this.data.tenants().find((t) => t.id === tenantId)?.name ?? '—';
  }
  unitLabel(unitId: string): string {
    return this.data.units().find((u) => u.id === unitId)?.unitNumber ?? '—';
  }
}
