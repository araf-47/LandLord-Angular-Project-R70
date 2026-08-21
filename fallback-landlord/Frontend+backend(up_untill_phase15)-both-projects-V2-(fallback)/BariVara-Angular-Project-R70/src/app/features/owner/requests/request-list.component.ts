import { Component, computed, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { BookingApiService } from '../../../core/booking-api.service';
import { ListingApiService } from '../../../core/listing-api.service';
import { CURRENT_OWNER_ID_REAL } from '../../../core/current-owner';

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
  private readonly bookingApi = inject(BookingApiService);
  private readonly listingApi = inject(ListingApiService);

  constructor() {
    this.bookingApi.loadForOwner(CURRENT_OWNER_ID_REAL);
    this.listingApi.search({ ownerId: CURRENT_OWNER_ID_REAL });
  }

  readonly myRequests = computed(() => this.bookingApi.requests());

  listingTitle(listingId: number): string {
    return this.listingApi.listings().find((l) => l.id === listingId)?.title ?? '—';
  }
}
