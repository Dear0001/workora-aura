import React from 'react';
import { CheckCircle2, Bug, UserPlus, FolderPlus, AlertTriangle, MessageSquare } from 'lucide-react';
import type { DashboardActivityItem } from '@/lib/api/types';
import Icon from '@/components/ui/AppIcon';


const ICONS = { task_done: CheckCircle2, bug: Bug, member: UserPlus, project: FolderPlus, warn: AlertTriangle, comment: MessageSquare };

export default function DashboardActivityFeed({ items }: { items: DashboardActivityItem[] }) {
  return (
    <div>
      <div className="flex items-center justify-between mb-4">
        <h3 className="text-base font-semibold text-foreground">Recent Activity</h3>
      </div>
      {items.length === 0 ? (
        <p className="text-sm text-muted-foreground">No recent activity available.</p>
      ) : (
        <div className="space-y-3 overflow-y-auto max-h-[260px] scrollbar-thin pr-1">
          {items.map((item) => {
            const Icon = ICONS[item.type as keyof typeof ICONS] ?? MessageSquare;
            return (
              <div key={item.id} className="flex gap-3 group">
                <div className="w-7 h-7 rounded-lg bg-muted flex items-center justify-center flex-shrink-0 mt-0.5">
                  <Icon size={13} className="text-muted-foreground" />
                </div>
                <div className="flex-1 min-w-0">
                  <p className="text-xs text-foreground leading-relaxed">{item.message}</p>
                  <div className="flex items-center gap-2 mt-0.5">
                    <span className="text-xs text-muted-foreground">{item.meta ?? 'Dashboard'}</span>
                    {item.occurredAt && <><span className="text-xs text-muted-foreground">·</span><span className="text-xs text-muted-foreground">{new Date(item.occurredAt).toLocaleString()}</span></>}
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}
