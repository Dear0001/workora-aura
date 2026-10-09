'use client';
import React, { useState, useEffect, useCallback, useMemo } from 'react';
import { Search, Mail, Building2, Shield, Loader2, RefreshCw, AlertCircle, Users, ChevronDown, Calendar, Clock, UserCheck,  } from 'lucide-react';
import { toast } from 'sonner';
import { AxiosError } from 'axios';
import { organizationApi } from '@/lib/api/organization';
import { dashboardApi } from '@/lib/api/dashboard';
import type { CompanyMember, Company } from '@/lib/api/types';
import { getStoredUser } from '@/lib/auth';

// ── Role config ───────────────────────────────────────────────────────────────

const ROLE_CONFIG: Record<string, { label: string; color: string; bg: string; border: string; dot: string }> = {
  OWNER:           { label: 'Owner',           color: 'text-purple-700', bg: 'bg-purple-50',  border: 'border-purple-200', dot: 'bg-purple-500' },
  ADMIN:           { label: 'Admin',           color: 'text-violet-700', bg: 'bg-violet-50',  border: 'border-violet-200', dot: 'bg-violet-500' },
  PROJECT_MANAGER: { label: 'Project Manager', color: 'text-blue-700',   bg: 'bg-blue-50',    border: 'border-blue-200',   dot: 'bg-blue-500'   },
  DEVELOPER:       { label: 'Developer',       color: 'text-green-700',  bg: 'bg-green-50',   border: 'border-green-200',  dot: 'bg-green-500'  },
  TESTER:          { label: 'Tester',          color: 'text-amber-700',  bg: 'bg-amber-50',   border: 'border-amber-200',  dot: 'bg-amber-500'  },
  BUG_HUNTER:      { label: 'Bug Hunter',      color: 'text-rose-700',   bg: 'bg-rose-50',    border: 'border-rose-200',   dot: 'bg-rose-500'   },
  MEMBER:          { label: 'Member',          color: 'text-muted-foreground', bg: 'bg-muted', border: 'border-border',    dot: 'bg-muted-foreground' },
};

const ROLE_FILTERS = ['ALL', 'OWNER', 'ADMIN', 'PROJECT_MANAGER', 'DEVELOPER', 'TESTER', 'BUG_HUNTER'];

// ── Avatar initials ───────────────────────────────────────────────────────────

const AVATAR_COLORS = [
  'from-violet-500 to-purple-600',
  'from-blue-500 to-cyan-600',
  'from-green-500 to-emerald-600',
  'from-amber-500 to-orange-600',
  'from-rose-500 to-pink-600',
  'from-indigo-500 to-blue-600',
  'from-teal-500 to-green-600',
  'from-fuchsia-500 to-violet-600',
];

function getAvatarColor(name: string) {
  let hash = 0;
  for (let i = 0; i < name.length; i++) hash = name.charCodeAt(i) + ((hash << 5) - hash);
  return AVATAR_COLORS[Math.abs(hash) % AVATAR_COLORS.length];
}

function getInitials(first: string, last: string) {
  return `${first?.[0] ?? ''}${last?.[0] ?? ''}`.toUpperCase();
}

// ── Recent activity mock per member (seeded by userId) ───────────────────────

const ACTIVITY_VERBS = [
  'Completed task', 'Submitted work', 'Reviewed PR', 'Resolved bug',
  'Updated status', 'Joined project', 'Closed ticket', 'Deployed build',
];

function getMockActivity(userId: string): { text: string; ago: string }[] {
  let seed = 0;
  for (let i = 0; i < userId.length; i++) seed += userId.charCodeAt(i);
  const pick = (arr: string[], offset: number) => arr[(seed + offset) % arr.length];
  const agoOptions = ['2h ago', '5h ago', 'Yesterday', '2d ago', '3d ago'];
  return [
    { text: pick(ACTIVITY_VERBS, 0), ago: pick(agoOptions, 1) },
    { text: pick(ACTIVITY_VERBS, 3), ago: pick(agoOptions, 2) },
    { text: pick(ACTIVITY_VERBS, 5), ago: pick(agoOptions, 4) },
  ];
}

