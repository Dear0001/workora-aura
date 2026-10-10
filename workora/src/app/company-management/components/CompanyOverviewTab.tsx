import React, { useEffect, useState } from 'react';
import { MapPin, Globe, Calendar, Building2, Star, Edit2, Trash2 } from 'lucide-react';
import type { Company } from '@/lib/api/types';
import { storageApi } from '@/lib/api/organization';

interface Props {
  company: Company;
  onEdit?: () => void;
  onDelete?: () => void;
}

export default function CompanyOverviewTab({ company, onEdit, onDelete }: Props) {
  const rating = company.averageRating ?? 0;
  const [profileImage, setProfileImage] = useState('');
  const [coverImage, setCoverImage] = useState('');
  useEffect(() => {
    Promise.all([
      company.profileImageUrl ? storageApi.getDownloadUrl(company.profileImageUrl) : Promise.resolve(''),
      company.coverImageUrl ? storageApi.getDownloadUrl(company.coverImageUrl) : Promise.resolve(''),
    ]).then(([profile, cover]) => {
      setProfileImage(profile);
      setCoverImage(cover);
    }).catch(() => undefined);
  }, [company.coverImageUrl, company.profileImageUrl]);

  return (
    <div className="grid grid-cols-1 xl:grid-cols-3 gap-6">
      {coverImage && <img src={coverImage} alt={`${company.name} cover`} className="xl:col-span-3 h-40 w-full rounded-2xl object-cover" />}
      <div className="xl:col-span-2 card-elevated p-6">
        <div className="flex items-start justify-between mb-5">
          <div className="flex items-center gap-4">
            {profileImage ? <img src={profileImage} alt={`${company.name} logo`} className="w-14 h-14 rounded-xl object-cover flex-shrink-0" /> : <div className="w-14 h-14 rounded-xl bg-primary/10 flex items-center justify-center flex-shrink-0">
              <Building2 size={24} className="text-primary" />
            </div>}
            <div>
              <h2 className="text-xl font-bold text-foreground">{company.name}</h2>
              <div className="flex items-center gap-1 mt-1">
                {[1, 2, 3, 4, 5].map((star) => (
                  <Star
                    key={star}
                    size={12}
                    className={star <= Math.floor(rating) ? 'text-amber-400 fill-amber-400' : 'text-border'}
                  />
                ))}
                <span className="text-xs text-muted-foreground ml-1">
                  {rating.toFixed(1)} ({company.ratingCount ?? 0} ratings)
                </span>
              </div>
            </div>
          </div>
          <div className="flex items-center gap-2">
            {onEdit && <button
              type="button"
              onClick={onEdit}
              className="flex items-center gap-1.5 text-xs text-muted-foreground bg-muted hover:bg-primary/10 hover:text-primary px-3 py-1.5 rounded-lg transition-all"
            >
              <Edit2 size={12} />
              Edit Profile
            </button>}
            {onDelete && (
              <button
                type="button"
                onClick={onDelete}
                className="flex items-center gap-1.5 text-xs text-red-600 bg-red-50 hover:bg-red-100 px-3 py-1.5 rounded-lg transition-all"
              >
                <Trash2 size={12} />
                Delete
              </button>
            )}
          </div>
        </div>

        <p className="text-sm text-muted-foreground leading-relaxed mb-5">
          {company.description || 'No company description has been provided.'}
        </p>

        <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 text-sm">
          <div className="flex items-center gap-2 text-muted-foreground">
            <MapPin size={14} className="text-primary flex-shrink-0" />
            <span>{company.address || 'No address provided'}</span>
          </div>
          <div className="flex items-center gap-2 text-muted-foreground">
            <Globe size={14} className="text-primary flex-shrink-0" />
            <span>{company.contactEmail || 'No contact email'}</span>
          </div>
          <div className="flex items-center gap-2 text-muted-foreground">
            <Calendar size={14} className="text-primary flex-shrink-0" />
            <span>Created {company.createdAt ? new Date(company.createdAt).toLocaleDateString() : '—'}</span>
          </div>
        </div>
      </div>
    </div>
  );
}
