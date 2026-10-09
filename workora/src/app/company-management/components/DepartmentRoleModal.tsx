'use client';
import React, { useState, useEffect, useCallback } from 'react';
import { X, Loader2, Shield, UserMinus, ChevronDown, Search, Users } from 'lucide-react';
import { toast } from 'sonner';
import { organizationApi } from '@/lib/api/organization';
import { AxiosError } from 'axios';
import type { CompanyMember, Department } from '@/lib/api/types';

const ROLES = [
  { value: 'OWNER', label: 'Owner', color: 'text-purple-700 bg-purple-50 border-purple-200' },
  { value: 'ADMIN', label: 'Admin', color: 'text-violet-700 bg-violet-50 border-violet-200' },
  { value: 'PROJECT_MANAGER', label: 'Project Manager', color: 'text-blue-700 bg-blue-50 border-blue-200' },
  { value: 'DEVELOPER', label: 'Developer', color: 'text-green-700 bg-green-50 border-green-200' },
  { value: 'TESTER', label: 'Tester', color: 'text-amber-700 bg-amber-50 border-amber-200' },
  { value: 'BUG_HUNTER', label: 'Bug Hunter', color: 'text-rose-700 bg-rose-50 border-rose-200' },
];

const ROLE_BADGE: Record<string, string> = {
  OWNER: 'bg-purple-50 text-purple-700 border border-purple-200',
  ADMIN: 'bg-violet-50 text-violet-700 border border-violet-200',
  PROJECT_MANAGER: 'bg-blue-50 text-blue-700 border border-blue-200',
  DEVELOPER: 'bg-green-50 text-green-700 border border-green-200',
  TESTER: 'bg-amber-50 text-amber-700 border border-amber-200',
  BUG_HUNTER: 'bg-rose-50 text-rose-700 border border-rose-200',
};

interface Props {
  department: Department;
  companyId: string;
  onClose: () => void;
}

interface MemberRoleState {
  selectedRole: string;
  saving: boolean;
  removing: boolean;
  dropdownOpen: boolean;
}

