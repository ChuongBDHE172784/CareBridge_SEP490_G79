import { useCallback, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  decideExpertChecklist,
  decideExpertContent,
  fetchExpertApprovalQueue,
} from '../../contentManagement/services/contentApi';
import type { ContentStage, ContentType, ExpertApprovalQueueItem } from '../../contentManagement/models/content';
import { STAGE_LABELS, STAGE_OPTIONS, TYPE_LABELS } from '../../contentManagement/models/content';

type TypeFilter = 'ALL' | ContentType;

type PendingDecision = {
  item: ExpertApprovalQueueItem;
  decision: 'APPROVE' | 'REJECT';
};

export default function ExpertContentApprovalQueuePage() {
  const navigate = useNavigate();
  const [items, setItems] = useState<ExpertApprovalQueueItem[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  // Filters
  const [typeFilter, setTypeFilter] = useState<TypeFilter>('ALL');
  const [stageFilter, setStageFilter] = useState<string>('ALL');
  const [searchQuery, setSearchQuery] = useState<string>('');

  // Pagination
  const [page, setPage] = useState<number>(0);
  const pageSize = 20;
  const [totalElements, setTotalElements] = useState<number>(0);
  const [totalPages, setTotalPages] = useState<number>(1);

  // Decision Modal
  const [pendingDecision, setPendingDecision] = useState<PendingDecision | null>(null);
  const [rejectReason, setRejectReason] = useState<string>('');
  const [rejectReasonError, setRejectReasonError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState<boolean>(false);

  const getDetailPath = (item: ExpertApprovalQueueItem) => {
    return item.kind === 'CHECKLIST'
      ? `/expert/content-review/checklists/${item.id}`
      : `/expert/content-review/${item.id}`;
  };

  const loadQueue = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await fetchExpertApprovalQueue({
        type: typeFilter === 'ALL' ? undefined : typeFilter,
        stage: stageFilter === 'ALL' ? undefined : (stageFilter as ContentStage),
        keyword: searchQuery.trim() || undefined,
        page,
        size: pageSize,
      });
      setItems(res.content || []);
      setTotalElements(res.totalElements || 0);
      setTotalPages(res.totalPages || 1);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Không thể tải danh sách thẩm định';
      setError(msg);
    } finally {
      setLoading(false);
    }
  }, [typeFilter, stageFilter, searchQuery, page, pageSize]);

  useEffect(() => {
    loadQueue();
  }, [loadQueue]);

  const handleOpenDecision = (item: ExpertApprovalQueueItem, decision: 'APPROVE' | 'REJECT') => {
    setPendingDecision({ item, decision });
    setRejectReason('');
    setRejectReasonError(null);
  };

  const handleCloseDecision = () => {
    if (submitting) return;
    setPendingDecision(null);
    setRejectReason('');
    setRejectReasonError(null);
  };

  const handleSubmitDecision = async () => {
    if (!pendingDecision) return;

    if (pendingDecision.decision === 'REJECT' && !rejectReason.trim()) {
      setRejectReasonError('Vui lòng nhập lý do từ chối / góp ý chỉnh sửa cho tác giả.');
      return;
    }

    setSubmitting(true);
    setError(null);
    try {
      const { item, decision } = pendingDecision;
      if (item.kind === 'CHECKLIST') {
        await decideExpertChecklist(item.id, decision, decision === 'REJECT' ? rejectReason.trim() : undefined);
      } else {
        await decideExpertContent(item.id, decision, decision === 'REJECT' ? rejectReason.trim() : undefined);
      }

      setSuccessMessage(
        decision === 'APPROVE'
          ? `Đã phê duyệt và xuất bản: "${item.title}"`
          : `Đã trả về yêu cầu chỉnh sửa: "${item.title}"`
      );
      handleCloseDecision();
      await loadQueue();
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Thao tác thất bại';
      setError(msg);
    } finally {
      setSubmitting(false);
    }
  };

  const formatDateTime = (iso?: string | null) => {
    if (!iso) return '—';
    return new Date(iso).toLocaleString('vi-VN', { dateStyle: 'short', timeStyle: 'short' });
  };

  return (
    <div className="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
      {/* Header */}
      <div className="mb-6">
        <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <h1 className="text-2xl font-bold text-on-surface flex items-center gap-2">
              <span className="material-symbols-outlined text-primary text-28">rate_review</span>
              Thẩm định & Phê duyệt nội dung
            </h1>
            <p className="mt-1 text-sm text-outline">
              Các bài viết y khoa, giải đáp FAQ và checklist sức khỏe được hệ thống tự động phân chia cho bạn thẩm định
            </p>
          </div>
          <button
            type="button"
            onClick={() => loadQueue()}
            disabled={loading}
            className="inline-flex items-center gap-1.5 self-start rounded-lg border border-outline-variant bg-surface px-3.5 py-2 text-xs font-semibold text-on-surface hover:bg-surface-container-low disabled:opacity-50 cursor-pointer"
          >
            <span className={`material-symbols-outlined text-[18px] ${loading ? 'animate-spin' : ''}`}>
              refresh
            </span>
            Làm mới
          </button>
        </div>
      </div>

      {/* Notifications */}
      {successMessage && (
        <div className="mb-4 flex items-center justify-between rounded-lg bg-emerald-500/10 border border-emerald-500/30 p-4 text-sm text-emerald-600 dark:text-emerald-400">
          <div className="flex items-center gap-2">
            <span className="material-symbols-outlined text-[20px]">check_circle</span>
            <span>{successMessage}</span>
          </div>
          <button
            type="button"
            onClick={() => setSuccessMessage(null)}
            className="text-emerald-700 hover:text-emerald-900 dark:text-emerald-300 cursor-pointer"
          >
            <span className="material-symbols-outlined text-[18px]">close</span>
          </button>
        </div>
      )}

      {error && (
        <div className="mb-4 flex items-center justify-between rounded-lg bg-rose-500/10 border border-rose-500/30 p-4 text-sm text-rose-600 dark:text-rose-400">
          <div className="flex items-center gap-2">
            <span className="material-symbols-outlined text-[20px]">error</span>
            <span>{error}</span>
          </div>
          <button
            type="button"
            onClick={() => setError(null)}
            className="text-rose-700 hover:text-rose-900 dark:text-rose-300 cursor-pointer"
          >
            <span className="material-symbols-outlined text-[18px]">close</span>
          </button>
        </div>
      )}

      {/* Filter Bar */}
      <div className="mb-6 rounded-xl border border-outline-variant/70 bg-surface p-4 shadow-sm">
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
          {/* Search */}
          <div className="lg:col-span-2">
            <label className="block text-xs font-semibold text-on-surface-variant mb-1.5">Tìm kiếm tiêu đề, mô tả</label>
            <div className="relative">
              <span className="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-[18px] text-outline">
                search
              </span>
              <input
                type="text"
                placeholder="Nhập từ khóa tìm kiếm..."
                value={searchQuery}
                onChange={(e) => {
                  setSearchQuery(e.target.value);
                  setPage(0);
                }}
                className="w-full rounded-lg border border-outline-variant bg-surface py-2 pl-9 pr-3 text-xs text-on-surface placeholder:text-outline focus:border-primary focus:outline-none"
              />
            </div>
          </div>

          {/* Type Filter */}
          <div>
            <label className="block text-xs font-semibold text-on-surface-variant mb-1.5">Loại nội dung</label>
            <select
              value={typeFilter}
              onChange={(e) => {
                setTypeFilter(e.target.value as TypeFilter);
                setPage(0);
              }}
              className="w-full rounded-lg border border-outline-variant bg-surface px-3 py-2 text-xs text-on-surface focus:border-primary focus:outline-none"
            >
              <option value="ALL">Tất cả loại ({totalElements})</option>
              <option value="ARTICLE">Bài viết y tế</option>
              <option value="FAQ">Câu hỏi thường gặp (FAQ)</option>
              <option value="CHECKLIST">Checklist hành trình</option>
            </select>
          </div>

          {/* Stage Filter */}
          <div>
            <label className="block text-xs font-semibold text-on-surface-variant mb-1.5">Giai đoạn thai kỳ / chăm bé</label>
            <select
              value={stageFilter}
              onChange={(e) => {
                setStageFilter(e.target.value);
                setPage(0);
              }}
              className="w-full rounded-lg border border-outline-variant bg-surface px-3 py-2 text-xs text-on-surface focus:border-primary focus:outline-none"
            >
              <option value="ALL">Tất cả giai đoạn</option>
              {STAGE_OPTIONS.map((opt) => (
                <option key={opt.value} value={opt.value}>
                  {opt.label}
                </option>
              ))}
            </select>
          </div>
        </div>
      </div>

      {/* Content Table / Cards */}
      <div className="overflow-hidden rounded-xl border border-outline-variant/70 bg-surface shadow-sm">
        {loading ? (
          <div className="flex flex-col items-center justify-center p-12 text-outline">
            <span className="material-symbols-outlined animate-spin text-4xl mb-3 text-primary">progress_activity</span>
            <p className="text-sm font-medium">Đang tải danh sách thẩm định...</p>
          </div>
        ) : items.length === 0 ? (
          <div className="flex flex-col items-center justify-center p-16 text-center">
            <div className="rounded-full bg-surface-container-low p-4 text-outline mb-3">
              <span className="material-symbols-outlined text-4xl">task_alt</span>
            </div>
            <h3 className="text-base font-semibold text-on-surface">Không có nội dung nào chờ thẩm định</h3>
            <p className="mt-1 max-w-sm text-xs text-outline">
              Bạn đã hoàn thành tất cả nhiệm vụ thẩm định được giao hoặc chưa có bài viết/checklist mới cần xem xét.
            </p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs text-on-surface">
              <thead className="border-b border-outline-variant/70 bg-surface-container-low/50 text-[11px] font-semibold uppercase tracking-wider text-on-surface-variant">
                <tr>
                  <th className="px-4 py-3.5">Nội dung</th>
                  <th className="px-4 py-3.5">Phân loại</th>
                  <th className="px-4 py-3.5">Giai đoạn</th>
                  <th className="px-4 py-3.5">Thời điểm gán</th>
                  <th className="px-4 py-3.5 text-right">Thao tác</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-outline-variant/40">
                {items.map((item) => (
                  <tr key={`${item.kind}-${item.id}`} className="hover:bg-surface-container-low/30 transition-colors">
                    {/* Content Title & Summary */}
                    <td className="px-4 py-3.5 max-w-md">
                      <div className="font-medium text-sm text-on-surface hover:text-primary transition-colors cursor-pointer"
                           onClick={() => navigate(getDetailPath(item))}>
                        {item.title}
                      </div>
                      {item.summary && (
                        <p className="mt-0.5 line-clamp-2 text-[11px] text-outline">{item.summary}</p>
                      )}
                      <div className="mt-1 flex items-center gap-2 text-[10px] text-outline">
                        <span>Phiên bản v{item.versionNo ?? 1}</span>
                        {item.itemCount !== undefined && <span>· {item.itemCount} mục checklist</span>}
                        {item.sourceLabel && <span>· Nguồn: {item.sourceLabel}</span>}
                      </div>
                    </td>

                    {/* Type Badge */}
                    <td className="px-4 py-3.5 whitespace-nowrap">
                      <span className={`inline-flex items-center gap-1 rounded-full px-2.5 py-0.5 text-[11px] font-medium ${
                        item.type === 'ARTICLE'
                          ? 'bg-blue-500/10 text-blue-600 dark:text-blue-400 border border-blue-500/20'
                          : item.type === 'FAQ'
                          ? 'bg-amber-500/10 text-amber-600 dark:text-amber-400 border border-amber-500/20'
                          : 'bg-purple-500/10 text-purple-600 dark:text-purple-400 border border-purple-500/20'
                      }`}>
                        <span className="material-symbols-outlined text-[13px]">
                          {item.type === 'ARTICLE' ? 'article' : item.type === 'FAQ' ? 'quiz' : 'checklist'}
                        </span>
                        {TYPE_LABELS[item.type] || item.type}
                      </span>
                    </td>

                    {/* Stage Badge */}
                    <td className="px-4 py-3.5 whitespace-nowrap">
                      <span className="inline-block rounded-md bg-surface-container px-2 py-0.5 text-[11px] font-medium text-on-surface-variant">
                        {item.stage ? STAGE_LABELS[item.stage] : 'Chung'}
                      </span>
                    </td>

                    {/* Assigned At */}
                    <td className="px-4 py-3.5 whitespace-nowrap text-outline text-[11px]">
                      {formatDateTime(item.assignedAt || item.updatedAt)}
                    </td>

                    {/* Actions */}
                    <td className="px-4 py-3.5 whitespace-nowrap text-right">
                      <div className="flex items-center justify-end gap-1.5">
                        <button
                          type="button"
                          onClick={() => navigate(getDetailPath(item))}
                          className="inline-flex items-center gap-1 rounded-md border border-outline-variant bg-surface px-2.5 py-1.5 text-xs font-semibold text-primary hover:bg-surface-container-low cursor-pointer transition-colors"
                          title="Xem chi tiết nội dung và thẩm định"
                        >
                          <span className="material-symbols-outlined text-[16px]">visibility</span>
                          Xem
                        </button>

                        {/* Duyet thang tu bang cho phep ky duyet mot bai chua he mo ra
                            doc. Nut nay dua sang trang chi tiet, noi than bai duoc
                            render va nut phe duyet chi mo sau khi doc het. */}
                        <button
                          type="button"
                          onClick={() => navigate(getDetailPath(item))}
                          className="inline-flex items-center gap-1 rounded-md bg-emerald-600 px-2.5 py-1.5 text-xs font-semibold text-white hover:bg-emerald-700 transition-colors cursor-pointer"
                          title="Mở nội dung để thẩm định và phê duyệt"
                        >
                          <span className="material-symbols-outlined text-[16px]">check</span>
                          Duyệt
                        </button>

                        <button
                          type="button"
                          onClick={() => handleOpenDecision(item, 'REJECT')}
                          className="inline-flex items-center gap-1 rounded-md border border-rose-500/30 bg-rose-500/10 px-2.5 py-1.5 text-xs font-semibold text-rose-600 hover:bg-rose-500/20 transition-colors cursor-pointer"
                          title="Từ chối / Yêu cầu sửa"
                        >
                          <span className="material-symbols-outlined text-[16px]">close</span>
                          Trả về
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        {/* Pagination */}
        {totalPages > 1 && (
          <div className="flex items-center justify-between border-t border-outline-variant/70 px-4 py-3 sm:px-6">
            <div className="text-xs text-outline">
              Hiển thị <span className="font-semibold text-on-surface">{items.length}</span> trên{' '}
              <span className="font-semibold text-on-surface">{totalElements}</span> mục
            </div>
            <div className="flex items-center gap-2">
              <button
                type="button"
                onClick={() => setPage((p) => Math.max(0, p - 1))}
                disabled={page === 0 || loading}
                className="rounded-md border border-outline-variant bg-surface px-2.5 py-1 text-xs font-medium text-on-surface hover:bg-surface-container-low disabled:opacity-50 cursor-pointer"
              >
                Trước
              </button>
              <span className="text-xs text-outline">
                Trang {page + 1} / {totalPages}
              </span>
              <button
                type="button"
                onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
                disabled={page >= totalPages - 1 || loading}
                className="rounded-md border border-outline-variant bg-surface px-2.5 py-1 text-xs font-medium text-on-surface hover:bg-surface-container-low disabled:opacity-50 cursor-pointer"
              >
                Sau
              </button>
            </div>
          </div>
        )}
      </div>

      {/* Decision Dialog */}
      {pendingDecision && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
          <div className="w-full max-w-lg rounded-2xl border border-outline-variant/70 bg-surface p-6 shadow-xl">
            <div className="flex items-center justify-between border-b border-outline-variant/70 pb-3 mb-4">
              <h3 className="text-base font-bold text-on-surface flex items-center gap-2">
                <span className={`material-symbols-outlined ${pendingDecision.decision === 'APPROVE' ? 'text-emerald-600' : 'text-rose-600'}`}>
                  {pendingDecision.decision === 'APPROVE' ? 'verified' : 'assignment_late'}
                </span>
                {pendingDecision.decision === 'APPROVE' ? 'Xác nhận phê duyệt nội dung' : 'Yêu cầu chỉnh sửa / Trả về'}
              </h3>
              <button
                type="button"
                onClick={handleCloseDecision}
                disabled={submitting}
                className="text-outline hover:text-on-surface cursor-pointer"
              >
                <span className="material-symbols-outlined text-[20px]">close</span>
              </button>
            </div>

            <div className="space-y-4 text-xs text-on-surface">
              <div className="rounded-lg bg-surface-container-low p-3">
                <p className="font-semibold text-sm text-on-surface">{pendingDecision.item.title}</p>
                <div className="mt-1 flex flex-wrap gap-2 text-[11px] text-outline">
                  <span>Loại: {TYPE_LABELS[pendingDecision.item.type]}</span>
                  <span>· Giai đoạn: {pendingDecision.item.stage ? STAGE_LABELS[pendingDecision.item.stage] : 'Chung'}</span>
                  <span>· Phiên bản: v{pendingDecision.item.versionNo ?? 1}</span>
                </div>
              </div>

              {pendingDecision.decision === 'APPROVE' ? (
                <p className="text-on-surface-variant text-xs leading-relaxed">
                  Khi bạn phê duyệt, nội dung này sẽ chính thức được kích hoạt và xuất bản trên Thư viện chăm sóc cho các bà mẹ và gia đình. Bạn có chắc chắn muốn xuất bản không?
                </p>
              ) : (
                <div>
                  <label className="block font-semibold text-on-surface-variant mb-1">
                    Lý do từ chối & Hướng dẫn chỉnh sửa cho tác giả <span className="text-rose-500">*</span>
                  </label>
                  <textarea
                    rows={4}
                    value={rejectReason}
                    onChange={(e) => {
                      setRejectReason(e.target.value);
                      if (rejectReasonError) setRejectReasonError(null);
                    }}
                    placeholder="Ghi rõ các điểm chưa chính xác về chuyên môn y khoa hoặc cần bổ sung tài liệu..."
                    className="w-full rounded-lg border border-outline-variant bg-surface p-2.5 text-xs text-on-surface focus:border-primary focus:outline-none"
                  />
                  {rejectReasonError && (
                    <p className="mt-1 text-xs text-rose-500">{rejectReasonError}</p>
                  )}
                </div>
              )}
            </div>

            <div className="mt-6 flex items-center justify-end gap-3 border-t border-outline-variant/70 pt-4">
              <button
                type="button"
                onClick={handleCloseDecision}
                disabled={submitting}
                className="rounded-lg border border-outline-variant bg-surface px-4 py-2 text-xs font-semibold text-on-surface hover:bg-surface-container-low disabled:opacity-50 cursor-pointer"
              >
                Hủy
              </button>
              <button
                type="button"
                onClick={handleSubmitDecision}
                disabled={submitting}
                className={`inline-flex items-center gap-1.5 rounded-lg px-4 py-2 text-xs font-semibold text-white transition-colors disabled:opacity-50 cursor-pointer ${
                  pendingDecision.decision === 'APPROVE'
                    ? 'bg-emerald-600 hover:bg-emerald-700'
                    : 'bg-rose-600 hover:bg-rose-700'
                }`}
              >
                {submitting ? (
                  <>
                    <span className="material-symbols-outlined animate-spin text-[16px]">progress_activity</span>
                    Đang xử lý...
                  </>
                ) : (
                  <>
                    <span className="material-symbols-outlined text-[16px]">
                      {pendingDecision.decision === 'APPROVE' ? 'check' : 'send'}
                    </span>
                    {pendingDecision.decision === 'APPROVE' ? 'Xác nhận xuất bản' : 'Gửi yêu cầu sửa'}
                  </>
                )}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
