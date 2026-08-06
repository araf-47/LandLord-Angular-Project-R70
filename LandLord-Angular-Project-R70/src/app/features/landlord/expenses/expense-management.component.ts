import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ExpenseRecord, MockDataService, nextId } from '../../../core/mock-data.service';

@Component({
  selector: 'app-expense-management',
  standalone: true,
  imports: [FormsModule],
  template: `
    <h1>Expenses</h1>

    <div class="card" style="max-width:520px;">
      <h3>Log expense</h3>
      <div class="field">
        <label for="tag">Tag</label>
        <select id="tag" name="tag" [(ngModel)]="tag">
          <option value="property">Property</option>
          <option value="tenant">Tenant</option>
        </select>
      </div>
      <div class="field">
        <label for="amount">Amount</label>
        <input id="amount" type="number" name="amount" [(ngModel)]="amount" />
      </div>
      <div class="field">
        <label for="category">Category</label>
        <input id="category" name="category" [(ngModel)]="category" placeholder="e.g. Repairs, Utilities" />
      </div>
      <div class="field">
        <label for="description">Description</label>
        <input id="description" name="description" [(ngModel)]="description" />
      </div>
      <div class="actions-row">
        <button class="btn btn-primary" (click)="save()">Save expense</button>
      </div>
    </div>

    <div class="card">
      <table>
        <thead><tr><th>Category</th><th>Description</th><th>Tag</th><th>Amount</th></tr></thead>
        <tbody>
          @for (e of data.expenses(); track e.id) {
            <tr>
              <td>{{ e.category }}</td>
              <td>{{ e.description }}</td>
              <td>{{ e.tag }}</td>
              <td>{{ e.amount }}</td>
            </tr>
          }
        </tbody>
      </table>
    </div>
  `,
})
export class ExpenseManagementComponent {
  protected readonly data = inject(MockDataService);

  tag: ExpenseRecord['tag'] = 'property';
  amount = 0;
  category = '';
  description = '';

  save(): void {
    if (!this.amount || !this.category) return;
    this.data.expenses.update((list) => [
      ...list,
      { id: nextId('exp'), category: this.category, description: this.description, amount: this.amount, tag: this.tag },
    ]);
    this.amount = 0;
    this.category = '';
    this.description = '';
  }
}