export default function DepartmentRoleModal({ department, companyId, onClose }: Props) {
  const [members, setMembers] = useState<CompanyMember[]>([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [memberStates, setMemberStates] = useState<Record<string, MemberRoleState>>({});

  const fetchMembers = useCallback(async () => {
    setLoading(true);
    try {
      const res = await organizationApi.listDepartmentMembers(department.id);
      const list: CompanyMember[] = Array.isArray(res.data)
        ? res.data
        : (res.data as { content?: CompanyMember[] })?.content ?? [];
      setMembers(list);
      const states: Record<string, MemberRoleState> = {};
      list.forEach(m => {
        states[m.userId] = {
          selectedRole: m.role ?? 'DEVELOPER',
          saving: false,
          removing: false,
          dropdownOpen: false,
        };
      });
      setMemberStates(states);
    } catch (err) {
      toast.error(
        err instanceof AxiosError && !err.response
          ? 'Cannot reach the organization service.' :'Failed to load department members'
      );
    } finally {
      setLoading(false);
    }
  }, [department.id]);

  useEffect(() => { fetchMembers(); }, [fetchMembers]);

  const setMemberState = (userId: string, patch: Partial<MemberRoleState>) => {
    setMemberStates(prev => ({ ...prev, [userId]: { ...prev[userId], ...patch } }));
  };

  const handleRoleSelect = (userId: string, role: string) => {
    setMemberState(userId, { selectedRole: role, dropdownOpen: false });
  };

  const handleSaveRole = async (member: CompanyMember) => {
    const state = memberStates[member.userId];
    if (!state) return;
    setMemberState(member.userId, { saving: true });
    try {
      // Try PATCH (update) first; if member already has a role use update, otherwise assign
      if (member.role && member.role !== '') {
        await organizationApi.updateMemberRole(companyId, member.userId, { role: state.selectedRole });
      } else {
        await organizationApi.assignMemberRole(companyId, member.userId, { role: state.selectedRole });
      }
      setMembers(prev => prev.map(m => m.userId === member.userId ? { ...m, role: state.selectedRole } : m));
      toast.success(`Role updated to ${state.selectedRole.replace(/_/g, ' ')} for ${member.firstName} ${member.lastName}`);
    } catch (err) {
      const msg = err instanceof AxiosError
        ? (err.response?.data?.message ?? (!err.response ? 'Cannot reach the organization service.' : 'Failed to update role'))
        : 'Failed to update role';
      toast.error(msg);
    } finally {
      setMemberState(member.userId, { saving: false });
    }
  };

  const handleRemoveMember = async (member: CompanyMember) => {
    if (!window.confirm(`Remove ${member.firstName} ${member.lastName} from ${department.name}?`)) return;
    setMemberState(member.userId, { removing: true });
    try {
      await organizationApi.removeDepartmentMember(department.id, member.userId);
      setMembers(prev => prev.filter(m => m.userId !== member.userId));
      toast.success(`${member.firstName} ${member.lastName} removed from ${department.name}`);
    } catch (err) {
      const msg = err instanceof AxiosError
        ? (err.response?.data?.message ?? (!err.response ? 'Cannot reach the organization service.' : 'Failed to remove member'))
        : 'Failed to remove member';
      toast.error(msg);
      setMemberState(member.userId, { removing: false });
    }
  };

  const filtered = members.filter(m => {
    const q = search.toLowerCase();
    return !q || `${m.firstName} ${m.lastName} ${m.email}`.toLowerCase().includes(q);
  });

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      <div className="fixed inset-0 bg-black/40 fade-in" onClick={onClose} />
      <div className="relative bg-card rounded-2xl shadow-modal w-full max-w-2xl max-h-[90vh] flex flex-col fade-scale-in">
        {/* Header */}
        <div className="flex items-center justify-between p-6 border-b border-border flex-shrink-0">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-primary/10 flex items-center justify-center">
              <Shield size={20} className="text-primary" />
            </div>
            <div>
              <h2 className="text-base font-bold text-foreground">Manage Member Roles</h2>
              <p className="text-xs text-muted-foreground">{department.name} · Assign, update, or remove roles</p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="w-8 h-8 rounded-lg flex items-center justify-center text-muted-foreground hover:text-foreground hover:bg-muted transition-all"
          >
            <X size={16} />
          </button>
        </div>

        {/* Role legend */}
        <div className="px-6 pt-4 flex-shrink-0">
          <div className="flex flex-wrap gap-1.5">
            {ROLES.map(r => (
              <span key={r.value} className={`badge-base border text-xs ${r.color}`}>
                {r.label}
              </span>
            ))}
          </div>
        </div>

        {/* Search */}
        <div className="px-6 pt-3 pb-2 flex-shrink-0">
          <div className="relative">
            <Search size={14} className="absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground" />
            <input
              type="text"
              placeholder="Search members…"
              value={search}
              onChange={e => setSearch(e.target.value)}
              className="input-base pl-8 text-sm"
            />
          </div>
        </div>

        {/* Member list */}
        <div className="flex-1 overflow-y-auto px-6 pb-6 space-y-2 scrollbar-thin">
          {loading ? (
            <div className="flex items-center justify-center gap-2 py-12">
              <Loader2 size={18} className="animate-spin text-primary" />
              <span className="text-sm text-muted-foreground">Loading members…</span>
            </div>
          ) : filtered.length === 0 ? (
            <div className="flex flex-col items-center justify-center gap-3 py-12">
              <div className="w-12 h-12 rounded-full bg-muted flex items-center justify-center">
                <Users size={20} className="text-muted-foreground" />
              </div>
              <p className="text-sm font-medium text-muted-foreground">
                {search ? 'No members match your search' : 'No members in this department'}
              </p>
            </div>
          ) : (
            filtered.map(member => {
              const state = memberStates[member.userId];
              if (!state) return null;
              const currentRole = ROLES.find(r => r.value === member.role);
              const selectedRoleObj = ROLES.find(r => r.value === state.selectedRole);
              const hasChanged = state.selectedRole !== (member.role ?? 'DEVELOPER');

              return (
                <div
                  key={member.userId}
                  className="flex items-center gap-3 p-3 rounded-xl border border-border bg-card hover:bg-muted/30 transition-colors"
                >
                  {/* Avatar */}
                  <div className="w-9 h-9 rounded-full bg-primary/10 flex items-center justify-center flex-shrink-0">
                    <span className="text-xs font-semibold text-primary">
                      {member.firstName?.[0]}{member.lastName?.[0]}
                    </span>
                  </div>

                  {/* Info */}
                  <div className="flex-1 min-w-0">
                    <p className="text-sm font-medium text-foreground truncate">
                      {member.firstName} {member.lastName}
                    </p>
                    <p className="text-xs text-muted-foreground truncate">{member.email}</p>
                  </div>

                  {/* Current role badge */}
                  {currentRole && (
                    <span className={`badge-base border text-xs hidden sm:inline-flex flex-shrink-0 ${currentRole.color}`}>
                      {currentRole.label}
                    </span>
                  )}

                  {/* Role selector dropdown */}
                  <div className="relative flex-shrink-0">
                    <button
                      onClick={() => setMemberState(member.userId, { dropdownOpen: !state.dropdownOpen })}
                      className={`flex items-center gap-1.5 px-3 py-1.5 rounded-lg border text-xs font-medium transition-all ${
                        selectedRoleObj ? selectedRoleObj.color : 'border-border text-foreground'
                      } hover:opacity-80`}
                    >
                      {selectedRoleObj?.label ?? 'Select Role'}
                      <ChevronDown size={11} className={`transition-transform ${state.dropdownOpen ? 'rotate-180' : ''}`} />
                    </button>
                    {state.dropdownOpen && (
                      <>
                        <div className="fixed inset-0 z-10" onClick={() => setMemberState(member.userId, { dropdownOpen: false })} />
                        <div className="absolute right-0 top-full mt-1 w-44 bg-card border border-border rounded-xl shadow-elevated z-20 py-1 fade-scale-in">
                          {ROLES.map(role => (
                            <button
                              key={role.value}
                              onClick={() => handleRoleSelect(member.userId, role.value)}
                              className={`flex items-center gap-2 w-full px-3 py-2 text-xs transition-colors hover:bg-muted ${
                                state.selectedRole === role.value ? 'font-semibold text-primary bg-primary/5' : 'text-foreground'
                              }`}
                            >
                              <span className={`w-2 h-2 rounded-full border ${role.color}`} />
                              {role.label}
                            </button>
                          ))}
                        </div>
                      </>
                    )}
                  </div>

                  {/* Save role button */}
                  <button
                    onClick={() => handleSaveRole(member)}
                    disabled={!hasChanged || state.saving || state.removing}
                    className="flex items-center gap-1 px-3 py-1.5 rounded-lg bg-primary text-primary-foreground text-xs font-semibold disabled:opacity-40 disabled:cursor-not-allowed hover:opacity-90 transition-all flex-shrink-0"
                  >
                    {state.saving ? (
                      <Loader2 size={11} className="animate-spin" />
                    ) : (
                      <Shield size={11} />
                    )}
                    {state.saving ? 'Saving…' : 'Save'}
                  </button>

                  {/* Remove member button */}
                  <button
                    onClick={() => handleRemoveMember(member)}
                    disabled={state.removing || state.saving}
                    className="w-7 h-7 rounded-lg flex items-center justify-center text-muted-foreground hover:text-red-600 hover:bg-red-50 disabled:opacity-40 disabled:cursor-not-allowed transition-all flex-shrink-0"
                    title="Remove from department"
                  >
                    {state.removing ? (
                      <Loader2 size={13} className="animate-spin" />
                    ) : (
                      <UserMinus size={13} />
                    )}
                  </button>
                </div>
              );
            })
          )}
        </div>

        {/* Footer */}
        <div className="flex items-center justify-between px-6 py-4 border-t border-border flex-shrink-0">
          <p className="text-xs text-muted-foreground">
            {filtered.length} member{filtered.length !== 1 ? 's' : ''} in this department
          </p>
          <button
            onClick={onClose}
            className="btn-secondary px-4 py-2 rounded-lg text-sm font-medium"
          >
            Close
          </button>
        </div>
      </div>
    </div>
  );
}
