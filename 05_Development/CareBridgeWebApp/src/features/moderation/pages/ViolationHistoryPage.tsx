import { useNavigate } from "react-router-dom";
import ModPortalSidebar from "../components/ModPortalSidebar";

export default function ViolationHistoryPage() {
  const navigate = useNavigate();

  return (
    <div className="min-h-screen bg-background">
      <ModPortalSidebar />
      <div className="ml-64 min-h-screen p-8 font-sans">
        <button
          onClick={() => navigate(-1)}
          className="mb-4 inline-flex cursor-pointer items-center gap-1.5 text-sm font-semibold text-on-surface"
        >
          <span className="material-symbols-outlined text-lg">arrow_back</span>
          Quay lại
        </button>

        <div className="mb-5 flex items-center justify-between">
          <div>
            <h1 className="m-0 text-2xl font-bold text-on-surface">
              Lịch sử vi phạm
            </h1>
            <p className="mt-1 text-sm text-outline">
              Ghi nhận các hành động kỷ luật đối với tài khoản.
            </p>
          </div>
          <button
            disabled
            className="flex items-center gap-2 rounded-full bg-surface-container px-5 py-2.5 text-sm font-semibold text-primary opacity-50"
          >
            <span className="material-symbols-outlined text-lg">download</span>
            Xuất báo cáo
          </button>
        </div>

        <div className="rounded-2xl bg-surface p-10 text-center shadow-md">
          <span className="material-symbols-outlined text-5xl text-outline">
            gavel
          </span>
          <h2 className="mt-4 text-xl font-bold text-on-surface">
            Chưa có dữ liệu lịch sử vi phạm
          </h2>
          <p className="mx-auto mt-2 max-w-xl text-sm text-on-surface-variant">
            Backend hiện chưa cung cấp endpoint liệt kê lịch sử vi phạm theo tài
            khoản. Màn hình không hiển thị dữ liệu mẫu cố định.
          </p>
        </div>
      </div>
    </div>
  );
}
