import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { BillingApiService } from '../../../core/billing-api.service';
import { ApiRentalAgreement, ApiTenant, TenantApiService } from '../../../core/tenant-api.service';

@Component({
  selector: 'app-tenant-moveout',
  standalone: true,
  imports: [FormsModule],
  template: `
    @switch (status()) {
      @case ('loading') {
        <div class="card"><p class="hint-text">Loading tenant…</p></div>
      }
      @case ('error') {
        <div class="card">
          <p class="text-danger mb-sm">Couldn't load this page. {{ error() }}</p>
          <button type="button" class="btn btn-sm" (click)="ngOnInit()">Retry</button>
        </div>
      }
      @case ('ready') {
    @if (tenant()) {
      <h1>Move out — {{ tenant()!.name }}</h1>

      <div class="card stack max-w-lg">
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
      }
    }
  `,
})
export class TenantMoveoutComponent implements OnInit {
  private readonly api = inject(TenantApiService);
  private readonly billingApi = inject(BillingApiService);
  private readonly router = inject(Router);
  private readonly tenantId = +inject(ActivatedRoute).snapshot.paramMap.get('tenantId')!;

  deductions = 0;
  mode: 'refund' | 'bill' = 'refund';
  readonly done = signal(false);

  readonly tenant = signal<ApiTenant | null>(null);
  readonly agreement = signal<ApiRentalAgreement | null>(null);
  readonly outstandingBalance = signal(0);

  readonly status = signal<'loading' | 'error' | 'ready'>('loading');
  readonly error = signal<string | undefined>(undefined);

  async ngOnInit(): Promise<void> {
    this.status.set('loading');
    try {
      const [tenant, agreement, balance] = await Promise.all([
        this.api.get(this.tenantId),
        this.api.agreementFor(this.tenantId),
        this.billingApi.outstandingBalance(this.tenantId),
      ]);
      this.tenant.set(tenant);
      this.agreement.set(agreement);
      this.outstandingBalance.set(balance);
      this.status.set('ready');
    } catch {
      this.error.set('Check your connection and try again.');
      this.status.set('error');
    }
  }

  resultLabel(): string {
    return this.mode === 'refund' ? 'Calculated refund' : 'Final invoice';
  }

  resultAmount(): number {
    const deposit = this.agreement()?.deposit ?? 0;
    return this.mode === 'refund' ? Math.max(0, deposit - this.deductions) : this.outstandingBalance() + this.deductions;
  }

  async process(): Promise<void> {
    if (!this.tenant()) return;
    const result = await this.api.moveOut(this.tenantId);
    this.outstandingBalance.set(result.outstandingBalance);
    this.done.set(true);
    setTimeout(() => this.router.navigateByUrl('/landlord/tenants'), 900);
  }
}
