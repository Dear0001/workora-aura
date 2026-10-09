import axios, { AxiosError, InternalAxiosRequestConfig } from 'axios';

const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL ?? 'http://localhost:8080';

export const apiClient = axios.create({
  baseURL: API_BASE_URL,
  timeout: 15000,
  headers: { 'Content-Type': 'application/json' },
});

// Storage keys — must match auth.ts
const STORAGE_KEY_ACCESS = 'workora_access_token';
const STORAGE_KEY_REFRESH = 'workora_refresh_token';
const STORAGE_KEY_USER = 'workora_user';

// Request interceptor — attach Bearer token
apiClient.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  if (typeof window !== 'undefined') {
    const token = localStorage.getItem(STORAGE_KEY_ACCESS);
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
  }
  return config;
});

let isRefreshing = false;
let failedQueue: Array<{ resolve: (token: string) => void; reject: (err: unknown) => void }> = [];

function processQueue(error: unknown, token: string | null = null) {
  failedQueue.forEach((prom) => {
    if (error) prom.reject(error);
    else prom.resolve(token!);
  });
  failedQueue = [];
}

// Response interceptor — handle 401 with token refresh
apiClient.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const originalRequest = error.config as InternalAxiosRequestConfig & { _retry?: boolean };

    if (error.response?.status === 401 && !originalRequest._retry) {
      if (isRefreshing) {
        return new Promise((resolve, reject) => {
          failedQueue.push({ resolve, reject });
        }).then((token) => {
          originalRequest.headers.Authorization = `Bearer ${token}`;
          return apiClient(originalRequest);
        });
      }

      originalRequest._retry = true;
      isRefreshing = true;

      const refreshToken = typeof window !== 'undefined'
        ? localStorage.getItem(STORAGE_KEY_REFRESH)
        : null;

      if (!refreshToken) {
        isRefreshing = false;
        if (typeof window !== 'undefined') {
          localStorage.removeItem(STORAGE_KEY_ACCESS);
          localStorage.removeItem(STORAGE_KEY_REFRESH);
          localStorage.removeItem(STORAGE_KEY_USER);
          window.location.href = '/';
        }
        return Promise.reject(error);
      }

      try {
        const response = await axios.post(
          `${API_BASE_URL}/api/identity/api/v1/auth/refresh`,
          { refreshToken },
          { headers: { 'Content-Type': 'application/json' } }
        );

        const data = response.data?.data;
        const newAccessToken: string = data?.accessToken;
        const newRefreshToken: string = data?.refreshToken;

        if (typeof window !== 'undefined') {
          localStorage.setItem(STORAGE_KEY_ACCESS, newAccessToken);
          if (newRefreshToken) localStorage.setItem(STORAGE_KEY_REFRESH, newRefreshToken);
        }

        apiClient.defaults.headers.common.Authorization = `Bearer ${newAccessToken}`;
        processQueue(null, newAccessToken);
        originalRequest.headers.Authorization = `Bearer ${newAccessToken}`;
        return apiClient(originalRequest);
      } catch (refreshError) {
        processQueue(refreshError, null);
        if (typeof window !== 'undefined') {
          localStorage.removeItem(STORAGE_KEY_ACCESS);
          localStorage.removeItem(STORAGE_KEY_REFRESH);
          localStorage.removeItem(STORAGE_KEY_USER);
          window.location.href = '/';
        }
        return Promise.reject(refreshError);
      } finally {
        isRefreshing = false;
      }
    }

    return Promise.reject(error);
  }
);

export default apiClient;
