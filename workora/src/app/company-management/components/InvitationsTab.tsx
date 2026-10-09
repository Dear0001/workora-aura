'use client';
import React, { useCallback, useEffect, useState } from 'react';
import { Clock, RotateCcw, X, Mail } from 'lucide-react';
import { toast } from 'sonner';
import { AxiosError } from 'axios';
import { organizationApi } from '@/lib/api/organization';
import type { Invitation as ApiInvitation } from '@/lib/api/types';

interface Invitation {
  id: string;
  email: string;
  role: string;
  department: string;
  invitedBy: string;
  invitedDate: string;
  expiresIn: string;
  status: 'PENDING' | 'ACCEPTED' | 'EXPIRED' | 'CANCELLED';
}

const STATUS_BADGE: Record<string, string> = {
  PENDING: 'bg-amber-50 text-amber-700 border border-amber-200',
  ACCEPTED: 'bg-green-50 text-green-700 border border-green-200',
  EXPIRED: 'bg-muted text-muted-foreground border border-border',
  CANCELLED: 'bg-red-50 text-red-600 border border-red-200',
};

const ROLE_BADGE: Record<string, string> = {
  ADMIN: 'bg-violet-50 text-violet-700 border border-violet-200',
  MANAGER: 'bg-blue-50 text-blue-700 border border-blue-200',
  MEMBER: 'bg-muted text-muted-foreground border border-border',
};

interface Props {
  companyId?: string;
}

function mapInvitation(invitation: ApiInvitation): Invitation {
  const expiresIn = invitation.expiresAt
    ? `${Math.max(0, Math.ceil((new Date(invitation.expiresAt).getTime() - Date.now()) / 86400000))} days`
    : '—';
  return {
    id: invitation.id,
    email: invitation.email,
    role: invitation.role,
    department: invitation.departmentId ?? '—',
    invitedBy: '—',
    invitedDate: invitation.createdAt ? new Date(invitation.createdAt).toLocaleDateString() : '—',
    expiresIn,
    status: invitation.status,
  };
}

