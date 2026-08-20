import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';

export interface ApiUnit {
  id: number;
  propertyId: number;
  unitNumber: string;
  rent: number;
  status: 'vacant' | 'occupied';
  adPaused: boolean;
}

const API_BASE = 'http://localhost:8080/api/units';

@Injectable({ providedIn: 'root' })
export class UnitApiService {
  private readonly http = inject(HttpClient);

  readonly units = signal<ApiUnit[]>([]);

  async load(propertyId?: number): Promise<void> {
    const url = propertyId == null ? API_BASE : `${API_BASE}?propertyId=${propertyId}`;
    const result = await firstValueFrom(this.http.get<ApiUnit[]>(url));
    this.units.set(result);
  }

  async get(id: number): Promise<ApiUnit> {
    return firstValueFrom(this.http.get<ApiUnit>(`${API_BASE}/${id}`));
  }

  async create(propertyId: number, unitNumber: string, rent: number, status: ApiUnit['status']): Promise<ApiUnit> {
    const created = await firstValueFrom(
      this.http.post<ApiUnit>(API_BASE, { propertyId, unitNumber, rent, status })
    );
    this.units.update((list) => [...list, created]);
    return created;
  }

  async update(id: number, propertyId: number, unitNumber: string, rent: number, status: ApiUnit['status']): Promise<ApiUnit> {
    const updated = await firstValueFrom(
      this.http.put<ApiUnit>(`${API_BASE}/${id}`, { propertyId, unitNumber, rent, status })
    );
    this.units.update((list) => list.map((u) => (u.id === id ? updated : u)));
    return updated;
  }

  async pauseAd(id: number): Promise<ApiUnit> {
    const updated = await firstValueFrom(this.http.put<ApiUnit>(`${API_BASE}/${id}/ad-pause`, {}));
    this.units.update((list) => list.map((u) => (u.id === id ? updated : u)));
    return updated;
  }

  async repostAd(id: number): Promise<ApiUnit> {
    const updated = await firstValueFrom(this.http.put<ApiUnit>(`${API_BASE}/${id}/ad-repost`, {}));
    this.units.update((list) => list.map((u) => (u.id === id ? updated : u)));
    return updated;
  }

  async delete(id: number): Promise<void> {
    await firstValueFrom(this.http.delete<void>(`${API_BASE}/${id}`));
    this.units.update((list) => list.filter((u) => u.id !== id));
  }
}
