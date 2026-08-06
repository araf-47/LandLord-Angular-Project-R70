import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { MockDataService, nextId } from '../../../core/mock-data.service';

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
          </div>
        </div>
        <div class="field">
          <label for="email">Email</label>
          <input id="email" type="email" name="email" [(ngModel)]="email" required />
        </div>
      </div>

      <div>
        <h3>2. Assign unit &amp; agreement</h3>
        <div class="field">
          <label for="unit">Vacant unit</label>
          <select id="unit" name="unit" [(ngModel)]="unitId">
            @for (u of vacantUnits(); track u.id) {
              <option [value]="u.id">{{ u.unitNumber }} — {{ u.rent }}/mo</option>
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
export class TenantRegisterComponent {
  private readonly data = inject(MockDataService);
  private readonly router = inject(Router);

  name = '';
  phone = '';
  email = '';
  unitId = '';
  terms = '';
  deposit: number | null = null;

  vacantUnits() {
    return this.data.units().filter((u) => u.status === 'vacant');
  }

  save(): void {
    if (!this.name || !this.unitId) return;

    const tenantId = nextId('t');
    this.data.tenants.update((list) => [
      ...list,
      { id: tenantId, name: this.name, phone: this.phone, email: this.email, unitId: this.unitId, status: 'active' },
    ]);
    this.data.agreements.update((list) => [
      ...list,
      {
        id: nextId('a'),
        tenantId,
        unitId: this.unitId,
        startDate: new Date().toISOString().slice(0, 10),
        terms: this.terms || 'Standard lease',
        deposit: this.deposit ?? 0,
      },
    ]);
    this.data.units.update((list) => list.map((u) => (u.id === this.unitId ? { ...u, status: 'occupied' } : u)));

    this.router.navigateByUrl('/landlord/tenants');
  }
}
