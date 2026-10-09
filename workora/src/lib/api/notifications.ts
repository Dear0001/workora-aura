import apiClient from './client';
import type { ApiResponse } from './types';

const BASE = '/api/notification/api/v1';

export interface Notification {
  id: string;
  type?: string;
  title?: string;
  message: string;
  read: boolean;
  archived?: boolean;
  occurredAt?: string;
  createdAt?: string;
  meta?: Record<string, string>;
}

export interface NotificationPreferences {
  emailEnabled: boolean;
  pushEnabled: boolean;
  taskAssigned?: boolean;
  bugReported?: boolean;
  projectUpdated?: boolean;
  invitationReceived?: boolean;
  paymentReceived?: boolean;
}

export interface UnreadCountResponse {
  unread: number;
}

export const notificationsApi = {
  createNotification: async (data: { type: string; title: string; message: string }): Promise<ApiResponse<Notification>> => {
    const res = await apiClient.post<ApiResponse<Notification>>(`${BASE}/notifications`, data);
    return res.data;
  },

  getUnreadCount: async (): Promise<ApiResponse<UnreadCountResponse>> => {
    const res = await apiClient.get<ApiResponse<UnreadCountResponse>>(
      `${BASE}/notifications/unread-count`
    );
    return res.data;
  },

  listNotifications: async (params?: {
    page?: number;
    size?: number;
    read?: boolean;
  }): Promise<ApiResponse<Notification[]>> => {
    const res = await apiClient.get<ApiResponse<Notification[]>>(`${BASE}/notifications`, { params });
    return res.data;
  },

  getNotification: async (notificationId: string): Promise<ApiResponse<Notification>> => {
    const res = await apiClient.get<ApiResponse<Notification>>(`${BASE}/notifications/${notificationId}`);
    return res.data;
  },

  markAsRead: async (notificationId: string): Promise<ApiResponse<Notification>> => {
    const res = await apiClient.patch<ApiResponse<Notification>>(
      `${BASE}/notifications/${notificationId}/read`
    );
    return res.data;
  },

  archiveNotification: async (notificationId: string): Promise<ApiResponse<Notification>> => {
    const res = await apiClient.patch<ApiResponse<Notification>>(
      `${BASE}/notifications/${notificationId}/archive`
    );
    return res.data;
  },

  deleteNotification: async (notificationId: string): Promise<ApiResponse<string>> => {
    const res = await apiClient.delete<ApiResponse<string>>(`${BASE}/notifications/${notificationId}`);
    return res.data;
  },

  getPreferences: async (): Promise<ApiResponse<NotificationPreferences>> => {
    const res = await apiClient.get<ApiResponse<NotificationPreferences>>(
      `${BASE}/notification-preferences`
    );
    return res.data;
  },

  updatePreferences: async (data: Partial<NotificationPreferences>): Promise<ApiResponse<NotificationPreferences>> => {
    const res = await apiClient.put<ApiResponse<NotificationPreferences>>(
      `${BASE}/notification-preferences`,
      data
    );
    return res.data;
  },

  sendTestEmail: async (): Promise<ApiResponse<string>> => {
    const res = await apiClient.post<ApiResponse<string>>(`${BASE}/notifications/test-email`);
    return res.data;
  },
};
