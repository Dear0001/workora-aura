'use client';
import React, { useState, useEffect, useCallback } from 'react';
import {
  Search,
  MoreHorizontal,
  UserMinus,
  ChevronUp,
  ChevronDown,
  ChevronLeft,
  ChevronRight,
  Loader2,
  Shield,
} from 'lucide-react';
import { toast } from 'sonner';
import { organizationApi } from '@/lib/api/organization';
import type { CompanyMember } from '@/lib/api/types';
import { AxiosError } from 'axios';
import { getStoredUser } from '@/lib/auth';
import AssignRoleModal from './AssignRoleModal';

const ROLE_BADGE: Record<string, string> = {
  ADMIN: 'bg-violet-50 text-violet-700 border border-violet-200',
  MANAGER: 'bg-blue-50 text-blue-700 border border-blue-200',
  PROJECT_MANAGER: 'bg-blue-50 text-blue-700 border border-blue-200',
  DEVELOPER: 'bg-green-50 text-green-700 border border-green-200',
  TESTER: 'bg-amber-50 text-amber-700 border border-amber-200',
  BUG_HUNTER: 'bg-rose-50 text-rose-700 border border-rose-200',
  OWNER: 'bg-purple-50 text-purple-700 border border-purple-200',
  MEMBER: 'bg-muted text-muted-foreground border border-border',
};

const STATUS_BADGE: Record<string, string> = {
  ACTIVE: 'bg-green-50 text-green-700',
  INACTIVE: 'bg-muted text-muted-foreground',
};

type SortField = 'name' | 'role' | 'department';
type SortDir = 'asc' | 'desc';

interface Props {
  onInvite: () => void;
  companyId?: string;
}

