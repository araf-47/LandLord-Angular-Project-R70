import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MockDataService } from '../../core/mock-data.service';

@Component({
  selector: 'app-tenant-profile',
  standalone: true,
  imports: [FormsModule],
  template: `
    <h1>My Profile</h1>
    <div class="card" style="max-width:480px;">
      @if (!editing()) {
        <p><strong>Name:</strong> {{ data.tenantProfile().name }}</p>
        <p><strong>Phone:</strong> {{ data.tenantProfile().phone }}</p>
        <p><strong>Email:</strong> {{ data.tenantProfile().email }}</p>
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
        <div class="actions-row">
          <button class="btn btn-primary" (click)="save()">Save changes</button>
          <button class="btn" (click)="editing.set(false)">Cancel</button>
        </div>
      }
    </div>
  `,
})
export class TenantProfileComponent {
  protected readonly data = inject(MockDataService);
  readonly editing = signal(false);

  name = '';
  phone = '';
  email = '';

  edit(): void {
    const p = this.data.tenantProfile();
    this.name = p.name;
    this.phone = p.phone;
    this.email = p.email;
    this.editing.set(true);
  }

  save(): void {
    if (!this.name || !this.email) return;
    this.data.tenantProfile.update((p) => ({ ...p, name: this.name, phone: this.phone, email: this.email }));
    this.editing.set(false);
  }
}
