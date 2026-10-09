import React from 'react';
import {
  FolderKanban, CheckSquare, Bug, Users, Bell, TrendingUp,
  TrendingDown, AlertTriangle, ArrowUpRight
} from 'lucide-react';
import type { DashboardSummary } from '@/lib/api/types';

interface Props { summary: DashboardSummary | null; }

export default function DashboardKPIGrid({ summary }: Props) {
  const activeProjects = summary?.activeProjects ?? 0;
  const tasksDueToday = summary?.tasksDueToday ?? 0;
  const openBugs = summary?.openBugReports ?? 0;
  const teamMembers = summary?.teamMembers ?? 0;
  const notifications = summary?.unreadNotifications ?? 0;
  const phaseCompletion = summary?.phaseCompletion ?? 0;
  const status = summary?.taskStatusBreakdown ?? { open: 0, inProgress: 0, inReview: 0, done: 0 };
  return (
    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 xl:grid-cols-4 2xl:grid-cols-4 gap-4">
      {/* Hero card — Active Projects — spans 2 cols */}
      <div className="sm:col-span-2 lg:col-span-2 card-elevated p-6 relative overflow-hidden card-hover"
        style={{ background: 'linear-gradient(135deg, #1e40af 0%, #2563EB 60%, #3b82f6 100%)' }}>
        <div className="absolute top-0 right-0 w-32 h-32 bg-white/5 rounded-full -translate-y-1/2 translate-x-1/2" />
        <div className="absolute bottom-0 left-0 w-20 h-20 bg-white/5 rounded-full translate-y-1/2 -translate-x-1/2" />
        <div className="relative z-10">
          <div className="flex items-start justify-between mb-4">
            <div>
              <p className="text-blue-200 text-xs font-semibold uppercase tracking-wider mb-1">
                Active Projects
              </p>
              <div className="text-5xl font-bold text-white font-tabular leading-none">
                {activeProjects}
              </div>
            </div>
            <div className="w-10 h-10 bg-white/15 rounded-xl flex items-center justify-center">
              <FolderKanban size={20} className="text-white" />
            </div>
          </div>
          <div className="flex items-center gap-2">
            <span className="flex items-center gap-1 text-green-300 text-xs font-semibold">
              <TrendingUp size={12} />
              Live data
            </span>
            <span className="text-blue-200 text-xs">From dashboard service</span>
          </div>
        </div>
      </div>

      {/* Tasks Due Today — warning amber */}
      <div className="card-elevated p-5 card-hover border-amber-200 bg-amber-50 relative overflow-hidden">
        <div className="flex items-start justify-between mb-3">
          <p className="text-amber-700 text-xs font-semibold uppercase tracking-wider">
            Tasks Due Today
          </p>
          <AlertTriangle size={16} className="text-amber-500" />
        </div>
        <div className="text-4xl font-bold text-amber-800 font-tabular mb-2">
          {tasksDueToday}
        </div>
        <p className="text-amber-600 text-xs">Current assigned work</p>
        <div className="mt-2 flex items-center gap-1 text-amber-600 text-xs font-medium">
          <ArrowUpRight size={11} />
          <span>Live data</span>
        </div>
      </div>

      {/* Open Bugs — alert red */}
      <div className="card-elevated p-5 card-hover border-red-200 bg-red-50 relative overflow-hidden">
        <div className="flex items-start justify-between mb-3">
          <p className="text-red-700 text-xs font-semibold uppercase tracking-wider">
            Open Bug Reports
          </p>
          <Bug size={16} className="text-red-500" />
        </div>
        <div className="text-4xl font-bold text-red-800 font-tabular mb-2">
          {openBugs}
        </div>
        <p className="text-red-600 text-xs">Open across your workspace</p>
        <div className="mt-2 flex items-center gap-1 text-red-600 text-xs font-medium">
          <TrendingDown size={11} />
          <span>Live data</span>
        </div>
      </div>

      {/* Team Members */}
      <div className="card-elevated p-5 card-hover">
        <div className="flex items-start justify-between mb-3">
          <p className="text-muted-foreground text-xs font-semibold uppercase tracking-wider">
            Team Members
          </p>
          <Users size={16} className="text-primary" />
        </div>
        <div className="text-4xl font-bold text-foreground font-tabular mb-2">
          {teamMembers}
        </div>
        <p className="text-muted-foreground text-xs">Current organization members</p>
        <div className="mt-2 flex items-center gap-1 text-green-600 text-xs font-medium">
          <TrendingUp size={11} />
          <span>Live data</span>
        </div>
      </div>

      {/* Unread Notifications */}
      <div className="card-elevated p-5 card-hover">
        <div className="flex items-start justify-between mb-3">
          <p className="text-muted-foreground text-xs font-semibold uppercase tracking-wider">
            Unread Notifications
          </p>
          <Bell size={16} className="text-primary" />
        </div>
        <div className="text-4xl font-bold text-foreground font-tabular mb-2">
          {notifications}
        </div>
        <p className="text-muted-foreground text-xs">Unread in your inbox</p>
        <div className="mt-2 text-xs text-muted-foreground">Live data</div>
      </div>

      {/* Phase Completion Rate */}
      <div className="card-elevated p-5 card-hover">
        <div className="flex items-start justify-between mb-3">
          <p className="text-muted-foreground text-xs font-semibold uppercase tracking-wider">
            Phase Completion
          </p>
          <TrendingUp size={16} className="text-green-500" />
        </div>
        <div className="text-4xl font-bold text-foreground font-tabular mb-2">
          {phaseCompletion}<span className="text-2xl text-muted-foreground">%</span>
        </div>
        <div className="progress-bar mb-2">
          <div className="progress-fill bg-green-500" style={{ width: `${phaseCompletion}%` }} />
        </div>
        <div className="flex items-center gap-1 text-green-600 text-xs font-medium">
          <TrendingUp size={11} />
          <span>Live data</span>
        </div>
      </div>

      {/* Last card — spans 2 cols to fill row evenly */}
      <div className="sm:col-span-2 lg:col-span-2 card-elevated p-5 card-hover">
        <div className="flex items-start justify-between mb-3">
          <div>
            <p className="text-muted-foreground text-xs font-semibold uppercase tracking-wider mb-1">Task Status Breakdown</p>
            <p className="text-xs text-muted-foreground">Current sprint across all phases</p>
          </div>
          <CheckSquare size={16} className="text-primary" />
        </div>
        <div className="grid grid-cols-4 gap-3 mt-4">
          {[
            { label: 'Open', value: status.open, color: 'bg-blue-500' },
            { label: 'In Progress', value: status.inProgress, color: 'bg-amber-500' },
            { label: 'In Review', value: status.inReview, color: 'bg-violet-500' },
            { label: 'Done', value: status.done, color: 'bg-green-500' },
          ]?.map((s) => (
            <div key={`status-${s?.label}`} className="text-center">
              <div className={`w-2 h-2 rounded-full ${s?.color} mx-auto mb-1`} />
              <div className="text-xl font-bold text-foreground font-tabular">{s?.value}</div>
              <div className="text-xs text-muted-foreground">{s?.label}</div>
            </div>
          ))}
        </div>
        <div className="mt-3 flex gap-1">
          {[
            { pct: 31, color: 'bg-blue-500' },
            { pct: 18, color: 'bg-amber-500' },
            { pct: 9, color: 'bg-violet-500' },
            { pct: 44, color: 'bg-green-500' },
          ]?.map((s, i) => (
            <div
              key={`bar-${i}`}
              className={`h-1.5 rounded-full ${s?.color}`}
              style={{ width: `${(s?.pct / 102) * 100}%` }}
            />
          ))}
        </div>
      </div>
    </div>
  );
}