import { Component, OnInit, computed, inject, signal } from '@angular/core';
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

        <div class="search-bar flex-wrap">
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
      @switch (status()) {
        @case ('loading') {
          <div class="card"><p class="hint-text">Loading recent listings…</p></div>
        }
        @case ('error') {
          <div class="card">
            <p class="text-danger mb-sm">Couldn't load this page. {{ error() }}</p>
            <button type="button" class="btn btn-sm" (click)="ngOnInit()">Retry</button>
          </div>
        }
        @case ('ready') {
          <div class="listing-grid">
            @for (l of recentListings(); track l.id) {
              <a class="listing-card" [routerLink]="['/listings', l.id]">
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
        }
      }
    </div>
  `,
})
export class HomepageComponent implements OnInit {
  private readonly router = inject(Router);
  private readonly api = inject(ListingApiService);
  protected readonly photoOrigin = API_ORIGIN;

  readonly districts = DISTRICTS;
  readonly propertyTypes = PROPERTY_TYPES;
  readonly district = signal('');
  readonly area = signal('');
  readonly propertyType = signal('');

  readonly areas = computed(() => (this.district() ? AREAS_BY_DISTRICT[this.district()] ?? [] : []));

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
