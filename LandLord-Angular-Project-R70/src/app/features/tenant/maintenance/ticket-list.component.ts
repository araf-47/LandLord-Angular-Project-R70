import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { CURRENT_TENANT_ID, MockDataService } from '../../../core/mock-data.service';

@Component({
  selector: 'app-tenant-ticket-list',
  standalone: true,
  imports: [RouterLink],
  template: `
    <div class="topbar" style="background:transparent;border:none;padding:0;margin-bottom:1rem;">
      <h1>Maintenance</h1>
      <a class="btn btn-primary" routerLink="/tenant/maintenance/new">Create new ticket</a>
    </div>

    <div class="card">
      <table>
        <thead><tr><th>Description</th><th>Status</th></tr></thead>
        <tbody>
          @for (t of myTickets(); track t.id) {
            <tr>
              <td>{{ t.description }}</td>
              <td><span class="badge" [class.badge-pending]="t.status === 'pending'" [class.badge-paid]="t.status === 'resolved'">{{ t.status }}</span></td>
            </tr>
          } @empty {
            <tr><td colspan="2" class="hint-text">No tickets yet.</td></tr>
          }
        </tbody>
      </table>
    </div>
  `,
})
export class TenantTicketListComponent {
  private readonly data = inject(MockDataService);

  myTickets() {
    return this.data.tickets().filter((t) => t.tenantId === CURRENT_TENANT_ID);
  }
}
