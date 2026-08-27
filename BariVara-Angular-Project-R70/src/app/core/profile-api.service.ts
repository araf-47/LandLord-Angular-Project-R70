import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';

export interface ApiProfile {
  id: number;
  name: string;
  email: string;
  phone: string;
}

export interface SignupRequest {
  name: string;
  email: string;
  phone: string;
  password: string;
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

  /** The logged-in tenant/owner's own profile, resolved server-side from the auth token. */
  myTenantProfile(): Promise<ApiProfile> {
    return firstValueFrom(this.http.get<ApiProfile>(`${API_BASE}/tenant-profiles/me`));
  }

  myOwnerProfile(): Promise<ApiProfile> {
    return firstValueFrom(this.http.get<ApiProfile>(`${API_BASE}/owner-profiles/me`));
  }

  registerTenant(request: SignupRequest): Promise<ApiProfile> {
    return firstValueFrom(this.http.post<ApiProfile>(`${API_BASE}/tenant-profiles`, request));
  }

  registerOwner(request: SignupRequest): Promise<ApiProfile> {
    return firstValueFrom(this.http.post<ApiProfile>(`${API_BASE}/owner-profiles`, request));
  }
}
