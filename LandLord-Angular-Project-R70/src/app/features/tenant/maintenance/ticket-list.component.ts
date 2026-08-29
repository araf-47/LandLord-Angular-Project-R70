import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MaintenanceApiService } from '../../../core/maintenance-api.service';

@Component({
  selector: 'app-tenant-ticket-list',
  standalone: true,
  imports: [RouterLink],
  template: `
    <div class="topbar topbar-plain">
      <h1>Maintenance</h1>
      <a class="btn btn-primary" routerLink="/tenant/maintenance/new">Create new ticket</a>
    </div>

    @switch (status()) {
      @case ('loading') {
        <div class="card"><p class="hint-text">Loading tickets…</p></div>
      }
      @case ('error') {
        <div class="card">
          <p class="text-danger mb-sm">Couldn't load this page. {{ error() }}</p>
          <button type="button" class="btn btn-sm" (click)="ngOnInit()">Retry</button>
        </div>
      }
      @case ('ready') {
        <div class="card">
          <div class="table-scroll">
          <table>
            <thead><tr><th>Description</th><th>Status</th></tr></thead>
            <tbody>
              @for (t of api.tickets(); track t.id) {
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
        </div>
      }
    }
  `,
})
export class TenantTicketListComponent implements OnInit {
  protected readonly api = inject(MaintenanceApiService);

  readonly status = signal<'loading' | 'error' | 'ready'>('loading');
  readonly error = signal<string | undefined>(undefined);

  async ngOnInit(): Promise<void> {
    // No id needed: the backend derives "which tenant" from the auth token
    // for a TENANT-role caller and ignores any client-supplied id anyway.
    this.status.set('loading');
    try {
      await this.api.load();
      this.status.set('ready');
    } catch {
      this.error.set('Check your connection and try again.');
      this.status.set('error');
    }
  }
}
