'use client';
import React, { useCallback, useEffect, useState } from 'react';
import { Users, Plus, Search, MoreHorizontal, Loader2, Briefcase, CheckCircle, XCircle, Eye } from 'lucide-react';
import FloatingMenu from '@/components/ui/FloatingMenu';
import { toast } from 'sonner';
import { AxiosError } from 'axios';
import { recruitmentApi, type RecruitmentPost, type Application } from '@/lib/api/recruitment';

const POST_STATUS_BADGE: Record<string, string> = {
  OPEN: 'bg-green-50 text-green-700 border border-green-200',
  CLOSED: 'bg-muted text-muted-foreground border border-border',
  DRAFT: 'bg-amber-50 text-amber-700 border border-amber-200',
};

const APP_STATUS_BADGE: Record<string, string> = {
  PENDING: 'bg-amber-50 text-amber-700 border border-amber-200',
  UNDER_REVIEW: 'bg-blue-50 text-blue-700 border border-blue-200',
  MANAGER_APPROVED: 'bg-violet-50 text-violet-700 border border-violet-200',
  OWNER_APPROVED: 'bg-green-50 text-green-700 border border-green-200',
  REJECTED: 'bg-red-50 text-red-600 border border-red-200',
  WITHDRAWN: 'bg-muted text-muted-foreground border border-border',
};

interface CreatePostModalProps {
  onClose: () => void;
  onCreated: (post: RecruitmentPost) => void;
}

