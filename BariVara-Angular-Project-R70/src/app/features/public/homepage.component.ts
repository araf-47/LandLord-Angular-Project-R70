import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AREAS_BY_DISTRICT, DISTRICTS, PROPERTY_TYPES } from '../../core/mock-data.service';
import { API_ORIGIN, ListingApiService } from '../../core/listing-api.service';

@Component({
  selector: 'app-homepage',
  standalone: true,
  imports: [FormsModule, RouterLink],
  template: `
    <div class="public-content">
      <div class="hero">
        <h1>Now search the property very easily</h1>
        <p>Save your valuable time — browse verified rentals across the city.</p>

        <div class="search-bar" style="flex-wrap:wrap;">
          <select [ngModel]="district()" (ngModelChange)="onDistrictChange($event)" name="district">
            <option value="">Select District</option>
            @for (d of districts; track d) {
              <option [value]="d">{{ d }}</option>
            }
          </select>
          <select [ngModel]="area()" (ngModelChange)="area.set($event)" name="area">
            <option value="">Select Area Name</option>
            @for (a of areas(); track a) {
              <option [value]="a">{{ a }}</option>
            }
          </select>
          <select [ngModel]="propertyType()" (ngModelChange)="propertyType.set($event)" name="propertyType">
            <option value="">Select property type</option>
            @for (t of propertyTypes; track t.value) {
              <option [value]="t.value">{{ t.label }}</option>
            }
          </select>
          <button class="btn btn-primary" (click)="search()">Find</button>
        </div>
      </div>

      <h2>Recent listings</h2>
      <div class="listing-grid">
        @for (l of recentListings(); track l.id) {
          <a class="listing-card" [routerLink]="['/listings', l.id]">
            @if (l.photoUrl) {
              <img class="listing-card-image" [src]="photoOrigin + l.photoUrl" alt="" style="width:100%;object-fit:cover;" />
            } @else {
              <div class="listing-card-image">No photo</div>
            }
            <div class="listing-card-body">
              <div class="listing-card-title">{{ l.title }}</div>
              <p>{{ l.area }}, {{ l.district }}</p>
              <span class="badge" [class.badge-owner]="l.source === 'owner'" [class.badge-landlord-linked]="l.source === 'landlord-linked'">
                {{ l.source === 'owner' ? 'Owner-posted' : 'LandLord-linked' }}
              </span>
              <div class="listing-card-rent">৳{{ l.rent }}/mo</div>
            </div>
          </a>
        } @empty {
          <p class="hint-text">No listings yet.</p>
        }
      </div>
    </div>
  `,
})
export class HomepageComponent {
  private readonly router = inject(Router);
  private readonly api = inject(ListingApiService);
  protected readonly photoOrigin = API_ORIGIN;

  readonly districts = DISTRICTS;
  readonly propertyTypes = PROPERTY_TYPES;
  readonly district = signal('');
  readonly area = signal('');
  readonly propertyType = signal('');

  readonly areas = computed(() => (this.district() ? AREAS_BY_DISTRICT[this.district()] ?? [] : []));

  constructor() {
    this.api.search();
  }

  readonly recentListings = computed(() => this.api.listings().filter((l) => l.status === 'active').slice(0, 8));

  onDistrictChange(value: string): void {
    this.district.set(value);
    this.area.set('');
  }

  search(): void {
    const queryParams: Record<string, string> = {};
    if (this.district()) queryParams['district'] = this.district();
    if (this.area()) queryParams['area'] = this.area();
    if (this.propertyType()) queryParams['propertyType'] = this.propertyType();
    this.router.navigate(['/browse'], { queryParams });
  }
}
