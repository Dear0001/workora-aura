import apiClient from './client';
import type { StorageUpload } from './organization';
import type { ApiResponse } from './types';

const BASE = '/api/work/api/v1';

export interface Task {
  id: string;
  phaseId: string;
  projectId: string;
  departmentId: string;
  companyId: string;
  projectManagerId?: string;
  title: string;
  description?: string;
  status: TaskStatus;
  priority: TaskPriority;
  completionRule: 'ALL_ASSIGNEES' | 'ANY_ASSIGNEE';
  createdBy: string;
  dueDate?: string;
  createdAt?: string;
  updatedAt?: string;
}

export type TaskStatus =
  | 'DRAFT'
  | 'ASSIGNED'
  | 'NOT_YET'
  | 'IN_PROGRESS'
  | 'SUBMITTED'
  | 'ACCEPTED'
  | 'CHANGES_REQUESTED'
  | 'COMPLETED'
  | 'CANCELLED';

export type TaskPriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';

const TASK_STATUS_TRANSITIONS: Record<TaskStatus, TaskStatus[]> = {
  DRAFT: ['ASSIGNED', 'CANCELLED'],
  ASSIGNED: ['NOT_YET', 'IN_PROGRESS', 'SUBMITTED', 'CANCELLED'],
  NOT_YET: ['IN_PROGRESS', 'SUBMITTED', 'CANCELLED'],
  IN_PROGRESS: ['SUBMITTED', 'CANCELLED'],
  SUBMITTED: ['ACCEPTED', 'CHANGES_REQUESTED', 'CANCELLED'],
  ACCEPTED: ['COMPLETED', 'CANCELLED'],
  CHANGES_REQUESTED: ['IN_PROGRESS', 'SUBMITTED', 'COMPLETED', 'CANCELLED'],
  COMPLETED: [],
  CANCELLED: [],
};

export function getAllowedTaskTransitions(status: TaskStatus): TaskStatus[] {
  return TASK_STATUS_TRANSITIONS[status];
}

export interface TaskAssignee {
  id: string;
  taskId: string;
  userId: string;
  departmentId: string;
  status: 'ASSIGNED' | 'IN_PROGRESS' | 'SUBMITTED' | 'ACCEPTED' | 'CHANGES_REQUESTED' | 'COMPLETED';
  assignedBy: string;
  assignedAt?: string;
  removedAt?: string;
}

export interface TaskDetail {
  task: Task;
  assignees: TaskAssignee[];
  progress: {
    taskId: string;
    status: TaskStatus;
    completionRule: 'ALL_ASSIGNEES' | 'ANY_ASSIGNEE';
    assignees: { userId: string; status: string; submissionCount: number }[];
  };
}

export interface TaskHistory {
  id: string;
  fromStatus?: TaskStatus;
  toStatus: TaskStatus;
  actorId: string;
  reason?: string;
  changedAt: string;
}

export interface TaskSubmission {
  id: string;
  taskId: string;
  authorId: string;
  version: number;
  note?: string;
  attachmentKeys: string[];
  submittedAt?: string;
}

export interface TaskAttachment {
  id: string;
  taskId: string;
  fileKey: string;
  fileName: string;
  mimeType: string;
  description?: string;
  uploadedBy: string;
  uploadedAt?: string;
}

export interface CreateTaskAttachmentRequest {
  fileKey: string;
  fileName: string;
  mimeType: string;
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
  dueDate?: string;
  priority?: TaskPriority;
}

export interface AssignTaskRequest {
  userId: string;
}

export interface SubmitTaskRequest {
  note?: string;
  attachmentKeys?: string[];
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

  listCompanyTasks: async (companyId: string): Promise<ApiResponse<Task[]>> => {
    const res = await apiClient.get<ApiResponse<Task[]>>(`${BASE}/companies/${companyId}/tasks`);
    return res.data;
  },

  getTask: async (taskId: string): Promise<ApiResponse<TaskDetail>> => {
    const res = await apiClient.get<ApiResponse<TaskDetail>>(`${BASE}/tasks/${taskId}`);
    return res.data;
  },

