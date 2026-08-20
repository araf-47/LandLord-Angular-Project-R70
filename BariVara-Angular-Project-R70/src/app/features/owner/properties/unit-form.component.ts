import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { OwnerUnitApiService } from '../../../core/owner-unit-api.service';

@Component({
  selector: 'app-owner-unit-form',
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
      @if (error) {
        <p class="hint-text" style="color:var(--color-danger, #c0392b);">{{ error }}</p>
      }

      <div class="actions-row">
        <button class="btn btn-primary" (click)="save()">Save unit</button>
      </div>
    </div>
  `,
})
export class OwnerUnitFormComponent implements OnInit {
  private readonly api = inject(OwnerUnitApiService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  private readonly propertyId = this.route.snapshot.paramMap.get('propertyId')!;
  private readonly unitId = this.route.snapshot.paramMap.get('unitId');
  readonly editing = !!this.unitId;

  unitNumber = '';
  rent: number | null = null;
  error = '';

  async ngOnInit(): Promise<void> {
    if (this.unitId) {
      const unit = await this.api.get(+this.unitId);
      this.unitNumber = unit.unitNumber;
      this.rent = unit.rent;
    }
  }

  async save(): Promise<void> {
    if (!this.unitNumber || this.rent == null) {
      this.error = 'Unit number and rent are required.';
      return;
    }

    if (this.editing && this.unitId) {
      await this.api.update(+this.unitId, +this.propertyId, this.unitNumber, this.rent);
    } else {
      await this.api.create({ propertyId: +this.propertyId, unitNumber: this.unitNumber, rent: this.rent });
    }

    this.router.navigate(['/owner/properties', this.propertyId, 'units']);
  }
}
