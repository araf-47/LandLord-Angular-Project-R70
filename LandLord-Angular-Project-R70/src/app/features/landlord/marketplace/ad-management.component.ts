import { Component, inject } from '@angular/core';
import { MockDataService } from '../../../core/mock-data.service';

@Component({
  selector: 'app-ad-management',
  standalone: true,
  template: `
    <h1>Ad status per unit</h1>
    <div class="card">
      <table>
        <thead><tr><th>Unit</th><th>Status</th><th>Ad state</th><th></th></tr></thead>
        <tbody>
          @for (u of data.units(); track u.id) {
            <tr>
              <td>{{ u.unitNumber }}</td>
              <td><span class="badge" [class.badge-vacant]="u.status === 'vacant'" [class.badge-occupied]="u.status === 'occupied'">{{ u.status }}</span></td>
              <td>{{ u.status === 'vacant' ? 'Live on BariVara.com' : 'Not listed' }}</td>
              <td>
                @if (u.status === 'vacant') {
                  <button class="btn btn-sm">Repost</button>
                  <button class="btn btn-sm">Pause</button>
                }
              </td>
            </tr>
          }
        </tbody>
      </table>
    </div>
  `,
})
export class AdManagementComponent {
  protected readonly data = inject(MockDataService);
}
