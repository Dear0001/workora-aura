import apiClient from './client';
import type { ApiResponse, PageResponse } from './types';

const BASE = '/api/bug/api/v1';

export interface BugReport {
  id: string;
  phaseId?: string;
  taskId?: string;
  title: string;
  description?: string;
  severity?: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
  status?: 'OPEN' | 'TRIAGED' | 'ACCEPTED' | 'REJECTED' | 'RESOLVED' | 'DUPLICATE' | 'CLOSED';
  reportedBy?: string;
  assignedTo?: string;
  createdAt?: string;
  updatedAt?: string;
  evidenceCount?: number;
}

export interface BugEvidence {
  id: string;
  bugReportId: string;
  fileUrl?: string;
  fileName?: string;
  description?: string;
  uploadedBy?: string;
  uploadedAt?: string;
}

export interface CreateBugReportRequest {
  title: string;
  description?: string;
  severity?: string;
}

export interface TriageBugRequest {
  severity?: string;
  assignedTo?: string;
  notes?: string;
}

export const bugApi = {
  // ── Bug Reports ───────────────────────────────────────────────────────────

  createBugFromPhase: async (phaseId: string, data: CreateBugReportRequest): Promise<ApiResponse<BugReport>> => {
    const res = await apiClient.post<ApiResponse<BugReport>>(`${BASE}/phases/${phaseId}/bug-reports`, data);
    return res.data;
  },

  createBugFromTask: async (taskId: string, data: CreateBugReportRequest): Promise<ApiResponse<BugReport>> => {
    throw new Error(`Bug reports can only be created from phases; task ${taskId} is not supported by the API`);
  },

  listBugReports: async (params?: {
    page?: number;
    size?: number;
    status?: string;
    severity?: string;
  }): Promise<ApiResponse<PageResponse<BugReport>>> => {
    const res = await apiClient.get<ApiResponse<BugReport[]>>(`${BASE}/bug-reports/me`);
    return { ...res.data, data: { content: res.data.data, number: 0, size: res.data.data.length, totalElements: res.data.data.length, totalPages: res.data.data.length ? 1 : 0 } };
  },

  getBugReport: async (reportId: string): Promise<ApiResponse<BugReport>> => {
    const res = await apiClient.get<ApiResponse<BugReport>>(`${BASE}/bug-reports/${reportId}`);
    return res.data;
  },

  updateBugStatus: async (reportId: string, status: string): Promise<ApiResponse<BugReport>> => {
    const res = await apiClient.patch<ApiResponse<BugReport>>(`${BASE}/bug-reports/${reportId}/status`, { status });
    return res.data;
  },

  acceptBug: async (reportId: string): Promise<ApiResponse<BugReport>> => {
    return bugApi.updateBugStatus(reportId, 'ACCEPTED');
  },

  rejectBug: async (reportId: string, reason?: string): Promise<ApiResponse<BugReport>> => {
    return bugApi.updateBugStatus(reportId, 'REJECTED');
  },

  resolveBug: async (reportId: string): Promise<ApiResponse<BugReport>> => {
    return bugApi.updateBugStatus(reportId, 'RESOLVED');
  },

  markDuplicate: async (reportId: string, duplicateOfId?: string): Promise<ApiResponse<BugReport>> => {
    return bugApi.updateBugStatus(reportId, 'DUPLICATE');
  },

  closeBug: async (reportId: string): Promise<ApiResponse<BugReport>> => {
    return bugApi.updateBugStatus(reportId, 'CLOSED');
  },

  // ── Evidence ──────────────────────────────────────────────────────────────

  addEvidence: async (reportId: string, formData: FormData): Promise<ApiResponse<BugEvidence>> => {
    const res = await apiClient.post<ApiResponse<BugEvidence>>(
      `${BASE}/bug-reports/${reportId}/evidence`,
      formData,
      { headers: { 'Content-Type': 'multipart/form-data' } }
    );
    return res.data;
  },

  listEvidence: async (reportId: string): Promise<ApiResponse<BugEvidence[]>> => {
    const res = await apiClient.get<ApiResponse<BugEvidence[]>>(`${BASE}/bug-reports/${reportId}/evidence`);
    return res.data;
  },

  removeEvidence: async (reportId: string, evidenceId: string): Promise<ApiResponse<void>> => {
    const res = await apiClient.delete<ApiResponse<void>>(
      `${BASE}/bug-reports/${reportId}/evidence/${evidenceId}`
    );
    return res.data;
  },
};
