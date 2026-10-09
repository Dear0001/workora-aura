import apiClient from './client';
import type { AdminDashboardSummary, ApiResponse } from './types';

const BASE = '/api/dashboard/api/v1/dashboard/admin';

export const adminDashboardApi = {
  getSummary: async (): Promise<ApiResponse<AdminDashboardSummary>> => {
    const response = await apiClient.get<ApiResponse<AdminDashboardSummary>>(`${BASE}/summary`);
    return response.data;
  },
};
