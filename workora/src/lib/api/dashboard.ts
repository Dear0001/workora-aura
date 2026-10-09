import apiClient from './client';
import type { ApiResponse, DashboardSummary } from './types';

const BASE = '/api/dashboard/api/v1/dashboard';

export const dashboardApi = {
  getSummary: async (): Promise<ApiResponse<DashboardSummary>> => {
    const response = await apiClient.get<ApiResponse<DashboardSummary>>(`${BASE}/summary`);
    return response.data;
  },
};
