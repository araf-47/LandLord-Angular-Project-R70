import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MaintenanceApiService } from '../../../core/maintenance-api.service';
import { TenantApiService } from '../../../core/tenant-api.service';
import { UnitApiService } from '../../../core/unit-api.service';

@Component({
  selector: 'app-landlord-ticket-list',
  standalone: true,
  imports: [RouterLink],
  template: `
    <div class="topbar topbar-plain">
      <h1>Maintenance</h1>
      <a class="btn btn-primary" routerLink="/landlord/maintenance/new">Log new issue</a>
    </div>

    @switch (status()) {
      @case ('loading') {
        <div class="card"><p class="hint-text">Loading tickets…</p></div>
      }
      @case ('error') {
        <div class="card">
          <p class="text-danger mb-sm">Couldn't load this page. {{ error() }}</p>
          <button type="button" class="btn btn-sm" (click)="ngOnInit()">Retry</button>
        </div>
      }
      @case ('ready') {
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
      }
    }
  `,
})
export class LandlordTicketListComponent implements OnInit {
  protected readonly api = inject(MaintenanceApiService);
  private readonly unitApi = inject(UnitApiService);
  private readonly tenantApi = inject(TenantApiService);

  readonly status = signal<'loading' | 'error' | 'ready'>('loading');
  readonly error = signal<string | undefined>(undefined);

  async ngOnInit(): Promise<void> {
    this.status.set('loading');
    try {
      await Promise.all([this.api.load(), this.unitApi.load(), this.tenantApi.load()]);
      this.status.set('ready');
    } catch {
      this.error.set('Check your connection and try again.');
      this.status.set('error');
    }
  }

  unitLabel(unitId: number): string {
    return this.unitApi.units().find((u) => u.id === unitId)?.unitNumber ?? '—';
  }
  tenantName(tenantId: number): string {
    return this.tenantApi.tenants().find((t) => t.id === tenantId)?.name ?? '—';
  }
}
