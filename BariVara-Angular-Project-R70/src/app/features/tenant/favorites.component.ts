import { Component, computed, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { CURRENT_TENANT_ID, MockDataService } from '../../core/mock-data.service';

@Component({
  selector: 'app-tenant-favorites',
  standalone: true,
  imports: [RouterLink],
  template: `
    <h1>Favorite List</h1>
    <div class="listing-grid">
      @for (l of favoriteListings(); track l.id) {
        <div class="listing-card">
          <div class="listing-card-image">No photo</div>
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
  private readonly data = inject(MockDataService);

  readonly favoriteListings = computed(() => {
    const listingIds = new Set(this.data.favoritesForTenant(CURRENT_TENANT_ID).map((f) => f.listingId));
    return this.data.listings().filter((l) => listingIds.has(l.id));
  });

  remove(listingId: string): void {
    this.data.favorites.update((list) => list.filter((f) => !(f.tenantId === CURRENT_TENANT_ID && f.listingId === listingId)));
  }
}
