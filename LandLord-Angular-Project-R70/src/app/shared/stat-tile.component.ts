import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-stat-tile',
  standalone: true,
  template: `
    <div class="card">
      <p class="hint-text">{{ label }}</p>
      <h2 [class.text-success]="color === 'success'" [class.text-danger]="color === 'danger'">{{ value }}</h2>
    </div>
  `,
})
export class StatTileComponent {
  @Input() label = '';
  @Input() value: string | number = '';
  @Input() color?: 'success' | 'danger';
}