export default function MembersTab({ onInvite, companyId }: Props) {
  const [members, setMembers] = useState<CompanyMember[]>([]);
  const [loading, setLoading] = useState(Boolean(companyId));
  const [search, setSearch] = useState('');
  const [roleFilter, setRoleFilter] = useState<string>('ALL');
  const [sortField, setSortField] = useState<SortField>('name');
  const [sortDir, setSortDir] = useState<SortDir>('asc');
  const [selectedIds, setSelectedIds] = useState<Set<string>>(new Set());
  const [activeRowMenu, setActiveRowMenu] = useState<string | null>(null);
  const [currentPage, setCurrentPage] = useState(1);
  const [currentUserId, setCurrentUserId] = useState<string | null>(null);
  const [roleModalMember, setRoleModalMember] = useState<CompanyMember | null>(null);
  const pageSize = 8;

  const fetchMembers = useCallback(async () => {
    if (!companyId) {
      setMembers([]);
      setLoading(false);
      return;
    }
    setLoading(true);
    try {
      const response = await organizationApi.listMembers(companyId);
      setMembers(Array.isArray(response.data) ? response.data : []);
    } catch (err) {
      toast.error(
        err instanceof AxiosError && !err.response
          ? 'Cannot reach the organization service.'
          : 'Failed to load members'
      );
      setMembers([]);
    } finally {
      setLoading(false);
    }
  }, [companyId]);

  useEffect(() => {
    setCurrentUserId(getStoredUser()?.id ?? null);
    fetchMembers();
  }, [fetchMembers]);

  const filtered = members
    .filter((m) => {
      const q = search.toLowerCase();
      const matchSearch =
        !q ||
        `${m.firstName} ${m.lastName} ${m.email} ${m.departmentName ?? ''}`
          .toLowerCase()
          .includes(q);
      const matchRole = roleFilter === 'ALL' || m.role === roleFilter;
      return matchSearch && matchRole;
    })
    .sort((a, b) => {
      let valA = '';
      let valB = '';
      if (sortField === 'name') {
        valA = `${a.firstName} ${a.lastName}`;
        valB = `${b.firstName} ${b.lastName}`;
      } else if (sortField === 'role') {
        valA = a.role;
        valB = b.role;
      } else if (sortField === 'department') {
        valA = a.departmentName ?? '';
        valB = b.departmentName ?? '';
      }
      if (valA < valB) return sortDir === 'asc' ? -1 : 1;
      if (valA > valB) return sortDir === 'asc' ? 1 : -1;
      return 0;
    });

  const totalPages = Math.ceil(filtered.length / pageSize);
  const paginated = filtered.slice((currentPage - 1) * pageSize, currentPage * pageSize);

  const toggleSort = (field: SortField) => {
    if (sortField === field) setSortDir((d) => (d === 'asc' ? 'desc' : 'asc'));
    else {
      setSortField(field);
      setSortDir('asc');
    }
  };

  const toggleSelect = (id: string) => {
    setSelectedIds((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  };

  const toggleSelectAll = () => {
    const removableIds = paginated.filter((m) => m.userId !== currentUserId).map((m) => m.userId);
    if (selectedIds.size === removableIds.length) setSelectedIds(new Set());
    else setSelectedIds(new Set(removableIds));
  };

  const handleRemoveMember = async (userId: string, name: string) => {
    if (userId === currentUserId) {
      toast.error('You cannot remove yourself from the company.');
      setActiveRowMenu(null);
      return;
    }
    if (!companyId) {
      toast.error('Select a company before removing a member.');
      setActiveRowMenu(null);
      return;
    }
    try {
      await organizationApi.removeMember(companyId, userId);
      setMembers((prev) => prev.filter((m) => m.userId !== userId));
      toast.success(`${name} removed from company`);
    } catch (err) {
      toast.error(
        err instanceof AxiosError
          ? (err.response?.data?.message ?? 'Failed to remove member')
          : 'Failed to remove member'
      );
    }
    setActiveRowMenu(null);
  };

  const SortIcon = ({ field }: { field: SortField }) => {
    if (sortField !== field) return <div className="w-3" />;
    return sortDir === 'asc' ? (
      <ChevronUp size={12} className="text-primary" />
    ) : (
      <ChevronDown size={12} className="text-primary" />
    );
  };

  return (
    <>
      <div className="space-y-4">
        {/* Filters */}
        <div className="flex flex-col sm:flex-row gap-3">
          <div className="relative flex-1">
            <Search
              size={14}
              className="absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground"
            />
            <input
              type="text"
              placeholder="Search by name, email, or department…"
              value={search}
              onChange={(e) => {
                setSearch(e.target.value);
                setCurrentPage(1);
              }}
              className="input-base pl-8 text-sm"
            />
          </div>
          <select
            value={roleFilter}
            onChange={(e) => {
              setRoleFilter(e.target.value);
              setCurrentPage(1);
            }}
            className="input-base text-sm w-full sm:w-40"
          >
            <option value="ALL">All Roles</option>
            <option value="OWNER">Owner</option>
            <option value="ADMIN">Admin</option>
            <option value="PROJECT_MANAGER">Project Manager</option>
            <option value="DEVELOPER">Developer</option>
            <option value="TESTER">Tester</option>
            <option value="BUG_HUNTER">Bug Hunter</option>
            <option value="MEMBER">Member</option>
          </select>
          <button
            onClick={onInvite}
            className="btn-primary flex items-center gap-2 px-4 py-2 rounded-lg text-sm font-semibold sm:hidden"
          >
            + Invite
          </button>
        </div>

        {/* Bulk action bar */}
        {selectedIds.size > 0 && (
          <div className="flex items-center gap-3 px-4 py-2.5 bg-primary/5 border border-primary/20 rounded-lg slide-up">
            <span className="text-sm font-medium text-primary">{selectedIds.size} selected</span>
            <div className="flex items-center gap-2 ml-auto">
              <button
                onClick={() => {
                  toast.success(`${selectedIds.size} members removed`);
                  setSelectedIds(new Set());
                }}
                className="flex items-center gap-1.5 text-xs text-red-600 font-medium hover:bg-red-50 px-2.5 py-1.5 rounded-lg transition-colors"
              >
                <UserMinus size={13} />
                Remove Selected
              </button>
              <button
                onClick={() => setSelectedIds(new Set())}
                className="text-xs text-muted-foreground hover:text-foreground px-2 py-1.5 rounded-lg hover:bg-muted transition-colors"
              >
                Clear
              </button>
            </div>
          </div>
        )}

        {/* Table */}
        <div className="card-elevated overflow-hidden">
          {loading && (
            <div className="flex items-center justify-center gap-2 py-4 border-b border-border bg-muted/20">
              <Loader2 size={14} className="animate-spin text-primary" />
              <span className="text-xs text-muted-foreground">Loading members from API…</span>
            </div>
          )}
          <div className="overflow-x-auto scrollbar-thin">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-border bg-muted/40">
                  <th className="w-10 px-4 py-3 text-left">
                    <input
                      type="checkbox"
                      checked={selectedIds.size === paginated.length && paginated.length > 0}
                      onChange={toggleSelectAll}
                      className="w-4 h-4 rounded border-input text-primary"
                    />
                  </th>
                  <th className="px-4 py-3 text-left">
                    <button
                      onClick={() => toggleSort('name')}
                      className="flex items-center gap-1 text-xs font-semibold text-muted-foreground uppercase tracking-wider hover:text-foreground"
                    >
                      Member <SortIcon field="name" />
                    </button>
                  </th>
                  <th className="px-4 py-3 text-left">
                    <button
                      onClick={() => toggleSort('role')}
                      className="flex items-center gap-1 text-xs font-semibold text-muted-foreground uppercase tracking-wider hover:text-foreground"
                    >
                      Role <SortIcon field="role" />
                    </button>
                  </th>
                  <th className="px-4 py-3 text-left">
                    <button
                      onClick={() => toggleSort('department')}
                      className="flex items-center gap-1 text-xs font-semibold text-muted-foreground uppercase tracking-wider hover:text-foreground"
                    >
                      Department <SortIcon field="department" />
                    </button>
                  </th>
                  <th className="px-4 py-3 text-left">
                    <span className="text-xs font-semibold text-muted-foreground uppercase tracking-wider">
                      Status
                    </span>
                  </th>
                  <th className="px-4 py-3 text-left">
                    <span className="text-xs font-semibold text-muted-foreground uppercase tracking-wider">
                      Joined
                    </span>
                  </th>
                  <th className="px-4 py-3 w-16" />
                </tr>
              </thead>
              <tbody>
                {paginated.length === 0 ? (
                  <tr>
                    <td colSpan={7} className="px-4 py-12 text-center">
                      <div className="flex flex-col items-center gap-2">
                        <Search size={24} className="text-muted-foreground/40" />
                        <p className="text-sm font-medium text-muted-foreground">
                          No members match your filters
                        </p>
                      </div>
                    </td>
                  </tr>
                ) : (
                  paginated.map((member, i) => (
                    <tr
                      key={member.userId}
                      className={`border-b border-border last:border-0 hover:bg-muted/40 transition-colors group ${i % 2 === 1 ? 'bg-muted/10' : ''}`}
                    >
                      <td className="px-4 py-3">
                        <input
                          type="checkbox"
                          checked={selectedIds.has(member.userId)}
                          disabled={member.userId === currentUserId}
                          onChange={() => toggleSelect(member.userId)}
                          className="w-4 h-4 rounded border-input text-primary"
                        />
                      </td>
                      <td className="px-4 py-3">
                        <div className="flex items-center gap-2.5">
                          <div className="w-8 h-8 rounded-full bg-primary/10 flex items-center justify-center flex-shrink-0">
                            <span className="text-xs font-semibold text-primary">
                              {member?.firstName?.[0]}
                              {member?.lastName?.[0]}
                            </span>
                          </div>
                          <div>
                            <p className="text-sm font-medium text-foreground">
                              {member?.firstName} {member?.lastName}
                            </p>
                            <p className="text-xs text-muted-foreground">{member?.email}</p>
                          </div>
                        </div>
                      </td>
                      <td className="px-4 py-3">
                        <span
                          className={`badge-base ${ROLE_BADGE[member?.role] ?? ROLE_BADGE.MEMBER}`}
                        >
                          {member?.role?.replace(/_/g, ' ')}
                        </span>
                      </td>
                      <td className="px-4 py-3 text-sm text-muted-foreground">
                        {member?.departmentName ?? '—'}
                      </td>
                      <td className="px-4 py-3">
                        <span
                          className={`badge-base ${STATUS_BADGE[member?.status ?? 'ACTIVE'] ?? STATUS_BADGE.ACTIVE}`}
                        >
                          {member?.status ?? 'ACTIVE'}
                        </span>
                      </td>
                      <td className="px-4 py-3 text-sm text-muted-foreground">
                        {member?.joinedAt
                          ? new Date(member?.joinedAt).toLocaleDateString('en-US', {
                              month: 'short',
                              day: 'numeric',
                              year: 'numeric',
                            })
                          : '—'}
                      </td>
                      <td className="px-4 py-3">
                        <div className="relative">
                          <button
                            onClick={() =>
                              setActiveRowMenu(
                                activeRowMenu === member?.userId ? null : member?.userId
                              )
                            }
                            className="w-7 h-7 rounded-md flex items-center justify-center text-muted-foreground hover:text-foreground hover:bg-muted opacity-0 group-hover:opacity-100 transition-all"
                          >
                            <MoreHorizontal size={15} />
                          </button>
                          {activeRowMenu === member?.userId && (
                            <>
                              <div
                                className="fixed inset-0 z-10"
                                onClick={() => setActiveRowMenu(null)}
                              />
                              <div className="absolute right-0 top-full mt-1 w-40 bg-card border border-border rounded-xl shadow-elevated z-20 py-1 fade-scale-in">
                                <button
                                  onClick={() => {
                                    setRoleModalMember(member);
                                    setActiveRowMenu(null);
                                  }}
                                  className="flex items-center gap-2 w-full px-3 py-2 text-xs text-foreground hover:bg-muted transition-colors"
                                >
                                  <Shield size={12} className="text-muted-foreground" />
                                  Assign Role
                                </button>
                                <button
                                  onClick={() =>
                                    handleRemoveMember(
                                      member?.userId,
                                      `${member?.firstName} ${member?.lastName}`
                                    )
                                  }
                                  disabled={member.userId === currentUserId}
                                  className="flex items-center gap-2 w-full px-3 py-2 text-xs text-red-600 hover:bg-red-50 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
                                >
                                  <UserMinus size={12} />
                                  Remove
                                </button>
                              </div>
                            </>
                          )}
                        </div>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>

          {/* Pagination */}
          {totalPages > 1 && (
            <div className="flex items-center justify-between px-4 py-3 border-t border-border">
              <span className="text-xs text-muted-foreground">
                Showing {(currentPage - 1) * pageSize + 1}–
                {Math.min(currentPage * pageSize, filtered.length)} of {filtered.length}
              </span>
              <div className="flex items-center gap-1">
                <button
                  onClick={() => setCurrentPage((p) => Math.max(1, p - 1))}
                  disabled={currentPage === 1}
                  className="w-7 h-7 rounded-md flex items-center justify-center text-muted-foreground hover:text-foreground hover:bg-muted disabled:opacity-40 transition-all"
                >
                  <ChevronLeft size={14} />
                </button>
                <span className="text-xs text-foreground font-medium px-2">
                  {currentPage} / {totalPages}
                </span>
                <button
                  onClick={() => setCurrentPage((p) => Math.min(totalPages, p + 1))}
                  disabled={currentPage === totalPages}
                  className="w-7 h-7 rounded-md flex items-center justify-center text-muted-foreground hover:text-foreground hover:bg-muted disabled:opacity-40 transition-all"
                >
                  <ChevronRight size={14} />
                </button>
              </div>
            </div>
          )}
        </div>
      </div>

      {roleModalMember && companyId && (
        <AssignRoleModal
          member={roleModalMember}
          companyId={companyId}
          onClose={() => setRoleModalMember(null)}
          onUpdated={(updated) => {
            setMembers((prev) => prev.map((m) => (m.userId === updated.userId ? updated : m)));
            setRoleModalMember(null);
          }}
        />
      )}
    </>
  );
}
