'use client';

import { useCallback, useEffect, useState } from 'react';
import { AxiosError } from 'axios';
import { CreditCard, RefreshCw } from 'lucide-react';
import { adminManagementApi } from '@/lib/api/admin-management';
import type { AdminSubscription } from '@/lib/api/types';

interface SubscriptionPage {
  content: AdminSubscription[];
  page: number;
  totalPages: number;
  totalElements: number;
}

function getError(error: unknown): string {
  if (error instanceof AxiosError) {
    return (error.response?.data as { message?: string } | undefined)?.message ?? error.message;
  }
  return 'Subscription update failed.';
}

export default function AdminSubscriptionsPage() {
  const [result, setResult] = useState<SubscriptionPage | null>(null);
  const [page, setPage] = useState(0);
  const [drafts, setDrafts] = useState<Record<string, AdminSubscription>>({});
  const [busyId, setBusyId] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);

  const refresh = useCallback(async () => {
    setError(null);
    try {
      const next = await adminManagementApi.listSubscriptions(page);
      setResult(next);
      setDrafts(
        Object.fromEntries(next.content.map((subscription) => [subscription.userId, subscription]))
      );
    } catch (loadError) {
      setError(getError(loadError));
    }
  }, [page]);

  useEffect(() => {
    void refresh();
  }, [refresh]);

  const updateDraft = (userId: string, update: Partial<AdminSubscription>) => {
    setDrafts((current) => ({ ...current, [userId]: { ...current[userId], ...update } }));
  };

  const save = async (subscription: AdminSubscription) => {
    setBusyId(subscription.userId);
    setError(null);
    setNotice(null);
    try {
      await adminManagementApi.updateSubscription(subscription.userId, {
        plan: subscription.plan,
        maxCompanies: subscription.maxCompanies,
        entitled: subscription.entitled,
      });
      setNotice(`Subscription updated for owner ${subscription.userId}.`);
      await refresh();
    } catch (saveError) {
      setError(getError(saveError));
    } finally {
      setBusyId(null);
    }
  };

  return (
    <div className="space-y-6">
      <header className="flex items-end justify-between gap-4">
        <div>
          <p className="text-sm font-medium text-primary">Billing controls</p>
          <h1 className="mt-1 text-3xl font-bold text-foreground">Subscriptions</h1>
          <p className="mt-2 text-sm text-muted-foreground">
            Manage the current owner-account subscription records.
          </p>
        </div>
        <button
          type="button"
          onClick={() => void refresh()}
          aria-label="Refresh subscriptions"
          className="rounded-lg border border-border p-2"
        >
          <RefreshCw size={16} />
        </button>
      </header>
      <p className="rounded-xl border border-amber-500/30 bg-amber-500/5 p-4 text-sm text-muted-foreground">
        Workora currently stores subscriptions by owner user ID, not by company. This screen does
        not change payment-provider charges or billing history.
      </p>
      {error && (
        <p role="alert" className="rounded-xl border border-destructive/30 p-4 text-sm">
          {error}
        </p>
      )}
      {notice && (
        <p role="status" className="rounded-xl border border-emerald-500/30 p-4 text-sm">
          {notice}
        </p>
      )}
      <section className="space-y-3">
        {result?.content.map((subscription) => {
          const draft = drafts[subscription.userId] ?? subscription;
          return (
            <article
              key={subscription.userId}
              className="grid gap-4 rounded-xl border border-border bg-card p-4 md:grid-cols-[minmax(0,1fr)_150px_150px_auto_auto] md:items-end"
            >
              <div className="min-w-0">
                <span className="inline-flex items-center gap-2 text-sm font-semibold">
                  <CreditCard size={16} /> Owner subscription
                </span>
                <p className="mt-2 break-all text-xs text-muted-foreground">
                  {subscription.userId}
                </p>
              </div>
              <label className="text-xs font-medium text-muted-foreground">
                Plan
                <select
                  value={draft.plan}
                  onChange={(event) =>
                    updateDraft(subscription.userId, {
                      plan: event.target.value as AdminSubscription['plan'],
                    })
                  }
                  className="mt-1 w-full rounded-lg border border-border bg-background px-2 py-2 text-sm text-foreground"
                >
                  <option value="FREE">Free</option>
                  <option value="PRO">Pro</option>
                  <option value="ENTERPRISE">Enterprise</option>
                </select>
              </label>
              <label className="text-xs font-medium text-muted-foreground">
                Max companies
                <input
                  type="number"
                  min={1}
                  max={10000}
                  value={draft.maxCompanies}
                  onChange={(event) =>
                    updateDraft(subscription.userId, { maxCompanies: Number(event.target.value) })
                  }
                  className="mt-1 w-full rounded-lg border border-border bg-background px-2 py-2 text-sm text-foreground"
                />
              </label>
              <label className="flex items-center gap-2 pb-2 text-sm">
                <input
                  type="checkbox"
                  checked={draft.entitled}
                  onChange={(event) =>
                    updateDraft(subscription.userId, { entitled: event.target.checked })
                  }
                />
                Entitled
              </label>
              <button
                type="button"
                disabled={busyId === subscription.userId}
                onClick={() => void save(draft)}
                className="rounded-lg bg-primary px-4 py-2 text-sm font-semibold text-primary-foreground disabled:opacity-50"
              >
                Save
              </button>
            </article>
          );
        })}
        {!result?.content.length && (
          <p className="rounded-xl border border-border bg-card p-5 text-sm text-muted-foreground">
            No subscription records found.
          </p>
        )}
      </section>
      <footer className="flex items-center justify-between text-sm text-muted-foreground">
        <span>{result?.totalElements ?? 0} subscriptions</span>
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
            disabled={!result || result.page + 1 >= result.totalPages}
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
