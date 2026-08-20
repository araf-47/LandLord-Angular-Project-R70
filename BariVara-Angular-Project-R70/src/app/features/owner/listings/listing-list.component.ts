import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ListingApiService } from '../../../core/listing-api.service';
import { CURRENT_OWNER_ID_REAL } from '../../../core/current-owner';

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
      <div class="table-scroll">
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
    </div>
  `,
})
export class OwnerListingListComponent {
  protected readonly api = inject(ListingApiService);

  constructor() {
    this.api.search({ ownerId: CURRENT_OWNER_ID_REAL });
  }

  myListings() {
    return this.api.listings();
  }

  repost(id: number): void {
    this.api.setStatus(id, 'active');
  }

  remove(id: number): void {
    this.api.delete(id);
  }
}
