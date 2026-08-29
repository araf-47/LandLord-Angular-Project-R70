import { Component, inject } from '@angular/core';
import { ToastService } from './toast.service';

@Component({
  selector: 'app-toast-host',
  standalone: true,
  template: `
    <div class="toast-host" aria-live="polite">
      @for (toast of toastService.toasts(); track toast.id) {
        <div class="toast" [class.toast-error]="toast.type === 'error'" [class.toast-success]="toast.type === 'success'">
          <span>{{ toast.message }}</span>
          <button type="button" class="toast-dismiss" aria-label="Dismiss notification" (click)="toastService.dismiss(toast.id)">&times;</button>
        </div>
      }
    </div>
  `,
  styles: [
    `
      .toast-host {
        position: fixed;
        top: 1rem;
        right: 1rem;
        z-index: 1000;
        display: flex;
        flex-direction: column;
        gap: 0.5rem;
        max-width: 320px;
      }
      .toast {
        display: flex;
        align-items: center;
        justify-content: space-between;
        gap: 0.75rem;
        padding: 0.75rem 1rem;
        border-radius: var(--radius-sm, 6px);
        box-shadow: var(--shadow-md, 0 10px 24px rgba(15, 23, 42, 0.1));
        background: var(--surface, #fff);
        border: 1px solid var(--border, #e2e8f0);
        border-left: 4px solid var(--text-muted, #64748b);
        font-size: 0.9rem;
        color: var(--text, #1e293b);
      }
      .toast-success {
        border-left-color: var(--success, #16a34a);
      }
      .toast-error {
        border-left-color: var(--danger, #dc2626);
      }
      .toast-dismiss {
        background: none;
        border: none;
        cursor: pointer;
        font-size: 1.1rem;
        line-height: 1;
        color: var(--text-muted, #64748b);
        padding: 0;
      }
    `,
  ],
})
export class ToastHostComponent {
  readonly toastService = inject(ToastService);
}
