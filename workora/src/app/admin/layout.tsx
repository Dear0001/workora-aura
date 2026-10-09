'use client';

import React, { useEffect, useState } from 'react';
import Link from 'next/link';
import { usePathname, useRouter } from 'next/navigation';
import {
  Activity,
  ArrowLeft,
  Building2,
  CreditCard,
  LayoutDashboard,
  Server,
  ShieldCheck,
  UserCog,
} from 'lucide-react';
import AppLogo from '@/components/ui/AppLogo';
import { getCurrentUser, isAuthenticated, storeUser, type UserProfile } from '@/lib/auth';

type AccessState = 'loading' | 'admin' | 'denied' | 'error';

export default function AdminLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  const [accessState, setAccessState] = useState<AccessState>('loading');
  const [user, setUser] = useState<UserProfile | null>(null);
  const pathname = usePathname();
  const router = useRouter();

  useEffect(() => {
    let active = true;
    if (!isAuthenticated()) {
      router.replace('/');
      return () => {
        active = false;
      };
    }

    getCurrentUser()
      .then((profile) => {
        if (!active) return;
        storeUser(profile);
        setUser(profile);
        setAccessState(profile.role?.toUpperCase() === 'ADMIN' ? 'admin' : 'denied');
      })
      .catch(() => {
        if (active) setAccessState('error');
      });

    return () => {
      active = false;
    };
  }, [router]);

  if (accessState === 'loading') {
    return (
      <main className="min-h-screen bg-background flex items-center justify-center">
        <div className="text-sm text-muted-foreground">Verifying administrator access…</div>
      </main>
    );
  }

  if (accessState === 'denied') {
    return (
      <main className="min-h-screen bg-background flex items-center justify-center p-6">
        <section className="w-full max-w-md rounded-2xl border border-border bg-card p-8 text-center shadow-sm">
          <ShieldCheck className="mx-auto mb-4 text-muted-foreground" size={32} />
          <h1 className="text-xl font-semibold text-foreground">Administrator access required</h1>
          <p className="mt-2 text-sm text-muted-foreground">
            This area is available only to accounts with the global ADMIN role.
          </p>
          <Link
            className="mt-6 inline-flex items-center gap-2 text-sm font-medium text-primary"
            href="/dashboard"
          >
            <ArrowLeft size={16} /> Return to Workora
          </Link>
        </section>
      </main>
    );
  }

  if (accessState === 'error') {
    return (
      <main className="min-h-screen bg-background flex items-center justify-center p-6">
        <section className="w-full max-w-md rounded-2xl border border-border bg-card p-8 text-center shadow-sm">
          <h1 className="text-xl font-semibold text-foreground">
            Could not verify administrator access
          </h1>
          <p className="mt-2 text-sm text-muted-foreground">
            The identity service did not confirm your role. Retry after checking your connection.
          </p>
          <button
            className="mt-6 rounded-lg bg-primary px-4 py-2 text-sm font-medium text-primary-foreground"
            onClick={() => window.location.reload()}
          >
            Retry
          </button>
        </section>
      </main>
    );
  }

  return (
    <div className="min-h-screen bg-background lg:flex">
      <aside className="border-b border-border bg-card lg:fixed lg:inset-y-0 lg:left-0 lg:flex lg:w-64 lg:flex-col lg:border-b-0 lg:border-r">
        <div className="flex h-16 items-center gap-3 border-b border-border px-5">
          <AppLogo size={30} />
          <div>
            <p className="font-bold text-foreground">Workora</p>
            <p className="text-[10px] font-semibold uppercase tracking-widest text-primary">
              Administration
            </p>
          </div>
        </div>
        <nav className="flex gap-2 overflow-x-auto p-3 lg:flex-1 lg:flex-col lg:gap-1 lg:p-4">
          {[
            { href: '/admin', label: 'Overview', icon: LayoutDashboard },
            { href: '/admin/health', label: 'System health', icon: Server },
            { href: '/admin/accounts', label: 'Admin accounts', icon: UserCog },
            { href: '/admin/companies', label: 'Companies', icon: Building2 },
            { href: '/admin/subscriptions', label: 'Subscriptions', icon: CreditCard },
            { href: '/admin/activity', label: 'User activity', icon: Activity },
          ].map(({ href, label, icon: Icon }) => (
            <Link
              key={href}
              href={href}
              aria-current={pathname === href ? 'page' : undefined}
              className={`inline-flex items-center gap-3 rounded-lg px-3 py-2 text-sm font-medium ${
                pathname === href
                  ? 'bg-primary/10 text-primary'
                  : 'text-muted-foreground hover:bg-muted hover:text-foreground'
              }`}
            >
              <Icon size={17} /> {label}
            </Link>
          ))}
          <Link
            href="/dashboard"
            className="inline-flex items-center gap-3 rounded-lg px-3 py-2 text-sm font-medium text-muted-foreground hover:bg-muted hover:text-foreground"
          >
            <ArrowLeft size={17} /> Workora dashboard
          </Link>
        </nav>
        <div className="hidden border-t border-border p-4 text-xs text-muted-foreground lg:block">
          Signed in as <span className="font-medium text-foreground">{user?.email ?? 'Admin'}</span>
        </div>
      </aside>
      <main className="min-w-0 flex-1 p-5 sm:p-8 lg:ml-64">
        <div className="mx-auto max-w-screen-2xl">{children}</div>
      </main>
    </div>
  );
}
