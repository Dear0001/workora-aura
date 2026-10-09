import { Suspense } from 'react';
import AppLayout from '@/components/AppLayout';
import ProjectsContent from './components/ProjectsContent';

export default function ProjectsPage() {
  return (
    <AppLayout>
      <Suspense fallback={<div className="p-8 text-sm text-muted-foreground">Loading projects…</div>}>
        <ProjectsContent />
      </Suspense>
    </AppLayout>
  );
}
