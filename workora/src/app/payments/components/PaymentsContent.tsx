'use client';
import React, { useCallback, useEffect, useState } from 'react';
import { CreditCard, Loader2, Edit2, Trash2, QrCode, CheckCircle, AlertCircle } from 'lucide-react';
import { toast } from 'sonner';
import { AxiosError } from 'axios';
import { paymentsApi, type BakongProfile, type FeePayment } from '@/lib/api/payments';

type Tab = 'profile' | 'payments';

export default function PaymentsContent() {
  const [activeTab, setActiveTab] = useState<Tab>('profile');
  const [profile, setProfile] = useState<BakongProfile | null>(null);
  const [loadingProfile, setLoadingProfile] = useState(true);
  const [editingProfile, setEditingProfile] = useState(false);
  const [walletName, setWalletName] = useState('');
  const [phoneNumber, setPhoneNumber] = useState('');
  const [savingProfile, setSavingProfile] = useState(false);

  const loadProfile = useCallback(async () => {
    setLoadingProfile(true);
    try {
      const res = await paymentsApi.getBakongProfile();
      setProfile(res.data);
    } catch (err) {
      if (err instanceof AxiosError && err.response?.status === 404) {
        setProfile(null);
      } else if (err instanceof AxiosError && !err.response) {
        toast.error('Cannot reach the payment service.');
      }
    } finally {
      setLoadingProfile(false);
    }
  }, []);

  useEffect(() => { loadProfile(); }, [loadProfile]);

  const handleCreateProfile = async () => {
    if (!walletName.trim() || !phoneNumber.trim()) return;
    setSavingProfile(true);
    try {
      const res = await paymentsApi.createBakongProfile({ walletName: walletName.trim(), phoneNumber: phoneNumber.trim(), isPrimary: true });
      setProfile(res.data);
      setEditingProfile(false);
      toast.success('Bakong profile created');
    } catch (err) {
      toast.error(err instanceof AxiosError ? (err.response?.data?.message ?? 'Failed to create profile') : 'Failed to create profile');
    } finally {
      setSavingProfile(false);
    }
  };

  const handleUpdateProfile = async () => {
    if (!walletName.trim() || !phoneNumber.trim()) return;
    setSavingProfile(true);
    try {
      const res = await paymentsApi.updateBakongProfile({ walletName: walletName.trim(), phoneNumber: phoneNumber.trim() });
      setProfile(res.data);
      setEditingProfile(false);
      toast.success('Bakong profile updated');
    } catch (err) {
      toast.error(err instanceof AxiosError ? (err.response?.data?.message ?? 'Failed to update profile') : 'Failed to update profile');
    } finally {
      setSavingProfile(false);
    }
  };

  const handleDeleteProfile = async () => {
    try {
      await paymentsApi.deleteBakongProfile();
      setProfile(null);
      toast.success('Bakong profile deleted');
    } catch (err) {
      toast.error(err instanceof AxiosError ? (err.response?.data?.message ?? 'Failed to delete profile') : 'Failed to delete profile');
    }
  };

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-foreground">Payments</h1>
        <p className="text-muted-foreground text-sm mt-0.5">Manage your Bakong payment profile and fee payments</p>
      </div>

      {/* Tabs */}
      <div className="flex gap-1 border-b border-border">
        {[
          { id: 'profile' as Tab, label: 'Bakong Profile', icon: QrCode },
          { id: 'payments' as Tab, label: 'Fee Payments', icon: CreditCard },
        ].map(tab => (
          <button
            key={tab.id}
            onClick={() => setActiveTab(tab.id)}
            className={`flex items-center gap-2 px-4 py-3 text-sm font-medium border-b-2 transition-all duration-150 -mb-px ${
              activeTab === tab.id ? 'border-primary text-primary' : 'border-transparent text-muted-foreground hover:text-foreground hover:border-border'
            }`}
          >
            <tab.icon size={15} />
            {tab.label}
          </button>
        ))}
      </div>

      {/* Bakong Profile Tab */}
      {activeTab === 'profile' && (
        <div className="max-w-lg space-y-4">
          {loadingProfile ? (
            <div className="flex items-center gap-2 text-sm text-muted-foreground"><Loader2 size={14} className="animate-spin" />Loading profile…</div>
          ) : profile && !editingProfile ? (
            <div className="card-elevated p-6">
              <div className="flex items-start justify-between mb-4">
                <div className="flex items-center gap-3">
                  <div className="w-12 h-12 rounded-xl bg-primary/10 flex items-center justify-center">
                    <QrCode size={22} className="text-primary" />
                  </div>
                  <div>
                    <h2 className="font-bold text-foreground">{profile.walletName}</h2>
                    <p className="text-sm text-muted-foreground">{profile.phoneNumber}</p>
                  </div>
                </div>
                <div className="flex items-center gap-2">
                  <button
                    onClick={() => { setWalletName(profile.walletName); setPhoneNumber(profile.phoneNumber); setEditingProfile(true); }}
                    className="w-8 h-8 rounded-lg flex items-center justify-center text-muted-foreground hover:text-primary hover:bg-primary/10 transition-all"
                  >
                    <Edit2 size={14} />
                  </button>
                  <button onClick={handleDeleteProfile} className="w-8 h-8 rounded-lg flex items-center justify-center text-muted-foreground hover:text-red-500 hover:bg-red-50 transition-all">
                    <Trash2 size={14} />
                  </button>
                </div>
              </div>
              <div className="flex items-center gap-2">
                <span className={`badge-base text-xs ${profile.status === 'ACTIVE' ? 'bg-green-50 text-green-700 border border-green-200' : 'bg-muted text-muted-foreground border border-border'}`}>
                  {profile.status ?? 'ACTIVE'}
                </span>
                {profile.isPrimary && <span className="badge-base text-xs bg-primary/10 text-primary border border-primary/20">Primary</span>}
              </div>
            </div>
          ) : (
            <div className="card-elevated p-6 space-y-4">
              <div className="flex items-center gap-3 mb-2">
                <div className="w-9 h-9 rounded-xl bg-primary/10 flex items-center justify-center">
                  <QrCode size={18} className="text-primary" />
                </div>
                <div>
                  <h2 className="font-bold text-foreground text-sm">{profile ? 'Edit Bakong Profile' : 'Set Up Bakong Profile'}</h2>
                  <p className="text-xs text-muted-foreground">Link your Bakong wallet for payments</p>
                </div>
              </div>
              <div>
                <label className="block text-sm font-medium text-foreground mb-1.5">Wallet Name *</label>
                <input type="text" value={walletName} onChange={e => setWalletName(e.target.value)} placeholder="Your full name" className="input-base" />
              </div>
              <div>
                <label className="block text-sm font-medium text-foreground mb-1.5">Phone Number *</label>
                <input type="text" value={phoneNumber} onChange={e => setPhoneNumber(e.target.value)} placeholder="+85512345678" className="input-base" />
              </div>
              <div className="flex items-center gap-3">
                {editingProfile && (
                  <button onClick={() => setEditingProfile(false)} className="btn-secondary flex-1 py-2.5 rounded-lg font-medium text-sm">Cancel</button>
                )}
                <button
                  onClick={profile ? handleUpdateProfile : handleCreateProfile}
                  disabled={savingProfile || !walletName.trim() || !phoneNumber.trim()}
                  className="btn-primary flex-1 py-2.5 rounded-lg font-semibold text-sm flex items-center justify-center gap-2 disabled:opacity-60"
                >
                  {savingProfile ? <><Loader2 size={14} className="animate-spin" /><span>Saving…</span></> : <><QrCode size={14} /><span>{profile ? 'Save Changes' : 'Create Profile'}</span></>}
                </button>
              </div>
            </div>
          )}

          {!profile && !loadingProfile && (
            <div className="flex items-start gap-3 p-4 bg-amber-50 border border-amber-200 rounded-xl">
              <AlertCircle size={16} className="text-amber-600 flex-shrink-0 mt-0.5" />
              <p className="text-sm text-amber-700">You need a Bakong profile to receive payments for bug-fix jobs. Set up your wallet above.</p>
            </div>
          )}
        </div>
      )}

      {/* Fee Payments Tab */}
      {activeTab === 'payments' && (
        <div className="space-y-4">
          <div className="card-elevated p-8 text-center">
            <CreditCard size={36} className="text-muted-foreground/30 mx-auto mb-3" />
            <p className="text-sm font-medium text-muted-foreground">Fee payments are linked to bug-fix jobs</p>
            <p className="text-xs text-muted-foreground mt-1">When a bug-fix job is funded, the payment will appear here for verification and confirmation.</p>
            <div className="mt-4 flex items-center justify-center gap-2 text-xs text-muted-foreground">
              <CheckCircle size={13} className="text-green-500" />
              <span>Bakong QR payment integration ready</span>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
