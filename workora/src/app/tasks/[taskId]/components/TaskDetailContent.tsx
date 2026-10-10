'use client';
import React, { useCallback, useEffect, useRef, useState } from 'react';
import FloatingMenu from '@/components/ui/FloatingMenu';
import {
  ArrowLeft,
  CheckSquare,
  Users,
  Calendar,
  Flag,
  Loader2,
  Paperclip,
  Send,
  Trash2,
  FileText,
  ImageIcon,
  Clock,
  ChevronDown,
  User,
  UserPlus,
  UserMinus,
  X,
  Upload,
  Download,
} from 'lucide-react';
import Link from 'next/link';
import { toast } from 'sonner';
import { AxiosError } from 'axios';
import { getAllowedTaskTransitions, workApi, type Task, type TaskAssignee, type TaskSubmission, type TaskAttachment, type TaskHistory } from '@/lib/api/work';
import { projectsApi, type Phase } from '@/lib/api/projects';
import { organizationApi } from '@/lib/api/organization';
import { identityApi } from '@/lib/api/identity';
import type { CompanyMember } from '@/lib/api/types';
import { getTaskFileValidationError, TASK_FILE_ACCEPT } from '@/lib/task-files';

const STATUS_COLORS: Record<string, string> = {
  DRAFT: 'bg-muted text-muted-foreground border border-border',
  ASSIGNED: 'bg-violet-50 text-violet-700 border border-violet-200',
  NOT_YET: 'bg-muted text-muted-foreground border border-border',
  IN_PROGRESS: 'bg-blue-50 text-blue-700 border border-blue-200',
  SUBMITTED: 'bg-amber-50 text-amber-700 border border-amber-200',
  ACCEPTED: 'bg-teal-50 text-teal-700 border border-teal-200',
  CHANGES_REQUESTED: 'bg-orange-50 text-orange-700 border border-orange-200',
  COMPLETED: 'bg-green-50 text-green-700 border border-green-200',
  CANCELLED: 'bg-red-50 text-red-600 border border-red-200',
};

const PRIORITY_COLORS: Record<string, string> = {
  LOW: 'text-muted-foreground',
  MEDIUM: 'text-amber-600',
  HIGH: 'text-orange-600',
  CRITICAL: 'text-red-600',
};

const STATUS_HISTORY_LABELS: Record<string, string> = {
  DRAFT: 'Task created',
  ASSIGNED: 'Task assigned',
  NOT_YET: 'Task not started',
  IN_PROGRESS: 'Work started',
  SUBMITTED: 'Submitted for testing',
  ACCEPTED: 'Testing passed',
  CHANGES_REQUESTED: 'Changes requested',
  COMPLETED: 'Task completed',
  CANCELLED: 'Task cancelled',
};

