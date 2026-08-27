import { useNavigate } from "react-router-dom";

export default function PostureConfigListPage() {
  const navigate = useNavigate();

  return (
    <div className="portal-page px-5 py-5 md:px-6 md:py-6">
      <div className="portal-contained">
      <div className="portal-header">
        <div>
          <p className="portal-eyebrow">CB-196</p>
          <h1 className="portal-title">Cấu hình phân tích tư thế</h1>
          <p className="portal-subtitle">
            Quản lý model, ngưỡng tin cậy và mức độ phản hồi an toàn cho từng
            bài tập.
          </p>
        </div>
        <button
          onClick={() => navigate("/admin/posture-configs/new")}
          className="portal-primary-button"
        >
          <span className="material-symbols-outlined text-[20px]">add</span>
          Thêm cấu hình mới
        </button>
      </div>

      <div className="portal-card-padded text-center">
        <span className="material-symbols-outlined text-5xl text-outline">
          analytics
        </span>
        <h2 className="mt-4 text-base font-semibold text-on-surface">
          Chưa có danh sách cấu hình
        </h2>
        <p className="mx-auto mt-2 max-w-xl text-sm text-on-surface-variant">
          Màn hình không còn dùng exerciseId cố định để tải dữ liệu mẫu. Khi
          backend có endpoint danh sách cấu hình tư thế, dữ liệu thật sẽ được
          hiển thị tại đây.
        </p>
      </div>
      </div>
    </div>
  );
}
