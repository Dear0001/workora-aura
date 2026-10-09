'use client';
import { Suspense } from 'react';
import { useSearchParams } from 'next/navigation';
import AppLayout from '@/components/AppLayout';
import TasksContent from './components/TasksContent';

function TasksPageInner() {
  const searchParams = useSearchParams();
  const phaseId = searchParams?.get('phaseId') ?? undefined;
  return <TasksContent initialPhaseId={phaseId} />;
}

export default function TasksPage() {
  return (
    <AppLayout>
      <Suspense fallback={<div className="p-8 text-sm text-muted-foreground">Loading…</div>}>
        <TasksPageInner />
      </Suspense>
    </AppLayout>
  );
}
