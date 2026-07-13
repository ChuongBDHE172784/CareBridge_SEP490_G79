import { Outlet } from 'react-router-dom';
import ContentPortalSidebar from '../../features/contentManagement/components/ContentPortalSidebar';

export default function ContentLayout() {
  return (
    <div className="flex min-h-screen font-sans">
      <ContentPortalSidebar />
      <main className="ml-64 min-h-screen bg-background overflow-auto flex-1">
        <Outlet />
      </main>
    </div>
  );
}
