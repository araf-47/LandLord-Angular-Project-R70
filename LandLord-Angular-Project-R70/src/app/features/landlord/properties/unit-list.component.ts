import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { API_ORIGIN } from '../../../core/maintenance-api.service';
import { PropertyApiService } from '../../../core/property-api.service';
import { UnitApiService } from '../../../core/unit-api.service';
import { TenantApiService } from '../../../core/tenant-api.service';
import { BillingApiService } from '../../../core/billing-api.service';
import { ConfirmService } from '../../../shared/confirm.service';
import { UnitCardComponent } from './unit-card.component';

@Component({
  selector: 'app-unit-list',
  standalone: true,
  imports: [RouterLink, UnitCardComponent],
  template: `
    <div class="topbar topbar-plain">
      <h1>{{ propertyName() }} — units</h1>
      <a class="btn btn-primary" [routerLink]="['/landlord/properties', propertyId, 'units', 'new']">Add unit</a>
    </div>

    <div class="actions-row">
      <button type="button" class="btn btn-sm" [class.btn-primary]="viewMode() === 'grid'" (click)="viewMode.set('grid')">Grid</button>
      <button type="button" class="btn btn-sm" [class.btn-primary]="viewMode() === 'table'" (click)="viewMode.set('table')">Table</button>
    </div>

    @if (viewMode() === 'grid') {
      <div class="unit-grid">
        @for (u of unitApi.units(); track u.id) {
          <app-unit-card [unit]="u" [tenantName]="tenantNameFor(u.id)" [overdue]="isOverdue(u.id)" />
        }
      </div>
    } @else {
      <div class="card">
        <div class="table-scroll">
        <table>
          <thead>
            <tr><th>Photo</th><th>Unit</th><th>Rent</th><th>Status</th><th></th></tr>
          </thead>
          <tbody>
            @for (u of unitApi.units(); track u.id) {
              <tr>
                <td>
                  @if (u.photoUrl) {
                    <img [src]="photoOrigin + u.photoUrl" alt="" class="img-thumb" />
                  } @else {
                    <span class="hint-text">No photo</span>
                  }
                </td>
                <td>{{ u.unitNumber }}</td>
                <td>{{ u.rent }}</td>
                <td><span class="badge" [class.badge-vacant]="u.status === 'vacant'" [class.badge-occupied]="u.status === 'occupied'">{{ u.status }}</span></td>
                <td class="actions-row">
                  <a class="btn btn-sm" [routerLink]="['/landlord/properties', propertyId, 'units', u.id, 'edit']">Edit</a>
                  <button type="button" class="btn btn-sm btn-danger" (click)="remove(u.id)">Delete</button>
                </td>
              </tr>
            }
          </tbody>
        </table>
        </div>
      </div>
    }
  `,
})
export class UnitListComponent implements OnInit {
  protected readonly propertyApi = inject(PropertyApiService);
  protected readonly unitApi = inject(UnitApiService);
  protected readonly tenantApi = inject(TenantApiService);
  private readonly billingApi = inject(BillingApiService);
  protected readonly propertyId = inject(ActivatedRoute).snapshot.paramMap.get('propertyId')!;
  protected readonly photoOrigin = API_ORIGIN;
  private readonly confirmService = inject(ConfirmService);

  protected readonly viewMode = signal<'grid' | 'table'>('grid');
  private readonly outstandingByTenant = signal<Map<number, number>>(new Map());

  async ngOnInit(): Promise<void> {
    if (this.propertyApi.properties().length === 0) {
      await this.propertyApi.load();
    }
    await this.unitApi.load(+this.propertyId);
    if (this.tenantApi.tenants().length === 0) {
      await this.tenantApi.load();
    }
    await this.loadOverdueStatus();
  }

  private async loadOverdueStatus(): Promise<void> {
    const occupiedTenantIds = this.unitApi
      .units()
      .filter((u) => u.status === 'occupied')
      .map((u) => this.tenantApi.tenants().find((t) => t.unitId === u.id)?.id)
      .filter((id): id is number => id != null);

    const entries = await Promise.all(
      occupiedTenantIds.map(async (id) => [id, await this.billingApi.outstandingBalance(id)] as const)
    );
    this.outstandingByTenant.set(new Map(entries));
  }

  propertyName(): string {
    return this.propertyApi.properties().find((p) => p.id === +this.propertyId)?.name ?? 'Property';
  }

  tenantNameFor(unitId: number): string | null {
    return this.tenantApi.tenants().find((t) => t.unitId === unitId)?.name ?? null;
  }

  isOverdue(unitId: number): boolean {
    const tenant = this.tenantApi.tenants().find((t) => t.unitId === unitId);
    if (!tenant) return false;
    return (this.outstandingByTenant().get(tenant.id) ?? 0) > 0;
  }

  async remove(unitId: number): Promise<void> {
    const confirmed = await this.confirmService.confirm('Delete unit', 'Delete this unit? This cannot be undone.');
    if (!confirmed) return;
    await this.unitApi.delete(unitId);
  }
}
