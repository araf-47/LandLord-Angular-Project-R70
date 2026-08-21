import { Component, Input } from '@angular/core';

/**
 * Brand mark — pin+house icon plus wordmark. Picks the light or dark-background
 * SVG depending on what it's placed on: theme="dark" wherever the surrounding
 * background is dark (the sidebar), theme="light" everywhere else (topbar, auth
 * card). Inlined rather than referenced as an asset file since both SVGs are
 * under 2KB. Icon is identical between variants — only the wordmark/tld colors
 * change, since the orange badge and cream cutout already read on dark surfaces.
 */
@Component({
  selector: 'app-logo',
  standalone: true,
  template: `
    @if (theme === 'dark') {
      <svg viewBox="0 0 720 230" xmlns="http://www.w3.org/2000/svg" role="img" aria-label="BariVara.com logo" [attr.height]="height">
        <defs>
          <linearGradient id="badgeGradDark" x1="0" y1="0" x2="1" y2="1">
            <stop offset="0" stop-color="#FF8A3D" />
            <stop offset="1" stop-color="#FF5C00" />
          </linearGradient>
        </defs>
        <g transform="translate(20,6)">
          <path d="M95 0 C48 0 10 38 10 85 C10 133 55 172 95 214 C135 172 180 133 180 85 C180 38 142 0 95 0 Z" fill="url(#badgeGradDark)" />
          <path d="M95 38 L146 84 L133 84 L133 132 L102 132 L102 104 L88 104 L88 132 L57 132 L57 84 L44 84 Z" fill="#FFF6EE" />
          <rect x="87" y="110" width="16" height="22" rx="2" fill="url(#badgeGradDark)" />
        </g>
        <text x="230" y="128" font-family="'Poppins','Segoe UI','Helvetica Neue',Arial,sans-serif" font-weight="700" font-size="72" fill="#FBF3EA">BariVara</text>
        <text x="230" y="162" font-family="'Poppins','Segoe UI','Helvetica Neue',Arial,sans-serif" font-weight="500" font-size="26" letter-spacing="1" fill="#FF8A3D">.com</text>
      </svg>
    } @else {
      <svg viewBox="0 0 720 230" xmlns="http://www.w3.org/2000/svg" role="img" aria-label="BariVara.com logo" [attr.height]="height">
        <defs>
          <linearGradient id="badgeGradLight" x1="0" y1="0" x2="1" y2="1">
            <stop offset="0" stop-color="#FF8A3D" />
            <stop offset="1" stop-color="#FF5C00" />
          </linearGradient>
        </defs>
        <g transform="translate(20,6)">
          <path d="M95 0 C48 0 10 38 10 85 C10 133 55 172 95 214 C135 172 180 133 180 85 C180 38 142 0 95 0 Z" fill="url(#badgeGradLight)" />
          <path d="M95 38 L146 84 L133 84 L133 132 L102 132 L102 104 L88 104 L88 132 L57 132 L57 84 L44 84 Z" fill="#FFF6EE" />
          <rect x="87" y="110" width="16" height="22" rx="2" fill="url(#badgeGradLight)" />
        </g>
        <text x="230" y="128" font-family="'Poppins','Segoe UI','Helvetica Neue',Arial,sans-serif" font-weight="700" font-size="72" fill="#241B12">BariVara</text>
        <text x="230" y="162" font-family="'Poppins','Segoe UI','Helvetica Neue',Arial,sans-serif" font-weight="500" font-size="26" letter-spacing="1" fill="#FF6A1A">.com</text>
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
