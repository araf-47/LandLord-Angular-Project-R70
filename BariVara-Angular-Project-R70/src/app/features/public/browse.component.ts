import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { AREAS_BY_DISTRICT, DISTRICTS, PROPERTY_TYPES } from '../../core/mock-data.service';
import { ListingApiService } from '../../core/listing-api.service';

@Component({
  selector: 'app-browse',
  standalone: true,
  imports: [FormsModule, RouterLink],
  template: `
    <div class="public-content">
      <h1>Browse listings</h1>

      <div class="search-bar" style="flex-wrap:wrap; margin-bottom:1.5rem;">
        <input [ngModel]="query()" (ngModelChange)="query.set($event)" name="query" placeholder="Search by title" />
        <select [ngModel]="district()" (ngModelChange)="onDistrictChange($event)" name="district">
          <option value="">All districts</option>
          @for (d of districts; track d) {
            <option [value]="d">{{ d }}</option>
          }
        </select>
        <select [ngModel]="area()" (ngModelChange)="area.set($event)" name="area">
          <option value="">All areas</option>
          @for (a of areas(); track a) {
            <option [value]="a">{{ a }}</option>
          }
        </select>
        <select [ngModel]="propertyType()" (ngModelChange)="propertyType.set($event)" name="propertyType">
          <option value="">All property types</option>
          @for (t of propertyTypes; track t.value) {
            <option [value]="t.value">{{ t.label }}</option>
          }
        </select>
        <select [ngModel]="sourceFilter()" (ngModelChange)="sourceFilter.set($event)" name="source">
          <option value="">All sources</option>
          <option value="owner">Owner-posted</option>
          <option value="landlord-linked">LandLord-linked</option>
        </select>
      </div>

      <div class="listing-grid">
        @for (l of filtered(); track l.id) {
          <a class="listing-card" [routerLink]="['/listings', l.id]">
            <div class="listing-card-image">No photo</div>
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
          <p class="hint-text">No listings match your search.</p>
        }
      </div>
    </div>
  `,
})
export class BrowseComponent {
  protected readonly api = inject(ListingApiService);

  readonly districts = DISTRICTS;
  readonly propertyTypes = PROPERTY_TYPES;

  private readonly initialParams = inject(ActivatedRoute).snapshot.queryParamMap;
  readonly query = signal(this.initialParams.get('q') ?? '');
  readonly district = signal(this.initialParams.get('district') ?? '');
  readonly area = signal(this.initialParams.get('area') ?? '');
  readonly propertyType = signal(this.initialParams.get('propertyType') ?? '');
  readonly sourceFilter = signal('');

  readonly areas = computed(() => (this.district() ? AREAS_BY_DISTRICT[this.district()] ?? [] : []));

  constructor() {
    this.api.search();
  }

  onDistrictChange(value: string): void {
    this.district.set(value);
    this.area.set('');
  }

  readonly filtered = computed(() => {
    const q = this.query().trim().toLowerCase();
    return this.api.listings().filter((l) => {
      const matchesQuery = !q || l.title.toLowerCase().includes(q) || l.address.toLowerCase().includes(q);
      const matchesDistrict = !this.district() || l.district === this.district();
      const matchesArea = !this.area() || l.area === this.area();
      const matchesType = !this.propertyType() || l.propertyType === this.propertyType();
      const matchesSource = !this.sourceFilter() || l.source === this.sourceFilter();
      return matchesQuery && matchesDistrict && matchesArea && matchesType && matchesSource && l.status === 'active';
    });
  });
}
