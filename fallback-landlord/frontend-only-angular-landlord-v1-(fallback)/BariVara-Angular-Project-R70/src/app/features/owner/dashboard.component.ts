import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-owner-dashboard',
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
export class OwnerDashboardComponent {
  readonly modules = [
    { title: 'Properties', desc: 'Add property and unit info.', link: '/owner/properties' },
    { title: 'My Listings', desc: 'Post, edit, repost, or delete ads.', link: '/owner/listings' },
    { title: 'Requests', desc: 'Review booking requests.', link: '/owner/requests' },
    { title: 'Messages', desc: 'Chat with applicants.', link: '/owner/messages' },
  ];
}
