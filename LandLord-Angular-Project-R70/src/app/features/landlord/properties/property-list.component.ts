import { Component, OnInit, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MockDataService } from '../../../core/mock-data.service';
import { PropertyApiService } from '../../../core/property-api.service';

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
      <div class="table-scroll">
      <table>
        <thead>
          <tr><th>Property</th><th>Address</th><th>Units</th><th></th></tr>
        </thead>
        <tbody>
          @for (p of api.properties(); track p.id) {
            <tr>
              <td>{{ p.name }}</td>
              <td>{{ p.address }}</td>
              <td>{{ data.unitsByProperty(p.id + '').length }}</td>
              <td class="actions-row">
                <a class="btn btn-sm" [routerLink]="['/landlord/properties', p.id, 'units']">Manage units</a>
                <a class="btn btn-sm" [routerLink]="['/landlord/properties', p.id, 'edit']">Edit</a>
                <button type="button" class="btn btn-sm btn-danger" (click)="remove(p.id)">Delete</button>
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
export class PropertyListComponent implements OnInit {
  protected readonly data = inject(MockDataService);
  protected readonly api = inject(PropertyApiService);

  ngOnInit(): void {
    this.api.load();
  }

  async remove(id: number): Promise<void> {
    if (!confirm('Delete this property? This cannot be undone.')) return;
    await this.api.delete(id);
  }
}
