import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { PropertyType } from './mock-data.service';

export interface ApiListing {
  id: number;
  ownerId: number;
  unitId: number | null;
  source: 'owner' | 'landlord-linked';
  title: string;
  address: string;
  district: string;
  area: string;
  propertyType: PropertyType;
  rent: number;
  status: 'active' | 'paused' | 'taken';
  photoUrl: string | null;
}

export interface NewListing {
  ownerId: number;
  unitId?: number | null;
  title: string;
  address: string;
  district: string;
  area: string;
  propertyType: PropertyType;
  rent: number;
  photoUrl?: string | null;
}

export const API_ORIGIN = 'http://localhost:8081';
const API_BASE = `${API_ORIGIN}/api/listings`;

@Injectable({ providedIn: 'root' })
export class ListingApiService {
  private readonly http = inject(HttpClient);

  readonly listings = signal<ApiListing[]>([]);

  async search(params: { ownerId?: number; district?: string; area?: string; propertyType?: string } = {}): Promise<ApiListing[]> {
    let query = '';
    const entries = Object.entries(params).filter(([, v]) => v !== undefined && v !== '');
    if (entries.length) {
      query = '?' + entries.map(([k, v]) => `${k}=${encodeURIComponent(String(v))}`).join('&');
    }
    const result = await firstValueFrom(this.http.get<ApiListing[]>(`${API_BASE}${query}`));
    this.listings.set(result);
    return result;
  }

  async get(id: number): Promise<ApiListing> {
    return firstValueFrom(this.http.get<ApiListing>(`${API_BASE}/${id}`));
  }

  async create(payload: NewListing): Promise<ApiListing> {
    const created = await firstValueFrom(this.http.post<ApiListing>(API_BASE, payload));
    this.listings.update((list) => [...list, created]);
    return created;
  }

  async update(id: number, payload: Omit<NewListing, 'ownerId' | 'unitId'>): Promise<ApiListing> {
    const updated = await firstValueFrom(this.http.put<ApiListing>(`${API_BASE}/${id}`, payload));
    this.listings.update((list) => list.map((l) => (l.id === id ? updated : l)));
    return updated;
  }

  async setStatus(id: number, status: 'active' | 'paused' | 'taken'): Promise<ApiListing> {
    const updated = await firstValueFrom(this.http.put<ApiListing>(`${API_BASE}/${id}/status`, { status }));
    this.listings.update((list) => list.map((l) => (l.id === id ? updated : l)));
    return updated;
  }

  async delete(id: number): Promise<void> {
    await firstValueFrom(this.http.delete<void>(`${API_BASE}/${id}`));
    this.listings.update((list) => list.filter((l) => l.id !== id));
  }
}
