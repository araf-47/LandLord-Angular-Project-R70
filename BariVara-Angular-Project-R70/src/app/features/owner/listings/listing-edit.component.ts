import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { AREAS_BY_DISTRICT, DISTRICTS, MockDataService, PROPERTY_TYPES, PropertyType } from '../../../core/mock-data.service';

@Component({
  selector: 'app-owner-listing-edit',
  standalone: true,
  imports: [FormsModule],
  template: `
    @if (listing) {
      <h1>Edit listing</h1>
      <div class="card stack" style="max-width:520px;">
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
  `,
})
export class OwnerListingEditComponent {
  private readonly data = inject(MockDataService);
  private readonly router = inject(Router);
  private readonly listingId = inject(ActivatedRoute).snapshot.paramMap.get('listingId')!;

  readonly listing = this.data.listingById(this.listingId);
  readonly districts = DISTRICTS;
  readonly propertyTypes = PROPERTY_TYPES;

  title = this.listing?.title ?? '';
  address = this.listing?.address ?? '';
  district = this.listing?.district ?? DISTRICTS[0];
  area = this.listing?.area ?? '';
  propertyType: PropertyType = this.listing?.propertyType ?? 'apartment';
  rent = this.listing?.rent ?? 0;

  areas(): string[] {
    return AREAS_BY_DISTRICT[this.district] ?? [];
  }

  save(): void {
    this.data.listings.update((list) =>
      list.map((l) =>
        l.id === this.listingId
          ? { ...l, title: this.title, address: this.address, district: this.district, area: this.area, propertyType: this.propertyType, rent: this.rent }
          : l
      )
    );
    this.router.navigateByUrl('/owner/listings');
  }
}
