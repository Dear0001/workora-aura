import React, { Suspense } from 'react';
import AppLayout from '@/components/AppLayout';
import TaskDetailContent from './components/TaskDetailContent';
import { Loader2 } from 'lucide-react';

interface Props {
  params: Promise<{ taskId: string }>;
}

export default async function TaskDetailPage({ params }: Props) {
  const { taskId } = await params;
  return (
    <AppLayout>
      <Suspense fallback={<div className="flex items-center justify-center py-24"><Loader2 size={28} className="animate-spin text-primary" /></div>}>
        <TaskDetailContent taskId={taskId} />
      </Suspense>
    </AppLayout>
  );
}
