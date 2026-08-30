import { Component, Input, OnChanges, OnDestroy, SimpleChanges, signal } from '@angular/core';

@Component({
  selector: 'app-stat-tile',
  standalone: true,
  template: `
    <div class="card">
      <p class="hint-text">{{ label }}</p>
      <h2 [class.text-success]="color === 'success'" [class.text-danger]="color === 'danger'">{{ displayValue() }}</h2>
    </div>
  `,
})
export class StatTileComponent implements OnChanges, OnDestroy {
  @Input() label = '';
  @Input() value: string | number = '';
  @Input() color?: 'success' | 'danger';

  readonly displayValue = signal<string | number>('');

  private animationFrame?: number;

  ngOnChanges(changes: SimpleChanges): void {
    if (!changes['value']) return;
    const target = this.value;
    if (typeof target !== 'number' || this.prefersReducedMotion()) {
      this.displayValue.set(target);
      return;
    }
    const previous = changes['value'].previousValue;
    const start = typeof previous === 'number' ? previous : 0;
    this.animateTo(start, target);
  }

  ngOnDestroy(): void {
    if (this.animationFrame) cancelAnimationFrame(this.animationFrame);
  }

  private prefersReducedMotion(): boolean {
    return typeof window !== 'undefined' && !!window.matchMedia?.('(prefers-reduced-motion: reduce)').matches;
  }

  private animateTo(start: number, end: number): void {
    if (this.animationFrame) cancelAnimationFrame(this.animationFrame);
    const duration = 500;
    const startTime = performance.now();
    const step = (now: number) => {
      const progress = Math.min((now - startTime) / duration, 1);
      const eased = 1 - Math.pow(1 - progress, 3);
      this.displayValue.set(Math.round(start + (end - start) * eased));
      if (progress < 1) {
        this.animationFrame = requestAnimationFrame(step);
      } else {
        this.displayValue.set(end);
      }
    };
    this.animationFrame = requestAnimationFrame(step);
  }
}
