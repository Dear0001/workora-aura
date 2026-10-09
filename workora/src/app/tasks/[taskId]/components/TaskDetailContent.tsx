'use client';
import React, { useCallback, useEffect, useRef, useState } from 'react';
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
  Download,
  FileText,
  ImageIcon,
  Clock,
  ChevronDown,
  User,
  X,
  Upload,
} from 'lucide-react';
import Link from 'next/link';
import { toast } from 'sonner';
import { AxiosError } from 'axios';
import { workApi, type Task, type TaskAssignee, type TaskSubmission, type TaskAttachment } from '@/lib/api/work';
import { projectsApi, type Phase } from '@/lib/api/projects';

const STATUS_COLORS: Record<string, string> = {
  OPEN: 'bg-muted text-muted-foreground border border-border',
  IN_PROGRESS: 'bg-blue-50 text-blue-700 border border-blue-200',
  IN_REVIEW: 'bg-amber-50 text-amber-700 border border-amber-200',
  DONE: 'bg-green-50 text-green-700 border border-green-200',
  CANCELLED: 'bg-red-50 text-red-600 border border-red-200',
};

const SUBMISSION_STATUS_COLORS: Record<string, string> = {
  PENDING: 'bg-amber-50 text-amber-700 border border-amber-200',
  APPROVED: 'bg-green-50 text-green-700 border border-green-200',
  REJECTED: 'bg-red-50 text-red-600 border border-red-200',
};

const PRIORITY_COLORS: Record<string, string> = {
  LOW: 'text-muted-foreground',
  MEDIUM: 'text-amber-600',
  HIGH: 'text-orange-600',
  CRITICAL: 'text-red-600',
};