  getTaskHistory: async (taskId: string): Promise<ApiResponse<TaskHistory[]>> => {
    const res = await apiClient.get<ApiResponse<TaskHistory[]>>(`${BASE}/tasks/${taskId}/history`);
    return res.data;
  },

  updateTask: async (taskId: string, data: Partial<CreateTaskRequest>): Promise<ApiResponse<Task>> => {
    const res = await apiClient.patch<ApiResponse<Task>>(`${BASE}/tasks/${taskId}`, data);
    return res.data;
  },

  archiveTask: async (taskId: string): Promise<ApiResponse<void>> => {
    const res = await apiClient.delete<ApiResponse<void>>(`${BASE}/tasks/${taskId}`);
    return res.data;
  },

  updateTaskStatus: async (taskId: string, status: TaskStatus, reason?: string): Promise<ApiResponse<Task>> => {
    const res = await apiClient.patch<ApiResponse<Task>>(`${BASE}/tasks/${taskId}/status`, { status, reason });
    return res.data;
  },

  // ── Assignees ─────────────────────────────────────────────────────────────

  listAssignees: async (taskId: string): Promise<ApiResponse<TaskAssignee[]>> => {
    const res = await apiClient.get<ApiResponse<TaskAssignee[]>>(`${BASE}/tasks/${taskId}/assignees`);
    return res.data;
  },

  assignUser: async (taskId: string, data: AssignTaskRequest): Promise<ApiResponse<TaskAssignee>> => {
    const res = await apiClient.post<ApiResponse<TaskAssignee>>(`${BASE}/tasks/${taskId}/assignees`, data);
    return res.data;
  },

  removeAssignee: async (taskId: string, userId: string): Promise<ApiResponse<void>> => {
    const res = await apiClient.delete<ApiResponse<void>>(`${BASE}/tasks/${taskId}/assignees/${userId}`);
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

  createAttachmentUpload: async (
    taskId: string,
    data: { fileName: string; contentType: string }
  ): Promise<ApiResponse<StorageUpload>> => {
    const res = await apiClient.post<ApiResponse<StorageUpload>>(
      `${BASE}/tasks/${taskId}/attachments/uploads`,
      data
    );
    return res.data;
  },

  listAttachments: async (taskId: string): Promise<ApiResponse<TaskAttachment[]>> => {
    const res = await apiClient.get<ApiResponse<TaskAttachment[]>>(`${BASE}/tasks/${taskId}/attachments`);
    return res.data;
  },

  uploadAttachment: async (
    taskId: string,
    data: CreateTaskAttachmentRequest
  ): Promise<ApiResponse<TaskAttachment>> => {
    const res = await apiClient.post<ApiResponse<TaskAttachment>>(
      `${BASE}/tasks/${taskId}/attachments`,
      data
    );
    return res.data;
  },

  removeAttachment: async (taskId: string, attachmentId: string): Promise<ApiResponse<string>> => {
    const res = await apiClient.delete<ApiResponse<string>>(`${BASE}/tasks/${taskId}/attachments/${attachmentId}`);
    return res.data;
  },

  uploadTaskFile: async (taskId: string, file: File): Promise<ApiResponse<TaskAttachment>> => {
    const contentType = file.type || 'application/octet-stream';
    const upload = await workApi.createAttachmentUpload(taskId, {
      fileName: file.name,
      contentType,
    });
    const response = await fetch(upload.data.uploadUrl, {
      method: 'PUT',
      headers: { 'Content-Type': contentType },
      body: file,
    });
    if (!response.ok) throw new Error(`File upload failed (${response.status})`);
    return workApi.uploadAttachment(taskId, {
      fileKey: upload.data.key,
      fileName: file.name,
      mimeType: contentType,
    });
  },

  getAttachmentDownloadUrl: async (
    taskId: string,
    attachmentId: string
  ): Promise<ApiResponse<{ downloadUrl: string }>> => {
    const res = await apiClient.get<ApiResponse<{ downloadUrl: string }>>(
      `${BASE}/tasks/${taskId}/attachments/${attachmentId}/download`
    );
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
