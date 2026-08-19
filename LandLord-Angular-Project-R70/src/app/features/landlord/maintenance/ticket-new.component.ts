import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { MaintenanceApiService } from '../../../core/maintenance-api.service';
import { TenantApiService } from '../../../core/tenant-api.service';
import { UnitApiService } from '../../../core/unit-api.service';

@Component({
  selector: 'app-landlord-ticket-new',
  standalone: true,
  imports: [FormsModule],
  template: `
    <h1>Log new issue</h1>
    <div class="card stack" style="max-width:520px;">
      <div class="field">
        <label for="tenant">Tenant &amp; unit</label>
        <select id="tenant" name="tenant" (change)="onTenantChange($event)">
          <option value="">— choose —</option>
          @for (t of tenantApi.tenants(); track t.id) {
            <option [value]="t.id" [selected]="t.id === tenantId">{{ t.name }} — {{ unitLabel(t.unitId) }}</option>
          }
        </select>
      </div>
      <div class="field">
        <label for="description">Issue description</label>
        <textarea id="description" rows="3" name="description" [(ngModel)]="description"></textarea>
      </div>
      @if (error()) {
        <p class="error-text">{{ error() }}</p>
      }
      <div class="actions-row">
        <button class="btn btn-primary" (click)="save()">Save ticket (status: Pending)</button>
      </div>
    </div>
  `,
})
export class LandlordTicketNewComponent implements OnInit {
  private readonly api = inject(MaintenanceApiService);
  protected readonly tenantApi = inject(TenantApiService);
  private readonly unitApi = inject(UnitApiService);
  private readonly router = inject(Router);

  tenantId: number | null = null;
  description = '';
  readonly error = signal('');

  async ngOnInit(): Promise<void> {
    await Promise.all([this.tenantApi.load(), this.unitApi.load()]);
  }

  unitLabel(unitId: number | null): string {
    return this.unitApi.units().find((u) => u.id === unitId)?.unitNumber ?? '—';
  }

  onTenantChange(event: Event): void {
    const value = (event.target as HTMLSelectElement).value;
    this.tenantId = value ? Number(value) : null;
  }

  async save(): Promise<void> {
    const tenant = this.tenantApi.tenants().find((t) => t.id === this.tenantId);
    if (!tenant) {
      this.error.set('Choose a tenant first.');
      return;
    }
    if (!this.description) {
      this.error.set('Enter an issue description.');
      return;
    }
    if (!tenant.unitId) {
      this.error.set(`${tenant.name} has no assigned unit — can't log a ticket for them.`);
      return;
    }

    this.error.set('');
    try {
      await this.api.createTicket(tenant.unitId, tenant.id, this.description);
      this.router.navigateByUrl('/landlord/maintenance');
    } catch {
      this.error.set('Could not save the ticket — check the backend is running and try again.');
    }
  }
}
