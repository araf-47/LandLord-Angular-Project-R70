import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { MockDataService, nextId } from '../../../core/mock-data.service';

@Component({
  selector: 'app-landlord-ticket-new',
  standalone: true,
  imports: [FormsModule],
  template: `
    <h1>Log new issue</h1>
    <div class="card stack" style="max-width:520px;">
      <div class="field">
        <label for="tenant">Tenant &amp; unit</label>
        <select id="tenant" name="tenant" [(ngModel)]="tenantId">
          <option value="">— choose —</option>
          @for (t of data.tenants(); track t.id) {
            <option [value]="t.id">{{ t.name }} — {{ unitLabel(t.unitId) }}</option>
          }
        </select>
      </div>
      <div class="field">
        <label for="description">Issue description</label>
        <textarea id="description" rows="3" name="description" [(ngModel)]="description"></textarea>
      </div>
      <div class="actions-row">
        <button class="btn btn-primary" (click)="save()">Save ticket (status: Pending)</button>
      </div>
    </div>
  `,
})
export class LandlordTicketNewComponent {
  protected readonly data = inject(MockDataService);
  private readonly router = inject(Router);

  tenantId = '';
  description = '';

  unitLabel(unitId?: string): string {
    return this.data.units().find((u) => u.id === unitId)?.unitNumber ?? '—';
  }

  save(): void {
    const tenant = this.data.tenants().find((t) => t.id === this.tenantId);
    if (!tenant || !this.description || !tenant.unitId) return;

    this.data.tickets.update((list) => [
      ...list,
      { id: nextId('tk'), unitId: tenant.unitId!, tenantId: tenant.id, description: this.description, status: 'pending' },
    ]);
    this.router.navigateByUrl('/landlord/maintenance');
  }
}
