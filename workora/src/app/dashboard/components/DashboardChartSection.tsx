'use client';
import React from 'react';
import dynamic from 'next/dynamic';

const TaskTrendChart = dynamic(() => import('./TaskTrendChart'), { ssr: false });
import DashboardActivityFeed from './DashboardActivityFeed';
import type { DashboardSummary } from '@/lib/api/types';

export default function DashboardChartSection({ summary }: { summary: DashboardSummary | null }) {
  return (
    <div className="grid grid-cols-1 lg:grid-cols-5 xl:grid-cols-5 2xl:grid-cols-5 gap-4">
      {/* Chart — 3 cols */}
      <div className="lg:col-span-3 card-elevated p-5">
        <div className="flex items-center justify-between mb-5">
          <div>
            <h3 className="text-base font-semibold text-foreground">Task Activity</h3>
            <p className="text-xs text-muted-foreground mt-0.5">
              Completed vs created — last 14 days
            </p>
          </div>
          <div className="flex items-center gap-3 text-xs text-muted-foreground">
            <span className="flex items-center gap-1.5">
              <span className="w-2.5 h-2.5 rounded-full bg-primary inline-block" />
              Completed
            </span>
            <span className="flex items-center gap-1.5">
              <span className="w-2.5 h-2.5 rounded-full bg-violet-400 inline-block" />
              Created
            </span>
          </div>
        </div>
        <TaskTrendChart data={summary?.taskActivity ?? []} />
      </div>

      {/* Activity feed — 2 cols */}
      <div className="lg:col-span-2 card-elevated p-5">
        <DashboardActivityFeed items={summary?.recentActivity ?? []} />
      </div>
    </div>
  );
}
