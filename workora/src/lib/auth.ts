// Real API integration — all auth calls go through the gateway at localhost:8080
// Gateway path: /api/identity/api/v1/auth/*

import { identityApi } from './api/identity';
import type { UserProfile as ApiUserProfile } from './api/types';

export interface AuthTokens {
  access_token: string;
  refresh_token: string;
  expires_in?: number;
}

export interface UserProfile {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  emailVerified: boolean;
  role?: string;
  accountStatus?: string;
  createdAt?: string;
}

export interface AuthState {
  user: UserProfile | null;
  tokens: AuthTokens | null;
  isAuthenticated: boolean;
}

const STORAGE_KEY_ACCESS = 'workora_access_token';
const STORAGE_KEY_REFRESH = 'workora_refresh_token';
const STORAGE_KEY_USER = 'workora_user';

export function storeTokens(tokens: AuthTokens): void {
  if (typeof window !== 'undefined') {
    localStorage.setItem(STORAGE_KEY_ACCESS, tokens.access_token);
    localStorage.setItem(STORAGE_KEY_REFRESH, tokens.refresh_token);
  }
}

export function getAccessToken(): string | null {
  if (typeof window === 'undefined') return null;
  return localStorage.getItem(STORAGE_KEY_ACCESS);
}

export function getRefreshToken(): string | null {
  if (typeof window === 'undefined') return null;
  return localStorage.getItem(STORAGE_KEY_REFRESH);
}

export function storeUser(user: UserProfile): void {
  if (typeof window !== 'undefined') {
    localStorage.setItem(STORAGE_KEY_USER, JSON.stringify(user));
  }
}

export function getStoredUser(): UserProfile | null {
  if (typeof window === 'undefined') return null;
  const raw = localStorage.getItem(STORAGE_KEY_USER);
  if (!raw) return null;
  try { return JSON.parse(raw); } catch { return null; }
}

export function clearAuth(): void {
  if (typeof window !== 'undefined') {
    localStorage.removeItem(STORAGE_KEY_ACCESS);
    localStorage.removeItem(STORAGE_KEY_REFRESH);
    localStorage.removeItem(STORAGE_KEY_USER);
  }
}

export function isAuthenticated(): boolean {
  return !!getAccessToken();
}

function mapApiUserToProfile(apiUser: ApiUserProfile): UserProfile {
  return {
    id: apiUser.id,
    email: apiUser.email,
    firstName: apiUser.firstName,
    lastName: apiUser.lastName,
    emailVerified: apiUser.emailVerified,
    role: apiUser.role,
    accountStatus: apiUser.accountStatus,
  };
}

// Real login — POST /api/identity/api/v1/auth/login
export async function login(
  email: string,
  password: string
): Promise<{ tokens: AuthTokens; user: UserProfile; verificationSessionToken?: string }> {
  const response = await identityApi.login({ email, password });
  const data = response.data;

  const tokens: AuthTokens = {
    access_token: data.accessToken,
    refresh_token: data.refreshToken,
  };

  const user: UserProfile = {
    id: data.userId,
    email: data.email,
    firstName: data.firstName,
    lastName: data.lastName,
    emailVerified: true,
    role: data.role,
  };

  return { tokens, user };
}

// Real register — POST /api/identity/api/v1/auth/register
export async function register(data: {
  firstName: string;
  lastName: string;
  email: string;
  password: string;
}): Promise<{ success: boolean; verificationSessionToken: string; userId: string }> {
  const response = await identityApi.register({
    email: data.email,
    password: data.password,
    firstName: data.firstName,
    lastName: data.lastName,
  });

  return {
    success: true,
    verificationSessionToken: response.data.verificationSessionToken,
    userId: response.data.userId,
  };
}

// Real logout — POST /api/identity/api/v1/auth/logout
export async function logout(): Promise<void> {
  const refreshToken = getRefreshToken();
  if (refreshToken) {
    try {
      await identityApi.logout(refreshToken);
    } catch {
      // Ignore logout errors — clear local state regardless
    }
  }
  clearAuth();
}

// Real get current user — GET /api/identity/api/v1/users/me
export async function getCurrentUser(): Promise<UserProfile> {
  const response = await identityApi.getMe();
  return mapApiUserToProfile(response.data);
}

// Real resend verification email — POST /api/identity/api/v1/auth/verification-email-requests
export async function resendVerificationEmail(email: string): Promise<void> {
  await identityApi.requestVerificationEmail(email);
}

// Real password reset request — POST /api/identity/api/v1/auth/password-reset-requests
export async function requestPasswordReset(email: string): Promise<void> {
  await identityApi.requestPasswordReset(email);
}

// Real confirm password reset — POST /api/identity/api/v1/auth/password-resets
export async function confirmPasswordReset(data: {
  email: string;
  token: string;
  newPassword: string;
}): Promise<void> {
  await identityApi.confirmPasswordReset(data);
}

// Check verification session status — GET /api/identity/api/v1/auth/registration-verification-sessions/{token}
export async function checkVerificationSession(
  token: string
): Promise<{ status: 'PENDING' | 'VERIFIED' | 'EXPIRED'; expiresAt: string }> {
  const response = await identityApi.checkVerificationSession(token);
  return response.data;
}

// ── Legacy mock aliases (kept for backward compatibility) ──────────────────
// These now call the real API
export const mockLogin = async (email: string, password: string) => login(email, password);
export const mockRegister = async (data: {
  firstName: string;
  lastName: string;
  email: string;
  password: string;
}) => register(data);