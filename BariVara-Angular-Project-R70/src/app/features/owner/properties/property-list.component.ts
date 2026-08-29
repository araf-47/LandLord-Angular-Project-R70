import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { OwnerPropertyApiService } from '../../../core/owner-property-api.service';
import { OwnerUnitApiService } from '../../../core/owner-unit-api.service';
import { ProfileApiService } from '../../../core/profile-api.service';

@Component({
  selector: 'app-owner-property-list',
  standalone: true,
  imports: [RouterLink],
  template: `
    @switch (status()) {
      @case ('loading') {
        <div class="card"><p class="hint-text">Loading properties…</p></div>
      }
      @case ('error') {
        <div class="card">
          <p class="text-danger mb-sm">Couldn't load this page. {{ error() }}</p>
          <button type="button" class="btn btn-sm" (click)="ngOnInit()">Retry</button>
        </div>
      }
      @case ('ready') {
        <div class="topbar topbar-plain">
          <h1>Properties</h1>
          <a class="btn btn-primary" routerLink="/owner/properties/new">Add property</a>
        </div>

        <div class="card">
          <div class="table-scroll">
          <table>
            <thead><tr><th>Property</th><th>Address</th><th>Units</th><th></th></tr></thead>
            <tbody>
              @for (p of myProperties(); track p.id) {
                <tr>
                  <td>{{ p.name }}</td>
                  <td>{{ p.address }}</td>
                  <td>{{ unitCount(p.id) }}</td>
                  <td class="actions-row">
                    <a class="btn btn-sm" [routerLink]="['/owner/properties', p.id, 'units']">Manage units</a>
                  </td>
                </tr>
              } @empty {
                <tr><td colspan="4" class="hint-text">No properties yet.</td></tr>
              }
            </tbody>
          </table>
          </div>
        </div>
      }
    }
  `,
})
export class OwnerPropertyListComponent implements OnInit {
  protected readonly propertyApi = inject(OwnerPropertyApiService);
  private readonly unitApi = inject(OwnerUnitApiService);
  private readonly profileApi = inject(ProfileApiService);

  readonly status = signal<'loading' | 'error' | 'ready'>('loading');
  readonly error = signal<string | undefined>(undefined);

  async ngOnInit(): Promise<void> {
    this.status.set('loading');
    try {
      const profile = await this.profileApi.myOwnerProfile();
      const properties = await this.propertyApi.loadForOwner(profile.id);
      await this.unitApi.loadForProperties(properties.map((p) => p.id));
      this.status.set('ready');
    } catch {
      this.error.set('Check your connection and try again.');
      this.status.set('error');
    }
  }

  readonly myProperties = computed(() => this.propertyApi.properties());

  unitCount(propertyId: number): number {
    return this.unitApi.units().filter((u) => u.propertyId === propertyId).length;
  }
}
