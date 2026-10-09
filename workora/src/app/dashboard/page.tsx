'use client';
import React, { useEffect, useState } from 'react';
import AppLayout from '@/components/AppLayout';
import DashboardKPIGrid from './components/DashboardKPIGrid';
import DashboardChartSection from './components/DashboardChartSection';
import DashboardProjectsList from './components/DashboardProjectsList';

import DashboardQuickActions from './components/DashboardQuickActions';
import { getStoredUser } from '@/lib/auth';
import { dashboardApi } from '@/lib/api/dashboard';
import type { DashboardSummary } from '@/lib/api/types';
import { AxiosError } from 'axios';
import { toast } from 'sonner';

export default function DashboardPage() {
  const [user, setUser] = useState<ReturnType<typeof getStoredUser>>(null);
  const [summary, setSummary] = useState<DashboardSummary | null>(null);

  useEffect(() => {
    setUser(getStoredUser());

    let active = true;
    let hasShownError = false;
    const loadSummary = async () => {
      try {
        const response = await dashboardApi.getSummary();
        if (active) setSummary(response.data);
      } catch (error) {
        if (!hasShownError) {
          toast.error(error instanceof AxiosError && !error.response
            ? 'Cannot reach the dashboard service.' :'Failed to load dashboard data.');
          hasShownError = true;
        }
      }
    };

    loadSummary();
    const intervalId = window.setInterval(loadSummary, 30000);
    window.addEventListener('focus', loadSummary);

    return () => {
      active = false;
      window.clearInterval(intervalId);
      window.removeEventListener('focus', loadSummary);
    };
  }, []);

  const firstName = user?.firstName ?? 'there';

  return (
    <AppLayout>
      <div className="space-y-6">
        {/* Page header */}
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-2xl font-bold text-foreground">Good morning, {firstName}</h1>
            <p className="text-muted-foreground text-sm mt-0.5">
              Here&apos;s what needs your attention today
            </p>
          </div>
          <DashboardQuickActions />
        </div>

        {/* KPI bento grid */}
        <DashboardKPIGrid summary={summary} />

        {/* Chart + Activity feed */}
        <DashboardChartSection summary={summary} />

        {/* Projects list */}
        <DashboardProjectsList projects={summary?.activeProjectsList ?? []} />
      </div>
    </AppLayout>
  );
}