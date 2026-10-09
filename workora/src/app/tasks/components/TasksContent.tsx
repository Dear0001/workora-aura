'use client';
import React, { useCallback, useEffect, useState } from 'react';
import { CheckSquare, Plus, Search, MoreHorizontal, Trash2, Loader2, Calendar, Flag } from 'lucide-react';
import { toast } from 'sonner';
import { AxiosError } from 'axios';
import { getAllowedTaskTransitions, workApi, type Task } from '@/lib/api/work';
import { projectsApi, type Phase } from '@/lib/api/projects';
import { organizationApi } from '@/lib/api/organization';
import type { Company } from '@/lib/api/types';
import { formatDateOnly } from '@/lib/date';
import Link from 'next/link';

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

interface CreateTaskModalProps {
  phaseId: string;
  onClose: () => void;
  onCreated: (task: Task) => void;
}

function CreateTaskModal({ phaseId, onClose, onCreated }: CreateTaskModalProps) {
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [priority, setPriority] = useState<'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL'>('MEDIUM');
  const [dueDate, setDueDate] = useState('');
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!title.trim()) return;
    setLoading(true);
    try {
      const res = await workApi.createTask(phaseId, { title: title.trim(), description: description || undefined, priority, dueDate: dueDate || undefined });
      toast.success(`Task "${res.data.title}" created`);
      onCreated(res.data);
    } catch (err) {
      toast.error(err instanceof AxiosError ? (err.response?.data?.message ?? 'Failed to create task') : 'Failed to create task');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      <div className="fixed inset-0 bg-black/40 fade-in" onClick={onClose} />
      <div className="relative bg-card rounded-2xl shadow-modal w-full max-w-md fade-scale-in">
        <div className="flex items-center justify-between p-6 border-b border-border">
          <div><h2 className="text-base font-bold text-foreground">New Task</h2><p className="text-xs text-muted-foreground">Add a task to this phase</p></div>
          <button onClick={onClose} className="w-8 h-8 rounded-lg flex items-center justify-center text-muted-foreground hover:text-foreground hover:bg-muted transition-all">✕</button>
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
          <div className="flex items-center gap-3 pt-2">
            <button type="button" onClick={onClose} className="btn-secondary flex-1 py-2.5 rounded-lg font-medium text-sm">Cancel</button>
            <button type="submit" disabled={loading || !title.trim()} className="btn-primary flex-1 py-2.5 rounded-lg font-semibold text-sm flex items-center justify-center gap-2 disabled:opacity-60">
              {loading ? <><Loader2 size={14} className="animate-spin" /><span>Creating…</span></> : 'Create Task'}
            </button>
          </div>
        </form>
      </div>
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
  const [companyFilter, setCompanyFilter] = useState('ALL');
  const [selectedPhaseId, setSelectedPhaseId] = useState<string>(initialPhaseId ?? '');
  const [loading, setLoading] = useState(false);
  const [loadingPhases, setLoadingPhases] = useState(true);
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [createOpen, setCreateOpen] = useState(false);
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

  const handleDelete = async (taskId: string, title: string) => {
    try {
      await workApi.deleteTask(taskId);
      setTasks(prev => prev.filter(t => t.id !== taskId));
      toast.success(`"${title}" deleted`);
    } catch (err) {
      toast.error(err instanceof AxiosError ? (err.response?.data?.message ?? 'Failed to delete task') : 'Failed to delete task');
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
        {selectedPhaseId && (
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
          <input type="text" placeholder="Search tasks…" value={search} onChange={e => setSearch(e.target.value)} className="input-base pl-8 text-sm" />
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
          {!search && statusFilter === 'ALL' && selectedPhaseId && (
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
                        <button onClick={() => setActiveMenu(activeMenu === task.id ? null : task.id)} className="w-7 h-7 rounded-md flex items-center justify-center text-muted-foreground hover:text-foreground hover:bg-muted opacity-0 group-hover:opacity-100 transition-all">
                          <MoreHorizontal size={15} />
                        </button>
                        {activeMenu === task.id && (
                          <>
                            <div className="fixed inset-0 z-10" onClick={() => setActiveMenu(null)} />
                            <div className="absolute right-0 top-full mt-1 w-44 bg-card border border-border rounded-xl shadow-elevated z-20 py-1 fade-scale-in">
                              <p className="px-3 py-1.5 text-xs font-semibold text-muted-foreground uppercase tracking-wider">Change Status</p>
                              {getAllowedTaskTransitions(task.status).map(s => (
                                <button key={s} onClick={() => handleStatusChange(task.id, s)} className={`flex items-center gap-2 w-full px-3 py-2 text-xs hover:bg-muted transition-colors ${task.status === s ? 'text-primary font-medium' : 'text-foreground'}`}>
                                  {s.replace(/_/g, ' ')}
                                </button>
                              ))}
                              <div className="border-t border-border my-1" />
                              <button onClick={() => handleDelete(task.id, task.title)} className="flex items-center gap-2 w-full px-3 py-2 text-xs text-red-600 hover:bg-red-50 transition-colors">
                                <Trash2 size={12} />Delete
                              </button>
                            </div>
                          </>
                        )}
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
        <CreateTaskModal
          phaseId={selectedPhaseId}
          onClose={() => setCreateOpen(false)}
          onCreated={(t) => { setTasks(prev => [t, ...prev]); setCreateOpen(false); }}
        />
      )}
    </div>
  );
}
