import apiClient from './client';
import type { ApiResponse, PageResponse } from './types';

const BASE = '/api/project/api/v1';

export interface Project {
  id: string;
  companyId: string;
  name: string;
  description?: string;
  visibility?: 'INTERNAL' | 'PUBLIC' | 'PRIVATE';
  status?: 'ACTIVE' | 'ARCHIVED' | 'COMPLETED';
  priority?: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
  dueDate?: string;
  createdAt?: string;
  updatedAt?: string;
  memberCount?: number;
  phaseCount?: number;
}

export interface ProjectMember {
  userId: string;
  firstName: string;
  lastName: string;
  email: string;
  role: string;
  joinedAt?: string;
}

export interface PhaseMember {
  id?: string;
  userId: string;
  email?: string;
  addedBy?: string;
  createdAt?: string;
}

export interface Phase {
  id: string;
  projectId: string;
  name: string;
  description?: string;
  status?: 'DRAFT' | 'PUBLISHED' | 'CLOSED' | 'ARCHIVED';
  startDate?: string;
  endDate?: string;
  createdAt?: string;
  memberCount?: number;
  taskCount?: number;
}

export interface BugFixJob {
  id: string;
  phaseId: string;
  title: string;
  description?: string;
  status?: 'DRAFT' | 'PUBLISHED' | 'CLOSED';
  reward?: number;
  currency?: string;
  createdAt?: string;
}

export interface CreateProjectRequest {
  name: string;
  description?: string;
  projectManagerId?: string;
}

export interface CreatePhaseRequest {
  name: string;
  description?: string;
  startDate?: string;
  endDate?: string;
}

export interface CreateBugFixJobRequest {
  title: string;
  description?: string;
  reward?: number;
  currency?: string;
}

