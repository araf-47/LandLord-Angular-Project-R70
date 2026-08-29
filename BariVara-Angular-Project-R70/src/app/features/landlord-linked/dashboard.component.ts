import { Component, OnInit, inject, signal } from '@angular/core';
import { LANDLORD_CORE_DEV_URL } from '../../core/cross-app.config';
import { ListingApiService } from '../../core/listing-api.service';

@Component({
  selector: 'app-landlord-linked-dashboard',
  standalone: true,
  template: `
    @switch (status()) {
      @case ('loading') {
        <div class="card"><p class="hint-text">Loading synced ads…</p></div>
      }
      @case ('error') {
        <div class="card">
          <p class="text-danger mb-sm">Couldn't load this page. {{ error() }}</p>
          <button type="button" class="btn btn-sm" (click)="ngOnInit()">Retry</button>
        </div>
      }
      @case ('ready') {
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
          <p class="hint-text mt-sm">
            Opens the LandLord dev site in a new tab — a separate app, so this just jumps
            over there rather than embedding it here.
          </p>
        </div>
      }
    }
  `,
})
export class LandlordLinkedDashboardComponent implements OnInit {
  private readonly api = inject(ListingApiService);
  protected readonly landlordCoreUrl = LANDLORD_CORE_DEV_URL;

  readonly status = signal<'loading' | 'error' | 'ready'>('loading');
  readonly error = signal<string | undefined>(undefined);

  async ngOnInit(): Promise<void> {
    this.status.set('loading');
    try {
      await this.api.search();
      this.status.set('ready');
    } catch {
      this.error.set('Check your connection and try again.');
      this.status.set('error');
    }
  }

  syncedListings() {
    return this.api.listings().filter((l) => l.source === 'landlord-linked');
  }
}
