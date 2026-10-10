'use client';
import React, { useCallback, useEffect, useState } from 'react';
import { CheckSquare, Plus, Search, MoreVertical, Archive, Loader2, Calendar, Flag, UserPlus, Upload, X, Paperclip, Pencil } from 'lucide-react';
import FloatingMenu from '@/components/ui/FloatingMenu';
import { toast } from 'sonner';
import { AxiosError } from 'axios';
import { getAllowedTaskTransitions, workApi, type Task, type TaskAssignee } from '@/lib/api/work';
import { projectsApi, type Phase, type Project } from '@/lib/api/projects';
import { organizationApi } from '@/lib/api/organization';
import type { Company, CompanyMember } from '@/lib/api/types';
import { formatDateOnly } from '@/lib/date';
import { getTaskFileValidationError, TASK_FILE_ACCEPT } from '@/lib/task-files';
import Link from 'next/link';
import { getStoredUser } from '@/lib/auth';

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

function formatFileSize(size: number): string {
  return size >= 1024 * 1024
    ? `${(size / (1024 * 1024)).toFixed(1)} MB`
    : `${Math.max(1, Math.round(size / 1024))} KB`;
}

interface TaskFormModalProps {
  phaseId: string;
  task?: Task;
  onClose: () => void;
  onSaved: (task: Task) => void;
}

