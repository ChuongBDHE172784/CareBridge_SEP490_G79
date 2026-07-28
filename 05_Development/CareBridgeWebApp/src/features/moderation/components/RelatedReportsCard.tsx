import type { RelatedReportItem } from '../models/moderation';
import { formatReportReason } from '../models/moderation';

interface RelatedReportsCardProps {
  items: RelatedReportItem[];
  totalElements: number;
  page: number;
  size: number;
  loading: boolean;
  error: boolean;
  onPageChange: (page: number) => void;
}

function formatDateTime(value: string): string {
  return new Date(value).toLocaleString('vi-VN', { dateStyle: 'short', timeStyle: 'short' });
}

export default function RelatedReportsCard({ items, totalElements, page, size, loading, error, onPageChange }: RelatedReportsCardProps) {
  const hasNext = (page + 1) * size < totalElements;
  return (
    <div className="bg-surface rounded-2xl p-6 shadow-sm border border-surface-container-highest">
      <p className="text-[11px] font-semibold text-outline uppercase tracking-[0.05em] mb-3">Lịch sử báo cáo</p>
      {loading ? <p className="text-sm text-outline">Đang tải lịch sử báo cáo...</p>
        : error ? <p className="text-sm text-error">Không tải được lịch sử báo cáo.</p>
          : items.length === 0 ? <p className="text-sm text-outline">Chưa có báo cáo nào cho mục tiêu này.</p>
            : <>
              <p className="text-sm text-outline mb-3">Tổng số lượt báo cáo trên mục tiêu này: <strong>{totalElements}</strong></p>
              <div className="space-y-3">
                {items.map((report) => <div key={report.id} className="rounded-xl bg-surface-container-low p-3 text-sm">
                  <div className="flex justify-between gap-3 text-xs text-outline"><span>{formatDateTime(report.reportedAt)}</span><span>{report.status}</span></div>
                  <p className="mt-1 font-medium text-on-surface">{formatReportReason(report.category ?? 'OTHER')}</p>
                  {report.reason && <p className="mt-1 text-on-surface-variant">{report.reason}</p>}
                  <p className="mt-1 text-xs text-outline">Nguồn: {report.reportSource === 'AUTOMATED' ? 'Tự động' : 'Người dùng'}</p>
                </div>)}
              </div>
              {totalElements > size && <div className="mt-3 flex justify-end gap-2 text-sm text-on-surface-variant">
                <button type="button" disabled={page === 0} onClick={() => onPageChange(page - 1)} className="rounded-lg px-2 py-1 disabled:opacity-40">Trước</button>
                <button type="button" disabled={!hasNext} onClick={() => onPageChange(page + 1)} className="rounded-lg px-2 py-1 disabled:opacity-40">Sau</button>
              </div>}
            </>}
    </div>
  );
}
