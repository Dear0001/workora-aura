'use client';

import { useCallback, useEffect, useState } from 'react';
import { AxiosError } from 'axios';
import { ExternalLink, RefreshCw, Server } from 'lucide-react';
import { adminManagementApi } from '@/lib/api/admin-management';
import type { AdminServiceHealth } from '@/lib/api/types';

function grafanaDashboardUrl(): string | null {
  const configuredUrl = process.env.NEXT_PUBLIC_GRAFANA_URL;
  if (!configuredUrl) return null;
  try {
    const parsed = new URL(configuredUrl);
    return parsed.protocol === 'https:' || parsed.protocol === 'http:' ? parsed.toString() : null;
  } catch {
    return null;
  }
}

export default function AdminHealthPage() {
  const [services, setServices] = useState<AdminServiceHealth[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const grafanaUrl = grafanaDashboardUrl();

  const refresh = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      setServices(await adminManagementApi.getHealth());
    } catch (loadError) {
      setError(
        loadError instanceof AxiosError && loadError.response?.status === 403
          ? 'Administrator access is required to view health information.'
          : 'Could not retrieve service health. Check dashboard and service connectivity, then retry.'
      );
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void refresh();
  }, [refresh]);

  return (
    <div className="space-y-6">
      <header className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <p className="text-sm font-medium text-primary">Operations</p>
          <h1 className="mt-1 text-3xl font-bold text-foreground">System health</h1>
          <p className="mt-2 text-sm text-muted-foreground">
            Live readiness checks include each service&apos;s database health indicator.
          </p>
        </div>
        <div className="flex gap-2">
          {grafanaUrl && (
            <a
              href={grafanaUrl}
              target="_blank"
              rel="noopener noreferrer"
              className="inline-flex items-center gap-2 rounded-lg border border-border bg-card px-3 py-2 text-sm font-medium text-foreground"
            >
              Grafana <ExternalLink size={15} />
            </a>
          )}
          <button
            type="button"
            onClick={() => void refresh()}
            disabled={loading}
            className="inline-flex items-center gap-2 rounded-lg border border-border bg-card px-3 py-2 text-sm font-medium text-foreground disabled:opacity-60"
          >
            <RefreshCw size={15} className={loading ? 'animate-spin' : ''} /> Refresh
          </button>
        </div>
      </header>

      {!grafanaUrl && (
        <p className="rounded-xl border border-border bg-card p-4 text-sm text-muted-foreground">
          Grafana is not configured. Set <code>NEXT_PUBLIC_GRAFANA_URL</code> to enable a dashboard
          link.
        </p>
      )}
      {error && (
        <p role="alert" className="rounded-xl border border-destructive/30 p-4 text-sm">
          {error}
        </p>
      )}
      {loading && services.length === 0 && (
        <p className="text-sm text-muted-foreground">Checking services…</p>
      )}
      <section aria-label="Service health" className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
        <article className="rounded-2xl border border-border bg-card p-5">
          <div className="flex items-start justify-between">
            <div>
              <h2 className="font-semibold text-foreground">Frontend</h2>
              <p className="mt-2 text-sm font-semibold text-emerald-600">UP</p>
            </div>
            <span className="rounded-xl bg-emerald-500/10 p-3 text-emerald-600">
              <Server size={19} />
            </span>
          </div>
          <p className="mt-3 text-xs text-muted-foreground">
            The administration UI is responding in this browser session.
          </p>
        </article>
        {services.map((service) => {
          const healthy = service.status === 'UP';
          return (
            <article key={service.service} className="rounded-2xl border border-border bg-card p-5">
              <div className="flex items-start justify-between">
                <div>
                  <h2 className="font-semibold capitalize text-foreground">{service.service}</h2>
                  <p
                    className={`mt-2 text-sm font-semibold ${healthy ? 'text-emerald-600' : 'text-destructive'}`}
                  >
                    {service.status}
                  </p>
                </div>
                <span
                  className={`rounded-xl p-3 ${healthy ? 'bg-emerald-500/10 text-emerald-600' : 'bg-destructive/10 text-destructive'}`}
                >
                  <Server size={19} />
                </span>
              </div>
              {Object.keys(service.components).length > 0 && (
                <ul className="mt-4 space-y-2 border-t border-border pt-3 text-xs text-muted-foreground">
                  {Object.entries(service.components).map(([component, status]) => (
                    <li key={component} className="flex justify-between gap-3">
                      <span className="capitalize">{component}</span>
                      <span>{status}</span>
                    </li>
                  ))}
                </ul>
              )}
              <p className="mt-3 text-[11px] text-muted-foreground">
                Checked {new Date(service.checkedAt).toLocaleTimeString()}
              </p>
            </article>
          );
        })}
      </section>
    </div>
  );
}
