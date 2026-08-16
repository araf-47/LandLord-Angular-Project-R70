import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { ApiUnit, UnitApiService } from '../../../core/unit-api.service';

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
    </div>
  `,
})
export class UnitFormComponent implements OnInit {
  private readonly api = inject(UnitApiService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  private readonly propertyId = this.route.snapshot.paramMap.get('propertyId')!;
  private readonly unitId = this.route.snapshot.paramMap.get('unitId');
  readonly editing = !!this.unitId;

  unitNumber = '';
  rent: number | null = null;
  status: ApiUnit['status'] = 'vacant';

  async ngOnInit(): Promise<void> {
    if (this.unitId) {
      const unit = await this.api.get(+this.unitId);
      this.unitNumber = unit.unitNumber;
      this.rent = unit.rent;
      this.status = unit.status;
    }
  }

  async save(): Promise<void> {
    if (!this.unitNumber || this.rent == null) return;

    if (this.editing && this.unitId) {
      await this.api.update(+this.unitId, +this.propertyId, this.unitNumber, this.rent, this.status);
    } else {
      await this.api.create(+this.propertyId, this.unitNumber, this.rent, this.status);
    }

    this.router.navigate(['/landlord/properties', this.propertyId, 'units']);
  }
}
