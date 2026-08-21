import { Component, inject } from '@angular/core';
import { LANDLORD_CORE_DEV_URL } from '../../core/cross-app.config';
import { ListingApiService } from '../../core/listing-api.service';

@Component({
  selector: 'app-landlord-linked-dashboard',
  standalone: true,
  template: `
    <h1>View my live ads (auto-synced, read-only)</h1>
    <div class="card">
      <div class="table-scroll">
      <table>
        <thead><tr><th>Listing</th><th>Rent</th><th>Status</th></tr></thead>
        <tbody>
          @for (l of syncedListings(); track l.id) {
            <tr>
              <td>{{ l.title }}</td>
              <td>৳{{ l.rent }}</td>
              <td><span class="badge" [class.badge-vacant]="l.status === 'active'" [class.badge-occupied]="l.status === 'taken'">{{ l.status }}</span></td>
            </tr>
          } @empty {
            <tr><td colspan="3" class="hint-text">No synced ads yet.</td></tr>
          }
        </tbody>
      </table>
      </div>
    </div>

    <div class="card">
      <h3>Manage requests, chat, tenants, or anything else?</h3>
      <p>These are managed from LandLord core's Marketplace &amp; Leads module, not here.</p>
      <a class="btn btn-primary" [href]="landlordCoreUrl + '/landlord/marketplace/requests'" target="_blank" rel="noopener">
        Redirect to LandLord core → Marketplace &amp; Leads
      </a>
      <p class="hint-text" style="margin-top:0.5rem;">
        Opens the LandLord dev site in a new tab — a separate app, so this just jumps
        over there rather than embedding it here.
      </p>
    </div>
  `,
})
export class LandlordLinkedDashboardComponent {
  private readonly api = inject(ListingApiService);
  protected readonly landlordCoreUrl = LANDLORD_CORE_DEV_URL;

  constructor() {
    this.api.search();
  }

  syncedListings() {
    return this.api.listings().filter((l) => l.source === 'landlord-linked');
  }
}