export const projectsApi = {
  // ── Projects ──────────────────────────────────────────────────────────────

  createProject: async (departmentId: string, data: CreateProjectRequest): Promise<ApiResponse<Project>> => {
    const res = await apiClient.post<ApiResponse<Project>>(`${BASE}/departments/${departmentId}/projects`, data);
    return res.data;
  },

  listCompanyProjects: async (
    companyId: string,
    params?: { page?: number; size?: number; status?: string }
  ): Promise<ApiResponse<PageResponse<Project>>> => {
    const res = await apiClient.get<ApiResponse<PageResponse<Project>>>(
      `${BASE}/companies/${companyId}/projects`,
      { params }
    );
    return res.data;
  },

  getProject: async (projectId: string): Promise<ApiResponse<Project>> => {
    const res = await apiClient.get<ApiResponse<Project>>(`${BASE}/projects/${projectId}`);
    return res.data;
  },

  updateProject: async (projectId: string, data: Partial<CreateProjectRequest>): Promise<ApiResponse<Project>> => {
    const res = await apiClient.patch<ApiResponse<Project>>(`${BASE}/projects/${projectId}`, data);
    return res.data;
  },

  archiveProject: async (projectId: string): Promise<ApiResponse<Project>> => {
    const res = await apiClient.post<ApiResponse<Project>>(`${BASE}/projects/${projectId}/archive`);
    return res.data;
  },

  deleteProject: async (projectId: string): Promise<ApiResponse<Project>> => {
    const res = await apiClient.delete<ApiResponse<Project>>(`${BASE}/projects/${projectId}`);
    return res.data;
  },

  listProjectMembers: async (projectId: string): Promise<ApiResponse<ProjectMember[]>> => {
    const res = await apiClient.get<ApiResponse<ProjectMember[]>>(`${BASE}/projects/${projectId}/members`);
    return res.data;
  },

  assignManager: async (projectId: string, userId: string): Promise<ApiResponse<string>> => {
    const res = await apiClient.post<ApiResponse<string>>(`${BASE}/projects/${projectId}/managers/${userId}`);
    return res.data;
  },

  // ── Phases ────────────────────────────────────────────────────────────────

  createPhase: async (projectId: string, data: CreatePhaseRequest): Promise<ApiResponse<Phase>> => {
    const res = await apiClient.post<ApiResponse<Phase>>(`${BASE}/projects/${projectId}/phases`, data);
    return res.data;
  },

  listPhases: async (projectId: string): Promise<ApiResponse<Phase[]>> => {
    const res = await apiClient.get<ApiResponse<Phase[]>>(`${BASE}/projects/${projectId}/phases`);
    return res.data;
  },

  getPhase: async (phaseId: string): Promise<ApiResponse<Phase>> => {
    const res = await apiClient.get<ApiResponse<Phase>>(`${BASE}/phases/${phaseId}`);
    return res.data;
  },

  updatePhase: async (phaseId: string, data: Partial<CreatePhaseRequest>): Promise<ApiResponse<Phase>> => {
    const res = await apiClient.patch<ApiResponse<Phase>>(`${BASE}/phases/${phaseId}`, data);
    return res.data;
  },

  deletePhase: async (phaseId: string): Promise<ApiResponse<Phase>> => {
    const res = await apiClient.delete<ApiResponse<Phase>>(`${BASE}/phases/${phaseId}`);
    return res.data;
  },

  publishPhase: async (phaseId: string): Promise<ApiResponse<Phase>> => {
    const res = await apiClient.post<ApiResponse<Phase>>(`${BASE}/phases/${phaseId}/publish`);
    return res.data;
  },

  hidePhase: async (phaseId: string): Promise<ApiResponse<Phase>> => {
    const res = await apiClient.post<ApiResponse<Phase>>(`${BASE}/phases/${phaseId}/hide`);
    return res.data;
  },

  closePhase: async (phaseId: string): Promise<ApiResponse<Phase>> => {
    const res = await apiClient.post<ApiResponse<Phase>>(`${BASE}/phases/${phaseId}/close`);
    return res.data;
  },

  listPhaseMembers: async (phaseId: string): Promise<ApiResponse<PhaseMember[]>> => {
    const res = await apiClient.get<ApiResponse<PhaseMember[]>>(`${BASE}/phases/${phaseId}/members`);
    return res.data;
  },

  addPhaseMember: async (phaseId: string, userId: string): Promise<ApiResponse<PhaseMember>> => {
    const res = await apiClient.post<ApiResponse<PhaseMember>>(`${BASE}/phases/${phaseId}/members/${userId}`);
    return res.data;
  },

  removePhaseMember: async (phaseId: string, userId: string): Promise<ApiResponse<string>> => {
    const res = await apiClient.delete<ApiResponse<string>>(`${BASE}/phases/${phaseId}/members/${userId}`);
    return res.data;
  },

  // ── Bug-Fix Jobs ──────────────────────────────────────────────────────────

  createBugFixJob: async (phaseId: string, data: CreateBugFixJobRequest): Promise<ApiResponse<BugFixJob>> => {
    const res = await apiClient.post<ApiResponse<BugFixJob>>(`${BASE}/phases/${phaseId}/bug-fix-jobs`, data);
    return res.data;
  },

  listBugFixJobs: async (phaseId: string): Promise<ApiResponse<BugFixJob[]>> => {
    const res = await apiClient.get<ApiResponse<BugFixJob[]>>(`${BASE}/phases/${phaseId}/bug-fix-jobs`);
    return res.data;
  },

  getBugFixJob: async (jobId: string): Promise<ApiResponse<BugFixJob>> => {
    const res = await apiClient.get<ApiResponse<BugFixJob>>(`${BASE}/bug-fix-jobs/${jobId}`);
    return res.data;
  },

  publishBugFixJob: async (jobId: string): Promise<ApiResponse<BugFixJob>> => {
    const res = await apiClient.post<ApiResponse<BugFixJob>>(`${BASE}/bug-fix-jobs/${jobId}/publish`);
    return res.data;
  },

  updateBugFixJob: async (jobId: string, data: Partial<CreateBugFixJobRequest>): Promise<ApiResponse<BugFixJob>> => {
    const res = await apiClient.patch<ApiResponse<BugFixJob>>(`${BASE}/bug-fix-jobs/${jobId}`, data);
    return res.data;
  },

  listCompanyBugFixJobs: async (companyId: string): Promise<ApiResponse<BugFixJob[]>> => {
    const res = await apiClient.get<ApiResponse<BugFixJob[]>>(`${BASE}/companies/${companyId}/bug-fix-jobs`);
    return res.data;
  },

  listMyBugFixJobs: async (): Promise<ApiResponse<BugFixJob[]>> => {
    const res = await apiClient.get<ApiResponse<BugFixJob[]>>(`${BASE}/bug-fix-jobs/me`);
    return res.data;
  },

  selectFreelancer: async (jobId: string, userId: string): Promise<ApiResponse<BugFixJob>> => {
    const res = await apiClient.post<ApiResponse<BugFixJob>>(`${BASE}/bug-fix-jobs/${jobId}/select-freelancer`, { userId });
    return res.data;
  },

  listBugFixApplications: async (jobId: string): Promise<ApiResponse<unknown[]>> => {
    const res = await apiClient.get<ApiResponse<unknown[]>>(`${BASE}/bug-fix-jobs/${jobId}/applications`);
    return res.data;
  },

  applyToBugFixJob: async (jobId: string, data: Record<string, unknown>): Promise<ApiResponse<unknown>> => {
    const res = await apiClient.post<ApiResponse<unknown>>(`${BASE}/bug-fix-jobs/${jobId}/applications`, data);
    return res.data;
  },
};
