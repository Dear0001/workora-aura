'use client';
import React, { useCallback, useEffect, useState } from 'react';
import {
  Bell, Check, Archive, Trash2, Loader2, Settings, RefreshCw,
  Filter, Calendar, Bookmark, BookmarkCheck, X, ChevronDown, Tag,
} from 'lucide-react';
import { toast } from 'sonner';
import { AxiosError } from 'axios';
import { notificationsApi, type Notification, type NotificationPreferences } from '@/lib/api/notifications';

type Tab = 'inbox' | 'preferences';
type ReadFilter = 'ALL' | 'UNREAD';
type Domain = 'Tasks' | 'Bugs' | 'Recruitment' | 'Payments';

const DOMAINS: { label: Domain; color: string; bg: string; keywords: string[] }[] = [
  { label: 'Tasks', color: 'text-blue-600', bg: 'bg-blue-50 border-blue-200', keywords: ['task', 'assigned', 'work', 'todo'] },
  { label: 'Bugs', color: 'text-red-600', bg: 'bg-red-50 border-red-200', keywords: ['bug', 'issue', 'error', 'fix', 'defect'] },
  { label: 'Recruitment', color: 'text-purple-600', bg: 'bg-purple-50 border-purple-200', keywords: ['recruit', 'applicant', 'application', 'hire', 'interview', 'invitation'] },
  { label: 'Payments', color: 'text-green-600', bg: 'bg-green-50 border-green-200', keywords: ['payment', 'invoice', 'paid', 'billing', 'charge'] },
];

interface FilterPreset {
  id: string;
  name: string;
  readFilter: ReadFilter;
  domains: Domain[];
  dateFrom: string;
  dateTo: string;
}

function matchesDomain(notif: Notification, domain: Domain): boolean {
  const domainDef = DOMAINS.find(d => d.label === domain);
  if (!domainDef) return false;
  const text = `${notif.title ?? ''} ${notif.message ?? ''} ${notif.type ?? ''}`.toLowerCase();
  return domainDef.keywords.some(kw => text.includes(kw));
}

function isInDateRange(notif: Notification, from: string, to: string): boolean {
  const dateStr = notif.createdAt ?? notif.occurredAt;
  if (!dateStr) return true;
  const d = new Date(dateStr);
  if (from && d < new Date(from)) return false;
  if (to) {
    const toEnd = new Date(to);
    toEnd.setHours(23, 59, 59, 999);
    if (d > toEnd) return false;
  }
  return true;
}

