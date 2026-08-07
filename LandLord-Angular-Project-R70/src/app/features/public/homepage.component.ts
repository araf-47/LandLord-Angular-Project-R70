import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-homepage',
  standalone: true,
  imports: [RouterLink],
  template: `
    <div class="public-content">
      <div class="hero hero-split">
        <div class="hero-copy">
          <h1>Run your rental properties without the spreadsheet chaos</h1>
          <p>
            Properties, tenants, rent collection, maintenance, and a real cash-book ledger —
            all in one place built for landlords, not accountants.
          </p>
          <div class="hero-actions">
            <a class="btn btn-primary btn-lg" routerLink="/auth/signup">Get Started Free</a>
            <a class="btn btn-lg" routerLink="/auth/login" style="background:rgba(255,255,255,0.15); border-color:rgba(255,255,255,0.4); color:#fff;">Log in</a>
          </div>
        </div>

        <div class="hero-preview-card" aria-hidden="true">
          <div class="hero-preview-title">Ledger — This Month</div>
          <div class="hero-preview-stats">
            <div>
              <span class="stat-value" style="color:var(--success);">৳48,000</span>
              <span class="stat-label">In</span>
            </div>
            <div>
              <span class="stat-value" style="color:var(--danger);">৳6,200</span>
              <span class="stat-label">Out</span>
            </div>
            <div>
              <span class="stat-value">৳41,800</span>
              <span class="stat-label">Net</span>
            </div>
          </div>
          <div class="hero-preview-row">
            <span>Payment — Rahim Uddin</span>
            <span class="badge badge-paid">+15,000</span>
          </div>
          <div class="hero-preview-row">
            <span>Expense — Maintenance</span>
            <span class="badge badge-unpaid">-1,200</span>
          </div>
          <div class="hero-preview-row">
            <span>Unit A-102</span>
            <span class="badge badge-vacant">vacant</span>
          </div>
        </div>
      </div>

      <h2>Everything you need, already built</h2>
      <div class="module-grid">
        @for (f of features; track f.title) {
          <div class="module-tile">
            <div class="module-title">{{ f.title }}</div>
            <p>{{ f.desc }}</p>
          </div>
        }
      </div>

      <div class="callout-banner">
        <h2>Vacant unit? It's already advertised.</h2>
        <p>
          The moment a unit goes vacant, LandLord auto-posts it to <strong>BariVara.com</strong> —
          our connected rental marketplace. No printed signs, no word-of-mouth-only reach.
          Your listing goes out to renters searching online, far beyond your neighborhood,
          with zero extra work on your part.
        </p>
      </div>

      <h2>Built for landlords managing one unit or a hundred</h2>
      <p>
        Whether it's a single family home or a full apartment building, LandLord scales with
        your portfolio — the same billing, tenant, and maintenance tools either way.
      </p>

      <div class="cta-banner">
        <h2>Ready to get started?</h2>
        <p>Create your account in minutes — no credit card, no setup fees.</p>
        <a class="btn btn-primary btn-lg" routerLink="/auth/signup">Get Started Free</a>
      </div>
    </div>
  `,
})
export class HomepageComponent {
  readonly features = [
    { title: 'Property & Units', desc: 'Track every property and unit, vacant or occupied, in one list.' },
    { title: 'Tenant Management', desc: 'Register tenants, manage lease agreements, handle move-outs cleanly.' },
    { title: 'Monthly Billing', desc: 'Automatic monthly bills with rollover balances — never lose track of who owes what.' },
    { title: 'Cash-Book Ledger', desc: 'Every payment in, every expense out, one running balance.' },
    { title: 'Maintenance Tracking', desc: 'Log issues, track resolution, and know exactly what repairs cost.' },
    { title: 'Marketplace Reach', desc: 'Vacant units auto-post to BariVara.com for wider exposure.' },
  ];
}
