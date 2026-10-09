'use client';

import React, { useCallback, useEffect, useState } from 'react';
import {
  Activity,
  Bell,
  Bug,
  Building2,
  CreditCard,
  FolderKanban,
  RefreshCw,
  Users,
  BriefcaseBusiness,
} from 'lucide-react';
import { AxiosError } from 'axios';
import { adminDashboardApi } from '@/lib/api/admin-dashboard';
import type { AdminDashboardSummary } from '@/lib/api/types';

const METRICS = [
  {
    key: 'accounts',
    label: 'Accounts',
    note: 'Identity profiles',
    icon: Users,
    color: 'text-blue-600 bg-blue-500/10',
  },
  {
    key: 'companies',
    label: 'Companies',
    note: 'Organization records',
    icon: Building2,
    color: 'text-violet-600 bg-violet-500/10',
  },
  {
    key: 'projects',
    label: 'Projects',
    note: 'Across all companies',
    icon: FolderKanban,
    color: 'text-indigo-600 bg-indigo-500/10',
  },
  {
    key: 'tasks',
    label: 'Tasks',
    note: 'Work records',
    icon: Activity,
    color: 'text-cyan-600 bg-cyan-500/10',
  },
  {
    key: 'bugReports',
    label: 'Bug reports',
    note: 'All reported bugs',
    icon: Bug,
    color: 'text-rose-600 bg-rose-500/10',
  },
  {
    key: 'recruitmentPosts',
    label: 'Recruitment posts',
    note: 'All hiring posts',
    icon: BriefcaseBusiness,
    color: 'text-amber-600 bg-amber-500/10',
  },
  {
    key: 'notifications',
    label: 'Notifications',
    note: 'Notification records',
    icon: Bell,
    color: 'text-emerald-600 bg-emerald-500/10',
  },
  {
    key: 'payments',
    label: 'Payments',
    note: 'Payment records',
    icon: CreditCard,
    color: 'text-orange-600 bg-orange-500/10',
  },
] as const satisfies readonly {
  key: keyof AdminDashboardSummary;
  label: string;
  note: string;
  icon: typeof Users;
  color: string;
}[];

export default function AdminDashboardPage() {
  const [summary, setSummary] = useState<AdminDashboardSummary | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  const loadSummary = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await adminDashboardApi.getSummary();
      setSummary(response.data);
    } catch (loadError) {
      setError(
        loadError instanceof AxiosError && loadError.response?.status === 403
          ? 'Your account has the identity ADMIN role, but this access token is missing the Keycloak ADMIN realm role. Assign the realm role, sign in again, and retry.'
          : 'Unable to load system metrics. Check that the gateway and required services are running, then retry.'
      );
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void loadSummary();
  }, [loadSummary]);

  return (
    <div className="space-y-8">
      <header className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <p className="text-sm font-medium text-primary">System administration</p>
          <h1 className="mt-1 text-3xl font-bold tracking-tight text-foreground">Overview</h1>
          <p className="mt-2 text-sm text-muted-foreground">
            Read-only aggregate metrics from Workora services.
          </p>
        </div>
        <button
          type="button"
          onClick={() => void loadSummary()}
          disabled={loading}
          className="inline-flex items-center justify-center gap-2 self-start rounded-lg border border-border bg-card px-3 py-2 text-sm font-medium text-foreground hover:bg-muted disabled:cursor-not-allowed disabled:opacity-60 sm:self-auto"
        >
          <RefreshCw size={15} className={loading ? 'animate-spin' : ''} />
          Refresh metrics
        </button>
      </header>

      {error && (
        <div
          role="alert"
          className="flex flex-col gap-3 rounded-xl border border-destructive/30 bg-destructive/5 p-4 sm:flex-row sm:items-center sm:justify-between"
        >
          <p className="text-sm text-foreground">{error}</p>
          <button
            type="button"
            onClick={() => void loadSummary()}
            className="text-sm font-semibold text-primary"
          >
            Try again
          </button>
        </div>
      )}

      <section aria-label="System metrics" className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        {METRICS.map(({ key, label, note, icon: Icon, color }) => (
          <article key={key} className="rounded-2xl border border-border bg-card p-5 shadow-sm">
            <div className="flex items-start justify-between">
              <div>
                <p className="text-sm font-medium text-muted-foreground">{label}</p>
                <p className="mt-3 text-3xl font-bold tabular-nums text-foreground">
                  {loading && !summary ? (
                    <span className="inline-block h-9 w-20 animate-pulse rounded bg-muted" />
                  ) : (
                    (summary?.[key] ?? '—').toLocaleString()
                  )}
                </p>
              </div>
              <span className={`rounded-xl p-3 ${color}`}>
                <Icon size={20} />
              </span>
            </div>
            <p className="mt-4 text-xs text-muted-foreground">{note}</p>
          </article>
        ))}
      </section>
    </div>
  );
}