function CreatePostModal({ onClose, onCreated }: CreatePostModalProps) {
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [requirements, setRequirements] = useState('');
  const [role, setRole] = useState('DEVELOPER');
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!title.trim()) return;
    setLoading(true);
    try {
      const res = await recruitmentApi.createPost({ title: title.trim(), description: description || undefined, requirements: requirements || undefined, role });
      toast.success('Recruitment post created');
      onCreated(res.data);
    } catch (err) {
      toast.error(err instanceof AxiosError ? (err.response?.data?.message ?? 'Failed to create post') : 'Failed to create post');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 z-[1100] flex items-center justify-center p-4">
      <div className="fixed inset-0 bg-black/40 fade-in" onClick={onClose} />
      <div className="relative max-h-[calc(100dvh-2rem)] w-full max-w-lg overflow-y-auto rounded-2xl bg-card shadow-modal fade-scale-in">
        <div className="flex items-center justify-between p-6 border-b border-border">
          <div><h2 className="text-base font-bold text-foreground">New Recruitment Post</h2><p className="text-xs text-muted-foreground">Post an open position</p></div>
          <button onClick={onClose} className="w-8 h-8 rounded-lg flex items-center justify-center text-muted-foreground hover:text-foreground hover:bg-muted transition-all">✕</button>
        </div>
        <form onSubmit={handleSubmit} className="p-6 space-y-4">
          <div>
            <label className="block text-sm font-medium text-foreground mb-1.5">Title *</label>
            <input type="text" value={title} onChange={e => setTitle(e.target.value)} placeholder="Senior React Developer" className="input-base" required />
          </div>
          <div>
            <label className="block text-sm font-medium text-foreground mb-1.5">Role</label>
            <select value={role} onChange={e => setRole(e.target.value)} className="input-base">
              <option value="DEVELOPER">Developer</option>
              <option value="TESTER">Tester</option>
              <option value="BUG_HUNTER">Bug Hunter</option>
              <option value="PROJECT_MANAGER">Project Manager</option>
            </select>
          </div>
          <div>
            <label className="block text-sm font-medium text-foreground mb-1.5">Description</label>
            <textarea rows={3} value={description} onChange={e => setDescription(e.target.value)} placeholder="What will this person work on?" className="input-base resize-none" />
          </div>
          <div>
            <label className="block text-sm font-medium text-foreground mb-1.5">Requirements</label>
            <textarea rows={2} value={requirements} onChange={e => setRequirements(e.target.value)} placeholder="Skills and experience required…" className="input-base resize-none" />
          </div>
          <div className="flex items-center gap-3 pt-2">
            <button type="button" onClick={onClose} className="btn-secondary flex-1 py-2.5 rounded-lg font-medium text-sm">Cancel</button>
            <button type="submit" disabled={loading || !title.trim()} className="btn-primary flex-1 py-2.5 rounded-lg font-semibold text-sm flex items-center justify-center gap-2 disabled:opacity-60">
              {loading ? <><Loader2 size={14} className="animate-spin" /><span>Creating…</span></> : <><Briefcase size={14} /><span>Create Post</span></>}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

interface ApplicationsDrawerProps {
  post: RecruitmentPost;
  onClose: () => void;
}

function ApplicationsDrawer({ post, onClose }: ApplicationsDrawerProps) {
  const [applications, setApplications] = useState<Application[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    (async () => {
      try {
        const res = await recruitmentApi.listApplications(post.id);
        setApplications(Array.isArray(res.data) ? res.data : []);
      } catch { /* ignore */ } finally {
        setLoading(false);
      }
    })();
  }, [post.id]);

  const handleReview = async (appId: string, decision: 'APPROVE' | 'REJECT') => {
    try {
      const res = await recruitmentApi.managerReview(appId, { decision });
      setApplications(prev => prev.map(a => a.id === appId ? res.data : a));
      toast.success(`Application ${decision === 'APPROVE' ? 'approved' : 'rejected'}`);
    } catch (err) {
      toast.error(err instanceof AxiosError ? (err.response?.data?.message ?? 'Failed to review application') : 'Failed to review application');
    }
  };

  return (
    <div className="fixed inset-0 z-[1100] flex items-end justify-center p-4 sm:items-center">
      <div className="fixed inset-0 bg-black/40 fade-in" onClick={onClose} />
      <div className="relative flex max-h-[calc(100dvh-2rem)] w-full max-w-2xl flex-col overflow-hidden rounded-2xl bg-card shadow-modal fade-scale-in">
        <div className="flex items-center justify-between p-6 border-b border-border flex-shrink-0">
          <div>
            <h2 className="text-base font-bold text-foreground">Applications — {post.title}</h2>
            <p className="text-xs text-muted-foreground">{applications.length} application{applications.length !== 1 ? 's' : ''}</p>
          </div>
          <button onClick={onClose} className="w-8 h-8 rounded-lg flex items-center justify-center text-muted-foreground hover:text-foreground hover:bg-muted transition-all">✕</button>
        </div>
        <div className="overflow-y-auto flex-1 p-4 space-y-3">
          {loading ? (
            <div className="flex items-center justify-center py-8"><Loader2 size={20} className="animate-spin text-primary" /></div>
          ) : applications.length === 0 ? (
            <div className="text-center py-8 text-sm text-muted-foreground">No applications yet</div>
          ) : (
            applications.map(app => (
              <div key={app.id} className="card-elevated p-4">
                <div className="flex items-start justify-between">
                  <div>
                    <p className="font-medium text-foreground text-sm">{app.applicantName ?? app.applicantEmail ?? app.applicantUserId}</p>
                    {app.coverLetter && <p className="text-xs text-muted-foreground mt-1 line-clamp-2">{app.coverLetter}</p>}
                    <p className="text-xs text-muted-foreground mt-1">{app.appliedAt ? new Date(app.appliedAt).toLocaleDateString() : '—'}</p>
                  </div>
                  <span className={`badge-base text-xs ${APP_STATUS_BADGE[app.status ?? 'PENDING'] ?? APP_STATUS_BADGE.PENDING}`}>
                    {(app.status ?? 'PENDING').replace(/_/g, ' ')}
                  </span>
                </div>
                {(app.status === 'PENDING' || app.status === 'UNDER_REVIEW') && (
                  <div className="flex items-center gap-2 mt-3">
                    <button onClick={() => handleReview(app.id, 'APPROVE')} className="flex items-center gap-1.5 text-xs bg-green-50 text-green-700 border border-green-200 px-3 py-1.5 rounded-lg hover:bg-green-100 transition-colors">
                      <CheckCircle size={12} />Approve
                    </button>
                    <button onClick={() => handleReview(app.id, 'REJECT')} className="flex items-center gap-1.5 text-xs bg-red-50 text-red-600 border border-red-200 px-3 py-1.5 rounded-lg hover:bg-red-100 transition-colors">
                      <XCircle size={12} />Reject
                    </button>
                  </div>
                )}
              </div>
            ))
          )}
        </div>
      </div>
    </div>
  );
}

