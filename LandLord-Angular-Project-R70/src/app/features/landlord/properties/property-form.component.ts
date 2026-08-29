import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { AREAS_BY_DISTRICT, DISTRICTS, PROPERTY_TYPES, PropertyType } from '../../../core/mock-data.service';
import { PropertyApiService } from '../../../core/property-api.service';

@Component({
  selector: 'app-property-form',
  standalone: true,
  imports: [FormsModule],
  template: `
    <h1>{{ editing ? 'Edit property' : 'Add property' }}</h1>

    @switch (status()) {
      @case ('loading') {
        <div class="card"><p class="hint-text">Loading property…</p></div>
      }
      @case ('error') {
        <div class="card">
          <p class="text-danger mb-sm">Couldn't load this page. {{ error() }}</p>
          <button type="button" class="btn btn-sm" (click)="ngOnInit()">Retry</button>
        </div>
      }
      @case ('ready') {
        <div class="card max-w-md">
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
          <div class="field">
            <label for="propertyType">Property type</label>
            <select id="propertyType" name="propertyType" [(ngModel)]="propertyType">
              @for (t of propertyTypes; track t.value) {
                <option [value]="t.value">{{ t.label }}</option>
              }
            </select>
          </div>
          <p class="hint-text">
            District/area/property type are what let this property's vacant units
            auto-post to BariVara.com — leave them set so that keeps working.
          </p>
          <div class="actions-row">
            <button class="btn btn-primary" (click)="save()">Validate & save</button>
          </div>
        </div>
      }
    }
  `,
})
export class PropertyFormComponent implements OnInit {
  private readonly api = inject(PropertyApiService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  private readonly propertyId = this.route.snapshot.paramMap.get('propertyId');
  readonly editing = !!this.propertyId;

  name = '';
  address = '';
  readonly districts = DISTRICTS;
  readonly propertyTypes = PROPERTY_TYPES;
  readonly district = signal(DISTRICTS[0]);
  readonly areas = computed(() => AREAS_BY_DISTRICT[this.district()] ?? []);
  area = this.areas()[0] ?? '';
  propertyType: PropertyType = 'apartment';

  readonly status = signal<'loading' | 'error' | 'ready'>('loading');
  readonly error = signal<string | undefined>(undefined);

  async ngOnInit(): Promise<void> {
    this.status.set('loading');
    try {
      if (this.propertyId) {
        const property = await this.api.get(+this.propertyId);
        this.name = property.name;
        this.address = property.address;
        if (property.district) this.district.set(property.district);
        if (property.area) this.area = property.area;
        if (property.propertyType) this.propertyType = property.propertyType;
      }
      this.status.set('ready');
    } catch {
      this.error.set('Check your connection and try again.');
      this.status.set('error');
    }
  }

  onDistrictChange(value: string): void {
    this.district.set(value);
    this.area = this.areas()[0] ?? '';
  }

  async save(): Promise<void> {
    if (!this.name || !this.address) return;
    if (this.editing && this.propertyId) {
      await this.api.update(+this.propertyId, this.name, this.address, this.district(), this.area, this.propertyType);
    } else {
      await this.api.create(this.name, this.address, this.district(), this.area, this.propertyType);
    }
    this.router.navigateByUrl('/landlord/properties');
  }
}
