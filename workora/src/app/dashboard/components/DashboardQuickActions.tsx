'use client';
import React, { useState } from 'react';
import { Plus, ChevronDown, FolderPlus, CheckSquare, Bug, UserPlus } from 'lucide-react';
import FloatingMenu from '@/components/ui/FloatingMenu';

const QUICK_ACTIONS = [
  { icon: FolderPlus, label: 'New Project', href: '/projects/new' },
  { icon: CheckSquare, label: 'Create Task', href: '/tasks/new' },
  { icon: Bug, label: 'Report Bug', href: '/bugs/new' },
  { icon: UserPlus, label: 'Invite Member', href: '/company-management' },
];

export default function DashboardQuickActions() {
  const [open, setOpen] = useState(false);

  return (
    <FloatingMenu
      open={open}
      onOpenChange={setOpen}
      widthClassName="w-48"
      trigger={({ ref, onClick, ...ariaProps }) => (
        <button
          ref={ref}
          onClick={onClick}
          {...ariaProps}
          type="button"
          aria-label="Quick actions"
          className="btn-primary flex items-center gap-2 px-4 py-2 rounded-lg text-sm font-semibold"
        >
          <Plus size={16} />
          <span className="hidden sm:inline">Quick Action</span>
          <ChevronDown size={14} className={`transition-transform duration-150 ${open ? 'rotate-180' : ''}`} />
        </button>
      )}
    >
      {QUICK_ACTIONS.map((action) => (
        <a
          key={`qa-${action.label}`}
          href={action.href}
          className="flex items-center gap-3 px-3 py-2.5 text-sm text-foreground hover:bg-muted transition-colors"
        >
          <action.icon size={15} className="text-muted-foreground" />
          {action.label}
        </a>
      ))}
    </FloatingMenu>
  );
}