'use client';
import React, { useEffect, useState } from 'react';
import { X, Loader2, Building2, Upload, Image as ImageIcon } from 'lucide-react';
import { useForm, type UseFormRegisterReturn } from 'react-hook-form';
import { toast } from 'sonner';
import { organizationApi, storageApi } from '@/lib/api/organization';
import type { Company } from '@/lib/api/types';
import { AxiosError } from 'axios';

interface FormData {
  name: string;
  description: string;
  contactEmail: string;
  contactPhone: string;
  address: string;
  profileImage?: FileList;
  coverImage?: FileList;
}

interface Props {
  onClose: () => void;
  onCreated: (company: Company) => void;
}

export default function CreateCompanyModal({ onClose, onCreated }: Props) {
  const { register, watch, handleSubmit, formState: { errors, isSubmitting } } = useForm<FormData>({
    defaultValues: { name: '', description: '', contactEmail: '', contactPhone: '', address: '' },
  });
  const profileFile = watch('profileImage')?.[0];
  const coverFile = watch('coverImage')?.[0];
  const [profilePreview, setProfilePreview] = useState<string>();
  const [coverPreview, setCoverPreview] = useState<string>();
  useEffect(() => {
    if (!profileFile) return;
    const url = URL.createObjectURL(profileFile);
    setProfilePreview(url);
    return () => URL.revokeObjectURL(url);
  }, [profileFile]);
  useEffect(() => {
    if (!coverFile) return;
    const url = URL.createObjectURL(coverFile);
    setCoverPreview(url);
    return () => URL.revokeObjectURL(url);
  }, [coverFile]);

  const onSubmit = async (data: FormData) => {
    try {
      const [profileUpload, coverUpload] = await Promise.allSettled([
        profileFile ? storageApi.createUpload(profileFile) : Promise.resolve(null),
        coverFile ? storageApi.createUpload(coverFile) : Promise.resolve(null),
      ]);

      const payload: Partial<Company> = {
        name: data.name,
        description: data.description || undefined,
        contactEmail: data.contactEmail || undefined,
        contactPhone: data.contactPhone || undefined,
        address: data.address || undefined,
      };

      if (profileFile) {
        payload.profileImageUrl =
          profileUpload.status === 'fulfilled' && profileUpload.value
            ? profileUpload.value.data.key
            : undefined;
      }

      if (coverFile) {
        payload.coverImageUrl =
          coverUpload.status === 'fulfilled' && coverUpload.value
            ? coverUpload.value.data.key
            : undefined;
      }

      const res = await organizationApi.createCompany(payload);

      if (profileUpload.status === 'rejected' || coverUpload.status === 'rejected') {
        toast.warning('Company created, but one image could not be uploaded and was left out.');
      } else {
        toast.success(`Company "${res.data.name}" created`);
      }
      onCreated(res.data);
    } catch (err) {
      const msg = err instanceof AxiosError
        ? (err.response?.data?.message ??
            (err.response?.status === 402 || err.response?.status === 403
              ? 'Your current plan does not allow creating another company.'
              : !err.response
                ? 'Cannot reach the organization service.'
                : 'Failed to create company'))
        : 'Failed to create company';
      toast.error(msg);
    }
  };

  return (
    <div className="fixed inset-0 z-[1100] flex items-center justify-center p-4">
      <div className="fixed inset-0 bg-black/40 fade-in" onClick={onClose} />
      <div className="relative max-h-[calc(100dvh-2rem)] w-full max-w-lg overflow-y-auto rounded-2xl bg-card shadow-modal fade-scale-in">
        <div className="flex items-center justify-between p-6 border-b border-border">
          <div className="flex items-center gap-3">
            <div className="w-9 h-9 rounded-xl bg-primary/10 flex items-center justify-center">
              <Building2 size={18} className="text-primary" />
            </div>
            <div>
              <h2 className="text-base font-bold text-foreground">Create Company</h2>
              <p className="text-xs text-muted-foreground">Set up your organization</p>
            </div>
          </div>
          <button onClick={onClose} className="w-8 h-8 rounded-lg flex items-center justify-center text-muted-foreground hover:text-foreground hover:bg-muted transition-all">
            <X size={16} />
          </button>
        </div>
        <form onSubmit={handleSubmit(onSubmit)} className="p-6 space-y-4">
          <div className="grid gap-3 sm:grid-cols-2">
            <ImageField label="Profile image / logo" preview={profilePreview} registration={register('profileImage')} />
            <ImageField label="Cover image" preview={coverPreview} registration={register('coverImage')} />
          </div>
          <div>
            <label className="block text-sm font-medium text-foreground mb-1.5">Company Name *</label>
            <input
              type="text"
              placeholder="Acme Corp"
              className={`input-base ${errors.name ? 'input-error' : ''}`}
              {...register('name', { required: 'Company name is required' })}
            />
            {errors.name && <p className="mt-1 text-xs text-red-500">{errors.name.message}</p>}
          </div>
          <div>
            <label className="block text-sm font-medium text-foreground mb-1.5">Description</label>
            <textarea rows={3} placeholder="What does your company do?" className="input-base resize-none" {...register('description')} />
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-sm font-medium text-foreground mb-1.5">Contact Email</label>
              <input type="email" placeholder="contact@company.com" className="input-base" {...register('contactEmail')} />
            </div>
            <div>
              <label className="block text-sm font-medium text-foreground mb-1.5">Phone</label>
              <input type="text" placeholder="+1 555 000 0000" className="input-base" {...register('contactPhone')} />
            </div>
          </div>
          <div>
            <label className="block text-sm font-medium text-foreground mb-1.5">Address</label>
            <input type="text" placeholder="123 Main St, City, Country" className="input-base" {...register('address')} />
          </div>
          <div className="flex items-center gap-3 pt-2">
            <button type="button" onClick={onClose} className="btn-secondary flex-1 py-2.5 rounded-lg font-medium text-sm">Cancel</button>
            <button type="submit" disabled={isSubmitting} className="btn-primary flex-1 py-2.5 rounded-lg font-semibold text-sm flex items-center justify-center gap-2 disabled:opacity-60">
              {isSubmitting ? <><Loader2 size={14} className="animate-spin" /><span>Creating…</span></> : <><Building2 size={14} /><span>Create Company</span></>}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

function ImageField({ label, preview, registration }: {
  label: string;
  preview?: string;
  registration: UseFormRegisterReturn;
}) {
  return (
    <div>
      <label className="block text-sm font-medium text-foreground mb-1.5">{label}</label>
      <div className="flex items-center gap-3">
        {preview ? <img src={preview} alt={`${label} preview`} className="h-14 w-14 rounded-lg object-cover border border-border" /> : <ImageIcon size={24} className="text-muted-foreground" />}
        <label className="btn-secondary inline-flex items-center gap-2 px-3 py-2 rounded-lg text-sm cursor-pointer">
          <Upload size={14} /> Choose image
          <input type="file" accept="image/*" className="hidden" {...registration} />
        </label>
      </div>
    </div>
  );
}