function TaskFormModal({ phaseId, task, onClose, onSaved }: TaskFormModalProps) {
  const [title, setTitle] = useState(task?.title ?? '');
  const [description, setDescription] = useState(task?.description ?? '');
  const [priority, setPriority] = useState<'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL'>(task?.priority ?? 'MEDIUM');
  const [dueDate, setDueDate] = useState(task?.dueDate ?? '');
  const [files, setFiles] = useState<File[]>([]);
  const [loading, setLoading] = useState(false);

  const handleFileSelection = (event: React.ChangeEvent<HTMLInputElement>) => {
    const selected = Array.from(event.target.files ?? []);
    const accepted: File[] = [];
    for (const file of selected) {
      const error = getTaskFileValidationError(file);
      if (error) toast.error(`${file.name}: ${error}`);
      else accepted.push(file);
    }
    setFiles(current => [...current, ...accepted]);
    event.target.value = '';
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!title.trim()) return;
    setLoading(true);
    try {
      const savedTask = task
        ? (await workApi.updateTask(task.id, {
            title: title.trim(),
            description,
            priority,
            ...(dueDate ? { dueDate } : {}),
          })).data
        : (await workApi.createTask(phaseId, {
            title: title.trim(),
            description: description || undefined,
            priority,
            dueDate: dueDate || undefined,
          })).data;

      onSaved(savedTask);
      if (files.length === 0) {
        toast.success(`Task "${savedTask.title}" ${task ? 'updated' : 'created'}`);
        onClose();
        return;
      }

      const failedFiles: string[] = [];
      for (const file of files) {
        try {
          await workApi.uploadTaskFile(savedTask.id, file);
        } catch {
          failedFiles.push(file.name);
        }
      }
      if (failedFiles.length > 0) {
        toast.success(`Task "${savedTask.title}" was saved`);
        toast.error(`Could not attach: ${failedFiles.join(', ')}. Edit the task to retry.`);
      } else {
        toast.success(`Task "${savedTask.title}" ${task ? 'updated' : 'created'} with ${files.length} attachment${files.length === 1 ? '' : 's'}`);
      }
      onClose();
    } catch (err) {
      toast.error(err instanceof AxiosError
        ? (err.response?.data?.message ?? `Failed to ${task ? 'update' : 'create'} task`)
        : `Failed to ${task ? 'update' : 'create'} task`);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 z-[1100] flex items-center justify-center p-4">
      <button
        type="button"
        aria-label="Close task form"
        disabled={loading}
        className="fixed inset-0 border-0 bg-black/40 p-0 fade-in disabled:cursor-wait"
        onClick={onClose}
      />
      <section
        role="dialog"
        aria-modal="true"
        aria-labelledby="task-form-title"
        className="relative max-h-[calc(100dvh-2rem)] w-full max-w-md overflow-y-auto rounded-2xl bg-card shadow-modal fade-scale-in"
      >
        <div className="flex items-center justify-between p-6 border-b border-border">
          <div>
            <h2 id="task-form-title" className="text-base font-bold text-foreground">{task ? 'Edit Task' : 'New Task'}</h2>
            <p className="text-xs text-muted-foreground">{task ? 'Update task details and attachments' : 'Add a task to this phase'}</p>
          </div>
          <button type="button" disabled={loading} aria-label="Close task form" onClick={onClose} className="w-8 h-8 rounded-lg flex items-center justify-center text-muted-foreground hover:text-foreground hover:bg-muted transition-all disabled:opacity-50">✕</button>
        </div>
        <form onSubmit={handleSubmit} className="p-6 space-y-4">
          <div>
            <label className="block text-sm font-medium text-foreground mb-1.5">Title *</label>
            <input type="text" value={title} onChange={e => setTitle(e.target.value)} placeholder="Implement login screen" className="input-base" required />
          </div>
          <div>
            <label className="block text-sm font-medium text-foreground mb-1.5">Description</label>
            <textarea rows={2} value={description} onChange={e => setDescription(e.target.value)} className="input-base resize-none" />
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-sm font-medium text-foreground mb-1.5">Priority</label>
              <select
                value={priority}
                onChange={e => {
                  const value = e.target.value;
                  if (value === 'LOW' || value === 'MEDIUM' || value === 'HIGH' || value === 'CRITICAL') {
                    setPriority(value);
                  }
                }}
                className="input-base"
              >
                <option value="LOW">Low</option>
                <option value="MEDIUM">Medium</option>
                <option value="HIGH">High</option>
                <option value="CRITICAL">Critical</option>
              </select>
            </div>
            <div>
              <label className="block text-sm font-medium text-foreground mb-1.5">Due Date</label>
              <input type="date" value={dueDate} onChange={e => setDueDate(e.target.value)} className="input-base" />
            </div>
          </div>
          <div>
            <label className="block text-sm font-medium text-foreground mb-1.5">File Upload</label>
            <label className="flex cursor-pointer items-center justify-center gap-2 rounded-lg border border-dashed border-border px-4 py-3 text-sm text-muted-foreground hover:border-primary/50 hover:bg-muted/40">
              <Upload size={15} />
              Choose files (25 MB max each)
              <input type="file" multiple accept={TASK_FILE_ACCEPT} className="hidden" onChange={handleFileSelection} />
            </label>
            <p className="mt-1 text-xs text-muted-foreground">Images, PDF, TXT, LOG, MP4, MOV, and ZIP files.</p>
            {files.length > 0 && (
              <ul className="mt-2 space-y-1.5">
                {files.map((file, index) => (
                  <li key={`${file.name}-${file.size}-${index}`} className="flex items-center gap-2 rounded-md bg-muted/40 px-2.5 py-2">
                    <Paperclip size={13} className="shrink-0 text-muted-foreground" />
                    <span className="min-w-0 flex-1 truncate text-xs text-foreground">{file.name}</span>
                    <span className="shrink-0 text-xs text-muted-foreground">{formatFileSize(file.size)}</span>
                    <button
                      type="button"
                      aria-label={`Remove ${file.name}`}
                      onClick={() => setFiles(current => current.filter((_, fileIndex) => fileIndex !== index))}
                      className="flex h-6 w-6 shrink-0 items-center justify-center rounded text-muted-foreground hover:bg-background hover:text-foreground"
                    >
                      <X size={13} />
                    </button>
                  </li>
                ))}
              </ul>
            )}
          </div>
          <div className="flex items-center gap-3 pt-2">
            <button type="button" disabled={loading} onClick={onClose} className="btn-secondary flex-1 py-2.5 rounded-lg font-medium text-sm disabled:opacity-50">Cancel</button>
            <button type="submit" disabled={loading || !title.trim()} className="btn-primary flex-1 py-2.5 rounded-lg font-semibold text-sm flex items-center justify-center gap-2 disabled:opacity-60">
              {loading ? <><Loader2 size={14} className="animate-spin" /><span>Saving…</span></> : task ? 'Save Changes' : 'Create Task'}
            </button>
          </div>
        </form>
      </section>
    </div>
  );
}

interface AssignTaskModalProps {
  task: Task;
  onClose: () => void;
  onAssigned: (taskId: string, member: CompanyMember) => void;
}

