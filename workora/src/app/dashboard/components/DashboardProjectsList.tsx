import React from 'react';
import { ArrowRight, Clock, Users, CheckCircle2 } from 'lucide-react';
import Link from 'next/link';
import type { DashboardProject } from '@/lib/api/types';

const STATUS_BADGE: Record<string, string> = { ACTIVE: 'bg-green-50 text-green-700 border border-green-200', ON_HOLD: 'bg-amber-50 text-amber-700 border border-amber-200', COMPLETED: 'bg-blue-50 text-blue-700 border border-blue-200' };
const PRIORITY_BADGE: Record<string, string> = { HIGH: 'bg-red-50 text-red-600', MEDIUM: 'bg-amber-50 text-amber-600', LOW: 'bg-muted text-muted-foreground' };

export default function DashboardProjectsList({ projects }: { projects: DashboardProject[] }) {
  return (
    <div className="card-elevated p-5">
      <div className="flex items-center justify-between mb-5">
        <div><h3 className="text-base font-semibold text-foreground">Active Projects</h3><p className="text-xs text-muted-foreground mt-0.5">Phase progress across your workspace</p></div>
        <Link href="/projects" className="flex items-center gap-1 text-xs text-primary font-medium hover:underline">View all projects<ArrowRight size={12} /></Link>
      </div>
      {projects.length === 0 ? <p className="text-sm text-muted-foreground">No active projects available.</p> : (
        <div className="space-y-4">{projects.map(project => (
          <div key={project.id} className="flex flex-col sm:flex-row sm:items-center gap-3 p-3 rounded-lg hover:bg-muted/50 transition-colors group">
            <div className="flex-1 min-w-0"><div className="flex items-center gap-2 flex-wrap"><span className="font-semibold text-sm text-foreground">{project.name}</span><span className={`badge-base ${STATUS_BADGE[project.status] ?? ''}`}>{project.status.replace('_', ' ')}</span>{project.priority && <span className={`badge-base ${PRIORITY_BADGE[project.priority] ?? ''}`}>{project.priority}</span>}</div><div className="flex items-center gap-3 mt-1 text-xs text-muted-foreground">{project.dueDate && <span className="flex items-center gap-1"><Clock size={10} />Due {project.dueDate}</span>}</div></div>
            <div className="sm:w-48 flex-shrink-0"><div className="flex items-center justify-between text-xs mb-1"><span className="text-muted-foreground">Phase progress</span><span className="font-semibold text-foreground font-tabular">{project.phaseCompletion}%</span></div><div className="progress-bar"><div className="progress-fill bg-primary" style={{ width: `${project.phaseCompletion}%` }} /></div></div>
            <div className="flex items-center gap-4 text-xs text-muted-foreground flex-shrink-0"><span className="flex items-center gap-1"><Users size={11} />{project.members}</span><span className="flex items-center gap-1"><CheckCircle2 size={11} className="text-green-500" />{project.tasksDone}</span><span className="text-amber-600 font-medium">{project.tasksOpen} open</span></div>
          </div>
        ))}</div>
      )}
    </div>
  );
}
