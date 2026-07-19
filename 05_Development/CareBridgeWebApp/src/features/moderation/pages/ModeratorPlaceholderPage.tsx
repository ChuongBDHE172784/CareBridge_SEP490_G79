import ModPortalSidebar from '../components/ModPortalSidebar';

interface ModeratorPlaceholderPageProps {
  icon: string;
  title: string;
  description: string;
}

export default function ModeratorPlaceholderPage({
  icon,
  title,
  description,
}: ModeratorPlaceholderPageProps) {
  return (
    <div className="portal-page">
      <ModPortalSidebar />
      <main className="portal-content">
        <div className="portal-contained">
          <div className="portal-header">
            <div>
              <p className="portal-eyebrow">Kiểm duyệt</p>
              <h1 className="portal-title">{title}</h1>
              <p className="portal-subtitle">{description}</p>
            </div>
          </div>

          <div className="portal-card-padded text-center">
          <span className="material-symbols-outlined mb-3 text-5xl text-outline">{icon}</span>
          <p className="font-semibold text-on-surface">Màn hình này đang được chuẩn bị.</p>
          <p className="mt-1 text-sm text-on-surface-variant">
            Bạn vẫn đang ở trong ModPortal và có thể chuyển sang các mục khác từ thanh bên.
          </p>
        </div>
        </div>
      </main>
    </div>
  );
}
