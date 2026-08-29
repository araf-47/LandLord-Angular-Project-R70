import { Component, OnInit, inject, signal } from '@angular/core';
import { BARIVARA_DEV_URL } from '../../../core/cross-app.config';
import { MessagingApiService } from '../../../core/messaging-api.service';
import { ApiUnit, UnitApiService } from '../../../core/unit-api.service';

@Component({
  selector: 'app-ad-management',
  standalone: true,
  template: `
    <h1>Ad status per unit</h1>

    @switch (status()) {
      @case ('loading') {
        <div class="card"><p class="hint-text">Loading ads…</p></div>
      }
      @case ('error') {
        <div class="card">
          <p class="text-danger mb-sm">Couldn't load this page. {{ error() }}</p>
          <button type="button" class="btn btn-sm" (click)="ngOnInit()">Retry</button>
        </div>
      }
      @case ('ready') {
        <p class="hint-text">
          Repost/Pause only affects this app's own data — two separate mock worlds until Phase 15's real
          sync, so BariVara's copy of a listing won't change when you click these here.
        </p>
        @if (messaging.notifications().length > 0) {
          <div class="card stack">
            <strong>Reminders</strong>
            @for (n of messaging.notifications(); track n.id) {
              <div class="card">
                <strong>{{ n.title }}</strong>
                <p>{{ n.body }}</p>
                <button class="btn btn-sm" (click)="dismiss(n.id)">Dismiss</button>
              </div>
            }
          </div>
        }
        <div class="card">
          <div class="table-scroll">
          <table>
            <thead><tr><th>Unit</th><th>Status</th><th>Ad state</th><th></th></tr></thead>
            <tbody>
              @for (u of unitApi.units(); track u.id) {
                <tr>
                  <td>{{ u.unitNumber }}</td>
                  <td><span class="badge" [class.badge-vacant]="u.status === 'vacant'" [class.badge-occupied]="u.status === 'occupied'">{{ u.status }}</span></td>
                  <td>
                    @if (u.status === 'vacant' && !u.adPaused) {
                      <a [href]="bariVaraUrl + '/browse'" target="_blank" rel="noopener">Live on BariVara.com</a>
                    } @else {
                      {{ adStateLabel(u) }}
                    }
                  </td>
                  <td>
                    @if (u.status === 'vacant') {
                      @if (u.adPaused) {
                        <button class="btn btn-sm" (click)="repost(u.id)">Repost</button>
                      } @else {
                        <button class="btn btn-sm" (click)="pause(u.id)">Pause</button>
                      }
                    }
                  </td>
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
export class AdManagementComponent implements OnInit {
  protected readonly unitApi = inject(UnitApiService);
  protected readonly messaging = inject(MessagingApiService);
  protected readonly bariVaraUrl = BARIVARA_DEV_URL;

  readonly status = signal<'loading' | 'error' | 'ready'>('loading');
  readonly error = signal<string | undefined>(undefined);

  async ngOnInit(): Promise<void> {
    this.status.set('loading');
    try {
      await this.unitApi.load();
      await this.messaging.loadLandlordNotifications();
      this.status.set('ready');
    } catch {
      this.error.set('Check your connection and try again.');
      this.status.set('error');
    }
  }

  async dismiss(id: number): Promise<void> {
    await this.messaging.deleteNotification(id);
  }

  adStateLabel(unit: ApiUnit): string {
    if (unit.status !== 'vacant') return 'Not listed';
    return unit.adPaused ? 'Paused' : 'Live on BariVara.com';
  }

  async pause(unitId: number): Promise<void> {
    await this.unitApi.pauseAd(unitId);
  }

  async repost(unitId: number): Promise<void> {
    await this.unitApi.repostAd(unitId);
  }
}