function AssignTaskModal({ task, onClose, onAssigned }: AssignTaskModalProps) {
  const [members, setMembers] = useState<CompanyMember[]>([]);
  const [assignees, setAssignees] = useState<TaskAssignee[]>([]);
  const [selectedMemberId, setSelectedMemberId] = useState('');
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState(false);
  const [assigning, setAssigning] = useState(false);

  useEffect(() => {
    let cancelled = false;
    Promise.all([
      workApi.getTask(task.id),
      organizationApi.listDepartmentMembers(task.departmentId),
    ]).then(([taskResponse, membersResponse]) => {
      if (cancelled) return;
      setAssignees(taskResponse.data.assignees ?? []);
      setMembers(membersResponse.data.filter(member => {
        if (member.status?.toUpperCase() !== 'ACTIVE') return false;
        const departmentRole = member.departmentMemberships
          ?.find(assignment => assignment.departmentId === task.departmentId)?.role
          ?? (member.departmentId === task.departmentId ? member.role : undefined);
        return departmentRole === 'DEVELOPER' || departmentRole === 'TESTER';
      }));
    }).catch(err => {
      if (!cancelled) {
        setLoadError(true);
        toast.error(err instanceof AxiosError
          ? (err.response?.data?.message ?? 'Failed to load eligible task assignees')
          : 'Failed to load eligible task assignees');
      }
    }).finally(() => {
      if (!cancelled) setLoading(false);
    });
    return () => { cancelled = true; };
  }, [task.departmentId, task.id]);

  const availableMembers = members.filter(member =>
    member.userId !== task.createdBy
      && !assignees.some(assignee => assignee.userId === member.userId)
  );

  const handleAssign = async () => {
    const member = availableMembers.find(candidate => candidate.userId === selectedMemberId);
    if (!member) {
      if (selectedMemberId === task.createdBy) {
        toast.error('You cannot assign a task you created to yourself. Please select another eligible member.');
      }
      return;
    }
    setAssigning(true);
    try {
      const response = await workApi.assignUser(task.id, { userId: member.userId });
      setAssignees(current => [...current, response.data]);
      setSelectedMemberId('');
      onAssigned(task.id, member);
      toast.success(`${member.firstName || member.email} assigned to task`);
    } catch (err) {
      toast.error(err instanceof AxiosError
        ? (err.response?.data?.message ?? 'Failed to assign task')
        : 'Failed to assign task');
    } finally {
      setAssigning(false);
    }
  };

  return (
    <div className="fixed inset-0 z-[1100] flex items-center justify-center p-4">
      <button
        type="button"
        aria-label="Close assignment dialog"
        className="fixed inset-0 bg-black/40 fade-in"
        onClick={onClose}
      />
      <section
        role="dialog"
        aria-modal="true"
        aria-labelledby="assign-task-title"
        className="relative max-h-[calc(100dvh-2rem)] w-full max-w-md overflow-y-auto rounded-2xl bg-card shadow-modal fade-scale-in"
      >
        <div className="flex items-center justify-between p-5 border-b border-border">
          <div>
            <h2 id="assign-task-title" className="text-base font-bold text-foreground">Assign Task</h2>
            <p className="text-xs text-muted-foreground mt-1">{task.title}</p>
          </div>
          <button
            type="button"
            onClick={onClose}
            aria-label="Close assignment dialog"
            className="w-8 h-8 rounded-lg flex items-center justify-center text-muted-foreground hover:text-foreground hover:bg-muted transition-all"
          >
            ×
          </button>
        </div>
        <div className="p-5 space-y-4">
          {assignees.length > 0 && (
            <div>
              <p className="text-xs font-semibold text-muted-foreground mb-2">Currently assigned</p>
              <ul className="space-y-1 text-sm text-foreground">
                {assignees.map(assignee => {
                  const member = members.find(candidate => candidate.userId === assignee.userId);
                  return <li key={assignee.id}>{member?.email ?? assignee.userId}</li>;
                })}
              </ul>
            </div>
          )}
          <div>
            <label htmlFor="task-assignee" className="block text-sm font-medium text-foreground mb-1.5">
              Eligible department member
            </label>
            <select
              id="task-assignee"
              value={selectedMemberId}
              onChange={event => setSelectedMemberId(event.target.value)}
              disabled={loading || assigning || availableMembers.length === 0}
              className="input-base"
            >
              <option value="">
                {loading ? 'Loading eligible members…' : 'Select a Developer or Tester'}
              </option>
              {availableMembers.map(member => (
                <option key={member.userId} value={member.userId}>
                  {member.firstName || member.lastName
                    ? `${member.firstName} ${member.lastName}`.trim()
                    : member.email} ({member.email})
                </option>
              ))}
            </select>
            {!loading && loadError && (
              <p className="mt-2 text-xs text-red-600">
                Eligible members could not be loaded. Close this dialog and try again.
              </p>
            )}
            {!loading && !loadError && availableMembers.length === 0 && (
              <p className="mt-2 text-xs text-muted-foreground">
                There are no unassigned active Developers or Testers in this task&apos;s department.
              </p>
            )}
          </div>
          <div className="flex items-center gap-3 pt-1">
            <button type="button" onClick={onClose} className="btn-secondary flex-1 py-2.5 rounded-lg font-medium text-sm">
              Close
            </button>
            <button
              type="button"
              onClick={handleAssign}
              disabled={!selectedMemberId || loading || assigning}
              className="btn-primary flex-1 py-2.5 rounded-lg font-semibold text-sm flex items-center justify-center gap-2 disabled:opacity-60"
            >
              {assigning ? <Loader2 size={14} className="animate-spin" /> : <UserPlus size={14} />}
              Assign Task
            </button>
          </div>
        </div>
      </section>
    </div>
  );
}

