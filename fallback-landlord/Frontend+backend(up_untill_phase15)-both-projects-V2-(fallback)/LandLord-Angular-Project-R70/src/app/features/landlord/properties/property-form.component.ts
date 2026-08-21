import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { PropertyApiService } from '../../../core/property-api.service';

@Component({
  selector: 'app-property-form',
  standalone: true,
  imports: [FormsModule],
  template: `
    <h1>{{ editing ? 'Edit property' : 'Add property' }}</h1>
    <div class="card" style="max-width:520px;">
      <div class="field">
        <label for="name">Property name</label>
        <input id="name" name="name" [(ngModel)]="name" required />
      </div>
      <div class="field">
        <label for="address">Address</label>
        <input id="address" name="address" [(ngModel)]="address" required />
      </div>
      <div class="actions-row">
        <button class="btn btn-primary" (click)="save()">Validate & save</button>
      </div>
    </div>
  `,
})
export class PropertyFormComponent implements OnInit {
  private readonly api = inject(PropertyApiService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  private readonly propertyId = this.route.snapshot.paramMap.get('propertyId');
  readonly editing = !!this.propertyId;

  name = '';
  address = '';

  async ngOnInit(): Promise<void> {
    if (this.propertyId) {
      const property = await this.api.get(+this.propertyId);
      this.name = property.name;
      this.address = property.address;
    }
  }

  async save(): Promise<void> {
    if (!this.name || !this.address) return;
    if (this.editing && this.propertyId) {
      await this.api.update(+this.propertyId, this.name, this.address);
    } else {
      await this.api.create(this.name, this.address);
    }
    this.router.navigateByUrl('/landlord/properties');
  }
}
