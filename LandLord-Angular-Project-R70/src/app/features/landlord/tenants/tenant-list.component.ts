import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ApiTenant, TenantApiService } from '../../../core/tenant-api.service';
import { UnitApiService } from '../../../core/unit-api.service';

@Component({
  selector: 'app-tenant-list',
  standalone: true,
  imports: [RouterLink, FormsModule],
  template: `
    <div class="topbar topbar-plain">
      <h1>Tenant Management</h1>
      <a class="btn btn-primary" routerLink="/landlord/tenants/register">Register tenant (walk-in)</a>
    </div>

    <div class="search-bar mb-md">
      <input
        [ngModel]="query()"
        (ngModelChange)="query.set($event)"
        name="query"
        placeholder="Search by name, National ID, or phone"
      />
      <select [ngModel]="statusFilter()" (ngModelChange)="statusFilter.set($event)" name="statusFilter" class="max-w-xs">
        <option value="active">Active</option>
        <option value="inactive">Inactive</option>
        <option value="all">All</option>
      </select>
    </div>

    @switch (status()) {
      @case ('loading') {
        <div class="card"><p class="hint-text">Loading tenants…</p></div>
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
                  <td class="actions-row mb-0">
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
      }
    }
  `,
})
export class TenantListComponent implements OnInit {
  protected readonly api = inject(TenantApiService);
  protected readonly unitApi = inject(UnitApiService);

  readonly query = signal('');
  readonly statusFilter = signal<'active' | 'inactive' | 'all'>('active');

  readonly status = signal<'loading' | 'error' | 'ready'>('loading');
  readonly error = signal<string | undefined>(undefined);

  async ngOnInit(): Promise<void> {
    this.status.set('loading');
    try {
      await Promise.all([this.api.load(), this.unitApi.load()]);
      this.status.set('ready');
    } catch {
      this.error.set('Check your connection and try again.');
      this.status.set('error');
    }
  }

  readonly filteredTenants = computed(() => {
    const q = this.query().trim().toLowerCase();
    const status = this.statusFilter();

    return this.api.tenants().filter((t: ApiTenant) => {
      const matchesStatus = status === 'all' || t.status === status;
      const matchesQuery =
        !q ||
        t.name.toLowerCase().includes(q) ||
        t.nationalId.toLowerCase().includes(q) ||
        t.phone.toLowerCase().includes(q);
      return matchesStatus && matchesQuery;
    });
  });

  unitLabel(unitId: number | null): string {
    return this.unitApi.units().find((u) => u.id === unitId)?.unitNumber ?? '—';
  }
}
