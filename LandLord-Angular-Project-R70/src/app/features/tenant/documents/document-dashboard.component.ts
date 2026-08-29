import { Component, inject, signal } from '@angular/core';
import { CURRENT_TENANT_ID, MockDataService } from '../../../core/mock-data.service';

@Component({
  selector: 'app-tenant-documents',
  standalone: true,
  template: `
    <h1>Documents</h1>

    @if (agreement()) {
      <div class="card">
        <h3>Active rental agreement</h3>
        <p><strong>Terms:</strong> {{ agreement()!.terms }}</p>
        <p><strong>Start date:</strong> {{ agreement()!.startDate }}</p>
      </div>
    }

    <div class="card">
      <h3>Your documents</h3>
      <div class="stack">
        @for (d of documents(); track d) {
          <p>{{ d }}</p>
        } @empty {
          <p class="hint-text">No documents uploaded yet.</p>
        }
      </div>
      <div class="field mt-md">
        <label for="upload">Upload document</label>
        <input id="upload" type="file" (change)="upload($event)" />
      </div>
    </div>
  `,
})
export class TenantDocumentDashboardComponent {
  private readonly data = inject(MockDataService);
  readonly documents = signal<string[]>(['NID copy.pdf']);

  agreement() {
    return this.data.agreements().find((a) => a.tenantId === CURRENT_TENANT_ID);
  }

  upload(event: Event): void {
    const file = (event.target as HTMLInputElement).files?.[0];
    if (file) this.documents.update((list) => [...list, file.name]);
  }
}
