'use client';
import React, { useCallback, useEffect, useState } from 'react';
import {
  Users, Search, Loader2, CheckCircle, XCircle, ChevronDown, ChevronUp,
  FileText, Paperclip, Clock, User, Briefcase, MessageSquare, Filter
} from 'lucide-react';
import { toast } from 'sonner';
import { AxiosError } from 'axios';
import { recruitmentApi, type Application, type RecruitmentPost } from '@/lib/api/recruitment';

const STATUS_BADGE: Record<string, string> = {
  PENDING: 'bg-amber-50 text-amber-700 border border-amber-200',
  UNDER_REVIEW: 'bg-blue-50 text-blue-700 border border-blue-200',
  MANAGER_APPROVED: 'bg-violet-50 text-violet-700 border border-violet-200',
  OWNER_APPROVED: 'bg-green-50 text-green-700 border border-green-200',
  REJECTED: 'bg-red-50 text-red-600 border border-red-200',
  WITHDRAWN: 'bg-muted text-muted-foreground border border-border',
};

const ROLE_LABEL: Record<string, string> = {
  DEVELOPER: 'Developer',
  TESTER: 'Tester',
  BUG_HUNTER: 'Bug Hunter',
  PROJECT_MANAGER: 'Project Manager',
};

interface ApplicationWithPost extends Application {
  post?: RecruitmentPost;
}

interface DecisionPanelProps {
  app: ApplicationWithPost;
  onDecision: (appId: string, decision: 'APPROVE' | 'REJECT', notes: string) => Promise<void>;
}

