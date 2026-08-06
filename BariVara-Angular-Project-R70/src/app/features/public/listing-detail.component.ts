import { Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../../core/auth.service';
import { CURRENT_TENANT_ID, MockDataService, nextId } from '../../core/mock-data.service';

@Component({
  selector: 'app-listing-detail',
  standalone: true,
  template: `
    @if (listing()) {
      <div class="public-content">
        <div class="card">
          <div class="listing-card-image" style="height:220px; border-radius:var(--radius); margin-bottom:1rem;">No photo</div>
          <h1>{{ listing()!.title }}</h1>
          <p>{{ listing()!.address }}</p>
          <span class="badge" [class.badge-owner]="listing()!.source === 'owner'" [class.badge-landlord-linked]="listing()!.source === 'landlord-linked'">
            {{ listing()!.source === 'owner' ? 'Owner-posted' : 'LandLord-linked' }}
          </span>
          <div class="listing-card-rent" style="font-size:1.4rem; margin-top:0.75rem;">৳{{ listing()!.rent }}/mo</div>

          @if (!auth.isAuthenticated()) {
            <div class="actions-row">
              <button class="btn" (click)="promptSignup()">Save</button>
              <button class="btn btn-primary" (click)="promptSignup()">Book now</button>
            </div>
            <p class="hint-text" style="margin-top:0.5rem;">Sign up or log in to save or book this listing.</p>
          } @else if (auth.role() === 'tenant') {
            <div class="actions-row">
              <button class="btn" (click)="toggleFavorite()">{{ isFavorite() ? 'Saved ✓' : 'Save' }}</button>
              @if (!booked()) {
                <button class="btn btn-primary" (click)="book()">Book now</button>
              }
            </div>
            @if (booked()) {
              <p class="hint-text" style="margin-top:0.75rem;">
                Request submitted —
                {{ listing()!.source === 'owner' ? "sent to the owner's request inbox." : 'synced to LandLord core Marketplace & Leads.' }}
              </p>
            }
          }
        </div>
      </div>
    } @else {
      <div class="public-content"><p class="hint-text">Listing not found.</p></div>
    }
  `,
})
export class ListingDetailComponent {
  protected readonly data = inject(MockDataService);
  protected readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly listingId = inject(ActivatedRoute).snapshot.paramMap.get('id')!;

  readonly booked = signal(false);
  readonly listing = computed(() => this.data.listingById(this.listingId));

  isFavorite(): boolean {
    return this.data.favorites().some((f) => f.tenantId === CURRENT_TENANT_ID && f.listingId === this.listingId);
  }

  toggleFavorite(): void {
    if (this.isFavorite()) {
      this.data.favorites.update((list) => list.filter((f) => !(f.tenantId === CURRENT_TENANT_ID && f.listingId === this.listingId)));
    } else {
      this.data.favorites.update((list) => [...list, { id: nextId('fav'), tenantId: CURRENT_TENANT_ID, listingId: this.listingId }]);
    }
  }

  book(): void {
    this.data.bookingRequests.update((list) => [
      ...list,
      {
        id: nextId('br'),
        listingId: this.listingId,
        tenantId: CURRENT_TENANT_ID,
        applicantName: this.data.tenantProfile().name,
        status: 'pending',
      },
    ]);
    this.booked.set(true);
  }

  promptSignup(): void {
    this.router.navigateByUrl('/auth/signup');
  }
}
