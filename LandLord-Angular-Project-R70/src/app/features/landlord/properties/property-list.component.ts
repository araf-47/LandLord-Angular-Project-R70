import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MockDataService } from '../../../core/mock-data.service';

@Component({
  selector: 'app-property-list',
  standalone: true,
  imports: [RouterLink],
  template: `
    <div class="topbar" style="background:transparent;border:none;padding:0;margin-bottom:1rem;">
      <h1>Property & Units</h1>
      <a class="btn btn-primary" routerLink="/landlord/properties/new">Add property</a>
    </div>

    <div class="card">
      <table>
        <thead>
          <tr><th>Property</th><th>Address</th><th>Units</th><th></th></tr>
        </thead>
        <tbody>
          @for (p of data.properties(); track p.id) {
            <tr>
              <td>{{ p.name }}</td>
              <td>{{ p.address }}</td>
              <td>{{ data.unitsByProperty(p.id).length }}</td>
              <td><a class="btn btn-sm" [routerLink]="['/landlord/properties', p.id, 'units']">Manage units</a></td>
            </tr>
          }
        </tbody>
      </table>
    </div>
  `,
})
export class PropertyListComponent {
  protected readonly data = inject(MockDataService);
}
