import { DecimalPipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ReportsService, MonthlySummaryPoint, CollectionRatePoint, PropertyOccupancy } from '../../../core/reports.service';
import {
  ReportApiService,
  IncomeStatementReport,
  ExpenseReport,
  OccupancyReport,
  TenantLedgerReport,
} from '../../../core/report-api.service';
import { PropertyApiService, ApiProperty } from '../../../core/property-api.service';
import { TenantApiService, ApiTenant } from '../../../core/tenant-api.service';
import { BarChartComponent, BarChartPoint } from '../../../shared/charts/bar-chart.component';

@Component({
  selector: 'app-reports',
  standalone: true,
  imports: [BarChartComponent, FormsModule, DecimalPipe],
  template: `
    @switch (status()) {
      @case ('loading') {
        <div class="card"><p class="hint-text">Loading reports…</p></div>
      }
      @case ('error') {
        <div class="card">
          <p class="text-danger mb-sm">Couldn't load reports. {{ error() }}</p>
          <button type="button" class="btn btn-sm" (click)="ngOnInit()">Retry</button>
        </div>
      }
      @case ('ready') {
        <h1>Reports</h1>

        <div class="card mb-lg">
          <div class="module-title mb-sm">Income &amp; expenses, last {{ monthly().length }} months</div>
          <app-bar-chart [data]="chartPoints()" />
        </div>

        <div class="card mb-lg">
          <div class="module-title mb-sm">Collection rate</div>
          <div class="stack">
            @for (r of collectionRate(); track r.period) {
              <div class="rate-row">
                <span class="rate-label">{{ r.label }}</span>
                <div class="rate-bar-track">
                  <div class="rate-bar-fill" [style.width.%]="r.rate"></div>
                </div>
                <span class="rate-value">{{ r.rate }}%</span>
              </div>
            }
          </div>
        </div>

        <div class="card mb-lg">
          <div class="module-title mb-sm">Occupancy by property</div>
          <div class="stack">
            @for (p of occupancy(); track p.propertyId) {
              <div class="rate-row">
                <span class="rate-label">{{ p.name }}</span>
                <div class="rate-bar-track">
                  <div class="rate-bar-fill" [style.width.%]="p.total ? (p.occupied / p.total) * 100 : 0"></div>
                </div>
                <span class="rate-value">{{ p.occupied }}/{{ p.total }}</span>
              </div>
            }
          </div>
        </div>

        <h2 class="mb-sm">Exportable reports</h2>

        <div class="card mb-lg">
          <div class="module-title mb-sm">Income statement</div>
          <div class="actions-row mb-sm">
            <div class="field">
              <label for="income-start">From</label>
              <input id="income-start" type="date" [(ngModel)]="incomeStart" name="incomeStart" />
            </div>
            <div class="field">
              <label for="income-end">To</label>
              <input id="income-end" type="date" [(ngModel)]="incomeEnd" name="incomeEnd" />
            </div>
            <div class="field">
              <label for="income-property">Property</label>
              <select id="income-property" name="incomeProperty" [(ngModel)]="incomeProperty">
                <option [ngValue]="undefined">All properties</option>
                @for (p of properties(); track p.id) {
                  <option [ngValue]="p.id">{{ p.name }}</option>
                }
              </select>
            </div>
            <button type="button" class="btn btn-sm" (click)="loadIncomeStatement()">Run report</button>
            <button type="button" class="btn btn-sm" (click)="downloadIncomeStatementPdf()">Download PDF</button>
            <button type="button" class="btn btn-sm" (click)="downloadIncomeStatementExcel()">Download Excel</button>
          </div>

          @if (incomeStatus() === 'loading') {
            <p class="hint-text">Loading…</p>
          }
          @if (incomeStatus() === 'error') {
            <p class="text-danger">Couldn't load the income statement.</p>
          }
          @if (incomeStatus() === 'ready' && incomeStatement()) {
            <div class="table-scroll">
              <table>
                <thead>
                  <tr>
                    <th>Property</th>
                    <th>Billed</th>
                    <th>Collected</th>
                    <th>Outstanding</th>
                    <th>Collection rate</th>
                  </tr>
                </thead>
                <tbody>
                  @for (r of incomeStatement()!.rows; track r.propertyId) {
                    <tr>
                      <td>{{ r.propertyName }}</td>
                      <td>{{ r.billed | number: '1.2-2' }}</td>
                      <td>{{ r.collected | number: '1.2-2' }}</td>
                      <td>{{ r.outstanding | number: '1.2-2' }}</td>
                      <td>{{ r.collectionRatePercent }}%</td>
                    </tr>
                  } @empty {
                    <tr><td colspan="5" class="hint-text">No invoices in this range.</td></tr>
                  }
                </tbody>
                <tfoot>
                  <tr>
                    <td><strong>Total</strong></td>
                    <td>{{ incomeStatement()!.totalBilled | number: '1.2-2' }}</td>
                    <td>{{ incomeStatement()!.totalCollected | number: '1.2-2' }}</td>
                    <td>{{ incomeStatement()!.totalOutstanding | number: '1.2-2' }}</td>
                    <td>{{ incomeStatement()!.collectionRatePercent }}%</td>
                  </tr>
                </tfoot>
              </table>
            </div>
          }
        </div>

        <div class="card mb-lg">
          <div class="module-title mb-sm">Expense report</div>
          <div class="actions-row mb-sm">
            <div class="field">
              <label for="expense-start">From</label>
              <input id="expense-start" type="date" [(ngModel)]="expenseStart" name="expenseStart" />
            </div>
            <div class="field">
              <label for="expense-end">To</label>
              <input id="expense-end" type="date" [(ngModel)]="expenseEnd" name="expenseEnd" />
            </div>
            <div class="field">
              <label for="expense-property">Property</label>
              <select id="expense-property" name="expenseProperty" [(ngModel)]="expenseProperty">
                <option [ngValue]="undefined">All properties</option>
                @for (p of properties(); track p.id) {
                  <option [ngValue]="p.id">{{ p.name }}</option>
                }
              </select>
            </div>
            <button type="button" class="btn btn-sm" (click)="loadExpenseReport()">Run report</button>
            <button type="button" class="btn btn-sm" (click)="downloadExpenseReportPdf()">Download PDF</button>
            <button type="button" class="btn btn-sm" (click)="downloadExpenseReportExcel()">Download Excel</button>
          </div>

          @if (expenseStatus() === 'loading') {
            <p class="hint-text">Loading…</p>
          }
          @if (expenseStatus() === 'error') {
            <p class="text-danger">Couldn't load the expense report.</p>
          }
          @if (expenseStatus() === 'ready' && expenseReport()) {
            <div class="table-scroll">
              <table>
                <thead>
                  <tr>
                    <th>Category</th>
                    <th>Landlord-borne</th>
                    <th>Tenant-borne</th>
                    <th>Total</th>
                    <th>Count</th>
                  </tr>
                </thead>
                <tbody>
                  @for (r of expenseReport()!.rows; track r.category) {
                    <tr>
                      <td>{{ r.category }}</td>
                      <td>{{ r.landlordAmount | number: '1.2-2' }}</td>
                      <td>{{ r.tenantAmount | number: '1.2-2' }}</td>
                      <td>{{ r.total | number: '1.2-2' }}</td>
                      <td>{{ r.count }}</td>
                    </tr>
                  } @empty {
                    <tr><td colspan="5" class="hint-text">No expenses in this range.</td></tr>
                  }
                </tbody>
                <tfoot>
                  <tr>
                    <td colspan="3"><strong>Total</strong></td>
                    <td>{{ expenseReport()!.totalAmount | number: '1.2-2' }}</td>
                    <td></td>
                  </tr>
                </tfoot>
              </table>
            </div>
          }
        </div>

        <div class="card mb-lg">
          <div class="module-title mb-sm">Occupancy report</div>
          <div class="actions-row mb-sm">
            <div class="field">
              <label for="occupancy-property">Property</label>
              <select id="occupancy-property" name="occupancyProperty" [(ngModel)]="occupancyProperty">
                <option [ngValue]="undefined">All properties</option>
                @for (p of properties(); track p.id) {
                  <option [ngValue]="p.id">{{ p.name }}</option>
                }
              </select>
            </div>
            <button type="button" class="btn btn-sm" (click)="loadOccupancyReport()">Run report</button>
            <button type="button" class="btn btn-sm" (click)="downloadOccupancyReportPdf()">Download PDF</button>
            <button type="button" class="btn btn-sm" (click)="downloadOccupancyReportExcel()">Download Excel</button>
          </div>

          @if (occupancyReportStatus() === 'loading') {
            <p class="hint-text">Loading…</p>
          }
          @if (occupancyReportStatus() === 'error') {
            <p class="text-danger">Couldn't load the occupancy report.</p>
          }
          @if (occupancyReportStatus() === 'ready' && occupancyReport()) {
            <div class="table-scroll">
              <table>
                <thead>
                  <tr>
                    <th>Property</th>
                    <th>Units</th>
                    <th>Occupied</th>
                    <th>Vacant</th>
                    <th>Rate</th>
                  </tr>
                </thead>
                <tbody>
                  @for (r of occupancyReport()!.rows; track r.propertyId) {
                    <tr>
                      <td>{{ r.propertyName }}</td>
                      <td>{{ r.totalUnits }}</td>
                      <td>{{ r.occupiedUnits }}</td>
                      <td>{{ r.vacantUnits }}</td>
                      <td>{{ r.occupancyRatePercent }}%</td>
                    </tr>
                  } @empty {
                    <tr><td colspan="5" class="hint-text">No units found.</td></tr>
                  }
                </tbody>
                <tfoot>
                  <tr>
                    <td><strong>Total</strong></td>
                    <td>{{ occupancyReport()!.totalUnits }}</td>
                    <td>{{ occupancyReport()!.totalOccupied }}</td>
                    <td>{{ occupancyReport()!.totalUnits - occupancyReport()!.totalOccupied }}</td>
                    <td>{{ occupancyReport()!.occupancyRatePercent }}%</td>
                  </tr>
                </tfoot>
              </table>
            </div>
          }
        </div>

        <div class="card">
          <div class="module-title mb-sm">Tenant ledger</div>
          <div class="actions-row mb-sm">
            <div class="field">
              <label for="ledger-tenant">Tenant</label>
              <select id="ledger-tenant" name="ledgerTenant" [(ngModel)]="ledgerTenantId">
                <option [ngValue]="undefined">Select a tenant</option>
                @for (t of tenants(); track t.id) {
                  <option [ngValue]="t.id">{{ t.name }}</option>
                }
              </select>
            </div>
            <div class="field">
              <label for="ledger-start">From</label>
              <input id="ledger-start" type="date" [(ngModel)]="ledgerStart" name="ledgerStart" />
            </div>
            <div class="field">
              <label for="ledger-end">To</label>
              <input id="ledger-end" type="date" [(ngModel)]="ledgerEnd" name="ledgerEnd" />
            </div>
            <button type="button" class="btn btn-sm" [disabled]="!ledgerTenantId" (click)="loadTenantLedger()">Run report</button>
            <button type="button" class="btn btn-sm" [disabled]="!ledgerTenantId" (click)="downloadTenantLedgerPdf()">Download PDF</button>
            <button type="button" class="btn btn-sm" [disabled]="!ledgerTenantId" (click)="downloadTenantLedgerExcel()">Download Excel</button>
          </div>

          @if (ledgerStatus() === 'loading') {
            <p class="hint-text">Loading…</p>
          }
          @if (ledgerStatus() === 'error') {
            <p class="text-danger">Couldn't load the tenant ledger.</p>
          }
          @if (ledgerStatus() === 'ready' && tenantLedger()) {
            <div class="table-scroll">
              <table>
                <thead>
                  <tr>
                    <th>Period</th>
                    <th>Invoiced</th>
                    <th>Paid</th>
                    <th>Balance</th>
                    <th>Status</th>
                    <th>Due date</th>
                  </tr>
                </thead>
                <tbody>
                  @for (r of tenantLedger()!.rows; track r.period) {
                    <tr>
                      <td>{{ r.period }}</td>
                      <td>{{ r.amount | number: '1.2-2' }}</td>
                      <td>{{ r.paid | number: '1.2-2' }}</td>
                      <td>{{ r.balance | number: '1.2-2' }}</td>
                      <td>{{ r.status }}</td>
                      <td>{{ r.dueDate ?? '-' }}</td>
                    </tr>
                  } @empty {
                    <tr><td colspan="6" class="hint-text">No invoices for this tenant.</td></tr>
                  }
                </tbody>
                <tfoot>
                  <tr>
                    <td><strong>Total</strong></td>
                    <td>{{ tenantLedger()!.totalInvoiced | number: '1.2-2' }}</td>
                    <td>{{ tenantLedger()!.totalPaid | number: '1.2-2' }}</td>
                    <td>{{ tenantLedger()!.totalOutstanding | number: '1.2-2' }}</td>
                    <td colspan="2"></td>
                  </tr>
                </tfoot>
              </table>
            </div>
          }
        </div>
      }
    }
  `,
})
export class ReportsComponent implements OnInit {
  private readonly reportsService = inject(ReportsService);
  private readonly reportApi = inject(ReportApiService);
  private readonly propertyApi = inject(PropertyApiService);
  private readonly tenantApi = inject(TenantApiService);

