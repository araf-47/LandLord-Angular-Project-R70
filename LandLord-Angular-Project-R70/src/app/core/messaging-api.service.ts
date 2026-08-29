import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { API_ORIGIN } from './maintenance-api.service';

export interface ApiConversation {
  id: number;
  tenantId: number | null;
  withName: string;
  createdAt: string;
}

export interface ApiMessage {
  id: number;
  conversationId: number;
  senderRole: 'landlord' | 'tenant';
  text: string;
  read: boolean;
  sentAt: string;
}

export interface ApiNotification {
  id: number;
  tenantId: number | null;
  type: string | null;
  title: string;
  body: string;
  read: boolean;
  createdAt: string;
}

const BASE = `${API_ORIGIN}/api`;

@Injectable({ providedIn: 'root' })
export class MessagingApiService {
  private readonly http = inject(HttpClient);

  readonly conversations = signal<ApiConversation[]>([]);
  readonly notifications = signal<ApiNotification[]>([]);

  private sortConversations(list: ApiConversation[]): ApiConversation[] {
    return [...list].sort((a, b) => b.createdAt.localeCompare(a.createdAt) || b.id - a.id);
  }

  async loadConversations(): Promise<void> {
    const result = await firstValueFrom(this.http.get<ApiConversation[]>(`${BASE}/conversations`));
    this.conversations.set(this.sortConversations(result));
  }

  async loadConversationsForTenant(tenantId: number): Promise<void> {
    const result = await firstValueFrom(this.http.get<ApiConversation[]>(`${BASE}/conversations`, { params: { tenantId } }));
    this.conversations.set(this.sortConversations(result));
  }

  async createConversation(tenantId: number | null, withName: string): Promise<ApiConversation> {
    const created = await firstValueFrom(this.http.post<ApiConversation>(`${BASE}/conversations`, { tenantId, withName }));
    this.conversations.update((list) => [created, ...list]);
    return created;
  }

  async messagesFor(conversationId: number): Promise<ApiMessage[]> {
    return firstValueFrom(this.http.get<ApiMessage[]>(`${BASE}/conversations/${conversationId}/messages`));
  }

  async sendMessage(conversationId: number, senderRole: 'landlord' | 'tenant', text: string): Promise<ApiMessage> {
    return firstValueFrom(this.http.post<ApiMessage>(`${BASE}/conversations/${conversationId}/messages`, { senderRole, text }));
  }

  private sortNotifications(list: ApiNotification[]): ApiNotification[] {
    return [...list].sort((a, b) => b.createdAt.localeCompare(a.createdAt) || b.id - a.id);
  }

  async loadNotifications(): Promise<void> {
    const result = await firstValueFrom(this.http.get<ApiNotification[]>(`${BASE}/notifications`));
    this.notifications.set(this.sortNotifications(result));
  }

  async loadNotificationsForTenant(tenantId: number): Promise<void> {
    const result = await firstValueFrom(this.http.get<ApiNotification[]>(`${BASE}/notifications`, { params: { tenantId } }));
    this.notifications.set(this.sortNotifications(result));
  }

  async loadLandlordNotifications(): Promise<void> {
    const result = await firstValueFrom(this.http.get<ApiNotification[]>(`${BASE}/notifications`, { params: { audience: 'landlord' } }));
    this.notifications.set(this.sortNotifications(result));
  }

  async markNotificationRead(id: number): Promise<void> {
    const updated = await firstValueFrom(this.http.put<ApiNotification>(`${BASE}/notifications/${id}/read`, {}));
    this.notifications.update((list) => list.map((n) => (n.id === id ? updated : n)));
  }

  async deleteNotification(id: number): Promise<void> {
    await firstValueFrom(this.http.delete<void>(`${BASE}/notifications/${id}`));
    this.notifications.update((list) => list.filter((n) => n.id !== id));
  }
}
