'use client';
import React from 'react';
import { useForm } from 'react-hook-form';
import { X, Loader2, UserPlus } from 'lucide-react';
import { toast } from 'sonner';
import { organizationApi } from '@/lib/api/organization';
import { AxiosError } from 'axios';

interface InviteFormData {
  email: string;
  role: 'ADMIN' | 'MANAGER' | 'MEMBER';
  message: string;
}

interface Props {
  onClose: () => void;
  companyId?: string;
}

export default function InviteMemberModal({ onClose, companyId }: Props) {
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<InviteFormData>({
    defaultValues: { email: '', role: 'MEMBER', message: '' },
  });

  const onSubmit = async (data: InviteFormData) => {
    if (!companyId) {
      toast.error('Select a company before sending an invitation.');
      onClose();
      return;
    }

    try {
      await organizationApi.inviteMember(companyId, {
        email: data.email,
        role: data.role,
        message: data.message || undefined,
      });
      toast.success(`Invitation sent to ${data.email}`);
      onClose();
    } catch (err: unknown) {
      let message = 'Failed to send invitation. Please try again.';
      if (err instanceof AxiosError) {
        const apiMsg = err.response?.data?.message;
        if (apiMsg) message = apiMsg;
        else if (err.response?.status === 409) message = 'This user is already a member or has a pending invitation.';
        else if (!err.response) message = 'Cannot reach the server. Make sure the backend is running on localhost:8080.';
      }
      toast.error(message);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      <div className="fixed inset-0 bg-black/40 fade-in" onClick={onClose} />
      <div className="relative bg-card rounded-2xl shadow-modal w-full max-w-md fade-scale-in">
        {/* Header */}
        <div className="flex items-center justify-between p-6 border-b border-border">
          <div className="flex items-center gap-3">
            <div className="w-9 h-9 rounded-xl bg-primary/10 flex items-center justify-center">
              <UserPlus size={18} className="text-primary" />
            </div>
            <div>
              <h2 className="text-base font-bold text-foreground">Invite Team Member</h2>
              <p className="text-xs text-muted-foreground">Send an invitation to join your company</p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="w-8 h-8 rounded-lg flex items-center justify-center text-muted-foreground hover:text-foreground hover:bg-muted transition-all"
          >
            <X size={16} />
          </button>
        </div>

        {/* Form */}
        <form onSubmit={handleSubmit(onSubmit)} className="p-6 space-y-4">
          <div>
            <label className="block text-sm font-medium text-foreground mb-1.5" htmlFor="inv-email">
              Email address
            </label>
            <p className="text-xs text-muted-foreground mb-1.5">The invitation will be sent to this email</p>
            <input
              id="inv-email"
              type="email"
              placeholder="colleague@company.com"
              className={`input-base ${errors.email ? 'input-error' : ''}`}
              {...register('email', {
                required: 'Email is required',
                pattern: { value: /^[^\s@]+@[^\s@]+\.[^\s@]+$/, message: 'Enter a valid email address' },
              })}
            />
            {errors.email && <p className="mt-1 text-xs text-red-500">{errors.email.message}</p>}
          </div>

          <div>
            <label className="block text-sm font-medium text-foreground mb-1.5" htmlFor="inv-role">
              Role
            </label>
            <select
              id="inv-role"
              className="input-base"
              {...register('role', { required: true })}
            >
              <option value="MEMBER">Member</option>
              <option value="MANAGER">Manager</option>
              <option value="ADMIN">Admin</option>
            </select>
          </div>

          <div>
            <label className="block text-sm font-medium text-foreground mb-1.5" htmlFor="inv-message">
              Personal message <span className="text-muted-foreground font-normal">(optional)</span>
            </label>
            <textarea
              id="inv-message"
              rows={3}
              placeholder="Add a personal note to your invitation…"
              className="input-base resize-none"
              {...register('message')}
            />
          </div>

          {/* Footer */}
          <div className="flex items-center gap-3 pt-2">
            <button
              type="button"
              onClick={onClose}
              className="btn-secondary flex-1 py-2.5 rounded-lg font-medium text-sm"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={isSubmitting}
              className="btn-primary flex-1 py-2.5 rounded-lg font-semibold text-sm flex items-center justify-center gap-2 disabled:opacity-60"
              style={{ minHeight: '42px' }}
            >
              {isSubmitting ? (
                <><Loader2 size={14} className="animate-spin" /><span>Sending…</span></>
              ) : (
                <><UserPlus size={14} /><span>Send Invitation</span></>
              )}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}