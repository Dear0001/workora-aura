'use client';
import React, { useCallback, useEffect, useState } from 'react';
import { FolderKanban, Plus, Search, MoreHorizontal, Archive, Users, Calendar, Loader2, ChevronRight, Eye, ArrowLeft } from 'lucide-react';
import FloatingMenu from '@/components/ui/FloatingMenu';
import { toast } from 'sonner';
import { AxiosError } from 'axios';
import { useSearchParams, useRouter } from 'next/navigation';
import { projectsApi, type Project } from '@/lib/api/projects';
import { organizationApi } from '@/lib/api/organization';
import type { Department } from '@/lib/api/types';
import { formatDateOnly } from '@/lib/date';
import Link from 'next/link';

const STATUS_BADGE: Record<string, string> = {
  ACTIVE: 'bg-green-50 text-green-700 border border-green-200',
  ARCHIVED: 'bg-muted text-muted-foreground border border-border',
  COMPLETED: 'bg-blue-50 text-blue-700 border border-blue-200',
};

const PRIORITY_BADGE: Record<string, string> = {
  LOW: 'bg-muted text-muted-foreground',
  MEDIUM: 'bg-amber-50 text-amber-700',
  HIGH: 'bg-orange-50 text-orange-700',
  CRITICAL: 'bg-red-50 text-red-700',
};

interface CreateProjectModalProps {
  departmentId: string;
  departments: Department[];
  allowManagerSelection: boolean;
  onClose: () => void;
  onCreated: (project: Project) => void;
}

