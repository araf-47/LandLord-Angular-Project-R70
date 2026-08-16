import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { MockDataService } from '../../../core/mock-data.service';

@Component({
  selector: 'app-tenant-moveout',
  standalone: true,
  imports: [FormsModule],
  template: `
    @if (tenant()) {
      <h1>Move out — {{ tenant()!.name }}</h1>

      <div class="card stack" style="max-width:560px;">
        <p><strong>Outstanding balance:</strong> {{ outstandingBalance() }}</p>

        <div class="field">
          <label for="deductions">Damage deductions</label>
          <input id="deductions" type="number" name="deductions" [(ngModel)]="deductions" />
        </div>

        <div class="field">
          <label for="mode">Refund or final bill?</label>
          <select id="mode" name="mode" [(ngModel)]="mode">
            <option value="refund">Refund</option>
            <option value="bill">Final bill</option>
          </select>
        </div>

        <p class="hint-text">{{ resultLabel() }}: {{ resultAmount() }}</p>

        <div class="actions-row">
          <button class="btn btn-danger" (click)="process()">Process move-out</button>
        </div>

        @if (done()) {
          <p class="hint-text">Unit set vacant, ad auto-posted to BariVara, tenant archived.</p>
        }
      </div>
    }
  `,
})
export class TenantMoveoutComponent {
  private readonly data = inject(MockDataService);
  private readonly router = inject(Router);
  private readonly tenantId = inject(ActivatedRoute).snapshot.paramMap.get('tenantId')!;

  deductions = 0;
  mode: 'refund' | 'bill' = 'refund';
  readonly done = signal(false);

  readonly tenant = computed(() => this.data.tenants().find((t) => t.id === this.tenantId));
  readonly agreement = computed(() => this.data.agreements().find((a) => a.tenantId === this.tenantId));

  outstandingBalance(): number {
    return this.data
      .invoices()
      .filter((i) => i.tenantId === this.tenantId)
      .reduce((sum, i) => sum + i.balance, 0);
  }

  resultLabel(): string {
    return this.mode === 'refund' ? 'Calculated refund' : 'Final invoice';
  }

  resultAmount(): number {
    const deposit = this.agreement()?.deposit ?? 0;
    return this.mode === 'refund' ? Math.max(0, deposit - this.deductions) : this.outstandingBalance() + this.deductions;
  }

  process(): void {
    const t = this.tenant();
    if (!t) return;

    this.data.units.update((list) => list.map((u) => (u.id === t.unitId ? { ...u, status: 'vacant' } : u)));
    this.data.tenants.update((list) => list.map((x) => (x.id === t.id ? { ...x, status: 'inactive', unitId: undefined } : x)));
    this.done.set(true);
    setTimeout(() => this.router.navigateByUrl('/landlord/tenants'), 900);
  }
}
