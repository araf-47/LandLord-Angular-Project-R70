import { Component, computed, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { OwnerPropertyApiService } from '../../../core/owner-property-api.service';
import { OwnerUnitApiService } from '../../../core/owner-unit-api.service';
import { ProfileApiService } from '../../../core/profile-api.service';

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
        <thead><tr><th>Property</th><th>Address</th><th>Units</th><th></th></tr></thead>
        <tbody>
          @for (p of myProperties(); track p.id) {
            <tr>
              <td>{{ p.name }}</td>
              <td>{{ p.address }}</td>
              <td>{{ unitCount(p.id) }}</td>
              <td class="actions-row">
                <a class="btn btn-sm" [routerLink]="['/owner/properties', p.id, 'units']">Manage units</a>
              </td>
            </tr>
          } @empty {
            <tr><td colspan="4" class="hint-text">No properties yet.</td></tr>
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
  private readonly profileApi = inject(ProfileApiService);

  constructor() {
    this.profileApi.myOwnerProfile().then((profile) => {
      this.propertyApi.loadForOwner(profile.id).then((properties) => {
        this.unitApi.loadForProperties(properties.map((p) => p.id));
      });
    });
  }

  readonly myProperties = computed(() => this.propertyApi.properties());

  unitCount(propertyId: number): number {
    return this.unitApi.units().filter((u) => u.propertyId === propertyId).length;
  }
}
