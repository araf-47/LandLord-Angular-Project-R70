import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { ApiInvoice, ApiPayment, BillingApiService } from '../../../core/billing-api.service';
import { ApiExpense, MaintenanceApiService } from '../../../core/maintenance-api.service';
import { MessagingApiService } from '../../../core/messaging-api.service';
import { periodLabel } from '../../../core/mock-data.service';
import { ApiRentalAgreement, ApiTenant, TenantApiService } from '../../../core/tenant-api.service';
import { UnitApiService } from '../../../core/unit-api.service';

@Component({
  selector: 'app-tenant-detail',
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
      <h1>{{ tenant()!.name }}</h1>
      <div class="card">
        <p><strong>National ID:</strong> {{ tenant()!.nationalId }}</p>
        <p><strong>Phone:</strong> {{ tenant()!.phone }}</p>
        <p><strong>Email:</strong> {{ tenant()!.email }}</p>
        <p><strong>Unit:</strong> {{ unitLabel() }}</p>
        <p><strong>Status:</strong> {{ tenant()!.status }}</p>
        <button class="btn" (click)="messageTenant()">Message tenant</button>
      </div>

      @if (agreement()) {
        <div class="card">
          <h3>Rental agreement</h3>
          @if (!editing()) {
            <p><strong>Terms:</strong> {{ agreement()!.terms }}</p>
            <p><strong>Deposit:</strong> {{ agreement()!.deposit }}</p>
            <p><strong>Start date:</strong> {{ agreement()!.startDate }}</p>
            <button class="btn" (click)="editing.set(true)">Edit lease agreement</button>
          } @else {
            <div class="field">
              <label for="terms">Terms</label>
              <input id="terms" name="terms" [(ngModel)]="termsDraft" />
            </div>
            <div class="actions-row">
              <button class="btn btn-primary" (click)="saveTerms()">Save changes</button>
              <button class="btn" (click)="editing.set(false)">Cancel</button>
            </div>
          }
        </div>
      }

      <div class="module-grid" style="margin-bottom:1rem;">
        <div class="card">
          <p class="hint-text">Total due</p>
          <h2 style="color:var(--danger);">{{ totalDue() }}</h2>
        </div>
        <div class="card">
          <p class="hint-text">Total paid (lifetime)</p>
          <h2 style="color:var(--success);">{{ totalPaid() }}</h2>
        </div>
        <div class="card">
          <p class="hint-text">Total maintenance cost (tenant-borne)</p>
          <h2>{{ totalMaintenanceCost() }}</h2>
        </div>
      </div>

      <div class="card">
        <h3>Billing history</h3>
        <div class="table-scroll">
        <table>
          <thead><tr><th>Month</th><th>Rent</th><th>Utilities</th><th>Rolled over</th><th>Total</th><th>Status</th></tr></thead>
          <tbody>
            @for (i of billingHistory(); track i.id) {
              <tr>
                <td>{{ monthLabel(i.period) }}</td>
                <td>{{ i.rent }}</td>
                <td>{{ i.utilitiesTotal }}</td>
                <td>{{ i.prevUnpaidRolled }}</td>
                <td>
                  @if (i.status === 'partial') {
                    {{ i.balance }}/{{ i.amount }}
                  } @else {
                    {{ i.balance }}
                  }
                </td>
                <td><span class="badge" [class.badge-unpaid]="i.status === 'unpaid'" [class.badge-partial]="i.status === 'partial'" [class.badge-paid]="i.status === 'paid'">{{ i.status }}</span></td>
              </tr>
            } @empty {
              <tr><td colspan="6" class="hint-text">No bills yet.</td></tr>
            }
          </tbody>
        </table>
        </div>
      </div>

      <div class="card">
        <h3>Payment history</h3>
        <div class="table-scroll">
        <table>
          <thead><tr><th>Date</th><th>Amount</th><th>Method</th><th>Status</th></tr></thead>
          <tbody>
            @for (p of paymentHistory(); track p.id) {
              <tr>
                <td>{{ dateLabel(p.date) }}</td>
                <td>{{ p.amount }}</td>
                <td>{{ p.method }}</td>
                <td>
                  <span class="badge" [class.badge-paid]="p.status === 'confirmed'" [class.badge-pending]="p.status === 'pending'" [class.badge-unpaid]="p.status === 'rejected'">
                    {{ p.status }}
                  </span>
                </td>
              </tr>
            } @empty {
              <tr><td colspan="4" class="hint-text">No payments yet.</td></tr>
            }
          </tbody>
        </table>
        </div>
      </div>

      <div class="card">
        <h3>Maintenance cost history</h3>
        <div class="table-scroll">
        <table>
          <thead><tr><th>Date</th><th>Description</th><th>Bearer</th><th>Amount</th></tr></thead>
          <tbody>
            @for (m of maintenanceHistory(); track m.id) {
              <tr>
                <td>{{ m.date }}</td>
                <td>{{ m.description }}</td>
                <td>{{ m.bearer === 'tenant' ? 'Tenant' : 'Landlord' }}</td>
                <td>{{ m.amount }}</td>
              </tr>
            } @empty {
              <tr><td colspan="4" class="hint-text">No maintenance costs yet.</td></tr>
            }
          </tbody>
        </table>
        </div>
      </div>
    }
      }
    }
  `,
})
export class TenantDetailComponent implements OnInit {
  private readonly api = inject(TenantApiService);
  private readonly billingApi = inject(BillingApiService);
  private readonly unitApi = inject(UnitApiService);
  private readonly maintenanceApi = inject(MaintenanceApiService);
  private readonly messagingApi = inject(MessagingApiService);
  private readonly router = inject(Router);
  private readonly tenantId = +inject(ActivatedRoute).snapshot.paramMap.get('tenantId')!;

  readonly editing = signal(false);
  termsDraft = '';

  readonly tenant = signal<ApiTenant | null>(null);
  readonly agreement = signal<ApiRentalAgreement | null>(null);
  readonly billingHistory = signal<ApiInvoice[]>([]);
  readonly paymentHistory = signal<ApiPayment[]>([]);
  readonly maintenanceHistory = signal<ApiExpense[]>([]);

  readonly totalDue = computed(() => this.billingHistory().reduce((sum, i) => sum + i.balance, 0));
  readonly totalPaid = computed(() => this.paymentHistory().reduce((sum, p) => sum + p.amount, 0));
  readonly totalMaintenanceCost = computed(() =>
    this.maintenanceHistory()
      .filter((e) => e.bearer === 'tenant')
      .reduce((sum, e) => sum + e.amount, 0)
  );

  readonly status = signal<'loading' | 'error' | 'ready'>('loading');
  readonly error = signal<string | undefined>(undefined);

  async ngOnInit(): Promise<void> {
    this.status.set('loading');
    try {
      const [tenant, agreement, invoices, payments, expenses] = await Promise.all([
        this.api.get(this.tenantId),
        this.api.agreementFor(this.tenantId),
        this.billingApi.invoicesForTenant(this.tenantId),
        this.billingApi.paymentsForTenant(this.tenantId),
        this.maintenanceApi.expensesForTenant(this.tenantId),
        this.unitApi.units().length ? Promise.resolve() : this.unitApi.load(),
      ]);
      this.tenant.set(tenant);
      this.agreement.set(agreement);
      this.termsDraft = agreement?.terms ?? '';
      this.billingHistory.set(invoices);
      this.paymentHistory.set(payments);
      this.maintenanceHistory.set(
        expenses.filter((e) => e.category === 'Maintenance').sort((a, b) => b.date.localeCompare(a.date))
      );
      this.status.set('ready');
    } catch {
      this.error.set('Check your connection and try again.');
      this.status.set('error');
    }
  }

  monthLabel(period: string): string {
    return periodLabel(period);
  }

  dateLabel(date: string): string {
    return date.slice(0, 10);
  }

  unitLabel(): string {
    return this.unitApi.units().find((u) => u.id === this.tenant()?.unitId)?.unitNumber ?? '—';
  }

  async saveTerms(): Promise<void> {
    const a = this.agreement();
    if (!a) return;
    const updated = await this.api.updateAgreement(this.tenantId, this.termsDraft);
    this.agreement.set(updated);
    this.editing.set(false);
  }

  async messageTenant(): Promise<void> {
    await this.messagingApi.loadConversationsForTenant(this.tenantId);
    let conversation = this.messagingApi.conversations()[0];
    if (!conversation) {
      conversation = await this.messagingApi.createConversation(this.tenantId, this.tenant()!.name);
    }
    this.router.navigate(['/landlord/messages', conversation.id]);
  }
}
