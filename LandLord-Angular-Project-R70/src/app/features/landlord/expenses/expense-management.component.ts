import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MaintenanceApiService } from '../../../core/maintenance-api.service';
import { PropertyApiService } from '../../../core/property-api.service';
import { TenantApiService } from '../../../core/tenant-api.service';

@Component({
  selector: 'app-expense-management',
  standalone: true,
  imports: [FormsModule],
  template: `
    <h1>Expenses</h1>

    <div class="card" style="max-width:520px;">
      <h3>Log expense</h3>
      <div class="field">
        <label for="property">Property</label>
        <select id="property" name="property" (change)="onPropertyChange($event)">
          @for (p of propertyApi.properties(); track p.id) {
            <option [value]="p.id" [selected]="p.id === propertyId">{{ p.name }}</option>
          }
        </select>
      </div>
      <div class="field">
        <label for="bearer">Who bears this?</label>
        <select id="bearer" name="bearer" [(ngModel)]="bearer">
          <option value="landlord">Landlord</option>
          <option value="tenant">Tenant</option>
        </select>
      </div>
      @if (bearer === 'tenant') {
        <div class="field">
          <label for="tenant">Tenant</label>
          <select id="tenant" name="tenant" (change)="onTenantChange($event)">
            <option value="">— choose —</option>
            @for (t of tenantApi.tenants(); track t.id) {
              <option [value]="t.id" [selected]="t.id === tenantId">{{ t.name }}</option>
            }
          </select>
        </div>
      }
      <div class="field">
        <label for="amount">Amount</label>
        <input id="amount" type="number" name="amount" [(ngModel)]="amount" />
      </div>
      <div class="field">
        <label for="category">Category</label>
        <input id="category" name="category" [(ngModel)]="category" placeholder="e.g. Repairs, Utilities" />
      </div>
      <div class="field">
        <label for="description">Description</label>
        <input id="description" name="description" [(ngModel)]="description" />
      </div>
      <div class="actions-row">
        <button class="btn btn-primary" (click)="save()">Save expense</button>
      </div>
    </div>

    <div class="card">
      <div class="table-scroll">
      <table>
        <thead><tr><th>Date</th><th>Category</th><th>Description</th><th>Bearer</th><th>Amount</th></tr></thead>
        <tbody>
          @for (e of expenses; track e.id) {
            <tr>
              <td>{{ e.date }}</td>
              <td>{{ e.category }}</td>
              <td>{{ e.description }}</td>
              <td>{{ e.bearer === 'tenant' ? tenantName(e.tenantId) : 'Landlord' }}</td>
              <td>{{ e.amount }}</td>
            </tr>
          }
        </tbody>
      </table>
      </div>
    </div>
  `,
})
export class ExpenseManagementComponent implements OnInit {
  protected readonly maintenanceApi = inject(MaintenanceApiService);
  protected readonly propertyApi = inject(PropertyApiService);
  protected readonly tenantApi = inject(TenantApiService);

  propertyId: number | null = null;
  bearer: 'landlord' | 'tenant' = 'landlord';
  tenantId: number | null = null;
  amount = 0;
  category = '';
  description = '';
  expenses: Awaited<ReturnType<MaintenanceApiService['allExpenses']>> = [];

  async ngOnInit(): Promise<void> {
    const [, , expenses] = await Promise.all([
      this.propertyApi.load(),
      this.tenantApi.load(),
      this.maintenanceApi.allExpenses(),
    ]);
    this.expenses = expenses;
    this.propertyId = this.propertyApi.properties()[0]?.id ?? null;
  }

  tenantName(tenantId: number | null): string {
    return this.tenantApi.tenants().find((t) => t.id === tenantId)?.name ?? '—';
  }

  onPropertyChange(event: Event): void {
    const value = (event.target as HTMLSelectElement).value;
    this.propertyId = value ? Number(value) : null;
  }

  onTenantChange(event: Event): void {
    const value = (event.target as HTMLSelectElement).value;
    this.tenantId = value ? Number(value) : null;
  }

  async save(): Promise<void> {
    if (!this.amount || !this.category || !this.propertyId) return;
    if (this.bearer === 'tenant' && !this.tenantId) return;

    const created = await this.maintenanceApi.createExpense({
      propertyId: this.propertyId,
      category: this.category,
      description: this.description,
      amount: this.amount,
      bearer: this.bearer,
      tenantId: this.bearer === 'tenant' ? this.tenantId! : undefined,
    });
    this.expenses = [...this.expenses, created];
    this.amount = 0;
    this.category = '';
    this.description = '';
    this.tenantId = null;
  }
}
