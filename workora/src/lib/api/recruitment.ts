import apiClient from './client';
import type { ApiResponse, PageResponse } from './types';

const BASE = '/api/recruitment/api/v1';

export interface RecruitmentPost {
  id: string;
  title: string;
  description?: string;
  requirements?: string;
  companyId?: string;
  projectId?: string;
  role?: string;
  status?: 'OPEN' | 'CLOSED' | 'DRAFT';
  applicationCount?: number;
  createdAt?: string;
  closedAt?: string;
}

export interface Application {
  id: string;
  postId: string;
  applicantUserId: string;
  applicantName?: string;
  applicantEmail?: string;
  coverLetter?: string;
  status?: 'PENDING' | 'UNDER_REVIEW' | 'MANAGER_APPROVED' | 'OWNER_APPROVED' | 'REJECTED' | 'WITHDRAWN';
  managerReview?: string;
  ownerApproval?: string;
  appliedAt?: string;
  updatedAt?: string;
}

export interface CreateRecruitmentPostRequest {
  title: string;
  description?: string;
  requirements?: string;
  role?: string;
  projectId?: string;
}

export interface ApplyRequest {
  coverLetter?: string;
}

export interface ManagerReviewRequest {
  decision: 'APPROVE' | 'REJECT';
  notes?: string;
}

export interface OwnerApprovalRequest {
  decision: 'APPROVE' | 'REJECT';
  notes?: string;
}

export const recruitmentApi = {
  // ── Posts ─────────────────────────────────────────────────────────────────

  createPost: async (data: CreateRecruitmentPostRequest): Promise<ApiResponse<RecruitmentPost>> => {
    const res = await apiClient.post<ApiResponse<RecruitmentPost>>(`${BASE}/recruitment-posts`, data);
    return res.data;
  },

  listPosts: async (params?: {
    companyId?: string;
    page?: number;
    size?: number;
  }): Promise<ApiResponse<PageResponse<RecruitmentPost>>> => {
    const path = params?.companyId
      ? `${BASE}/companies/${params.companyId}/recruitment-posts`
      : `${BASE}/public/recruitment-posts`;
    const res = await apiClient.get<ApiResponse<RecruitmentPost[]>>(path);
    const posts = res.data.data;
    return {
      ...res.data,
      data: {
        content: posts,
        number: params?.page ?? 0,
        size: params?.size ?? posts.length,
        totalElements: posts.length,
        totalPages: posts.length > 0 ? 1 : 0,
      },
    };
  },

  getPost: async (postId: string): Promise<ApiResponse<RecruitmentPost>> => {
    const res = await apiClient.get<ApiResponse<RecruitmentPost>>(`${BASE}/recruitment-posts/${postId}`);
    return res.data;
  },

  updatePost: async (postId: string, data: Partial<CreateRecruitmentPostRequest>): Promise<ApiResponse<RecruitmentPost>> => {
    const res = await apiClient.patch<ApiResponse<RecruitmentPost>>(`${BASE}/recruitment-posts/${postId}`, data);
    return res.data;
  },

  publishPost: async (postId: string): Promise<ApiResponse<RecruitmentPost>> => {
    const res = await apiClient.post<ApiResponse<RecruitmentPost>>(`${BASE}/recruitment-posts/${postId}/publish`);
    return res.data;
  },

  deletePost: async (postId: string): Promise<ApiResponse<void>> => {
    const res = await apiClient.delete<ApiResponse<void>>(`${BASE}/recruitment-posts/${postId}`);
    return res.data;
  },

  closePost: async (postId: string): Promise<ApiResponse<RecruitmentPost>> => {
    const res = await apiClient.post<ApiResponse<RecruitmentPost>>(`${BASE}/recruitment-posts/${postId}/close`);
    return res.data;
  },

  // ── Applications ──────────────────────────────────────────────────────────

  applyToPost: async (postId: string, data: ApplyRequest): Promise<ApiResponse<Application>> => {
    const res = await apiClient.post<ApiResponse<Application>>(
      `${BASE}/recruitment-posts/${postId}/applications`,
      data
    );
    return res.data;
  },

  listApplications: async (postId: string): Promise<ApiResponse<Application[]>> => {
    const res = await apiClient.get<ApiResponse<Application[]>>(
      `${BASE}/recruitment-posts/${postId}/applications`
    );
    return res.data;
  },

  getApplication: async (applicationId: string): Promise<ApiResponse<Application>> => {
    const res = await apiClient.get<ApiResponse<Application>>(`${BASE}/applications/${applicationId}`);
    return res.data;
  },

  updateApplicationStatus: async (applicationId: string, data: { status: string }): Promise<ApiResponse<Application>> => {
    const res = await apiClient.patch<ApiResponse<Application>>(`${BASE}/applications/${applicationId}/status`, data);
    return res.data;
  },

  managerReview: async (applicationId: string, data: ManagerReviewRequest): Promise<ApiResponse<Application>> => {
    return recruitmentApi.updateApplicationStatus(applicationId, {
      status: data.decision === 'APPROVE' ? 'MANAGER_APPROVED' : 'REJECTED',
    });
  },

  ownerApproval: async (applicationId: string, data: OwnerApprovalRequest): Promise<ApiResponse<Application>> => {
    return recruitmentApi.updateApplicationStatus(applicationId, {
      status: data.decision === 'APPROVE' ? 'OWNER_APPROVED' : 'REJECTED',
    });
  },

  listMyApplications: async (): Promise<ApiResponse<Application[]>> => {
    const res = await apiClient.get<ApiResponse<Application[]>>(`${BASE}/applications/me`);
    return res.data;
  },

  addApplicationFile: async (applicationId: string, data: { fileName: string; fileUrl: string }): Promise<ApiResponse<unknown>> => {
    const res = await apiClient.post<ApiResponse<unknown>>(`${BASE}/applications/${applicationId}/files`, data);
    return res.data;
  },

  listApplicationFiles: async (applicationId: string): Promise<ApiResponse<unknown[]>> => {
    const res = await apiClient.get<ApiResponse<unknown[]>>(`${BASE}/applications/${applicationId}/files`);
    return res.data;
  },

  withdrawApplication: async (applicationId: string): Promise<ApiResponse<Application>> => {
    const res = await apiClient.patch<ApiResponse<Application>>(`${BASE}/applications/${applicationId}/status`, {
      status: 'WITHDRAWN',
    });
    return res.data;
  },
};
