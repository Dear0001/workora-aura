'use client';
import React, { useEffect, useState } from 'react';
import Link from 'next/link';
import { usePathname } from 'next/navigation';
import AppLogo from '@/components/ui/AppLogo';
import {
  LayoutDashboard,
  Building2,
  FolderKanban,
  CheckSquare,
  Bug,
  Users,
  Bell,
  CreditCard,
  ChevronRight,
  LogOut,
  User,
  Activity,
  UsersRound,
  ClipboardCheck,
  UserCircle,
  ShieldCheck,
} from 'lucide-react';
import { getStoredUser, logout } from '@/lib/auth';
import { useRouter } from 'next/navigation';
import { notificationsApi } from '@/lib/api/notifications';
import { toast } from 'sonner';

interface Props {
  collapsed: boolean;
  mobileOpen: boolean;
  onMobileClose: () => void;
}

const NAV_SECTIONS = [
  {
    label: 'Overview',
    items: [
      { href: '/dashboard', icon: LayoutDashboard, label: 'Dashboard', badge: null },
      { href: '/activity-timeline', icon: Activity, label: 'Activity Timeline', badge: null },
    ],
  },
  {
    label: 'Workspace',
    items: [
      { href: '/company-management', icon: Building2, label: 'Company', badge: null },
      { href: '/projects', icon: FolderKanban, label: 'Projects', badge: null },
      { href: '/tasks', icon: CheckSquare, label: 'Tasks', badge: null },
      { href: '/colleague-profiles', icon: UsersRound, label: 'Colleagues', badge: null },
    ],
  },
  {
    label: 'Operations',
    items: [
      { href: '/bugs', icon: Bug, label: 'Bug Triage', badge: null },
      { href: '/recruitment', icon: Users, label: 'Recruitment', badge: null },
      { href: '/recruitment/manager-decisions', icon: ClipboardCheck, label: 'Manager Decisions', badge: null },
      { href: '/payments', icon: CreditCard, label: 'Payments', badge: null },
    ],
  },
  {
    label: 'System',
    items: [
      { href: '/admin', icon: ShieldCheck, label: 'Admin overview', badge: null },
      { href: '/notifications', icon: Bell, label: 'Notifications', badge: null },
      { href: '/profile', icon: UserCircle, label: 'My Profile', badge: null },
    ],
  },
];

export default function Sidebar({ collapsed, mobileOpen, onMobileClose }: Props) {
  const pathname = usePathname();
  const router = useRouter();
  const [user, setUser] = useState<ReturnType<typeof getStoredUser>>(null);
  const [unreadCount, setUnreadCount] = useState(0);

  useEffect(() => {
    setUser(getStoredUser());
    notificationsApi
      .getUnreadCount()
      .then((res) => {
        setUnreadCount(res.data?.unread ?? 0);
      })
      .catch(() => toast.error('Failed to load unread notification count'));
  }, []);

  const isActive = (href: string) => {
    if (href === '/dashboard') return pathname === '/dashboard';
    return pathname.startsWith(href);
  };

  const sidebarClasses = `
    fixed top-0 left-0 h-full z-40 flex flex-col bg-card border-r border-border sidebar-transition
    ${collapsed ? 'w-16' : 'w-60'}
    ${mobileOpen ? 'translate-x-0' : '-translate-x-full lg:translate-x-0'}
  `;

  return (
    <aside className={sidebarClasses}>
      {/* Logo area */}
      <div className={`flex items-center h-16 border-b border-border flex-shrink-0 ${collapsed ? 'justify-center px-0' : 'px-4 gap-3'}`}>
        <AppLogo size={32} />
        {!collapsed && (
          <span className="font-bold text-base text-foreground tracking-tight">Workora</span>
        )}
      </div>

      {/* Nav */}
      <nav className="flex-1 overflow-y-auto scrollbar-thin py-3 px-2">
        {NAV_SECTIONS.map((section) => (
          <div key={`section-${section.label}`} className="mb-4">
            {!collapsed && (
              <p className="text-xs font-semibold text-muted-foreground uppercase tracking-wider px-2 mb-1.5">
                {section.label}
              </p>
            )}
            {section.items
              .filter((item) => item.href !== '/admin' || user?.role?.toUpperCase() === 'ADMIN')
              .map((item) => {
                const active = isActive(item.href);
                const isNotifications = item.href === '/notifications';
                const badge = isNotifications && unreadCount > 0 ? String(unreadCount) : item.badge;
                return (
                  <Link
                    key={`nav-${item.href}`}
                    href={item.href}
                    onClick={onMobileClose}
                    title={collapsed ? item.label : undefined}
                    className={`
                    flex items-center gap-3 px-2 py-2 rounded-lg mb-0.5 text-sm font-medium transition-all duration-150
                    ${active ? 'nav-item-active' : 'text-muted-foreground hover:text-foreground nav-item-hover'}
                    ${collapsed ? 'justify-center' : ''}
                  `}
                  >
                    <div className="relative flex-shrink-0">
                      <item.icon size={18} />
                      {collapsed && badge && (
                        <span className="absolute -top-1 -right-1 w-4 h-4 bg-primary text-primary-foreground text-xs font-bold rounded-full flex items-center justify-center font-tabular">
                          {Number(badge) > 9 ? '9+' : badge}
                        </span>
                      )}
                    </div>
                    {!collapsed && (
                      <>
                        <span className="flex-1 truncate">{item.label}</span>
                        {badge && (
                          <span className="bg-primary/10 text-primary text-xs font-semibold px-1.5 py-0.5 rounded-full font-tabular">
                            {badge}
                          </span>
                        )}
                      </>
                    )}
                  </Link>
                );
              })}
          </div>
        ))}
      </nav>

      {/* Bottom user area */}
      <div className="border-t border-border p-2 flex-shrink-0">
        <Link
          href="/profile"
          onClick={onMobileClose}
          className={`flex items-center gap-3 p-2 rounded-lg hover:bg-muted transition-colors ${collapsed ? 'justify-center' : ''}`}
        >
          <div className="w-7 h-7 rounded-full bg-primary/10 flex items-center justify-center flex-shrink-0">
            <User size={14} className="text-primary" />
          </div>
          {!collapsed && (
            <>
              <div className="flex-1 min-w-0">
                <p className="text-xs font-semibold text-foreground truncate">
                  {user ? `${user.firstName} ${user.lastName}` : 'Account'}
                </p>
                <p className="text-xs text-muted-foreground truncate">{user?.email ?? 'Not signed in'}</p>
              </div>
              <ChevronRight size={14} className="text-muted-foreground flex-shrink-0" />
            </>
          )}
        </Link>
        <button
          type="button"
          onClick={async () => {
            await logout();
            router.push('/');
          }}
          className={`flex items-center gap-3 px-2 py-2 rounded-lg text-sm text-muted-foreground hover:text-red-500 hover:bg-red-50 transition-all duration-150 mt-1 ${collapsed ? 'justify-center' : ''}`}
          title={collapsed ? 'Sign Out' : undefined}
        >
          <LogOut size={16} className="flex-shrink-0" />
          {!collapsed && <span>Sign Out</span>}
        </button>
      </div>
    </aside>
  );
}