export default function InvitationsTab({ companyId }: Props) {
  const [statusFilter, setStatusFilter] = useState<string>('ALL');
  const [invitations, setInvitations] = useState<Invitation[]>([]);

  const fetchInvitations = useCallback(async () => {
    if (!companyId) {
      setInvitations([]);
      return;
    }
    try {
      const response = await organizationApi.listInvitations(companyId);
      setInvitations(response.data.map(mapInvitation));
    } catch (err) {
      toast.error(err instanceof AxiosError && !err.response
        ? 'Cannot reach the organization service.' :'Failed to load invitations');
      setInvitations([]);
    }
  }, [companyId]);

  useEffect(() => { fetchInvitations(); }, [fetchInvitations]);

  const filtered = statusFilter === 'ALL'
    ? invitations
    : invitations.filter(i => i.status === statusFilter);

  const handleResend = (_id: string, _email: string) => {
    toast.error('Resending invitations is not supported by the backend yet.');
  };

  const handleCancel = async (id: string, email: string) => {
    if (!companyId) return;
    try {
      await organizationApi.revokeInvitation(companyId, id);
      setInvitations(prev => prev.map(i => i.id === id ? { ...i, status: 'CANCELLED' } : i));
      toast.success(`Invitation to ${email} cancelled`);
    } catch (err) {
      toast.error(err instanceof AxiosError
        ? (err.response?.data?.message ?? 'Failed to cancel invitation')
        : 'Failed to cancel invitation');
    }
  };

  const pendingCount = invitations.filter(i => i.status === 'PENDING').length;

  return (
    <div className="space-y-4">
      {/* Summary bar */}
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
        {[
          { label: 'Pending', value: invitations.filter(i => i.status === 'PENDING').length, color: 'text-amber-600', bg: 'bg-amber-50 border-amber-200' },
          { label: 'Accepted', value: invitations.filter(i => i.status === 'ACCEPTED').length, color: 'text-green-600', bg: 'bg-green-50 border-green-200' },
          { label: 'Expired', value: invitations.filter(i => i.status === 'EXPIRED').length, color: 'text-muted-foreground', bg: 'bg-muted border-border' },
          { label: 'Cancelled', value: invitations.filter(i => i.status === 'CANCELLED').length, color: 'text-red-600', bg: 'bg-red-50 border-red-200' },
        ].map((s) => (
          <div key={`inv-stat-${s.label}`} className={`card-elevated p-3 border ${s.bg}`}>
            <div className={`text-2xl font-bold font-tabular ${s.color}`}>{s.value}</div>
            <div className="text-xs text-muted-foreground mt-0.5">{s.label}</div>
          </div>
        ))}
      </div>

      {/* Filter */}
      <div className="flex items-center gap-3">
        <div className="flex gap-1 p-1 bg-muted rounded-lg">
          {['ALL', 'PENDING', 'ACCEPTED', 'EXPIRED', 'CANCELLED'].map((s) => (
            <button
              key={`inv-filter-${s}`}
              onClick={() => setStatusFilter(s)}
              className={`px-3 py-1.5 rounded-md text-xs font-medium transition-all duration-150 ${
                statusFilter === s
                  ? 'bg-card text-foreground shadow-card'
                  : 'text-muted-foreground hover:text-foreground'
              }`}
            >
              {s === 'ALL' ? 'All' : s.charAt(0) + s.slice(1).toLowerCase()}
              {s === 'PENDING' && pendingCount > 0 && (
                <span className="ml-1.5 bg-amber-100 text-amber-700 text-xs px-1 rounded-full font-tabular">{pendingCount}</span>
              )}
            </button>
          ))}
        </div>
      </div>

      {/* Table */}
      <div className="card-elevated overflow-hidden">
        <div className="overflow-x-auto scrollbar-thin">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-border bg-muted/40">
                <th className="px-4 py-3 text-left text-xs font-semibold text-muted-foreground uppercase tracking-wider">Recipient</th>
                <th className="px-4 py-3 text-left text-xs font-semibold text-muted-foreground uppercase tracking-wider">Role</th>
                <th className="px-4 py-3 text-left text-xs font-semibold text-muted-foreground uppercase tracking-wider">Department</th>
                <th className="px-4 py-3 text-left text-xs font-semibold text-muted-foreground uppercase tracking-wider">Invited By</th>
                <th className="px-4 py-3 text-left text-xs font-semibold text-muted-foreground uppercase tracking-wider">Sent</th>
                <th className="px-4 py-3 text-left text-xs font-semibold text-muted-foreground uppercase tracking-wider">Expires</th>
                <th className="px-4 py-3 text-left text-xs font-semibold text-muted-foreground uppercase tracking-wider">Status</th>
                <th className="px-4 py-3 w-24" />
              </tr>
            </thead>
            <tbody>
              {filtered.length === 0 ? (
                <tr>
                  <td colSpan={8} className="px-4 py-12 text-center">
                    <div className="flex flex-col items-center gap-2">
                      <Mail size={24} className="text-muted-foreground/40" />
                      <p className="text-sm font-medium text-muted-foreground">No invitations found</p>
                      <p className="text-xs text-muted-foreground">Invite team members to grow your organization</p>
                    </div>
                  </td>
                </tr>
              ) : (
                filtered.map((inv, i) => (
                  <tr
                    key={inv.id}
                    className={`border-b border-border last:border-0 hover:bg-muted/40 transition-colors group ${i % 2 === 1 ? 'bg-muted/10' : ''}`}
                  >
                    <td className="px-4 py-3">
                      <div className="flex items-center gap-2">
                        <div className="w-7 h-7 rounded-full bg-muted flex items-center justify-center flex-shrink-0">
                          <Mail size={12} className="text-muted-foreground" />
                        </div>
                        <span className="text-sm text-foreground font-medium">{inv.email}</span>
                      </div>
                    </td>
                    <td className="px-4 py-3">
                      <span className={`badge-base ${ROLE_BADGE[inv.role]}`}>{inv.role}</span>
                    </td>
                    <td className="px-4 py-3 text-sm text-muted-foreground">{inv.department}</td>
                    <td className="px-4 py-3 text-sm text-muted-foreground">{inv.invitedBy}</td>
                    <td className="px-4 py-3 text-sm text-muted-foreground">{inv.invitedDate}</td>
                    <td className="px-4 py-3">
                      {inv.status === 'PENDING' ? (
                        <div className="flex items-center gap-1 text-xs">
                          <Clock size={11} className={parseInt(inv.expiresIn) <= 2 ? 'text-red-500' : 'text-amber-500'} />
                          <span className={parseInt(inv.expiresIn) <= 2 ? 'text-red-600 font-medium' : 'text-muted-foreground'}>
                            {inv.expiresIn}
                          </span>
                        </div>
                      ) : (
                        <span className="text-xs text-muted-foreground">{inv.expiresIn}</span>
                      )}
                    </td>
                    <td className="px-4 py-3">
                      <span className={`badge-base ${STATUS_BADGE[inv.status]}`}>
                        {inv.status.charAt(0) + inv.status.slice(1).toLowerCase()}
                      </span>
                    </td>
                    <td className="px-4 py-3">
                      {inv.status === 'PENDING' && (
                        <div className="flex items-center gap-1 opacity-0 group-hover:opacity-100 transition-opacity">
                          <button
                            onClick={() => handleResend(inv.id, inv.email)}
                            title="Resend invitation"
                            className="w-7 h-7 rounded-md flex items-center justify-center text-muted-foreground hover:text-primary hover:bg-primary/10 transition-all"
                          >
                            <RotateCcw size={13} />
                          </button>
                          <button
                            onClick={() => handleCancel(inv.id, inv.email)}
                            title="Cancel invitation"
                            className="w-7 h-7 rounded-md flex items-center justify-center text-muted-foreground hover:text-red-500 hover:bg-red-50 transition-all"
                          >
                            <X size={13} />
                          </button>
                        </div>
                      )}
                      {inv.status === 'EXPIRED' && (
                        <button
                          onClick={() => handleResend(inv.id, inv.email)}
                          className="text-xs text-primary font-medium hover:underline opacity-0 group-hover:opacity-100 transition-opacity"
                        >
                          Resend
                        </button>
                      )}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}