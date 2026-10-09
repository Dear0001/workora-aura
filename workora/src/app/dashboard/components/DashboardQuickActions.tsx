'use client';
import React, { useState } from 'react';
import { Plus, ChevronDown, FolderPlus, CheckSquare, Bug, UserPlus } from 'lucide-react';

const QUICK_ACTIONS = [
  { icon: FolderPlus, label: 'New Project', href: '/projects/new' },
  { icon: CheckSquare, label: 'Create Task', href: '/tasks/new' },
  { icon: Bug, label: 'Report Bug', href: '/bugs/new' },
  { icon: UserPlus, label: 'Invite Member', href: '/company-management' },
];

export default function DashboardQuickActions() {
  const [open, setOpen] = useState(false);

  return (
    <div className="relative">
      <button
        onClick={() => setOpen(!open)}
        className="btn-primary flex items-center gap-2 px-4 py-2 rounded-lg text-sm font-semibold"
      >
        <Plus size={16} />
        <span className="hidden sm:inline">Quick Action</span>
        <ChevronDown size={14} className={`transition-transform duration-150 ${open ? 'rotate-180' : ''}`} />
      </button>

      {open && (
        <>
          <div className="fixed inset-0 z-10" onClick={() => setOpen(false)} />
          <div className="absolute right-0 top-full mt-2 w-48 bg-card border border-border rounded-xl shadow-elevated z-20 py-1 fade-scale-in">
            {QUICK_ACTIONS?.map((action) => (
              <a
                key={`qa-${action?.label}`}
                href={action?.href}
                onClick={() => setOpen(false)}
                className="flex items-center gap-3 px-3 py-2.5 text-sm text-foreground hover:bg-muted transition-colors"
              >
                <action.icon size={15} className="text-muted-foreground" />
                {action?.label}
              </a>
            ))}
          </div>
        </>
      )}
    </div>
  );
}