  readonly status = signal<'loading' | 'error' | 'ready'>('loading');
  readonly error = signal<string | undefined>(undefined);

  readonly monthly = signal<MonthlySummaryPoint[]>([]);
  readonly collectionRate = signal<CollectionRatePoint[]>([]);
  readonly occupancy = signal<PropertyOccupancy[]>([]);
  readonly properties = signal<ApiProperty[]>([]);
  readonly tenants = signal<ApiTenant[]>([]);

  incomeStart = '';
  incomeEnd = '';
  incomeProperty: number | undefined = undefined;
  readonly incomeStatus = signal<'idle' | 'loading' | 'error' | 'ready'>('idle');
  readonly incomeStatement = signal<IncomeStatementReport | undefined>(undefined);

  expenseStart = '';
  expenseEnd = '';
  expenseProperty: number | undefined = undefined;
  readonly expenseStatus = signal<'idle' | 'loading' | 'error' | 'ready'>('idle');
  readonly expenseReport = signal<ExpenseReport | undefined>(undefined);

  occupancyProperty: number | undefined = undefined;
  readonly occupancyReportStatus = signal<'idle' | 'loading' | 'error' | 'ready'>('idle');
  readonly occupancyReport = signal<OccupancyReport | undefined>(undefined);

  ledgerTenantId: number | undefined = undefined;
  ledgerStart = '';
  ledgerEnd = '';
  readonly ledgerStatus = signal<'idle' | 'loading' | 'error' | 'ready'>('idle');
  readonly tenantLedger = signal<TenantLedgerReport | undefined>(undefined);

