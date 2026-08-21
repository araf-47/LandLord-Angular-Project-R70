import { Component, OnInit, inject } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { API_ORIGIN } from '../../../core/maintenance-api.service';
import { PropertyApiService } from '../../../core/property-api.service';
import { UnitApiService } from '../../../core/unit-api.service';

@Component({
  selector: 'app-unit-list',
  standalone: true,
  imports: [RouterLink],
  template: `
    <div class="topbar" style="background:transparent;border:none;padding:0;margin-bottom:1rem;">
      <h1>{{ propertyName() }} — units</h1>
      <a class="btn btn-primary" [routerLink]="['/landlord/properties', propertyId, 'units', 'new']">Add unit</a>
    </div>

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
                  <img [src]="photoOrigin + u.photoUrl" alt="" style="width:48px;height:48px;object-fit:cover;border-radius:4px;" />
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
  `,
})
export class UnitListComponent implements OnInit {
  protected readonly propertyApi = inject(PropertyApiService);
  protected readonly unitApi = inject(UnitApiService);
  protected readonly propertyId = inject(ActivatedRoute).snapshot.paramMap.get('propertyId')!;
  protected readonly photoOrigin = API_ORIGIN;

  async ngOnInit(): Promise<void> {
    if (this.propertyApi.properties().length === 0) {
      await this.propertyApi.load();
    }
    await this.unitApi.load(+this.propertyId);
  }

  propertyName(): string {
    return this.propertyApi.properties().find((p) => p.id === +this.propertyId)?.name ?? 'Property';
  }

  async remove(unitId: number): Promise<void> {
    if (!confirm('Delete this unit? This cannot be undone.')) return;
    await this.unitApi.delete(unitId);
  }
}
