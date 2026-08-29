import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { API_ORIGIN, OwnerUnitApiService } from '../../../core/owner-unit-api.service';

@Component({
  selector: 'app-owner-unit-form',
  standalone: true,
  imports: [FormsModule],
  template: `
    @switch (status()) {
      @case ('loading') {
        <div class="card"><p class="hint-text">Loading unit…</p></div>
      }
      @case ('error') {
        <div class="card">
          <p class="text-danger mb-sm">Couldn't load this page. {{ loadError() }}</p>
          <button type="button" class="btn btn-sm" (click)="ngOnInit()">Retry</button>
        </div>
      }
      @case ('ready') {
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
            <label for="photo">Photo (optional)</label>
            @if (existingPhotoUrl) {
              <img [src]="photoOrigin + existingPhotoUrl" alt="Unit photo" style="max-width:200px;display:block;margin-bottom:0.5rem;" />
            }
            <input id="photo" type="file" name="photo" accept="image/*" (change)="onFileSelected($event)" />
            @if (fileName) {
              <span class="hint-text">{{ fileName }}</span>
            }
          </div>
          @if (error) {
            <p class="hint-text" style="color:var(--color-danger, #c0392b);">{{ error }}</p>
          }

          <div class="actions-row">
            <button class="btn btn-primary" (click)="save()">Save unit</button>
          </div>
        </div>
      }
    }
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
  existingPhotoUrl: string | null = null;
  fileName = '';
  private selectedFile: File | null = null;
  readonly photoOrigin = API_ORIGIN;
  error = '';

  readonly status = signal<'loading' | 'error' | 'ready'>('loading');
  readonly loadError = signal<string | undefined>(undefined);

  async ngOnInit(): Promise<void> {
    if (!this.unitId) {
      this.status.set('ready');
      return;
    }
    this.status.set('loading');
    try {
      const unit = await this.api.get(+this.unitId);
      this.unitNumber = unit.unitNumber;
      this.rent = unit.rent;
      this.existingPhotoUrl = unit.photoUrl;
      this.status.set('ready');
    } catch {
      this.loadError.set('Check your connection and try again.');
      this.status.set('error');
    }
  }

  onFileSelected(event: Event): void {
    const file = (event.target as HTMLInputElement).files?.[0] ?? null;
    this.selectedFile = file;
    this.fileName = file?.name ?? '';
  }

  async save(): Promise<void> {
    if (!this.unitNumber || this.rent == null) {
      this.error = 'Unit number and rent are required.';
      return;
    }

    let unitId: number;
    if (this.editing && this.unitId) {
      unitId = +this.unitId;
      await this.api.update(unitId, +this.propertyId, this.unitNumber, this.rent);
    } else {
      const created = await this.api.create({ propertyId: +this.propertyId, unitNumber: this.unitNumber, rent: this.rent });
      unitId = created.id;
    }

    if (this.selectedFile) {
      await this.api.uploadPhoto(unitId, this.selectedFile);
    }

    this.router.navigate(['/owner/properties', this.propertyId, 'units']);
  }
}
