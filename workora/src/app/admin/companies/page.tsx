'use client';

import { useCallback, useEffect, useState } from 'react';
import { AxiosError } from 'axios';
import { Building2, RefreshCw } from 'lucide-react';
import { adminManagementApi } from '@/lib/api/admin-management';
import type { AdminCompany } from '@/lib/api/types';

interface CompanyPage {
  content: AdminCompany[];
  page: number;
  totalPages: number;
  totalElements: number;
}

function getError(error: unknown): string {
  if (error instanceof AxiosError) {
    return (error.response?.data as { message?: string } | undefined)?.message ?? error.message;
  }
  return 'Company access operation failed.';
}

export default function AdminCompaniesPage() {
  const [result, setResult] = useState<CompanyPage | null>(null);
  const [page, setPage] = useState(0);
  const [busyId, setBusyId] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  const refresh = useCallback(async () => {
    setError(null);
    try {
      setResult(await adminManagementApi.listCompanies(page));
    } catch (loadError) {
      setError(getError(loadError));
    }
  }, [page]);

  useEffect(() => {
    void refresh();
  }, [refresh]);

  const changeAccess = async (company: AdminCompany) => {
    const status = company.status === 'SUSPENDED' ? 'ACTIVE' : 'SUSPENDED';
    setBusyId(company.id);
    setError(null);
    try {
      await adminManagementApi.setCompanyStatus(company.id, status);
      await refresh();
    } catch (changeError) {
      setError(getError(changeError));
    } finally {
      setBusyId(null);
    }
  };

  return (
    <div className="space-y-6">
      <header className="flex items-end justify-between gap-4">
        <div>
          <p className="text-sm font-medium text-primary">Platform access</p>
          <h1 className="mt-1 text-3xl font-bold text-foreground">Companies</h1>
          <p className="mt-2 text-sm text-muted-foreground">
            Suspend organization-service access or reactivate it.
          </p>
        </div>
        <button
          type="button"
          onClick={() => void refresh()}
          aria-label="Refresh companies"
          className="rounded-lg border border-border p-2"
        >
          <RefreshCw size={16} />
        </button>
      </header>
      <p className="rounded-xl border border-amber-500/30 bg-amber-500/5 p-4 text-sm text-muted-foreground">
        Suspension blocks organization-service company membership operations. Domain-service APIs
        that do not consult Organization are not automatically disabled.
      </p>
      {error && (
        <p role="alert" className="rounded-xl border border-destructive/30 p-4 text-sm">
          {error}
        </p>
      )}
      <div className="space-y-3">
        {result?.content.map((company) => (
          <article
            key={company.id}
            className="flex flex-col gap-4 rounded-xl border border-border bg-card p-4 sm:flex-row sm:items-center sm:justify-between"
          >
            <div className="flex items-start gap-3">
              <span className="rounded-xl bg-primary/10 p-3 text-primary">
                <Building2 size={18} />
              </span>
              <div>
                <h2 className="font-semibold text-foreground">{company.name}</h2>
                <p className="mt-1 text-xs text-muted-foreground">
                  Company {company.id} · owner {company.ownerUserId}
                </p>
                <p
                  className={`mt-2 text-xs font-semibold ${company.status === 'ACTIVE' ? 'text-emerald-600' : 'text-amber-600'}`}
                >
                  {company.status}
                </p>
              </div>
            </div>
            <button
              type="button"
              disabled={company.status === 'ARCHIVED' || busyId === company.id}
              onClick={() => void changeAccess(company)}
              className="rounded-lg border border-border px-3 py-2 text-sm font-medium disabled:cursor-not-allowed disabled:opacity-50"
            >
              {company.status === 'SUSPENDED' ? 'Reactivate' : 'Suspend'}
            </button>
          </article>
        ))}
        {!result?.content.length && (
          <p className="rounded-xl border border-border bg-card p-5 text-sm text-muted-foreground">
            No companies found.
          </p>
        )}
      </div>
      <footer className="flex items-center justify-between text-sm text-muted-foreground">
        <span>{result?.totalElements ?? 0} companies</span>
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
