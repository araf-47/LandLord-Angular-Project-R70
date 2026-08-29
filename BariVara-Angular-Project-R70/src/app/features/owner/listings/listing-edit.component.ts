import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { AREAS_BY_DISTRICT, DISTRICTS, PROPERTY_TYPES, PropertyType } from '../../../core/mock-data.service';
import { ListingApiService } from '../../../core/listing-api.service';

@Component({
  selector: 'app-owner-listing-edit',
  standalone: true,
  imports: [FormsModule],
  template: `
    @switch (status()) {
      @case ('loading') {
        <div class="card"><p class="hint-text">Loading listing…</p></div>
      }
      @case ('error') {
        <div class="card">
          <p class="text-danger mb-sm">Couldn't load this page. {{ error() }}</p>
          <button type="button" class="btn btn-sm" (click)="ngOnInit()">Retry</button>
        </div>
      }
      @case ('ready') {
        <h1>Edit listing</h1>
        <div class="card stack max-w-md">
          <div class="field">
            <label for="title">Title</label>
            <input id="title" name="title" [(ngModel)]="title" />
          </div>
          <div class="field">
            <label for="address">Address</label>
            <input id="address" name="address" [(ngModel)]="address" />
          </div>
          <div class="form-row">
            <div class="field">
              <label for="district">District</label>
              <select id="district" name="district" [(ngModel)]="district">
                @for (d of districts; track d) {
                  <option [value]="d">{{ d }}</option>
                }
              </select>
            </div>
            <div class="field">
              <label for="area">Area</label>
              <select id="area" name="area" [(ngModel)]="area">
                @for (a of areas(); track a) {
                  <option [value]="a">{{ a }}</option>
                }
              </select>
            </div>
          </div>
          <div class="field">
            <label for="propertyType">Property type</label>
            <select id="propertyType" name="propertyType" [(ngModel)]="propertyType">
              @for (t of propertyTypes; track t.value) {
                <option [value]="t.value">{{ t.label }}</option>
              }
            </select>
          </div>
          <div class="field">
            <label for="rent">Rent</label>
            <input id="rent" type="number" name="rent" [(ngModel)]="rent" />
          </div>
          <div class="actions-row">
            <button class="btn btn-primary" (click)="save()">Edit &amp; save</button>
          </div>
        </div>
      }
    }
  `,
})
export class OwnerListingEditComponent implements OnInit {
  private readonly api = inject(ListingApiService);
  private readonly router = inject(Router);
  private readonly listingId = Number(inject(ActivatedRoute).snapshot.paramMap.get('listingId'));

  readonly districts = DISTRICTS;
  readonly propertyTypes = PROPERTY_TYPES;
  readonly status = signal<'loading' | 'error' | 'ready'>('loading');
  readonly error = signal<string | undefined>(undefined);

  title = '';
  address = '';
  district = DISTRICTS[0];
  area = '';
  propertyType: PropertyType = 'apartment';
  rent = 0;

  async ngOnInit(): Promise<void> {
    this.status.set('loading');
    try {
      const listing = await this.api.get(this.listingId);
      this.title = listing.title;
      this.address = listing.address;
      this.district = listing.district;
      this.area = listing.area;
      this.propertyType = listing.propertyType;
      this.rent = listing.rent;
      this.status.set('ready');
    } catch {
      this.error.set('Check your connection and try again.');
      this.status.set('error');
    }
  }

  areas(): string[] {
    return AREAS_BY_DISTRICT[this.district] ?? [];
  }

  async save(): Promise<void> {
    await this.api.update(this.listingId, {
      title: this.title,
      address: this.address,
      district: this.district,
      area: this.area,
      propertyType: this.propertyType,
      rent: this.rent,
    });
    this.router.navigateByUrl('/owner/listings');
  }
}
