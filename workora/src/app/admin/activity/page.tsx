'use client';

import { useCallback, useEffect, useState } from 'react';
import { AxiosError } from 'axios';
import { Activity, Search } from 'lucide-react';
import { adminManagementApi } from '@/lib/api/admin-management';
import type { AdminMembershipActivity } from '@/lib/api/types';

interface ActivityPage {
  content: AdminMembershipActivity[];
  page: number;
  totalPages: number;
  totalElements: number;
}

export default function AdminActivityPage() {
  const [events, setEvents] = useState<ActivityPage | null>(null);
  const [filters, setFilters] = useState({
    companyId: '',
    userId: '',
    action: '',
    from: '',
    to: '',
  });
  const [page, setPage] = useState(0);
  const [error, setError] = useState<string | null>(null);

  const search = useCallback(async () => {
    setError(null);
    try {
      setEvents(
        await adminManagementApi.listActivity(
          {
            ...filters,
            from: filters.from ? new Date(filters.from).toISOString() : undefined,
            to: filters.to ? new Date(filters.to).toISOString() : undefined,
          },
          page
        )
      );
    } catch (loadError) {
      const response =
        loadError instanceof AxiosError
          ? (loadError.response?.data as { message?: string } | undefined)
          : undefined;
      setError(response?.message ?? 'Unable to load organization activity records.');
    }
  }, [filters, page]);

  useEffect(() => {
    void search();
  }, [search]);

  return (
    <div className="space-y-6">
      <header>
        <p className="text-sm font-medium text-primary">Audit review</p>
        <h1 className="mt-1 text-3xl font-bold text-foreground">User activity</h1>
        <p className="mt-2 text-sm text-muted-foreground">
          Search recorded company-membership changes by company, user, action, and time.
        </p>
      </header>
      <p className="rounded-xl border border-amber-500/30 bg-amber-500/5 p-4 text-sm text-muted-foreground">
        Coverage is limited to organization membership audit events currently recorded by Workora.
        It is not a complete feed of every user action across services.
      </p>
      <section className="grid gap-3 rounded-2xl border border-border bg-card p-4 sm:grid-cols-2 xl:grid-cols-5">
        {(
          [
            ['companyId', 'Company ID'],
            ['userId', 'User ID'],
            ['action', 'Action'],
            ['from', 'From'],
            ['to', 'To'],
          ] as const
        ).map(([key, label]) => (
          <label key={key} className="text-xs font-medium text-muted-foreground">
            {label}
            <input
              type={key === 'from' || key === 'to' ? 'datetime-local' : 'text'}
              value={filters[key]}
              onChange={(event) => {
                setPage(0);
                setFilters((current) => ({ ...current, [key]: event.target.value }));
              }}
              className="mt-1 w-full rounded-lg border border-border bg-background px-3 py-2 text-sm text-foreground"
            />
          </label>
        ))}
        <button
          type="button"
          onClick={() => void search()}
          className="inline-flex items-center justify-center gap-2 self-end rounded-lg bg-primary px-3 py-2 text-sm font-semibold text-primary-foreground"
        >
          <Search size={15} /> Apply filters
        </button>
      </section>
      {error && (
        <p role="alert" className="rounded-xl border border-destructive/30 p-4 text-sm">
          {error}
        </p>
      )}
      <div className="overflow-x-auto rounded-xl border border-border bg-card">
        <table className="w-full min-w-[800px] text-left text-sm">
          <thead className="border-b border-border text-xs text-muted-foreground">
            <tr>
              {['Time', 'Action', 'Company', 'User', 'Actor', 'Details'].map((heading) => (
                <th key={heading} className="px-4 py-3 font-medium">
                  {heading}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {events?.content.map((event) => (
              <tr key={event.id} className="border-b border-border last:border-0">
                <td className="px-4 py-3 whitespace-nowrap text-xs">
                  {new Date(event.createdAt).toLocaleString()}
                </td>
                <td className="px-4 py-3 font-medium">
                  <Activity className="mr-1 inline" size={14} />
                  {event.action}
                </td>
                <td className="px-4 py-3 font-mono text-xs">{event.companyId}</td>
                <td className="px-4 py-3 font-mono text-xs">{event.userId}</td>
                <td className="px-4 py-3 font-mono text-xs">{event.actorUserId}</td>
                <td className="px-4 py-3 text-xs text-muted-foreground">{event.details ?? '—'}</td>
              </tr>
            ))}
          </tbody>
        </table>
        {!events?.content.length && (
          <p className="p-5 text-sm text-muted-foreground">No matching membership events.</p>
        )}
      </div>
      <footer className="flex items-center justify-between text-sm text-muted-foreground">
        <span>{events?.totalElements ?? 0} matching events</span>
        <div className="flex gap-2">
          <button
            type="button"
            disabled={page === 0}
            onClick={() => setPage((current) => current - 1)}
            className="rounded-lg border border-border px-3 py-1.5 disabled:opacity-50"
          >
            Previous
          </button>
          <button
            type="button"
            disabled={!events || events.page + 1 >= events.totalPages}
            onClick={() => setPage((current) => current + 1)}
            className="rounded-lg border border-border px-3 py-1.5 disabled:opacity-50"
          >
            Next
          </button>
        </div>
      </footer>
    </div>
  );
}
