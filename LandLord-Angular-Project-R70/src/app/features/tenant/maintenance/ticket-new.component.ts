import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { MaintenanceApiService } from '../../../core/maintenance-api.service';
import { TenantApiService } from '../../../core/tenant-api.service';

@Component({
  selector: 'app-tenant-ticket-new',
  standalone: true,
  imports: [FormsModule],
  template: `
    <h1>Create new ticket</h1>
    <div class="card stack max-w-md">
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
        <input id="image" type="file" name="image" accept="image/*" (change)="onFileSelected($event)" />
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
  private readonly api = inject(MaintenanceApiService);
  private readonly tenantApi = inject(TenantApiService);
  private readonly router = inject(Router);

  category = 'plumbing';
  description = '';
  fileName = '';
  private selectedFile: File | null = null;

  onFileSelected(event: Event): void {
    const file = (event.target as HTMLInputElement).files?.[0] ?? null;
    this.selectedFile = file;
    this.fileName = file?.name ?? '';
  }

  async submit(): Promise<void> {
    if (!this.description) return;
    const tenant = await this.tenantApi.me();
    if (!tenant?.unitId) return;

    const ticket = await this.api.createTicket(tenant.unitId, tenant.id, `[${this.category}] ${this.description}`);
    if (this.selectedFile) {
      await this.api.uploadPhoto(ticket.id, this.selectedFile);
    }
    this.router.navigateByUrl('/tenant/maintenance');
  }
}