interface Props {
  initialPhaseId?: string;
}

async function mapInBatches<T, R>(items: T[], batchSize: number, load: (item: T) => Promise<R>): Promise<R[]> {
  const results: R[] = [];
  for (let offset = 0; offset < items.length; offset += batchSize) {
    const batch = items.slice(offset, offset + batchSize);
    results.push(...await Promise.all(batch.map(load)));
  }
  return results;
}

export default function TasksContent({ initialPhaseId }: Props) {
  const [tasks, setTasks] = useState<Task[]>([]);
  const [phases, setPhases] = useState<Phase[]>([]);
  const [companies, setCompanies] = useState<Company[]>([]);
  const [projectNames, setProjectNames] = useState<Record<string, string>>({});
  const [projectsById, setProjectsById] = useState<Record<string, Project>>({});
  const [companyMemberships, setCompanyMemberships] = useState<Record<string, CompanyMember>>({});
  const [currentUserId, setCurrentUserId] = useState<string | null>(null);
  const [assignedMemberEmails, setAssignedMemberEmails] = useState<Record<string, string[]>>({});
  const [companyFilter, setCompanyFilter] = useState('ALL');
  const [selectedPhaseId, setSelectedPhaseId] = useState<string>(initialPhaseId ?? '');
  const [loading, setLoading] = useState(false);
  const [loadingPhases, setLoadingPhases] = useState(true);
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [createOpen, setCreateOpen] = useState(false);
  const [editingTask, setEditingTask] = useState<Task | null>(null);
  const [assignTask, setAssignTask] = useState<Task | null>(null);
  const [activeMenu, setActiveMenu] = useState<string | null>(null);

  // Load company, project, and phase context so the task view covers every active membership.
  useEffect(() => {
    (async () => {
      setLoadingPhases(true);
      try {
        const firstCompanyPage = await organizationApi.listCompanies({ page: 0, size: 100 });
        const allCompanies = [...(firstCompanyPage.data?.content ?? [])];
        for (let page = 1; page < (firstCompanyPage.data?.totalPages ?? 0); page += 1) {
          const response = await organizationApi.listCompanies({ page, size: 100 });
          allCompanies.push(...(response.data?.content ?? []));
        }
        setCompanies(allCompanies);
        const projectPages = await mapInBatches(allCompanies, 10, async company => {
          const firstPage = await projectsApi.listCompanyProjects(company.id, { page: 0, size: 100 });
          const companyProjects = [...(firstPage.data?.content ?? [])];
          for (let page = 1; page < (firstPage.data?.totalPages ?? 0); page += 1) {
            const response = await projectsApi.listCompanyProjects(company.id, { page, size: 100 });
            companyProjects.push(...(response.data?.content ?? []));
          }
          return companyProjects;
        });
        const allProjects = projectPages.flat();
        setProjectNames(Object.fromEntries(allProjects.map(project => [project.id, project.name])));
        setProjectsById(Object.fromEntries(allProjects.map(project => [project.id, project])));
        const phasePages = await mapInBatches(allProjects, 10, async project => {
          const response = await projectsApi.listPhases(project.id);
          return Array.isArray(response.data) ? response.data : [];
        });
        const allPhases = phasePages.flat();
        setPhases(allPhases);
      } catch (err) {
        toast.error(err instanceof AxiosError
          ? (err.response?.data?.message ?? 'Failed to load company project and phase context')
          : 'Failed to load company project and phase context');
      } finally {
        setLoadingPhases(false);
      }
    })();
  }, []);

  const loadTasks = useCallback(async () => {
    setLoading(true);
    try {
      if (selectedPhaseId) {
        const response = await workApi.listTasks(selectedPhaseId);
        setTasks(Array.isArray(response.data) ? response.data : []);
      } else {
        const responses = await mapInBatches(companies, 10, company => workApi.listCompanyTasks(company.id));
        setTasks(responses.flatMap(response => Array.isArray(response.data) ? response.data : []));
      }
    } catch (err) {
      toast.error(err instanceof AxiosError && !err.response ? 'Cannot reach the work service.' : 'Failed to load tasks');
    } finally {
      setLoading(false);
    }
  }, [companies, selectedPhaseId]);

  useEffect(() => { loadTasks(); }, [loadTasks]);

  useEffect(() => {
    if (companies.length === 0) return;
    const userId = getStoredUser()?.id;
    if (!userId) {
      toast.error('Unable to determine the current account for task permissions');
      return;
    }
    setCurrentUserId(userId);
    let cancelled = false;
    mapInBatches(companies, 10, company => organizationApi.getMyMembership(company.id))
      .then(responses => {
        if (!cancelled) {
          setCompanyMemberships(Object.fromEntries(
            companies.map((company, index) => [company.id, responses[index].data])
          ));
        }
      })
      .catch(err => {
        if (!cancelled) {
          toast.error(err instanceof AxiosError
            ? (err.response?.data?.message ?? 'Failed to load task management permissions')
            : 'Failed to load task management permissions');
        }
      });
    return () => { cancelled = true; };
  }, [companies]);

  const canManageTask = (task: Task) => {
    const membership = companyMemberships[task.companyId];
    if (membership?.role === 'OWNER' || membership?.role === 'ADMIN') return true;
    if (!membership || !currentUserId) return false;
    const departmentRole = membership.departmentMemberships
      ?.find(assignment => assignment.departmentId === task.departmentId)?.role
      ?? (membership.departmentId === task.departmentId ? membership.role : undefined);
    return departmentRole === 'PROJECT_MANAGER'
      && projectsById[task.projectId]?.projectManagerId === currentUserId;
  };

  const canDeleteTask = (task: Task) => {
    const role = companyMemberships[task.companyId]?.role;
    return role === 'OWNER' || role === 'ADMIN';
  };

  const canCreateTaskInSelectedPhase = () => {
    const phase = phases.find(candidate => candidate.id === selectedPhaseId);
    const project = phase ? projectsById[phase.projectId] : undefined;
    if (!project) return false;
    if (companyMemberships[project.companyId]?.role === 'OWNER'
      || companyMemberships[project.companyId]?.role === 'ADMIN') return true;
    const membership = companyMemberships[project.companyId];
    if (!membership || !currentUserId) return false;
    const departmentRole = membership.departmentMemberships
      ?.find(assignment => assignment.departmentId === project.departmentId)?.role
      ?? (membership.departmentId === project.departmentId ? membership.role : undefined);
    return departmentRole === 'PROJECT_MANAGER' && project.projectManagerId === currentUserId;
  };

  const handleTaskAssigned = (taskId: string, member: CompanyMember) => {
    setTasks(current => current.map(task => task.id === taskId && task.status === 'DRAFT'
      ? { ...task, status: 'ASSIGNED' }
      : task));
    setAssignedMemberEmails(current => ({
      ...current,
      [taskId]: [...(current[taskId] ?? []), member.email],
    }));
  };

  const handleStatusChange = async (taskId: string, status: Task['status']) => {
    let reason: string | undefined;
    if (status === 'CHANGES_REQUESTED') {
      reason = window.prompt('Explain what needs to be changed:')?.trim();
      if (!reason) {
        if (reason !== undefined) toast.error('A reason is required to request changes');
        return;
      }
    }
    try {
      const res = await workApi.updateTaskStatus(taskId, status, reason);
      setTasks(prev => prev.map(t => t.id === taskId ? res.data : t));
      toast.success('Task status updated');
    } catch (err) {
      toast.error(err instanceof AxiosError ? (err.response?.data?.message ?? 'Failed to update status') : 'Failed to update status');
    }
    setActiveMenu(null);
  };

  const handleArchive = async (taskId: string, title: string) => {
    if (!window.confirm(`Archive "${title}"? It will be hidden from active task lists while its history and files are preserved.`)) return;
    try {
      await workApi.archiveTask(taskId);
      setTasks(prev => prev.filter(t => t.id !== taskId));
      toast.success(`"${title}" archived`);
    } catch (err) {
      toast.error(err instanceof AxiosError ? (err.response?.data?.message ?? 'Failed to archive task') : 'Failed to archive task');
    }
    setActiveMenu(null);
  };

  const filtered = tasks.filter(t => {
    const q = search.toLowerCase();
    const matchSearch = !q || t.title.toLowerCase().includes(q) || (t.description ?? '').toLowerCase().includes(q);
    const matchStatus = statusFilter === 'ALL' || t.status === statusFilter;
    const matchCompany = companyFilter === 'ALL' || t.companyId === companyFilter;
    return matchSearch && matchStatus && matchCompany;
  });

  const statusCounts = tasks.reduce((acc, t) => {
    acc[t.status] = (acc[t.status] ?? 0) + 1;
    return acc;
  }, {} as Record<string, number>);

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-foreground">Tasks</h1>
          <p className="text-muted-foreground text-sm mt-0.5">Manage and track work across project phases</p>
        </div>
        {selectedPhaseId && canCreateTaskInSelectedPhase() && (
          <button onClick={() => setCreateOpen(true)} className="btn-primary flex items-center gap-2 px-4 py-2 rounded-lg text-sm font-semibold">
            <Plus size={15} />New Task
          </button>
        )}
      </div>

      {/* Scope selectors */}
      {loadingPhases ? (
        <div className="flex items-center gap-2 text-sm text-muted-foreground"><Loader2 size={14} className="animate-spin" />Loading phases…</div>
      ) : (
        <div className="flex flex-wrap items-center gap-3">
          <label className="text-sm font-medium text-foreground" htmlFor="task-company-filter">Company:</label>
          <select id="task-company-filter" value={companyFilter} onChange={e => setCompanyFilter(e.target.value)} className="input-base text-sm">
            <option value="ALL">All companies</option>
            {companies.map(company => <option key={company.id} value={company.id}>{company.name}</option>)}
          </select>
          <label className="text-sm font-medium text-foreground" htmlFor="task-phase-filter">Phase:</label>
          <select id="task-phase-filter" value={selectedPhaseId} onChange={e => setSelectedPhaseId(e.target.value)} className="input-base text-sm">
            <option value="">All accessible tasks</option>
            {phases.map(phase => (
              <option key={phase.id} value={phase.id}>
                {projectNames[phase.projectId] ? `${projectNames[phase.projectId]} / ` : ''}{phase.name}
              </option>
            ))}
          </select>
        </div>
      )}
      {!loadingPhases && phases.length === 0 && (
        <div className="card-elevated p-4 text-center text-sm text-muted-foreground">
          No project phases found. You can still browse tasks assigned to you.
        </div>
      )}

      {/* Status summary */}
      {tasks.length > 0 && (
        <div className="grid grid-cols-2 sm:grid-cols-5 gap-3">
          {[
            { label: 'Assigned', key: 'ASSIGNED', color: 'text-violet-600' },
            { label: 'In Progress', key: 'IN_PROGRESS', color: 'text-blue-600' },
            { label: 'Submitted', key: 'SUBMITTED', color: 'text-amber-600' },
            { label: 'Completed', key: 'COMPLETED', color: 'text-green-600' },
            { label: 'Cancelled', key: 'CANCELLED', color: 'text-red-600' },
          ].map(s => (
            <div key={s.key} className="card-elevated p-3">
              <div className={`text-2xl font-bold font-tabular ${s.color}`}>{statusCounts[s.key] ?? 0}</div>
              <div className="text-xs text-muted-foreground mt-0.5">{s.label}</div>
            </div>
          ))}
        </div>
      )}

      {/* Filters */}
      <div className="flex flex-col sm:flex-row gap-3">
        <div className="relative flex-1">
          <Search size={14} className="absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground" />
          <input type="text" placeholder="Search tasks…" value={search} onChange={e => setSearch(e.target.value)} className="input-base input-search text-sm" />
        </div>
        <div className="flex gap-1 p-1 bg-muted rounded-lg">
          {['ALL', 'DRAFT', 'ASSIGNED', 'NOT_YET', 'IN_PROGRESS', 'SUBMITTED', 'ACCEPTED', 'CHANGES_REQUESTED', 'COMPLETED', 'CANCELLED'].map(s => (
            <button key={s} onClick={() => setStatusFilter(s)} className={`px-3 py-1.5 rounded-md text-xs font-medium transition-all ${statusFilter === s ? 'bg-card text-foreground shadow-card' : 'text-muted-foreground hover:text-foreground'}`}>
              {s === 'ALL' ? 'All' : s.replace(/_/g, ' ')}
            </button>
          ))}
        </div>
      </div>

      {/* Task list */}
      {loading ? (
        <div className="flex items-center justify-center py-16"><Loader2 size={24} className="animate-spin text-primary" /></div>
      ) : filtered.length === 0 ? (
        <div className="card-elevated p-12 text-center">
          <CheckSquare size={36} className="text-muted-foreground/30 mx-auto mb-3" />
          <p className="text-sm text-muted-foreground">{search || statusFilter !== 'ALL' || companyFilter !== 'ALL' ? 'No tasks match your filters' : selectedPhaseId ? 'No tasks yet in this phase' : 'No tasks in the selected companies'}</p>
          {!search && statusFilter === 'ALL' && selectedPhaseId && canCreateTaskInSelectedPhase() && (
            <button onClick={() => setCreateOpen(true)} className="btn-primary mt-4 px-4 py-2 rounded-lg text-sm font-semibold inline-flex items-center gap-2">
              <Plus size={14} />Create First Task
            </button>
          )}
        </div>
      ) : (
        <div className="card-elevated overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-border bg-muted/40">
                  <th className="px-4 py-3 text-left text-xs font-semibold text-muted-foreground uppercase tracking-wider">Task</th>
                  <th className="px-4 py-3 text-left text-xs font-semibold text-muted-foreground uppercase tracking-wider">Status</th>
                  <th className="px-4 py-3 text-left text-xs font-semibold text-muted-foreground uppercase tracking-wider">Priority</th>
                  <th className="px-4 py-3 text-left text-xs font-semibold text-muted-foreground uppercase tracking-wider">Due</th>
                  <th className="px-4 py-3 w-12" />
                </tr>
              </thead>
              <tbody>
                {filtered.map((task, i) => (
                  <tr key={task.id} className={`border-b border-border last:border-0 hover:bg-muted/40 transition-colors group ${i % 2 === 1 ? 'bg-muted/10' : ''}`}>
                    <td className="px-4 py-3">
                      <div className="flex items-center gap-2.5">
                        <CheckSquare size={14} className="text-muted-foreground flex-shrink-0" />
                        <div>
                          <Link href={`/tasks/${task.id}`} className="font-medium text-foreground hover:text-primary transition-colors">{task.title}</Link>
                          <p className="text-xs text-muted-foreground truncate max-w-xs">
                            {[companies.find(company => company.id === task.companyId)?.name, projectNames[task.projectId], phases.find(phase => phase.id === task.phaseId)?.name, task.description]
                              .filter(Boolean)
                              .join(' · ')}
                          </p>
                            {assignedMemberEmails[task.id]?.length ? (
                              <p className="text-xs text-muted-foreground mt-0.5">
                                Assigned: {assignedMemberEmails[task.id].join(', ')}
                              </p>
                            ) : null}
                        </div>
                      </div>
                    </td>
                    <td className="px-4 py-3">
                      <span className={`badge-base text-xs ${STATUS_COLORS[task.status] ?? STATUS_COLORS.DRAFT}`}>
                        {task.status.replace(/_/g, ' ')}
                      </span>
                    </td>
                    <td className="px-4 py-3">
                      <div className={`flex items-center gap-1 text-xs font-medium ${PRIORITY_COLORS[task.priority ?? 'MEDIUM'] ?? ''}`}>
                        <Flag size={11} />
                        {task.priority ?? 'MEDIUM'}
                      </div>
                    </td>
                    <td className="px-4 py-3 text-xs text-muted-foreground">
                      {task.dueDate ? (
                        <div className="flex items-center gap-1"><Calendar size={11} />{formatDateOnly(task.dueDate)}</div>
                      ) : '—'}
                    </td>
                    <td className="px-4 py-3">
                      <div className="relative">
                        <FloatingMenu
                          open={activeMenu === task.id}
                          onOpenChange={(open) => setActiveMenu(open ? task.id : null)}
                          widthClassName="w-44"
                          trigger={(triggerProps) => (
                            <button
                              {...triggerProps}
                              type="button"
                              aria-label={`Actions for ${task.title}`}
                              className="w-7 h-7 rounded-md flex items-center justify-center text-muted-foreground hover:text-foreground hover:bg-muted opacity-100 sm:opacity-0 sm:group-hover:opacity-100 focus-visible:opacity-100 transition-all"
                            >
                              <MoreVertical size={15} />
                            </button>
                          )}
                        >
                          {canManageTask(task) && (
                            <>
                              <button
                                type="button"
                                onClick={() => setEditingTask(task)}
                                className="flex items-center gap-2 w-full px-3 py-2 text-xs text-foreground hover:bg-muted transition-colors"
                              >
                                <Pencil size={12} className="text-muted-foreground" />
                                Edit Task
                              </button>
                              <button
                                type="button"
                                onClick={() => setAssignTask(task)}
                                className="flex items-center gap-2 w-full px-3 py-2 text-xs text-foreground hover:bg-muted transition-colors"
                              >
                                <UserPlus size={12} className="text-muted-foreground" />
                                Assign Task
                              </button>
                              <div className="border-t border-border my-1" />
                            </>
                          )}
                          <p className="px-3 py-1.5 text-xs font-semibold text-muted-foreground uppercase tracking-wider">Change Status</p>
                          {getAllowedTaskTransitions(task.status).map(s => (
                            <button key={s} onClick={() => handleStatusChange(task.id, s)} className={`flex items-center gap-2 w-full px-3 py-2 text-xs hover:bg-muted transition-colors ${task.status === s ? 'text-primary font-medium' : 'text-foreground'}`}>
                              {s.replace(/_/g, ' ')}
                            </button>
                          ))}
                          <div className="border-t border-border my-1" />
                          {canDeleteTask(task) && (
                            <button onClick={() => handleArchive(task.id, task.title)} className="flex items-center gap-2 w-full px-3 py-2 text-xs text-red-600 hover:bg-red-50 transition-colors">
                              <Archive size={12} />Archive
                            </button>
                          )}
                        </FloatingMenu>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {createOpen && selectedPhaseId && (
        <TaskFormModal
          phaseId={selectedPhaseId}
          onClose={() => setCreateOpen(false)}
          onSaved={savedTask => {
            setTasks(current => current.some(item => item.id === savedTask.id)
              ? current.map(item => item.id === savedTask.id ? savedTask : item)
              : [savedTask, ...current]);
          }}
        />
      )}
      {editingTask && (
        <TaskFormModal
          phaseId={editingTask.phaseId}
          task={editingTask}
          onClose={() => setEditingTask(null)}
          onSaved={savedTask => {
            setTasks(current => current.map(item => item.id === savedTask.id ? savedTask : item));
            setEditingTask(savedTask);
          }}
        />
      )}
      {assignTask && (
        <AssignTaskModal
          task={assignTask}
          onClose={() => setAssignTask(null)}
          onAssigned={handleTaskAssigned}
        />
      )}
    </div>
  );
}
