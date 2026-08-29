import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { BookingApiService } from '../../../core/booking-api.service';
import { ListingApiService } from '../../../core/listing-api.service';
import { ProfileApiService } from '../../../core/profile-api.service';

@Component({
  selector: 'app-owner-request-list',
  standalone: true,
  imports: [RouterLink],
  template: `
    @switch (status()) {
      @case ('loading') {
        <div class="card"><p class="hint-text">Loading requests…</p></div>
      }
      @case ('error') {
        <div class="card">
          <p class="text-danger mb-sm">Couldn't load this page. {{ error() }}</p>
          <button type="button" class="btn btn-sm" (click)="ngOnInit()">Retry</button>
        </div>
      }
      @case ('ready') {
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
      }
    }
  `,
})
export class OwnerRequestListComponent implements OnInit {
  private readonly bookingApi = inject(BookingApiService);
  private readonly listingApi = inject(ListingApiService);
  private readonly profileApi = inject(ProfileApiService);

  readonly status = signal<'loading' | 'error' | 'ready'>('loading');
  readonly error = signal<string | undefined>(undefined);

  async ngOnInit(): Promise<void> {
    this.status.set('loading');
    try {
      const profile = await this.profileApi.myOwnerProfile();
      await this.bookingApi.loadForOwner(profile.id);
      await this.listingApi.search({ ownerId: profile.id });
      this.status.set('ready');
    } catch {
      this.error.set('Check your connection and try again.');
      this.status.set('error');
    }
  }

  readonly myRequests = computed(() => this.bookingApi.requests());

  listingTitle(listingId: number): string {
    return this.listingApi.listings().find((l) => l.id === listingId)?.title ?? '—';
  }
}
