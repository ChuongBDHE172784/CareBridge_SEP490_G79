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
    <div className="min-h-screen bg-[#F6F1EC]">
      <ModPortalSidebar />
      <div className="ml-64 min-h-screen p-8">
        <div className="mb-6">
          <div className="flex items-center gap-2 mb-1">
            <span className="material-symbols-outlined text-[#845143] text-2xl">{icon}</span>
            <h1 className="text-2xl font-bold text-[#271812]">{title}</h1>
          </div>
          <p className="text-sm text-[#84736F] ml-8">{description}</p>
        </div>

        <div className="bg-white rounded-2xl shadow-sm border border-[#FFE9E3] p-12 text-center">
          <span className="material-symbols-outlined text-[#845143] text-5xl mb-3">{icon}</span>
          <p className="font-semibold text-[#271812]">Màn hình này đang được chuẩn bị.</p>
          <p className="text-sm text-[#84736F] mt-1">
            Bạn vẫn đang ở trong ModPortal và có thể chuyển sang các mục khác từ thanh bên.
          </p>
        </div>
      </div>
    </div>
  );
}
