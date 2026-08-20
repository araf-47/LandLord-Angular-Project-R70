import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';

export interface ApiBookingRequest {
  id: number;
  listingId: number;
  tenantId: number;
  applicantName: string;
  message: string | null;
  status: 'pending' | 'approved' | 'rejected';
}

const API_BASE = 'http://localhost:8081/api/booking-requests';

@Injectable({ providedIn: 'root' })
export class BookingApiService {
  private readonly http = inject(HttpClient);

  readonly requests = signal<ApiBookingRequest[]>([]);

  async loadForOwner(ownerId: number): Promise<ApiBookingRequest[]> {
    const result = await firstValueFrom(this.http.get<ApiBookingRequest[]>(`${API_BASE}?ownerId=${ownerId}`));
    this.requests.set(result);
    return result;
  }

  async loadForTenant(tenantId: number): Promise<ApiBookingRequest[]> {
    const result = await firstValueFrom(this.http.get<ApiBookingRequest[]>(`${API_BASE}?tenantId=${tenantId}`));
    this.requests.set(result);
    return result;
  }

  async get(id: number): Promise<ApiBookingRequest> {
    return firstValueFrom(this.http.get<ApiBookingRequest>(`${API_BASE}/${id}`));
  }

  async create(payload: { listingId: number; tenantId: number; applicantName: string; message?: string }): Promise<ApiBookingRequest> {
    const created = await firstValueFrom(this.http.post<ApiBookingRequest>(API_BASE, payload));
    this.requests.update((list) => [...list, created]);
    return created;
  }

  async decide(id: number, status: 'approved' | 'rejected'): Promise<ApiBookingRequest> {
    const updated = await firstValueFrom(this.http.put<ApiBookingRequest>(`${API_BASE}/${id}/status`, { status }));
    this.requests.update((list) => list.map((r) => (r.id === id ? updated : r)));
    return updated;
  }
}