export default function NotificationsContent() {
  const [activeTab, setActiveTab] = useState<Tab>('inbox');
  const [notifications, setNotifications] = useState<Notification[]>([]);
  const [loading, setLoading] = useState(true);
  const [preferences, setPreferences] = useState<NotificationPreferences | null>(null);
  const [loadingPrefs, setLoadingPrefs] = useState(false);
  const [savingPrefs, setSavingPrefs] = useState(false);

  // Filters
  const [readFilter, setReadFilter] = useState<ReadFilter>('ALL');
  const [activeDomains, setActiveDomains] = useState<Domain[]>([]);
  const [dateFrom, setDateFrom] = useState('');
  const [dateTo, setDateTo] = useState('');
  const [showDatePicker, setShowDatePicker] = useState(false);

  // Presets
  const [presets, setPresets] = useState<FilterPreset[]>(() => {
    if (typeof window === 'undefined') return [];
    try { return JSON.parse(localStorage.getItem('notif_presets') ?? '[]'); } catch { return []; }
  });
  const [savePresetName, setSavePresetName] = useState('');
  const [showSavePreset, setShowSavePreset] = useState(false);

  const loadNotifications = useCallback(async () => {
    setLoading(true);
    try {
      const res = await notificationsApi.listNotifications({ page: 0, size: 100 });
      const list = Array.isArray(res.data) ? res.data : [];
      setNotifications(list);
    } catch (err) {
      toast.error(err instanceof AxiosError && !err.response ? 'Cannot reach the notification service.' : 'Failed to load notifications');
    } finally {
      setLoading(false);
    }
  }, []);

  const loadPreferences = useCallback(async () => {
    setLoadingPrefs(true);
    try {
      const res = await notificationsApi.getPreferences();
      setPreferences(res.data);
    } catch { /* ignore */ } finally {
      setLoadingPrefs(false);
    }
  }, []);

  useEffect(() => { loadNotifications(); }, [loadNotifications]);
  useEffect(() => { if (activeTab === 'preferences') loadPreferences(); }, [activeTab, loadPreferences]);

  const handleMarkRead = async (id: string) => {
    try {
      const res = await notificationsApi.markAsRead(id);
      setNotifications(prev => prev.map(n => n.id === id ? res.data : n));
    } catch (err) {
      toast.error(err instanceof AxiosError ? (err.response?.data?.message ?? 'Failed to mark as read') : 'Failed to mark as read');
    }
  };

  const handleArchive = async (id: string) => {
    try {
      await notificationsApi.archiveNotification(id);
      setNotifications(prev => prev.filter(n => n.id !== id));
      toast.success('Notification archived');
    } catch (err) {
      toast.error(err instanceof AxiosError ? (err.response?.data?.message ?? 'Failed to archive') : 'Failed to archive');
    }
  };

  const handleDelete = async (id: string) => {
    try {
      await notificationsApi.deleteNotification(id);
      setNotifications(prev => prev.filter(n => n.id !== id));
      toast.success('Notification deleted');
    } catch (err) {
      toast.error(err instanceof AxiosError ? (err.response?.data?.message ?? 'Failed to delete') : 'Failed to delete');
    }
  };

  const handleMarkAllRead = async () => {
    const unread = notifications.filter(n => !n.read);
    await Promise.all(unread.map(n => notificationsApi.markAsRead(n.id).catch(() => null)));
    setNotifications(prev => prev.map(n => ({ ...n, read: true })));
    toast.success('All marked as read');
  };

  const handleSavePreferences = async () => {
    if (!preferences) return;
    setSavingPrefs(true);
    try {
      const res = await notificationsApi.updatePreferences(preferences);
      setPreferences(res.data);
      toast.success('Preferences saved');
    } catch (err) {
      toast.error(err instanceof AxiosError ? (err.response?.data?.message ?? 'Failed to save preferences') : 'Failed to save preferences');
    } finally {
      setSavingPrefs(false);
    }
  };

  const toggleDomain = (domain: Domain) => {
    setActiveDomains(prev =>
      prev.includes(domain) ? prev.filter(d => d !== domain) : [...prev, domain]
    );
  };

  const clearFilters = () => {
    setReadFilter('ALL');
    setActiveDomains([]);
    setDateFrom('');
    setDateTo('');
  };

  const hasActiveFilters = readFilter !== 'ALL' || activeDomains.length > 0 || dateFrom || dateTo;

  const savePreset = () => {
    if (!savePresetName.trim()) return;
    const preset: FilterPreset = {
      id: Date.now().toString(),
      name: savePresetName.trim(),
      readFilter,
      domains: activeDomains,
      dateFrom,
      dateTo,
    };
    const updated = [...presets, preset];
    setPresets(updated);
    localStorage.setItem('notif_presets', JSON.stringify(updated));
    setSavePresetName('');
    setShowSavePreset(false);
    toast.success(`Preset "${preset.name}" saved`);
  };

  const applyPreset = (preset: FilterPreset) => {
    setReadFilter(preset.readFilter);
    setActiveDomains(preset.domains);
    setDateFrom(preset.dateFrom);
    setDateTo(preset.dateTo);
    toast.success(`Applied preset "${preset.name}"`);
  };

  const deletePreset = (id: string) => {
    const updated = presets.filter(p => p.id !== id);
    setPresets(updated);
    localStorage.setItem('notif_presets', JSON.stringify(updated));
  };

  // Apply all filters
  const filtered = notifications.filter(n => {
    if (readFilter === 'UNREAD' && n.read) return false;
    if (activeDomains.length > 0 && !activeDomains.some(d => matchesDomain(n, d))) return false;
    if (!isInDateRange(n, dateFrom, dateTo)) return false;
    return true;
  });

  const unreadCount = notifications.filter(n => !n.read).length;

  const dateRangeLabel = dateFrom || dateTo
    ? `${dateFrom || '…'} → ${dateTo || '…'}`
    : null;

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-foreground">Notifications</h1>
          <p className="text-muted-foreground text-sm mt-0.5">
            {unreadCount > 0 ? `${unreadCount} unread notification${unreadCount !== 1 ? 's' : ''}` : 'All caught up'}
          </p>
        </div>
        <div className="flex items-center gap-2">
          <button onClick={loadNotifications} className="flex items-center gap-1.5 text-xs text-muted-foreground bg-muted hover:bg-muted/80 px-3 py-2 rounded-lg transition-all">
            <RefreshCw size={13} />Refresh
          </button>
          {unreadCount > 0 && (
            <button onClick={handleMarkAllRead} className="flex items-center gap-1.5 text-xs text-primary bg-primary/10 hover:bg-primary/20 px-3 py-2 rounded-lg transition-all">
              <Check size={13} />Mark all read
            </button>
          )}
        </div>
      </div>

      {/* Tabs */}
      <div className="flex gap-1 border-b border-border">
        {[
          { id: 'inbox' as Tab, label: 'Inbox', icon: Bell },
          { id: 'preferences' as Tab, label: 'Preferences', icon: Settings },
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
            {tab.id === 'inbox' && unreadCount > 0 && (
              <span className="bg-primary text-primary-foreground text-xs font-semibold px-1.5 py-0.5 rounded-full">{unreadCount}</span>
            )}
          </button>
        ))}
      </div>

      {/* Inbox */}
      {activeTab === 'inbox' && (
        <div className="space-y-4">

          {/* ── Filter Bar ── */}
          <div className="card-elevated p-4 space-y-4">

            {/* Row 1: Read filter + Domain filters */}
            <div className="flex flex-wrap items-center gap-3">
              {/* Read/Unread toggle */}
              <div className="flex gap-1 p-1 bg-muted rounded-lg">
                {(['ALL', 'UNREAD'] as const).map(f => (
                  <button
                    key={f}
                    onClick={() => setReadFilter(f)}
                    className={`px-3 py-1.5 rounded-md text-xs font-medium transition-all ${readFilter === f ? 'bg-card text-foreground shadow-sm' : 'text-muted-foreground hover:text-foreground'}`}
                  >
                    {f === 'ALL' ? 'All' : 'Unread'}
                    {f === 'UNREAD' && unreadCount > 0 && <span className="ml-1.5 bg-primary/10 text-primary text-xs px-1 rounded-full">{unreadCount}</span>}
                  </button>
                ))}
              </div>

              <div className="w-px h-5 bg-border" />

              {/* Domain filter pills */}
              <div className="flex items-center gap-1.5 flex-wrap">
                <Tag size={13} className="text-muted-foreground flex-shrink-0" />
                {DOMAINS.map(domain => {
                  const active = activeDomains.includes(domain.label);
                  return (
                    <button
                      key={domain.label}
                      onClick={() => toggleDomain(domain.label)}
                      className={`px-2.5 py-1 rounded-full text-xs font-medium border transition-all ${
                        active
                          ? `${domain.bg} ${domain.color} border-current`
                          : 'bg-muted text-muted-foreground border-transparent hover:border-border hover:text-foreground'
                      }`}
                    >
                      {domain.label}
                      {active && <X size={10} className="inline ml-1 -mr-0.5" />}
                    </button>
                  );
                })}
              </div>

              <div className="w-px h-5 bg-border" />

              {/* Date range picker toggle */}
              <button
                onClick={() => setShowDatePicker(v => !v)}
                className={`flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-medium border transition-all ${
                  dateRangeLabel
                    ? 'bg-primary/10 text-primary border-primary/30' :'bg-muted text-muted-foreground border-transparent hover:border-border hover:text-foreground'
                }`}
              >
                <Calendar size={13} />
                {dateRangeLabel ?? 'Date range'}
                <ChevronDown size={11} className={`transition-transform ${showDatePicker ? 'rotate-180' : ''}`} />
              </button>

              {/* Clear filters */}
              {hasActiveFilters && (
                <button
                  onClick={clearFilters}
                  className="flex items-center gap-1 text-xs text-muted-foreground hover:text-foreground transition-colors ml-auto"
                >
                  <X size={12} />Clear filters
                </button>
              )}
            </div>

            {/* Date range picker (expandable) */}
            {showDatePicker && (
              <div className="flex flex-wrap items-center gap-3 pt-3 border-t border-border">
                <div className="flex items-center gap-2">
                  <label className="text-xs text-muted-foreground font-medium w-8">From</label>
                  <input
                    type="date"
                    value={dateFrom}
                    onChange={e => setDateFrom(e.target.value)}
                    className="text-xs bg-muted border border-border rounded-lg px-2.5 py-1.5 text-foreground focus:outline-none focus:ring-1 focus:ring-primary"
                  />
                </div>
                <div className="flex items-center gap-2">
                  <label className="text-xs text-muted-foreground font-medium w-8">To</label>
                  <input
                    type="date"
                    value={dateTo}
                    min={dateFrom}
                    onChange={e => setDateTo(e.target.value)}
                    className="text-xs bg-muted border border-border rounded-lg px-2.5 py-1.5 text-foreground focus:outline-none focus:ring-1 focus:ring-primary"
                  />
                </div>
                {(dateFrom || dateTo) && (
                  <button
                    onClick={() => { setDateFrom(''); setDateTo(''); }}
                    className="text-xs text-muted-foreground hover:text-foreground flex items-center gap-1"
                  >
                    <X size={11} />Clear dates
                  </button>
                )}
              </div>
            )}

            {/* Row 3: Saved presets */}
            <div className="flex flex-wrap items-center gap-2 pt-3 border-t border-border">
              <Bookmark size={13} className="text-muted-foreground flex-shrink-0" />
              <span className="text-xs text-muted-foreground font-medium">Presets:</span>

              {presets.length === 0 && (
                <span className="text-xs text-muted-foreground italic">No saved presets yet</span>
              )}

              {presets.map(preset => (
                <div key={preset.id} className="flex items-center gap-0.5 bg-muted rounded-lg overflow-hidden border border-border">
                  <button
                    onClick={() => applyPreset(preset)}
                    className="flex items-center gap-1.5 px-2.5 py-1 text-xs font-medium text-foreground hover:bg-primary/10 hover:text-primary transition-all"
                  >
                    <BookmarkCheck size={11} />
                    {preset.name}
                  </button>
                  <button
                    onClick={() => deletePreset(preset.id)}
                    className="px-1.5 py-1 text-muted-foreground hover:text-red-500 hover:bg-red-50 transition-all border-l border-border"
                    title="Delete preset"
                  >
                    <X size={10} />
                  </button>
                </div>
              ))}

              {/* Save current filter as preset */}
              {hasActiveFilters && !showSavePreset && (
                <button
                  onClick={() => setShowSavePreset(true)}
                  className="flex items-center gap-1 text-xs text-primary hover:underline ml-1"
                >
                  <Bookmark size={11} />Save current
                </button>
              )}

              {showSavePreset && (
                <div className="flex items-center gap-1.5">
                  <input
                    autoFocus
                    type="text"
                    placeholder="Preset name…"
                    value={savePresetName}
                    onChange={e => setSavePresetName(e.target.value)}
                    onKeyDown={e => { if (e.key === 'Enter') savePreset(); if (e.key === 'Escape') setShowSavePreset(false); }}
                    className="text-xs bg-muted border border-border rounded-lg px-2.5 py-1.5 text-foreground focus:outline-none focus:ring-1 focus:ring-primary w-36"
                  />
                  <button onClick={savePreset} className="text-xs bg-primary text-primary-foreground px-2.5 py-1.5 rounded-lg hover:opacity-90 transition-all">Save</button>
                  <button onClick={() => setShowSavePreset(false)} className="text-xs text-muted-foreground hover:text-foreground"><X size={12} /></button>
                </div>
              )}
            </div>
          </div>

          {/* Results count */}
          {hasActiveFilters && !loading && (
            <p className="text-xs text-muted-foreground">
              Showing <span className="font-semibold text-foreground">{filtered.length}</span> of {notifications.length} notifications
            </p>
          )}

          {/* Notification list */}
          {loading ? (
            <div className="flex items-center justify-center py-16"><Loader2 size={24} className="animate-spin text-primary" /></div>
          ) : filtered.length === 0 ? (
            <div className="card-elevated p-12 text-center">
              <Bell size={36} className="text-muted-foreground/30 mx-auto mb-3" />
              <p className="text-sm text-muted-foreground">
                {hasActiveFilters ? 'No notifications match the current filters' : 'No notifications yet'}
              </p>
              {hasActiveFilters && (
                <button onClick={clearFilters} className="mt-3 text-xs text-primary hover:underline">Clear filters</button>
              )}
            </div>
          ) : (
            <div className="space-y-2">
              {filtered.map(notif => {
                const matchedDomain = DOMAINS.find(d => matchesDomain(notif, d.label));
                return (
                  <div key={notif.id} className={`card-elevated p-4 group transition-all ${!notif.read ? 'border-l-4 border-l-primary' : ''}`}>
                    <div className="flex items-start justify-between gap-3">
                      <div className="flex items-start gap-3 flex-1 min-w-0">
                        <div className={`w-2 h-2 rounded-full mt-1.5 flex-shrink-0 ${notif.read ? 'bg-muted' : 'bg-primary'}`} />
                        <div className="flex-1 min-w-0">
                          <div className="flex items-center gap-2 flex-wrap mb-0.5">
                            {notif.title && <p className="font-semibold text-foreground text-sm">{notif.title}</p>}
                            {matchedDomain && (
                              <span className={`text-[10px] font-semibold px-1.5 py-0.5 rounded-full border ${matchedDomain.bg} ${matchedDomain.color}`}>
                                {matchedDomain.label}
                              </span>
                            )}
                          </div>
                          <p className="text-sm text-muted-foreground">{notif.message}</p>
                          <p className="text-xs text-muted-foreground mt-1">
                            {notif.createdAt
                              ? new Date(notif.createdAt).toLocaleString()
                              : notif.occurredAt
                              ? new Date(notif.occurredAt).toLocaleString()
                              : '—'}
                          </p>
                        </div>
                      </div>
                      <div className="flex items-center gap-1 opacity-0 group-hover:opacity-100 transition-opacity flex-shrink-0">
                        {!notif.read && (
                          <button onClick={() => handleMarkRead(notif.id)} title="Mark as read" className="w-7 h-7 rounded-md flex items-center justify-center text-muted-foreground hover:text-primary hover:bg-primary/10 transition-all">
                            <Check size={13} />
                          </button>
                        )}
                        <button onClick={() => handleArchive(notif.id)} title="Archive" className="w-7 h-7 rounded-md flex items-center justify-center text-muted-foreground hover:text-amber-600 hover:bg-amber-50 transition-all">
                          <Archive size={13} />
                        </button>
                        <button onClick={() => handleDelete(notif.id)} title="Delete" className="w-7 h-7 rounded-md flex items-center justify-center text-muted-foreground hover:text-red-500 hover:bg-red-50 transition-all">
                          <Trash2 size={13} />
                        </button>
                      </div>
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </div>
      )}

      {/* Preferences */}
      {activeTab === 'preferences' && (
        <div className="card-elevated p-6 max-w-lg">
          <h2 className="text-base font-bold text-foreground mb-4">Notification Preferences</h2>
          {loadingPrefs ? (
            <div className="flex items-center gap-2 text-sm text-muted-foreground"><Loader2 size={14} className="animate-spin" />Loading preferences…</div>
          ) : preferences ? (
            <div className="space-y-4">
              {[
                { key: 'emailEnabled' as keyof NotificationPreferences, label: 'Email Notifications', desc: 'Receive notifications via email' },
                { key: 'pushEnabled' as keyof NotificationPreferences, label: 'Push Notifications', desc: 'Receive in-app push notifications' },
                { key: 'taskAssigned' as keyof NotificationPreferences, label: 'Task Assigned', desc: 'When a task is assigned to you' },
                { key: 'bugReported' as keyof NotificationPreferences, label: 'Bug Reported', desc: 'When a new bug is reported' },
                { key: 'projectUpdated' as keyof NotificationPreferences, label: 'Project Updated', desc: 'When a project you are on is updated' },
                { key: 'invitationReceived' as keyof NotificationPreferences, label: 'Invitation Received', desc: 'When you receive a company invitation' },
                { key: 'paymentReceived' as keyof NotificationPreferences, label: 'Payment Received', desc: 'When a payment is confirmed' },
              ].map(pref => (
                <div key={pref.key} className="flex items-center justify-between py-3 border-b border-border last:border-0">
                  <div>
                    <p className="text-sm font-medium text-foreground">{pref.label}</p>
                    <p className="text-xs text-muted-foreground">{pref.desc}</p>
                  </div>
                  <button
                    onClick={() => setPreferences(prev => prev ? { ...prev, [pref.key]: !prev[pref.key] } : prev)}
                    className={`relative w-10 h-6 rounded-full transition-colors ${preferences[pref.key] ? 'bg-primary' : 'bg-muted'}`}
                  >
                    <span className={`absolute top-1 w-4 h-4 rounded-full bg-white shadow transition-transform ${preferences[pref.key] ? 'translate-x-5' : 'translate-x-1'}`} />
                  </button>
                </div>
              ))}
              <button onClick={handleSavePreferences} disabled={savingPrefs} className="btn-primary w-full py-2.5 rounded-lg font-semibold text-sm flex items-center justify-center gap-2 disabled:opacity-60 mt-2">
                {savingPrefs ? <><Loader2 size={14} className="animate-spin" /><span>Saving…</span></> : 'Save Preferences'}
              </button>
            </div>
          ) : (
            <div className="text-sm text-muted-foreground">Could not load preferences. The notification service may not be available.</div>
          )}
        </div>
      )}
    </div>
  );
}
