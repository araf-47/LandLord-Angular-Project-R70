import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { AREAS_BY_DISTRICT, DISTRICTS, PROPERTY_TYPES } from '../../core/mock-data.service';
import { API_ORIGIN, ListingApiService } from '../../core/listing-api.service';

@Component({
  selector: 'app-browse',
  standalone: true,
  imports: [FormsModule, RouterLink],
  template: `
    <div class="public-content">
      <h1>Browse listings</h1>

      @switch (status()) {
        @case ('loading') {
          <div class="card"><p class="hint-text">Loading listings…</p></div>
        }
        @case ('error') {
          <div class="card">
            <p class="text-danger mb-sm">Couldn't load this page. {{ error() }}</p>
            <button type="button" class="btn btn-sm" (click)="ngOnInit()">Retry</button>
          </div>
        }
        @case ('ready') {
          <div class="search-bar flex-wrap mb-lg">
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

          @if (activeFilters().length > 0) {
            <div class="filter-chips mb-md">
              @for (f of activeFilters(); track f.key) {
                <button type="button" class="filter-chip" (click)="f.clear()">{{ f.label }} &times;</button>
              }
              <button type="button" class="filter-chip filter-chip-clear-all" (click)="clearAllFilters()">Clear all</button>
            </div>
          }

          <div class="listing-grid">
            @for (l of filtered(); track l.id) {
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
              <p class="hint-text">No listings match your search.</p>
            }
          </div>
        }
      }
    </div>
  `,
})
export class BrowseComponent implements OnInit {
  protected readonly api = inject(ListingApiService);
  protected readonly photoOrigin = API_ORIGIN;

  readonly districts = DISTRICTS;
  readonly propertyTypes = PROPERTY_TYPES;

  private readonly initialParams = inject(ActivatedRoute).snapshot.queryParamMap;
  readonly query = signal(this.initialParams.get('q') ?? '');
  readonly district = signal(this.initialParams.get('district') ?? '');
  readonly area = signal(this.initialParams.get('area') ?? '');
  readonly propertyType = signal(this.initialParams.get('propertyType') ?? '');
  readonly sourceFilter = signal('');

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

  onDistrictChange(value: string): void {
    this.district.set(value);
    this.area.set('');
  }

  readonly activeFilters = computed(() => {
    const filters: { key: string; label: string; clear: () => void }[] = [];
    if (this.query()) filters.push({ key: 'query', label: `"${this.query()}"`, clear: () => this.query.set('') });
    if (this.district()) filters.push({ key: 'district', label: this.district(), clear: () => this.onDistrictChange('') });
    if (this.area()) filters.push({ key: 'area', label: this.area(), clear: () => this.area.set('') });
    if (this.propertyType()) {
      const label = this.propertyTypes.find((t) => t.value === this.propertyType())?.label ?? this.propertyType();
      filters.push({ key: 'propertyType', label, clear: () => this.propertyType.set('') });
    }
    if (this.sourceFilter()) {
      const label = this.sourceFilter() === 'owner' ? 'Owner-posted' : 'LandLord-linked';
      filters.push({ key: 'source', label, clear: () => this.sourceFilter.set('') });
    }
    return filters;
  });

  clearAllFilters(): void {
    this.query.set('');
    this.district.set('');
    this.area.set('');
    this.propertyType.set('');
    this.sourceFilter.set('');
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
