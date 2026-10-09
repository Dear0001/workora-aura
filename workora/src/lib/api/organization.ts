import apiClient from './client';
import type {
  ApiResponse,
  Company,
  Department,
  CompanyMember,
  Invitation,
  InviteMemberRequest,
  CreateDepartmentRequest,
  PageResponse,
} from './types';

const BASE = '/api/organization/api/v1';

export interface StorageUpload {
  key: string;
  uploadUrl: string;
  expiresAt: string;
  contentType: string;
}

export const storageApi = {
  createUpload: async (
    file: File,
    namespace = 'companies'
  ): Promise<ApiResponse<StorageUpload>> => {
    const res = await apiClient.post<ApiResponse<StorageUpload>>(
      '/api/organization/api/v1/storage/uploads',
      {
        namespace,
        fileName: file.name,
        contentType: file.type,
      }
    );
    await fetch(res.data.data.uploadUrl, {
      method: 'PUT',
      headers: { 'Content-Type': file.type || 'application/octet-stream' },
      body: file,
    }).then((response) => {
      if (!response.ok) throw new Error(`Image upload failed (${response.status})`);
    });
    return res.data;
  },

  getDownloadUrl: async (key: string): Promise<string> => {
    const res = await apiClient.get<ApiResponse<{ key: string; downloadUrl: string }>>(
      '/api/organization/api/v1/storage/downloads',
      { params: { key } }
    );
    return res.data.data.downloadUrl;
  },
};

