import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { MockDataService, PropertyType, Unit, nextId } from '../../../core/mock-data.service';

@Component({
  selector: 'app-unit-form',
  standalone: true,
  imports: [FormsModule],
  template: `
    <h1>{{ editing ? 'Edit unit' : 'Add unit' }}</h1>
    <div class="card" style="max-width:520px;">
      <div class="field">
        <label for="unitNumber">Unit number</label>
        <input id="unitNumber" name="unitNumber" [(ngModel)]="unitNumber" required />
      </div>
      <div class="field">
        <label for="propertyType">Property type</label>
        <select id="propertyType" name="propertyType" [(ngModel)]="propertyType">
          <option value="apartment">Flat / Apartment</option>
          <option value="room">Room / Sublet</option>
          <option value="office">Office Space</option>
        </select>
      </div>
      <div class="field">
        <label for="rent">Rent</label>
        <input id="rent" type="number" name="rent" [(ngModel)]="rent" required />
      </div>
      <div class="field">
        <label for="status">Status</label>
        <select id="status" name="status" [(ngModel)]="status">
          <option value="vacant">Vacant</option>
          <option value="occupied">Occupied</option>
        </select>
      </div>
      <div class="actions-row">
        <button class="btn btn-primary" (click)="save()">Save unit</button>
      </div>
      @if (posted()) {
        <p class="hint-text" style="margin-top:0.75rem;">Vacant unit — ad auto-posted to BariVara.com.</p>
      }
    </div>
  `,
})
export class UnitFormComponent {
  private readonly data = inject(MockDataService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  private readonly propertyId = this.route.snapshot.paramMap.get('propertyId')!;
  private readonly unitId = this.route.snapshot.paramMap.get('unitId');
  readonly editing = !!this.unitId;
  readonly posted = signal(false);

  unitNumber = '';
  propertyType: PropertyType = 'apartment';
  rent: number | null = null;
  status: Unit['status'] = 'vacant';

  constructor() {
    if (this.unitId) {
      const unit = this.data.units().find((u) => u.id === this.unitId);
      if (unit) {
        this.unitNumber = unit.unitNumber;
        this.propertyType = unit.propertyType;
        this.rent = unit.rent;
        this.status = unit.status;
      }
    }
  }

  save(): void {
    if (!this.unitNumber || this.rent == null) return;

    if (this.editing) {
      this.data.units.update((list) =>
        list.map((u) =>
          u.id === this.unitId
            ? { ...u, unitNumber: this.unitNumber, propertyType: this.propertyType, rent: this.rent!, status: this.status }
            : u
        )
      );
    } else {
      this.data.units.update((list) => [
        ...list,
        { id: nextId('u'), propertyId: this.propertyId, unitNumber: this.unitNumber, propertyType: this.propertyType, rent: this.rent!, status: this.status },
      ]);
    }

    if (this.status === 'vacant') {
      this.posted.set(true);
      setTimeout(() => this.router.navigate(['/landlord/properties', this.propertyId, 'units']), 800);
    } else {
      this.router.navigate(['/landlord/properties', this.propertyId, 'units']);
    }
  }
}
