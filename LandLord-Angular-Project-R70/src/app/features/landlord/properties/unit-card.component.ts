import { Component, Input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { API_ORIGIN } from '../../../core/maintenance-api.service';
import type { ApiUnit } from '../../../core/unit-api.service';

@Component({
  selector: 'app-unit-card',
  standalone: true,
  imports: [RouterLink],
  template: `
    <div class="unit-card" [class.unit-card-vacant]="unit.status === 'vacant'"
         [class.unit-card-overdue]="overdue" [class.unit-card-occupied]="unit.status === 'occupied' && !overdue">
      @if (unit.photoUrl) {
        <img [src]="photoOrigin + unit.photoUrl" alt="" class="unit-card-photo" />
      } @else {
        <div class="unit-card-photo unit-card-photo-empty">No photo</div>
      }
      <div class="unit-card-body">
        <div class="unit-card-title">{{ unit.unitNumber }}</div>
        <div class="hint-text">৳{{ unit.rent }}/mo</div>
        @if (tenantName) {
          <div class="hint-text">{{ tenantName }}</div>
        }
        <span class="badge" [class.badge-vacant]="unit.status === 'vacant'"
              [class.badge-occupied]="unit.status === 'occupied' && !overdue"
              [class.badge-overdue]="overdue">
          {{ overdue ? 'overdue' : unit.status }}
        </span>
      </div>
      <a class="btn btn-sm unit-card-edit" [routerLink]="['/landlord/properties', unit.propertyId, 'units', unit.id, 'edit']">Edit</a>
    </div>
  `,
})
export class UnitCardComponent {
  @Input({ required: true }) unit!: ApiUnit;
  @Input() tenantName: string | null = null;
  @Input() overdue = false;
  protected readonly photoOrigin = API_ORIGIN;
}