export const organizationApi = {
  // ── Companies ──────────────────────────────────────────────────────────────

  // POST /api/organization/api/v1/companies
  createCompany: async (data: Partial<Company>): Promise<ApiResponse<Company>> => {
    const res = await apiClient.post<ApiResponse<Company>>(`${BASE}/companies`, data);
    return res.data;
  },

  // GET /api/organization/api/v1/companies
  listCompanies: async (params?: {
    page?: number;
    size?: number;
  }): Promise<ApiResponse<PageResponse<Company>>> => {
    const res = await apiClient.get<ApiResponse<PageResponse<Company> | Company[]>>(
      `${BASE}/companies`,
      { params }
    );
    const data = res.data.data;
    if (Array.isArray(data)) {
      return {
        ...res.data,
        data: {
          content: data,
          number: params?.page ?? 0,
          size: params?.size ?? data.length,
          totalElements: data.length,
          totalPages: data.length > 0 ? 1 : 0,
        },
      };
    }
    return res.data as ApiResponse<PageResponse<Company>>;
  },

  // GET /api/organization/api/v1/companies/{companyId}
  getCompany: async (companyId: string): Promise<ApiResponse<Company>> => {
    const res = await apiClient.get<ApiResponse<Company>>(`${BASE}/companies/${companyId}`);
    return res.data;
  },

  // PATCH /api/organization/api/v1/companies/{companyId}
  updateCompany: async (
    companyId: string,
    data: Partial<Company>
  ): Promise<ApiResponse<Company>> => {
    const res = await apiClient.patch<ApiResponse<Company>>(`${BASE}/companies/${companyId}`, data);
    return res.data;
  },

  // DELETE /api/organization/api/v1/companies/{companyId}
  deleteCompany: async (companyId: string): Promise<ApiResponse<string>> => {
    const res = await apiClient.delete<ApiResponse<string>>(`${BASE}/companies/${companyId}`);
    return res.data;
  },

  // ── Departments ────────────────────────────────────────────────────────────

  // POST /api/organization/api/v1/companies/{companyId}/departments
  createDepartment: async (
    companyId: string,
    data: CreateDepartmentRequest
  ): Promise<ApiResponse<Department>> => {
    const res = await apiClient.post<ApiResponse<Department>>(
      `${BASE}/companies/${companyId}/departments`,
      data
    );
    return res.data;
  },

  // GET /api/organization/api/v1/companies/{companyId}/departments
  listDepartments: async (companyId: string): Promise<ApiResponse<Department[]>> => {
    const res = await apiClient.get<ApiResponse<Department[]>>(
      `${BASE}/companies/${companyId}/departments`
    );
    return res.data;
  },

  // GET /api/organization/api/v1/departments/{departmentId}
  getDepartment: async (departmentId: string): Promise<ApiResponse<Department>> => {
    const res = await apiClient.get<ApiResponse<Department>>(`${BASE}/departments/${departmentId}`);
    return res.data;
  },

  // PATCH /api/organization/api/v1/departments/{departmentId}
  updateDepartment: async (
    companyId: string,
    departmentId: string,
    data: Partial<Department>
  ): Promise<ApiResponse<Department>> => {
    const res = await apiClient.patch<ApiResponse<Department>>(
      `${BASE}/companies/${companyId}/departments/${departmentId}`,
      data
    );
    return res.data;
  },

  // DELETE /api/organization/api/v1/departments/{departmentId}
  deleteDepartment: async (companyId: string, departmentId: string): Promise<ApiResponse<Department>> => {
    const res = await apiClient.delete<ApiResponse<Department>>(
      `${BASE}/companies/${companyId}/departments/${departmentId}`
    );
    return res.data;
  },

  // ── Members ────────────────────────────────────────────────────────────────

  // GET /api/organization/api/v1/companies/{companyId}/members
  listMembers: async (companyId: string): Promise<ApiResponse<CompanyMember[]>> => {
    const res = await apiClient.get<ApiResponse<CompanyMember[]>>(
      `${BASE}/companies/${companyId}/members`
    );
    return res.data;
  },

  getMyMembership: async (companyId: string): Promise<ApiResponse<CompanyMember>> => {
    const res = await apiClient.get<ApiResponse<CompanyMember>>(
      `${BASE}/companies/${companyId}/members/me`
    );
    return res.data;
  },

  // DELETE /api/organization/api/v1/companies/{companyId}/members/{userId}
  removeMember: async (companyId: string, userId: string): Promise<ApiResponse<string>> => {
    const res = await apiClient.delete<ApiResponse<string>>(
      `${BASE}/companies/${companyId}/members/${userId}`
    );
    return res.data;
  },

  // POST /api/organization/api/v1/companies/{companyId}/members/{userId}/roles
  assignMemberRole: async (
    companyId: string,
    userId: string,
    data: { role: string }
  ): Promise<ApiResponse<CompanyMember>> => {
    const res = await apiClient.post<ApiResponse<CompanyMember>>(
      `${BASE}/companies/${companyId}/members/${userId}/roles`,
      data
    );
    return res.data;
  },

  // ── Invitations ────────────────────────────────────────────────────────────

  // POST /api/organization/api/v1/companies/{companyId}/members/invitations
  inviteMember: async (
    companyId: string,
    data: InviteMemberRequest
  ): Promise<ApiResponse<Invitation>> => {
    const res = await apiClient.post<ApiResponse<{ invitation: Invitation; acceptToken: string }>>(
      `${BASE}/companies/${companyId}/invitations`,
      data
    );
    return {
      ...res.data,
      data: res.data.data.invitation,
    };
  },

  // GET /api/organization/api/v1/companies/{companyId}/invitations
  listInvitations: async (companyId: string): Promise<ApiResponse<Invitation[]>> => {
    const res = await apiClient.get<ApiResponse<Invitation[]>>(
      `${BASE}/companies/${companyId}/invitations`
    );
    return res.data;
  },

  // POST /api/organization/api/v1/companies/{companyId}/invitations/{invitationId}/accept
  acceptInvitation: async (
    companyId: string,
    invitationId: string
  ): Promise<ApiResponse<Invitation>> => {
    const res = await apiClient.post<ApiResponse<Invitation>>(
      `${BASE}/companies/${companyId}/invitations/${invitationId}/accept`
    );
    return res.data;
  },

  revokeInvitation: async (
    companyId: string,
    invitationId: string
  ): Promise<ApiResponse<Invitation>> => {
    const res = await apiClient.delete<ApiResponse<Invitation>>(
      `${BASE}/companies/${companyId}/invitations/${invitationId}`
    );
    return res.data;
  },

  // ── Department Members ─────────────────────────────────────────────────────

  // GET /api/organization/api/v1/departments/{departmentId}/members
  listDepartmentMembers: async (departmentId: string): Promise<ApiResponse<CompanyMember[]>> => {
    const res = await apiClient.get<ApiResponse<CompanyMember[]>>(
      `${BASE}/departments/${departmentId}/members`
    );
    return res.data;
  },

  // POST /api/organization/api/v1/departments/{departmentId}/members/{userId}
  addDepartmentMember: async (
    departmentId: string,
    userId: string,
    data?: { role: 'DEPARTMENT_MANAGER' | 'PROJECT_MANAGER' | 'DEVELOPER' | 'TESTER' | 'BUG_HUNTER' }
  ): Promise<ApiResponse<CompanyMember>> => {
    const res = await apiClient.post<ApiResponse<CompanyMember>>(
      `${BASE}/departments/${departmentId}/members/${userId}`,
      data
    );
    return res.data;
  },

  updateDepartmentMemberRole: async (
    departmentId: string,
    userId: string,
    role: 'DEPARTMENT_MANAGER' | 'PROJECT_MANAGER' | 'DEVELOPER' | 'TESTER' | 'BUG_HUNTER'
  ): Promise<ApiResponse<CompanyMember>> => {
    const res = await apiClient.put<ApiResponse<CompanyMember>>(
      `${BASE}/departments/${departmentId}/members/${userId}/role`,
      { role }
    );
    return res.data;
  },

  // DELETE /api/organization/api/v1/departments/{departmentId}/members/{userId}
  removeDepartmentMember: async (
    departmentId: string,
    userId: string
  ): Promise<ApiResponse<void>> => {
    const res = await apiClient.delete<ApiResponse<void>>(
      `${BASE}/departments/${departmentId}/members/${userId}`
    );
    return res.data;
  },

  // PATCH /api/organization/api/v1/companies/{companyId}/members/{userId}/roles
  updateMemberRole: async (
    companyId: string,
    userId: string,
    data: { role: string }
  ): Promise<ApiResponse<CompanyMember>> => {
    const res = await apiClient.patch<ApiResponse<CompanyMember>>(
      `${BASE}/companies/${companyId}/members/${userId}/roles`,
      data
    );
    return res.data;
  },
};
