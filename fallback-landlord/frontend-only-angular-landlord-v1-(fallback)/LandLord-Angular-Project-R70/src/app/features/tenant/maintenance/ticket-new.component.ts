import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { CURRENT_TENANT_ID, MockDataService, nextId } from '../../../core/mock-data.service';

@Component({
  selector: 'app-tenant-ticket-new',
  standalone: true,
  imports: [FormsModule],
  template: `
    <h1>Create new ticket</h1>
    <div class="card stack" style="max-width:520px;">
      <div class="field">
        <label for="category">Category</label>
        <select id="category" name="category" [(ngModel)]="category">
          <option value="plumbing">Plumbing</option>
          <option value="electrical">Electrical</option>
          <option value="appliance">Appliance</option>
          <option value="other">Other</option>
        </select>
      </div>
      <div class="field">
        <label for="description">Describe the problem</label>
        <textarea id="description" rows="3" name="description" [(ngModel)]="description"></textarea>
      </div>
      <div class="field">
        <label for="image">Upload image (optional)</label>
        <input id="image" type="file" name="image" (change)="fileName = $any($event.target).files?.[0]?.name ?? ''" />
        @if (fileName) {
          <span class="hint-text">{{ fileName }}</span>
        }
      </div>
      <div class="actions-row">
        <button class="btn btn-primary" (click)="submit()">Submit</button>
      </div>
    </div>
  `,
})
export class TenantTicketNewComponent {
  private readonly data = inject(MockDataService);
  private readonly router = inject(Router);

  category = 'plumbing';
  description = '';
  fileName = '';

  submit(): void {
    if (!this.description) return;
    const tenant = this.data.tenants().find((t) => t.id === CURRENT_TENANT_ID);
    if (!tenant?.unitId) return;

    this.data.tickets.update((list) => [
      ...list,
      { id: nextId('tk'), unitId: tenant.unitId!, tenantId: CURRENT_TENANT_ID, description: `[${this.category}] ${this.description}`, status: 'pending' },
    ]);
    this.router.navigateByUrl('/tenant/maintenance');
  }
}