function CreateProjectModal({
  departmentId,
  departments,
  allowManagerSelection,
  onClose,
  onCreated,
}: CreateProjectModalProps) {
  const [selectedDepartmentId, setSelectedDepartmentId] = useState(departmentId);
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [dueDate, setDueDate] = useState('');
  const [projectManagerId, setProjectManagerId] = useState('');
  const [eligibleManagers, setEligibleManagers] = useState<{ userId: string; email: string }[]>([]);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    setProjectManagerId('');
    organizationApi.listDepartmentMembers(selectedDepartmentId)
      .then(response => {
        setEligibleManagers(response.data
            .filter(member => member.departmentMemberships?.some(
              assignment => assignment.departmentId === selectedDepartmentId && assignment.role === 'PROJECT_MANAGER'
            ) || (member.role === 'PROJECT_MANAGER' && member.departmentId === selectedDepartmentId))
            .map(({ userId, email }) => ({ userId, email })));
      })
      .catch(err => {
        toast.error(err instanceof AxiosError
          ? (err.response?.data?.message ?? 'Failed to load department project managers')
          : 'Failed to load department project managers');
      });
  }, [selectedDepartmentId]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim()) return;
    setLoading(true);
    try {
      const res = await projectsApi.createProject(selectedDepartmentId, {
        name: name.trim(),
        description: description || undefined,
        projectManagerId: projectManagerId || undefined,
        dueDate: dueDate || undefined,
      });
      toast.success(`Project "${res.data.name}" created`);
      onCreated(res.data);
    } catch (err) {
      toast.error(err instanceof AxiosError ? (err.response?.data?.message ?? (!err.response ? 'Cannot reach the project service.' : 'Failed to create project')) : 'Failed to create project');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 z-[1100] flex items-center justify-center p-4">
      <div className="fixed inset-0 bg-black/40 fade-in" onClick={onClose} />
      <div className="relative max-h-[calc(100dvh-2rem)] w-full max-w-lg overflow-y-auto rounded-2xl bg-card shadow-modal fade-scale-in">
        <div className="flex items-center justify-between p-6 border-b border-border">
          <div className="flex items-center gap-3">
            <div className="w-9 h-9 rounded-xl bg-primary/10 flex items-center justify-center"><FolderKanban size={18} className="text-primary" /></div>
            <div><h2 className="text-base font-bold text-foreground">New Project</h2><p className="text-xs text-muted-foreground">Create a project for your company</p></div>
          </div>
          <button onClick={onClose} className="w-8 h-8 rounded-lg flex items-center justify-center text-muted-foreground hover:text-foreground hover:bg-muted transition-all">✕</button>
        </div>
        <form onSubmit={handleSubmit} className="p-6 space-y-4">
          {departments.length > 1 && (
            <div>
              <label className="block text-sm font-medium text-foreground mb-1.5">Department *</label>
              <select
                value={selectedDepartmentId}
                onChange={event => setSelectedDepartmentId(event.target.value)}
                className="input-base"
                required
              >
                {departments.map(department => (
                  <option key={department.id} value={department.id}>{department.name}</option>
                ))}
              </select>
            </div>
          )}
          <div>
            <label className="block text-sm font-medium text-foreground mb-1.5">Project Name *</label>
            <input type="text" value={name} onChange={e => setName(e.target.value)} placeholder="Mobile app refresh" className="input-base" required />
          </div>
          <div>
            <label className="block text-sm font-medium text-foreground mb-1.5">Description</label>
            <textarea rows={3} value={description} onChange={e => setDescription(e.target.value)} placeholder="What is this project about?" className="input-base resize-none" />
          </div>
          <div>
            <label className="block text-sm font-medium text-foreground mb-1.5">Project Deadline</label>
            <input type="date" value={dueDate} onChange={e => setDueDate(e.target.value)} className="input-base" />
          </div>
          {allowManagerSelection && (
            <div>
              <label className="block text-sm font-medium text-foreground mb-1.5">Project Manager</label>
              <select
                value={projectManagerId}
                onChange={e => setProjectManagerId(e.target.value)}
                className="input-base"
              >
                <option value="">Use my account</option>
                {eligibleManagers.map(manager => (
                  <option key={manager.userId} value={manager.userId}>{manager.email}</option>
                ))}
              </select>
              <p className="text-xs text-muted-foreground mt-1">
                Only active Project Managers assigned to this department can be selected.
              </p>
            </div>
          )}
          <div className="flex items-center gap-3 pt-2">
            <button type="button" onClick={onClose} className="btn-secondary flex-1 py-2.5 rounded-lg font-medium text-sm">Cancel</button>
            <button type="submit" disabled={loading || !name.trim()} className="btn-primary flex-1 py-2.5 rounded-lg font-semibold text-sm flex items-center justify-center gap-2 disabled:opacity-60">
              {loading ? <><Loader2 size={14} className="animate-spin" /><span>Creating…</span></> : <><FolderKanban size={14} /><span>Create Project</span></>}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

export default function ProjectsContent() {
  const searchParams = useSearchParams();
  const router = useRouter();
  const departmentId = searchParams.get('departmentId');
  const departmentName = searchParams.get('departmentName');

  const [projects, setProjects] = useState<Project[]>([]);
  const [loading, setLoading] = useState(true);
  const [companyId, setCompanyId] = useState<string | null>(null);
  const [projectDepartments, setProjectDepartments] = useState<Department[]>([]);
  const [createDepartmentId, setCreateDepartmentId] = useState('');
  const [canCreateInScope, setCanCreateInScope] = useState(false);
  const [canManageCompanyProjects, setCanManageCompanyProjects] = useState(false);
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [createOpen, setCreateOpen] = useState(false);
  const [activeMenu, setActiveMenu] = useState<string | null>(null);

  const loadData = useCallback(async () => {
    setLoading(true);
    setCompanyId(null);
    setProjectDepartments([]);
    setCanCreateInScope(false);
    setCanManageCompanyProjects(false);
    try {
      const orgRes = await organizationApi.listCompanies({ page: 0, size: 1 });
      const cId = orgRes.data?.content?.[0]?.id;
      if (!cId) { setLoading(false); return; }
      setCompanyId(cId);
      const membershipRes = await organizationApi.getMyMembership(cId);
      const membership = membershipRes.data;
      const isOwnerOrAdmin = membership.role === 'OWNER' || membership.role === 'ADMIN';
      const departmentRes = await organizationApi.listDepartments(cId);
      const projectManagerDepartmentIds = new Set(
        membership.departmentMemberships
          ?.filter(assignment => assignment.role === 'PROJECT_MANAGER')
          .map(assignment => assignment.departmentId) ?? []
      );
      if (membership.role === 'PROJECT_MANAGER' && membership.departmentId) {
        projectManagerDepartmentIds.add(membership.departmentId);
      }
      const availableDepartments = isOwnerOrAdmin
        ? departmentRes.data
        : departmentRes.data.filter(department => projectManagerDepartmentIds.has(department.id));
      setProjectDepartments(availableDepartments);
      setCreateDepartmentId(departmentId && availableDepartments.some(department => department.id === departmentId)
        ? departmentId
        : availableDepartments[0]?.id ?? '');
      setCanManageCompanyProjects(isOwnerOrAdmin);
      setCanCreateInScope(availableDepartments.length > 0);
      const projRes = await projectsApi.listCompanyProjects(cId, { page: 0, size: 50 });
      const list = projRes.data?.content ?? [];
      setProjects(list);
    } catch (err) {
      toast.error(err instanceof AxiosError && !err.response ? 'Cannot reach the project service.' : 'Failed to load projects');
    } finally {
      setLoading(false);
    }
  }, [departmentId]);

  useEffect(() => { loadData(); }, [loadData]);

  const handleArchive = async (id: string, name: string) => {
    try {
      await projectsApi.archiveProject(id);
      setProjects(prev => prev.map(p => p.id === id ? { ...p, status: 'ARCHIVED' } : p));
      toast.success(`"${name}" archived`);
    } catch (err) {
      toast.error(err instanceof AxiosError ? (err.response?.data?.message ?? 'Failed to archive project') : 'Failed to archive project');
    }
    setActiveMenu(null);
  };

  const filtered = projects.filter(p => {
    const q = search.toLowerCase();
    const matchSearch = !q || p.name.toLowerCase().includes(q) || (p.description ?? '').toLowerCase().includes(q);
    const matchStatus = statusFilter === 'ALL' || p.status === statusFilter;
    // If filtering by department, filter by departmentId if the project has it
    const matchDept = !departmentId || (p as Project & { departmentId?: string }).departmentId === departmentId;
    return matchSearch && matchStatus && matchDept;
  });

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          {departmentId && departmentName && (
            <button
              onClick={() => router.push('/company-management')}
              className="flex items-center gap-1.5 text-xs text-muted-foreground hover:text-foreground mb-2 transition-colors"
            >
              <ArrowLeft size={13} />
              Back to Company Management
            </button>
          )}
          <h1 className="text-2xl font-bold text-foreground">
            {departmentName ? `${departmentName} — Projects` : 'Projects'}
          </h1>
          <p className="text-muted-foreground text-sm mt-0.5">
            {departmentName
              ? `Projects under the ${departmentName} department`
              : "Manage your company's projects and phases"}
          </p>
        </div>
        {companyId && canCreateInScope && (
          <button onClick={() => setCreateOpen(true)} className="btn-primary flex items-center gap-2 px-4 py-2 rounded-lg text-sm font-semibold">
            <Plus size={15} />New Project
          </button>
        )}
      </div>

      {/* Filters */}
      <div className="flex flex-col sm:flex-row gap-3">
        <div className="relative flex-1">
          <Search size={14} className="absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground" />
          <input type="text" placeholder="Search projects…" value={search} onChange={e => setSearch(e.target.value)} className="input-base input-search text-sm" />
        </div>
        <div className="flex gap-1 p-1 bg-muted rounded-lg">
          {['ALL', 'ACTIVE', 'ARCHIVED', 'COMPLETED'].map(s => (
            <button key={s} onClick={() => setStatusFilter(s)} className={`px-3 py-1.5 rounded-md text-xs font-medium transition-all ${statusFilter === s ? 'bg-card text-foreground shadow-card' : 'text-muted-foreground hover:text-foreground'}`}>
              {s === 'ALL' ? 'All' : s.charAt(0) + s.slice(1).toLowerCase()}
            </button>
          ))}
        </div>
      </div>

      {/* Content */}
      {loading ? (
        <div className="flex items-center justify-center py-20">
          <Loader2 size={24} className="animate-spin text-primary" />
        </div>
      ) : !companyId ? (
        <div className="card-elevated p-12 text-center">
          <FolderKanban size={40} className="text-muted-foreground/30 mx-auto mb-3" />
          <p className="text-sm font-medium text-muted-foreground">No company found. Create a company first.</p>
          <Link href="/company-management" className="text-xs text-primary hover:underline mt-2 inline-block">Go to Company Management →</Link>
        </div>
      ) : filtered.length === 0 ? (
        <div className="card-elevated p-12 text-center">
          <FolderKanban size={40} className="text-muted-foreground/30 mx-auto mb-3" />
          <p className="text-sm font-medium text-muted-foreground">
            {departmentId
              ? `No projects found for the ${departmentName ?? 'selected'} department`
              : search || statusFilter !== 'ALL' ? 'No projects match your filters' : 'No projects yet'}
          </p>
          {!search && statusFilter === 'ALL' && canCreateInScope && (
            <button onClick={() => setCreateOpen(true)} className="btn-primary mt-4 px-4 py-2 rounded-lg text-sm font-semibold inline-flex items-center gap-2">
              <Plus size={14} />Create First Project
            </button>
          )}
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4">
          {filtered.map((project) => (
            <div key={project.id} className="card-elevated p-5 card-hover group relative">
              <div className="flex items-start justify-between mb-3">
                <div className="flex items-center gap-3 flex-1 min-w-0">
                  <div className="w-10 h-10 rounded-xl bg-primary/10 flex items-center justify-center flex-shrink-0">
                    <FolderKanban size={18} className="text-primary" />
                  </div>
                  <div className="min-w-0">
                    <h3 className="font-semibold text-foreground text-sm truncate">{project.name}</h3>
                    <div className="flex items-center gap-2 mt-1">
                      <span className={`badge-base text-xs ${STATUS_BADGE[project.status ?? 'ACTIVE'] ?? STATUS_BADGE.ACTIVE}`}>
                        {project.status ?? 'ACTIVE'}
                      </span>
                      {project.priority && (
                        <span className={`badge-base text-xs ${PRIORITY_BADGE[project.priority] ?? ''}`}>
                          {project.priority}
                        </span>
                      )}
                    </div>
                  </div>
                </div>
                <div className="relative flex-shrink-0">
                  <FloatingMenu
                    open={activeMenu === project.id}
                    onOpenChange={(open) => setActiveMenu(open ? project.id : null)}
                    widthClassName="w-40"
                    trigger={(triggerProps) => (
                      <button
                        {...triggerProps}
                        type="button"
                        aria-label={`Actions for ${project.name}`}
                        className="w-7 h-7 rounded-md flex items-center justify-center text-muted-foreground hover:text-foreground hover:bg-muted opacity-100 sm:opacity-0 sm:group-hover:opacity-100 focus-visible:opacity-100 transition-all"
                      >
                        <MoreHorizontal size={15} />
                      </button>
                    )}
                  >
                    <Link href={`/projects/${project.id}`} className="flex items-center gap-2 w-full px-3 py-2 text-xs text-foreground hover:bg-muted transition-colors">
                      <Eye size={12} className="text-muted-foreground" />View Details
                    </Link>
                    {canManageCompanyProjects && project.status !== 'ARCHIVED' && (
                      <button onClick={() => handleArchive(project.id, project.name)} className="flex items-center gap-2 w-full px-3 py-2 text-xs text-amber-600 hover:bg-amber-50 transition-colors">
                        <Archive size={12} />Archive
                      </button>
                    )}
                  </FloatingMenu>
                </div>
              </div>

              <p className="text-xs text-muted-foreground leading-relaxed mb-4 line-clamp-2">
                {project.description || 'No description provided.'}
              </p>

              <div className="mb-4">
                <div className="mb-1.5 flex items-center justify-between text-xs">
                  <span className="text-muted-foreground">Progress</span>
                  <span className="font-medium text-foreground">{project.progress ?? 0}%</span>
                </div>
                <div className="h-1.5 overflow-hidden rounded-full bg-muted">
                  <div className="h-full rounded-full bg-primary transition-all" style={{ width: `${Math.min(100, Math.max(0, project.progress ?? 0))}%` }} />
                </div>
              </div>

              <div className="flex items-center justify-between pt-3 border-t border-border">
                <div className="flex items-center gap-1.5 text-xs text-muted-foreground">
                  <Users size={12} />
                  <span className="font-medium text-foreground">{project.memberCount ?? '—'}</span>
                  <span>members</span>
                </div>
                {project.dueDate && (
                  <div className="flex items-center gap-1.5 text-xs text-muted-foreground">
                    <Calendar size={12} />
                    <span>{formatDateOnly(project.dueDate)}</span>
                  </div>
                )}
                <Link href={`/projects/${project.id}`} className="flex items-center gap-1 text-xs text-primary font-medium hover:underline opacity-0 group-hover:opacity-100 transition-opacity">
                  View <ChevronRight size={12} />
                </Link>
              </div>
            </div>
          ))}
        </div>
      )}

      {createOpen && createDepartmentId && (
        <CreateProjectModal
          departmentId={createDepartmentId}
          departments={projectDepartments}
          allowManagerSelection={canManageCompanyProjects}
          onClose={() => setCreateOpen(false)}
          onCreated={(p) => { setProjects(prev => [p, ...prev]); setCreateOpen(false); }}
        />
      )}
    </div>
  );
}
