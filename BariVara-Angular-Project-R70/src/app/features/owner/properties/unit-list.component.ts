import { Component, OnInit, inject } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { OwnerPropertyApiService } from '../../../core/owner-property-api.service';
import { OwnerUnitApiService } from '../../../core/owner-unit-api.service';
import { CURRENT_OWNER_ID_REAL } from '../../../core/current-owner';

@Component({
  selector: 'app-owner-unit-list',
  standalone: true,
  imports: [RouterLink],
  template: `
    <div class="topbar" style="background:transparent;border:none;padding:0;margin-bottom:1rem;">
      <h1>{{ propertyName() }} — units</h1>
      <a class="btn btn-primary" [routerLink]="['/owner/properties', propertyId, 'units', 'new']">Add unit</a>
    </div>

    <div class="card">
      <div class="table-scroll">
      <table>
        <thead>
          <tr><th>Unit</th><th>Rent</th><th></th></tr>
        </thead>
        <tbody>
          @for (u of unitsForProperty(); track u.id) {
            <tr>
              <td>{{ u.unitNumber }}</td>
              <td>{{ u.rent }}</td>
              <td class="actions-row">
                <a class="btn btn-sm" [routerLink]="['/owner/properties', propertyId, 'units', u.id, 'edit']">Edit</a>
                <button type="button" class="btn btn-sm btn-danger" (click)="remove(u.id)">Delete</button>
              </td>
            </tr>
          } @empty {
            <tr><td colspan="3" class="hint-text">No units yet.</td></tr>
          }
        </tbody>
      </table>
      </div>
    </div>
  `,
})
export class OwnerUnitListComponent implements OnInit {
  protected readonly propertyApi = inject(OwnerPropertyApiService);
  protected readonly unitApi = inject(OwnerUnitApiService);
  protected readonly propertyId = inject(ActivatedRoute).snapshot.paramMap.get('propertyId')!;

  async ngOnInit(): Promise<void> {
    if (this.propertyApi.properties().length === 0) {
      await this.propertyApi.loadForOwner(CURRENT_OWNER_ID_REAL);
    }
    await this.unitApi.loadForProperties([+this.propertyId]);
  }

  propertyName(): string {
    return this.propertyApi.properties().find((p) => p.id === +this.propertyId)?.name ?? 'Property';
  }

  unitsForProperty() {
    return this.unitApi.units().filter((u) => u.propertyId === +this.propertyId);
  }

  async remove(unitId: number): Promise<void> {
    if (!confirm('Delete this unit? This cannot be undone.')) return;
    await this.unitApi.delete(unitId);
  }
}
