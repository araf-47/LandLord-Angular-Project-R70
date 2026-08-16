import { Component, inject } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MockDataService } from '../../../core/mock-data.service';

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
          <tr><th>Unit</th><th>Rent</th><th>Status</th><th></th></tr>
        </thead>
        <tbody>
          @for (u of data.unitsByProperty(propertyId); track u.id) {
            <tr>
              <td>{{ u.unitNumber }}</td>
              <td>{{ u.rent }}</td>
              <td><span class="badge" [class.badge-vacant]="u.status === 'vacant'" [class.badge-occupied]="u.status === 'occupied'">{{ u.status }}</span></td>
              <td><a class="btn btn-sm" [routerLink]="['/landlord/properties', propertyId, 'units', u.id, 'edit']">Edit</a></td>
            </tr>
          }
        </tbody>
      </table>
      </div>
    </div>
  `,
})
export class UnitListComponent {
  protected readonly data = inject(MockDataService);
  protected readonly propertyId = inject(ActivatedRoute).snapshot.paramMap.get('propertyId')!;

  propertyName(): string {
    return this.data.properties().find((p) => p.id === this.propertyId)?.name ?? 'Property';
  }
}
