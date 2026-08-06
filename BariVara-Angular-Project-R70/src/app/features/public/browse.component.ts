import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MockDataService } from '../../core/mock-data.service';

@Component({
  selector: 'app-browse',
  standalone: true,
  imports: [FormsModule, RouterLink],
  template: `
    <div class="public-content">
      <h1>Browse listings</h1>

      <div class="search-bar" style="margin-bottom:1.5rem;">
        <input [ngModel]="query()" (ngModelChange)="query.set($event)" name="query" placeholder="Search by location or title" />
        <select [ngModel]="sourceFilter()" (ngModelChange)="sourceFilter.set($event)" name="source">
          <option value="">All sources</option>
          <option value="owner">Owner-posted</option>
          <option value="landlord-linked">LandLord-linked</option>
        </select>
      </div>

      <div class="listing-grid">
        @for (l of filtered(); track l.id) {
          <a class="listing-card" [routerLink]="['/listings', l.id]">
            <div class="listing-card-image">No photo</div>
            <div class="listing-card-body">
              <div class="listing-card-title">{{ l.title }}</div>
              <p>{{ l.address }}</p>
              <span class="badge" [class.badge-owner]="l.source === 'owner'" [class.badge-landlord-linked]="l.source === 'landlord-linked'">
                {{ l.source === 'owner' ? 'Owner-posted' : 'LandLord-linked' }}
              </span>
              <div class="listing-card-rent">৳{{ l.rent }}/mo</div>
            </div>
          </a>
        } @empty {
          <p class="hint-text">No listings match your search.</p>
        }
      </div>
    </div>
  `,
})
export class BrowseComponent {
  protected readonly data = inject(MockDataService);

  readonly query = signal(inject(ActivatedRoute).snapshot.queryParamMap.get('q') ?? '');
  readonly sourceFilter = signal('');

  readonly filtered = computed(() => {
    const q = this.query().trim().toLowerCase();
    return this.data.listings().filter((l) => {
      const matchesQuery = !q || l.title.toLowerCase().includes(q) || l.address.toLowerCase().includes(q);
      const matchesSource = !this.sourceFilter() || l.source === this.sourceFilter();
      return matchesQuery && matchesSource && l.status === 'active';
    });
  });
}
