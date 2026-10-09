'use client';

import { useCallback, useEffect, useState } from 'react';
import { AxiosError } from 'axios';
import { Check, RefreshCw, UserPlus, X } from 'lucide-react';
import { adminManagementApi } from '@/lib/api/admin-management';
import type { AdminAccessRequest } from '@/lib/api/types';

function errorMessage(error: unknown): string {
  if (error instanceof AxiosError) {
    const data = error.response?.data as { message?: string } | undefined;
    return data?.message ?? error.message;
  }
  return 'Administrator account request failed.';
}

export default function AdminAccountsPage() {
  const [requests, setRequests] = useState<AdminAccessRequest[]>([]);
  const [email, setEmail] = useState('');
  const [firstName, setFirstName] = useState('');
  const [lastName, setLastName] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [busyId, setBusyId] = useState<string | null>(null);

  const refresh = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      setRequests(await adminManagementApi.listAccountRequests());
    } catch (loadError) {
      setError(errorMessage(loadError));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void refresh();
  }, [refresh]);

  const createRequest = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setError(null);
    setNotice(null);
    try {
      await adminManagementApi.requestAdminAccess({
        email: email.trim(),
        firstName: firstName.trim() || undefined,
        lastName: lastName.trim() || undefined,
      });
      setEmail('');
      setFirstName('');
      setLastName('');
      setNotice(
        'Account invitation/request created. A different existing admin must approve it before ADMIN access is granted.'
      );
      await refresh();
    } catch (requestError) {
      setError(errorMessage(requestError));
    }
  };

  const decide = async (request: AdminAccessRequest, decision: 'approve' | 'reject') => {
    setBusyId(request.id);
    setError(null);
    try {
      await adminManagementApi.decideAccountRequest(request.id, decision);
      await refresh();
    } catch (decisionError) {
      setError(errorMessage(decisionError));
    } finally {
      setBusyId(null);
    }
  };

  return (
    <div className="space-y-6">
      <header className="flex items-end justify-between gap-4">
        <div>
          <p className="text-sm font-medium text-primary">Access control</p>
          <h1 className="mt-1 text-3xl font-bold text-foreground">Admin accounts</h1>
          <p className="mt-2 text-sm text-muted-foreground">
            Invite an email or nominate an existing account. New invitees start as regular users; a
            different existing admin must approve before the Keycloak ADMIN role is granted.
          </p>
        </div>
        <button
          onClick={() => void refresh()}
          aria-label="Refresh requests"
          className="rounded-lg border border-border p-2"
        >
          <RefreshCw size={16} />
        </button>
      </header>

      <form
        onSubmit={createRequest}
        className="grid gap-3 rounded-2xl border border-border bg-card p-5 sm:grid-cols-2 xl:grid-cols-4"
      >
        <label>
          <span className="mb-1 block text-sm font-medium">Account email</span>
          <input
            type="email"
            required
            value={email}
            onChange={(event) => setEmail(event.target.value)}
            className="w-full rounded-lg border border-border bg-background px-3 py-2 text-sm"
            placeholder="person@example.com"
          />
        </label>
        <label>
          <span className="mb-1 block text-sm font-medium">First name (for new account)</span>
          <input
            value={firstName}
            onChange={(event) => setFirstName(event.target.value)}
            maxLength={100}
            className="w-full rounded-lg border border-border bg-background px-3 py-2 text-sm"
          />
        </label>
        <label>
          <span className="mb-1 block text-sm font-medium">Last name (for new account)</span>
          <input
            value={lastName}
            onChange={(event) => setLastName(event.target.value)}
            maxLength={100}
            className="w-full rounded-lg border border-border bg-background px-3 py-2 text-sm"
          />
        </label>
        <button
          type="submit"
          className="mt-auto inline-flex items-center justify-center gap-2 rounded-lg bg-primary px-4 py-2 text-sm font-semibold text-primary-foreground"
        >
          <UserPlus size={16} /> Request access
        </button>
      </form>

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
      <section className="space-y-3" aria-label="Pending admin approvals">
        <h2 className="text-lg font-semibold">Pending approvals</h2>
        {loading ? (
          <p className="text-sm text-muted-foreground">Loading…</p>
        ) : requests.length === 0 ? (
          <p className="rounded-xl border border-border bg-card p-5 text-sm text-muted-foreground">
            No pending approval requests.
          </p>
        ) : (
          requests.map((request) => (
            <article
              key={request.id}
              className="flex flex-col gap-3 rounded-xl border border-border bg-card p-4 sm:flex-row sm:items-center sm:justify-between"
            >
              <div>
                <p className="font-medium text-foreground">{request.email}</p>
                <p className="mt-1 text-xs text-muted-foreground">
                  Requested by {request.requestedBy} ·{' '}
                  {new Date(request.createdAt).toLocaleString()}
                </p>
              </div>
              <div className="flex gap-2">
                <button
                  disabled={busyId === request.id}
                  onClick={() => void decide(request, 'approve')}
                  className="inline-flex items-center gap-1 rounded-lg bg-primary px-3 py-2 text-sm text-primary-foreground disabled:opacity-50"
                >
                  <Check size={15} /> Approve
                </button>
                <button
                  disabled={busyId === request.id}
                  onClick={() => void decide(request, 'reject')}
                  className="inline-flex items-center gap-1 rounded-lg border border-border px-3 py-2 text-sm disabled:opacity-50"
                >
                  <X size={15} /> Reject
                </button>
              </div>
            </article>
          ))
        )}
      </section>
    </div>
  );
}
