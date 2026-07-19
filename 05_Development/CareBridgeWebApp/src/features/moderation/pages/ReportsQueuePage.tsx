import { useEffect, useState, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import ModPortalSidebar from '../components/ModPortalSidebar';
import { fetchModerationQueue } from '../services/moderationApi';
import type { ModerationQueueItem } from '../models/moderation';
import { formatReportReason, TARGET_TYPE_LABELS } from '../models/moderation';

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
    <div className="portal-page">
      <ModPortalSidebar />
      <main className="portal-content">
        <div className="portal-contained">
        <div className="portal-header">
          <div>
            <p className="portal-eyebrow">Kiểm duyệt</p>
            <h1 className="portal-title">Báo cáo</h1>
            <p className="portal-subtitle">Theo dõi các báo cáo nội dung và hoạt động cần kiểm duyệt.</p>
          </div>
        </div>

        {isLoading ? (
          <div className="portal-empty">Đang tải...</div>
        ) : error ? (
          <div className="portal-error">{error}</div>
        ) : (
          <div className="portal-table-card">
            <table className="w-full">
              <thead>
                <tr>
                  {['LÝ DO', 'LOẠI', 'NỘI DUNG XEM TRƯỚC', 'SỐ LƯỢT', 'THỜI GIAN', ''].map((h) => (
                    <th key={h}>{h}</th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {items.map((item) => (
                  <tr
                    key={item.id}
                    onClick={() => goToDetail(item)}
                    className="cursor-pointer"
                  >
                    <td>
                      <span className="rounded-md bg-error-container px-2.5 py-1 text-xs font-semibold text-error">
                        {formatReportReason(item.reportReason)}
                      </span>
                    </td>
                    <td className="text-on-surface-variant">{TARGET_TYPE_LABELS[item.targetType]}</td>
                    <td className="max-w-[360px] truncate text-on-surface">{item.contentPreview}</td>
                    <td className="text-on-surface-variant">{item.reportCount}</td>
                    <td className="whitespace-nowrap text-on-surface-variant">{formatDateTime(item.reportedAt)}</td>
                    <td className="font-semibold text-primary">Xem chi tiết</td>
                  </tr>
                ))}
                {items.length === 0 && (
                  <tr><td colSpan={6} className="text-center text-outline">Không có báo cáo nào đang chờ xử lý.</td></tr>
                )}
              </tbody>
            </table>
          </div>
        )}
        </div>
      </main>
    </div>
  );
}
