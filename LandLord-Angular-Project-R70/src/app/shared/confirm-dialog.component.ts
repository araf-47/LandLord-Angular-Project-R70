import { Component, ElementRef, HostListener, effect, inject, viewChild } from '@angular/core';
import { ConfirmService } from './confirm.service';

@Component({
  selector: 'app-confirm-dialog',
  standalone: true,
  template: `
    @if (confirmService.request(); as req) {
      <div class="confirm-backdrop" (click)="respond(false)">
        <div
          class="confirm-dialog"
          role="alertdialog"
          aria-modal="true"
          [attr.aria-labelledby]="'confirm-title'"
          (click)="$event.stopPropagation()"
        >
          <h3 id="confirm-title">{{ req.title }}</h3>
          <p>{{ req.body }}</p>
          <div class="confirm-actions">
            <button type="button" class="btn btn-ghost" (click)="respond(false)">Cancel</button>
            <button #confirmBtn type="button" class="btn btn-danger" (click)="respond(true)">Confirm</button>
          </div>
        </div>
      </div>
    }
  `,
  styles: [
    `
      .confirm-backdrop {
        position: fixed;
        inset: 0;
        background: rgba(15, 23, 42, 0.45);
        display: flex;
        align-items: center;
        justify-content: center;
        z-index: 1100;
      }
      .confirm-dialog {
        background: var(--surface, #fff);
        border-radius: var(--radius, 10px);
        box-shadow: var(--shadow-md, 0 10px 24px rgba(15, 23, 42, 0.1));
        padding: 1.5rem;
        max-width: 360px;
        width: 90%;
      }
      .confirm-dialog h3 {
        margin: 0 0 0.5rem;
      }
      .confirm-dialog p {
        margin: 0 0 1.25rem;
        color: var(--text-muted, #64748b);
      }
      .confirm-actions {
        display: flex;
        justify-content: flex-end;
        gap: 0.5rem;
      }
    `,
  ],
})
export class ConfirmDialogComponent {
  readonly confirmService = inject(ConfirmService);
  private readonly confirmBtn = viewChild<ElementRef<HTMLButtonElement>>('confirmBtn');

  constructor() {
    effect(() => this.confirmBtn()?.nativeElement.focus());
  }

  @HostListener('document:keydown.escape')
  onEscape(): void {
    this.respond(false);
  }

  respond(result: boolean): void {
    this.confirmService.respond(result);
  }
}
