import { ChangeDetectionStrategy, Component, Input, computed, signal } from '@angular/core';

export interface BarChartPoint {
  label: string;
  collected: number;
  expenses: number;
}

const CHART_HEIGHT = 140;

@Component({
  selector: 'app-bar-chart',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="bar-chart">
      <svg [attr.viewBox]="'0 0 ' + width() + ' ' + height" preserveAspectRatio="none" class="bar-chart-svg">
        @for (bar of bars(); track bar.label) {
          <rect [attr.x]="bar.x" [attr.y]="height - bar.collectedH" [attr.width]="barWidth()" [attr.height]="bar.collectedH" class="bar-fill bar-success" />
          <rect [attr.x]="bar.x + barWidth() + 2" [attr.y]="height - bar.expensesH" [attr.width]="barWidth()" [attr.height]="bar.expensesH" class="bar-fill bar-danger" />
        }
      </svg>
      <div class="bar-chart-labels">
        @for (bar of bars(); track bar.label) {
          <span>{{ bar.label }}</span>
        }
      </div>
      <div class="bar-chart-legend">
        <span class="bar-chart-legend-item"><i class="bar-swatch bar-success"></i>Collected</span>
        <span class="bar-chart-legend-item"><i class="bar-swatch bar-danger"></i>Expenses</span>
      </div>
    </div>
  `,
})
export class BarChartComponent {
  @Input({ required: true }) set data(value: BarChartPoint[]) {
    this.points.set(value);
  }

  protected readonly height = CHART_HEIGHT;
  private readonly points = signal<BarChartPoint[]>([]);

  protected readonly width = computed(() => Math.max(this.points().length * 70, 140));
  protected readonly barWidth = computed(() => (this.points().length ? this.width() / this.points().length / 3 : 20));

  protected readonly bars = computed(() => {
    const points = this.points();
    const max = Math.max(1, ...points.flatMap((p) => [p.collected, p.expenses]));
    const groupWidth = this.width() / Math.max(points.length, 1);

    return points.map((p, i) => ({
      label: p.label,
      x: i * groupWidth + (groupWidth - 2 * this.barWidth()) / 2,
      collectedH: (p.collected / max) * (this.height - 4),
      expensesH: (p.expenses / max) * (this.height - 4),
    }));
  });
}
