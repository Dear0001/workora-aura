import apiClient from './client';
import type { ApiResponse } from './types';

const BASE = '/api/payment/api/v1';

export interface BakongProfile {
  id: string;
  walletName: string;
  phoneNumber: string;
  isPrimary: boolean;
  status?: 'ACTIVE' | 'INACTIVE';
  qrImageUrl?: string;
}

export interface FeePayment {
  id: string;
  bugFixJobId?: string;
  amount?: number;
  currency?: string;
  status?: 'PENDING' | 'VERIFIED' | 'CONFIRMED' | 'DISPUTED' | 'FAILED';
  qrPayload?: string;
  qrImageUrl?: string;
  createdAt?: string;
  verifiedAt?: string;
  confirmedAt?: string;
}

export interface Dispute {
  id: string;
  paymentId: string;
  reason?: string;
  status?: 'OPEN' | 'RESOLVED' | 'CLOSED';
  createdAt?: string;
}

export interface CreateBakongProfileRequest {
  walletName: string;
  phoneNumber: string;
  isPrimary?: boolean;
}

export interface FeeApprovalRequest {
  amount: number;
  currency?: string;
  notes?: string;
}

export interface DisputeRequest {
  reason: string;
}

export const paymentsApi = {
  // ── Bakong Profile ────────────────────────────────────────────────────────

  getBakongProfile: async (): Promise<ApiResponse<BakongProfile>> => {
    const res = await apiClient.get<ApiResponse<BakongProfile>>(
      `${BASE}/users/me/payment-profiles/bakong`
    );
    return res.data;
  },

  createBakongProfile: async (data: CreateBakongProfileRequest): Promise<ApiResponse<BakongProfile>> => {
    const res = await apiClient.post<ApiResponse<BakongProfile>>(
      `${BASE}/users/me/payment-profiles/bakong`,
      data
    );
    return res.data;
  },

  updateBakongProfile: async (data: Partial<CreateBakongProfileRequest>): Promise<ApiResponse<BakongProfile>> => {
    const res = await apiClient.patch<ApiResponse<BakongProfile>>(
      `${BASE}/users/me/payment-profiles/bakong`,
      data
    );
    return res.data;
  },

  deleteBakongProfile: async (): Promise<ApiResponse<string>> => {
    const res = await apiClient.delete<ApiResponse<string>>(
      `${BASE}/users/me/payment-profiles/bakong`
    );
    return res.data;
  },

  uploadBakongQR: async (formData: FormData): Promise<ApiResponse<BakongProfile>> => {
    const res = await apiClient.post<ApiResponse<BakongProfile>>(
      `${BASE}/users/me/payment-profiles/bakong/qr-upload`,
      formData,
      { headers: { 'Content-Type': 'multipart/form-data' } }
    );
    return res.data;
  },

  // ── Fee Payments ──────────────────────────────────────────────────────────

  approveFee: async (jobId: string, data: FeeApprovalRequest): Promise<ApiResponse<FeePayment>> => {
    const res = await apiClient.post<ApiResponse<FeePayment>>(
      `${BASE}/bug-fix-jobs/${jobId}/fee-approval`,
      data
    );
    return res.data;
  },

  getPayment: async (paymentId: string): Promise<ApiResponse<FeePayment>> => {
    const res = await apiClient.get<ApiResponse<FeePayment>>(`${BASE}/fee-payments/${paymentId}`);
    return res.data;
  },

  getPaymentQR: async (paymentId: string): Promise<ApiResponse<FeePayment>> => {
    const res = await apiClient.get<ApiResponse<FeePayment>>(`${BASE}/fee-payments/${paymentId}/qr`);
    return res.data;
  },

  verifyPayment: async (paymentId: string): Promise<ApiResponse<FeePayment>> => {
    const res = await apiClient.post<ApiResponse<FeePayment>>(`${BASE}/fee-payments/${paymentId}/verify`);
    return res.data;
  },

  confirmReceipt: async (paymentId: string): Promise<ApiResponse<FeePayment>> => {
    const res = await apiClient.post<ApiResponse<FeePayment>>(
      `${BASE}/fee-payments/${paymentId}/confirm-receipt`
    );
    return res.data;
  },

  raiseDispute: async (paymentId: string, data: DisputeRequest): Promise<ApiResponse<Dispute>> => {
    const res = await apiClient.post<ApiResponse<Dispute>>(
      `${BASE}/fee-payments/${paymentId}/dispute`,
      data
    );
    return res.data;
  },

  getDispute: async (disputeId: string): Promise<ApiResponse<Dispute>> => {
    const res = await apiClient.get<ApiResponse<Dispute>>(`${BASE}/disputes/${disputeId}`);
    return res.data;
  },
};
