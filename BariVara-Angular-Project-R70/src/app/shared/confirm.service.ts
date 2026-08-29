import { Injectable, signal } from '@angular/core';

export interface ConfirmRequest {
  title: string;
  body: string;
  resolve: (result: boolean) => void;
}

@Injectable({ providedIn: 'root' })
export class ConfirmService {
  readonly request = signal<ConfirmRequest | null>(null);

  confirm(title: string, body: string): Promise<boolean> {
    return new Promise((resolve) => {
      this.request.set({ title, body, resolve });
    });
  }

  respond(result: boolean): void {
    this.request()?.resolve(result);
    this.request.set(null);
  }
}
