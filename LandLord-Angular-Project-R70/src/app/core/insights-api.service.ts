import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';

interface ChatResponse {
  answer: string;
}

const BASE = 'http://localhost:8080/api/insights';

@Injectable({ providedIn: 'root' })
export class InsightsApiService {
  private readonly http = inject(HttpClient);

  async askQuestion(question: string): Promise<string> {
    const response = await firstValueFrom(this.http.post<ChatResponse>(`${BASE}/chat`, { question }));
    return response.answer;
  }
}
