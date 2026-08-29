import { Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../../core/auth.service';
import { API_ORIGIN, ApiListing, ListingApiService } from '../../core/listing-api.service';
import { FavoriteApiService } from '../../core/favorite-api.service';
import { BookingApiService } from '../../core/booking-api.service';
import { ProfileApiService } from '../../core/profile-api.service';
import { ToastService } from '../../shared/toast.service';

@Component({
  selector: 'app-listing-detail',
  standalone: true,
  template: `
    @if (listing()) {
      <div class="public-content">
        <div class="card">
          @if (listing()!.photoUrl) {
            <img class="listing-card-image listing-hero-image" [src]="photoOrigin + listing()!.photoUrl" alt="" />
          } @else {
            <div class="listing-card-image no-photo listing-hero-image">
              <svg width="36" height="36" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M3 11.5 12 4l9 7.5" /><path d="M5 10v9a1 1 0 0 0 1 1h4v-6h4v6h4a1 1 0 0 0 1-1v-9" /></svg>
              <span>No photo yet</span>
            </div>
          }
          <h1>{{ listing()!.title }}</h1>
          <p>{{ listing()!.address }}</p>
          <span class="badge" [class.badge-owner]="listing()!.source === 'owner'" [class.badge-landlord-linked]="listing()!.source === 'landlord-linked'">
            {{ listing()!.source === 'owner' ? 'Owner-posted' : 'LandLord-linked' }}
          </span>
          <div class="listing-card-rent listing-detail-rent">৳{{ listing()!.rent }}/mo</div>

          @if (!auth.isAuthenticated()) {
            <div class="actions-row">
              <button class="btn" (click)="promptSignup()">Save</button>
              <button class="btn btn-primary" (click)="promptSignup()">Book now</button>
            </div>
            <p class="hint-text mt-sm">Sign up or log in to save or book this listing.</p>
          } @else if (auth.role() === 'tenant') {
            <div class="actions-row">
              <button class="btn" (click)="toggleFavorite()">{{ isFavorite() ? 'Saved ✓' : 'Save' }}</button>
              @if (!alreadyRequested()) {
                <button class="btn btn-primary" [disabled]="booking()" (click)="book()">{{ booking() ? 'Booking…' : 'Book now' }}</button>
              }
            </div>
            @if (alreadyRequested()) {
              <p class="hint-text mt-sm">
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
  protected readonly auth = inject(AuthService);
  protected readonly photoOrigin = API_ORIGIN;
  private readonly listingApi = inject(ListingApiService);
  private readonly favoriteApi = inject(FavoriteApiService);
  private readonly bookingApi = inject(BookingApiService);
  private readonly profileApi = inject(ProfileApiService);
  private readonly router = inject(Router);
  private readonly toast = inject(ToastService);
  private readonly listingId = Number(inject(ActivatedRoute).snapshot.paramMap.get('id'));

  readonly listing = signal<ApiListing | undefined>(undefined);
  readonly booking = signal(false);
  private readonly tenantId = signal<number | null>(null);

  readonly alreadyRequested = computed(() => {
    const id = this.tenantId();
    return id !== null && this.bookingApi.requests().some((r) => r.listingId === this.listingId && r.tenantId === id && r.status !== 'rejected');
  });

  constructor() {
    this.listingApi.get(this.listingId).then((l) => this.listing.set(l));
    if (this.auth.isAuthenticated() && this.auth.role() === 'tenant') {
      this.profileApi.myTenantProfile().then((profile) => {
        this.tenantId.set(profile.id);
        this.favoriteApi.loadForTenant(profile.id);
        this.bookingApi.loadForTenant(profile.id);
      });
    }
  }

  isFavorite(): boolean {
    const id = this.tenantId();
    return id !== null && this.favoriteApi.favorites().some((f) => f.tenantId === id && f.listingId === this.listingId);
  }

  toggleFavorite(): void {
    const id = this.tenantId();
    if (id === null) return;
    const existing = this.favoriteApi.favorites().find((f) => f.tenantId === id && f.listingId === this.listingId);
    if (existing) {
      this.favoriteApi.remove(existing.id);
    } else {
      this.favoriteApi.add(id, this.listingId);
    }
  }

  async book(): Promise<void> {
    const id = this.tenantId();
    if (id === null || this.booking()) return;
    this.booking.set(true);
    try {
      const tenant = await this.profileApi.myTenantProfile();
      await this.bookingApi.create({
        listingId: this.listingId,
        tenantId: id,
        applicantName: tenant.name,
      });
      this.toast.success('Request sent.');
    } catch {
      this.toast.error("Couldn't send your request. Please try again.");
    } finally {
      this.booking.set(false);
    }
  }

  promptSignup(): void {
    this.router.navigateByUrl('/auth/signup');
  }
}
