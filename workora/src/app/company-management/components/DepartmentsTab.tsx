'use client';
import React, { useCallback, useEffect, useState } from 'react';
import { Users, MoreHorizontal, Edit2, Trash2, FolderKanban, ChevronRight, Shield } from 'lucide-react';
import { toast } from 'sonner';
import { AxiosError } from 'axios';
import { useRouter } from 'next/navigation';
import { organizationApi } from '@/lib/api/organization';
import type { Department } from '@/lib/api/types';
import EditDepartmentModal from './EditDepartmentModal';
import DepartmentRoleModal from './DepartmentRoleModal';

interface Props {
  onCreateDept: () => void;
  companyId?: string;
  onCreated?: () => void;
}

const COLORS = ['bg-blue-500', 'bg-violet-500', 'bg-indigo-500', 'bg-amber-500', 'bg-green-500', 'bg-rose-500'];

export default function DepartmentsTab({ onCreateDept, companyId, onCreated }: Props) {
  const router = useRouter();
  const [activeMenu, setActiveMenu] = useState<string | null>(null);
  const [departments, setDepartments] = useState<Department[]>([]);
  const [editingDept, setEditingDept] = useState<Department | null>(null);
  const [roleModalDept, setRoleModalDept] = useState<Department | null>(null);
  const [deptMemberCounts, setDeptMemberCounts] = useState<Record<string, number>>({});

  const fetchDepartments = useCallback(async () => {
    if (!companyId) {
      setDepartments([]);
      return;
    }
    try {
      const response = await organizationApi.listDepartments(companyId);
      setDepartments(response.data);
    } catch (err) {
      toast.error(err instanceof AxiosError && !err.response
        ? 'Cannot reach the organization service.' : 'Failed to load departments');
      setDepartments([]);
    }
  }, [companyId]);

  useEffect(() => { fetchDepartments(); }, [fetchDepartments]);

  // Fetch member counts for each department
  useEffect(() => {
    if (!departments.length) return;
    departments.forEach(async (dept) => {
      try {
        const res = await organizationApi.listDepartmentMembers(dept.id);
        const count = Array.isArray(res.data) ? res.data.length : (res.data as { content?: unknown[] })?.content?.length ?? 0;
        setDeptMemberCounts(prev => ({ ...prev, [dept.id]: count }));
      } catch {
        // silently ignore per-department errors
      }
    });
  }, [departments]);

  const handleDelete = async (e: React.MouseEvent, id: string, name: string) => {
    e.stopPropagation();
    try {
      await organizationApi.deleteDepartment(id);
      setDepartments(prev => prev.filter(dept => dept.id !== id));
      toast.success(`Department "${name}" removed`);
    } catch (err) {
      toast.error(err instanceof AxiosError
        ? (err.response?.data?.message ?? 'Failed to remove department')
        : 'Failed to remove department');
    } finally {
      setActiveMenu(null);
    }
  };

  const handleCardClick = (dept: Department) => {
    router.push(`/projects?departmentId=${dept.id}&departmentName=${encodeURIComponent(dept.name)}`);
  };

  return (
    <>
      <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-3 2xl:grid-cols-3 gap-4">
        {departments.map((dept, index) => (
          <div
            key={dept.id}
            onClick={() => handleCardClick(dept)}
            className="card-elevated p-5 card-hover group cursor-pointer"
          >
            <div className="flex items-start justify-between mb-4">
              <div className="flex items-center gap-3">
                <div className={`w-3 h-10 rounded-full ${COLORS[index % COLORS.length]}`} />
                <div>
                  <h3 className="font-semibold text-foreground text-sm">{dept.name}</h3>
                  <p className="text-xs text-muted-foreground mt-0.5">Company department</p>
                </div>
              </div>
              <div className="relative" onClick={e => e.stopPropagation()}>
                <button
                  onClick={(e) => { e.stopPropagation(); setActiveMenu(activeMenu === dept.id ? null : dept.id); }}
                  className="w-7 h-7 rounded-md flex items-center justify-center text-muted-foreground hover:text-foreground hover:bg-muted transition-all"
                >
                  <MoreHorizontal size={15} />
                </button>
                {activeMenu === dept.id && (
                  <>
                    <div className="fixed inset-0 z-10" onClick={(e) => { e.stopPropagation(); setActiveMenu(null); }} />
                    <div className="absolute right-0 top-full mt-1 w-44 bg-card border border-border rounded-xl shadow-elevated z-20 py-1 fade-scale-in">
                      <button
                        onClick={(e) => { e.stopPropagation(); setRoleModalDept(dept); setActiveMenu(null); }}
                        className="flex items-center gap-2 w-full px-3 py-2 text-xs text-foreground hover:bg-muted transition-colors"
                      >
                        <Shield size={12} className="text-muted-foreground" />
                        Manage Roles
                      </button>
                      <button
                        onClick={(e) => { e.stopPropagation(); setEditingDept(dept); setActiveMenu(null); }}
                        className="flex items-center gap-2 w-full px-3 py-2 text-xs text-foreground hover:bg-muted transition-colors"
                      >
                        <Edit2 size={12} className="text-muted-foreground" />
                        Edit Department
                      </button>
                      <button
                        onClick={(e) => handleDelete(e, dept.id, dept.name)}
                        className="flex items-center gap-2 w-full px-3 py-2 text-xs text-red-600 hover:bg-red-50 transition-colors"
                      >
                        <Trash2 size={12} />
                        Delete
                      </button>
                    </div>
                  </>
                )}
              </div>
            </div>

            <p className="text-xs text-muted-foreground leading-relaxed mb-4">{dept.description}</p>

            <div className="flex items-center justify-between pt-3 border-t border-border">
              <div className="flex items-center gap-1.5 text-xs text-muted-foreground">
                <Users size={12} />
                <span className="font-tabular font-medium text-foreground">
                  {deptMemberCounts[dept.id] !== undefined ? deptMemberCounts[dept.id] : '—'}
                </span>
                <span>members</span>
              </div>
              <div className="flex items-center gap-1.5 text-xs text-muted-foreground">
                <FolderKanban size={12} />
                <span className="font-tabular font-medium text-foreground">—</span>
                <span>projects</span>
              </div>
              <div className="flex items-center gap-1 text-xs text-primary font-medium">
                <span>View Projects</span>
                <ChevronRight size={12} />
              </div>
            </div>
          </div>
        ))}

        {/* Add department card */}
        <button
          onClick={onCreateDept}
          className="card-elevated p-5 border-dashed border-2 border-border hover:border-primary hover:bg-primary/3 transition-all duration-150 flex flex-col items-center justify-center gap-2 min-h-[160px] group"
        >
          <div className="w-10 h-10 rounded-full bg-muted group-hover:bg-primary/10 flex items-center justify-center transition-colors">
            <span className="text-xl text-muted-foreground group-hover:text-primary transition-colors">+</span>
          </div>
          <span className="text-sm font-medium text-muted-foreground group-hover:text-primary transition-colors">Create Department</span>
        </button>
      </div>

      {editingDept && (
        <EditDepartmentModal
          department={editingDept}
          onClose={() => setEditingDept(null)}
          onUpdated={(updated) => {
            setDepartments(prev => prev.map(d => d.id === updated.id ? updated : d));
            setEditingDept(null);
          }}
        />
      )}

      {roleModalDept && companyId && (
        <DepartmentRoleModal
          department={roleModalDept}
          companyId={companyId}
          onClose={() => setRoleModalDept(null)}
        />
      )}
    </>
  );
}