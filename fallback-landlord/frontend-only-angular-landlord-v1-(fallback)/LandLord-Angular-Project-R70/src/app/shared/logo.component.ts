import { Component, Input } from '@angular/core';

/**
 * Brand mark, badge style. Picks the light or dark SVG depending on what
 * background it's being placed on — pass theme="dark" wherever the surrounding
 * background is dark (the sidebar), theme="light" everywhere else (topbar,
 * auth card). Inlined rather than referenced as an asset file since both SVGs
 * are under 1KB.
 */
@Component({
  selector: 'app-logo',
  standalone: true,
  template: `
    @if (theme === 'dark') {
      <svg viewBox="0 0 620 190" xmlns="http://www.w3.org/2000/svg" role="img" aria-label="LandLord Core logo" [attr.height]="height">
        <text x="16" y="120" font-family="'Nunito','Century Gothic','Segoe UI',Arial,sans-serif" font-weight="800" font-size="88" fill="#F3F7FE">LandLord</text>
        <rect x="447" y="30" width="98" height="36" rx="18" fill="#5B9CFF" />
        <text x="496" y="54" font-family="'Nunito','Century Gothic','Segoe UI',Arial,sans-serif" font-weight="800" font-size="16" letter-spacing="2" fill="#0B1C36" text-anchor="middle">CORE</text>
      </svg>
    } @else {
      <svg viewBox="0 0 620 190" xmlns="http://www.w3.org/2000/svg" role="img" aria-label="LandLord Core logo" [attr.height]="height">
        <text x="16" y="120" font-family="'Nunito','Century Gothic','Segoe UI',Arial,sans-serif" font-weight="800" font-size="88" fill="#122A52">LandLord</text>
        <rect x="447" y="30" width="98" height="36" rx="18" fill="#2F7FE0" />
        <text x="496" y="54" font-family="'Nunito','Century Gothic','Segoe UI',Arial,sans-serif" font-weight="800" font-size="16" letter-spacing="2" fill="#FFFFFF" text-anchor="middle">CORE</text>
      </svg>
    }
  `,
  styles: [
    `
      :host {
        display: inline-flex;
        align-items: center;
      }
      svg {
        width: auto;
      }
    `,
  ],
})
export class LogoComponent {
  @Input() theme: 'light' | 'dark' = 'light';
  @Input() height = 40;
}
