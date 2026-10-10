'use client';
import React, { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import CompanyOverviewTab from './CompanyOverviewTab';
import MembersTab from './MembersTab';
import DepartmentsTab from './DepartmentsTab';
import InvitationsTab from './InvitationsTab';
import InviteMemberModal from './InviteMemberModal';
import CreateDepartmentModal from './CreateDepartmentModal';
import CreateCompanyModal from './CreateCompanyModal';
import EditCompanyModal from './EditCompanyModal';
import { Building2, Users, GitBranch, Mail, Plus, CreditCard, X } from 'lucide-react';
import { organizationApi } from '@/lib/api/organization';
import type { Company } from '@/lib/api/types';
import { AxiosError } from 'axios';
import { toast } from 'sonner';
import { getStoredUser, isAuthenticated } from '@/lib/auth';

const MAX_COMPANIES_IN_FREE_PLAN = 1;

type Tab = 'overview' | 'members' | 'departments' | 'invitations';

const TABS: { id: Tab; label: string; icon: React.ElementType }[] = [
  { id: 'overview', label: 'Overview', icon: Building2 },
  { id: 'members', label: 'Members', icon: Users },
  { id: 'departments', label: 'Departments', icon: GitBranch },
  { id: 'invitations', label: 'Invitations', icon: Mail },
];

export default function CompanyManagementContent() {
  const [activeTab, setActiveTab] = useState<Tab>('overview');
  const [inviteModalOpen, setInviteModalOpen] = useState(false);
  const [deptModalOpen, setDeptModalOpen] = useState(false);
  const [createCompanyOpen, setCreateCompanyOpen] = useState(false);
  const [subscriptionPromptOpen, setSubscriptionPromptOpen] = useState(false);
  const [editCompanyOpen, setEditCompanyOpen] = useState(false);
  const [departmentsRefreshKey, setDepartmentsRefreshKey] = useState(0);
  const [companies, setCompanies] = useState<Company[]>([]);
  const [selectedCompanyId, setSelectedCompanyId] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [deletingCompany, setDeletingCompany] = useState(false);
  const [canManageCompany, setCanManageCompany] = useState(false);
  const [authReady, setAuthReady] = useState(false);
  const [authenticated, setAuthenticated] = useState(false);
  const router = useRouter();

  const selectedCompany =
    companies.find((company) => company.id === selectedCompanyId) ?? companies[0] ?? null;

  const handleCreateCompanyClick = () => {
    if (companies.length >= MAX_COMPANIES_IN_FREE_PLAN) {
      setSubscriptionPromptOpen(true);
      return;
    }
    setCreateCompanyOpen(true);
  };

  useEffect(() => {
    const hasAccessToken = isAuthenticated();
    setAuthenticated(hasAccessToken);
    setAuthReady(true);
    if (!hasAccessToken) {
      setLoading(false);
      return;
    }
    organizationApi
      .listCompanies({ page: 0, size: 100 })
      .then((response) => {
        const loadedCompanies = response.data?.content ?? [];
        setCompanies(loadedCompanies);
        setSelectedCompanyId(loadedCompanies[0]?.id ?? null);
      })
      .catch((err) => {
        toast.error(
          err instanceof AxiosError && !err.response
            ? 'Cannot reach the organization service.'
            : 'Failed to load company data'
        );
      })
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    if (!selectedCompany) {
      setCanManageCompany(false);
      return;
    }
    let cancelled = false;
    setCanManageCompany(false);
    organizationApi.getMyMembership(selectedCompany.id)
      .then(({ data }) => {
        if (!cancelled) setCanManageCompany(data.role === 'OWNER' || data.role === 'ADMIN');
      })
      .catch(err => {
        if (!cancelled) {
          toast.error(err instanceof AxiosError
            ? (err.response?.data?.message ?? 'Failed to load company permissions')
            : 'Failed to load company permissions');
        }
      });
    return () => { cancelled = true; };
  }, [selectedCompany?.id]);

  const handleDeleteCompany = async () => {
    if (!selectedCompany) return;
    if (
      !window.confirm(
        `Are you sure you want to delete "${selectedCompany.name}"? This action cannot be undone.`
      )
    )
      return;
    setDeletingCompany(true);
    try {
      await organizationApi.deleteCompany(selectedCompany.id);
      toast.success(`Company "${selectedCompany.name}" deleted`);
      const remainingCompanies = companies.filter((company) => company.id !== selectedCompany.id);
      setCompanies(remainingCompanies);
      setSelectedCompanyId(remainingCompanies[0]?.id ?? null);
    } catch (err) {
      toast.error(
        err instanceof AxiosError
          ? (err.response?.data?.message ??
              (!err.response
                ? 'Cannot reach the organization service.'
                : 'Failed to delete company'))
          : 'Failed to delete company'
      );
    } finally {
      setDeletingCompany(false);
    }
  };

  if (!authReady || loading) {
    return (
      <div className="card-elevated p-8 text-sm text-muted-foreground">Loading company data…</div>
    );
  }

  if (!authenticated) {
    return (
      <div className="card-elevated p-8 text-center">
        <h1 className="text-xl font-bold text-foreground">Sign in required</h1>
        <p className="text-sm text-muted-foreground mt-2">
          Sign in before loading or creating a company.
        </p>
        <p className="text-xs text-muted-foreground mt-4">
          Current account: {getStoredUser()?.email ?? 'No authenticated account'}
        </p>
      </div>
    );
  }

  if (!selectedCompany) {
    return (
      <div className="space-y-6">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div>
            <h1 className="text-2xl font-bold text-foreground">Company Management</h1>
            <p className="text-muted-foreground text-sm mt-0.5">
              Create your organization to start managing your workspace
            </p>
          </div>
          <button
            onClick={handleCreateCompanyClick}
            className="btn-primary flex items-center justify-center gap-2 px-4 py-2 rounded-lg text-sm font-semibold"
          >
            <Plus size={15} />
            Create Company
          </button>
        </div>
        <div className="card-elevated p-8 text-center">
          <div className="w-16 h-16 rounded-2xl bg-primary/10 flex items-center justify-center mx-auto mb-4">
            <Building2 size={28} className="text-primary" />
          </div>
          <h2 className="text-xl font-bold text-foreground">No company found</h2>
          <p className="text-sm text-muted-foreground mt-2">
            Create your organization to start managing members, departments, and projects.
          </p>
        </div>
        {createCompanyOpen && (
          <CreateCompanyModal
            onClose={() => setCreateCompanyOpen(false)}
            onCreated={(created) => {
              const nextCompanies = [created, ...companies];
              setCompanies(nextCompanies);
              setSelectedCompanyId(created.id);
              setCreateCompanyOpen(false);
            }}
          />
        )}
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Page header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-foreground">Company Management</h1>
          <p className="text-muted-foreground text-sm mt-0.5">
            {selectedCompany.name} — Manage your organization, teams, and access
          </p>
        </div>
        <div className="flex items-center gap-2">
          {companies.length > 1 && (
            <label className="flex items-center gap-2 rounded-lg border border-border bg-card px-3 py-2 text-sm text-muted-foreground">
              <span>Company</span>
              <select
                value={selectedCompany.id}
                onChange={(event) => setSelectedCompanyId(event.target.value)}
                className="bg-transparent text-foreground focus:outline-none min-w-[160px]"
              >
                {companies.map((company) => (
                  <option key={company.id} value={company.id} className="text-foreground bg-card">
                    {company.name}
                  </option>
                ))}
              </select>
            </label>
          )}
          {activeTab === 'overview' && (
            <button
              onClick={handleCreateCompanyClick}
              className="btn-primary flex items-center gap-2 px-4 py-2 rounded-lg text-sm font-semibold"
            >
              <Plus size={15} />
              Create Company
            </button>
          )}
          {activeTab === 'members' && canManageCompany && (
            <button
              onClick={() => setInviteModalOpen(true)}
              className="btn-primary flex items-center gap-2 px-4 py-2 rounded-lg text-sm font-semibold"
            >
              <Plus size={15} />
              Invite Member
            </button>
          )}
          {activeTab === 'departments' && canManageCompany && (
            <button
              onClick={() => setDeptModalOpen(true)}
              className="btn-primary flex items-center gap-2 px-4 py-2 rounded-lg text-sm font-semibold"
            >
              <Plus size={15} />
              New Department
            </button>
          )}
        </div>
      </div>

      {/* Tab nav */}
      <div className="flex gap-1 border-b border-border">
        {TABS.map((tab) => (
          <button
            key={`tab-${tab.id}`}
            onClick={() => setActiveTab(tab.id)}
            className={`flex items-center gap-2 px-4 py-3 text-sm font-medium border-b-2 transition-all duration-150 -mb-px ${
              activeTab === tab.id
                ? 'border-primary text-primary'
                : 'border-transparent text-muted-foreground hover:text-foreground hover:border-border'
            }`}
          >
            <tab.icon size={15} />
            {tab.label}
          </button>
        ))}
      </div>

      {/* Tab content */}
      <div className="fade-in">
        {activeTab === 'overview' && (
          <CompanyOverviewTab
            company={selectedCompany}
            onEdit={canManageCompany ? () => setEditCompanyOpen(true) : undefined}
            onDelete={canManageCompany ? handleDeleteCompany : undefined}
          />
        )}
        {activeTab === 'members' && (
          <MembersTab
            companyId={selectedCompany.id}
            canManageMembers={canManageCompany}
            onInvite={() => setInviteModalOpen(true)}
          />
        )}
        {activeTab === 'departments' && (
          <DepartmentsTab
            key={`${selectedCompany.id}-${departmentsRefreshKey}`}
            companyId={selectedCompany.id}
            canManageDepartments={canManageCompany}
            onCreateDept={() => setDeptModalOpen(true)}
          />
        )}
        {activeTab === 'invitations' && <InvitationsTab companyId={selectedCompany.id} />}
      </div>

      {/* Modals */}
      {inviteModalOpen && canManageCompany && (
        <InviteMemberModal companyId={selectedCompany.id} onClose={() => setInviteModalOpen(false)} />
      )}
      {deptModalOpen && canManageCompany && (
        <CreateDepartmentModal
          companyId={selectedCompany.id}
          onClose={() => setDeptModalOpen(false)}
          onCreated={() => setDepartmentsRefreshKey((key) => key + 1)}
        />
      )}
      {editCompanyOpen && canManageCompany && (
        <EditCompanyModal
          company={selectedCompany}
          onClose={() => setEditCompanyOpen(false)}
          onUpdated={(updated) => {
            setCompanies((current) =>
              current.map((company) => (company.id === updated.id ? updated : company))
            );
            setEditCompanyOpen(false);
          }}
        />
      )}
      {createCompanyOpen && (
        <CreateCompanyModal
          onClose={() => setCreateCompanyOpen(false)}
          onCreated={(created) => {
            const nextCompanies = [created, ...companies];
            setCompanies(nextCompanies);
            setSelectedCompanyId(created.id);
            setCreateCompanyOpen(false);
          }}
        />
      )}
      {deletingCompany && (
        <div className="fixed inset-0 z-[1100] flex items-center justify-center bg-black/40">
          <div className="bg-card rounded-2xl p-6 text-sm text-muted-foreground flex items-center gap-3">
            <span className="animate-spin">⏳</span> Deleting company…
          </div>
        </div>
      )}
      {subscriptionPromptOpen && (
        <SubscriptionPrompt
          onClose={() => setSubscriptionPromptOpen(false)}
          onUpgrade={() => router.push('/payments')}
        />
      )}
    </div>
  );
}

function SubscriptionPrompt({
  onClose,
  onUpgrade,
}: {
  onClose: () => void;
  onUpgrade: () => void;
}) {
  return (
    <div className="fixed inset-0 z-[1100] flex items-center justify-center overflow-y-auto p-4" role="dialog" aria-modal="true" aria-labelledby="subscription-prompt-title">
      <div className="fixed inset-0 bg-black/40 fade-in" onClick={onClose} />
      <div className="relative max-h-[calc(100dvh-2rem)] w-full max-w-md overflow-y-auto rounded-2xl bg-card p-6 shadow-modal fade-scale-in">
        <button
          type="button"
          onClick={onClose}
          aria-label="Close subscription prompt"
          className="absolute right-4 top-4 flex h-8 w-8 items-center justify-center rounded-lg text-muted-foreground transition-all hover:bg-muted hover:text-foreground"
        >
          <X size={16} />
        </button>
        <div className="flex h-11 w-11 items-center justify-center rounded-xl bg-primary/10">
          <CreditCard size={21} className="text-primary" />
        </div>
        <h2 id="subscription-prompt-title" className="mt-4 text-lg font-bold text-foreground">
          Upgrade to create another company
        </h2>
        <p className="mt-2 text-sm leading-6 text-muted-foreground">
          Your current plan allows only {MAX_COMPANIES_IN_FREE_PLAN} company. Subscribe first to create another company.
        </p>
        <div className="mt-6 flex gap-3">
          <button type="button" onClick={onClose} className="btn-secondary flex-1 rounded-lg py-2.5 text-sm font-medium">
            Not now
          </button>
          <button type="button" onClick={onUpgrade} className="btn-primary flex-1 rounded-lg py-2.5 text-sm font-semibold">
            View subscription
          </button>
        </div>
      </div>
    </div>
  );
}