function formatBytes(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

function FileIcon({ fileName }: { fileName: string }) {
  const ext = fileName.split('.').pop()?.toLowerCase() ?? '';
  if (['jpg', 'jpeg', 'png', 'gif', 'webp', 'svg'].includes(ext))
    return <ImageIcon size={16} className="text-blue-500" />;
  return <FileText size={16} className="text-muted-foreground" />;
}

interface Props {
  taskId: string;
}

export default function TaskDetailContent({ taskId }: Props) {
  const [task, setTask] = useState<Task | null>(null);
  const [phase, setPhase] = useState<Phase | null>(null);
  const [assignees, setAssignees] = useState<TaskAssignee[]>([]);
  const [eligibleMembers, setEligibleMembers] = useState<CompanyMember[]>([]);
  const [selectedAssigneeId, setSelectedAssigneeId] = useState('');
  const [canManageTask, setCanManageTask] = useState(false);
  const [currentUserId, setCurrentUserId] = useState<string | null>(null);
  const [assigning, setAssigning] = useState(false);
  const [submissions, setSubmissions] = useState<TaskSubmission[]>([]);
  const [attachments, setAttachments] = useState<TaskAttachment[]>([]);
  const [statusHistory, setStatusHistory] = useState<TaskHistory[]>([]);
  const [loading, setLoading] = useState(true);

  // Submit-work form state
  const [submitContent, setSubmitContent] = useState('');
  const [submitFiles, setSubmitFiles] = useState<File[]>([]);
  const [submitting, setSubmitting] = useState(false);
  const [uploadingFiles, setUploadingFiles] = useState(false);
  const [downloadingAttachmentId, setDownloadingAttachmentId] = useState<string | null>(null);
  const [dragging, setDragging] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);

  // Status change
  const [statusMenuOpen, setStatusMenuOpen] = useState(false);
  const [updatingStatus, setUpdatingStatus] = useState(false);
  const companyId = task?.companyId;
  const departmentId = task?.departmentId;
  const projectId = task?.projectId;

  const loadAll = useCallback(async () => {
    setLoading(true);
    try {
      const [taskRes, submissionsRes, attachmentsRes, historyRes] = await Promise.all([
        workApi.getTask(taskId),
        workApi.listSubmissions(taskId),
        workApi.listAttachments(taskId),
        workApi.getTaskHistory(taskId),
      ]);
      const t = taskRes.data.task;
      setTask(t);
      setAssignees(taskRes.data.assignees ?? []);
      setSubmissions(submissionsRes.data ?? []);
      setAttachments(attachmentsRes.data ?? []);
      setStatusHistory(historyRes.data ?? []);

      // Load phase info
      if (t.phaseId) {
        try {
          const phaseRes = await projectsApi.getPhase(t.phaseId);
          setPhase(phaseRes.data);
        } catch { /* phase info optional */ }
      }
    } catch (err) {
      toast.error(err instanceof AxiosError ? (err.response?.data?.message ?? 'Failed to load task') : 'Failed to load task');
    } finally {
      setLoading(false);
    }
  }, [taskId]);

  useEffect(() => { loadAll(); }, [loadAll]);

  useEffect(() => {
    if (!companyId || !departmentId || !projectId) return;
    let cancelled = false;
    Promise.all([
      organizationApi.getMyMembership(companyId),
      identityApi.getMe(),
      organizationApi.listDepartmentMembers(departmentId),
    ]).then(async ([membershipResponse, profileResponse, membersResponse]) => {
      if (cancelled) return;
      setCurrentUserId(profileResponse.data.id);
      const role = membershipResponse.data.role;
      const departmentRole = membershipResponse.data.departmentMemberships
        ?.find(assignment => assignment.departmentId === departmentId)?.role;
      let isProjectManager = false;
      if ((departmentRole === 'PROJECT_MANAGER'
        || (role === 'PROJECT_MANAGER' && membershipResponse.data.departmentId === departmentId))) {
        const projectResponse = await projectsApi.getProject(projectId);
        isProjectManager = projectResponse.data.projectManagerId === profileResponse.data.id;
      }
      setCanManageTask(role === 'OWNER' || role === 'ADMIN' || isProjectManager);
      setEligibleMembers(membersResponse.data.filter(member => {
        const departmentAssignment = member.departmentMemberships
          ?.find(assignment => assignment.departmentId === departmentId);
        const assignedRole = departmentAssignment?.role
          ?? (member.departmentId === departmentId ? member.role : undefined);
        return assignedRole === 'DEVELOPER' || assignedRole === 'TESTER';
      }));
    }).catch(err => {
      if (!cancelled) {
        toast.error(err instanceof AxiosError
          ? (err.response?.data?.message ?? 'Failed to load task assignment permissions')
          : 'Failed to load task assignment permissions');
      }
    });
    return () => { cancelled = true; };
  }, [companyId, departmentId, projectId]);

  const handleStatusChange = async (status: Task['status']) => {
    if (!task) return;
    let reason: string | undefined;
    if (status === 'CHANGES_REQUESTED') {
      reason = window.prompt('Explain what needs to be changed:')?.trim();
      if (!reason) {
        if (reason !== undefined) toast.error('A reason is required to request changes');
        return;
      }
    }
    setUpdatingStatus(true);
    setStatusMenuOpen(false);
    try {
      const res = await workApi.updateTaskStatus(taskId, status, reason);
      setTask(res.data);
      toast.success('Status updated');
      try {
        const historyRes = await workApi.getTaskHistory(taskId);
        setStatusHistory(historyRes.data);
      } catch {
        toast.error('Status changed, but task history could not be refreshed');
      }
    } catch (err) {
      toast.error(err instanceof AxiosError ? (err.response?.data?.message ?? 'Failed to update status') : 'Failed to update status');
    } finally {
      setUpdatingStatus(false);
    }
  };

  const addFiles = (files: FileList | null) => {
    if (!files) return;
    const arr = Array.from(files).filter(file => {
      const error = getTaskFileValidationError(file);
      if (error) toast.error(`${file.name}: ${error}`);
      return !error;
    });
    setSubmitFiles(prev => {
      const existing = new Set(prev.map(f => f.name));
      return [...prev, ...arr.filter(f => !existing.has(f.name))];
    });
  };

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    setDragging(false);
    addFiles(e.dataTransfer.files);
  };

  const handleSubmitWork = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!submitContent.trim() && submitFiles.length === 0) return;
    setSubmitting(true);
    try {
      // 1. Submit work note
      if (submitContent.trim()) {
        const subRes = await workApi.submitWork(taskId, { note: submitContent.trim() });
        setSubmissions(prev => [subRes.data, ...prev]);
      }

      // 2. Upload files and link them to the task.
      let failedFiles: File[] = [];
      if (submitFiles.length > 0) {
        setUploadingFiles(true);
        for (const file of submitFiles) {
          try {
            const attachment = await workApi.uploadTaskFile(taskId, file);
            setAttachments(prev => [attachment.data, ...prev]);
          } catch {
            failedFiles = [...failedFiles, file];
          }
        }
        setUploadingFiles(false);
      }

      if (failedFiles.length > 0) {
        toast.success('Work note submitted');
        toast.error(`Could not upload: ${failedFiles.map(file => file.name).join(', ')}. The failed files remain selected so you can retry.`);
        setSubmitContent('');
        setSubmitFiles(failedFiles);
        return;
      }
      toast.success('Work submitted successfully');
      setSubmitContent('');
      setSubmitFiles([]);
    } catch (err) {
      toast.error(err instanceof AxiosError ? (err.response?.data?.message ?? 'Failed to submit work') : 'Failed to submit work');
    } finally {
      setSubmitting(false);
      setUploadingFiles(false);
    }
  };

  const handleRemoveAttachment = async (attachmentId: string) => {
    const attachment = attachments.find(item => item.id === attachmentId);
    if (!attachment) return;
    try {
      await workApi.removeAttachment(taskId, attachmentId);
      setAttachments(prev => prev.filter(a => a.id !== attachmentId));
      toast.success('Attachment removed');
    } catch (err) {
      toast.error(err instanceof AxiosError
        ? (err.response?.data?.message ?? 'Failed to remove attachment')
        : 'Failed to remove attachment');
    }
  };

  const handleDownloadAttachment = async (attachment: TaskAttachment) => {
    const downloadWindow = window.open('about:blank', '_blank');
    if (!downloadWindow) {
      toast.error('Allow pop-ups to open the attachment');
      return;
    }
    setDownloadingAttachmentId(attachment.id);
    try {
      const response = await workApi.getAttachmentDownloadUrl(taskId, attachment.id);
      downloadWindow.location.href = response.data.downloadUrl;
    } catch (err) {
      downloadWindow.close();
      toast.error(err instanceof AxiosError
        ? (err.response?.data?.message ?? 'Failed to open attachment')
        : 'Failed to open attachment');
    } finally {
      setDownloadingAttachmentId(null);
    }
  };

  const handleAssignMember = async () => {
    if (!task || !selectedAssigneeId) return;
    if (selectedAssigneeId === task.createdBy) {
      toast.error('You cannot assign a task you created to yourself. Please select another eligible member.');
      return;
    }
    if (!eligibleMembers.some(member =>
      member.userId === selectedAssigneeId && member.userId !== task.createdBy
        && !assignees.some(assignee => assignee.userId === member.userId)
    )) return;
    setAssigning(true);
    try {
      const response = await workApi.assignUser(task.id, { userId: selectedAssigneeId });
      setAssignees(prev => [...prev, response.data]);
      if (task.status === 'DRAFT') setTask({ ...task, status: 'ASSIGNED' });
      setSelectedAssigneeId('');
      toast.success('Member assigned to task');
    } catch (err) {
      toast.error(err instanceof AxiosError
        ? (err.response?.data?.message ?? 'Failed to assign member')
        : 'Failed to assign member');
    } finally {
      setAssigning(false);
    }
  };

  const handleRemoveAssignee = async (userId: string) => {
    if (!task) return;
    try {
      await workApi.removeAssignee(task.id, userId);
      setAssignees(prev => prev.filter(assignee => assignee.userId !== userId));
      toast.success('Member removed from task');
    } catch (err) {
      toast.error(err instanceof AxiosError
        ? (err.response?.data?.message ?? 'Failed to remove member from task')
        : 'Failed to remove member from task');
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center py-24">
        <Loader2 size={28} className="animate-spin text-primary" />
      </div>
    );
  }

  if (!task) {
    return (
      <div className="card-elevated p-12 text-center">
        <CheckSquare size={36} className="text-muted-foreground/30 mx-auto mb-3" />
        <p className="text-sm text-muted-foreground">Task not found.</p>
        <Link href="/tasks" className="btn-primary mt-4 px-4 py-2 rounded-lg text-sm font-semibold inline-flex items-center gap-2">
          <ArrowLeft size={14} />Back to Tasks
        </Link>
      </div>
    );
  }

  const currentStatus = task.status;
  const currentPriority = task.priority;

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-start gap-4">
        <Link
          href="/tasks"
          className="mt-0.5 w-8 h-8 rounded-lg flex items-center justify-center text-muted-foreground hover:text-foreground hover:bg-muted transition-all flex-shrink-0"
        >
          <ArrowLeft size={16} />
        </Link>
        <div className="flex-1 min-w-0">
          <div className="flex flex-wrap items-center gap-2 mb-1">
            <span className={`badge-base text-xs ${STATUS_COLORS[currentStatus] ?? STATUS_COLORS.DRAFT}`}>
              {currentStatus.replace(/_/g, ' ')}
            </span>
            <span className={`flex items-center gap-1 text-xs font-medium ${PRIORITY_COLORS[currentPriority]}`}>
              <Flag size={11} />{currentPriority}
            </span>
          </div>
          <h1 className="text-2xl font-bold text-foreground leading-tight">{task.title}</h1>
          {task.description && (
            <p className="text-sm text-muted-foreground mt-1.5 leading-relaxed">{task.description}</p>
          )}
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Left column — meta + submit */}
        <div className="lg:col-span-2 space-y-5">

          {/* Phase & dates card */}
          <div className="card-elevated p-5 space-y-4">
            <h2 className="text-sm font-semibold text-foreground">Task Details</h2>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              {/* Phase */}
              <div className="flex items-start gap-3">
                <div className="w-8 h-8 rounded-lg bg-primary/10 flex items-center justify-center flex-shrink-0">
                  <CheckSquare size={14} className="text-primary" />
                </div>
                <div>
                  <p className="text-xs text-muted-foreground font-medium uppercase tracking-wider mb-0.5">Phase</p>
                  {phase ? (
                    <p className="text-sm font-medium text-foreground">{phase.name}</p>
                  ) : (
                    <p className="text-sm text-muted-foreground font-mono text-xs">{task.phaseId}</p>
                  )}
                  {phase?.status && (
                    <span className="text-xs text-muted-foreground">{phase.status}</span>
                  )}
                </div>
              </div>

              {/* Due date */}
              <div className="flex items-start gap-3">
                <div className="w-8 h-8 rounded-lg bg-amber-50 flex items-center justify-center flex-shrink-0">
                  <Calendar size={14} className="text-amber-600" />
                </div>
                <div>
                  <p className="text-xs text-muted-foreground font-medium uppercase tracking-wider mb-0.5">Due Date</p>
                  <p className="text-sm font-medium text-foreground">
                    {task.dueDate ? new Date(task.dueDate).toLocaleDateString('en-US', { year: 'numeric', month: 'short', day: 'numeric' }) : '—'}
                  </p>
                </div>
              </div>

              {/* Created */}
              <div className="flex items-start gap-3">
                <div className="w-8 h-8 rounded-lg bg-muted flex items-center justify-center flex-shrink-0">
                  <Clock size={14} className="text-muted-foreground" />
                </div>
                <div>
                  <p className="text-xs text-muted-foreground font-medium uppercase tracking-wider mb-0.5">Created</p>
                  <p className="text-sm font-medium text-foreground">
                    {task.createdAt ? new Date(task.createdAt).toLocaleDateString('en-US', { year: 'numeric', month: 'short', day: 'numeric' }) : '—'}
                  </p>
                </div>
              </div>

              {/* Status change */}
              <div className="flex items-start gap-3">
                <div className="w-8 h-8 rounded-lg bg-muted flex items-center justify-center flex-shrink-0">
                  <Flag size={14} className="text-muted-foreground" />
                </div>
                <div className="flex-1">
                  <p className="text-xs text-muted-foreground font-medium uppercase tracking-wider mb-1">Change Status</p>
                  <FloatingMenu
                    open={statusMenuOpen}
                    onOpenChange={setStatusMenuOpen}
                    align="start"
                    widthClassName="w-40"
                    trigger={(triggerProps) => (
                      <button
                        {...triggerProps}
                        disabled={updatingStatus}
                        className="flex items-center gap-2 text-sm font-medium text-foreground hover:text-primary transition-colors disabled:opacity-60"
                      >
                        {updatingStatus ? <Loader2 size={12} className="animate-spin" /> : null}
                        {currentStatus.replace(/_/g, ' ')}
                        <ChevronDown size={13} />
                      </button>
                    )}
                  >
                    {getAllowedTaskTransitions(currentStatus).map(s => (
                      <button
                        key={s}
                        onClick={() => handleStatusChange(s)}
                        className={`flex items-center gap-2 w-full px-3 py-2 text-xs hover:bg-muted transition-colors ${currentStatus === s ? 'text-primary font-semibold' : 'text-foreground'}`}
                      >
                        {s.replace(/_/g, ' ')}
                      </button>
                    ))}
                  </FloatingMenu>
                </div>
              </div>
            </div>
          </div>

          {/* Submit Work */}
          <div className="card-elevated p-5">
            <h2 className="text-sm font-semibold text-foreground mb-4">Submit Work</h2>
            <form onSubmit={handleSubmitWork} className="space-y-4">
              <div>
                <textarea
                  rows={4}
                  value={submitContent}
                  onChange={e => setSubmitContent(e.target.value)}
                  placeholder="Describe what you've done, include links, notes, or progress updates…"
                  className="input-base resize-none text-sm"
                />
              </div>

              {/* Drop zone */}
              <div
                onDragOver={e => { e.preventDefault(); setDragging(true); }}
                onDragLeave={() => setDragging(false)}
                onDrop={handleDrop}
                onClick={() => fileInputRef.current?.click()}
                className={`border-2 border-dashed rounded-xl p-5 text-center cursor-pointer transition-all ${dragging ? 'border-primary bg-primary/5' : 'border-border hover:border-primary/50 hover:bg-muted/40'}`}
              >
                <Upload size={20} className="mx-auto mb-2 text-muted-foreground" />
                <p className="text-sm text-muted-foreground">
                  <span className="font-medium text-foreground">Click to attach files</span> or drag & drop
                </p>
                <p className="text-xs text-muted-foreground mt-0.5">Images, PDFs, documents, archives</p>
                <input
                  ref={fileInputRef}
                  type="file"
                  multiple
                  accept={TASK_FILE_ACCEPT}
                  className="hidden"
                  onChange={e => {
                    addFiles(e.target.files);
                    e.target.value = '';
                  }}
                />
              </div>

              {/* Staged files */}
              {submitFiles.length > 0 && (
                <div className="space-y-2">
                  {submitFiles.map((file, i) => (
                    <div key={i} className="flex items-center gap-3 p-2.5 bg-muted/50 rounded-lg">
                      <FileIcon fileName={file.name} />
                      <div className="flex-1 min-w-0">
                        <p className="text-xs font-medium text-foreground truncate">{file.name}</p>
                        <p className="text-xs text-muted-foreground">{formatBytes(file.size)}</p>
                      </div>
                      <button
                        type="button"
                        onClick={() => setSubmitFiles(prev => prev.filter((_, idx) => idx !== i))}
                        className="w-6 h-6 rounded-md flex items-center justify-center text-muted-foreground hover:text-red-600 hover:bg-red-50 transition-all"
                      >
                        <X size={12} />
                      </button>
                    </div>
                  ))}
                </div>
              )}

              <div className="flex justify-end">
                <button
                  type="submit"
                  disabled={submitting || (!submitContent.trim() && submitFiles.length === 0)}
                  className="btn-primary px-5 py-2.5 rounded-lg text-sm font-semibold flex items-center gap-2 disabled:opacity-60"
                >
                  {submitting ? (
                    <><Loader2 size={14} className="animate-spin" />{uploadingFiles ? 'Uploading…' : 'Submitting…'}</>
                  ) : (
                    <><Send size={14} />Submit Work</>
                  )}
                </button>
              </div>
            </form>
          </div>

          {/* Submissions */}
          <div className="card-elevated p-5">
            <div className="flex items-center justify-between mb-4">
              <h2 className="text-sm font-semibold text-foreground">Submissions</h2>
              <span className="text-xs text-muted-foreground">{submissions.length} total</span>
            </div>
            {submissions.length === 0 ? (
              <div className="py-8 text-center">
                <Send size={24} className="text-muted-foreground/30 mx-auto mb-2" />
                <p className="text-xs text-muted-foreground">No submissions yet</p>
              </div>
            ) : (
              <div className="space-y-3">
                {submissions.map(sub => (
                  <div key={sub.id} className="p-4 bg-muted/30 rounded-xl border border-border">
                    <div className="flex items-start justify-between gap-3 mb-2">
                      <div className="flex items-center gap-2">
                        <div className="w-7 h-7 rounded-full bg-primary/10 flex items-center justify-center">
                          <User size={13} className="text-primary" />
                        </div>
                        <span className="text-xs font-medium text-foreground">Version {sub.version} · {sub.authorId}</span>
                      </div>
                    </div>
                    {sub.note && (
                      <p className="text-sm text-foreground leading-relaxed mb-2">{sub.note}</p>
                    )}
                    <div className="flex items-center gap-3 mt-2">
                      {sub.submittedAt && (
                        <span className="text-xs text-muted-foreground flex items-center gap-1">
                          <Clock size={10} />
                          Submitted {new Date(sub.submittedAt).toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' })}
                        </span>
                      )}
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>

          {/* Attachments */}
          <div className="card-elevated p-5">
            <div className="flex items-center justify-between mb-4">
              <h2 className="text-sm font-semibold text-foreground">Attachments</h2>
              <span className="text-xs text-muted-foreground">{attachments.length} files</span>
            </div>
            {attachments.length === 0 ? (
              <div className="py-8 text-center">
                <Paperclip size={24} className="text-muted-foreground/30 mx-auto mb-2" />
                <p className="text-xs text-muted-foreground">No attachments yet</p>
              </div>
            ) : (
              <div className="space-y-2">
                {attachments.map(att => (
                  <div key={att.id} className="flex items-center gap-3 p-3 bg-muted/30 rounded-xl border border-border group">
                    <FileIcon fileName={att.fileName} />
                    <div className="flex-1 min-w-0">
                      <p className="text-sm font-medium text-foreground truncate">{att.fileName}</p>
                      <div className="flex items-center gap-2 mt-0.5">
                        {att.description && <span className="text-xs text-muted-foreground">{att.description}</span>}
                        {att.uploadedAt && (
                          <span className="text-xs text-muted-foreground">
                            · {new Date(att.uploadedAt).toLocaleDateString('en-US', { month: 'short', day: 'numeric' })}
                          </span>
                        )}
                      </div>
                    </div>
                    <div className="flex items-center gap-1 opacity-0 group-hover:opacity-100 transition-opacity">
                      <button
                        type="button"
                        onClick={() => handleDownloadAttachment(att)}
                        disabled={downloadingAttachmentId === att.id}
                        className="w-7 h-7 rounded-md flex items-center justify-center text-muted-foreground hover:text-primary hover:bg-primary/10 transition-all disabled:opacity-50"
                        title="Download"
                        aria-label={`Download ${att.fileName}`}
                      >
                        {downloadingAttachmentId === att.id
                          ? <Loader2 size={13} className="animate-spin" />
                          : <Download size={13} />}
                      </button>
                      {(canManageTask || att.uploadedBy === currentUserId) && (
                        <button
                          type="button"
                          onClick={() => handleRemoveAttachment(att.id)}
                          className="w-7 h-7 rounded-md flex items-center justify-center text-muted-foreground hover:text-red-600 hover:bg-red-50 transition-all"
                          title="Remove"
                          aria-label={`Remove ${att.fileName}`}
                        >
                          <Trash2 size={13} />
                        </button>
                      )}
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>

        {/* Right column — assignees + status history */}
        <div className="space-y-5">

          {/* Assignees */}
          <div className="card-elevated p-5">
            <div className="flex items-center gap-2 mb-4">
              <Users size={15} className="text-muted-foreground" />
              <h2 className="text-sm font-semibold text-foreground">Assignees</h2>
              <span className="ml-auto text-xs text-muted-foreground">{assignees.length}</span>
            </div>
            {canManageTask && (
              <div className="flex gap-2 mb-4">
                <select
                  value={selectedAssigneeId}
                  onChange={event => setSelectedAssigneeId(event.target.value)}
                  className="input-base text-xs"
                >
                  <option value="">Select a department Developer or Tester</option>
                  {eligibleMembers
                    .filter(member => member.userId !== task.createdBy
                      && !assignees.some(assignee => assignee.userId === member.userId))
                    .map(member => (
                      <option key={member.userId} value={member.userId}>
                        {member.email} ({member.role})
                      </option>
                    ))}
                </select>
                <button
                  type="button"
                  onClick={handleAssignMember}
                  disabled={!selectedAssigneeId || assigning}
                  className="btn-primary px-3 rounded-lg text-xs font-semibold disabled:opacity-50"
                  aria-label="Assign task member"
                >
                  {assigning ? <Loader2 size={13} className="animate-spin" /> : <UserPlus size={13} />}
                </button>
              </div>
            )}
            {assignees.length === 0 ? (
              <div className="py-6 text-center">
                <Users size={22} className="text-muted-foreground/30 mx-auto mb-2" />
                <p className="text-xs text-muted-foreground">No assignees</p>
              </div>
            ) : (
              <div className="space-y-2.5">
                {assignees.map(a => {
                  const member = eligibleMembers.find(candidate => candidate.userId === a.userId);
                  const displayName = member
                    ? `${member.firstName ?? ''} ${member.lastName ?? ''}`.trim() || member.email
                    : a.userId;
                  return (
                    <div key={a.userId} className="flex items-center gap-3">
                      <div className="w-8 h-8 rounded-full bg-primary/10 flex items-center justify-center flex-shrink-0">
                        <span className="text-xs font-bold text-primary">
                          {displayName.slice(0, 2).toUpperCase()}
                        </span>
                      </div>
                      <div className="min-w-0">
                        <p className="text-sm font-medium text-foreground truncate">{displayName}</p>
                        <p className="text-xs text-muted-foreground truncate">{a.status.replace(/_/g, ' ')}</p>
                      </div>
                      {canManageTask && (
                        <button
                          type="button"
                          onClick={() => handleRemoveAssignee(a.userId)}
                          className="w-7 h-7 rounded-md flex items-center justify-center text-muted-foreground hover:text-red-600 hover:bg-red-50"
                          title="Remove assignee"
                        >
                          <UserMinus size={13} />
                        </button>
                      )}
                    </div>
                  );
                })}
              </div>
            )}
          </div>

          {/* Status History */}
          <div className="card-elevated p-5">
            <div className="flex items-center gap-2 mb-4">
              <Clock size={15} className="text-muted-foreground" />
              <h2 className="text-sm font-semibold text-foreground">Status History</h2>
            </div>
            {statusHistory.length === 0 ? (
              <p className="text-xs text-muted-foreground text-center py-4">No history available</p>
            ) : (
              <div className="relative">
                <div className="absolute left-3.5 top-0 bottom-0 w-px bg-border" />
                <div className="space-y-4">
                  {statusHistory.map((entry, i) => (
                    <div key={i} className="flex items-start gap-3 relative">
                      <div className={`w-7 h-7 rounded-full flex items-center justify-center flex-shrink-0 z-10 ${i === statusHistory.length - 1 ? 'bg-primary text-primary-foreground' : 'bg-muted border border-border'}`}>
                        <CheckSquare size={12} className={i === statusHistory.length - 1 ? 'text-white' : 'text-muted-foreground'} />
                      </div>
                      <div className="pt-0.5 min-w-0">
                        <p className="text-xs font-semibold text-foreground">
                          {STATUS_HISTORY_LABELS[entry.toStatus] ?? entry.toStatus.replace(/_/g, ' ')}
                        </p>
                        <p className="text-xs text-muted-foreground mt-0.5">
                          {new Date(entry.changedAt).toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' })}
                        </p>
                        {entry.reason && <p className="text-xs text-muted-foreground mt-1">{entry.reason}</p>}
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