  chartPoints(): BarChartPoint[] {
    return this.monthly().map((m) => ({ label: m.label.split(' ')[0].slice(0, 3), collected: m.collected, expenses: m.expenses }));
  }

  async ngOnInit(): Promise<void> {
    this.status.set('loading');
    try {
      const [monthly, collectionRate, occupancy] = await Promise.all([
        this.reportsService.monthlySummary(),
        this.reportsService.collectionRate(),
        this.reportsService.occupancyByProperty(),
      ]);
      this.monthly.set(monthly);
      this.collectionRate.set(collectionRate);
      this.occupancy.set(occupancy);
      if (this.propertyApi.properties().length === 0) {
        await this.propertyApi.load();
      }
      this.properties.set(this.propertyApi.properties());
      if (this.tenantApi.tenants().length === 0) {
        await this.tenantApi.load();
      }
      this.tenants.set(this.tenantApi.tenants());
      this.status.set('ready');
      await Promise.all([this.loadIncomeStatement(), this.loadExpenseReport(), this.loadOccupancyReport()]);
    } catch {
      this.error.set('Check your connection and try again.');
      this.status.set('error');
    }
  }

  async loadIncomeStatement(): Promise<void> {
    this.incomeStatus.set('loading');
    try {
      const report = await this.reportApi.incomeStatement({
        startDate: this.incomeStart || undefined,
        endDate: this.incomeEnd || undefined,
        propertyId: this.incomeProperty,
      });
      this.incomeStatement.set(report);
      this.incomeStatus.set('ready');
    } catch {
      this.incomeStatus.set('error');
    }
  }

