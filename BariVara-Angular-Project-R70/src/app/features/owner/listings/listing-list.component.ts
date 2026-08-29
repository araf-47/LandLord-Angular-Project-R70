import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { API_ORIGIN, ListingApiService } from '../../../core/listing-api.service';
import { ProfileApiService } from '../../../core/profile-api.service';
import { ConfirmService } from '../../../shared/confirm.service';

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
        <thead><tr><th>Photo</th><th>Title</th><th>Rent</th><th>Status</th><th></th></tr></thead>
        <tbody>
          @for (l of myListings(); track l.id) {
            <tr>
              <td>
                @if (l.photoUrl) {
                  <img [src]="photoOrigin + l.photoUrl" alt="" style="width:48px;height:48px;object-fit:cover;border-radius:4px;" />
                } @else {
                  <span class="hint-text">No photo</span>
                }
              </td>
              <td>{{ l.title }}</td>
              <td>৳{{ l.rent }}</td>
              <td><span class="badge" [class.badge-vacant]="l.status === 'active'" [class.badge-occupied]="l.status === 'taken'" [class.badge-pending]="l.status === 'paused'">{{ l.status }}</span></td>
              <td class="actions-row" style="margin:0;">
                <a class="btn btn-sm" [routerLink]="['/owner/listings', l.id, 'edit']">Edit</a>
                <button class="btn btn-sm" (click)="repost(l.id)">Repost</button>
                <button class="btn btn-sm btn-danger" (click)="remove(l.id)" type="button">Delete</button>
              </td>
            </tr>
          } @empty {
            <tr><td colspan="5" class="hint-text">No listings posted yet.</td></tr>
          }
        </tbody>
      </table>
      </div>
    </div>
  `,
})
export class OwnerListingListComponent {
  protected readonly api = inject(ListingApiService);
  private readonly profileApi = inject(ProfileApiService);
  protected readonly photoOrigin = API_ORIGIN;
  private readonly confirmService = inject(ConfirmService);

  constructor() {
    this.profileApi.myOwnerProfile().then((profile) => {
      this.api.search({ ownerId: profile.id });
    });
  }

  myListings() {
    return this.api.listings();
  }

  repost(id: number): void {
    this.api.setStatus(id, 'active');
  }

  async remove(id: number): Promise<void> {
    const confirmed = await this.confirmService.confirm('Delete listing', 'Delete this listing? This cannot be undone.');
    if (!confirmed) return;
    this.api.delete(id);
  }
}
