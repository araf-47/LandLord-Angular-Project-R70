import { Injectable, effect, signal } from '@angular/core';

const STORAGE_KEY = 'landlord-theme';

@Injectable({ providedIn: 'root' })
export class ThemeService {
  readonly theme = signal<'light' | 'dark'>((localStorage.getItem(STORAGE_KEY) as 'light' | 'dark') ?? 'light');

  constructor() {
    effect(() => {
      const theme = this.theme();
      document.documentElement.setAttribute('data-theme', theme);
      localStorage.setItem(STORAGE_KEY, theme);
    });
  }

  toggle(): void {
    this.theme.update((t) => (t === 'light' ? 'dark' : 'light'));
  }
}
