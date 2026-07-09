import { useCallback, useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import type { ContentDetail } from "../models/content";
import { fetchContentDetail } from "../services/contentApi";

export default function ContentPreviewPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [detail, setDetail] = useState<ContentDetail | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [viewMode, setViewMode] = useState<"mobile" | "desktop">("mobile");

  const loadDetail = useCallback(async () => {
    if (!id) {
      setError("Thiếu mã nội dung.");
      setIsLoading(false);
      return;
    }
    setIsLoading(true);
    setError(null);
    try {
      setDetail(await fetchContentDetail(id));
    } catch {
      setDetail(null);
      setError("Không thể tải nội dung để xem trước.");
    } finally {
      setIsLoading(false);
    }
  }, [id]);

  useEffect(() => {
    loadDetail();
  }, [loadDetail]);

  if (isLoading) {
    return (
      <div className="py-12 text-center font-sans text-outline">
        Đang tải...
      </div>
    );
  }

  if (error || !detail) {
    return (
      <div className="py-12 text-center font-sans">
        <p className="mb-4 text-error">{error ?? "Không tìm thấy nội dung."}</p>
        <button
          onClick={() => navigate(-1)}
          className="rounded-full border border-outline-variant px-6 py-2.5 text-sm font-semibold text-primary"
        >
          Quay lại
        </button>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-background font-sans">
      <div className="sticky top-0 z-10 flex items-center justify-between border-b border-surface-container-highest bg-surface px-8 py-3">
        <div className="flex items-center gap-4">
          <button
            onClick={() => navigate(-1)}
            className="inline-flex items-center gap-1.5 rounded-full border border-outline-variant px-5 py-2 text-[13px] font-semibold text-primary"
          >
            <span className="material-symbols-outlined text-base">
              arrow_back
            </span>
            Quay lại
          </button>
          <span className="max-w-[400px] overflow-hidden text-ellipsis whitespace-nowrap text-sm font-semibold text-on-surface">
            {detail.title}
          </span>
        </div>
        <div className="flex overflow-hidden rounded-full border border-outline-variant">
          <button
            onClick={() => setViewMode("mobile")}
            className={`flex items-center gap-1 px-4 py-2 text-[13px] font-semibold ${viewMode === "mobile" ? "bg-primary text-on-primary" : "bg-surface text-on-surface-variant"}`}
          >
            <span className="material-symbols-outlined text-base">
              smartphone
            </span>
            Mobile
          </button>
          <button
            onClick={() => setViewMode("desktop")}
            className={`flex items-center gap-1 border-l border-outline-variant px-4 py-2 text-[13px] font-semibold ${viewMode === "desktop" ? "bg-primary text-on-primary" : "bg-surface text-on-surface-variant"}`}
          >
            <span className="material-symbols-outlined text-base">
              desktop_windows
            </span>
            Desktop
          </button>
        </div>
      </div>

      <div className="flex min-h-[calc(100vh-60px)] items-start justify-center px-8 py-12">
        <article
          className={
            viewMode === "mobile"
              ? "h-[844px] w-[390px] overflow-y-auto rounded-[40px] border-8 border-[#FADCD3] bg-surface p-5 shadow-[0_24px_80px_rgba(90,70,63,0.18)]"
              : "w-full max-w-[900px] rounded-2xl bg-surface p-10 shadow-[0_24px_80px_rgba(90,70,63,0.12)]"
          }
        >
          <h1 className="mt-0 text-2xl font-bold text-on-surface">
            {detail.title}
          </h1>
          <div className="mb-6 flex gap-2">
            <span className="rounded-full bg-primary/10 px-3 py-1 text-xs font-semibold text-primary">
              {detail.stage}
            </span>
            <span className="rounded-full bg-surface-container px-3 py-1 text-xs font-semibold text-on-surface-variant">
              v{detail.version}
            </span>
          </div>
          <div
            className="text-base leading-8 text-on-surface"
            dangerouslySetInnerHTML={{ __html: detail.body }}
          />
        </article>
      </div>
    </div>
  );
}
