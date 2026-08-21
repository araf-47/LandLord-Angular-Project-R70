import { Component, computed, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { API_ORIGIN, ListingApiService } from '../../core/listing-api.service';
import { FavoriteApiService } from '../../core/favorite-api.service';
import { CURRENT_TENANT_ID_REAL } from '../../core/current-tenant';

@Component({
  selector: 'app-tenant-favorites',
  standalone: true,
  imports: [RouterLink],
  template: `
    <h1>Favorite List</h1>
    <div class="listing-grid">
      @for (l of favoriteListings(); track l.id) {
        <div class="listing-card">
          @if (l.photoUrl) {
            <img class="listing-card-image" [src]="photoOrigin + l.photoUrl" alt="" style="width:100%;object-fit:cover;" />
          } @else {
            <div class="listing-card-image">No photo</div>
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
  `,
})
export class TenantFavoritesComponent {
  private readonly listingApi = inject(ListingApiService);
  private readonly favoriteApi = inject(FavoriteApiService);
  protected readonly photoOrigin = API_ORIGIN;

  constructor() {
    this.favoriteApi.loadForTenant(CURRENT_TENANT_ID_REAL).then(() => {
      const listingIds = new Set(this.favoriteApi.favorites().map((f) => f.listingId));
      Promise.all([...listingIds].map((id) => this.listingApi.get(id))).then((listings) => {
        this.listingApi.listings.set(listings);
      });
    });
  }

  readonly favoriteListings = computed(() => {
    const listingIds = new Set(this.favoriteApi.favorites().map((f) => f.listingId));
    return this.listingApi.listings().filter((l) => listingIds.has(l.id));
  });

  remove(listingId: number): void {
    const favorite = this.favoriteApi.favorites().find((f) => f.tenantId === CURRENT_TENANT_ID_REAL && f.listingId === listingId);
    if (favorite) this.favoriteApi.remove(favorite.id);
  }
}
