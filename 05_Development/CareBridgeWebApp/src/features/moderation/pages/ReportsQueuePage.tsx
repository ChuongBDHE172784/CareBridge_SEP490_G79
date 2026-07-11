import { useEffect, useState, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import ModPortalSidebar from '../components/ModPortalSidebar';
import { fetchModerationQueue } from '../services/moderationApi';
import type { ModerationQueueItem } from '../models/moderation';
import { TARGET_TYPE_LABELS } from '../models/moderation';

function formatDateTime(iso: string): string {
  return new Date(iso).toLocaleString('vi-VN', { dateStyle: 'short', timeStyle: 'short' });
}

export default function ReportsQueuePage() {
  const navigate = useNavigate();
  const [items, setItems] = useState<ModerationQueueItem[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');

  const loadReports = useCallback(async () => {
    setIsLoading(true);
    setError('');
    try {
      const page = await fetchModerationQueue({ status: 'PENDING', size: 50 });
      setItems(page.content);
    } catch {
      setError('Không tải được danh sách báo cáo.');
      setItems([]);
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => { loadReports(); }, [loadReports]);

  const goToDetail = (item: ModerationQueueItem) => {
    if (item.targetType === 'ACCOUNT' || item.targetType === 'USER' || item.targetType === 'EXPERT') {
      navigate(`/moderator/reports/account/${item.id}`);
    } else {
      navigate(`/moderator/reports/${item.id}`);
    }
  };

  return (
    <div className="min-h-screen bg-background">
      <ModPortalSidebar />
      <div className="ml-64 min-h-screen p-8 font-sans">
        <div className="flex items-center gap-2 mb-1">
          <span className="material-symbols-outlined text-primary text-2xl">flag</span>
          <h1 className="text-2xl font-bold text-on-surface m-0">Báo cáo</h1>
        </div>
        <p className="text-sm text-outline ml-8 mb-6">Theo dõi các báo cáo nội dung và hoạt động cần kiểm duyệt.</p>

        {isLoading ? (
          <div className="py-16 text-center text-outline">Đang tải...</div>
        ) : error ? (
          <div className="bg-error-container rounded-2xl p-6 text-error text-sm">{error}</div>
        ) : (
          <div className="bg-surface rounded-2xl shadow-md overflow-hidden">
            <table className="w-full border-collapse">
              <thead>
                <tr className="border-b-2 border-surface-container-highest text-left bg-surface-container-low">
                  {['LÝ DO', 'LOẠI', 'NỘI DUNG XEM TRƯỚC', 'SỐ LƯỢT', 'THỜI GIAN', ''].map((h) => (
                    <th key={h} className="py-3 px-4 text-[11px] font-semibold text-outline uppercase tracking-[0.05em]">{h}</th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {items.map((item) => (
                  <tr
                    key={item.id}
                    onClick={() => goToDetail(item)}
                    className="border-b border-surface-container-highest hover:bg-surface-container-low cursor-pointer"
                  >
                    <td className="py-3.5 px-4">
                      <span className="py-1 px-3 rounded-full bg-error-container text-error text-xs font-semibold">
                        {item.reportReason}
                      </span>
                    </td>
                    <td className="py-3.5 px-4 text-sm text-on-surface-variant">{TARGET_TYPE_LABELS[item.targetType]}</td>
                    <td className="py-3.5 px-4 text-sm text-on-surface max-w-[360px] truncate">{item.contentPreview}</td>
                    <td className="py-3.5 px-4 text-sm text-on-surface-variant">{item.reportCount}</td>
                    <td className="py-3.5 px-4 text-sm text-on-surface-variant whitespace-nowrap">{formatDateTime(item.reportedAt)}</td>
                    <td className="py-3.5 px-4 text-primary text-sm font-semibold">Xem chi tiết</td>
                  </tr>
                ))}
                {items.length === 0 && (
                  <tr><td colSpan={6} className="py-12 text-center text-outline">Không có báo cáo nào đang chờ xử lý.</td></tr>
                )}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}
