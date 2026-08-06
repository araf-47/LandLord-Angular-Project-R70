import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { MockDataService } from '../../../core/mock-data.service';

@Component({
  selector: 'app-owner-listing-edit',
  standalone: true,
  imports: [FormsModule],
  template: `
    @if (listing) {
      <h1>Edit listing</h1>
      <div class="card stack" style="max-width:520px;">
        <div class="field">
          <label for="title">Title</label>
          <input id="title" name="title" [(ngModel)]="title" />
        </div>
        <div class="field">
          <label for="address">Address</label>
          <input id="address" name="address" [(ngModel)]="address" />
        </div>
        <div class="field">
          <label for="rent">Rent</label>
          <input id="rent" type="number" name="rent" [(ngModel)]="rent" />
        </div>
        <div class="actions-row">
          <button class="btn btn-primary" (click)="save()">Edit &amp; save</button>
        </div>
      </div>
    }
  `,
})
export class OwnerListingEditComponent {
  private readonly data = inject(MockDataService);
  private readonly router = inject(Router);
  private readonly listingId = inject(ActivatedRoute).snapshot.paramMap.get('listingId')!;

  readonly listing = this.data.listingById(this.listingId);
  title = this.listing?.title ?? '';
  address = this.listing?.address ?? '';
  rent = this.listing?.rent ?? 0;

  save(): void {
    this.data.listings.update((list) =>
      list.map((l) => (l.id === this.listingId ? { ...l, title: this.title, address: this.address, rent: this.rent } : l))
    );
    this.router.navigateByUrl('/owner/listings');
  }
}