const STATUS_HISTORY_LABELS: Record<string, string> = {
  OPEN: 'Task opened',
  IN_PROGRESS: 'Work started',
  IN_REVIEW: 'Submitted for review',
  DONE: 'Marked as done',
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

interface StatusHistoryEntry {
  status: string;
  timestamp: string;
}

interface Props {
  taskId: string;
}

export default function TaskDetailContent({ taskId }: Props) {
  const [task, setTask] = useState<Task | null>(null);
  const [phase, setPhase] = useState<Phase | null>(null);
  const [assignees, setAssignees] = useState<TaskAssignee[]>([]);
  const [submissions, setSubmissions] = useState<TaskSubmission[]>([]);
  const [attachments, setAttachments] = useState<TaskAttachment[]>([]);
  const [statusHistory, setStatusHistory] = useState<StatusHistoryEntry[]>([]);
  const [loading, setLoading] = useState(true);

  // Submit-work form state
  const [submitContent, setSubmitContent] = useState('');
  const [submitFiles, setSubmitFiles] = useState<File[]>([]);
  const [submitting, setSubmitting] = useState(false);
  const [uploadingFiles, setUploadingFiles] = useState(false);
  const [dragging, setDragging] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);

  // Status change
  const [statusMenuOpen, setStatusMenuOpen] = useState(false);
  const [updatingStatus, setUpdatingStatus] = useState(false);

  const loadAll = useCallback(async () => {
    setLoading(true);
    try {
      const [taskRes, assigneesRes, submissionsRes, attachmentsRes] = await Promise.all([
        workApi.getTask(taskId),
        workApi.listAssignees(taskId),
        workApi.listSubmissions(taskId),
        workApi.listAttachments(taskId),
      ]);
      const t = taskRes.data;
      setTask(t);
      setAssignees(assigneesRes.data ?? []);
      setSubmissions(submissionsRes.data ?? []);
      setAttachments(attachmentsRes.data ?? []);

      // Build synthetic status history from task timestamps
      const history: StatusHistoryEntry[] = [];
      if (t.createdAt) history.push({ status: 'OPEN', timestamp: t.createdAt });
      if (t.status && t.status !== 'OPEN' && t.updatedAt) {
        history.push({ status: t.status, timestamp: t.updatedAt });
      }
      setStatusHistory(history);

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

  const handleStatusChange = async (status: string) => {
    if (!task) return;
    setUpdatingStatus(true);
    setStatusMenuOpen(false);
    try {
      const res = await workApi.updateTaskStatus(taskId, status);
      setTask(res.data);
      setStatusHistory(prev => [...prev, { status, timestamp: new Date().toISOString() }]);
      toast.success('Status updated');
    } catch (err) {
      toast.error(err instanceof AxiosError ? (err.response?.data?.message ?? 'Failed to update status') : 'Failed to update status');
    } finally {
      setUpdatingStatus(false);
    }
  };

  const addFiles = (files: FileList | null) => {
    if (!files) return;
    const arr = Array.from(files);
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
        const subRes = await workApi.submitWork(taskId, { content: submitContent.trim() });
        setSubmissions(prev => [subRes.data, ...prev]);
      }

      // 2. Upload attachments
      if (submitFiles.length > 0) {
        setUploadingFiles(true);
        const uploaded: TaskAttachment[] = [];
        for (const file of submitFiles) {
          try {
            const fd = new FormData();
            fd.append('file', file);
            const attRes = await workApi.uploadAttachment(taskId, fd);
            uploaded.push(attRes.data);
          } catch { /* continue uploading rest */ }
        }
        setAttachments(prev => [...uploaded, ...prev]);
        setUploadingFiles(false);
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
    try {
      await workApi.removeAttachment(taskId, attachmentId);
      setAttachments(prev => prev.filter(a => a.id !== attachmentId));
      toast.success('Attachment removed');
    } catch {
      toast.error('Failed to remove attachment');
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

  const currentStatus = task.status ?? 'OPEN';
  const currentPriority = task.priority ?? 'MEDIUM';

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
            <span className={`badge-base text-xs ${STATUS_COLORS[currentStatus] ?? STATUS_COLORS.OPEN}`}>
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
                  <div className="relative">
                    <button
                      onClick={() => setStatusMenuOpen(v => !v)}
                      disabled={updatingStatus}
                      className="flex items-center gap-2 text-sm font-medium text-foreground hover:text-primary transition-colors disabled:opacity-60"
                    >
                      {updatingStatus ? <Loader2 size={12} className="animate-spin" /> : null}
                      {currentStatus.replace(/_/g, ' ')}
                      <ChevronDown size={13} />
                    </button>
                    {statusMenuOpen && (
                      <>
                        <div className="fixed inset-0 z-10" onClick={() => setStatusMenuOpen(false)} />
                        <div className="absolute left-0 top-full mt-1 w-40 bg-card border border-border rounded-xl shadow-elevated z-20 py-1 fade-scale-in">
                          {['OPEN', 'IN_PROGRESS', 'IN_REVIEW', 'DONE', 'CANCELLED'].map(s => (
                            <button
                              key={s}
                              onClick={() => handleStatusChange(s)}
                              className={`flex items-center gap-2 w-full px-3 py-2 text-xs hover:bg-muted transition-colors ${currentStatus === s ? 'text-primary font-semibold' : 'text-foreground'}`}
                            >
                              {s.replace(/_/g, ' ')}
                            </button>
                          ))}
                        </div>
                      </>
                    )}
                  </div>
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
                  className="hidden"
                  onChange={e => addFiles(e.target.files)}
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
                        <span className="text-xs font-medium text-foreground">{sub.submittedBy}</span>
                      </div>
                      <span className={`badge-base text-xs ${SUBMISSION_STATUS_COLORS[sub.status ?? 'PENDING'] ?? SUBMISSION_STATUS_COLORS.PENDING}`}>
                        {sub.status ?? 'PENDING'}
                      </span>
                    </div>
                    {sub.content && (
                      <p className="text-sm text-foreground leading-relaxed mb-2">{sub.content}</p>
                    )}
                    {sub.reviewNote && (
                      <div className="mt-2 p-2.5 bg-amber-50 border border-amber-200 rounded-lg">
                        <p className="text-xs text-amber-700"><span className="font-semibold">Review note:</span> {sub.reviewNote}</p>
                      </div>
                    )}
                    <div className="flex items-center gap-3 mt-2">
                      {sub.submittedAt && (
                        <span className="text-xs text-muted-foreground flex items-center gap-1">
                          <Clock size={10} />
                          Submitted {new Date(sub.submittedAt).toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' })}
                        </span>
                      )}
                      {sub.reviewedAt && (
                        <span className="text-xs text-muted-foreground flex items-center gap-1">
                          <CheckSquare size={10} />
                          Reviewed {new Date(sub.reviewedAt).toLocaleDateString('en-US', { month: 'short', day: 'numeric' })}
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
                        {att.fileSize && <span className="text-xs text-muted-foreground">{formatBytes(att.fileSize)}</span>}
                        {att.uploadedAt && (
                          <span className="text-xs text-muted-foreground">
                            · {new Date(att.uploadedAt).toLocaleDateString('en-US', { month: 'short', day: 'numeric' })}
                          </span>
                        )}
                      </div>
                    </div>
                    <div className="flex items-center gap-1 opacity-0 group-hover:opacity-100 transition-opacity">
                      {att.fileUrl && (
                        <a
                          href={att.fileUrl}
                          target="_blank"
                          rel="noopener noreferrer"
                          className="w-7 h-7 rounded-md flex items-center justify-center text-muted-foreground hover:text-primary hover:bg-primary/10 transition-all"
                          title="Download"
                        >
                          <Download size={13} />
                        </a>
                      )}
                      <button
                        onClick={() => handleRemoveAttachment(att.id)}
                        className="w-7 h-7 rounded-md flex items-center justify-center text-muted-foreground hover:text-red-600 hover:bg-red-50 transition-all"
                        title="Remove"
                      >
                        <Trash2 size={13} />
                      </button>
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
            {assignees.length === 0 ? (
              <div className="py-6 text-center">
                <Users size={22} className="text-muted-foreground/30 mx-auto mb-2" />
                <p className="text-xs text-muted-foreground">No assignees</p>
              </div>
            ) : (
              <div className="space-y-2.5">
                {assignees.map(a => (
                  <div key={a.userId} className="flex items-center gap-3">
                    <div className="w-8 h-8 rounded-full bg-primary/10 flex items-center justify-center flex-shrink-0">
                      <span className="text-xs font-bold text-primary">
                        {a.firstName?.[0]?.toUpperCase()}{a.lastName?.[0]?.toUpperCase()}
                      </span>
                    </div>
                    <div className="min-w-0">
                      <p className="text-sm font-medium text-foreground truncate">{a.firstName} {a.lastName}</p>
                      <p className="text-xs text-muted-foreground truncate">{a.email}</p>
                    </div>
                  </div>
                ))}
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
                          {STATUS_HISTORY_LABELS[entry.status] ?? entry.status.replace(/_/g, ' ')}
                        </p>
                        <p className="text-xs text-muted-foreground mt-0.5">
                          {new Date(entry.timestamp).toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' })}
                        </p>
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
