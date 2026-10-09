import apiClient from './client';
import type {
  AdminAccessRequest,
  AdminCompany,
  AdminMembershipActivity,
  AdminServiceHealth,
  AdminSubscription,
  ApiResponse,
} from './types';

interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}

const DASHBOARD = '/api/dashboard/api/v1/dashboard/admin';
const IDENTITY = '/api/identity/api/v1/admin/accounts';
const ORGANIZATION = '/api/organization/api/v1/admin';

export const adminManagementApi = {
  getHealth: async (): Promise<AdminServiceHealth[]> => {
    const response = await apiClient.get<ApiResponse<AdminServiceHealth[]>>(`${DASHBOARD}/health`);
    return response.data.data;
  },

  listAccountRequests: async (): Promise<AdminAccessRequest[]> => {
    const response = await apiClient.get<ApiResponse<AdminAccessRequest[]>>(
      `${IDENTITY}/requests?status=PENDING`
    );
    return response.data.data;
  },

  requestAdminAccess: async (request: {
    email: string;
    firstName?: string;
    lastName?: string;
  }): Promise<AdminAccessRequest> => {
    const response = await apiClient.post<ApiResponse<AdminAccessRequest>>(`${IDENTITY}/requests`, {
      ...request,
    });
    return response.data.data;
  },

  decideAccountRequest: async (
    id: string,
    decision: 'approve' | 'reject'
  ): Promise<AdminAccessRequest> => {
    const response = await apiClient.post<ApiResponse<AdminAccessRequest>>(
      `${IDENTITY}/requests/${id}/${decision}`
    );
    return response.data.data;
  },

  listCompanies: async (page = 0): Promise<PageResponse<AdminCompany>> => {
    const response = await apiClient.get<ApiResponse<PageResponse<AdminCompany>>>(
      `${ORGANIZATION}/companies?page=${page}&size=25`
    );
    return response.data.data;
  },

  setCompanyStatus: async (id: string, status: 'ACTIVE' | 'SUSPENDED'): Promise<AdminCompany> => {
    const response = await apiClient.patch<ApiResponse<AdminCompany>>(
      `${ORGANIZATION}/companies/${id}/status`,
      { status }
    );
    return response.data.data;
  },

  listSubscriptions: async (page = 0): Promise<PageResponse<AdminSubscription>> => {
    const response = await apiClient.get<ApiResponse<PageResponse<AdminSubscription>>>(
      `${ORGANIZATION}/subscriptions?page=${page}&size=25`
    );
    return response.data.data;
  },

  updateSubscription: async (
    userId: string,
    update: Pick<AdminSubscription, 'plan' | 'maxCompanies' | 'entitled'>
  ): Promise<AdminSubscription> => {
    const response = await apiClient.put<ApiResponse<AdminSubscription>>(
      `${ORGANIZATION}/subscriptions/${userId}`,
      update
    );
    return response.data.data;
  },

  listActivity: async (
    filters: {
      companyId?: string;
      userId?: string;
      action?: string;
      from?: string;
      to?: string;
    },
    page = 0
  ): Promise<PageResponse<AdminMembershipActivity>> => {
    const params = new URLSearchParams({ page: String(page), size: '25', sort: 'createdAt,desc' });
    Object.entries(filters).forEach(([key, value]) => {
      if (value) params.set(key, value);
    });
    const response = await apiClient.get<ApiResponse<PageResponse<AdminMembershipActivity>>>(
      `${ORGANIZATION}/activity?${params.toString()}`
    );
    return response.data.data;
  },
};