  downloadIncomeStatementPdf(): Promise<void> {
    return this.reportApi.downloadIncomeStatementPdf({
      startDate: this.incomeStart || undefined,
      endDate: this.incomeEnd || undefined,
      propertyId: this.incomeProperty,
    });
  }

  downloadIncomeStatementExcel(): Promise<void> {
    return this.reportApi.downloadIncomeStatementExcel({
      startDate: this.incomeStart || undefined,
      endDate: this.incomeEnd || undefined,
      propertyId: this.incomeProperty,
    });
  }

  async loadExpenseReport(): Promise<void> {
    this.expenseStatus.set('loading');
    try {
      const report = await this.reportApi.expenseReport({
        startDate: this.expenseStart || undefined,
        endDate: this.expenseEnd || undefined,
        propertyId: this.expenseProperty,
      });
      this.expenseReport.set(report);
      this.expenseStatus.set('ready');
    } catch {
      this.expenseStatus.set('error');
    }
  }

  downloadExpenseReportPdf(): Promise<void> {
    return this.reportApi.downloadExpenseReportPdf({
      startDate: this.expenseStart || undefined,
      endDate: this.expenseEnd || undefined,
      propertyId: this.expenseProperty,
    });
  }

  downloadExpenseReportExcel(): Promise<void> {
    return this.reportApi.downloadExpenseReportExcel({
      startDate: this.expenseStart || undefined,
      endDate: this.expenseEnd || undefined,
      propertyId: this.expenseProperty,
    });
  }

