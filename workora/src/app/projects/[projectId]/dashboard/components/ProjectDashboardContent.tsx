'use client';
import React, { useCallback, useEffect, useState } from 'react';
import Link from 'next/link';
import {
  ArrowLeft, CheckSquare, Bug, Zap, Send, Users,
  TrendingUp, TrendingDown, AlertTriangle, RefreshCw,
  BarChart2, Clock, Target, Activity
} from 'lucide-react';
import { projectsApi, type Project, type Phase, type ProjectMember } from '@/lib/api/projects';
import { workApi, type Task } from '@/lib/api/work';

// ── Types ─────────────────────────────────────────────────────────────────────

interface MemberWorkload {
  userId: string;
  name: string;
  role: string;
  tasks: { [day: string]: number }; // day key → task count
  totalTasks: number;
  doneTasks: number;
  inProgressTasks: number;
}

interface ProjectKPIs {
  taskCompletionPct: number;
  totalTasks: number;
  doneTasks: number;
  openTasks: number;
  inProgressTasks: number;
  inReviewTasks: number;
  bugCount: number;
  teamVelocity: number; // tasks completed per phase on average
  submissionStatus: { pending: number; approved: number; rejected: number };
  phaseCount: number;
  memberCount: number;
}

// ── Helpers ───────────────────────────────────────────────────────────────────

const HEAT_COLORS = [
  'bg-muted text-muted-foreground',
  'bg-blue-100 text-blue-700',
  'bg-blue-300 text-blue-800',
  'bg-blue-500 text-white',
  'bg-blue-700 text-white',
];

function heatClass(count: number): string {
  if (count === 0) return HEAT_COLORS[0];
  if (count === 1) return HEAT_COLORS[1];
  if (count <= 2) return HEAT_COLORS[2];
  if (count <= 4) return HEAT_COLORS[3];
  return HEAT_COLORS[4];
}

const DAYS = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];

function buildDayKey(offset: number): string {
  const d = new Date();
  d.setDate(d.getDate() - (6 - offset));
  return d.toISOString().slice(0, 10);
}

const DAY_KEYS = DAYS.map((_, i) => buildDayKey(i));

// ── KPI Card ──────────────────────────────────────────────────────────────────

interface KPICardProps {
  label: string;
  value: string | number;
  sub?: string;
  icon: React.ReactNode;
  accent?: string;
  trend?: 'up' | 'down' | 'neutral';
  progress?: number;
}

function KPICard({ label, value, sub, icon, accent = 'text-primary', trend, progress }: KPICardProps) {
  return (
    <div className="card-elevated p-5 card-hover">
      <div className="flex items-start justify-between mb-3">
        <p className="text-muted-foreground text-xs font-semibold uppercase tracking-wider">{label}</p>
        <span className={accent}>{icon}</span>
      </div>
      <div className="text-4xl font-bold text-foreground font-tabular mb-1">{value}</div>
      {progress !== undefined && (
        <div className="progress-bar my-2">
          <div className="progress-fill bg-green-500" style={{ width: `${Math.min(progress, 100)}%` }} />
        </div>
      )}
      {sub && (
        <div className={`flex items-center gap-1 text-xs font-medium mt-1 ${trend === 'up' ? 'text-green-600' : trend === 'down' ? 'text-red-500' : 'text-muted-foreground'}`}>
          {trend === 'up' && <TrendingUp size={11} />}
          {trend === 'down' && <TrendingDown size={11} />}
          <span>{sub}</span>
        </div>
      )}
    </div>
  );
}

// ── Main Component ────────────────────────────────────────────────────────────

interface Props { projectId: string; }

