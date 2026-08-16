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
    { title: 'Search', desc: 'Find vacant listings.', link: '/tenant/search' },
    { title: 'Favorites', desc: 'Listings you saved.', link: '/tenant/favorites' },
    { title: 'Notifications', desc: 'Updates on your requests.', link: '/tenant/notifications' },
    { title: 'Messages', desc: 'Chat with owners.', link: '/tenant/messages' },
    { title: 'My Profile', desc: 'View and edit your details.', link: '/tenant/profile' },
  ];
}
