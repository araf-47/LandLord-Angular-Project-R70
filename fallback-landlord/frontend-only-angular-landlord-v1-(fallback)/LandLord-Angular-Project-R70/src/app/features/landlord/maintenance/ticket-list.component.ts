import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MockDataService } from '../../../core/mock-data.service';

@Component({
  selector: 'app-landlord-ticket-list',
  standalone: true,
  imports: [RouterLink],
  template: `
    <div class="topbar" style="background:transparent;border:none;padding:0;margin-bottom:1rem;">
      <h1>Maintenance</h1>
      <a class="btn btn-primary" routerLink="/landlord/maintenance/new">Log new issue</a>
    </div>

    <div class="card">
      <div class="table-scroll">
      <table>
        <thead><tr><th>Unit</th><th>Tenant</th><th>Description</th><th>Status</th><th></th></tr></thead>
        <tbody>
          @for (t of data.tickets(); track t.id) {
            <tr>
              <td>{{ unitLabel(t.unitId) }}</td>
              <td>{{ tenantName(t.tenantId) }}</td>
              <td>{{ t.description }}</td>
              <td><span class="badge" [class.badge-pending]="t.status === 'pending'" [class.badge-paid]="t.status === 'resolved'">{{ t.status }}</span></td>
              <td><a class="btn btn-sm" [routerLink]="['/landlord/maintenance', t.id]">Open</a></td>
            </tr>
          }
        </tbody>
      </table>
      </div>
    </div>
  `,
})
export class LandlordTicketListComponent {
  protected readonly data = inject(MockDataService);

  unitLabel(unitId: string): string {
    return this.data.units().find((u) => u.id === unitId)?.unitNumber ?? '—';
  }
  tenantName(tenantId: string): string {
    return this.data.tenants().find((t) => t.id === tenantId)?.name ?? '—';
  }
}
