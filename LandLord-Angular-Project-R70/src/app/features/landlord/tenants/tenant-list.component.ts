import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MockDataService } from '../../../core/mock-data.service';

@Component({
  selector: 'app-tenant-list',
  standalone: true,
  imports: [RouterLink],
  template: `
    <div class="topbar" style="background:transparent;border:none;padding:0;margin-bottom:1rem;">
      <h1>Tenant Management</h1>
      <a class="btn btn-primary" routerLink="/landlord/tenants/register">Register tenant (walk-in)</a>
    </div>

    <div class="card">
      <table>
        <thead>
          <tr><th>Name</th><th>Phone</th><th>Unit</th><th>Status</th><th></th></tr>
        </thead>
        <tbody>
          @for (t of data.tenants(); track t.id) {
            <tr>
              <td>{{ t.name }}</td>
              <td>{{ t.phone }}</td>
              <td>{{ unitLabel(t.unitId) }}</td>
              <td>{{ t.status }}</td>
              <td class="actions-row" style="margin:0;">
                <a class="btn btn-sm" [routerLink]="['/landlord/tenants', t.id]">View</a>
                @if (t.status === 'active') {
                  <a class="btn btn-sm btn-danger" [routerLink]="['/landlord/tenants', t.id, 'move-out']">Move out</a>
                }
              </td>
            </tr>
          }
        </tbody>
      </table>
    </div>
  `,
})
export class TenantListComponent {
  protected readonly data = inject(MockDataService);

  unitLabel(unitId?: string): string {
    return this.data.units().find((u) => u.id === unitId)?.unitNumber ?? '—';
  }
}
