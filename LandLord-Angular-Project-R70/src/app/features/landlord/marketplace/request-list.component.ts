import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MockDataService } from '../../../core/mock-data.service';

@Component({
  selector: 'app-request-list',
  standalone: true,
  imports: [RouterLink],
  template: `
    <h1>Booking requests (synced from BariVara)</h1>
    <div class="card">
      <div class="table-scroll">
      <table>
        <thead><tr><th>Applicant</th><th>Unit</th><th>Status</th><th></th></tr></thead>
        <tbody>
          @for (r of data.marketplaceRequests(); track r.id) {
            <tr>
              <td>{{ r.applicantName }}</td>
              <td>{{ unitLabel(r.unitId) }}</td>
              <td><span class="badge badge-pending">{{ r.status }}</span></td>
              <td><a class="btn btn-sm" [routerLink]="['/landlord/marketplace/requests', r.id]">Open</a></td>
            </tr>
          }
        </tbody>
      </table>
      </div>
    </div>
  `,
})
export class RequestListComponent {
  protected readonly data = inject(MockDataService);

  unitLabel(unitId: string): string {
    return this.data.units().find((u) => u.id === unitId)?.unitNumber ?? '—';
  }
}
