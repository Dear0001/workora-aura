'use client';
import React, { useCallback, useEffect, useState } from 'react';
import { CheckSquare, Plus, Search, MoreHorizontal, Trash2, Loader2, Calendar, Flag, ChevronDown } from 'lucide-react';
import { toast } from 'sonner';
import { AxiosError } from 'axios';
import { workApi, type Task } from '@/lib/api/work';
import { projectsApi, type Phase } from '@/lib/api/projects';
import { organizationApi } from '@/lib/api/organization';
import Link from 'next/link';

const STATUS_COLORS: Record<string, string> = {
  OPEN: 'bg-muted text-muted-foreground border border-border',
  IN_PROGRESS: 'bg-blue-50 text-blue-700 border border-blue-200',
  IN_REVIEW: 'bg-amber-50 text-amber-700 border border-amber-200',
  DONE: 'bg-green-50 text-green-700 border border-green-200',
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
  const [priority, setPriority] = useState('MEDIUM');
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
              <select value={priority} onChange={e => setPriority(e.target.value)} className="input-base">
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

export default function TasksContent({ initialPhaseId }: Props) {
  const [tasks, setTasks] = useState<Task[]>([]);
  const [phases, setPhases] = useState<Phase[]>([]);
  const [selectedPhaseId, setSelectedPhaseId] = useState<string>(initialPhaseId ?? '');
  const [loading, setLoading] = useState(false);
  const [loadingPhases, setLoadingPhases] = useState(true);
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [createOpen, setCreateOpen] = useState(false);
  const [activeMenu, setActiveMenu] = useState<string | null>(null);

  // Load phases from all company projects
  useEffect(() => {
    (async () => {
      setLoadingPhases(true);
      try {
        const orgRes = await organizationApi.listCompanies({ page: 0, size: 1 });
        const cId = orgRes.data?.content?.[0]?.id;
        if (!cId) { setLoadingPhases(false); return; }
        const projRes = await projectsApi.listCompanyProjects(cId, { page: 0, size: 50 });
        const projects = projRes.data?.content ?? [];
        const allPhases: Phase[] = [];
        await Promise.all(projects.map(async (proj) => {
          try {
            const pRes = await projectsApi.listPhases(proj.id);
            const list = Array.isArray(pRes.data) ? pRes.data : (pRes.data as { content?: Phase[] })?.content ?? [];
            allPhases.push(...list);
          } catch { /* ignore */ }
        }));
        setPhases(allPhases);
        if (!selectedPhaseId && allPhases.length > 0) setSelectedPhaseId(allPhases[0].id);
      } catch { /* ignore */ } finally {
        setLoadingPhases(false);
      }
    })();
  }, []);

  const loadTasks = useCallback(async () => {
    if (!selectedPhaseId) return;
    setLoading(true);
    try {
      const res = await workApi.listTasks(selectedPhaseId);
      const list = Array.isArray(res.data) ? res.data : [];
      setTasks(list);
    } catch (err) {
      toast.error(err instanceof AxiosError && !err.response ? 'Cannot reach the work service.' : 'Failed to load tasks');
    } finally {
      setLoading(false);
    }
  }, [selectedPhaseId]);

  useEffect(() => { loadTasks(); }, [loadTasks]);

  const handleStatusChange = async (taskId: string, status: string) => {
    try {
      const res = await workApi.updateTaskStatus(taskId, status);
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
    return matchSearch && matchStatus;
  });

  const statusCounts = tasks.reduce((acc, t) => {
    acc[t.status ?? 'OPEN'] = (acc[t.status ?? 'OPEN'] ?? 0) + 1;
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

      {/* Phase selector */}
      {loadingPhases ? (
        <div className="flex items-center gap-2 text-sm text-muted-foreground"><Loader2 size={14} className="animate-spin" />Loading phases…</div>
      ) : phases.length > 0 ? (
        <div className="flex items-center gap-3">
          <label className="text-sm font-medium text-foreground">Phase:</label>
          <div className="relative">
            <select
              value={selectedPhaseId}
              onChange={e => setSelectedPhaseId(e.target.value)}
              className="input-base pr-8 text-sm w-64"
            >
              {phases.map(p => <option key={p.id} value={p.id}>{p.name}</option>)}
            </select>
            <ChevronDown size={14} className="absolute right-2.5 top-1/2 -translate-y-1/2 text-muted-foreground pointer-events-none" />
          </div>
        </div>
      ) : (
        <div className="card-elevated p-6 text-center text-sm text-muted-foreground">No phases found. Create a project and add phases first.</div>
      )}

      {/* Status summary */}
      {tasks.length > 0 && (
        <div className="grid grid-cols-2 sm:grid-cols-5 gap-3">
          {[
            { label: 'Open', key: 'OPEN', color: 'text-muted-foreground' },
            { label: 'In Progress', key: 'IN_PROGRESS', color: 'text-blue-600' },
            { label: 'In Review', key: 'IN_REVIEW', color: 'text-amber-600' },
            { label: 'Done', key: 'DONE', color: 'text-green-600' },
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
          {['ALL', 'OPEN', 'IN_PROGRESS', 'IN_REVIEW', 'DONE'].map(s => (
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
          <p className="text-sm text-muted-foreground">{search || statusFilter !== 'ALL' ? 'No tasks match your filters' : 'No tasks yet in this phase'}</p>
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
                          {task.description && <p className="text-xs text-muted-foreground truncate max-w-xs">{task.description}</p>}
                        </div>
                      </div>
                    </td>
                    <td className="px-4 py-3">
                      <span className={`badge-base text-xs ${STATUS_COLORS[task.status ?? 'OPEN'] ?? STATUS_COLORS.OPEN}`}>
                        {(task.status ?? 'OPEN').replace(/_/g, ' ')}
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
                        <div className="flex items-center gap-1"><Calendar size={11} />{new Date(task.dueDate).toLocaleDateString()}</div>
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
                              {['OPEN', 'IN_PROGRESS', 'IN_REVIEW', 'DONE', 'CANCELLED'].map(s => (
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
