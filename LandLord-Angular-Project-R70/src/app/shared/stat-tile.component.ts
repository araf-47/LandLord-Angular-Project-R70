import { Component, Input, OnChanges, OnDestroy, SimpleChanges, signal } from '@angular/core';

export type StatTileColor = 'success' | 'danger' | 'warning' | 'primary';
export type StatTileIcon = 'home' | 'coins' | 'alert' | 'trend';

@Component({
  selector: 'app-stat-tile',
  standalone: true,
  template: `
    <div class="card stat-card" [class.stat-card-success]="color === 'success'"
         [class.stat-card-danger]="color === 'danger'" [class.stat-card-warning]="color === 'warning'"
         [class.stat-card-primary]="color === 'primary' || !color">
      <div class="stat-card-head">
        @if (icon) {
          <span class="stat-icon">
            @switch (icon) {
              @case ('home') {
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M3 11l9-7 9 7" /><path d="M5 10v10h14V10" /></svg>
              }
              @case ('coins') {
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><ellipse cx="12" cy="6" rx="7" ry="3" /><path d="M5 6v12c0 1.66 3.13 3 7 3s7-1.34 7-3V6" /><path d="M5 12c0 1.66 3.13 3 7 3s7-1.34 7-3" /></svg>
              }
              @case ('alert') {
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 3l10 18H2L12 3z" /><path d="M12 10v4" /><path d="M12 17h.01" /></svg>
              }
              @case ('trend') {
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M3 17l6-6 4 4 8-8" /><path d="M15 7h6v6" /></svg>
              }
            }
          </span>
        }
        <p class="hint-text">{{ label }}</p>
      </div>
      <h2 [class.text-success]="color === 'success'" [class.text-danger]="color === 'danger'">{{ displayValue() }}</h2>
    </div>
  `,
})
export class StatTileComponent implements OnChanges, OnDestroy {
  @Input() label = '';
  @Input() value: string | number = '';
  @Input() color?: StatTileColor;
  @Input() icon?: StatTileIcon;

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
