import { Component, OnInit, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MaintenanceApiService } from '../../../core/maintenance-api.service';
import { TenantApiService } from '../../../core/tenant-api.service';
import { UnitApiService } from '../../../core/unit-api.service';

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
          @for (t of api.tickets(); track t.id) {
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
export class LandlordTicketListComponent implements OnInit {
  protected readonly api = inject(MaintenanceApiService);
  private readonly unitApi = inject(UnitApiService);
  private readonly tenantApi = inject(TenantApiService);

  async ngOnInit(): Promise<void> {
    await Promise.all([this.api.load(), this.unitApi.load(), this.tenantApi.load()]);
  }

  unitLabel(unitId: number): string {
    return this.unitApi.units().find((u) => u.id === unitId)?.unitNumber ?? '—';
  }
  tenantName(tenantId: number): string {
    return this.tenantApi.tenants().find((t) => t.id === tenantId)?.name ?? '—';
  }
}
