'use client';
import React, { useEffect, useState } from 'react';
import { Menu, Bell, Search, ChevronDown, X } from 'lucide-react';
import Link from 'next/link';
import { getStoredUser, type UserProfile } from '@/lib/auth';

interface Props {
  onToggleSidebar: () => void;
  onMobileMenuOpen: () => void;
  sidebarCollapsed: boolean;
}

export default function Topbar({ onToggleSidebar, onMobileMenuOpen, sidebarCollapsed }: Props) {
  const [searchOpen, setSearchOpen] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  const [user, setUser] = useState<UserProfile | null>(null);

  useEffect(() => {
    setUser(getStoredUser());
  }, []);

  const initials = user
    ? `${user.firstName.charAt(0)}${user.lastName.charAt(0)}`.toUpperCase()
    : '?';

  return (
    <header className="h-16 bg-card border-b border-border flex items-center gap-4 px-4 lg:px-6 flex-shrink-0 sticky top-0 z-20">
      {/* Sidebar toggle — desktop */}
      <button
        onClick={onToggleSidebar}
        className="hidden lg:flex items-center justify-center w-8 h-8 rounded-lg hover:bg-muted text-muted-foreground hover:text-foreground transition-all duration-150"
        aria-label="Toggle sidebar"
      >
        <Menu size={18} />
      </button>

      {/* Mobile menu */}
      <button
        onClick={onMobileMenuOpen}
        className="flex lg:hidden items-center justify-center w-8 h-8 rounded-lg hover:bg-muted text-muted-foreground hover:text-foreground transition-all duration-150"
        aria-label="Open menu"
      >
        <Menu size={18} />
      </button>

      {/* Search bar */}
      <div className="flex-1 max-w-md">
        {searchOpen ? (
          <div className="relative fade-in">
            <Search size={14} className="absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground" />
            <input
              autoFocus
              type="text"
              value={searchQuery}
              onChange={e => setSearchQuery(e.target.value)}
              placeholder="Search projects, tasks, bugs…"
              className="input-base pl-8 pr-8 text-sm h-9"
            />
            <button
              onClick={() => { setSearchOpen(false); setSearchQuery(''); }}
              className="absolute right-2.5 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground"
            >
              <X size={14} />
            </button>
          </div>
        ) : (
          <button
            onClick={() => setSearchOpen(true)}
            className="flex items-center gap-2 text-sm text-muted-foreground hover:text-foreground bg-muted hover:bg-border/80 px-3 py-1.5 rounded-lg transition-all duration-150 w-full sm:w-auto"
          >
            <Search size={14} />
            <span className="hidden sm:inline">Search…</span>
            <kbd className="hidden sm:inline text-xs bg-card border border-border px-1.5 py-0.5 rounded ml-1">⌘K</kbd>
          </button>
        )}
      </div>

      <div className="flex items-center gap-2 ml-auto">
        {/* Notifications */}
        <Link
          href="/notifications"
          className="relative flex items-center justify-center w-8 h-8 rounded-lg hover:bg-muted text-muted-foreground hover:text-foreground transition-all duration-150"
          aria-label="Notifications"
        >
          <Bell size={18} />
        </Link>

        {/* User avatar */}
        <div className="flex items-center gap-2 pl-2 border-l border-border cursor-pointer group">
          <div className="w-7 h-7 rounded-full bg-primary/10 flex items-center justify-center">
            <span className="text-xs font-bold text-primary">{initials}</span>
          </div>
          <span className="hidden sm:block text-sm font-medium text-foreground">
            {user?.firstName ?? 'Account'}
          </span>
          <ChevronDown size={14} className="text-muted-foreground group-hover:text-foreground transition-colors" />
        </div>
      </div>
    </header>
  );
}