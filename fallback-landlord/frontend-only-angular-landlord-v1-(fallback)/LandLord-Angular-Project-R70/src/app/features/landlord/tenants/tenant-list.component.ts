import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { MockDataService, TenantRecord } from '../../../core/mock-data.service';

@Component({
  selector: 'app-tenant-list',
  standalone: true,
  imports: [RouterLink, FormsModule],
  template: `
    <div class="topbar" style="background:transparent;border:none;padding:0;margin-bottom:1rem;">
      <h1>Tenant Management</h1>
      <a class="btn btn-primary" routerLink="/landlord/tenants/register">Register tenant (walk-in)</a>
    </div>

    <div class="search-bar" style="margin-bottom:1rem;">
      <input
        [ngModel]="query()"
        (ngModelChange)="query.set($event)"
        name="query"
        placeholder="Search by name, National ID, or phone"
      />
      <select [ngModel]="statusFilter()" (ngModelChange)="statusFilter.set($event)" name="statusFilter" style="max-width:160px;">
        <option value="active">Active</option>
        <option value="inactive">Inactive</option>
        <option value="all">All</option>
      </select>
    </div>

    <div class="card">
      <div class="table-scroll">
      <table>
        <thead>
          <tr><th>Name</th><th>National ID</th><th>Phone</th><th>Unit</th><th>Status</th><th></th></tr>
        </thead>
        <tbody>
          @for (t of filteredTenants(); track t.id) {
            <tr>
              <td>{{ t.name }}</td>
              <td>{{ t.nationalId }}</td>
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
          } @empty {
            <tr><td colspan="6" class="hint-text">No tenants match.</td></tr>
          }
        </tbody>
      </table>
      </div>
    </div>
  `,
})
export class TenantListComponent {
  protected readonly data = inject(MockDataService);

  readonly query = signal('');
  readonly statusFilter = signal<'active' | 'inactive' | 'all'>('active');

  readonly filteredTenants = computed(() => {
    const q = this.query().trim().toLowerCase();
    const status = this.statusFilter();

    return this.data.tenants().filter((t: TenantRecord) => {
      const matchesStatus = status === 'all' || t.status === status;
      const matchesQuery =
        !q ||
        t.name.toLowerCase().includes(q) ||
        t.nationalId.toLowerCase().includes(q) ||
        t.phone.toLowerCase().includes(q);
      return matchesStatus && matchesQuery;
    });
  });

  unitLabel(unitId?: string): string {
    return this.data.units().find((u) => u.id === unitId)?.unitNumber ?? '—';
  }
}
