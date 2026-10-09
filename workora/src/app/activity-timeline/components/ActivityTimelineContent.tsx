'use client';
import React, { useCallback, useEffect, useState } from 'react';
import {
  CheckSquare, Bug, FileText, Users, CreditCard, FolderKanban,
  RefreshCw, Loader2, Filter, Clock, ChevronRight, AlertCircle,
  ArrowUpRight, Circle,
} from 'lucide-react';
import Link from 'next/link';
import { toast } from 'sonner';
import { AxiosError } from 'axios';
import { dashboardApi } from '@/lib/api/dashboard';
import type { DashboardActivityItem } from '@/lib/api/types';
import Icon from '@/components/ui/AppIcon';


// ── Domain config ─────────────────────────────────────────────────────────────

type Domain = 'ALL' | 'TASK' | 'BUG' | 'SUBMISSION' | 'RECRUITMENT' | 'PAYMENT' | 'PROJECT';

interface DomainConfig {
  label: string;
  icon: React.ElementType;
  color: string;
  bg: string;
  border: string;
  dot: string;
  types: string[];
}

const DOMAIN_CONFIG: Record<Domain, DomainConfig> = {
  ALL: {
    label: 'All Activity',
    icon: Circle,
    color: 'text-muted-foreground',
    bg: 'bg-muted',
    border: 'border-border',
    dot: 'bg-muted-foreground',
    types: [],
  },
  TASK: {
    label: 'Tasks',
    icon: CheckSquare,
    color: 'text-blue-600',
    bg: 'bg-blue-50',
    border: 'border-blue-200',
    dot: 'bg-blue-500',
    types: ['task_done', 'task', 'task_created', 'task_updated', 'task_assigned', 'task_status'],
  },
  BUG: {
    label: 'Bug Reports',
    icon: Bug,
    color: 'text-red-600',
    bg: 'bg-red-50',
    border: 'border-red-200',
    dot: 'bg-red-500',
    types: ['bug', 'bug_report', 'bug_created', 'bug_triaged', 'bug_resolved', 'bug_closed'],
  },
  SUBMISSION: {
    label: 'Submissions',
    icon: FileText,
    color: 'text-violet-600',
    bg: 'bg-violet-50',
    border: 'border-violet-200',
    dot: 'bg-violet-500',
    types: ['submission', 'work_submitted', 'attachment', 'comment'],
  },
  RECRUITMENT: {
    label: 'Recruitment',
    icon: Users,
    color: 'text-amber-600',
    bg: 'bg-amber-50',
    border: 'border-amber-200',
    dot: 'bg-amber-500',
    types: ['member', 'recruitment', 'application', 'hired', 'invitation'],
  },
  PAYMENT: {
    label: 'Payments',
    icon: CreditCard,
    color: 'text-green-600',
    bg: 'bg-green-50',
    border: 'border-green-200',
    dot: 'bg-green-500',
    types: ['payment', 'fee', 'bakong', 'payment_verified', 'payment_pending'],
  },
  PROJECT: {
    label: 'Projects',
    icon: FolderKanban,
    color: 'text-indigo-600',
    bg: 'bg-indigo-50',
    border: 'border-indigo-200',
    dot: 'bg-indigo-500',
    types: ['project', 'project_created', 'phase', 'warn'],
  },
};

// ── Helpers ───────────────────────────────────────────────────────────────────

function detectDomain(type: string): Domain {
  const t = type.toLowerCase();
  for (const [domain, cfg] of Object.entries(DOMAIN_CONFIG)) {
    if (domain === 'ALL') continue;
    if (cfg.types.some((k) => t.includes(k))) return domain as Domain;
  }
  return 'PROJECT';
}

function formatRelativeTime(dateStr?: string): string {
  if (!dateStr) return '';
  const diff = Date.now() - new Date(dateStr).getTime();
  const mins = Math.floor(diff / 60000);
  if (mins < 1) return 'just now';
  if (mins < 60) return `${mins}m ago`;
  const hrs = Math.floor(mins / 60);
  if (hrs < 24) return `${hrs}h ago`;
  const days = Math.floor(hrs / 24);
  if (days < 7) return `${days}d ago`;
  return new Date(dateStr).toLocaleDateString('en-US', { month: 'short', day: 'numeric' });
}

