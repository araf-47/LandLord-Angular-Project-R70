import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-tenant-dashboard',
  standalone: true,
  imports: [RouterLink],
  template: `
    <h1>Choose an action</h1>
    <div class="module-grid">
      @for (m of modules; track m.link) {
        <a class="module-tile" [routerLink]="m.link">
          <div class="module-title">{{ m.title }}</div>
          <p>{{ m.desc }}</p>
        </a>
      }
    </div>
  `,
})
export class TenantDashboardComponent {
  readonly modules = [
    { title: 'My Profile', desc: 'View and edit your details.', link: '/tenant/profile' },
    { title: 'Notifications', desc: 'Landlord notices and system alerts.', link: '/tenant/notifications' },
    { title: 'Payments', desc: 'Pay rent, view history.', link: '/tenant/payments' },
    { title: 'Maintenance', desc: 'Report and track issues.', link: '/tenant/maintenance' },
    { title: 'Documents', desc: 'Your lease and uploaded files.', link: '/tenant/documents' },
    { title: 'Browse & Transfer', desc: 'Find vacant units for transfer.', link: '/tenant/browse-transfer' },
    { title: 'Messages', desc: 'Chat with your landlord.', link: '/tenant/messages' },
  ];
}
