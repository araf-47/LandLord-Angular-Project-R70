import { Component, inject } from '@angular/core';
import { ThemeService } from './theme.service';

@Component({
  selector: 'app-theme-toggle',
  standalone: true,
  template: `
    <button type="button" class="theme-toggle" [attr.aria-label]="themeService.theme() === 'dark' ? 'Switch to light mode' : 'Switch to dark mode'" (click)="themeService.toggle()">
      {{ themeService.theme() === 'dark' ? '☀️' : '🌙' }}
    </button>
  `,
  styles: [
    `
      .theme-toggle {
        position: fixed;
        bottom: 1rem;
        left: 1rem;
        z-index: 900;
        width: 40px;
        height: 40px;
        border-radius: 50%;
        border: 1px solid var(--border);
        background: var(--surface);
        box-shadow: var(--shadow-sm);
        cursor: pointer;
        font-size: 1.1rem;
        display: flex;
        align-items: center;
        justify-content: center;
      }
    `,
  ],
})
export class ThemeToggleComponent {
  protected readonly themeService = inject(ThemeService);
}
