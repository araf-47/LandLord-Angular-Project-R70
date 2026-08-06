import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

@Component({
  selector: 'app-homepage',
  standalone: true,
  imports: [FormsModule, RouterLink],
  template: `
    <div class="public-content">
      <div class="hero">
        <h1>Find your next home</h1>
        <p>Search vacant apartments and rooms posted by owners and landlords.</p>
        <div class="search-bar" style="max-width:520px;">
          <input [(ngModel)]="location" name="location" placeholder="Location, e.g. Dhanmondi" (keyup.enter)="search()" />
          <button class="btn btn-primary" (click)="search()">Search</button>
        </div>
      </div>

      <h2>Continue as</h2>
      <div class="module-grid">
        <a class="module-tile" routerLink="/browse">
          <div class="module-title">Just browsing</div>
          <p>Look around without an account.</p>
        </a>
        <a class="module-tile" routerLink="/auth/signup">
          <div class="module-title">Tenant / Apartment Owner</div>
          <p>Sign up to save listings, book, or post your own ads.</p>
        </a>
        <a class="module-tile" routerLink="/auth/login">
          <div class="module-title">Already have an account?</div>
          <p>Log in — including LandLord core accounts.</p>
        </a>
      </div>
    </div>
  `,
})
export class HomepageComponent {
  private readonly router = inject(Router);
  location = '';

  search(): void {
    this.router.navigate(['/browse'], { queryParams: this.location ? { q: this.location } : {} });
  }
}
