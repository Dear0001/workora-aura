'use client';
import React, { useEffect, useState } from 'react';
import { X, Loader2, Edit2, Upload, Image as ImageIcon } from 'lucide-react';
import { useForm, type UseFormRegisterReturn } from 'react-hook-form';
import { toast } from 'sonner';
import { organizationApi, storageApi } from '@/lib/api/organization';
import { AxiosError } from 'axios';
import type { Company } from '@/lib/api/types';

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
  company: Company;
  onClose: () => void;
  onUpdated: (company: Company) => void;
}

export default function EditCompanyModal({ company, onClose, onUpdated }: Props) {
  const { register, watch, handleSubmit, formState: { errors, isSubmitting } } = useForm<FormData>({
    defaultValues: {
      name: company.name ?? '',
      description: company.description ?? '',
      contactEmail: company.contactEmail ?? '',
      contactPhone: company.contactPhone ?? '',
      address: company.address ?? '',
    },
  });
  const profileFile = watch('profileImage')?.[0];
  const coverFile = watch('coverImage')?.[0];
  const [profilePreview, setProfilePreview] = useState(company.profileImageUrl ?? '');
  const [coverPreview, setCoverPreview] = useState(company.coverImageUrl ?? '');
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
  useEffect(() => {
    Promise.all([
      company.profileImageUrl ? storageApi.getDownloadUrl(company.profileImageUrl) : Promise.resolve(''),
      company.coverImageUrl ? storageApi.getDownloadUrl(company.coverImageUrl) : Promise.resolve(''),
    ]).then(([profile, cover]) => {
      setProfilePreview(profile);
      setCoverPreview(cover);
    }).catch(() => undefined);
  }, [company.coverImageUrl, company.profileImageUrl]);

  const onSubmit = async (data: FormData) => {
    try {
      const [profile, cover] = await Promise.all([
        profileFile ? storageApi.createUpload(profileFile) : Promise.resolve(null),
        coverFile ? storageApi.createUpload(coverFile) : Promise.resolve(null),
      ]);
      const res = await organizationApi.updateCompany(company.id, {
        name: data.name,
        description: data.description || undefined,
        contactEmail: data.contactEmail || undefined,
        contactPhone: data.contactPhone || undefined,
        address: data.address || undefined,
        profileImageUrl: profile?.data.key,
        coverImageUrl: cover?.data.key,
      });
      toast.success('Company updated');
      onUpdated(res.data);
    } catch (err) {
      const msg = err instanceof AxiosError
        ? (err.response?.data?.message ?? (!err.response ? 'Cannot reach the organization service.' : 'Failed to update company'))
        : 'Failed to update company';
      toast.error(msg);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      <div className="fixed inset-0 bg-black/40 fade-in" onClick={onClose} />
      <div className="relative bg-card rounded-2xl shadow-modal w-full max-w-lg fade-scale-in">
        <div className="flex items-center justify-between p-6 border-b border-border">
          <div className="flex items-center gap-3">
            <div className="w-9 h-9 rounded-xl bg-primary/10 flex items-center justify-center">
              <Edit2 size={18} className="text-primary" />
            </div>
            <div>
              <h2 className="text-base font-bold text-foreground">Edit Company</h2>
              <p className="text-xs text-muted-foreground">Update company profile</p>
            </div>
          </div>
          <button onClick={onClose} className="w-8 h-8 rounded-lg flex items-center justify-center text-muted-foreground hover:text-foreground hover:bg-muted transition-all">
            <X size={16} />
          </button>
        </div>
        <form onSubmit={handleSubmit(onSubmit)} className="p-6 space-y-4">
          <div>
            <label className="block text-sm font-medium text-foreground mb-1.5">Company Name *</label>
            <input type="text" className={`input-base ${errors.name ? 'input-error' : ''}`} {...register('name', { required: 'Company name is required' })} />
            {errors.name && <p className="mt-1 text-xs text-red-500">{errors.name.message}</p>}
          </div>
          <div>
            <label className="block text-sm font-medium text-foreground mb-1.5">Description</label>
            <textarea rows={3} className="input-base resize-none" {...register('description')} />
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-sm font-medium text-foreground mb-1.5">Contact Email</label>
              <input type="email" className="input-base" {...register('contactEmail')} />
            </div>
            <div>
              <label className="block text-sm font-medium text-foreground mb-1.5">Phone</label>
              <input type="text" className="input-base" {...register('contactPhone')} />
            </div>
          </div>
          <div>
            <label className="block text-sm font-medium text-foreground mb-1.5">Address</label>
            <input type="text" className="input-base" {...register('address')} />
          </div>
          <ImageField label="Profile image / logo" preview={profilePreview} registration={register('profileImage')} />
          <ImageField label="Cover image" preview={coverPreview} registration={register('coverImage')} />
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
