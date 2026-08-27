import { Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../../core/auth.service';
import { API_ORIGIN, ApiListing, ListingApiService } from '../../core/listing-api.service';
import { FavoriteApiService } from '../../core/favorite-api.service';
import { BookingApiService } from '../../core/booking-api.service';
import { ProfileApiService } from '../../core/profile-api.service';

@Component({
  selector: 'app-listing-detail',
  standalone: true,
  template: `
    @if (listing()) {
      <div class="public-content">
        <div class="card">
          @if (listing()!.photoUrl) {
            <img class="listing-card-image" [src]="photoOrigin + listing()!.photoUrl" alt="" style="height:220px; width:100%; border-radius:var(--radius); margin-bottom:1rem; object-fit:cover;" />
          } @else {
            <div class="listing-card-image" style="height:220px; border-radius:var(--radius); margin-bottom:1rem;">No photo</div>
          }
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
              @if (!alreadyRequested()) {
                <button class="btn btn-primary" (click)="book()">Book now</button>
              }
            </div>
            @if (alreadyRequested()) {
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
  protected readonly auth = inject(AuthService);
  protected readonly photoOrigin = API_ORIGIN;
  private readonly listingApi = inject(ListingApiService);
  private readonly favoriteApi = inject(FavoriteApiService);
  private readonly bookingApi = inject(BookingApiService);
  private readonly profileApi = inject(ProfileApiService);
  private readonly router = inject(Router);
  private readonly listingId = Number(inject(ActivatedRoute).snapshot.paramMap.get('id'));

  readonly listing = signal<ApiListing | undefined>(undefined);
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
    if (id === null) return;
    const tenant = await this.profileApi.myTenantProfile();
    await this.bookingApi.create({
      listingId: this.listingId,
      tenantId: id,
      applicantName: tenant.name,
    });
  }

  promptSignup(): void {
    this.router.navigateByUrl('/auth/signup');
  }
}
