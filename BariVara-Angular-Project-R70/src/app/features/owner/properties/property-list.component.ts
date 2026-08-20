import { Component, computed, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { OwnerPropertyApiService } from '../../../core/owner-property-api.service';
import { OwnerUnitApiService } from '../../../core/owner-unit-api.service';
import { CURRENT_OWNER_ID_REAL } from '../../../core/current-owner';

@Component({
  selector: 'app-owner-property-list',
  standalone: true,
  imports: [RouterLink],
  template: `
    <div class="topbar" style="background:transparent;border:none;padding:0;margin-bottom:1rem;">
      <h1>Properties</h1>
      <a class="btn btn-primary" routerLink="/owner/properties/new">Add property</a>
    </div>

    <div class="card">
      <div class="table-scroll">
      <table>
        <thead><tr><th>Property</th><th>Address</th><th>Units</th></tr></thead>
        <tbody>
          @for (p of myProperties(); track p.id) {
            <tr>
              <td>{{ p.name }}</td>
              <td>{{ p.address }}</td>
              <td>{{ unitCount(p.id) }}</td>
            </tr>
          } @empty {
            <tr><td colspan="3" class="hint-text">No properties yet.</td></tr>
          }
        </tbody>
      </table>
      </div>
    </div>
  `,
})
export class OwnerPropertyListComponent {
  protected readonly propertyApi = inject(OwnerPropertyApiService);
  private readonly unitApi = inject(OwnerUnitApiService);

  constructor() {
    this.propertyApi.loadForOwner(CURRENT_OWNER_ID_REAL).then((properties) => {
      this.unitApi.loadForProperties(properties.map((p) => p.id));
    });
  }

  readonly myProperties = computed(() => this.propertyApi.properties());

  unitCount(propertyId: number): number {
    return this.unitApi.units().filter((u) => u.propertyId === propertyId).length;
  }
}
