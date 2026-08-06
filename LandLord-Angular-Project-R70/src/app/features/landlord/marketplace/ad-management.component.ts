import { Component, inject } from '@angular/core';
import { BARIVARA_DEV_URL } from '../../../core/cross-app.config';
import { MockDataService } from '../../../core/mock-data.service';

@Component({
  selector: 'app-ad-management',
  standalone: true,
  template: `
    <h1>Ad status per unit</h1>
    <p class="hint-text">
      Repost/Pause only affects this app's own data — two separate mock worlds until Phase 15's real
      sync, so BariVara's copy of a listing won't change when you click these here.
    </p>
    <div class="card">
      <table>
        <thead><tr><th>Unit</th><th>Status</th><th>Ad state</th><th></th></tr></thead>
        <tbody>
          @for (u of data.units(); track u.id) {
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
  `,
})
export class AdManagementComponent {
  protected readonly data = inject(MockDataService);
  protected readonly bariVaraUrl = BARIVARA_DEV_URL;

  adStateLabel(unit: { status: string; adPaused?: boolean }): string {
    if (unit.status !== 'vacant') return 'Not listed';
    return unit.adPaused ? 'Paused' : 'Live on BariVara.com';
  }

  pause(unitId: string): void {
    this.data.units.update((list) => list.map((u) => (u.id === unitId ? { ...u, adPaused: true } : u)));
  }

  repost(unitId: string): void {
    this.data.units.update((list) => list.map((u) => (u.id === unitId ? { ...u, adPaused: false } : u)));
  }
}
