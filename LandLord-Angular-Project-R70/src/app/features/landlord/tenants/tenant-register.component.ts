import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { PropertyApiService } from '../../../core/property-api.service';
import { TenantApiService } from '../../../core/tenant-api.service';
import { UnitApiService } from '../../../core/unit-api.service';

@Component({
  selector: 'app-tenant-register',
  standalone: true,
  imports: [FormsModule],
  template: `
    <h1>Register tenant (walk-in)</h1>

    <div class="card stack" style="max-width:640px;">
      <div>
        <h3>1. Tenant info</h3>
        <div class="form-row">
          <div class="field">
            <label for="name">Full name</label>
            <input id="name" name="name" [(ngModel)]="name" required />
          </div>
          <div class="field">
            <label for="phone">Phone</label>
            <input id="phone" name="phone" [(ngModel)]="phone" required />
            <span class="hint-text">This becomes their login username — with or without +880, both work.</span>
          </div>
        </div>
        <div class="form-row">
          <div class="field">
            <label for="email">Email</label>
            <input id="email" type="email" name="email" [(ngModel)]="email" required />
          </div>
          <div class="field">
            <label for="nationalId">National ID (or passport)</label>
            <input id="nationalId" name="nationalId" [(ngModel)]="nationalId" required />
          </div>
        </div>
        <div class="form-row">
          <div class="field">
            <label for="password">Temporary password</label>
            <input id="password" name="password" [(ngModel)]="password" required />
            <span class="hint-text">8-16 chars, needs upper, lower, digit, and a special character. Hand this to the tenant in person — it's their login, along with their phone number.</span>
          </div>
        </div>
        @if (nidError()) {
          <p class="error-text">{{ nidError() }}</p>
        }
      </div>

      <div>
        <h3>2. Assign unit &amp; agreement</h3>
        <div class="field">
          <label for="unit">Vacant unit</label>
          <select id="unit" name="unit" (change)="onUnitChange($event)">
            <option value="">— choose —</option>
            @for (u of vacantUnits(); track u.id) {
              <option [value]="u.id" [selected]="u.id === unitId">{{ propertyName(u.propertyId) }} &gt; {{ u.unitNumber }} &gt; {{ u.rent }}/mo</option>
            }
          </select>
        </div>
        <div class="field">
          <label for="terms">Lease terms</label>
          <input id="terms" name="terms" [(ngModel)]="terms" placeholder="e.g. 12-month lease" />
        </div>
        <div class="field">
          <label for="deposit">Security deposit</label>
          <input id="deposit" type="number" name="deposit" [(ngModel)]="deposit" />
        </div>
      </div>

      <div class="actions-row">
        <button class="btn btn-primary" (click)="save()">Confirm agreement &amp; assign unit</button>
      </div>
    </div>
  `,
})
export class TenantRegisterComponent implements OnInit {
  private readonly propertyApi = inject(PropertyApiService);
  private readonly unitApi = inject(UnitApiService);
  private readonly tenantApi = inject(TenantApiService);
  private readonly router = inject(Router);

  name = '';
  phone = '';
  email = '';
  nationalId = '';
  unitId: number | null = null;
  terms = '';
  deposit: number | null = null;
  password = '';
  readonly nidError = signal('');

  async ngOnInit(): Promise<void> {
    await Promise.all([this.propertyApi.load(), this.unitApi.load()]);
  }

  vacantUnits() {
    return this.unitApi.units().filter((u) => u.status === 'vacant');
  }

  propertyName(propertyId: number): string {
    return this.propertyApi.properties().find((p) => p.id === propertyId)?.name ?? '—';
  }

  onUnitChange(event: Event): void {
    const value = (event.target as HTMLSelectElement).value;
    this.unitId = value ? Number(value) : null;
  }

  async save(): Promise<void> {
    if (!this.name || !this.nationalId) {
      this.nidError.set('Fill in name and National ID.');
      return;
    }
    if (!this.unitId) {
      this.nidError.set('Choose a unit to assign.');
      return;
    }
    if (!this.password) {
      this.nidError.set('Set a temporary password for the tenant to log in with.');
      return;
    }

    this.nidError.set('');
    try {
      await this.tenantApi.register({
        name: this.name,
        phone: this.phone,
        email: this.email,
        nationalId: this.nationalId,
        unitId: this.unitId,
        terms: this.terms,
        deposit: this.deposit ?? 0,
        password: this.password,
      });
    } catch (err: any) {
      if (err?.status === 409) {
        this.nidError.set(err?.error?.message || 'This National ID is already registered to an active tenant.');
        return;
      }
      throw err;
    }

    this.router.navigateByUrl('/landlord/tenants');
  }
}
