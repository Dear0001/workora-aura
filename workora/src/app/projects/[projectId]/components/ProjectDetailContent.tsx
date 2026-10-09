'use client';
import React, { useCallback, useEffect, useState } from 'react';
import { ArrowLeft, FolderKanban, Plus, Users, Calendar, Loader2, Edit2, ChevronDown, ChevronUp, Trash2, Eye, CheckSquare } from 'lucide-react';
import Link from 'next/link';
import { toast } from 'sonner';
import { AxiosError } from 'axios';
import { projectsApi, type Project, type Phase } from '@/lib/api/projects';

const PHASE_STATUS_BADGE: Record<string, string> = {
  DRAFT: 'bg-muted text-muted-foreground border border-border',
  PUBLISHED: 'bg-green-50 text-green-700 border border-green-200',
  ARCHIVED: 'bg-amber-50 text-amber-700 border border-amber-200',
  CLOSED: 'bg-blue-50 text-blue-700 border border-blue-200',
};

interface CreatePhaseModalProps {
  projectId: string;
  onClose: () => void;
  onCreated: (phase: Phase) => void;
}

function CreatePhaseModal({ projectId, onClose, onCreated }: CreatePhaseModalProps) {
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim()) return;
    setLoading(true);
    try {
      const res = await projectsApi.createPhase(projectId, {
        name: name.trim(),
        description: description || undefined,
        startDate: startDate || undefined,
        endDate: endDate || undefined,
      });
      toast.success(`Phase "${res.data.name}" created`);
      onCreated(res.data);
    } catch (err) {
      toast.error(err instanceof AxiosError ? (err.response?.data?.message ?? 'Failed to create phase') : 'Failed to create phase');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      <div className="fixed inset-0 bg-black/40 fade-in" onClick={onClose} />
      <div className="relative bg-card rounded-2xl shadow-modal w-full max-w-md fade-scale-in">
        <div className="flex items-center justify-between p-6 border-b border-border">
          <div>
            <h2 className="text-base font-bold text-foreground">New Phase</h2>
            <p className="text-xs text-muted-foreground">Add a phase to this project</p>
          </div>
          <button onClick={onClose} className="w-8 h-8 rounded-lg flex items-center justify-center text-muted-foreground hover:text-foreground hover:bg-muted transition-all">✕</button>
        </div>
        <form onSubmit={handleSubmit} className="p-6 space-y-4">
          <div>
            <label className="block text-sm font-medium text-foreground mb-1.5">Phase Name *</label>
            <input type="text" value={name} onChange={e => setName(e.target.value)} placeholder="Sprint 1 / Alpha / Beta…" className="input-base" required />
          </div>
          <div>
            <label className="block text-sm font-medium text-foreground mb-1.5">Description</label>
            <textarea rows={2} value={description} onChange={e => setDescription(e.target.value)} className="input-base resize-none" />
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-sm font-medium text-foreground mb-1.5">Start Date</label>
              <input type="date" value={startDate} onChange={e => setStartDate(e.target.value)} className="input-base" />
            </div>
            <div>
              <label className="block text-sm font-medium text-foreground mb-1.5">End Date</label>
              <input type="date" value={endDate} onChange={e => setEndDate(e.target.value)} className="input-base" />
            </div>
          </div>
          <div className="flex items-center gap-3 pt-2">
            <button type="button" onClick={onClose} className="btn-secondary flex-1 py-2.5 rounded-lg font-medium text-sm">Cancel</button>
            <button type="submit" disabled={loading || !name.trim()} className="btn-primary flex-1 py-2.5 rounded-lg font-semibold text-sm flex items-center justify-center gap-2 disabled:opacity-60">
              {loading ? <><Loader2 size={14} className="animate-spin" /><span>Creating…</span></> : 'Create Phase'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

interface Props {
  projectId: string;
}

export default function ProjectDetailContent({ projectId }: Props) {
  const [project, setProject] = useState<Project | null>(null);
  const [phases, setPhases] = useState<Phase[]>([]);
  const [loading, setLoading] = useState(true);
  const [createPhaseOpen, setCreatePhaseOpen] = useState(false);
  const [expandedPhase, setExpandedPhase] = useState<string | null>(null);
  const [editingProject, setEditingProject] = useState(false);
  const [editName, setEditName] = useState('');
  const [editDesc, setEditDesc] = useState('');
  const [saving, setSaving] = useState(false);

  const loadData = useCallback(async () => {
    setLoading(true);
    try {
      const [projRes, phasesRes] = await Promise.all([
        projectsApi.getProject(projectId),
        projectsApi.listPhases(projectId),
      ]);
      setProject(projRes.data);
      setPhases(Array.isArray(phasesRes.data) ? phasesRes.data : (phasesRes.data as { content?: Phase[] })?.content ?? []);
    } catch (err) {
      toast.error(err instanceof AxiosError && !err.response ? 'Cannot reach the project service.' : 'Failed to load project');
    } finally {
      setLoading(false);
    }
  }, [projectId]);

  useEffect(() => { loadData(); }, [loadData]);

  const handleSaveProject = async () => {
    if (!project) return;
    setSaving(true);
    try {
      const res = await projectsApi.updateProject(project.id, { name: editName, description: editDesc });
      setProject(res.data);
      setEditingProject(false);
      toast.success('Project updated');
    } catch (err) {
      toast.error(err instanceof AxiosError ? (err.response?.data?.message ?? 'Failed to update project') : 'Failed to update project');
    } finally {
      setSaving(false);
    }
  };

  const handlePhaseAction = async (phaseId: string, action: 'publish' | 'hide' | 'close' | 'delete') => {
    try {
      if (action === 'delete') {
        await projectsApi.deletePhase(phaseId);
        setPhases(prev => prev.filter(p => p.id !== phaseId));
        toast.success('Phase deleted');
      } else {
        const res = action === 'publish'
          ? await projectsApi.publishPhase(phaseId)
          : action === 'hide'
          ? await projectsApi.hidePhase(phaseId)
          : await projectsApi.closePhase(phaseId);
        setPhases(prev => prev.map(p => p.id === phaseId ? res.data : p));
        toast.success(`Phase ${action}ed`);
      }
    } catch (err) {
      toast.error(err instanceof AxiosError ? (err.response?.data?.message ?? `Failed to ${action} phase`) : `Failed to ${action} phase`);
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center py-20">
        <Loader2 size={24} className="animate-spin text-primary" />
      </div>
    );
  }

  if (!project) {
    return (
      <div className="card-elevated p-12 text-center">
        <p className="text-sm text-muted-foreground">Project not found.</p>
        <Link href="/projects" className="text-xs text-primary hover:underline mt-2 inline-block">← Back to Projects</Link>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Breadcrumb */}
      <div className="flex items-center gap-2 text-sm text-muted-foreground">
        <Link href="/projects" className="hover:text-foreground flex items-center gap-1"><ArrowLeft size={14} />Projects</Link>
        <span>/</span>
        <span className="text-foreground font-medium">{project.name}</span>
      </div>

      {/* Project header */}
      <div className="card-elevated p-6">
        {editingProject ? (
          <div className="space-y-3">
            <input type="text" value={editName} onChange={e => setEditName(e.target.value)} className="input-base text-lg font-bold" />
            <textarea rows={2} value={editDesc} onChange={e => setEditDesc(e.target.value)} className="input-base resize-none text-sm" />
            <div className="flex gap-2">
              <button onClick={() => setEditingProject(false)} className="btn-secondary px-4 py-2 rounded-lg text-sm">Cancel</button>
              <button onClick={handleSaveProject} disabled={saving} className="btn-primary px-4 py-2 rounded-lg text-sm flex items-center gap-2 disabled:opacity-60">
                {saving ? <Loader2 size={13} className="animate-spin" /> : null}Save
              </button>
            </div>
          </div>
        ) : (
          <div className="flex items-start justify-between">
            <div className="flex items-center gap-4">
              <div className="w-12 h-12 rounded-xl bg-primary/10 flex items-center justify-center">
                <FolderKanban size={22} className="text-primary" />
              </div>
              <div>
                <h1 className="text-xl font-bold text-foreground">{project.name}</h1>
                <p className="text-sm text-muted-foreground mt-0.5">{project.description || 'No description'}</p>
                <div className="flex items-center gap-3 mt-2 text-xs text-muted-foreground">
                  {project.dueDate && <span className="flex items-center gap-1"><Calendar size={11} />{new Date(project.dueDate).toLocaleDateString()}</span>}
                  <span className="flex items-center gap-1"><Users size={11} />{project.memberCount ?? 0} members</span>
                </div>
              </div>
            </div>
            <div className="flex items-center gap-2">
              <Link href={`/projects/${project.id}/dashboard`} className="flex items-center gap-1.5 text-xs text-primary bg-primary/10 hover:bg-primary/20 px-3 py-1.5 rounded-lg transition-all font-medium">
                <CheckSquare size={12} />Dashboard
              </Link>
              <button onClick={() => { setEditName(project.name); setEditDesc(project.description ?? ''); setEditingProject(true); }} className="flex items-center gap-1.5 text-xs text-muted-foreground bg-muted hover:bg-primary/10 hover:text-primary px-3 py-1.5 rounded-lg transition-all">
                <Edit2 size={12} />Edit
              </button>
            </div>
          </div>
        )}
      </div>

      {/* Phases */}
      <div className="space-y-4">
        <div className="flex items-center justify-between">
          <h2 className="text-lg font-bold text-foreground">Phases ({phases.length})</h2>
          <button onClick={() => setCreatePhaseOpen(true)} className="btn-primary flex items-center gap-2 px-4 py-2 rounded-lg text-sm font-semibold">
            <Plus size={14} />Add Phase
          </button>
        </div>

        {phases.length === 0 ? (
          <div className="card-elevated p-10 text-center">
            <Calendar size={32} className="text-muted-foreground/30 mx-auto mb-3" />
            <p className="text-sm text-muted-foreground">No phases yet. Add a phase to start organizing work.</p>
          </div>
        ) : (
          <div className="space-y-3">
            {phases.map((phase) => (
              <div key={phase.id} className="card-elevated overflow-hidden">
                <div
                  className="flex items-center justify-between p-4 cursor-pointer hover:bg-muted/30 transition-colors"
                  onClick={() => setExpandedPhase(expandedPhase === phase.id ? null : phase.id)}
                >
                  <div className="flex items-center gap-3">
                    <div className="w-8 h-8 rounded-lg bg-primary/10 flex items-center justify-center">
                      <Calendar size={14} className="text-primary" />
                    </div>
                    <div>
                      <p className="font-semibold text-foreground text-sm">{phase.name}</p>
                      <p className="text-xs text-muted-foreground">{phase.description || 'No description'}</p>
                    </div>
                  </div>
                  <div className="flex items-center gap-3">
                    <span className={`badge-base text-xs ${PHASE_STATUS_BADGE[phase.status ?? 'DRAFT'] ?? PHASE_STATUS_BADGE.DRAFT}`}>
                      {phase.status ?? 'DRAFT'}
                    </span>
                    {expandedPhase === phase.id ? <ChevronUp size={16} className="text-muted-foreground" /> : <ChevronDown size={16} className="text-muted-foreground" />}
                  </div>
                </div>

                {expandedPhase === phase.id && (
                  <div className="border-t border-border p-4 bg-muted/20 fade-in">
                    <div className="flex flex-wrap items-center gap-2">
                      {phase.status !== 'PUBLISHED' && (
                        <button onClick={() => handlePhaseAction(phase.id, 'publish')} className="flex items-center gap-1.5 text-xs bg-green-50 text-green-700 border border-green-200 px-3 py-1.5 rounded-lg hover:bg-green-100 transition-colors">
                          <Eye size={12} />Publish
                        </button>
                      )}
                      {phase.status === 'PUBLISHED' && (
                        <button onClick={() => handlePhaseAction(phase.id, 'hide')} className="flex items-center gap-1.5 text-xs bg-amber-50 text-amber-700 border border-amber-200 px-3 py-1.5 rounded-lg hover:bg-amber-100 transition-colors">
                          Hide
                        </button>
                      )}
                      {phase.status !== 'CLOSED' && (
                        <button onClick={() => handlePhaseAction(phase.id, 'close')} className="flex items-center gap-1.5 text-xs bg-blue-50 text-blue-700 border border-blue-200 px-3 py-1.5 rounded-lg hover:bg-blue-100 transition-colors">
                          Close
                        </button>
                      )}
                      <Link href={`/tasks?phaseId=${phase.id}`} className="flex items-center gap-1.5 text-xs bg-primary/10 text-primary px-3 py-1.5 rounded-lg hover:bg-primary/20 transition-colors">
                        <CheckSquare size={12} />View Tasks
                      </Link>
                      <button onClick={() => handlePhaseAction(phase.id, 'delete')} className="flex items-center gap-1.5 text-xs text-red-600 bg-red-50 border border-red-200 px-3 py-1.5 rounded-lg hover:bg-red-100 transition-colors ml-auto">
                        <Trash2 size={12} />Delete Phase
                      </button>
                    </div>
                    {(phase.startDate || phase.endDate) && (
                      <div className="flex items-center gap-4 mt-3 text-xs text-muted-foreground">
                        {phase.startDate && <span>Start: {new Date(phase.startDate).toLocaleDateString()}</span>}
                        {phase.endDate && <span>End: {new Date(phase.endDate).toLocaleDateString()}</span>}
                      </div>
                    )}
                  </div>
                )}
              </div>
            ))}
          </div>
        )}
      </div>

      {createPhaseOpen && (
        <CreatePhaseModal
          projectId={project.id}
          onClose={() => setCreatePhaseOpen(false)}
          onCreated={(phase) => { setPhases(prev => [...prev, phase]); setCreatePhaseOpen(false); }}
        />
      )}
    </div>
  );
}
