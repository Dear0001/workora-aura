'use client';
import React, { useCallback, useEffect, useRef, useState } from 'react';
import {
  Bug, Plus, Search, MoreHorizontal, Loader2, AlertTriangle,
  CheckCircle, XCircle, Clock, Paperclip, Upload, Trash2, Eye,
  FileText, Image as ImageIcon, X, Download,
} from 'lucide-react';
import { toast } from 'sonner';
import { AxiosError } from 'axios';
import { bugApi, type BugReport, type BugEvidence } from '@/lib/api/bugs';
import { projectsApi, type Phase } from '@/lib/api/projects';
import { organizationApi } from '@/lib/api/organization';

const STATUS_BADGE: Record<string, string> = {
  OPEN: 'bg-amber-50 text-amber-700 border border-amber-200',
  TRIAGED: 'bg-blue-50 text-blue-700 border border-blue-200',
  ACCEPTED: 'bg-violet-50 text-violet-700 border border-violet-200',
  REJECTED: 'bg-red-50 text-red-600 border border-red-200',
  RESOLVED: 'bg-green-50 text-green-700 border border-green-200',
  DUPLICATE: 'bg-muted text-muted-foreground border border-border',
  CLOSED: 'bg-muted text-muted-foreground border border-border',
};

const SEVERITY_BADGE: Record<string, string> = {
  LOW: 'bg-muted text-muted-foreground',
  MEDIUM: 'bg-amber-50 text-amber-700',
  HIGH: 'bg-orange-50 text-orange-700',
  CRITICAL: 'bg-red-50 text-red-700',
};

function isImageFile(name?: string) {
  if (!name) return false;
  return /\.(png|jpe?g|gif|webp|svg|bmp)$/i.test(name);
}

// ── Evidence Modal ────────────────────────────────────────────────────────────

interface EvidenceModalProps {
  bug: BugReport;
  onClose: () => void;
  onEvidenceChanged: (bugId: string, count: number) => void;
}

