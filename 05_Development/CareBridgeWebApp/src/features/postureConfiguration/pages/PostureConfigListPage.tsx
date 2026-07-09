import { useNavigate } from "react-router-dom";

export default function PostureConfigListPage() {
  const navigate = useNavigate();

  return (
    <div className="min-h-screen bg-background p-8 font-sans text-on-surface">
      <div className="mb-6 flex flex-wrap items-center justify-between gap-4">
        <div>
          <div className="mb-2 flex items-center gap-2">
            <h1 className="m-0 text-[28px] font-bold">
              Cấu hình phân tích tư thế
            </h1>
            <span className="rounded-lg bg-surface-container px-2 py-1 text-xs font-bold text-primary">
              CB-196
            </span>
          </div>
          <p className="m-0 text-sm text-on-surface-variant">
            Quản lý model, ngưỡng tin cậy và mức độ phản hồi an toàn cho từng
            bài tập.
          </p>
        </div>
        <button
          onClick={() => navigate("/posture-configs/new")}
          className="flex items-center gap-2 rounded-full bg-primary px-6 py-3 text-sm font-semibold text-on-primary shadow-[0_4px_12px_rgba(201,140,123,0.2)]"
        >
          <span className="material-symbols-outlined text-[20px]">add</span>
          Thêm cấu hình mới
        </button>
      </div>

      <div className="rounded-[24px] bg-surface p-10 text-center shadow-[0_4px_20px_rgba(201,140,123,0.08)]">
        <span className="material-symbols-outlined text-5xl text-outline">
          analytics
        </span>
        <h2 className="mt-4 text-xl font-bold text-on-surface">
          Chưa có danh sách cấu hình
        </h2>
        <p className="mx-auto mt-2 max-w-xl text-sm text-on-surface-variant">
          Màn hình không còn dùng exerciseId cố định để tải dữ liệu mẫu. Khi
          backend có endpoint danh sách cấu hình tư thế, dữ liệu thật sẽ được
          hiển thị tại đây.
        </p>
      </div>
    </div>
  );
}
