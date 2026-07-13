import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { fetchStaffContentDetail } from "../services/contentApi";
import type { ContentDetail } from "../models/content";

export default function ContentVersionHistoryPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [title, setTitle] = useState("");
  const [content, setContent] = useState<ContentDetail | null>(null);

  useEffect(() => {
    if (!id) return;
    fetchStaffContentDetail(id)
      .then((d) => { setTitle(d.title); setContent(d); })
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
          Phiên bản hiện tại: v{content?.version ?? "—"}
        </h2>
        <p className="mx-auto mt-2 max-w-xl text-sm text-on-surface-variant">
          Hệ thống dùng bộ đếm phiên bản trên bản ghi hiện hành, không lưu snapshot lịch sử.
          {content?.sources?.length ? ` Nguồn: ${content.sources.map(s => s.title).join(", ")}.` : " Chưa khai báo nguồn tham khảo."}
        </p>
      </div>
    </div>
  );
}
