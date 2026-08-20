import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';

export interface ApiFavorite {
  id: number;
  tenantId: number;
  listingId: number;
}

const API_BASE = 'http://localhost:8081/api/favorites';

@Injectable({ providedIn: 'root' })
export class FavoriteApiService {
  private readonly http = inject(HttpClient);

  readonly favorites = signal<ApiFavorite[]>([]);

  async loadForTenant(tenantId: number): Promise<ApiFavorite[]> {
    const result = await firstValueFrom(this.http.get<ApiFavorite[]>(`${API_BASE}?tenantId=${tenantId}`));
    this.favorites.set(result);
    return result;
  }

  async add(tenantId: number, listingId: number): Promise<ApiFavorite> {
    const favorite = await firstValueFrom(this.http.post<ApiFavorite>(API_BASE, { tenantId, listingId }));
    this.favorites.update((list) => (list.some((f) => f.id === favorite.id) ? list : [...list, favorite]));
    return favorite;
  }

  async remove(id: number): Promise<void> {
    await firstValueFrom(this.http.delete<void>(`${API_BASE}/${id}`));
    this.favorites.update((list) => list.filter((f) => f.id !== id));
  }
}
