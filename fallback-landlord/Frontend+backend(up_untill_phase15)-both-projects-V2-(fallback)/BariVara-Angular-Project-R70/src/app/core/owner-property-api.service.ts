import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';

export interface ApiOwnerProperty {
  id: number;
  ownerId: number;
  name: string;
  address: string;
  district: string;
  area: string;
}

const API_BASE = 'http://localhost:8081/api/owner-properties';

@Injectable({ providedIn: 'root' })
export class OwnerPropertyApiService {
  private readonly http = inject(HttpClient);

  readonly properties = signal<ApiOwnerProperty[]>([]);

  async loadForOwner(ownerId: number): Promise<ApiOwnerProperty[]> {
    const result = await firstValueFrom(this.http.get<ApiOwnerProperty[]>(`${API_BASE}?ownerId=${ownerId}`));
    this.properties.set(result);
    return result;
  }

  async create(payload: Omit<ApiOwnerProperty, 'id'>): Promise<ApiOwnerProperty> {
    const created = await firstValueFrom(this.http.post<ApiOwnerProperty>(API_BASE, payload));
    this.properties.update((list) => [...list, created]);
    return created;
  }
}
