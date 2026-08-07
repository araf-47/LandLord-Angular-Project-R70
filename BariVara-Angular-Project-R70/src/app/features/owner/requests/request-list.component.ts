import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { CURRENT_OWNER_ID, MockDataService } from '../../../core/mock-data.service';

@Component({
  selector: 'app-owner-request-list',
  standalone: true,
  imports: [RouterLink],
  template: `
    <h1>Requests for my listings</h1>
    <div class="card">
      <div class="table-scroll">
      <table>
        <thead><tr><th>Applicant</th><th>Listing</th><th>Status</th><th></th></tr></thead>
        <tbody>
          @for (r of myRequests(); track r.id) {
            <tr>
              <td>{{ r.applicantName }}</td>
              <td>{{ listingTitle(r.listingId) }}</td>
              <td><span class="badge badge-pending">{{ r.status }}</span></td>
              <td><a class="btn btn-sm" [routerLink]="['/owner/requests', r.id]">Open</a></td>
            </tr>
          } @empty {
            <tr><td colspan="4" class="hint-text">No requests yet.</td></tr>
          }
        </tbody>
      </table>
      </div>
    </div>
  `,
})
export class OwnerRequestListComponent {
  protected readonly data = inject(MockDataService);

  myRequests() {
    return this.data.requestsForOwner(CURRENT_OWNER_ID);
  }

  listingTitle(listingId: string): string {
    return this.data.listingById(listingId)?.title ?? '—';
  }
}
