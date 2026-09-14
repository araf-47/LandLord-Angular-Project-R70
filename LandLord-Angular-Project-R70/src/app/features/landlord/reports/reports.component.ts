import { Component, OnInit, inject, signal } from '@angular/core';
import { ReportsService, MonthlySummaryPoint, CollectionRatePoint, PropertyOccupancy } from '../../../core/reports.service';
import { BarChartComponent, BarChartPoint } from '../../../shared/charts/bar-chart.component';

@Component({
  selector: 'app-reports',
  standalone: true,
  imports: [BarChartComponent],
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

        <div class="card">
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
      }
    }
  `,
})
export class ReportsComponent implements OnInit {
  private readonly reportsService = inject(ReportsService);

  readonly status = signal<'loading' | 'error' | 'ready'>('loading');
  readonly error = signal<string | undefined>(undefined);

  readonly monthly = signal<MonthlySummaryPoint[]>([]);
  readonly collectionRate = signal<CollectionRatePoint[]>([]);
  readonly occupancy = signal<PropertyOccupancy[]>([]);

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
      this.status.set('ready');
    } catch {
      this.error.set('Check your connection and try again.');
      this.status.set('error');
    }
  }
}
