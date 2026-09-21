import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { InsightsApiService } from '../../../core/insights-api.service';

interface ChatMessage {
  role: 'user' | 'assistant';
  text: string;
}

@Component({
  selector: 'app-insights',
  standalone: true,
  imports: [FormsModule],
  template: `
    <h1>AI Insights</h1>
    <p class="hint-text mb-md">Ask questions about your portfolio - occupancy, overdue tenants, this month's income and expenses.</p>

    <div class="card">
      <div class="stack mb-md">
        @for (m of messages(); track $index) {
          <div class="chat-bubble" [class.chat-bubble-user]="m.role === 'user'">
            <strong>{{ m.role === 'user' ? 'You' : 'AI' }}:</strong> {{ m.text }}
          </div>
        } @empty {
          <p class="hint-text">No questions yet. Try "Which tenants are overdue?" or "Summarize this month's expenses."</p>
        }
        @if (sending()) {
          <p class="hint-text">Thinking…</p>
        }
      </div>

      @if (error()) {
        <p class="text-danger mb-sm">{{ error() }}</p>
      }

      <div class="actions-row">
        <input
          type="text"
          placeholder="Ask a question about your portfolio"
          [ngModel]="question()"
          (ngModelChange)="question.set($event)"
          (keydown.enter)="ask()"
          name="question"
          [disabled]="sending()"
        />
        <button type="button" class="btn btn-primary btn-sm" [disabled]="sending() || !question().trim()" (click)="ask()">
          Ask
        </button>
      </div>
    </div>
  `,
})
export class InsightsComponent {
  private readonly insightsApi = inject(InsightsApiService);

  readonly messages = signal<ChatMessage[]>([]);
  readonly question = signal('');
  readonly sending = signal(false);
  readonly error = signal<string | undefined>(undefined);

  async ask(): Promise<void> {
    const question = this.question().trim();
    if (!question || this.sending()) return;

    this.messages.update((list) => [...list, { role: 'user', text: question }]);
    this.question.set('');
    this.sending.set(true);
    this.error.set(undefined);

    try {
      const answer = await this.insightsApi.askQuestion(question);
      this.messages.update((list) => [...list, { role: 'assistant', text: answer }]);
    } catch (e) {
      this.error.set(this.errorMessage(e));
    } finally {
      this.sending.set(false);
    }
  }

  private errorMessage(e: unknown): string {
    if (e instanceof HttpErrorResponse) {
      if (e.status === 503) return "AI insights aren't set up yet - ask your admin to configure the Gemini API key.";
      if (e.status === 429) return 'AI insights are rate-limited right now - try again shortly.';
    }
    return "Couldn't get an answer. Check your connection and try again.";
  }
}