function EvidenceModal({ bug, onClose, onEvidenceChanged }: EvidenceModalProps) {
  const [evidence, setEvidence] = useState<BugEvidence[]>([]);
  const [loading, setLoading] = useState(true);
  const [uploading, setUploading] = useState(false);
  const [description, setDescription] = useState('');
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [previewUrl, setPreviewUrl] = useState<string | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const loadEvidence = useCallback(async () => {
    setLoading(true);
    try {
      let res = await bugApi.listEvidence(bug.id);
      const list = Array.isArray(res.data) ? res.data : [];
      setEvidence(list);
    } catch {
      toast.error('Failed to load evidence');
    } finally {
      setLoading(false);
    }
  }, [bug.id]);

  useEffect(() => { loadEvidence(); }, [loadEvidence]);

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0] ?? null;
    setSelectedFile(file);
    if (file && isImageFile(file.name)) {
      const url = URL.createObjectURL(file);
      setPreviewUrl(url);
    } else {
      setPreviewUrl(null);
    }
  };

  const handleUpload = async () => {
    if (!selectedFile) return;
    setUploading(true);
    try {
      const fd = new FormData();
      fd.append('file', selectedFile);
      if (description.trim()) fd.append('description', description.trim());
      let res = await bugApi.addEvidence(bug.id, fd);
      const newItem = res.data;
      setEvidence(prev => [...prev, newItem]);
      onEvidenceChanged(bug.id, evidence.length + 1);
      setSelectedFile(null);
      setDescription('');
      setPreviewUrl(null);
      if (fileInputRef.current) fileInputRef.current.value = '';
      toast.success('Evidence uploaded');
    } catch (err) {
      toast.error(err instanceof AxiosError ? (err.response?.data?.message ?? 'Upload failed') : 'Upload failed');
    } finally {
      setUploading(false);
    }
  };

  const handleRemove = async (evidenceId: string) => {
    try {
      await bugApi.removeEvidence(bug.id, evidenceId);
      const updated = evidence.filter(e => e.id !== evidenceId);
      setEvidence(updated);
      onEvidenceChanged(bug.id, updated.length);
      toast.success('Evidence removed');
    } catch {
      toast.error('Failed to remove evidence');
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      <div className="fixed inset-0 bg-black/40 fade-in" onClick={onClose} />
      <div className="relative bg-card rounded-2xl shadow-modal w-full max-w-lg max-h-[90vh] flex flex-col fade-scale-in">
        {/* Header */}
        <div className="flex items-start justify-between p-6 border-b border-border flex-shrink-0">
          <div className="flex-1 min-w-0 pr-4">
            <div className="flex items-center gap-2 mb-1">
              <Paperclip size={15} className="text-primary flex-shrink-0" />
              <h2 className="text-base font-bold text-foreground truncate">Evidence &amp; Attachments</h2>
            </div>
            <p className="text-xs text-muted-foreground truncate">{bug.title}</p>
          </div>
          <button onClick={onClose} className="w-8 h-8 rounded-lg flex items-center justify-center text-muted-foreground hover:text-foreground hover:bg-muted transition-all flex-shrink-0">
            <X size={15} />
          </button>
        </div>

        {/* Upload area */}
        <div className="p-6 border-b border-border flex-shrink-0 space-y-3">
          <p className="text-xs font-semibold text-muted-foreground uppercase tracking-wider">Upload New Evidence</p>
          <div
            className="border-2 border-dashed border-border rounded-xl p-4 text-center cursor-pointer hover:border-primary/50 hover:bg-primary/5 transition-all"
            onClick={() => fileInputRef.current?.click()}
          >
            {selectedFile ? (
              <div className="space-y-2">
                {previewUrl ? (
                  <img src={previewUrl} alt="Preview" className="max-h-32 mx-auto rounded-lg object-contain" />
                ) : (
                  <div className="flex items-center justify-center gap-2 text-sm text-foreground">
                    <FileText size={20} className="text-primary" />
                    <span className="font-medium truncate max-w-xs">{selectedFile.name}</span>
                  </div>
                )}
                <p className="text-xs text-muted-foreground">{(selectedFile.size / 1024).toFixed(1)} KB · Click to change</p>
              </div>
            ) : (
              <div className="space-y-1">
                <Upload size={24} className="text-muted-foreground/50 mx-auto" />
                <p className="text-sm text-muted-foreground">Click to select a file</p>
                <p className="text-xs text-muted-foreground/60">Images, PDFs, logs, videos…</p>
              </div>
            )}
          </div>
          <input ref={fileInputRef} type="file" className="hidden" onChange={handleFileChange} accept="image/*,.pdf,.txt,.log,.mp4,.mov,.zip" />
          <input
            type="text"
            value={description}
            onChange={e => setDescription(e.target.value)}
            placeholder="Optional description for this evidence…"
            className="input-base text-sm"
          />
          <button
            onClick={handleUpload}
            disabled={!selectedFile || uploading}
            className="btn-primary w-full py-2.5 rounded-lg font-semibold text-sm flex items-center justify-center gap-2 disabled:opacity-60"
          >
            {uploading ? <><Loader2 size={14} className="animate-spin" /><span>Uploading…</span></> : <><Upload size={14} /><span>Upload Evidence</span></>}
          </button>
        </div>

        {/* Evidence list */}
        <div className="flex-1 overflow-y-auto p-6 space-y-3">
          <p className="text-xs font-semibold text-muted-foreground uppercase tracking-wider">
            Attached Evidence {!loading && `(${evidence.length})`}
          </p>
          {loading ? (
            <div className="flex items-center justify-center py-8">
              <Loader2 size={20} className="animate-spin text-primary" />
            </div>
          ) : evidence.length === 0 ? (
            <div className="text-center py-8">
              <Paperclip size={28} className="text-muted-foreground/30 mx-auto mb-2" />
              <p className="text-sm text-muted-foreground">No evidence attached yet</p>
            </div>
          ) : (
            evidence.map(ev => (
              <div key={ev.id} className="flex items-start gap-3 p-3 rounded-xl border border-border bg-muted/30 hover:bg-muted/50 transition-colors group">
                <div className="w-9 h-9 rounded-lg bg-primary/10 flex items-center justify-center flex-shrink-0">
                  {isImageFile(ev.fileName) ? <ImageIcon size={16} className="text-primary" /> : <FileText size={16} className="text-primary" />}
                </div>
                <div className="flex-1 min-w-0">
                  <p className="text-sm font-medium text-foreground truncate">{ev.fileName ?? 'Attachment'}</p>
                  {ev.description && <p className="text-xs text-muted-foreground mt-0.5 line-clamp-2">{ev.description}</p>}
                  {ev.uploadedAt && (
                    <p className="text-xs text-muted-foreground/60 mt-0.5">
                      {new Date(ev.uploadedAt).toLocaleDateString()}
                    </p>
                  )}
                </div>
                <div className="flex items-center gap-1 opacity-0 group-hover:opacity-100 transition-opacity">
                  {ev.fileUrl && (
                    <a
                      href={ev.fileUrl}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="w-7 h-7 rounded-md flex items-center justify-center text-muted-foreground hover:text-primary hover:bg-primary/10 transition-all"
                      title="View / Download"
                    >
                      {isImageFile(ev.fileName) ? <Eye size={13} /> : <Download size={13} />}
                    </a>
                  )}
                  <button
                    onClick={() => handleRemove(ev.id)}
                    className="w-7 h-7 rounded-md flex items-center justify-center text-muted-foreground hover:text-red-600 hover:bg-red-50 transition-all"
                    title="Remove evidence"
                  >
                    <Trash2 size={13} />
                  </button>
                </div>
              </div>
            ))
          )}
        </div>
      </div>
    </div>
  );
}

