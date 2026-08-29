import { Component, OnInit, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { PropertyApiService } from '../../../core/property-api.service';
import { UnitApiService } from '../../../core/unit-api.service';
import { ConfirmService } from '../../../shared/confirm.service';

@Component({
  selector: 'app-property-list',
  standalone: true,
  imports: [RouterLink],
  template: `
    <div class="topbar topbar-plain">
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
              <td>{{ unitCount(p.id) }}</td>
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
  protected readonly api = inject(PropertyApiService);
  protected readonly unitApi = inject(UnitApiService);
  private readonly confirmService = inject(ConfirmService);

  async ngOnInit(): Promise<void> {
    await Promise.all([this.api.load(), this.unitApi.load()]);
  }

  unitCount(propertyId: number): number {
    return this.unitApi.units().filter((u) => u.propertyId === propertyId).length;
  }

  async remove(id: number): Promise<void> {
    const confirmed = await this.confirmService.confirm('Delete property', 'Delete this property? This cannot be undone.');
    if (!confirmed) return;
    await this.api.delete(id);
  }
}