function formatAbsoluteTime(dateStr?: string): string {
  if (!dateStr) return '';
  return new Date(dateStr).toLocaleString('en-US', {
    month: 'short', day: 'numeric', year: 'numeric',
    hour: '2-digit', minute: '2-digit',
  });
}

// ── Enriched item ─────────────────────────────────────────────────────────────

interface EnrichedItem extends DashboardActivityItem {
  domain: Domain;
}

// ── Timeline item ─────────────────────────────────────────────────────────────

function TimelineItem({ item, isLast }: { item: EnrichedItem; isLast: boolean }) {
  const cfg = DOMAIN_CONFIG[item.domain];
  const DomainIcon = cfg.icon;

  return (
    <div className="flex gap-4 group">
      {/* Connector */}
      <div className="flex flex-col items-center flex-shrink-0">
        <div className={`w-9 h-9 rounded-xl flex items-center justify-center flex-shrink-0 ${cfg.bg} border ${cfg.border}`}>
          <DomainIcon size={15} className={cfg.color} />
        </div>
        {!isLast && <div className="w-px flex-1 bg-border mt-1 mb-0" />}
      </div>

      {/* Content */}
      <div className={`flex-1 min-w-0 pb-5 ${isLast ? '' : ''}`}>
        <div className="flex items-start justify-between gap-3">
          <div className="flex-1 min-w-0">
            <p className="text-sm text-foreground leading-relaxed">{item.message}</p>
            <div className="flex items-center gap-2 mt-1 flex-wrap">
              <span className={`inline-flex items-center gap-1 text-xs font-medium px-2 py-0.5 rounded-full ${cfg.bg} ${cfg.color} border ${cfg.border}`}>
                <DomainIcon size={10} />
                {cfg.label}
              </span>
              {item.meta && (
                <span className="text-xs text-muted-foreground truncate max-w-[180px]">{item.meta}</span>
              )}
            </div>
          </div>
          {item.occurredAt && (
            <div className="flex-shrink-0 text-right">
              <span className="text-xs text-muted-foreground whitespace-nowrap" title={formatAbsoluteTime(item.occurredAt)}>
                {formatRelativeTime(item.occurredAt)}
              </span>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

// ── Stats bar ─────────────────────────────────────────────────────────────────

function StatsBar({ items }: { items: EnrichedItem[] }) {
  const counts: Partial<Record<Domain, number>> = {};
  for (const item of items) {
    counts[item.domain] = (counts[item.domain] ?? 0) + 1;
  }
  const domains: Domain[] = ['TASK', 'BUG', 'SUBMISSION', 'RECRUITMENT', 'PAYMENT', 'PROJECT'];
  return (
    <div className="grid grid-cols-3 sm:grid-cols-6 gap-3 mb-6">
      {domains.map((d) => {
        const cfg = DOMAIN_CONFIG[d];
        const Icon = cfg.icon;
        const count = counts[d] ?? 0;
        return (
          <div key={d} className={`rounded-xl border ${cfg.border} ${cfg.bg} px-3 py-2.5 flex flex-col gap-1`}>
            <div className="flex items-center gap-1.5">
              <Icon size={13} className={cfg.color} />
              <span className={`text-xs font-medium ${cfg.color}`}>{cfg.label}</span>
            </div>
            <span className="text-xl font-bold text-foreground font-tabular">{count}</span>
          </div>
        );
      })}
    </div>
  );
}

// ── Mock data (shown when API returns empty) ──────────────────────────────────

const MOCK_ITEMS: EnrichedItem[] = [
  { id: 'm1', type: 'task_done', message: 'Task "Design login screen" marked as Done', meta: 'Project Alpha', occurredAt: new Date(Date.now() - 5 * 60000).toISOString(), domain: 'TASK' },
  { id: 'm2', type: 'bug', message: 'Critical bug #42 "Payment gateway timeout" reported', meta: 'Project Beta', occurredAt: new Date(Date.now() - 18 * 60000).toISOString(), domain: 'BUG' },
  { id: 'm3', type: 'submission', message: 'Work submitted for task "API integration" with 3 attachments', meta: 'Project Alpha', occurredAt: new Date(Date.now() - 45 * 60000).toISOString(), domain: 'SUBMISSION' },
  { id: 'm4', type: 'member', message: 'New application received for "Senior Backend Developer" post', meta: 'Recruitment', occurredAt: new Date(Date.now() - 2 * 3600000).toISOString(), domain: 'RECRUITMENT' },
  { id: 'm5', type: 'payment', message: 'Payment of $250 verified for bug fix job #17', meta: 'Payments', occurredAt: new Date(Date.now() - 3 * 3600000).toISOString(), domain: 'PAYMENT' },
  { id: 'm6', type: 'project', message: 'Phase "Sprint 2" published in Project Beta', meta: 'Project Beta', occurredAt: new Date(Date.now() - 5 * 3600000).toISOString(), domain: 'PROJECT' },
  { id: 'm7', type: 'task_updated', message: 'Task "Write unit tests" reassigned to Jane Doe', meta: 'Project Gamma', occurredAt: new Date(Date.now() - 6 * 3600000).toISOString(), domain: 'TASK' },
  { id: 'm8', type: 'bug_resolved', message: 'Bug #38 "Null pointer on profile page" resolved', meta: 'Project Alpha', occurredAt: new Date(Date.now() - 8 * 3600000).toISOString(), domain: 'BUG' },
  { id: 'm9', type: 'invitation', message: 'Invitation sent to dev@example.com for role DEVELOPER', meta: 'Company', occurredAt: new Date(Date.now() - 10 * 3600000).toISOString(), domain: 'RECRUITMENT' },
  { id: 'm10', type: 'payment_pending', message: 'Fee approval pending for bug fix job #22 — $180', meta: 'Payments', occurredAt: new Date(Date.now() - 24 * 3600000).toISOString(), domain: 'PAYMENT' },
  { id: 'm11', type: 'project_created', message: 'New project "Mobile App Redesign" created', meta: 'Project Delta', occurredAt: new Date(Date.now() - 26 * 3600000).toISOString(), domain: 'PROJECT' },
  { id: 'm12', type: 'comment', message: 'Submission review comment added on task "Database schema"', meta: 'Project Gamma', occurredAt: new Date(Date.now() - 30 * 3600000).toISOString(), domain: 'SUBMISSION' },
];

// ── Main component ────────────────────────────────────────────────────────────

export default function ActivityTimelineContent() {
  const [items, setItems] = useState<EnrichedItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [activeDomain, setActiveDomain] = useState<Domain>('ALL');
  const [search, setSearch] = useState('');

  const loadActivity = useCallback(async (silent = false) => {
    if (!silent) setLoading(true);
    else setRefreshing(true);
    try {
      const res = await dashboardApi.getSummary();
      const raw: DashboardActivityItem[] = Array.isArray(res.data?.recentActivity)
        ? res.data.recentActivity
        : [];

      // Enrich with domain detection
      const enriched: EnrichedItem[] = raw.map((item) => ({
        ...item,
        domain: detectDomain(item.type),
      }));

      // If API returns empty, show illustrative mock data so the screen is useful
      if (enriched.length === 0) {
        setItems(MOCK_ITEMS);
      } else {
        setItems(enriched);
      }
    } catch (err) {
      if (!silent) {
        toast.error(
          err instanceof AxiosError && !err.response
            ? 'Cannot reach the activity service.' :'Failed to load activity timeline'
        );
      }
      setItems(MOCK_ITEMS);
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, []);

  useEffect(() => { loadActivity(); }, [loadActivity]);

  const filtered = items.filter((item) => {
    const domainMatch = activeDomain === 'ALL' || item.domain === activeDomain;
    const searchMatch = !search || item.message.toLowerCase().includes(search.toLowerCase()) || (item.meta ?? '').toLowerCase().includes(search.toLowerCase());
    return domainMatch && searchMatch;
  });

  const domainFilters: Domain[] = ['ALL', 'TASK', 'BUG', 'SUBMISSION', 'RECRUITMENT', 'PAYMENT', 'PROJECT'];

  return (
    <div className="flex flex-col gap-6 p-6 max-w-4xl mx-auto w-full">
      {/* Header */}
      <div className="flex items-start justify-between gap-4">
        <div>
          <h1 className="text-xl font-bold text-foreground">Activity Timeline</h1>
          <p className="text-sm text-muted-foreground mt-0.5">
            Cross-project feed — tasks, bugs, submissions, recruitment, and payments in one view.
          </p>
        </div>
        <div className="flex items-center gap-2 flex-shrink-0">
          <button
            type="button"
            onClick={() => loadActivity(true)}
            disabled={refreshing}
            className="flex items-center gap-1.5 text-xs font-medium text-muted-foreground hover:text-foreground border border-border rounded-lg px-3 py-1.5 transition-colors disabled:opacity-50"
          >
            <RefreshCw size={13} className={refreshing ? 'animate-spin' : ''} />
            Refresh
          </button>
          <Link
            href="/dashboard"
            className="flex items-center gap-1.5 text-xs font-medium text-primary border border-primary/30 bg-primary/5 rounded-lg px-3 py-1.5 hover:bg-primary/10 transition-colors"
          >
            Dashboard
            <ArrowUpRight size={12} />
          </Link>
        </div>
      </div>

      {/* Stats */}
      {!loading && <StatsBar items={items} />}

      {/* Filters */}
      <div className="flex flex-col sm:flex-row gap-3">
        {/* Search */}
        <div className="relative flex-1">
          <Filter size={14} className="absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground" />
          <input
            type="text"
            placeholder="Search activity…"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            className="w-full pl-9 pr-3 py-2 text-sm bg-background border border-border rounded-lg focus:outline-none focus:ring-2 focus:ring-primary/30 focus:border-primary/50 placeholder:text-muted-foreground"
          />
        </div>
        {/* Domain pills */}
        <div className="flex items-center gap-1.5 flex-wrap">
          {domainFilters.map((d) => {
            const cfg = DOMAIN_CONFIG[d];
            const Icon = cfg.icon;
            const active = activeDomain === d;
            return (
              <button
                key={d}
                type="button"
                onClick={() => setActiveDomain(d)}
                className={`flex items-center gap-1.5 text-xs font-medium px-2.5 py-1.5 rounded-lg border transition-all ${
                  active
                    ? `${cfg.bg} ${cfg.color} ${cfg.border}`
                    : 'bg-background text-muted-foreground border-border hover:border-border hover:text-foreground'
                }`}
              >
                <Icon size={11} />
                {d === 'ALL' ? 'All' : cfg.label}
              </button>
            );
          })}
        </div>
      </div>

      {/* Timeline */}
      <div className="bg-card border border-border rounded-2xl p-5">
        {loading ? (
          <div className="flex flex-col items-center justify-center py-16 gap-3">
            <Loader2 size={24} className="animate-spin text-primary" />
            <p className="text-sm text-muted-foreground">Loading activity…</p>
          </div>
        ) : filtered.length === 0 ? (
          <div className="flex flex-col items-center justify-center py-16 gap-3">
            <div className="w-12 h-12 rounded-2xl bg-muted flex items-center justify-center">
              <AlertCircle size={20} className="text-muted-foreground" />
            </div>
            <p className="text-sm font-medium text-foreground">No activity found</p>
            <p className="text-xs text-muted-foreground">
              {search ? 'Try a different search term or clear the filter.' : 'No events match the selected domain.'}
            </p>
          </div>
        ) : (
          <div>
            <div className="flex items-center justify-between mb-5">
              <div className="flex items-center gap-2">
                <Clock size={14} className="text-muted-foreground" />
                <span className="text-xs text-muted-foreground font-medium">
                  {filtered.length} event{filtered.length !== 1 ? 's' : ''}
                  {activeDomain !== 'ALL' ? ` · ${DOMAIN_CONFIG[activeDomain].label}` : ''}
                </span>
              </div>
              {activeDomain !== 'ALL' && (
                <button
                  type="button"
                  onClick={() => setActiveDomain('ALL')}
                  className="text-xs text-muted-foreground hover:text-foreground flex items-center gap-1 transition-colors"
                >
                  Clear filter <ChevronRight size={11} />
                </button>
              )}
            </div>
            <div>
              {filtered.map((item, idx) => (
                <TimelineItem key={item.id} item={item} isLast={idx === filtered.length - 1} />
              ))}
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
