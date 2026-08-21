import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';

export interface ApiProperty {
  id: number;
  name: string;
  address: string;
  district: string | null;
  area: string | null;
  propertyType: 'apartment' | 'room' | 'office' | null;
  landlordId: number | null;
  createdAt: string;
}

const API_BASE = 'http://localhost:8080/api/properties';

@Injectable({ providedIn: 'root' })
export class PropertyApiService {
  private readonly http = inject(HttpClient);

  readonly properties = signal<ApiProperty[]>([]);

  async load(): Promise<void> {
    const result = await firstValueFrom(this.http.get<ApiProperty[]>(API_BASE));
    this.properties.set(result);
  }

  async create(
    name: string,
    address: string,
    district: string | null,
    area: string | null,
    propertyType: ApiProperty['propertyType']
  ): Promise<ApiProperty> {
    const created = await firstValueFrom(
      this.http.post<ApiProperty>(API_BASE, { name, address, district, area, propertyType, landlordId: null })
    );
    this.properties.update((list) => [...list, created]);
    return created;
  }

  async get(id: number): Promise<ApiProperty> {
    return firstValueFrom(this.http.get<ApiProperty>(`${API_BASE}/${id}`));
  }

  async update(
    id: number,
    name: string,
    address: string,
    district: string | null,
    area: string | null,
    propertyType: ApiProperty['propertyType']
  ): Promise<ApiProperty> {
    const updated = await firstValueFrom(
      this.http.put<ApiProperty>(`${API_BASE}/${id}`, { name, address, district, area, propertyType, landlordId: null })
    );
    this.properties.update((list) => list.map((p) => (p.id === id ? updated : p)));
    return updated;
  }

  async delete(id: number): Promise<void> {
    await firstValueFrom(this.http.delete<void>(`${API_BASE}/${id}`));
    this.properties.update((list) => list.filter((p) => p.id !== id));
  }
}