  async loadOccupancyReport(): Promise<void> {
    this.occupancyReportStatus.set('loading');
    try {
      const report = await this.reportApi.occupancyReport({ propertyId: this.occupancyProperty });
      this.occupancyReport.set(report);
      this.occupancyReportStatus.set('ready');
    } catch {
      this.occupancyReportStatus.set('error');
    }
  }

  downloadOccupancyReportPdf(): Promise<void> {
    return this.reportApi.downloadOccupancyReportPdf({ propertyId: this.occupancyProperty });
  }

  downloadOccupancyReportExcel(): Promise<void> {
    return this.reportApi.downloadOccupancyReportExcel({ propertyId: this.occupancyProperty });
  }

  async loadTenantLedger(): Promise<void> {
    if (!this.ledgerTenantId) return;
    this.ledgerStatus.set('loading');
    try {
      const report = await this.reportApi.tenantLedger({
        tenantId: this.ledgerTenantId,
        startDate: this.ledgerStart || undefined,
        endDate: this.ledgerEnd || undefined,
      });
      this.tenantLedger.set(report);
      this.ledgerStatus.set('ready');
    } catch {
      this.ledgerStatus.set('error');
    }
  }

  downloadTenantLedgerPdf(): Promise<void> {
    if (!this.ledgerTenantId) return Promise.resolve();
    return this.reportApi.downloadTenantLedgerPdf({
      tenantId: this.ledgerTenantId,
      startDate: this.ledgerStart || undefined,
      endDate: this.ledgerEnd || undefined,
    });
  }

  downloadTenantLedgerExcel(): Promise<void> {
    if (!this.ledgerTenantId) return Promise.resolve();
    return this.reportApi.downloadTenantLedgerExcel({
      tenantId: this.ledgerTenantId,
      startDate: this.ledgerStart || undefined,
      endDate: this.ledgerEnd || undefined,
    });
  }
}