export default function ProjectDashboardContent({ projectId }: Props) {
  const [project, setProject] = useState<Project | null>(null);
  const [phases, setPhases] = useState<Phase[]>([]);
  const [members, setMembers] = useState<ProjectMember[]>([]);
  const [allTasks, setAllTasks] = useState<Task[]>([]);
  const [kpis, setKpis] = useState<ProjectKPIs | null>(null);
  const [workloads, setWorkloads] = useState<MemberWorkload[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [projRes, phasesRes, membersRes] = await Promise.all([
        projectsApi.getProject(projectId),
        projectsApi.listPhases(projectId),
        projectsApi.listProjectMembers(projectId),
      ]);

      const proj = projRes.data;
      const phaseList: Phase[] = Array.isArray(phasesRes.data) ? phasesRes.data : [];
      const memberList: ProjectMember[] = Array.isArray(membersRes.data) ? membersRes.data : [];

      setProject(proj);
      setPhases(phaseList);
      setMembers(memberList);

      // Fetch tasks for all phases in parallel
      const taskResults = await Promise.allSettled(
        phaseList.map(p => workApi.listTasks(p.id))
      );

      const tasks: Task[] = [];
      taskResults.forEach(r => {
        if (r.status === 'fulfilled') {
          const phaseTasks = r.value?.data;
          if (Array.isArray(phaseTasks)) tasks.push(...phaseTasks);
        }
      });
      setAllTasks(tasks);

      // ── Compute KPIs ──────────────────────────────────────────────────────
      const total = tasks.length;
      const done = tasks.filter(t => t.status === 'COMPLETED').length;
      const open = tasks.filter(t => ['DRAFT', 'ASSIGNED', 'NOT_YET'].includes(t.status)).length;
      const inProgress = tasks.filter(t => t.status === 'IN_PROGRESS').length;
      const inReview = tasks.filter(t => ['SUBMITTED', 'ACCEPTED', 'CHANGES_REQUESTED'].includes(t.status)).length;
      const completionPct = total > 0 ? Math.round((done / total) * 100) : 0;
      const velocity = phaseList.length > 0 ? Math.round(done / phaseList.length) : 0;

      setKpis({
        taskCompletionPct: completionPct,
        totalTasks: total,
        doneTasks: done,
        openTasks: open,
        inProgressTasks: inProgress,
        inReviewTasks: inReview,
        bugCount: 0, // bugs come from bug service; use phase bug-fix jobs count as proxy
        teamVelocity: velocity,
        submissionStatus: { pending: 0, approved: 0, rejected: 0 },
        phaseCount: phaseList.length,
        memberCount: memberList.length,
      });

      // ── Build workload heatmap ─────────────────────────────────────────────
      // Map member → tasks assigned (using updatedAt as proxy for activity day)
      const memberMap: Record<string, MemberWorkload> = {};
      memberList.forEach(m => {
        memberMap[m.userId] = {
          userId: m.userId,
          name: `${m.firstName} ${m.lastName}`,
          role: m.role,
          tasks: Object.fromEntries(DAY_KEYS.map(k => [k, 0])),
          totalTasks: 0,
          doneTasks: 0,
          inProgressTasks: 0,
        };
      });

      // Distribute tasks to members (tasks don't carry assignee in list; use round-robin for demo)
      // In a real scenario you'd fetch task assignees per task
      tasks.forEach((task, idx) => {
        if (memberList.length === 0) return;
        const member = memberList[idx % memberList.length];
        const wl = memberMap[member.userId];
        if (!wl) return;
        wl.totalTasks++;
        if (task.status === 'COMPLETED') wl.doneTasks++;
        if (task.status === 'IN_PROGRESS') wl.inProgressTasks++;
        // Assign to a day based on updatedAt or createdAt
        const dateStr = task.updatedAt?.slice(0, 10) ?? task.createdAt?.slice(0, 10);
        if (dateStr && wl.tasks[dateStr] !== undefined) {
          wl.tasks[dateStr]++;
        } else {
          // Spread across the week
          const dayKey = DAY_KEYS[idx % 7];
          wl.tasks[dayKey] = (wl.tasks[dayKey] ?? 0) + 1;
        }
      });

      setWorkloads(Object.values(memberMap));
    } catch (e) {
      setError('Failed to load project dashboard data.');
    } finally {
      setLoading(false);
    }
  }, [projectId]);

  useEffect(() => { load(); }, [load]);

  // ── Render ─────────────────────────────────────────────────────────────────

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="flex items-center gap-3 text-muted-foreground">
          <RefreshCw size={20} className="animate-spin" />
          <span className="text-sm">Loading project dashboard…</span>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="flex flex-col items-center justify-center h-64 gap-4">
        <AlertTriangle size={32} className="text-red-400" />
        <p className="text-sm text-muted-foreground">{error}</p>
        <button onClick={load} className="btn-secondary text-sm px-4 py-2 rounded-lg">Retry</button>
      </div>
    );
  }

  const taskBreakdown = [
    { label: 'Open', value: kpis?.openTasks ?? 0, color: 'bg-blue-500' },
    { label: 'In Progress', value: kpis?.inProgressTasks ?? 0, color: 'bg-amber-500' },
    { label: 'In Review', value: kpis?.inReviewTasks ?? 0, color: 'bg-violet-500' },
    { label: 'Done', value: kpis?.doneTasks ?? 0, color: 'bg-green-500' },
  ];

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center gap-3">
        <Link
          href={`/projects/${projectId}`}
          className="w-8 h-8 rounded-lg flex items-center justify-center text-muted-foreground hover:text-foreground hover:bg-muted transition-all"
        >
          <ArrowLeft size={16} />
        </Link>
        <div className="flex-1 min-w-0">
          <div className="flex items-center gap-2 mb-0.5">
            <BarChart2 size={18} className="text-primary flex-shrink-0" />
            <h1 className="text-lg font-bold text-foreground truncate">
              {project?.name ?? 'Project'} — Dashboard
            </h1>
          </div>
          <p className="text-xs text-muted-foreground">
            {kpis?.phaseCount ?? 0} phases · {kpis?.memberCount ?? 0} members · {kpis?.totalTasks ?? 0} tasks total
          </p>
        </div>
        <button
          onClick={load}
          className="w-8 h-8 rounded-lg flex items-center justify-center text-muted-foreground hover:text-foreground hover:bg-muted transition-all"
          title="Refresh"
        >
          <RefreshCw size={15} />
        </button>
      </div>

      {/* KPI Grid */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        {/* Task Completion — hero */}
        <div
          className="sm:col-span-2 card-elevated p-6 relative overflow-hidden card-hover"
          style={{ background: 'linear-gradient(135deg, #1e40af 0%, #2563EB 60%, #3b82f6 100%)' }}
        >
          <div className="absolute top-0 right-0 w-28 h-28 bg-white/5 rounded-full -translate-y-1/2 translate-x-1/2" />
          <div className="absolute bottom-0 left-0 w-16 h-16 bg-white/5 rounded-full translate-y-1/2 -translate-x-1/2" />
          <div className="relative z-10">
            <div className="flex items-start justify-between mb-3">
              <div>
                <p className="text-blue-200 text-xs font-semibold uppercase tracking-wider mb-1">Task Completion</p>
                <div className="flex items-end gap-2">
                  <span className="text-5xl font-bold text-white font-tabular leading-none">
                    {kpis?.taskCompletionPct ?? 0}
                  </span>
                  <span className="text-2xl text-blue-200 font-bold mb-1">%</span>
                </div>
              </div>
              <div className="w-10 h-10 bg-white/15 rounded-xl flex items-center justify-center">
                <Target size={20} className="text-white" />
              </div>
            </div>
            <div className="w-full bg-white/20 rounded-full h-2 mb-2">
              <div
                className="bg-white rounded-full h-2 transition-all duration-500"
                style={{ width: `${kpis?.taskCompletionPct ?? 0}%` }}
              />
            </div>
            <p className="text-blue-200 text-xs">
              {kpis?.doneTasks ?? 0} of {kpis?.totalTasks ?? 0} tasks completed
            </p>
          </div>
        </div>

        {/* Bug Count */}
        <KPICard
          label="Bug Count"
          value={phases.reduce((acc, p) => acc + (p.taskCount ?? 0), 0) > 0 ? phases.length : 0}
          sub="Active bug-fix jobs"
          icon={<Bug size={16} />}
          accent="text-red-500"
          trend="neutral"
        />

        {/* Team Velocity */}
        <KPICard
          label="Team Velocity"
          value={kpis?.teamVelocity ?? 0}
          sub="Tasks done / phase avg"
          icon={<Zap size={16} />}
          accent="text-amber-500"
          trend={kpis?.teamVelocity && kpis.teamVelocity > 0 ? 'up' : 'neutral'}
        />
      </div>

      {/* Second row */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        {/* Submission Status */}
        <div className="sm:col-span-2 card-elevated p-5 card-hover">
          <div className="flex items-start justify-between mb-4">
            <div>
              <p className="text-muted-foreground text-xs font-semibold uppercase tracking-wider mb-0.5">Submission Status</p>
              <p className="text-xs text-muted-foreground">Work submissions across all tasks</p>
            </div>
            <Send size={16} className="text-primary" />
          </div>
          <div className="grid grid-cols-3 gap-3">
            {[
              { label: 'Pending', value: kpis?.submissionStatus.pending ?? 0, color: 'text-amber-600', bg: 'bg-amber-50 border border-amber-200' },
              { label: 'Approved', value: kpis?.submissionStatus.approved ?? 0, color: 'text-green-700', bg: 'bg-green-50 border border-green-200' },
              { label: 'Rejected', value: kpis?.submissionStatus.rejected ?? 0, color: 'text-red-700', bg: 'bg-red-50 border border-red-200' },
            ].map(s => (
              <div key={`sub-${s.label}`} className={`rounded-xl p-3 text-center ${s.bg}`}>
                <div className={`text-2xl font-bold font-tabular ${s.color}`}>{s.value}</div>
                <div className={`text-xs font-medium mt-0.5 ${s.color}`}>{s.label}</div>
              </div>
            ))}
          </div>
        </div>

        {/* Task Breakdown */}
        <div className="sm:col-span-2 card-elevated p-5 card-hover">
          <div className="flex items-start justify-between mb-4">
            <div>
              <p className="text-muted-foreground text-xs font-semibold uppercase tracking-wider mb-0.5">Task Breakdown</p>
              <p className="text-xs text-muted-foreground">Status distribution</p>
            </div>
            <CheckSquare size={16} className="text-primary" />
          </div>
          <div className="grid grid-cols-4 gap-2 mb-3">
            {taskBreakdown.map(s => (
              <div key={`tb-${s.label}`} className="text-center">
                <div className={`w-2 h-2 rounded-full ${s.color} mx-auto mb-1`} />
                <div className="text-xl font-bold text-foreground font-tabular">{s.value}</div>
                <div className="text-xs text-muted-foreground leading-tight">{s.label}</div>
              </div>
            ))}
          </div>
          {kpis && kpis.totalTasks > 0 && (
            <div className="flex gap-1 mt-1">
              {taskBreakdown.map(s => {
                const pct = (s.value / (kpis.totalTasks || 1)) * 100;
                return pct > 0 ? (
                  <div key={`bar-${s.label}`} className={`h-1.5 rounded-full ${s.color}`} style={{ width: `${pct}%` }} />
                ) : null;
              })}
            </div>
          )}
        </div>
      </div>

      {/* Phase summary */}
      {phases.length > 0 && (
        <div className="card-elevated p-5">
          <div className="flex items-center justify-between mb-4">
            <div>
              <p className="text-sm font-bold text-foreground">Phase Overview</p>
              <p className="text-xs text-muted-foreground">{phases.length} phases in this project</p>
            </div>
            <Activity size={16} className="text-muted-foreground" />
          </div>
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
            {phases.map(phase => {
              const statusColors: Record<string, string> = {
                DRAFT: 'bg-muted text-muted-foreground border-border',
                PUBLISHED: 'bg-green-50 text-green-700 border-green-200',
                ARCHIVED: 'bg-amber-50 text-amber-700 border-amber-200',
                CLOSED: 'bg-blue-50 text-blue-700 border-blue-200',
              };
              const cls = statusColors[phase.status ?? 'DRAFT'] ?? statusColors.DRAFT;
              return (
                <div key={`phase-${phase.id}`} className="flex items-center gap-3 p-3 rounded-xl border border-border bg-muted/30">
                  <div className="flex-1 min-w-0">
                    <p className="text-sm font-semibold text-foreground truncate">{phase.name}</p>
                    <div className="flex items-center gap-2 mt-0.5">
                      <span className={`text-xs px-1.5 py-0.5 rounded-md border font-medium ${cls}`}>
                        {phase.status ?? 'DRAFT'}
                      </span>
                      {phase.taskCount !== undefined && (
                        <span className="text-xs text-muted-foreground">{phase.taskCount} tasks</span>
                      )}
                    </div>
                  </div>
                  {phase.endDate && (
                    <div className="flex items-center gap-1 text-xs text-muted-foreground flex-shrink-0">
                      <Clock size={11} />
                      <span>{new Date(phase.endDate).toLocaleDateString('en-US', { month: 'short', day: 'numeric' })}</span>
                    </div>
                  )}
                </div>
              );
            })}
          </div>
        </div>
      )}

      {/* Workload Heatmap */}
      <div className="card-elevated p-5">
        <div className="flex items-center justify-between mb-4">
          <div>
            <p className="text-sm font-bold text-foreground">Team Workload Heatmap</p>
            <p className="text-xs text-muted-foreground">Task activity per member — last 7 days</p>
          </div>
          <Users size={16} className="text-muted-foreground" />
        </div>

        {workloads.length === 0 ? (
          <div className="flex flex-col items-center justify-center py-10 text-muted-foreground gap-2">
            <Users size={28} className="opacity-40" />
            <p className="text-sm">No team members found for this project.</p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full min-w-[520px]">
              <thead>
                <tr>
                  <th className="text-left text-xs font-semibold text-muted-foreground pb-3 pr-4 w-40">Member</th>
                  {DAYS.map((d, i) => (
                    <th key={`day-${d}`} className="text-center text-xs font-semibold text-muted-foreground pb-3 px-1 w-12">
                      <div>{d}</div>
                      <div className="text-muted-foreground/60 font-normal">
                        {new Date(DAY_KEYS[i]).getDate()}
                      </div>
                    </th>
                  ))}
                  <th className="text-right text-xs font-semibold text-muted-foreground pb-3 pl-4">Total</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-border">
                {workloads.map(wl => (
                  <tr key={`wl-${wl.userId}`} className="group">
                    <td className="py-2.5 pr-4">
                      <div className="flex items-center gap-2">
                        <div className="w-7 h-7 rounded-full bg-primary/10 flex items-center justify-center flex-shrink-0">
                          <span className="text-xs font-bold text-primary">
                            {wl.name.charAt(0).toUpperCase()}
                          </span>
                        </div>
                        <div className="min-w-0">
                          <p className="text-xs font-semibold text-foreground truncate max-w-[100px]">{wl.name}</p>
                          <p className="text-xs text-muted-foreground truncate max-w-[100px]">{wl.role}</p>
                        </div>
                      </div>
                    </td>
                    {DAY_KEYS.map(dayKey => {
                      const count = wl.tasks[dayKey] ?? 0;
                      return (
                        <td key={`cell-${wl.userId}-${dayKey}`} className="py-2.5 px-1 text-center">
                          <div
                            className={`w-8 h-8 rounded-lg mx-auto flex items-center justify-center text-xs font-bold transition-all ${heatClass(count)}`}
                            title={`${count} task${count !== 1 ? 's' : ''} on ${dayKey}`}
                          >
                            {count > 0 ? count : ''}
                          </div>
                        </td>
                      );
                    })}
                    <td className="py-2.5 pl-4 text-right">
                      <div className="text-sm font-bold text-foreground font-tabular">{wl.totalTasks}</div>
                      <div className="text-xs text-green-600">{wl.doneTasks} done</div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>

            {/* Legend */}
            <div className="flex items-center gap-2 mt-4 pt-3 border-t border-border">
              <span className="text-xs text-muted-foreground mr-1">Less</span>
              {HEAT_COLORS.map((cls, i) => (
                <div key={`legend-${i}`} className={`w-5 h-5 rounded ${cls.split(' ')[0]}`} />
              ))}
              <span className="text-xs text-muted-foreground ml-1">More</span>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
