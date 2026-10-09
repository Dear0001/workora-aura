import apiClient from './client';
import type {
  ApiResponse,
  LoginRequest,
  LoginResponse,
  RegisterRequest,
  RegisterResponse,
  RefreshResponse,
  VerificationSessionResponse,
  UserProfile,
} from './types';

const BASE = '/api/identity/api/v1';

export const identityApi = {
  // POST /api/identity/api/v1/auth/register
  register: async (data: RegisterRequest): Promise<ApiResponse<RegisterResponse>> => {
    const res = await apiClient.post<ApiResponse<RegisterResponse>>(
      `${BASE}/auth/register`,
      data
    );
    return res.data;
  },

  // POST /api/identity/api/v1/auth/login
  login: async (data: LoginRequest): Promise<ApiResponse<LoginResponse>> => {
    const res = await apiClient.post<ApiResponse<LoginResponse>>(
      `${BASE}/auth/login`,
      data
    );
    return res.data;
  },

  // POST /api/identity/api/v1/auth/logout
  logout: async (refreshToken: string): Promise<ApiResponse<string>> => {
    const res = await apiClient.post<ApiResponse<string>>(
      `${BASE}/auth/logout`,
      { refreshToken }
    );
    return res.data;
  },

  // POST /api/identity/api/v1/auth/refresh
  refresh: async (refreshToken: string): Promise<ApiResponse<RefreshResponse>> => {
    const res = await apiClient.post<ApiResponse<RefreshResponse>>(
      `${BASE}/auth/refresh`,
      { refreshToken }
    );
    return res.data;
  },

  // POST /api/identity/api/v1/auth/verification-email-requests
  requestVerificationEmail: async (email: string): Promise<ApiResponse<string>> => {
    const res = await apiClient.post<ApiResponse<string>>(
      `${BASE}/auth/verification-email-requests`,
      { email }
    );
    return res.data;
  },

  // GET /api/identity/api/v1/auth/registration-verification-sessions/{token}
  checkVerificationSession: async (token: string): Promise<ApiResponse<VerificationSessionResponse>> => {
    const res = await apiClient.get<ApiResponse<VerificationSessionResponse>>(
      `${BASE}/auth/registration-verification-sessions/${token}`
    );
    return res.data;
  },

  // POST /api/identity/api/v1/auth/password-reset-requests
  requestPasswordReset: async (email: string): Promise<ApiResponse<string>> => {
    const res = await apiClient.post<ApiResponse<string>>(
      `${BASE}/auth/password-reset-requests`,
      { email }
    );
    return res.data;
  },

  // POST /api/identity/api/v1/auth/password-resets
  confirmPasswordReset: async (data: {
    email: string;
    token: string;
    newPassword: string;
  }): Promise<ApiResponse<string>> => {
    const res = await apiClient.post<ApiResponse<string>>(
      `${BASE}/auth/password-resets`,
      data
    );
    return res.data;
  },

  // GET /api/identity/api/v1/users/me
  getMe: async (): Promise<ApiResponse<UserProfile>> => {
    const res = await apiClient.get<ApiResponse<UserProfile>>(`${BASE}/users/me`);
    return res.data;
  },

  // PATCH /api/identity/api/v1/users/me
  updateMe: async (data: { firstName?: string; lastName?: string }): Promise<ApiResponse<UserProfile>> => {
    const res = await apiClient.patch<ApiResponse<UserProfile>>(`${BASE}/users/me`, data);
    return res.data;
  },
};
