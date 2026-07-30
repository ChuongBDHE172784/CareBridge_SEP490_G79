import { useState, useEffect, useCallback } from "react";
import apiClient from "../../../shared/api/apiClient";

interface ConsultationRequest {
  id: string;
  counterpartDisplayName: string;
  topic: string;
  status: string;
  createdAt: string;
}

const STATUS_MAP: Record<string, { label: string; cls: string }> = {
  PENDING: { label: "Chờ phản hồi", cls: "bg-amber-100 text-amber-700" },
  ACCEPTED: { label: "Đã chấp nhận", cls: "bg-green-100 text-green-700" },
  REJECTED: { label: "Đã từ chối", cls: "bg-red-100 text-red-700" },
  CANCELLED: { label: "Đã hủy", cls: "bg-gray-100 text-gray-700" },
};

function timeAgo(iso: string): string {
  const diff = Date.now() - new Date(iso).getTime();
  const h = Math.floor(diff / 3_600_000);
  if (h < 1) return "Vừa xong";
  if (h < 24) return `${h} giờ trước`;
  return `${Math.floor(h / 24)} ngày trước`;
}

export default function ExpertConsultationRequestsPage() {
  const [requests, setRequests] = useState<ConsultationRequest[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [page, setPage] = useState(0);
  const [hasMore, setHasMore] = useState(true);

  const fetchRequests = useCallback(async (p: number) => {
    try {
      setLoading(true);
      const { data } = await apiClient.get(
        `/api/v1/consultation-requests/assigned?size=20&page=${p}`,
      );
      const content = data.data?.content ?? data.data ?? [];
      setRequests((prev) => (p === 0 ? content : [...prev, ...content]));
      setHasMore(content.length >= 20);
      setPage(p);
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : "Không thể tải yêu cầu tư vấn");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchRequests(0);
  }, [fetchRequests]);

  const handleAction = async (id: string, action: "accept" | "reject") => {
    try {
      if (action === "accept") {
        await apiClient.patch(`/api/v1/consultation-requests/${id}/accept`);
      } else {
        await apiClient.post(`/api/v1/consultation-requests/${id}/reject`, {
          reason: "Chuyên gia bận",
        });
      }
      // Refresh list
      fetchRequests(0);
    } catch (e: unknown) {
      alert("Thao tác thất bại");
    }
  };

  return (
    <div className="max-w-4xl mx-auto p-6">
      <div className="mb-6">
        <h1 className="text-2xl font-bold text-on-surface">Yêu cầu tư vấn</h1>
        <p className="text-gray-500 mt-1">
          Quản lý các yêu cầu tư vấn từ mẹ bầu
        </p>
      </div>

      <div className="bg-white rounded-lg border border-gray-200 shadow-sm overflow-hidden">
        {loading && requests.length === 0 && (
          <div className="p-8 text-center text-gray-500">Đang tải...</div>
        )}

        {error && (
          <div className="p-4 bg-red-50 text-red-600 border-b border-red-100">
            {error}
          </div>
        )}

        {requests.length === 0 && !loading && !error && (
          <div className="p-12 text-center">
            <span className="text-4xl">📝</span>
            <p className="mt-4 text-gray-500 font-medium">
              Chưa có yêu cầu tư vấn nào
            </p>
          </div>
        )}

        <div className="divide-y divide-gray-100">
          {requests.map((req) => (
            <div
              key={req.id}
              className="p-5 flex flex-col sm:flex-row sm:items-center justify-between gap-4 hover:bg-gray-50"
            >
              <div>
                <h3 className="font-semibold text-gray-900">
                  {req.counterpartDisplayName || "Người dùng ẩn danh"}
                </h3>
                <p className="text-sm text-gray-600 mt-1">
                  Chủ đề: {req.topic}
                </p>
                <div className="flex items-center gap-3 mt-2 text-xs">
                  <span
                    className={`px-2 py-1 rounded-full font-medium ${STATUS_MAP[req.status]?.cls || "bg-gray-100"}`}
                  >
                    {STATUS_MAP[req.status]?.label || req.status}
                  </span>
                  <span className="text-gray-400">•</span>
                  <span className="text-gray-500">
                    {timeAgo(req.createdAt)}
                  </span>
                </div>
              </div>

              {req.status === "PENDING" && (
                <div className="flex items-center gap-2">
                  <button
                    onClick={() => handleAction(req.id, "reject")}
                    className="px-4 py-2 text-sm font-medium text-red-600 bg-red-50 hover:bg-red-100 rounded-lg transition-colors"
                  >
                    Từ chối
                  </button>
                  <button
                    onClick={() => handleAction(req.id, "accept")}
                    className="px-4 py-2 text-sm font-medium text-white bg-primary hover:brightness-110 rounded-lg transition-colors"
                  >
                    Chấp nhận
                  </button>
                </div>
              )}
            </div>
          ))}
        </div>

        {hasMore && !loading && (
          <div className="p-4 border-t border-gray-100 bg-gray-50 text-center">
            <button
              onClick={() => fetchRequests(page + 1)}
              className="text-primary font-medium hover:underline"
            >
              Xem thêm
            </button>
          </div>
        )}
      </div>
    </div>
  );
}
