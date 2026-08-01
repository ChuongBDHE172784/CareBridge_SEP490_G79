import { Outlet } from 'react-router-dom';
import ContentPortalSidebar from '../../features/contentManagement/components/ContentPortalSidebar';

export default function ContentLayout() {
  return (
    <div className="flex min-h-screen bg-background font-sans text-on-surface">
      <ContentPortalSidebar />
      <main className="min-h-screen flex-1 overflow-auto bg-background md:ml-64">
        <Outlet />
      </main>
    </div>
  );
}
