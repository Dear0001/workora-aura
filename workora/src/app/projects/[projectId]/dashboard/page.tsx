import AppLayout from '@/components/AppLayout';
import ProjectDashboardContent from './components/ProjectDashboardContent';

interface Props {
  params: Promise<{ projectId: string }>;
}

export default async function ProjectDashboardPage({ params }: Props) {
  const { projectId } = await params;
  return (
    <AppLayout>
      <ProjectDashboardContent projectId={projectId} />
    </AppLayout>
  );
}
