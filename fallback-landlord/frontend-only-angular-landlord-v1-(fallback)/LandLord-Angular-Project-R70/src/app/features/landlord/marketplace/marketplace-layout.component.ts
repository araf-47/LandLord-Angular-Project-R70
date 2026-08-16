import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-marketplace-layout',
  standalone: true,
  imports: [RouterLink, RouterLinkActive, RouterOutlet],
  template: `
    <nav class="tabs">
      <a routerLink="/landlord/marketplace/ads" routerLinkActive="active">Ad Status</a>
      <a routerLink="/landlord/marketplace/requests" routerLinkActive="active">Requests</a>
    </nav>
    <router-outlet />
  `,
})
export class MarketplaceLayoutComponent {}
