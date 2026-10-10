'use client';
import React, { useState, useEffect, useCallback } from 'react';
import { X, Loader2, Shield, UserMinus, Search, Users, UserPlus } from 'lucide-react';
import { toast } from 'sonner';
import { organizationApi } from '@/lib/api/organization';
import { AxiosError } from 'axios';
import type { CompanyMember, Department } from '@/lib/api/types';

interface Props {
  department: Department;
  companyId: string;
  onClose: () => void;
}

type DepartmentRole = 'DEPARTMENT_MANAGER' | 'PROJECT_MANAGER' | 'DEVELOPER' | 'TESTER' | 'BUG_HUNTER';

const DEPARTMENT_ROLES: { value: DepartmentRole; label: string }[] = [
  { value: 'DEPARTMENT_MANAGER', label: 'Department Manager' },
  { value: 'PROJECT_MANAGER', label: 'Project Manager' },
  { value: 'DEVELOPER', label: 'Developer' },
  { value: 'TESTER', label: 'Tester' },
  { value: 'BUG_HUNTER', label: 'Bug Hunter' },
];

export default function DepartmentRoleModal({ department, companyId, onClose }: Props) {
  const [members, setMembers] = useState<CompanyMember[]>([]);
  const [companyMembers, setCompanyMembers] = useState<CompanyMember[]>([]);
  const [selectedMemberId, setSelectedMemberId] = useState('');
  const [addingMember, setAddingMember] = useState(false);
  const [selectedDepartmentRole, setSelectedDepartmentRole] = useState<DepartmentRole>('DEVELOPER');
  const [departmentRoles, setDepartmentRoles] = useState<Record<string, DepartmentRole>>({});
  const [savingRoles, setSavingRoles] = useState<Record<string, boolean>>({});
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');

  const fetchMembers = useCallback(async () => {
    setLoading(true);
    try {
      const res = await organizationApi.listDepartmentMembers(department.id);
      const companyRes = await organizationApi.listMembers(companyId);
      const list: CompanyMember[] = Array.isArray(res.data)
        ? res.data
        : (res.data as { content?: CompanyMember[] })?.content ?? [];
      setMembers(list);
      setCompanyMembers(companyRes.data);
      const nextRoles: Record<string, DepartmentRole> = {};
      list.forEach(member => {
        const assignment = member.departmentMemberships?.find(item => item.departmentId === department.id);
        const legacyRole = DEPARTMENT_ROLES.find(item => item.value === member.role)?.value;
        const role = assignment?.role ?? legacyRole;
        const knownRole = DEPARTMENT_ROLES.find(item => item.value === role)?.value;
        if (knownRole) nextRoles[member.userId] = knownRole;
      });
      setDepartmentRoles(nextRoles);
    } catch (err) {
      toast.error(
        err instanceof AxiosError && !err.response
          ? 'Cannot reach the organization service.' :'Failed to load department members'
      );
    } finally {
      setLoading(false);
    }
  }, [companyId, department.id]);

  useEffect(() => { fetchMembers(); }, [fetchMembers]);

  const handleRemoveMember = async (member: CompanyMember) => {
    if (!window.confirm(`Remove ${member.firstName} ${member.lastName} from ${department.name}?`)) return;
    try {
      await organizationApi.removeDepartmentMember(department.id, member.userId);
      setMembers(prev => prev.filter(m => m.userId !== member.userId));
      toast.success(`${member.firstName} ${member.lastName} removed from ${department.name}`);
    } catch (err) {
      const msg = err instanceof AxiosError
        ? (err.response?.data?.message ?? (!err.response ? 'Cannot reach the organization service.' : 'Failed to remove member'))
        : 'Failed to remove member';
      toast.error(msg);
    }
  };

  const handleSaveDepartmentRole = async (member: CompanyMember) => {
    const selectedRole = departmentRoles[member.userId];
    if (!selectedRole) return;
    setSavingRoles(prev => ({ ...prev, [member.userId]: true }));
    try {
      const assigned = member.departmentMemberships?.some(item => item.departmentId === department.id);
      if (assigned) {
        await organizationApi.updateDepartmentMemberRole(department.id, member.userId, selectedRole);
      } else {
        await organizationApi.addDepartmentMember(department.id, member.userId, { role: selectedRole });
      }
      await fetchMembers();
      toast.success(`Department role updated for ${member.firstName} ${member.lastName}`);
    } catch (err) {
      toast.error(err instanceof AxiosError
        ? (err.response?.data?.message ?? 'Failed to update department role')
        : 'Failed to update department role');
    } finally {
      setSavingRoles(prev => ({ ...prev, [member.userId]: false }));
    }
  };

  const handleAddMember = async () => {
    if (!selectedMemberId) return;
    setAddingMember(true);
    try {
      await organizationApi.addDepartmentMember(department.id, selectedMemberId, {
        role: selectedDepartmentRole,
      });
      setSelectedMemberId('');
      await fetchMembers();
      toast.success('Member assigned to department');
    } catch (err) {
      const msg = err instanceof AxiosError
        ? (err.response?.data?.message ?? (!err.response ? 'Cannot reach the organization service.' : 'Failed to assign member'))
        : 'Failed to assign member';
      toast.error(msg);
    } finally {
      setAddingMember(false);
    }
  };

  const unassignedMembers = companyMembers.filter(
    member => !members.some(departmentMember => departmentMember.userId === member.userId)
  );

  const filtered = members.filter(m => {
    const q = search.toLowerCase();
    return !q || `${m.firstName} ${m.lastName} ${m.email}`.toLowerCase().includes(q);
  });

  return (
    <div className="fixed inset-0 z-[1100] flex items-center justify-center p-4">
      <div className="fixed inset-0 bg-black/40 fade-in" onClick={onClose} />
      <div className="relative flex max-h-[calc(100dvh-2rem)] w-full max-w-2xl flex-col overflow-hidden rounded-2xl bg-card shadow-modal fade-scale-in">
        {/* Header */}
        <div className="flex items-center justify-between p-6 border-b border-border flex-shrink-0">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-primary/10 flex items-center justify-center">
              <Shield size={20} className="text-primary" />
            </div>
            <div>
              <h2 className="text-base font-bold text-foreground">Manage Department Members</h2>
              <p className="text-xs text-muted-foreground">{department.name} · Manage membership and department-level roles</p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="w-8 h-8 rounded-lg flex items-center justify-center text-muted-foreground hover:text-foreground hover:bg-muted transition-all"
          >
            <X size={16} />
          </button>
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
              className="input-base input-search text-sm"
            />
          </div>
        </div>

        <div className="px-6 pb-3">
          <div className="flex gap-2">
            <select
              value={selectedDepartmentRole}
              onChange={event => {
                const role = DEPARTMENT_ROLES.find(item => item.value === event.target.value)?.value;
                if (role) setSelectedDepartmentRole(role);
              }}
              className="input-base text-sm max-w-52"
              aria-label="Department role for new member"
            >
              {DEPARTMENT_ROLES.map(role => <option key={role.value} value={role.value}>{role.label}</option>)}
            </select>
            <select
              value={selectedMemberId}
              onChange={event => setSelectedMemberId(event.target.value)}
              className="input-base text-sm"
              disabled={unassignedMembers.length === 0}
            >
              <option value="">
                {unassignedMembers.length ? 'Select a company member to assign' : 'No unassigned company members'}
              </option>
              {unassignedMembers.map(member => (
                <option key={member.userId} value={member.userId}>
                  {member.firstName || member.email} {member.lastName ?? ''}
                </option>
              ))}
            </select>
            <button
              type="button"
              onClick={handleAddMember}
              disabled={!selectedMemberId || addingMember}
              className="btn-primary whitespace-nowrap px-3 rounded-lg text-xs font-semibold flex items-center gap-1.5 disabled:opacity-50"
            >
              {addingMember ? <Loader2 size={13} className="animate-spin" /> : <UserPlus size={13} />}
              Assign
            </button>
          </div>
          <p className="text-xs text-muted-foreground mt-2">
            A member can be assigned to one department at a time. Reassigning moves them from their current department; their company role remains company-wide.
          </p>
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
            filtered.map(member => (
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

                  <select
                    aria-label={`Department role for ${member.email}`}
                    value={departmentRoles[member.userId] ?? 'DEVELOPER'}
                    onChange={event => {
                      const role = DEPARTMENT_ROLES.find(item => item.value === event.target.value)?.value;
                      if (role) {
                        setDepartmentRoles(prev => ({ ...prev, [member.userId]: role }));
                      }
                    }}
                    className="input-base text-xs max-w-48"
                  >
                    {DEPARTMENT_ROLES.map(role => (
                      <option key={role.value} value={role.value}>{role.label}</option>
                    ))}
                  </select>
                  <button
                    type="button"
                    onClick={() => handleSaveDepartmentRole(member)}
                    disabled={savingRoles[member.userId]}
                    className="btn-secondary px-2.5 py-1.5 rounded-md text-xs disabled:opacity-50"
                  >
                    {savingRoles[member.userId] ? <Loader2 size={12} className="animate-spin" /> : 'Save role'}
                  </button>

                  {/* Remove member button */}
                  <button
                    onClick={() => handleRemoveMember(member)}
                    className="w-7 h-7 rounded-lg flex items-center justify-center text-muted-foreground hover:text-red-600 hover:bg-red-50 transition-all flex-shrink-0"
                    title="Remove from department"
                  >
                    <UserMinus size={13} />
                  </button>
                </div>
            ))
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
