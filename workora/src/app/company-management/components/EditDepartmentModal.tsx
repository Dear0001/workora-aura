'use client';
import React from 'react';
import { X, Loader2, Edit2 } from 'lucide-react';
import { useForm } from 'react-hook-form';
import { toast } from 'sonner';
import { organizationApi } from '@/lib/api/organization';
import { AxiosError } from 'axios';
import type { Department } from '@/lib/api/types';

interface FormData {
  name: string;
  description: string;
}

interface Props {
  department: Department;
  onClose: () => void;
  onUpdated: (dept: Department) => void;
}

export default function EditDepartmentModal({ department, onClose, onUpdated }: Props) {
  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm<FormData>({
    defaultValues: { name: department.name ?? '', description: department.description ?? '' },
  });

  const onSubmit = async (data: FormData) => {
    try {
      const res = await organizationApi.updateDepartment(department.id, {
        name: data.name,
        description: data.description || undefined,
      });
      toast.success(`Department "${res.data.name}" updated`);
      onUpdated(res.data);
    } catch (err) {
      const msg = err instanceof AxiosError
        ? (err.response?.data?.message ?? (!err.response ? 'Cannot reach the organization service.' : 'Failed to update department'))
        : 'Failed to update department';
      toast.error(msg);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      <div className="fixed inset-0 bg-black/40 fade-in" onClick={onClose} />
      <div className="relative bg-card rounded-2xl shadow-modal w-full max-w-md fade-scale-in">
        <div className="flex items-center justify-between p-6 border-b border-border">
          <div className="flex items-center gap-3">
            <div className="w-9 h-9 rounded-xl bg-primary/10 flex items-center justify-center">
              <Edit2 size={18} className="text-primary" />
            </div>
            <div>
              <h2 className="text-base font-bold text-foreground">Edit Department</h2>
              <p className="text-xs text-muted-foreground">Update department details</p>
            </div>
          </div>
          <button onClick={onClose} className="w-8 h-8 rounded-lg flex items-center justify-center text-muted-foreground hover:text-foreground hover:bg-muted transition-all">
            <X size={16} />
          </button>
        </div>
        <form onSubmit={handleSubmit(onSubmit)} className="p-6 space-y-4">
          <div>
            <label className="block text-sm font-medium text-foreground mb-1.5">Department Name *</label>
            <input
              type="text"
              className={`input-base ${errors.name ? 'input-error' : ''}`}
              {...register('name', { required: 'Department name is required' })}
            />
            {errors.name && <p className="mt-1 text-xs text-red-500">{errors.name.message}</p>}
          </div>
          <div>
            <label className="block text-sm font-medium text-foreground mb-1.5">Description</label>
            <textarea rows={3} className="input-base resize-none" {...register('description')} />
          </div>
          <div className="flex items-center gap-3 pt-2">
            <button type="button" onClick={onClose} className="btn-secondary flex-1 py-2.5 rounded-lg font-medium text-sm">Cancel</button>
            <button type="submit" disabled={isSubmitting} className="btn-primary flex-1 py-2.5 rounded-lg font-semibold text-sm flex items-center justify-center gap-2 disabled:opacity-60">
              {isSubmitting ? <><Loader2 size={14} className="animate-spin" /><span>Saving…</span></> : <><Edit2 size={14} /><span>Save Changes</span></>}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