export default function RecruitmentContent() {
  const [posts, setPosts] = useState<RecruitmentPost[]>([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [createOpen, setCreateOpen] = useState(false);
  const [viewingPost, setViewingPost] = useState<RecruitmentPost | null>(null);
  const [activeMenu, setActiveMenu] = useState<string | null>(null);

  const loadPosts = useCallback(async () => {
    setLoading(true);
    try {
      const res = await recruitmentApi.listPosts({ page: 0, size: 50 });
      const list = res.data?.content ?? [];
      setPosts(list);
    } catch (err) {
      toast.error(err instanceof AxiosError && !err.response ? 'Cannot reach the recruitment service.' : 'Failed to load recruitment posts');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { loadPosts(); }, [loadPosts]);

  const handleClose = async (postId: string, title: string) => {
    try {
      const res = await recruitmentApi.closePost(postId);
      setPosts(prev => prev.map(p => p.id === postId ? res.data : p));
      toast.success(`"${title}" closed`);
    } catch (err) {
      toast.error(err instanceof AxiosError ? (err.response?.data?.message ?? 'Failed to close post') : 'Failed to close post');
    }
    setActiveMenu(null);
  };

  const handleDelete = async (postId: string, title: string) => {
    try {
      await recruitmentApi.deletePost(postId);
      setPosts(prev => prev.filter(p => p.id !== postId));
      toast.success(`"${title}" deleted`);
    } catch (err) {
      toast.error(err instanceof AxiosError ? (err.response?.data?.message ?? 'Failed to delete post') : 'Failed to delete post');
    }
    setActiveMenu(null);
  };

  const filtered = posts.filter(p => {
    const q = search.toLowerCase();
    const matchSearch = !q || p.title.toLowerCase().includes(q) || (p.description ?? '').toLowerCase().includes(q);
    const matchStatus = statusFilter === 'ALL' || p.status === statusFilter;
    return matchSearch && matchStatus;
  });

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-foreground">Recruitment</h1>
          <p className="text-muted-foreground text-sm mt-0.5">Manage open positions and review applications</p>
        </div>
        <button onClick={() => setCreateOpen(true)} className="btn-primary flex items-center gap-2 px-4 py-2 rounded-lg text-sm font-semibold">
          <Plus size={15} />New Post
        </button>
      </div>

      {/* Stats */}
      <div className="grid grid-cols-3 gap-3">
        {[
          { label: 'Open', value: posts.filter(p => p.status === 'OPEN').length, color: 'text-green-600' },
          { label: 'Total Posts', value: posts.length, color: 'text-foreground' },
          { label: 'Closed', value: posts.filter(p => p.status === 'CLOSED').length, color: 'text-muted-foreground' },
        ].map(s => (
          <div key={s.label} className="card-elevated p-4">
            <div className={`text-2xl font-bold font-tabular ${s.color}`}>{s.value}</div>
            <div className="text-xs text-muted-foreground mt-0.5">{s.label}</div>
          </div>
        ))}
      </div>

      {/* Filters */}
      <div className="flex flex-col sm:flex-row gap-3">
        <div className="relative flex-1">
          <Search size={14} className="absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground" />
          <input type="text" placeholder="Search posts…" value={search} onChange={e => setSearch(e.target.value)} className="input-base input-search text-sm" />
        </div>
        <div className="flex gap-1 p-1 bg-muted rounded-lg">
          {['ALL', 'OPEN', 'DRAFT', 'CLOSED'].map(s => (
            <button key={s} onClick={() => setStatusFilter(s)} className={`px-3 py-1.5 rounded-md text-xs font-medium transition-all ${statusFilter === s ? 'bg-card text-foreground shadow-card' : 'text-muted-foreground hover:text-foreground'}`}>
              {s === 'ALL' ? 'All' : s.charAt(0) + s.slice(1).toLowerCase()}
            </button>
          ))}
        </div>
      </div>

      {/* Posts */}
      {loading ? (
        <div className="flex items-center justify-center py-16"><Loader2 size={24} className="animate-spin text-primary" /></div>
      ) : filtered.length === 0 ? (
        <div className="card-elevated p-12 text-center">
          <Users size={36} className="text-muted-foreground/30 mx-auto mb-3" />
          <p className="text-sm text-muted-foreground">{search || statusFilter !== 'ALL' ? 'No posts match your filters' : 'No recruitment posts yet'}</p>
          {!search && statusFilter === 'ALL' && (
            <button onClick={() => setCreateOpen(true)} className="btn-primary mt-4 px-4 py-2 rounded-lg text-sm font-semibold inline-flex items-center gap-2">
              <Plus size={14} />Create First Post
            </button>
          )}
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4">
          {filtered.map(post => (
            <div key={post.id} className="card-elevated p-5 card-hover group relative">
              <div className="flex items-start justify-between mb-3">
                <div className="flex items-center gap-3 flex-1 min-w-0">
                  <div className="w-10 h-10 rounded-xl bg-primary/10 flex items-center justify-center flex-shrink-0">
                    <Briefcase size={18} className="text-primary" />
                  </div>
                  <div className="min-w-0">
                    <h3 className="font-semibold text-foreground text-sm truncate">{post.title}</h3>
                    <div className="flex items-center gap-2 mt-1">
                      <span className={`badge-base text-xs ${POST_STATUS_BADGE[post.status ?? 'OPEN'] ?? POST_STATUS_BADGE.OPEN}`}>{post.status ?? 'OPEN'}</span>
                      {post.role && <span className="text-xs text-muted-foreground">{post.role.replace(/_/g, ' ')}</span>}
                    </div>
                  </div>
                </div>
                <div className="relative flex-shrink-0">
                  <FloatingMenu
                    open={activeMenu === post.id}
                    onOpenChange={(open) => setActiveMenu(open ? post.id : null)}
                    widthClassName="w-40"
                    trigger={(triggerProps) => (
                      <button
                        {...triggerProps}
                        type="button"
                        aria-label={`Actions for ${post.title}`}
                        className="w-7 h-7 rounded-md flex items-center justify-center text-muted-foreground hover:text-foreground hover:bg-muted opacity-100 sm:opacity-0 sm:group-hover:opacity-100 focus-visible:opacity-100 transition-all"
                      >
                        <MoreHorizontal size={15} />
                      </button>
                    )}
                  >
                    <button onClick={() => setViewingPost(post)} className="flex items-center gap-2 w-full px-3 py-2 text-xs text-foreground hover:bg-muted transition-colors">
                      <Eye size={12} />View Applications
                    </button>
                    {post.status === 'OPEN' && (
                      <button onClick={() => handleClose(post.id, post.title)} className="flex items-center gap-2 w-full px-3 py-2 text-xs text-amber-600 hover:bg-amber-50 transition-colors">
                        Close Post
                      </button>
                    )}
                    <button onClick={() => handleDelete(post.id, post.title)} className="flex items-center gap-2 w-full px-3 py-2 text-xs text-red-600 hover:bg-red-50 transition-colors">
                      <XCircle size={12} />Delete
                    </button>
                  </FloatingMenu>
                </div>
              </div>
              <p className="text-xs text-muted-foreground leading-relaxed mb-4 line-clamp-2">{post.description || 'No description provided.'}</p>
              <div className="flex items-center justify-between pt-3 border-t border-border">
                <div className="flex items-center gap-1.5 text-xs text-muted-foreground">
                  <Users size={12} />
                  <span className="font-medium text-foreground">{post.applicationCount ?? 0}</span>
                  <span>applications</span>
                </div>
                <button onClick={() => setViewingPost(post)} className="text-xs text-primary font-medium hover:underline opacity-0 group-hover:opacity-100 transition-opacity">
                  View Applications
                </button>
              </div>
            </div>
          ))}
        </div>
      )}

      {createOpen && (
        <CreatePostModal
          onClose={() => setCreateOpen(false)}
          onCreated={(p) => { setPosts(prev => [p, ...prev]); setCreateOpen(false); }}
        />
      )}
      {viewingPost && <ApplicationsDrawer post={viewingPost} onClose={() => setViewingPost(null)} />}
    </div>
  );
}
