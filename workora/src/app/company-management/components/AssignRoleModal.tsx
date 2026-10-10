'use client';
import React, { useState } from 'react';
import { X, Loader2, Shield } from 'lucide-react';
import { toast } from 'sonner';
import { organizationApi } from '@/lib/api/organization';
import { AxiosError } from 'axios';
import type { CompanyMember } from '@/lib/api/types';

const ROLES = ['OWNER', 'ADMIN', 'PROJECT_MANAGER', 'DEVELOPER', 'TESTER', 'BUG_HUNTER'];

interface Props {
  member: CompanyMember;
  companyId: string;
  onClose: () => void;
  onUpdated: (member: CompanyMember) => void;
}

export default function AssignRoleModal({ member, companyId, onClose, onUpdated }: Props) {
  const [selectedRole, setSelectedRole] = useState(member.role ?? 'DEVELOPER');
  const [loading, setLoading] = useState(false);

  const handleSave = async () => {
    setLoading(true);
    try {
      await organizationApi.assignMemberRole(companyId, member.userId, { role: selectedRole });
      toast.success(`Role updated to ${selectedRole}`);
      onUpdated({ ...member, role: selectedRole });
    } catch (err) {
      const msg = err instanceof AxiosError
        ? (err.response?.data?.message ?? (!err.response ? 'Cannot reach the organization service.' : 'Failed to update role'))
        : 'Failed to update role';
      toast.error(msg);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 z-[1100] flex items-center justify-center p-4">
      <div className="fixed inset-0 bg-black/40 fade-in" onClick={onClose} />
      <div className="relative max-h-[calc(100dvh-2rem)] w-full max-w-sm overflow-y-auto rounded-2xl bg-card shadow-modal fade-scale-in">
        <div className="flex items-center justify-between p-6 border-b border-border">
          <div className="flex items-center gap-3">
            <div className="w-9 h-9 rounded-xl bg-primary/10 flex items-center justify-center">
              <Shield size={18} className="text-primary" />
            </div>
            <div>
              <h2 className="text-base font-bold text-foreground">Assign Role</h2>
              <p className="text-xs text-muted-foreground">{member.firstName} {member.lastName}</p>
            </div>
          </div>
          <button onClick={onClose} className="w-8 h-8 rounded-lg flex items-center justify-center text-muted-foreground hover:text-foreground hover:bg-muted transition-all">
            <X size={16} />
          </button>
        </div>
        <div className="p-6 space-y-3">
          <p className="text-sm text-muted-foreground">Select a role for this member:</p>
          <div className="space-y-2">
            {ROLES.map((role) => (
              <button
                key={role}
                onClick={() => setSelectedRole(role)}
                className={`w-full flex items-center gap-3 px-4 py-3 rounded-xl border text-sm font-medium transition-all ${
                  selectedRole === role
                    ? 'border-primary bg-primary/5 text-primary' :'border-border text-foreground hover:border-primary/40 hover:bg-muted'
                }`}
              >
                <div className={`w-4 h-4 rounded-full border-2 flex items-center justify-center flex-shrink-0 ${selectedRole === role ? 'border-primary' : 'border-border'}`}>
                  {selectedRole === role && <div className="w-2 h-2 rounded-full bg-primary" />}
                </div>
                {role.replace(/_/g, ' ')}
              </button>
            ))}
          </div>
          <div className="flex items-center gap-3 pt-2">
            <button type="button" onClick={onClose} className="btn-secondary flex-1 py-2.5 rounded-lg font-medium text-sm">Cancel</button>
            <button
              onClick={handleSave}
              disabled={loading || selectedRole === member.role}
              className="btn-primary flex-1 py-2.5 rounded-lg font-semibold text-sm flex items-center justify-center gap-2 disabled:opacity-60"
            >
              {loading ? <><Loader2 size={14} className="animate-spin" /><span>Saving…</span></> : <><Shield size={14} /><span>Assign Role</span></>}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
