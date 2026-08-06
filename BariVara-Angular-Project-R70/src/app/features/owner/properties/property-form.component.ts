import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AREAS_BY_DISTRICT, CURRENT_OWNER_ID, DISTRICTS, MockDataService, nextId } from '../../../core/mock-data.service';

@Component({
  selector: 'app-owner-property-form',
  standalone: true,
  imports: [FormsModule],
  template: `
    <h1>Add property</h1>
    <div class="card stack" style="max-width:520px;">
      <div>
        <h3>Property info</h3>
        <div class="field">
          <label for="name">Property name</label>
          <input id="name" name="name" [(ngModel)]="name" required />
        </div>
        <div class="field">
          <label for="address">Address</label>
          <input id="address" name="address" [(ngModel)]="address" required />
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
      </div>

      <div>
        <h3>Unit info &amp; images</h3>
        <div class="field">
          <label for="unitNumber">Unit number</label>
          <input id="unitNumber" name="unitNumber" [(ngModel)]="unitNumber" required />
        </div>
        <div class="field">
          <label for="rent">Rent</label>
          <input id="rent" type="number" name="rent" [(ngModel)]="rent" required />
        </div>
        <div class="field">
          <label for="images">Images</label>
          <input id="images" type="file" multiple (change)="imageCount = $any($event.target).files?.length ?? 0" />
          @if (imageCount) {
            <span class="hint-text">{{ imageCount }} file(s) selected</span>
          }
        </div>
      </div>

      <div class="actions-row">
        <button class="btn btn-primary" (click)="save()">Save property</button>
      </div>
    </div>
  `,
})
export class OwnerPropertyFormComponent {
  private readonly data = inject(MockDataService);
  private readonly router = inject(Router);

  readonly districts = DISTRICTS;
  readonly district = signal(DISTRICTS[0]);
  readonly areas = () => AREAS_BY_DISTRICT[this.district()] ?? [];
  area = this.areas()[0] ?? '';

  onDistrictChange(value: string): void {
    this.district.set(value);
    this.area = this.areas()[0] ?? '';
  }

  name = '';
  address = '';
  unitNumber = '';
  rent: number | null = null;
  imageCount = 0;

  save(): void {
    if (!this.name || !this.address || !this.unitNumber || this.rent == null) return;

    const propertyId = nextId('op');
    this.data.ownerProperties.update((list) => [
      ...list,
      { id: propertyId, ownerId: CURRENT_OWNER_ID, name: this.name, address: this.address, district: this.district(), area: this.area },
    ]);
    this.data.ownerUnits.update((list) => [...list, { id: nextId('ou'), propertyId, unitNumber: this.unitNumber, rent: this.rent! }]);

    this.router.navigateByUrl('/owner/properties');
  }
}
