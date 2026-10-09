import apiClient from './client';
import type { ApiResponse } from './types';

const BASE = '/api/work/api/v1';

export interface Task {
  id: string;
  phaseId: string;
  title: string;
  description?: string;
  status?: 'OPEN' | 'IN_PROGRESS' | 'IN_REVIEW' | 'DONE' | 'CANCELLED';
  priority?: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
  dueDate?: string;
  createdAt?: string;
  updatedAt?: string;
  assigneeCount?: number;
}

export interface TaskAssignee {
  userId: string;
  firstName: string;
  lastName: string;
  email: string;
  assignedAt?: string;
}

export interface TaskSubmission {
  id: string;
  taskId: string;
  submittedBy: string;
  content?: string;
  status?: 'PENDING' | 'APPROVED' | 'REJECTED';
  submittedAt?: string;
  reviewedAt?: string;
  reviewNote?: string;
}

export interface TaskAttachment {
  id: string;
  taskId: string;
  fileName: string;
  fileUrl?: string;
  fileSize?: number;
  uploadedBy?: string;
  uploadedAt?: string;
}

export interface WorkspaceSummary {
  totalTasks: number;
  openTasks: number;
  inProgressTasks: number;
  doneTasks: number;
  overdueTasks: number;
}

export interface CreateTaskRequest {
  title: string;
  description?: string;
  priority?: string;
  dueDate?: string;
}

export interface AssignTaskRequest {
  userId: string;
}

export interface SubmitTaskRequest {
  content: string;
}

export const workApi = {
  // ── Tasks ─────────────────────────────────────────────────────────────────

  createTask: async (phaseId: string, data: CreateTaskRequest): Promise<ApiResponse<Task>> => {
    const res = await apiClient.post<ApiResponse<Task>>(`${BASE}/phases/${phaseId}/tasks`, data);
    return res.data;
  },

  listTasks: async (phaseId: string): Promise<ApiResponse<Task[]>> => {
    const res = await apiClient.get<ApiResponse<Task[]>>(`${BASE}/phases/${phaseId}/tasks`);
    return res.data;
  },

  getTask: async (taskId: string): Promise<ApiResponse<Task>> => {
    const res = await apiClient.get<ApiResponse<Task>>(`${BASE}/tasks/${taskId}`);
    return res.data;
  },

  updateTask: async (taskId: string, data: Partial<CreateTaskRequest>): Promise<ApiResponse<Task>> => {
    const res = await apiClient.patch<ApiResponse<Task>>(`${BASE}/tasks/${taskId}`, data);
    return res.data;
  },

  deleteTask: async (taskId: string): Promise<ApiResponse<Task>> => {
    const res = await apiClient.patch<ApiResponse<Task>>(`${BASE}/tasks/${taskId}/status`, { status: 'CANCELLED' });
    return res.data;
  },

  updateTaskStatus: async (taskId: string, status: string): Promise<ApiResponse<Task>> => {
    const res = await apiClient.patch<ApiResponse<Task>>(`${BASE}/tasks/${taskId}/status`, { status });
    return res.data;
  },

  // ── Assignees ─────────────────────────────────────────────────────────────

  listAssignees: async (taskId: string): Promise<ApiResponse<TaskAssignee[]>> => {
    const res = await apiClient.get<ApiResponse<TaskAssignee[]>>(`${BASE}/tasks/${taskId}/assignees`);
    return res.data;
  },

  assignUser: async (taskId: string, data: AssignTaskRequest): Promise<ApiResponse<string>> => {
    const res = await apiClient.post<ApiResponse<string>>(`${BASE}/tasks/${taskId}/assignees`, data);
    return res.data;
  },

  removeAssignee: async (taskId: string, userId: string): Promise<ApiResponse<string>> => {
    const res = await apiClient.delete<ApiResponse<string>>(`${BASE}/tasks/${taskId}/assignees/${userId}`);
    return res.data;
  },

  // ── Submissions ───────────────────────────────────────────────────────────

  submitWork: async (taskId: string, data: SubmitTaskRequest): Promise<ApiResponse<TaskSubmission>> => {
    const res = await apiClient.post<ApiResponse<TaskSubmission>>(`${BASE}/tasks/${taskId}/submissions`, data);
    return res.data;
  },

  listSubmissions: async (taskId: string): Promise<ApiResponse<TaskSubmission[]>> => {
    const res = await apiClient.get<ApiResponse<TaskSubmission[]>>(`${BASE}/tasks/${taskId}/submissions`);
    return res.data;
  },

  getSubmission: async (taskId: string, submissionId: string): Promise<ApiResponse<TaskSubmission>> => {
    const res = await apiClient.get<ApiResponse<TaskSubmission>>(`${BASE}/tasks/${taskId}/submissions/${submissionId}`);
    return res.data;
  },

  // ── Attachments ───────────────────────────────────────────────────────────

  listAttachments: async (taskId: string): Promise<ApiResponse<TaskAttachment[]>> => {
    const res = await apiClient.get<ApiResponse<TaskAttachment[]>>(`${BASE}/tasks/${taskId}/attachments`);
    return res.data;
  },

  uploadAttachment: async (taskId: string, formData: FormData): Promise<ApiResponse<TaskAttachment>> => {
    const res = await apiClient.post<ApiResponse<TaskAttachment>>(
      `${BASE}/tasks/${taskId}/attachments`,
      formData,
      { headers: { 'Content-Type': 'multipart/form-data' } }
    );
    return res.data;
  },

  removeAttachment: async (taskId: string, attachmentId: string): Promise<ApiResponse<string>> => {
    const res = await apiClient.delete<ApiResponse<string>>(`${BASE}/tasks/${taskId}/attachments/${attachmentId}`);
    return res.data;
  },

  // ── Workspace ─────────────────────────────────────────────────────────────

  getWorkspace: async (): Promise<ApiResponse<WorkspaceSummary>> => {
    const res = await apiClient.get<ApiResponse<WorkspaceSummary>>(`${BASE}/workspace`);
    return res.data;
  },

  listMyTasks: async (): Promise<ApiResponse<Task[]>> => {
    const res = await apiClient.get<ApiResponse<Task[]>>(`${BASE}/tasks/me`);
    return res.data;
  },
};
