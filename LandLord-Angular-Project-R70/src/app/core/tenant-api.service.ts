import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';

export interface ApiTenant {
  id: number;
  name: string;
  phone: string;
  email: string;
  nationalId: string;
  unitId: number | null;
  status: 'active' | 'inactive';
}

export interface ApiRentalAgreement {
  id: number;
  tenantId: number;
  unitId: number;
  startDate: string;
  terms: string;
  deposit: number;
}

export interface RegisterTenantRequest {
  name: string;
  phone: string;
  email: string;
  nationalId: string;
  unitId: number;
  terms: string;
  deposit: number;
}

const API_BASE = 'http://localhost:8080/api/tenants';

@Injectable({ providedIn: 'root' })
export class TenantApiService {
  private readonly http = inject(HttpClient);

  readonly tenants = signal<ApiTenant[]>([]);

  async load(): Promise<void> {
    const result = await firstValueFrom(this.http.get<ApiTenant[]>(API_BASE));
    this.tenants.set(result);
  }

  async get(id: number): Promise<ApiTenant> {
    return firstValueFrom(this.http.get<ApiTenant>(`${API_BASE}/${id}`));
  }

  async agreementFor(tenantId: number): Promise<ApiRentalAgreement | null> {
    try {
      return await firstValueFrom(this.http.get<ApiRentalAgreement>(`${API_BASE}/${tenantId}/agreement`));
    } catch {
      return null;
    }
  }

  async activeByNationalId(nationalId: string): Promise<ApiTenant | null> {
    try {
      return await firstValueFrom(
        this.http.get<ApiTenant>(`${API_BASE}/active-by-nid`, { params: { nationalId } })
      );
    } catch {
      return null;
    }
  }

  async register(request: RegisterTenantRequest): Promise<ApiTenant> {
    const created = await firstValueFrom(this.http.post<ApiTenant>(`${API_BASE}/register`, request));
    this.tenants.update((list) => [...list, created]);
    return created;
  }

  async moveOut(id: number): Promise<{ outstandingBalance: number }> {
    const result = await firstValueFrom(
      this.http.post<{ outstandingBalance: number }>(`${API_BASE}/${id}/move-out`, {})
    );
    this.tenants.update((list) =>
      list.map((t) => (t.id === id ? { ...t, status: 'inactive', unitId: null } : t))
    );
    return result;
  }
}
