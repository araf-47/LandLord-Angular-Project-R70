import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { CURRENT_OWNER_ID, MockDataService } from '../../../core/mock-data.service';

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
      <table>
        <thead><tr><th>Property</th><th>Address</th><th>Units</th></tr></thead>
        <tbody>
          @for (p of myProperties(); track p.id) {
            <tr>
              <td>{{ p.name }}</td>
              <td>{{ p.address }}</td>
              <td>{{ data.unitsByProperty(p.id).length }}</td>
            </tr>
          } @empty {
            <tr><td colspan="3" class="hint-text">No properties yet.</td></tr>
          }
        </tbody>
      </table>
    </div>
  `,
})
export class OwnerPropertyListComponent {
  protected readonly data = inject(MockDataService);

  myProperties() {
    return this.data.ownerProperties().filter((p) => p.ownerId === CURRENT_OWNER_ID);
  }
}
