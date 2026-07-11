import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { fetchContentDetail } from "../services/contentApi";

export default function ContentVersionHistoryPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [title, setTitle] = useState("");

  useEffect(() => {
    if (!id) return;
    fetchContentDetail(id)
      .then((d) => setTitle(d.title))
      .catch(() => setTitle("(không tải được tiêu đề)"));
  }, [id]);

  return (
    <div className="p-8 font-sans">
      <div className="mb-2 flex items-center gap-2 text-[13px] text-outline">
        <span
          className="cursor-pointer"
          onClick={() => navigate("/content/list")}
        >
          Thư viện
        </span>
        <span className="material-symbols-outlined text-base">
          chevron_right
        </span>
        <span
          className="cursor-pointer"
          onClick={() => navigate(`/content/${id}`)}
        >
          Bài viết: {title || id}
        </span>
        <span className="material-symbols-outlined text-base">
          chevron_right
        </span>
        <span className="text-on-surface-variant">Lịch sử phiên bản</span>
      </div>

      <div className="mb-5 flex items-center justify-between">
        <h1 className="m-0 text-2xl font-bold text-on-surface">
          Lịch sử phiên bản
        </h1>
        <button
          disabled
          className="flex items-center gap-2 rounded-full bg-surface-container px-5 py-2.5 text-sm font-semibold text-primary opacity-50"
        >
          <span className="material-symbols-outlined text-lg">
            compare_arrows
          </span>
          So sánh các phiên bản
        </button>
      </div>

      <div className="rounded-2xl bg-surface p-10 text-center shadow-md">
        <span className="material-symbols-outlined text-5xl text-outline">
          history
        </span>
        <h2 className="mt-4 text-xl font-bold text-on-surface">
          Chưa có dữ liệu lịch sử phiên bản
        </h2>
        <p className="mx-auto mt-2 max-w-xl text-sm text-on-surface-variant">
          Backend hiện chỉ trả phiên bản hiện tại của nội dung. Màn hình không
          hiển thị dữ liệu mẫu cố định.
        </p>
      </div>
    </div>
  );
}
