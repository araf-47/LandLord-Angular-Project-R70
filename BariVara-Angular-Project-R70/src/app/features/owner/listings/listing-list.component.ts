import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { CURRENT_OWNER_ID, MockDataService } from '../../../core/mock-data.service';

@Component({
  selector: 'app-owner-listing-list',
  standalone: true,
  imports: [RouterLink],
  template: `
    <div class="topbar" style="background:transparent;border:none;padding:0;margin-bottom:1rem;">
      <h1>My Listings</h1>
      <a class="btn btn-primary" routerLink="/owner/listings/new">Add Post Ad</a>
    </div>

    <div class="card">
      <table>
        <thead><tr><th>Title</th><th>Rent</th><th>Status</th><th></th></tr></thead>
        <tbody>
          @for (l of myListings(); track l.id) {
            <tr>
              <td>{{ l.title }}</td>
              <td>৳{{ l.rent }}</td>
              <td><span class="badge" [class.badge-vacant]="l.status === 'active'" [class.badge-occupied]="l.status === 'taken'" [class.badge-pending]="l.status === 'paused'">{{ l.status }}</span></td>
              <td class="actions-row" style="margin:0;">
                <a class="btn btn-sm" [routerLink]="['/owner/listings', l.id, 'edit']">Edit</a>
                <button class="btn btn-sm" (click)="repost(l.id)">Repost</button>
                <button class="btn btn-sm btn-danger" (click)="remove(l.id)">Delete</button>
              </td>
            </tr>
          } @empty {
            <tr><td colspan="4" class="hint-text">No listings posted yet.</td></tr>
          }
        </tbody>
      </table>
    </div>
  `,
})
export class OwnerListingListComponent {
  protected readonly data = inject(MockDataService);

  myListings() {
    return this.data.listingsByOwner(CURRENT_OWNER_ID);
  }

  repost(id: string): void {
    this.data.listings.update((list) => list.map((l) => (l.id === id ? { ...l, status: 'active' } : l)));
  }

  remove(id: string): void {
    this.data.listings.update((list) => list.filter((l) => l.id !== id));
  }
}