function DecisionPanel({ app, onDecision }: DecisionPanelProps) {
  const [expanded, setExpanded] = useState(false);
  const [notes, setNotes] = useState('');
  const [submitting, setSubmitting] = useState<'APPROVE' | 'REJECT' | null>(null);

  const canDecide = app.status === 'PENDING' || app.status === 'UNDER_REVIEW';

  const handleDecision = async (decision: 'APPROVE' | 'REJECT') => {
    setSubmitting(decision);
    try {
      await onDecision(app.id, decision, notes);
      setNotes('');
      setExpanded(false);
    } finally {
      setSubmitting(null);
    }
  };

  return (
    <div className="border border-border rounded-xl overflow-hidden bg-card">
      {/* Header row */}
      <button
        type="button"
        onClick={() => setExpanded(v => !v)}
        className="w-full flex items-start gap-4 p-4 hover:bg-muted/40 transition-colors text-left"
      >
        {/* Avatar */}
        <div className="w-10 h-10 rounded-full bg-primary/10 flex items-center justify-center flex-shrink-0 mt-0.5">
          <User size={18} className="text-primary" />
        </div>

        {/* Info */}
        <div className="flex-1 min-w-0">
          <div className="flex items-center gap-2 flex-wrap">
            <span className="font-semibold text-sm text-foreground">
              {app.applicantName ?? app.applicantEmail ?? app.applicantUserId}
            </span>
            <span className={`badge-base text-xs px-2 py-0.5 rounded-full ${STATUS_BADGE[app.status ?? 'PENDING'] ?? STATUS_BADGE.PENDING}`}>
              {(app.status ?? 'PENDING').replace(/_/g, ' ')}
            </span>
          </div>
          {app.post && (
            <p className="text-xs text-muted-foreground mt-0.5 flex items-center gap-1">
              <Briefcase size={11} />
              {app.post.title}
              {app.post.role && <span className="ml-1 text-muted-foreground/70">· {ROLE_LABEL[app.post.role] ?? app.post.role}</span>}
            </p>
          )}
          <p className="text-xs text-muted-foreground mt-0.5 flex items-center gap-1">
            <Clock size={11} />
            Applied {app.appliedAt ? new Date(app.appliedAt).toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' }) : '—'}
          </p>
        </div>

        {/* Expand toggle */}
        <div className="flex-shrink-0 text-muted-foreground">
          {expanded ? <ChevronUp size={16} /> : <ChevronDown size={16} />}
        </div>
      </button>

      {/* Expanded detail */}
      {expanded && (
        <div className="border-t border-border">
          {/* Applicant details */}
          <div className="p-4 grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <h4 className="text-xs font-semibold text-muted-foreground uppercase tracking-wider mb-2 flex items-center gap-1.5">
                <User size={12} /> Applicant Details
              </h4>
              <div className="space-y-1.5">
                {app.applicantName && (
                  <div className="flex items-center gap-2">
                    <span className="text-xs text-muted-foreground w-16">Name</span>
                    <span className="text-xs font-medium text-foreground">{app.applicantName}</span>
                  </div>
                )}
                {app.applicantEmail && (
                  <div className="flex items-center gap-2">
                    <span className="text-xs text-muted-foreground w-16">Email</span>
                    <span className="text-xs font-medium text-foreground">{app.applicantEmail}</span>
                  </div>
                )}
                <div className="flex items-center gap-2">
                  <span className="text-xs text-muted-foreground w-16">User ID</span>
                  <span className="text-xs font-mono text-muted-foreground truncate max-w-[140px]">{app.applicantUserId}</span>
                </div>
                <div className="flex items-center gap-2">
                  <span className="text-xs text-muted-foreground w-16">Status</span>
                  <span className={`badge-base text-xs px-2 py-0.5 rounded-full ${STATUS_BADGE[app.status ?? 'PENDING'] ?? STATUS_BADGE.PENDING}`}>
                    {(app.status ?? 'PENDING').replace(/_/g, ' ')}
                  </span>
                </div>
                {app.updatedAt && (
                  <div className="flex items-center gap-2">
                    <span className="text-xs text-muted-foreground w-16">Updated</span>
                    <span className="text-xs text-muted-foreground">{new Date(app.updatedAt).toLocaleDateString()}</span>
                  </div>
                )}
              </div>
            </div>

            {/* Position details */}
            {app.post && (
              <div>
                <h4 className="text-xs font-semibold text-muted-foreground uppercase tracking-wider mb-2 flex items-center gap-1.5">
                  <Briefcase size={12} /> Position
                </h4>
                <div className="space-y-1.5">
                  <div className="flex items-start gap-2">
                    <span className="text-xs text-muted-foreground w-16 flex-shrink-0">Title</span>
                    <span className="text-xs font-medium text-foreground">{app.post.title}</span>
                  </div>
                  {app.post.role && (
                    <div className="flex items-center gap-2">
                      <span className="text-xs text-muted-foreground w-16">Role</span>
                      <span className="text-xs font-medium text-foreground">{ROLE_LABEL[app.post.role] ?? app.post.role}</span>
                    </div>
                  )}
                  {app.post.status && (
                    <div className="flex items-center gap-2">
                      <span className="text-xs text-muted-foreground w-16">Post</span>
                      <span className="text-xs text-muted-foreground">{app.post.status}</span>
                    </div>
                  )}
                </div>
              </div>
            )}
          </div>

          {/* Work samples / cover letter */}
          <div className="px-4 pb-4 space-y-3">
            {app.coverLetter && (
              <div>
                <h4 className="text-xs font-semibold text-muted-foreground uppercase tracking-wider mb-2 flex items-center gap-1.5">
                  <FileText size={12} /> Cover Letter / Work Sample
                </h4>
                <div className="bg-muted/50 rounded-lg p-3 border border-border">
                  <p className="text-sm text-foreground leading-relaxed whitespace-pre-wrap">{app.coverLetter}</p>
                </div>
              </div>
            )}

            {/* Attachments placeholder — shown when no cover letter to indicate where samples would appear */}
            {!app.coverLetter && (
              <div className="flex items-center gap-2 text-xs text-muted-foreground bg-muted/30 rounded-lg p-3 border border-dashed border-border">
                <Paperclip size={13} />
                <span>No work samples or cover letter submitted</span>
              </div>
            )}

            {/* Previous review notes */}
            {app.managerReview && (
              <div>
                <h4 className="text-xs font-semibold text-muted-foreground uppercase tracking-wider mb-2 flex items-center gap-1.5">
                  <MessageSquare size={12} /> Previous Manager Notes
                </h4>
                <div className="bg-violet-50 border border-violet-200 rounded-lg p-3">
                  <p className="text-xs text-violet-800">{app.managerReview}</p>
                </div>
              </div>
            )}
          </div>

          {/* Decision area */}
          {canDecide ? (
            <div className="border-t border-border p-4 bg-muted/20">
              <h4 className="text-xs font-semibold text-muted-foreground uppercase tracking-wider mb-3 flex items-center gap-1.5">
                <MessageSquare size={12} /> Manager Decision
              </h4>
              <textarea
                value={notes}
                onChange={e => setNotes(e.target.value)}
                placeholder="Add feedback or notes for this applicant (optional)…"
                rows={3}
                className="input-base resize-none text-sm w-full mb-3"
              />
              <div className="flex items-center gap-2">
                <button
                  type="button"
                  onClick={() => handleDecision('APPROVE')}
                  disabled={submitting !== null}
                  className="flex items-center gap-1.5 text-sm bg-green-600 text-white px-4 py-2 rounded-lg hover:bg-green-700 transition-colors font-medium disabled:opacity-60"
                >
                  {submitting === 'APPROVE' ? <Loader2 size={14} className="animate-spin" /> : <CheckCircle size={14} />}
                  Approve
                </button>
                <button
                  type="button"
                  onClick={() => handleDecision('REJECT')}
                  disabled={submitting !== null}
                  className="flex items-center gap-1.5 text-sm bg-red-50 text-red-600 border border-red-200 px-4 py-2 rounded-lg hover:bg-red-100 transition-colors font-medium disabled:opacity-60"
                >
                  {submitting === 'REJECT' ? <Loader2 size={14} className="animate-spin" /> : <XCircle size={14} />}
                  Reject
                </button>
              </div>
            </div>
          ) : (
            <div className="border-t border-border px-4 py-3 bg-muted/10">
              <p className="text-xs text-muted-foreground flex items-center gap-1.5">
                <CheckCircle size={12} className={app.status === 'REJECTED' ? 'text-red-400' : 'text-green-500'} />
                Decision already recorded: <span className="font-medium">{(app.status ?? '').replace(/_/g, ' ')}</span>
              </p>
            </div>
          )}
        </div>
      )}
    </div>
  );
}

export default function ManagerDecisionsContent() {
  const [posts, setPosts] = useState<RecruitmentPost[]>([]);
  const [applications, setApplications] = useState<ApplicationWithPost[]>([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState<string>('PENDING,UNDER_REVIEW');

  const loadData = useCallback(async () => {
    setLoading(true);
    try {
      const postsRes = await recruitmentApi.listPosts({ page: 0, size: 100 });
      const postList: RecruitmentPost[] = postsRes.data?.content ?? [];
      setPosts(postList);

      // Fetch applications for all open/draft posts
      const relevantPosts = postList.filter(p => p.status === 'OPEN' || p.status === 'DRAFT');
      const appResults = await Promise.allSettled(
        relevantPosts.map(p => recruitmentApi.listApplications(p.id).then(r => ({ post: p, apps: Array.isArray(r.data) ? r.data : [] })))
      );

      const allApps: ApplicationWithPost[] = [];
      appResults.forEach(result => {
        if (result.status === 'fulfilled') {
          result.value.apps.forEach(app => {
            allApps.push({ ...app, post: result.value.post });
          });
        }
      });
      setApplications(allApps);
    } catch (err) {
      toast.error(err instanceof AxiosError && !err.response ? 'Cannot reach the recruitment service.' : 'Failed to load applications');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { loadData(); }, [loadData]);

  const handleDecision = async (appId: string, decision: 'APPROVE' | 'REJECT', notes: string) => {
    try {
      const res = await recruitmentApi.managerReview(appId, { decision, notes: notes || undefined });
      setApplications(prev => prev.map(a => a.id === appId ? { ...res.data, post: a.post } : a));
      toast.success(`Application ${decision === 'APPROVE' ? 'approved' : 'rejected'}`);
    } catch (err) {
      toast.error(err instanceof AxiosError ? (err.response?.data?.message ?? 'Failed to submit decision') : 'Failed to submit decision');
      throw err;
    }
  };

  const filtered = applications.filter(app => {
    const q = search.toLowerCase();
    const matchSearch = !q
      || (app.applicantName ?? '').toLowerCase().includes(q)
      || (app.applicantEmail ?? '').toLowerCase().includes(q)
      || (app.post?.title ?? '').toLowerCase().includes(q);
    const statuses = statusFilter === 'ALL' ? null : statusFilter.split(',');
    const matchStatus = !statuses || statuses.includes(app.status ?? 'PENDING');
    return matchSearch && matchStatus;
  });

  const pendingCount = applications.filter(a => a.status === 'PENDING' || a.status === 'UNDER_REVIEW').length;
  const approvedCount = applications.filter(a => a.status === 'MANAGER_APPROVED' || a.status === 'OWNER_APPROVED').length;
  const rejectedCount = applications.filter(a => a.status === 'REJECTED').length;

  const STATUS_FILTERS = [
    { label: 'Needs Review', value: 'PENDING,UNDER_REVIEW' },
    { label: 'Approved', value: 'MANAGER_APPROVED,OWNER_APPROVED' },
    { label: 'Rejected', value: 'REJECTED' },
    { label: 'All', value: 'ALL' },
  ];

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-foreground">Manager Decisions</h1>
          <p className="text-muted-foreground text-sm mt-0.5">Review applicants and submit accept/reject decisions</p>
        </div>
      </div>

      {/* Stats */}
      <div className="grid grid-cols-3 gap-3">
        <div className="card-elevated p-4">
          <div className="text-2xl font-bold font-tabular text-amber-600">{pendingCount}</div>
          <div className="text-xs text-muted-foreground mt-0.5">Needs Review</div>
        </div>
        <div className="card-elevated p-4">
          <div className="text-2xl font-bold font-tabular text-green-600">{approvedCount}</div>
          <div className="text-xs text-muted-foreground mt-0.5">Approved</div>
        </div>
        <div className="card-elevated p-4">
          <div className="text-2xl font-bold font-tabular text-red-500">{rejectedCount}</div>
          <div className="text-xs text-muted-foreground mt-0.5">Rejected</div>
        </div>
      </div>

      {/* Filters */}
      <div className="flex flex-col sm:flex-row gap-3">
        <div className="relative flex-1">
          <Search size={15} className="absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground" />
          <input
            type="text"
            placeholder="Search by name, email, or position…"
            value={search}
            onChange={e => setSearch(e.target.value)}
            className="input-base input-search text-sm"
          />
        </div>
        <div className="flex items-center gap-1.5 bg-muted/50 rounded-lg p-1 border border-border">
          <Filter size={13} className="text-muted-foreground ml-1.5" />
          {STATUS_FILTERS.map(f => (
            <button
              key={f.value}
              type="button"
              onClick={() => setStatusFilter(f.value)}
              className={`px-3 py-1.5 rounded-md text-xs font-medium transition-all ${
                statusFilter === f.value
                  ? 'bg-card text-foreground shadow-sm'
                  : 'text-muted-foreground hover:text-foreground'
              }`}
            >
              {f.label}
            </button>
          ))}
        </div>
      </div>

      {/* Applications list */}
      {loading ? (
        <div className="flex items-center justify-center py-16">
          <Loader2 size={24} className="animate-spin text-primary" />
        </div>
      ) : filtered.length === 0 ? (
        <div className="flex flex-col items-center justify-center py-16 text-center">
          <div className="w-14 h-14 rounded-2xl bg-muted flex items-center justify-center mb-4">
            <Users size={24} className="text-muted-foreground" />
          </div>
          <p className="text-sm font-medium text-foreground">No applications found</p>
          <p className="text-xs text-muted-foreground mt-1">
            {search ? 'Try adjusting your search or filter' : 'No applications match the current filter'}
          </p>
        </div>
      ) : (
        <div className="space-y-3">
          {filtered.map(app => (
            <DecisionPanel key={app.id} app={app} onDecision={handleDecision} />
          ))}
        </div>
      )}
    </div>
  );
}
