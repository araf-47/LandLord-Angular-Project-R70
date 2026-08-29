import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { API_ORIGIN } from '../../../core/maintenance-api.service';
import { ApiUnit, UnitApiService } from '../../../core/unit-api.service';

@Component({
  selector: 'app-unit-form',
  standalone: true,
  imports: [FormsModule],
  template: `
    <h1>{{ editing ? 'Edit unit' : 'Add unit' }}</h1>

    @switch (pageStatus()) {
      @case ('loading') {
        <div class="card"><p class="hint-text">Loading unit…</p></div>
      }
      @case ('error') {
        <div class="card">
          <p class="text-danger mb-sm">Couldn't load this page. {{ pageError() }}</p>
          <button type="button" class="btn btn-sm" (click)="ngOnInit()">Retry</button>
        </div>
      }
      @case ('ready') {
        <div class="card max-w-md">
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
          <div class="field">
            <label for="photo">Photo (optional)</label>
            @if (existingPhotoUrl) {
              <img [src]="photoOrigin + existingPhotoUrl" alt="Unit photo" class="img-preview" />
            }
            <input id="photo" type="file" name="photo" accept="image/*" (change)="onFileSelected($event)" />
            @if (fileName) {
              <span class="hint-text">{{ fileName }}</span>
            }
          </div>

          <div class="actions-row">
            <button class="btn btn-primary" (click)="save()">Save unit</button>
          </div>
        </div>
      }
    }
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
  existingPhotoUrl: string | null = null;
  fileName = '';
  private selectedFile: File | null = null;
  readonly photoOrigin = API_ORIGIN;

  readonly pageStatus = signal<'loading' | 'error' | 'ready'>('loading');
  readonly pageError = signal<string | undefined>(undefined);

  async ngOnInit(): Promise<void> {
    this.pageStatus.set('loading');
    try {
      if (this.unitId) {
        const unit = await this.api.get(+this.unitId);
        this.unitNumber = unit.unitNumber;
        this.rent = unit.rent;
        this.status = unit.status;
        this.existingPhotoUrl = unit.photoUrl;
      }
      this.pageStatus.set('ready');
    } catch {
      this.pageError.set('Check your connection and try again.');
      this.pageStatus.set('error');
    }
  }

  onFileSelected(event: Event): void {
    const file = (event.target as HTMLInputElement).files?.[0] ?? null;
    this.selectedFile = file;
    this.fileName = file?.name ?? '';
  }

  async save(): Promise<void> {
    if (!this.unitNumber || this.rent == null) return;

    let unitId: number;
    if (this.editing && this.unitId) {
      unitId = +this.unitId;
      await this.api.update(unitId, +this.propertyId, this.unitNumber, this.rent, this.status);
    } else {
      const created = await this.api.create(+this.propertyId, this.unitNumber, this.rent, this.status);
      unitId = created.id;
    }

    if (this.selectedFile) {
      await this.api.uploadPhoto(unitId, this.selectedFile);
    }

    this.router.navigate(['/landlord/properties', this.propertyId, 'units']);
  }
}