// ── Profile Card ──────────────────────────────────────────────────────────────

interface ProfileCardProps {
  member: CompanyMember;
  isCurrentUser: boolean;
}

function ProfileCard({ member, isCurrentUser }: ProfileCardProps) {
  const fullName = `${member.firstName} ${member.lastName}`;
  const roleConf = ROLE_CONFIG[member.role] ?? ROLE_CONFIG.MEMBER;
  const avatarGrad = getAvatarColor(fullName);
  const activities = getMockActivity(member.userId);

  return (
    <div className="bg-card border border-border rounded-xl overflow-hidden hover:shadow-md hover:border-primary/30 transition-all duration-200 flex flex-col">
      {/* Card header strip */}
      <div className={`h-1.5 w-full ${roleConf.dot}`} />

      <div className="p-5 flex flex-col gap-4 flex-1">
        {/* Avatar + name row */}
        <div className="flex items-start gap-3">
          <div className={`w-12 h-12 rounded-xl bg-gradient-to-br ${avatarGrad} flex items-center justify-center text-white font-bold text-base flex-shrink-0 shadow-sm`}>
            {getInitials(member.firstName, member.lastName)}
          </div>
          <div className="flex-1 min-w-0">
            <div className="flex items-center gap-1.5 flex-wrap">
              <h3 className="text-sm font-semibold text-foreground truncate">{fullName}</h3>
              {isCurrentUser && (
                <span className="text-[10px] font-medium bg-primary/10 text-primary px-1.5 py-0.5 rounded-full">You</span>
              )}
            </div>
            <span className={`inline-flex items-center gap-1 text-xs font-medium px-2 py-0.5 rounded-full border mt-1 ${roleConf.bg} ${roleConf.color} ${roleConf.border}`}>
              <span className={`w-1.5 h-1.5 rounded-full ${roleConf.dot}`} />
              {roleConf.label}
            </span>
          </div>
        </div>

        {/* Info rows */}
        <div className="space-y-2">
          <div className="flex items-center gap-2 text-xs text-muted-foreground">
            <Mail size={12} className="flex-shrink-0 text-primary/60" />
            <span className="truncate">{member.email}</span>
          </div>
          {member.departmentName && (
            <div className="flex items-center gap-2 text-xs text-muted-foreground">
              <Building2 size={12} className="flex-shrink-0 text-primary/60" />
              <span className="truncate">{member.departmentName}</span>
            </div>
          )}
          {member.joinedAt && (
            <div className="flex items-center gap-2 text-xs text-muted-foreground">
              <Calendar size={12} className="flex-shrink-0 text-primary/60" />
              <span>Joined {new Date(member.joinedAt).toLocaleDateString('en-US', { month: 'short', year: 'numeric' })}</span>
            </div>
          )}
          {member.status && (
            <div className="flex items-center gap-2 text-xs">
              <UserCheck size={12} className={`flex-shrink-0 ${member.status === 'ACTIVE' ? 'text-green-500' : 'text-muted-foreground'}`} />
              <span className={member.status === 'ACTIVE' ? 'text-green-600 font-medium' : 'text-muted-foreground'}>
                {member.status === 'ACTIVE' ? 'Active' : 'Inactive'}
              </span>
            </div>
          )}
        </div>

        {/* Divider */}
        <div className="border-t border-border" />

        {/* Recent activity */}
        <div>
          <p className="text-[10px] font-semibold text-muted-foreground uppercase tracking-wider mb-2 flex items-center gap-1">
            <Clock size={10} />
            Recent Activity
          </p>
          <ul className="space-y-1.5">
            {activities.map((act, i) => (
              <li key={i} className="flex items-center justify-between gap-2">
                <span className="text-xs text-foreground/80 truncate">{act.text}</span>
                <span className="text-[10px] text-muted-foreground whitespace-nowrap flex-shrink-0">{act.ago}</span>
              </li>
            ))}
          </ul>
        </div>
      </div>
    </div>
  );
}

