import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { PropertyApiService } from '../../../core/property-api.service';
import { ApiUnit, UnitApiService } from '../../../core/unit-api.service';
import { TenantApiService } from '../../../core/tenant-api.service';
import { BillingApiService } from '../../../core/billing-api.service';
import { ConfirmService } from '../../../shared/confirm.service';
import { UnitCardComponent } from './unit-card.component';

@Component({
  selector: 'app-property-list',
  standalone: true,
  imports: [RouterLink, UnitCardComponent],
  template: `
    <div class="topbar topbar-plain">
      <h1>Property & Units</h1>
      <a class="btn btn-primary" routerLink="/landlord/properties/new">Add property</a>
    </div>

    <div class="card">
      <div class="table-scroll">
      <table>
        <thead>
          <tr><th></th><th>Property</th><th>Address</th><th>Units</th><th></th></tr>
        </thead>
        <tbody>
          @for (p of api.properties(); track p.id) {
            <tr class="property-row" [class.expanded]="expandedPropertyId() === p.id"
                role="button" tabindex="0"
                [attr.aria-expanded]="expandedPropertyId() === p.id"
                (click)="toggleExpand(p.id)" (keydown.enter)="toggleExpand(p.id)">
              <td class="expand-cell">
                <span class="chevron" [class.rotated]="expandedPropertyId() === p.id" aria-hidden="true">▸</span>
              </td>
              <td>{{ p.name }}</td>
              <td>{{ p.address }}</td>
              <td>{{ unitCount(p.id) }}</td>
              <td class="actions-row" (click)="$event.stopPropagation()">
                <a class="btn btn-sm" [routerLink]="['/landlord/properties', p.id, 'edit']">Edit</a>
                <button type="button" class="btn btn-sm btn-danger" (click)="remove(p.id)">Delete</button>
              </td>
            </tr>
            @if (expandedPropertyId() === p.id) {
              <tr class="property-units-row">
                <td colspan="5">
                  <div class="unit-grid">
                    @for (u of unitsFor(p.id); track u.id) {
                      <app-unit-card [unit]="u" [tenantName]="tenantNameFor(u.id)" [overdue]="isOverdue(u.id)" />
                    } @empty {
                      <p class="hint-text">No units yet for this property.</p>
                    }
                  </div>
                  <div class="actions-row" style="margin-top: 0.75rem;">
                    <a class="btn btn-sm btn-primary" [routerLink]="['/landlord/properties', p.id, 'units', 'new']">Add unit</a>
                  </div>
                </td>
              </tr>
            }
          } @empty {
            <tr><td colspan="5" class="hint-text">No properties yet.</td></tr>
          }
        </tbody>
      </table>
      </div>
    </div>
  `,
})
export class PropertyListComponent implements OnInit {
  protected readonly api = inject(PropertyApiService);
  protected readonly unitApi = inject(UnitApiService);
  protected readonly tenantApi = inject(TenantApiService);
  private readonly billingApi = inject(BillingApiService);
  private readonly confirmService = inject(ConfirmService);

  protected readonly expandedPropertyId = signal<number | null>(null);
  private readonly outstandingByTenant = signal<Map<number, number>>(new Map());

  async ngOnInit(): Promise<void> {
    await Promise.all([this.api.load(), this.unitApi.load(), this.tenantApi.load()]);
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

  toggleExpand(propertyId: number): void {
    this.expandedPropertyId.set(this.expandedPropertyId() === propertyId ? null : propertyId);
  }

  unitCount(propertyId: number): number {
    return this.unitApi.units().filter((u) => u.propertyId === propertyId).length;
  }

  unitsFor(propertyId: number): ApiUnit[] {
    return this.unitApi.units().filter((u) => u.propertyId === propertyId);
  }

  tenantNameFor(unitId: number): string | null {
    return this.tenantApi.tenants().find((t) => t.unitId === unitId)?.name ?? null;
  }

  isOverdue(unitId: number): boolean {
    const tenant = this.tenantApi.tenants().find((t) => t.unitId === unitId);
    if (!tenant) return false;
    return (this.outstandingByTenant().get(tenant.id) ?? 0) > 0;
  }

  async remove(id: number): Promise<void> {
    const confirmed = await this.confirmService.confirm('Delete property', 'Delete this property? This cannot be undone.');
    if (!confirmed) return;
    await this.api.delete(id);
  }
}
