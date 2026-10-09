import AppLayout from '@/components/AppLayout';
import ProjectDetailContent from './components/ProjectDetailContent';

interface Props {
  params: Promise<{ projectId: string }>;
}

export default async function ProjectDetailPage({ params }: Props) {
  const { projectId } = await params;
  return (
    <AppLayout>
      <ProjectDetailContent projectId={projectId} />
    </AppLayout>
  );
}
