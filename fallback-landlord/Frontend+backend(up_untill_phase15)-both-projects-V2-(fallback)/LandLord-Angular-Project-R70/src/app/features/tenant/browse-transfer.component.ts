import { Component, inject, signal } from '@angular/core';
import { BARIVARA_DEV_URL } from '../../core/cross-app.config';
import { CURRENT_TENANT_ID, MockDataService, nextId } from '../../core/mock-data.service';

@Component({
  selector: 'app-tenant-browse-transfer',
  standalone: true,
  template: `
    <h1>Browse &amp; Transfer</h1>

    <div class="card">
      <h3>Vacant units (this property)</h3>
      <div class="table-scroll">
      <table>
        <thead><tr><th>Unit</th><th>Rent</th><th></th></tr></thead>
        <tbody>
          @for (u of vacantUnits(); track u.id) {
            <tr>
              <td>{{ u.unitNumber }}</td>
              <td>{{ u.rent }}</td>
              <td><button class="btn btn-sm btn-primary" (click)="requestTransfer(u.id)">Request transfer</button></td>
            </tr>
          } @empty {
            <tr><td colspan="3" class="hint-text">No vacant units right now.</td></tr>
          }
        </tbody>
      </table>
      </div>
      @if (requested()) {
        <p class="hint-text" style="margin-top:0.75rem;">Landlord notified via Marketplace &amp; Leads inbox.</p>
      }
    </div>

    <div class="card">
      <h3>Looking outside this property?</h3>
      <p>Global search redirects to the BariVara.com marketplace.</p>
      <a class="btn" [href]="bariVaraUrl + '/browse'" target="_blank" rel="noopener">Search all listings on BariVara.com</a>
      <p class="hint-text" style="margin-top:0.5rem;">Opens the BariVara dev site in a new tab — separate mock data until Phase 15's real sync.</p>
    </div>
  `,
})
export class TenantBrowseTransferComponent {
  private readonly data = inject(MockDataService);
  protected readonly bariVaraUrl = BARIVARA_DEV_URL;
  readonly requested = signal(false);

  vacantUnits() {
    return this.data.units().filter((u) => u.status === 'vacant');
  }

  requestTransfer(unitId: string): void {
    this.data.marketplaceRequests.update((list) => [
      ...list,
      {
        id: nextId('r'),
        unitId,
        applicantName: this.data.tenants().find((t) => t.id === CURRENT_TENANT_ID)?.name ?? 'Tenant',
        tenantId: CURRENT_TENANT_ID,
        status: 'pending',
      },
    ]);
    this.requested.set(true);
  }
}