// ── Main Content ──────────────────────────────────────────────────────────────

export default function ColleagueProfilesContent() {
  const [members, setMembers] = useState<CompanyMember[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [search, setSearch] = useState('');
  const [roleFilter, setRoleFilter] = useState('ALL');
  const [deptFilter, setDeptFilter] = useState('ALL');
  const [companyId, setCompanyId] = useState<string | null>(null);
  const [currentUserId, setCurrentUserId] = useState<string | null>(null);
  const [showRoleDropdown, setShowRoleDropdown] = useState(false);
  const [showDeptDropdown, setShowDeptDropdown] = useState(false);

  const departments = React.useMemo(() => {
    const depts = new Set<string>();
    members.forEach(m => { if (m.departmentName) depts.add(m.departmentName); });
    return Array.from(depts).sort();
  }, [members]);

  const fetchData = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      // Get company from dashboard summary
      const dash = await dashboardApi.getSummary();
      const activeProjects = dash.data?.activeProjectsList ?? [];
      // Try to get company from organization API
      const companiesRes = await organizationApi.listCompanies({ page: 0, size: 1 });
      const companiesList = Array.isArray(companiesRes.data)
        ? companiesRes.data
        : (companiesRes.data as { content?: Company[] })?.content ?? [];

      if (companiesList.length === 0) {
        setMembers([]);
        setLoading(false);
        return;
      }

      const cId = companiesList[0].id;
      setCompanyId(cId);

      const membersRes = await organizationApi.listMembers(cId);
      setMembers(Array.isArray(membersRes.data) ? membersRes.data : []);
    } catch (err) {
      const msg = err instanceof AxiosError && !err.response
        ? 'Cannot reach the organization service.' :'Failed to load colleague profiles.';
      setError(msg);
      toast.error(msg);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    setCurrentUserId(getStoredUser()?.id ?? null);
    fetchData();
  }, [fetchData]);

  // Close dropdowns on outside click
  useEffect(() => {
    const handler = () => { setShowRoleDropdown(false); setShowDeptDropdown(false); };
    document.addEventListener('click', handler);
    return () => document.removeEventListener('click', handler);
  }, []);

  const filtered = members.filter(m => {
    const q = search.toLowerCase();
    const matchSearch = !q || `${m.firstName} ${m.lastName} ${m.email} ${m.departmentName ?? ''}`.toLowerCase().includes(q);
    const matchRole = roleFilter === 'ALL' || m.role === roleFilter;
    const matchDept = deptFilter === 'ALL' || m.departmentName === deptFilter;
    return matchSearch && matchRole && matchDept;
  });

  // Stats
  const totalActive = members.filter(m => m.status === 'ACTIVE' || !m.status).length;
  const roleBreakdown = ROLE_FILTERS.slice(1).map(r => ({
    role: r,
    count: members.filter(m => m.role === r).length,
    conf: ROLE_CONFIG[r],
  })).filter(x => x.count > 0);

  return (
    <div className="flex flex-col gap-6 p-6 min-h-full">
      {/* Header */}
      <div className="flex items-start justify-between gap-4 flex-wrap">
        <div>
          <h1 className="text-xl font-bold text-foreground">Colleague Profiles</h1>
          <p className="text-sm text-muted-foreground mt-0.5">
            Team visibility and recruitment — {members.length} member{members.length !== 1 ? 's' : ''} across your organisation
          </p>
        </div>
        <button
          onClick={fetchData}
          className="flex items-center gap-2 px-3 py-2 text-sm font-medium text-muted-foreground hover:text-foreground border border-border rounded-lg hover:bg-muted transition-colors"
        >
          <RefreshCw size={14} />
          Refresh
        </button>
      </div>

      {/* Stats bar */}
      {!loading && members.length > 0 && (
        <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-6 gap-3">
          <div className="bg-card border border-border rounded-xl p-3 flex items-center gap-3 col-span-2 sm:col-span-1">
            <div className="w-9 h-9 rounded-lg bg-primary/10 flex items-center justify-center flex-shrink-0">
              <Users size={16} className="text-primary" />
            </div>
            <div>
              <p className="text-lg font-bold text-foreground leading-none">{totalActive}</p>
              <p className="text-[11px] text-muted-foreground mt-0.5">Active</p>
            </div>
          </div>
          {roleBreakdown.slice(0, 5).map(({ role, count, conf }) => (
            <div key={role} className={`${conf.bg} border ${conf.border} rounded-xl p-3 flex items-center gap-3`}>
              <div className={`w-9 h-9 rounded-lg bg-white/60 flex items-center justify-center flex-shrink-0`}>
                <Shield size={14} className={conf.color} />
              </div>
              <div>
                <p className={`text-lg font-bold leading-none ${conf.color}`}>{count}</p>
                <p className={`text-[11px] mt-0.5 ${conf.color} opacity-80`}>{conf.label}</p>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Filters */}
      <div className="flex flex-wrap gap-3 items-center">
        <div className="relative flex-1 min-w-[200px] max-w-sm">
          <Search size={14} className="absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground" />
          <input
            type="text"
            placeholder="Search by name, email, department…"
            value={search}
            onChange={e => setSearch(e.target.value)}
            className="w-full pl-9 pr-3 py-2 text-sm bg-card border border-border rounded-lg focus:outline-none focus:ring-2 focus:ring-primary/30 focus:border-primary/50 placeholder:text-muted-foreground"
          />
        </div>

        {/* Role filter */}
        <div className="relative">
          <button
            onClick={e => { e.stopPropagation(); setShowRoleDropdown(p => !p); setShowDeptDropdown(false); }}
            className="flex items-center gap-2 px-3 py-2 text-sm border border-border rounded-lg bg-card hover:bg-muted transition-colors"
          >
            <Shield size={13} className="text-muted-foreground" />
            <span className="text-foreground font-medium">
              {roleFilter === 'ALL' ? 'All Roles' : (ROLE_CONFIG[roleFilter]?.label ?? roleFilter)}
            </span>
            <ChevronDown size={13} className="text-muted-foreground" />
          </button>
          {showRoleDropdown && (
            <div className="absolute top-full mt-1 left-0 z-20 bg-card border border-border rounded-xl shadow-lg py-1 min-w-[160px]" onClick={e => e.stopPropagation()}>
              {ROLE_FILTERS.map(r => (
                <button
                  key={r}
                  onClick={() => { setRoleFilter(r); setShowRoleDropdown(false); }}
                  className={`w-full text-left px-3 py-2 text-sm hover:bg-muted transition-colors flex items-center gap-2 ${roleFilter === r ? 'text-primary font-semibold' : 'text-foreground'}`}
                >
                  {r !== 'ALL' && <span className={`w-2 h-2 rounded-full ${ROLE_CONFIG[r]?.dot}`} />}
                  {r === 'ALL' ? 'All Roles' : (ROLE_CONFIG[r]?.label ?? r)}
                </button>
              ))}
            </div>
          )}
        </div>

        {/* Department filter */}
        {departments.length > 0 && (
          <div className="relative">
            <button
              onClick={e => { e.stopPropagation(); setShowDeptDropdown(p => !p); setShowRoleDropdown(false); }}
              className="flex items-center gap-2 px-3 py-2 text-sm border border-border rounded-lg bg-card hover:bg-muted transition-colors"
            >
              <Building2 size={13} className="text-muted-foreground" />
              <span className="text-foreground font-medium">
                {deptFilter === 'ALL' ? 'All Departments' : deptFilter}
              </span>
              <ChevronDown size={13} className="text-muted-foreground" />
            </button>
            {showDeptDropdown && (
              <div className="absolute top-full mt-1 left-0 z-20 bg-card border border-border rounded-xl shadow-lg py-1 min-w-[180px] max-h-52 overflow-y-auto" onClick={e => e.stopPropagation()}>
                <button
                  onClick={() => { setDeptFilter('ALL'); setShowDeptDropdown(false); }}
                  className={`w-full text-left px-3 py-2 text-sm hover:bg-muted transition-colors ${deptFilter === 'ALL' ? 'text-primary font-semibold' : 'text-foreground'}`}
                >
                  All Departments
                </button>
                {departments.map(d => (
                  <button
                    key={d}
                    onClick={() => { setDeptFilter(d); setShowDeptDropdown(false); }}
                    className={`w-full text-left px-3 py-2 text-sm hover:bg-muted transition-colors ${deptFilter === d ? 'text-primary font-semibold' : 'text-foreground'}`}
                  >
                    {d}
                  </button>
                ))}
              </div>
            )}
          </div>
        )}

        {(search || roleFilter !== 'ALL' || deptFilter !== 'ALL') && (
          <button
            onClick={() => { setSearch(''); setRoleFilter('ALL'); setDeptFilter('ALL'); }}
            className="text-xs text-muted-foreground hover:text-foreground underline underline-offset-2 transition-colors"
          >
            Clear filters
          </button>
        )}

        <span className="ml-auto text-xs text-muted-foreground">
          {filtered.length} of {members.length} member{members.length !== 1 ? 's' : ''}
        </span>
      </div>

      {/* Loading */}
      {loading && (
        <div className="flex flex-col items-center justify-center py-24 gap-3">
          <Loader2 size={28} className="animate-spin text-primary" />
          <p className="text-sm text-muted-foreground">Loading colleague profiles…</p>
        </div>
      )}

      {/* Error */}
      {!loading && error && (
        <div className="flex flex-col items-center justify-center py-20 gap-4">
          <div className="w-12 h-12 rounded-full bg-red-50 flex items-center justify-center">
            <AlertCircle size={22} className="text-red-500" />
          </div>
          <div className="text-center">
            <p className="text-sm font-semibold text-foreground">{error}</p>
            <p className="text-xs text-muted-foreground mt-1">Check your connection or try again.</p>
          </div>
          <button
            onClick={fetchData}
            className="flex items-center gap-2 px-4 py-2 text-sm font-medium bg-primary text-primary-foreground rounded-lg hover:bg-primary/90 transition-colors"
          >
            <RefreshCw size={14} />
            Retry
          </button>
        </div>
      )}

      {/* Empty state */}
      {!loading && !error && members.length === 0 && (
        <div className="flex flex-col items-center justify-center py-24 gap-4">
          <div className="w-14 h-14 rounded-2xl bg-muted flex items-center justify-center">
            <Users size={26} className="text-muted-foreground" />
          </div>
          <div className="text-center">
            <p className="text-sm font-semibold text-foreground">No colleagues found</p>
            <p className="text-xs text-muted-foreground mt-1">Join or create a company to see team profiles.</p>
          </div>
        </div>
      )}

      {/* No results after filter */}
      {!loading && !error && members.length > 0 && filtered.length === 0 && (
        <div className="flex flex-col items-center justify-center py-20 gap-3">
          <div className="w-12 h-12 rounded-full bg-muted flex items-center justify-center">
            <Search size={20} className="text-muted-foreground" />
          </div>
          <p className="text-sm text-muted-foreground">No members match your filters.</p>
          <button
            onClick={() => { setSearch(''); setRoleFilter('ALL'); setDeptFilter('ALL'); }}
            className="text-xs text-primary hover:underline"
          >
            Clear filters
          </button>
        </div>
      )}

      {/* Profile grid */}
      {!loading && !error && filtered.length > 0 && (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
          {filtered.map(member => (
            <ProfileCard
              key={member.userId}
              member={member}
              isCurrentUser={member.userId === currentUserId}
            />
          ))}
        </div>
      )}
    </div>
  );
}
