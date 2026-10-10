'use client';
import React, { useState } from 'react';
import { useForm } from 'react-hook-form';
import { X, Loader2, GitBranch } from 'lucide-react';
import { toast } from 'sonner';
import { organizationApi } from '@/lib/api/organization';
import { AxiosError } from 'axios';

interface DeptFormData {
  name: string;
  description: string;
}

interface Props {
  onClose: () => void;
  companyId?: string;
  onCreated?: () => void;
}

export default function CreateDepartmentModal({ onClose, companyId, onCreated }: Props) {
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<DeptFormData>({
    defaultValues: { name: '', description: '' },
  });

  const onSubmit = async (data: DeptFormData) => {
    if (!companyId) {
      toast.error('Select a company before creating a department.');
      onClose();
      return;
    }

    try {
      await organizationApi.createDepartment(companyId, {
        name: data.name,
        description: data.description || undefined,
      });
      toast.success(`Department "${data.name}" created`);
      onCreated?.();
      onClose();
    } catch (err: unknown) {
      let message = 'Failed to create department. Please try again.';
      if (err instanceof AxiosError) {
        const apiMsg = err.response?.data?.message;
        if (apiMsg) message = apiMsg;
        else if (err.response?.status === 409) message = 'A department with this name already exists.';
        else if (!err.response) message = 'Cannot reach the server. Make sure the backend is running on localhost:8080.';
      }
      toast.error(message);
    }
  };

  return (
    <div className="fixed inset-0 z-[1100] flex items-center justify-center p-4">
      <div className="fixed inset-0 bg-black/40 fade-in" onClick={onClose} />
      <div className="relative max-h-[calc(100dvh-2rem)] w-full max-w-md overflow-y-auto rounded-2xl bg-card shadow-modal fade-scale-in">
        <div className="flex items-center justify-between p-6 border-b border-border">
          <div className="flex items-center gap-3">
            <div className="w-9 h-9 rounded-xl bg-primary/10 flex items-center justify-center">
              <GitBranch size={18} className="text-primary" />
            </div>
            <div>
              <h2 className="text-base font-bold text-foreground">Create Department</h2>
              <p className="text-xs text-muted-foreground">Add a new department to your company</p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="w-8 h-8 rounded-lg flex items-center justify-center text-muted-foreground hover:text-foreground hover:bg-muted transition-all"
          >
            <X size={16} />
          </button>
        </div>

        <form onSubmit={handleSubmit(onSubmit)} className="p-6 space-y-4">
          <div>
            <label className="block text-sm font-medium text-foreground mb-1.5" htmlFor="dept-name">
              Department name
            </label>
            <input
              id="dept-name"
              type="text"
              placeholder="e.g. Engineering, Product, QA"
              className={`input-base ${errors.name ? 'input-error' : ''}`}
              {...register('name', { required: 'Department name is required', minLength: { value: 2, message: 'At least 2 characters' } })}
            />
            {errors.name && <p className="mt-1 text-xs text-red-500">{errors.name.message}</p>}
          </div>

          <div>
            <label className="block text-sm font-medium text-foreground mb-1.5" htmlFor="dept-desc">
              Description <span className="text-muted-foreground font-normal">(optional)</span>
            </label>
            <textarea
              id="dept-desc"
              rows={3}
              placeholder="Describe the department's responsibilities…"
              className="input-base resize-none"
              {...register('description')}
            />
          </div>

          <div className="flex items-center gap-3 pt-2">
            <button type="button" onClick={onClose} className="btn-secondary flex-1 py-2.5 rounded-lg font-medium text-sm">
              Cancel
            </button>
            <button
              type="submit"
              disabled={isSubmitting}
              className="btn-primary flex-1 py-2.5 rounded-lg font-semibold text-sm flex items-center justify-center gap-2 disabled:opacity-60"
              style={{ minHeight: '42px' }}
            >
              {isSubmitting ? (
                <><Loader2 size={14} className="animate-spin" /><span>Creating…</span></>
              ) : (
                <><GitBranch size={14} /><span>Create Department</span></>
              )}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}