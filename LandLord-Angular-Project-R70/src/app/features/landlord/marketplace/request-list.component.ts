import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MarketplaceApiService } from '../../../core/marketplace-api.service';
import { UnitApiService } from '../../../core/unit-api.service';

@Component({
  selector: 'app-request-list',
  standalone: true,
  imports: [RouterLink],
  template: `
    <h1>Booking requests (synced from BariVara)</h1>

    @switch (status()) {
      @case ('loading') {
        <div class="card"><p class="hint-text">Loading requests…</p></div>
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
            <thead><tr><th>Applicant</th><th>Unit</th><th>Status</th><th></th></tr></thead>
            <tbody>
              @for (r of marketplaceApi.requests(); track r.id) {
                <tr>
                  <td>{{ r.applicantName }}</td>
                  <td>{{ unitLabel(r.unitId) }}</td>
                  <td><span class="badge badge-pending">{{ r.status }}</span></td>
                  <td><a class="btn btn-sm" [routerLink]="['/landlord/marketplace/requests', r.id]">Open</a></td>
                </tr>
              }
            </tbody>
          </table>
          </div>
        </div>
      }
    }
  `,
})
export class RequestListComponent implements OnInit {
  protected readonly marketplaceApi = inject(MarketplaceApiService);
  private readonly unitApi = inject(UnitApiService);

  readonly status = signal<'loading' | 'error' | 'ready'>('loading');
  readonly error = signal<string | undefined>(undefined);

  async ngOnInit(): Promise<void> {
    this.status.set('loading');
    try {
      await Promise.all([this.marketplaceApi.load(), this.unitApi.load()]);
      this.status.set('ready');
    } catch {
      this.error.set('Check your connection and try again.');
      this.status.set('error');
    }
  }

  unitLabel(unitId: number): string {
    return this.unitApi.units().find((u) => u.id === unitId)?.unitNumber ?? '—';
  }
}
