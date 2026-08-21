import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';

export interface ApiMarketplaceRequest {
  id: number;
  unitId: number;
  applicantName: string;
  tenantId: number | null;
  status: 'pending' | 'approved' | 'rejected';
  createdAt: string;
}

const BASE = 'http://localhost:8080/api/marketplace-requests';

@Injectable({ providedIn: 'root' })
export class MarketplaceApiService {
  private readonly http = inject(HttpClient);

  readonly requests = signal<ApiMarketplaceRequest[]>([]);

  async load(): Promise<void> {
    const result = await firstValueFrom(this.http.get<ApiMarketplaceRequest[]>(BASE));
    this.requests.set(result);
  }

  async get(id: number): Promise<ApiMarketplaceRequest> {
    return firstValueFrom(this.http.get<ApiMarketplaceRequest>(`${BASE}/${id}`));
  }

  async decide(id: number, status: 'approved' | 'rejected'): Promise<ApiMarketplaceRequest> {
    const updated = await firstValueFrom(this.http.put<ApiMarketplaceRequest>(`${BASE}/${id}/status`, { status }));
    this.requests.update((list) => list.map((r) => (r.id === id ? updated : r)));
    return updated;
  }
}