// ── Create Bug Modal ──────────────────────────────────────────────────────────

interface CreateBugModalProps {
  phases: Phase[];
  onClose: () => void;
  onCreated: (bug: BugReport) => void;
}

function CreateBugModal({ phases, onClose, onCreated }: CreateBugModalProps) {
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [severity, setSeverity] = useState('MEDIUM');
  const [phaseId, setPhaseId] = useState(phases[0]?.id ?? '');
  const [attachments, setAttachments] = useState<File[]>([]);
  const [loading, setLoading] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const handleFileAdd = (e: React.ChangeEvent<HTMLInputElement>) => {
    const files = Array.from(e.target.files ?? []);
    setAttachments(prev => [...prev, ...files]);
    if (fileInputRef.current) fileInputRef.current.value = '';
  };

  const removeAttachment = (idx: number) => {
    setAttachments(prev => prev.filter((_, i) => i !== idx));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!title.trim() || !phaseId) return;
    setLoading(true);
    try {
      let res = await bugApi.createBugFromPhase(phaseId, {
        title: title.trim(),
        description: description || undefined,
        severity,
      });
      const newBug = res.data;

      // Upload any attached files as evidence
      if (attachments.length > 0) {
        await Promise.allSettled(
          attachments.map(file => {
            const fd = new FormData();
            fd.append('file', file);
            return bugApi.addEvidence(newBug.id, fd);
          })
        );
      }

      toast.success('Bug report created');
      onCreated({ ...newBug, evidenceCount: attachments.length });
    } catch (err) {
      toast.error(err instanceof AxiosError ? (err.response?.data?.message ?? 'Failed to create bug report') : 'Failed to create bug report');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      <div className="fixed inset-0 bg-black/40 fade-in" onClick={onClose} />
      <div className="relative bg-card rounded-2xl shadow-modal w-full max-w-md max-h-[90vh] flex flex-col fade-scale-in">
        <div className="flex items-center justify-between p-6 border-b border-border flex-shrink-0">
          <div>
            <h2 className="text-base font-bold text-foreground">Report Bug</h2>
            <p className="text-xs text-muted-foreground">Submit a new bug report</p>
          </div>
          <button onClick={onClose} className="w-8 h-8 rounded-lg flex items-center justify-center text-muted-foreground hover:text-foreground hover:bg-muted transition-all">
            <X size={15} />
          </button>
        </div>
        <form onSubmit={handleSubmit} className="p-6 space-y-4 overflow-y-auto flex-1">
          <div>
            <label className="block text-sm font-medium text-foreground mb-1.5">Phase</label>
            <select value={phaseId} onChange={e => setPhaseId(e.target.value)} className="input-base">
              {phases.map(p => <option key={p.id} value={p.id}>{p.name}</option>)}
            </select>
          </div>
          <div>
            <label className="block text-sm font-medium text-foreground mb-1.5">Title *</label>
            <input type="text" value={title} onChange={e => setTitle(e.target.value)} placeholder="Login button not responding" className="input-base" required />
          </div>
          <div>
            <label className="block text-sm font-medium text-foreground mb-1.5">Description</label>
            <textarea rows={3} value={description} onChange={e => setDescription(e.target.value)} placeholder="Steps to reproduce…" className="input-base resize-none" />
          </div>
          <div>
            <label className="block text-sm font-medium text-foreground mb-1.5">Severity</label>
            <select value={severity} onChange={e => setSeverity(e.target.value)} className="input-base">
              <option value="LOW">Low</option>
              <option value="MEDIUM">Medium</option>
              <option value="HIGH">High</option>
              <option value="CRITICAL">Critical</option>
            </select>
          </div>

          {/* Attachments */}
          <div>
            <label className="block text-sm font-medium text-foreground mb-1.5">
              Evidence / Attachments
              <span className="text-xs text-muted-foreground font-normal ml-1">(optional)</span>
            </label>
            <div
              className="border-2 border-dashed border-border rounded-xl p-3 text-center cursor-pointer hover:border-primary/50 hover:bg-primary/5 transition-all"
              onClick={() => fileInputRef.current?.click()}
            >
              <Upload size={18} className="text-muted-foreground/50 mx-auto mb-1" />
              <p className="text-xs text-muted-foreground">Click to attach screenshots, logs, or files</p>
            </div>
            <input ref={fileInputRef} type="file" multiple className="hidden" onChange={handleFileAdd} accept="image/*,.pdf,.txt,.log,.mp4,.mov,.zip" />
            {attachments.length > 0 && (
              <div className="mt-2 space-y-1.5">
                {attachments.map((f, i) => (
                  <div key={i} className="flex items-center gap-2 p-2 rounded-lg bg-muted/50 border border-border">
                    {isImageFile(f.name) ? <ImageIcon size={13} className="text-primary flex-shrink-0" /> : <FileText size={13} className="text-primary flex-shrink-0" />}
                    <span className="text-xs text-foreground truncate flex-1">{f.name}</span>
                    <span className="text-xs text-muted-foreground flex-shrink-0">{(f.size / 1024).toFixed(0)} KB</span>
                    <button type="button" onClick={() => removeAttachment(i)} className="w-5 h-5 rounded flex items-center justify-center text-muted-foreground hover:text-red-600 transition-colors flex-shrink-0">
                      <X size={11} />
                    </button>
                  </div>
                ))}
              </div>
            )}
          </div>

          <div className="flex items-center gap-3 pt-2">
            <button type="button" onClick={onClose} className="btn-secondary flex-1 py-2.5 rounded-lg font-medium text-sm">Cancel</button>
            <button type="submit" disabled={loading || !title.trim() || !phaseId} className="btn-primary flex-1 py-2.5 rounded-lg font-semibold text-sm flex items-center justify-center gap-2 disabled:opacity-60">
              {loading ? <><Loader2 size={14} className="animate-spin" /><span>Submitting…</span></> : <><Bug size={14} /><span>Submit Bug</span></>}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

// ── Main Component ────────────────────────────────────────────────────────────

export default function BugsContent() {
  const [bugs, setBugs] = useState<BugReport[]>([]);
  const [phases, setPhases] = useState<Phase[]>([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [createOpen, setCreateOpen] = useState(false);
  const [activeMenu, setActiveMenu] = useState<string | null>(null);
  const [evidenceBug, setEvidenceBug] = useState<BugReport | null>(null);

  useEffect(() => {
    (async () => {
      try {
        const orgRes = await organizationApi.listCompanies({ page: 0, size: 1 });
        const cId = orgRes.data?.content?.[0]?.id;
        if (!cId) return;
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
      } catch { /* ignore */ }
    })();
  }, []);

  const loadBugs = useCallback(async () => {
    setLoading(true);
    try {
      let res = await bugApi.listBugReports({ page: 0, size: 100 });
      const list = res.data?.content ?? [];
      setBugs(list);
    } catch (err) {
      toast.error(err instanceof AxiosError && !err.response ? 'Cannot reach the bug service.' : 'Failed to load bug reports');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { loadBugs(); }, [loadBugs]);

  const handleAction = async (bugId: string, action: 'accept' | 'reject' | 'resolve' | 'close') => {
    try {
      let res: { data: BugReport };
      if (action === 'accept') res = await bugApi.acceptBug(bugId);
      else if (action === 'reject') res = await bugApi.rejectBug(bugId);
      else if (action === 'resolve') res = await bugApi.resolveBug(bugId);
      else res = await bugApi.closeBug(bugId);
      setBugs(prev => prev.map(b => b.id === bugId ? res.data : b));
      toast.success(`Bug ${action}ed`);
    } catch (err) {
      toast.error(err instanceof AxiosError ? (err.response?.data?.message ?? `Failed to ${action} bug`) : `Failed to ${action} bug`);
    }
    setActiveMenu(null);
  };

  const handleEvidenceChanged = (bugId: string, count: number) => {
    setBugs(prev => prev.map(b => b.id === bugId ? { ...b, evidenceCount: count } : b));
  };

  const filtered = bugs.filter(b => {
    const q = search.toLowerCase();
    const matchSearch = !q || b.title.toLowerCase().includes(q) || (b.description ?? '').toLowerCase().includes(q);
    const matchStatus = statusFilter === 'ALL' || b.status === statusFilter;
    return matchSearch && matchStatus;
  });

  const stats = [
    { label: 'Open', value: bugs.filter(b => b.status === 'OPEN').length, icon: AlertTriangle, color: 'text-amber-600', bg: 'bg-amber-50 border-amber-200' },
    { label: 'In Triage', value: bugs.filter(b => b.status === 'TRIAGED').length, icon: Clock, color: 'text-blue-600', bg: 'bg-blue-50 border-blue-200' },
    { label: 'Resolved', value: bugs.filter(b => b.status === 'RESOLVED').length, icon: CheckCircle, color: 'text-green-600', bg: 'bg-green-50 border-green-200' },
    { label: 'Rejected', value: bugs.filter(b => b.status === 'REJECTED').length, icon: XCircle, color: 'text-red-600', bg: 'bg-red-50 border-red-200' },
  ];

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-foreground">Bug Triage</h1>
          <p className="text-muted-foreground text-sm mt-0.5">Track and manage bug reports across projects</p>
        </div>
        {phases.length > 0 && (
          <button onClick={() => setCreateOpen(true)} className="btn-primary flex items-center gap-2 px-4 py-2 rounded-lg text-sm font-semibold">
            <Plus size={15} />Report Bug
          </button>
        )}
      </div>

      {/* Stats */}
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
        {stats.map(s => (
          <div key={s.label} className={`card-elevated p-4 border ${s.bg}`}>
            <div className="flex items-center justify-between mb-1">
              <span className="text-xs text-muted-foreground">{s.label}</span>
              <s.icon size={14} className={s.color} />
            </div>
            <div className={`text-2xl font-bold font-tabular ${s.color}`}>{s.value}</div>
          </div>
        ))}
      </div>

      {/* Filters */}
      <div className="flex flex-col sm:flex-row gap-3">
        <div className="relative flex-1">
          <Search size={14} className="absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground" />
          <input type="text" placeholder="Search bug reports…" value={search} onChange={e => setSearch(e.target.value)} className="input-base pl-8 text-sm" />
        </div>
        <div className="flex gap-1 p-1 bg-muted rounded-lg overflow-x-auto">
          {['ALL', 'OPEN', 'TRIAGED', 'ACCEPTED', 'RESOLVED', 'CLOSED'].map(s => (
            <button key={s} onClick={() => setStatusFilter(s)} className={`px-3 py-1.5 rounded-md text-xs font-medium transition-all whitespace-nowrap ${statusFilter === s ? 'bg-card text-foreground shadow-card' : 'text-muted-foreground hover:text-foreground'}`}>
              {s === 'ALL' ? 'All' : s.charAt(0) + s.slice(1).toLowerCase()}
            </button>
          ))}
        </div>
      </div>

      {/* Bug list */}
      {loading ? (
        <div className="flex items-center justify-center py-16"><Loader2 size={24} className="animate-spin text-primary" /></div>
      ) : filtered.length === 0 ? (
        <div className="card-elevated p-12 text-center">
          <Bug size={36} className="text-muted-foreground/30 mx-auto mb-3" />
          <p className="text-sm text-muted-foreground">{search || statusFilter !== 'ALL' ? 'No bugs match your filters' : 'No bug reports yet'}</p>
        </div>
      ) : (
        <div className="card-elevated overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-border bg-muted/40">
                  <th className="px-4 py-3 text-left text-xs font-semibold text-muted-foreground uppercase tracking-wider">Bug</th>
                  <th className="px-4 py-3 text-left text-xs font-semibold text-muted-foreground uppercase tracking-wider">Severity</th>
                  <th className="px-4 py-3 text-left text-xs font-semibold text-muted-foreground uppercase tracking-wider">Status</th>
                  <th className="px-4 py-3 text-left text-xs font-semibold text-muted-foreground uppercase tracking-wider">Evidence</th>
                  <th className="px-4 py-3 text-left text-xs font-semibold text-muted-foreground uppercase tracking-wider">Reported</th>
                  <th className="px-4 py-3 w-12" />
                </tr>
              </thead>
              <tbody>
                {filtered.map((bug, i) => (
                  <tr key={bug.id} className={`border-b border-border last:border-0 hover:bg-muted/40 transition-colors group ${i % 2 === 1 ? 'bg-muted/10' : ''}`}>
                    <td className="px-4 py-3">
                      <div className="flex items-center gap-2.5">
                        <Bug size={14} className="text-muted-foreground flex-shrink-0" />
                        <div>
                          <p className="font-medium text-foreground">{bug.title}</p>
                          {bug.description && <p className="text-xs text-muted-foreground truncate max-w-xs">{bug.description}</p>}
                        </div>
                      </div>
                    </td>
                    <td className="px-4 py-3">
                      <span className={`badge-base text-xs ${SEVERITY_BADGE[bug.severity ?? 'MEDIUM'] ?? ''}`}>{bug.severity ?? 'MEDIUM'}</span>
                    </td>
                    <td className="px-4 py-3">
                      <span className={`badge-base text-xs ${STATUS_BADGE[bug.status ?? 'OPEN'] ?? STATUS_BADGE.OPEN}`}>{bug.status ?? 'OPEN'}</span>
                    </td>
                    <td className="px-4 py-3">
                      <button
                        onClick={() => setEvidenceBug(bug)}
                        className="flex items-center gap-1.5 text-xs text-muted-foreground hover:text-primary transition-colors group/ev"
                        title="View / manage evidence"
                      >
                        <Paperclip size={12} className="group-hover/ev:text-primary" />
                        <span className="font-medium">{bug.evidenceCount ?? 0}</span>
                      </button>
                    </td>
                    <td className="px-4 py-3 text-xs text-muted-foreground">
                      {bug.createdAt ? new Date(bug.createdAt).toLocaleDateString() : '—'}
                    </td>
                    <td className="px-4 py-3">
                      <div className="relative">
                        <button onClick={() => setActiveMenu(activeMenu === bug.id ? null : bug.id)} className="w-7 h-7 rounded-md flex items-center justify-center text-muted-foreground hover:text-foreground hover:bg-muted opacity-0 group-hover:opacity-100 transition-all">
                          <MoreHorizontal size={15} />
                        </button>
                        {activeMenu === bug.id && (
                          <>
                            <div className="fixed inset-0 z-10" onClick={() => setActiveMenu(null)} />
                            <div className="absolute right-0 top-full mt-1 w-44 bg-card border border-border rounded-xl shadow-elevated z-20 py-1 fade-scale-in">
                              <button
                                onClick={() => { setEvidenceBug(bug); setActiveMenu(null); }}
                                className="flex items-center gap-2 w-full px-3 py-2 text-xs text-foreground hover:bg-muted transition-colors"
                              >
                                <Paperclip size={12} />View Evidence
                                {(bug.evidenceCount ?? 0) > 0 && (
                                  <span className="ml-auto bg-primary/10 text-primary rounded-full px-1.5 py-0.5 text-[10px] font-semibold">{bug.evidenceCount}</span>
                                )}
                              </button>
                              <button
                                onClick={() => { setEvidenceBug(bug); setActiveMenu(null); }}
                                className="flex items-center gap-2 w-full px-3 py-2 text-xs text-foreground hover:bg-muted transition-colors"
                              >
                                <Upload size={12} />Attach Evidence
                              </button>
                              <div className="border-t border-border my-1" />
                              {bug.status === 'OPEN' && <button onClick={() => handleAction(bug.id, 'accept')} className="flex items-center gap-2 w-full px-3 py-2 text-xs text-green-600 hover:bg-green-50 transition-colors"><CheckCircle size={12} />Accept</button>}
                              {(bug.status === 'OPEN' || bug.status === 'TRIAGED') && <button onClick={() => handleAction(bug.id, 'reject')} className="flex items-center gap-2 w-full px-3 py-2 text-xs text-red-600 hover:bg-red-50 transition-colors"><XCircle size={12} />Reject</button>}
                              {bug.status === 'ACCEPTED' && <button onClick={() => handleAction(bug.id, 'resolve')} className="flex items-center gap-2 w-full px-3 py-2 text-xs text-blue-600 hover:bg-blue-50 transition-colors"><CheckCircle size={12} />Resolve</button>}
                              {bug.status !== 'CLOSED' && <button onClick={() => handleAction(bug.id, 'close')} className="flex items-center gap-2 w-full px-3 py-2 text-xs text-muted-foreground hover:bg-muted transition-colors">Close</button>}
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

      {createOpen && phases.length > 0 && (
        <CreateBugModal
          phases={phases}
          onClose={() => setCreateOpen(false)}
          onCreated={(b) => { setBugs(prev => [b, ...prev]); setCreateOpen(false); }}
        />
      )}

      {evidenceBug && (
        <EvidenceModal
          bug={evidenceBug}
          onClose={() => setEvidenceBug(null)}
          onEvidenceChanged={handleEvidenceChanged}
        />
      )}
    </div>
  );
}
