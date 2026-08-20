import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';

export interface ApiProfile {
  id: number;
  name: string;
  email: string;
  phone: string;
}

const API_BASE = 'http://localhost:8081/api';

@Injectable({ providedIn: 'root' })
export class ProfileApiService {
  private readonly http = inject(HttpClient);

  tenant(id: number): Promise<ApiProfile> {
    return firstValueFrom(this.http.get<ApiProfile>(`${API_BASE}/tenant-profiles/${id}`));
  }

  owner(id: number): Promise<ApiProfile> {
    return firstValueFrom(this.http.get<ApiProfile>(`${API_BASE}/owner-profiles/${id}`));
  }
}
