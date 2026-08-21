import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AREAS_BY_DISTRICT, DISTRICTS, PROPERTY_TYPES, PropertyType } from '../../../core/mock-data.service';
import { OwnerPropertyApiService } from '../../../core/owner-property-api.service';
import { ApiOwnerUnit, OwnerUnitApiService } from '../../../core/owner-unit-api.service';
import { ListingApiService } from '../../../core/listing-api.service';
import { CURRENT_OWNER_ID_REAL } from '../../../core/current-owner';

@Component({
  selector: 'app-owner-listing-form',
  standalone: true,
  imports: [FormsModule],
  template: `
    <h1>Add Post Ad</h1>

    <div class="card stack" style="max-width:520px;">
      @if (step() === 1) {
        <p>Auto-fill from an existing property?</p>
        <div class="form-row">
          @if (myUnits().length) {
            <button class="btn btn-primary" (click)="autoFill = true; step.set(2)">Yes</button>
          }
          <button class="btn" (click)="autoFill = false; step.set(2)">No, enter manually</button>
        </div>
      }

      @if (step() === 2 && autoFill) {
        <p>Select property &amp; unit</p>
        <div class="field">
          <label for="unit">Unit</label>
          <select id="unit" name="unit" [(ngModel)]="selectedUnitId">
            <option value="">— choose —</option>
            @for (u of myUnits(); track u.id) {
              <option [value]="u.id">{{ propertyName(u.propertyId) }} — {{ u.unitNumber }}</option>
            }
          </select>
        </div>
        <div class="actions-row">
          <button class="btn" (click)="step.set(1)">Back</button>
          <button class="btn btn-primary" [disabled]="!selectedUnitId" (click)="continueFromAutoFill()">Continue</button>
        </div>
      }

      @if (step() === 2 && !autoFill) {
        <p>Enter property/unit details manually</p>
        <div class="field">
          <label for="title">Title</label>
          <input id="title" name="title" [(ngModel)]="title" placeholder="e.g. Sunny 2-bed apartment" />
        </div>
        <div class="field">
          <label for="address">Address</label>
          <input id="address" name="address" [(ngModel)]="address" />
        </div>
        <div class="form-row">
          <div class="field">
            <label for="district">District</label>
            <select id="district" name="district" [ngModel]="district()" (ngModelChange)="onDistrictChange($event)">
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
        <div class="actions-row">
          <button class="btn" (click)="step.set(1)">Back</button>
          <button class="btn btn-primary" [disabled]="!title || !address" (click)="step.set(3)">Continue</button>
        </div>
      }

      @if (step() === 3) {
        <p>Enter rent, review</p>
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
        <div class="card">
          <strong>{{ title }}</strong>
          <p>{{ address }} — ৳{{ rent }}/mo</p>
        </div>
        <div class="actions-row">
          <button class="btn" (click)="step.set(2)">Back</button>
          <button class="btn btn-primary" [disabled]="!rent" (click)="publish()">Publish ad</button>
        </div>
      }
    </div>
  `,
})
export class OwnerListingFormComponent {
  private readonly propertyApi = inject(OwnerPropertyApiService);
  private readonly unitApi = inject(OwnerUnitApiService);
  private readonly listingApi = inject(ListingApiService);
  private readonly router = inject(Router);

  readonly step = signal(1);
  readonly districts = DISTRICTS;
  readonly propertyTypes = PROPERTY_TYPES;
  readonly district = signal(DISTRICTS[0]);
  readonly areas = () => AREAS_BY_DISTRICT[this.district()] ?? [];
  area = this.areas()[0] ?? '';

  autoFill = false;
  selectedUnitId = '';
  title = '';
  address = '';
  propertyType: PropertyType = 'apartment';
  rent: number | null = null;
  photoUrl: string | null = null;

  constructor() {
    this.propertyApi.loadForOwner(CURRENT_OWNER_ID_REAL).then((properties) => {
      this.unitApi.loadForProperties(properties.map((p) => p.id));
    });
  }

  onDistrictChange(value: string): void {
    this.district.set(value);
    this.area = this.areas()[0] ?? '';
  }

  myUnits(): ApiOwnerUnit[] {
    return this.unitApi.units();
  }

  propertyName(propertyId: number): string {
    return this.propertyApi.properties().find((p) => p.id === propertyId)?.name ?? '—';
  }

  continueFromAutoFill(): void {
    const unit = this.myUnits().find((u) => String(u.id) === this.selectedUnitId);
    if (!unit) return;
    const property = this.propertyApi.properties().find((p) => p.id === unit.propertyId);
    this.title = `${property?.name ?? ''} — ${unit.unitNumber}`;
    this.address = property?.address ?? '';
    this.district.set(property?.district ?? DISTRICTS[0]);
    this.area = property?.area ?? this.areas()[0] ?? '';
    this.rent = unit.rent;
    this.photoUrl = unit.photoUrl;
    this.step.set(3);
  }

  async publish(): Promise<void> {
    if (!this.title || !this.address || !this.rent) return;
    await this.listingApi.create({
      ownerId: CURRENT_OWNER_ID_REAL,
      unitId: this.autoFill ? Number(this.selectedUnitId) : null,
      title: this.title,
      address: this.address,
      district: this.district(),
      area: this.area,
      propertyType: this.propertyType,
      rent: this.rent!,
      photoUrl: this.photoUrl,
    });
    this.router.navigateByUrl('/owner/listings');
  }
}
