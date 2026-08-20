import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';

export interface ApiOwnerUnit {
  id: number;
  propertyId: number;
  unitNumber: string;
  rent: number;
}

const API_BASE = 'http://localhost:8081/api/owner-units';

@Injectable({ providedIn: 'root' })
export class OwnerUnitApiService {
  private readonly http = inject(HttpClient);

  readonly units = signal<ApiOwnerUnit[]>([]);

  /** Loads units across every given property (there's no ownerId filter on the
   *  unit endpoint, so callers pass the owner's own property ids). */
  async loadForProperties(propertyIds: number[]): Promise<ApiOwnerUnit[]> {
    const perProperty = await Promise.all(
      propertyIds.map((id) => firstValueFrom(this.http.get<ApiOwnerUnit[]>(`${API_BASE}?propertyId=${id}`)))
    );
    const result = perProperty.flat();
    this.units.set(result);
    return result;
  }

  async create(payload: Omit<ApiOwnerUnit, 'id'>): Promise<ApiOwnerUnit> {
    const created = await firstValueFrom(this.http.post<ApiOwnerUnit>(API_BASE, payload));
    this.units.update((list) => [...list, created]);
    return created;
  }
}
