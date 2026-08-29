import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { API_ORIGIN, ListingApiService } from '../../core/listing-api.service';
import { FavoriteApiService } from '../../core/favorite-api.service';
import { ProfileApiService } from '../../core/profile-api.service';

@Component({
  selector: 'app-tenant-favorites',
  standalone: true,
  imports: [RouterLink],
  template: `
    <h1>Favorite List</h1>
    @switch (status()) {
      @case ('loading') {
        <div class="card"><p class="hint-text">Loading favorites…</p></div>
      }
      @case ('error') {
        <div class="card">
          <p class="text-danger mb-sm">Couldn't load this page. {{ error() }}</p>
          <button type="button" class="btn btn-sm" (click)="ngOnInit()">Retry</button>
        </div>
      }
      @case ('ready') {
        <div class="listing-grid">
          @for (l of favoriteListings(); track l.id) {
            <div class="listing-card">
              @if (l.photoUrl) {
                <img class="listing-card-image img-cover" [src]="photoOrigin + l.photoUrl" alt="" />
              } @else {
                <div class="listing-card-image no-photo">
                  <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M3 11.5 12 4l9 7.5" /><path d="M5 10v9a1 1 0 0 0 1 1h4v-6h4v6h4a1 1 0 0 0 1-1v-9" /></svg>
                  <span>No photo yet</span>
                </div>
              }
              <div class="listing-card-body">
                <div class="listing-card-title">{{ l.title }}</div>
                <p>{{ l.address }}</p>
                <div class="listing-card-rent">৳{{ l.rent }}/mo</div>
                <div class="actions-row">
                  <a class="btn btn-sm btn-primary" [routerLink]="['/tenant/listings', l.id]">Book</a>
                  <button class="btn btn-sm btn-danger" (click)="remove(l.id)">Delete</button>
                </div>
              </div>
            </div>
          } @empty {
            <p class="hint-text">No favorites saved yet.</p>
          }
        </div>
      }
    }
  `,
})
export class TenantFavoritesComponent implements OnInit {
  private readonly listingApi = inject(ListingApiService);
  private readonly favoriteApi = inject(FavoriteApiService);
  private readonly profileApi = inject(ProfileApiService);
  protected readonly photoOrigin = API_ORIGIN;
  private tenantId: number | null = null;

  readonly status = signal<'loading' | 'error' | 'ready'>('loading');
  readonly error = signal<string | undefined>(undefined);

  async ngOnInit(): Promise<void> {
    this.status.set('loading');
    try {
      const profile = await this.profileApi.myTenantProfile();
      this.tenantId = profile.id;
      await this.favoriteApi.loadForTenant(profile.id);
      const listingIds = new Set(this.favoriteApi.favorites().map((f) => f.listingId));
      const listings = await Promise.all([...listingIds].map((id) => this.listingApi.get(id)));
      this.listingApi.listings.set(listings);
      this.status.set('ready');
    } catch {
      this.error.set('Check your connection and try again.');
      this.status.set('error');
    }
  }

  readonly favoriteListings = computed(() => {
    const listingIds = new Set(this.favoriteApi.favorites().map((f) => f.listingId));
    return this.listingApi.listings().filter((l) => listingIds.has(l.id));
  });

  remove(listingId: number): void {
    const favorite = this.favoriteApi.favorites().find((f) => f.tenantId === this.tenantId && f.listingId === listingId);
    if (favorite) this.favoriteApi.remove(favorite.id);
  }
}
