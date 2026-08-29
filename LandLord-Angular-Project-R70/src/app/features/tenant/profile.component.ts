import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApiTenant, TenantApiService } from '../../core/tenant-api.service';

@Component({
  selector: 'app-tenant-profile',
  standalone: true,
  imports: [FormsModule],
  template: `
    <h1>My Profile</h1>

    @switch (status()) {
      @case ('loading') {
        <div class="card"><p class="hint-text">Loading profile…</p></div>
      }
      @case ('error') {
        <div class="card">
          <p class="text-danger mb-sm">Couldn't load this page. {{ loadError() }}</p>
          <button type="button" class="btn btn-sm" (click)="ngOnInit()">Retry</button>
        </div>
      }
      @case ('ready') {
    @if (tenant(); as t) {
      <div class="card max-w-sm">
        @if (!editing()) {
          <p><strong>Name:</strong> {{ t.name }}</p>
          <p><strong>Phone:</strong> {{ t.phone }}</p>
          <p><strong>Email:</strong> {{ t.email }}</p>
          <button class="btn btn-primary" (click)="edit()">Edit info</button>
        } @else {
          <div class="field">
            <label for="name">Name</label>
            <input id="name" name="name" [(ngModel)]="name" required />
          </div>
          <div class="field">
            <label for="phone">Phone</label>
            <input id="phone" name="phone" [(ngModel)]="phone" required />
          </div>
          <div class="field">
            <label for="email">Email</label>
            <input id="email" type="email" name="email" [(ngModel)]="email" required />
          </div>
          @if (error()) {
            <p class="error-text">{{ error() }}</p>
          }
          <div class="actions-row">
            <button class="btn btn-primary" (click)="save()">Save changes</button>
            <button class="btn" (click)="editing.set(false)">Cancel</button>
          </div>
        }
      </div>
    }
      }
    }
  `,
})
export class TenantProfileComponent implements OnInit {
  private readonly tenantApi = inject(TenantApiService);

  readonly tenant = signal<ApiTenant | null>(null);
  readonly editing = signal(false);
  readonly error = signal('');
  name = '';
  phone = '';
  email = '';

  readonly status = signal<'loading' | 'error' | 'ready'>('loading');
  readonly loadError = signal<string | undefined>(undefined);

  async ngOnInit(): Promise<void> {
    this.status.set('loading');
    try {
      this.tenant.set(await this.tenantApi.me());
      this.status.set('ready');
    } catch {
      this.loadError.set('Check your connection and try again.');
      this.status.set('error');
    }
  }

  edit(): void {
    const t = this.tenant();
    if (!t) return;
    this.name = t.name;
    this.phone = t.phone;
    this.email = t.email;
    this.editing.set(true);
  }

  async save(): Promise<void> {
    const t = this.tenant();
    if (!t) return;
    if (!this.name || !this.email) {
      this.error.set('Name and email are required.');
      return;
    }
    this.error.set('');
    const updated = await this.tenantApi.update(t.id, {
      ...t,
      name: this.name,
      phone: this.phone,
      email: this.email,
    });
    this.tenant.set(updated);
    this.editing.set(false);
  }
}
