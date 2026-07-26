import { useState, useEffect, useCallback } from 'react';
import { listContributionsForReview, approveContribution, rejectContribution, type PaginatedContributionResponse, type ContributionResponse, type ContributionAttachmentResponse } from '../services/expertApi';

const STATUS_LABELS: Record<ContributionResponse['status'], string> = {
  DRAFT: 'Bản nháp',
  SUBMITTED: 'Đã gửi duyệt',
  APPROVED: 'Đã duyệt',
  REJECTED: 'Từ chối',
};

const STATUS_CLASSES: Record<ContributionResponse['status'], string> = {
  DRAFT: 'bg-gray-50 text-gray-700 border-gray-200',
  SUBMITTED: 'bg-yellow-50 text-yellow-700 border-yellow-200',
  APPROVED: 'bg-green-50 text-green-700 border-green-200',
  REJECTED: 'bg-red-50 text-red-700 border-red-200',
};

function fileExt(url: string): string {
  const m = url.split('?')[0].split('.').pop();
  return (m && ['pdf', 'doc', 'docx', 'jpg', 'jpeg', 'png', 'gif', 'webp', 'bmp', 'txt', 'rtf', 'odt'].includes(m.toLowerCase())) ? m.toLowerCase() : '';
}

function isImage(url: string): boolean {
  return ['jpg', 'jpeg', 'png', 'gif', 'webp', 'bmp'].includes(fileExt(url));
}

function fmtDate(iso: string): string {
  try {
    return new Date(iso).toLocaleString('vi-VN');
  } catch {
    return iso;
  }
}

export default function AdminContributionReviewQueuePage() {
  const [items, setItems] = useState<ContributionResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const SIZE = 10;
  const [statusFilter, setStatusFilter] = useState<'SUBMITTED' | 'APPROVED' | 'REJECTED' | 'DRAFT'>('SUBMITTED');
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [actionId, setActionId] = useState<string | null>(null);
  const [noteText, setNoteText] = useState('');
  const [viewFileUrl, setViewFileUrl] = useState<string | null>(null);

  const load = useCallback(async () => {
    setLoading(true); setError(null);
    try {
      const res: PaginatedContributionResponse = await listContributionsForReview({ status: statusFilter, page, size: SIZE });
      setItems(res.content);
      setTotalPages(res.totalPages);
      setTotalElements(res.totalElements);
    } catch (e: any) {
      setError(e.response?.data?.message || 'Không tải được danh sách');
    } finally {
      setLoading(false);
    }
  }, [page, statusFilter]);

  useEffect(() => { load(); }, [load]);

  const handleReview = async (contributionId: string, decision: 'APPROVED' | 'REJECTED') => {
    setActionId(contributionId);
    try {
      if (decision === 'APPROVED') {
        await approveContribution(contributionId);
      } else {
        await rejectContribution(contributionId, noteText.trim() || 'Không đạt yêu cầu chất lượng');
      }
      await load();
      setSelectedId(null);
      setNoteText('');
    } catch (e: any) {
      alert(e.response?.data?.message || 'Xử lý thất bại');
    } finally {
      setActionId(null);
    }
  };

  const selected = items.find(c => c.id === selectedId);

  const renderFilePreview = (att: ContributionAttachmentResponse) => {
    if (!att.presignedUrl) return <p className="text-sm text-gray-500">Không có URL</p>;
    if (isImage(att.presignedUrl)) {
      return <img src={att.presignedUrl} alt={att.originalName ?? 'attachment'} className="max-w-full h-48 object-cover rounded border" onClick={() => setViewFileUrl(att.presignedUrl!)} style={{cursor: 'zoom-in'}} />;
    }
    return (
      <div className="flex items-center justify-center w-full h-48 bg-gray-50 rounded border" onClick={() => setViewFileUrl(att.presignedUrl!)} style={{cursor: 'pointer'}}>
        <div className="text-center">
          <div className="text-4xl mb-1">📄</div>
          <p className="text-sm font-medium text-blue-700 truncate w-60 mx-auto">{att.originalName ?? 'attachment'}</p>
          <p className="text-xs text-blue-500">{(att.fileSizeBytes / 1024).toFixed(1)} KB</p>
        </div>
      </div>
    );
  };

  if (loading) return <div className="flex justify-center py-12">Đang tải...</div>;
  if (error) return <div className="p-6 text-center text-red-600">{error}</div>;

  return (
    <main className="p-6 max-w-7xl mx-auto">
      <div className="flex flex-col lg:flex-row lg:items-center lg:justify-between gap-4 mb-6">
        <div>
          <h1 className="text-2xl font-bold text-on-surface">Hàng chờ duyệt đóng góp y khoa</h1>
          <p className="text-gray-500 mt-1">Xét duyệt bài viết chuyên gia gửi chờ duyệt</p>
        </div>
        <div className="flex gap-2 items-center">
          <select
            value={statusFilter}
            onChange={e => { setStatusFilter(e.target.value as any); setPage(0); }}
            className="rounded border border-gray-300 px-3 py-2 focus:border-primary focus:ring-1 focus:ring-primary"
          >
            <option value="SUBMITTED">Đã gửi duyệt</option>
            <option value="APPROVED">Đã duyệt</option>
            <option value="REJECTED">Đã từ chối</option>
            <option value="DRAFT">Bản nháp</option>
          </select>
        </div>
      </div>

      <div className="flex gap-6">
        {/* List */}
        <div className="flex-1 min-w-0">
          <div className="bg-white rounded-lg border border-gray-200 shadow-sm">
            <div className="overflow-x-auto">
              <table className="w-full">
                <thead className="bg-gray-50">
                  <tr>
                    <th className="px-4 py-3 text-left text-xs font-semibold text-gray-500 uppercase tracking-wider">Tiêu đề</th>
                    <th className="px-4 py-3 text-left text-xs font-semibold text-gray-500 uppercase tracking-wider hidden md:table-cell">Chuyên khoa</th>
                    <th className="px-4 py-3 text-left text-xs font-semibold text-gray-500 uppercase tracking-wider hidden lg:table-cell">Bệnh viện</th>
                    <th className="px-4 py-3 text-left text-xs font-semibold text-gray-500 uppercase tracking-wider hidden lg:table-cell">Tác giả</th>
                    <th className="px-4 py-3 text-left text-xs font-semibold text-gray-500 uppercase tracking-wider">Trạng thái</th>
                    <th className="px-4 py-3 text-left text-xs font-semibold text-gray-500 uppercase tracking-wider">Cập nhật</th>
                    <th className="px-4 py-3 text-center text-xs font-semibold text-gray-500 uppercase tracking-wider w-24">Hành động</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-100">
                  {items.length === 0 ? (
                    <tr>
                      <td colSpan={7} className="px-4 py-12 text-center text-gray-500">
                        Không có bài viết nào ở trạng thái "<span className="font-medium">{statusFilter}</span>"
                      </td>
                    </tr>
                  ) : (
                    items.map(c => (
                      <tr
                        key={c.id}
                        className={selectedId === c.id ? 'bg-primary/5' : 'hover:bg-gray-50'}
                        onClick={() => setSelectedId(c.id)}
                      >
                        <td className="px-4 py-4">
                          <p className="font-medium text-gray-900 truncate max-w-xs">{c.title}</p>
                          <p className="text-sm text-gray-500 truncate max-w-xs mt-1">{c.content.substring(0, 80)}...</p>
                        </td>
                        <td className="px-4 py-4 text-gray-600 text-sm hidden md:table-cell">{c.specialtyId ?? '—'}</td>
                        <td className="px-4 py-4 text-gray-600 text-sm hidden lg:table-cell">{c.hospitalId ?? '—'}</td>
                        <td className="px-4 py-4 text-gray-600 text-sm hidden lg:table-cell">{c.expertUserId}</td>
                        <td className="px-4 py-4">
                          <span className={`inline-flex px-2 py-1 text-xs font-medium rounded-full border ${STATUS_CLASSES[c.status] || ''}`}>
                            {STATUS_LABELS[c.status] ?? c.status}
                          </span>
                        </td>
                        <td className="px-4 py-4 text-gray-500 text-sm hidden sm:table-cell">
                          {new Date(c.updatedAt).toLocaleString('vi-VN')}
                        </td>
                        <td className="px-4 py-4 text-center">
                          <button
                            onClick={e => { e.stopPropagation(); setSelectedId(c.id); }}
                            className={`px-3 py-1.5 text-sm rounded ${
                              selectedId === c.id
                                ? 'bg-primary text-white'
                                : 'border border-gray-300 text-gray-700 hover:bg-gray-50'
                            }`}
                          >
                            {selectedId === c.id ? 'Đang xem' : 'Xem'}
                          </button>
                        </td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>

            {totalPages > 1 && (
              <div className="flex items-center justify-center gap-2 p-4 border-t border-gray-100">
                <button
                  onClick={() => setPage(p => Math.max(0, p - 1))}
                  disabled={page === 0}
                  className="px-4 py-2 rounded border border-gray-300 disabled:opacity-50"
                >
                  Trước
                </button>
                <span className="text-sm text-gray-600">
                  Trang {page + 1} / {totalPages} · Tổng: {totalElements}
                </span>
                <button
                  onClick={() => setPage(p => Math.min(totalPages - 1, p + 1))}
                  disabled={page >= totalPages - 1}
                  className="px-4 py-2 rounded border border-gray-300 disabled:opacity-50"
                >
                  Tiếp
                </button>
              </div>
            )}
          </div>
        </div>

        {/* Detail panel */}
        <div className="lg:w-96 flex-shrink-0">
          {selected ? (
            <div className="sticky top-24 bg-white rounded-lg border border-gray-200 shadow-sm flex flex-col h-[calc(100vh-8rem)]">
              <div className="flex items-center justify-between border-b border-gray-200 p-4">
                <h3 className="text-sm font-semibold text-on-surface">Chi tiết bài viết</h3>
                <button
                  onClick={() => { setSelectedId(null); setNoteText(''); }}
                  className="p-1 text-gray-400 hover:text-gray-600"
                >
                  ✕
                </button>
              </div>

              <div className="flex-1 overflow-y-auto p-4 space-y-4">
                {/* Basic Info */}
                <div className="space-y-2 rounded border border-gray-200 bg-gray-50 p-4">
                  <h4 className="mb-3 text-sm font-semibold text-primary">Thông tin cơ bản</h4>
                  <p className="text-sm"><strong>Tiêu đề:</strong> {selected.title}</p>
                  <p className="text-sm"><strong>Chuyên khoa:</strong> {selected.specialtyId ?? '—'}</p>
                  <p className="text-sm"><strong>Bệnh viện:</strong> {selected.hospitalId ?? '—'}</p>
                  <p className="text-sm"><strong>Tác giả:</strong> {selected.expertUserId}</p>
                  <p className="text-sm"><strong>Phiên bản:</strong> #{selected.version}</p>
                  <p className="text-sm"><strong>Tạo:</strong> {fmtDate(selected.createdAt)}</p>
                  <p className="text-sm"><strong>Cập nhật:</strong> {fmtDate(selected.updatedAt)}</p>
                </div>

                {/* Content */}
                <div className="space-y-2 rounded border border-gray-200 bg-white p-4">
                  <h4 className="mb-2 text-sm font-semibold text-primary">Nội dung</h4>
                  <div className="prose prose-sm max-w-none text-gray-900" dangerouslySetInnerHTML={{ __html: selected.content.replace(/\n/g, '<br>') }} />
                </div>

                {/* Attachments */}
                {(selected.attachments?.length ?? 0) > 0 && (
                  <div className="space-y-2 rounded border border-gray-200 bg-white p-4">
                    <h4 className="mb-2 text-sm font-semibold text-primary">Tệp đính kèm ({selected.attachments!.length})</h4>
                    <div className="flex flex-wrap gap-3">
                      {selected.attachments!.map(att => (
                        <div key={att.id} className="w-36" onClick={() => setViewFileUrl(att.presignedUrl!)} style={{cursor: att.presignedUrl ? 'pointer' : 'default'}}>
                          {renderFilePreview(att)}
                          <p className="text-xs text-gray-600 truncate mt-1 text-center">{att.originalName ?? `${att.kind.toLowerCase()} ${att.displayOrder + 1}`}</p>
                          <p className="text-xs text-gray-400 text-center">{att.kind} · {att.accessMode}</p>
                        </div>
                      ))}
                    </div>
                  </div>
                )}

                {/* Review actions - only for SUBMITTED */}
                {selected.status === 'SUBMITTED' && (
                  <div className="border-t border-gray-200 pt-4 space-y-3">
                    <label className="block text-sm font-medium text-gray-700">Ghi chú (tuỳ chọn)</label>
                    <textarea
                      rows={4}
                      value={noteText}
                      onChange={e => setNoteText(e.target.value)}
                      placeholder="Ghi chú cho quyết định duyệt/từ chối…"
                      className="w-full rounded border border-gray-300 px-3 py-2 text-sm focus:border-primary focus:ring-1 focus:ring-primary"
                    />
                    <div className="flex gap-3">
                      <button
                        onClick={() => handleReview(selected.id, 'APPROVED')}
                        disabled={actionId === selected.id}
                        className="flex-1 rounded bg-green-600 px-3 py-2 text-sm font-medium text-white hover:bg-green-700 disabled:opacity-50"
                      >
                        {actionId === selected.id ? 'Đang duyệt…' : 'Duyệt'}
                      </button>
                      <button
                        onClick={() => handleReview(selected.id, 'REJECTED')}
                        disabled={actionId === selected.id}
                        className="flex-1 rounded bg-red-600 px-3 py-2 text-sm font-medium text-white hover:bg-red-700 disabled:opacity-50"
                      >
                        Từ chối
                      </button>
                    </div>
                  </div>
                )}

                {selected.status === 'REJECTED' && selected.rejectionReason && (
                  <div className="rounded bg-red-50 border border-red-200 p-3 text-sm text-red-700">
                    <strong>Lý do từ chối:</strong> {selected.rejectionReason}
                  </div>
                )}
              </div>
            </div>
          ) : (
            <div className="sticky top-24 bg-white rounded-lg border border-gray-200 shadow-sm p-8 text-center">
              <div className="text-4xl mb-3">📝</div>
              <p className="text-lg font-semibold text-gray-900">Chọn một bài viết</p>
              <p className="text-gray-500 mt-1">Nhấn vào hàng trong bảng để xem chi tiết và duyệt</p>
            </div>
          )}
        </div>
      </div>

      {viewFileUrl && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50" onClick={() => setViewFileUrl(null)}>
          <div className="bg-white rounded-lg max-w-4xl w-full mx-4 max-h-[90vh] overflow-auto" onClick={e => e.stopPropagation()}>
            <div className="sticky top-0 bg-white border-b border-gray-200 p-4 flex items-center justify-between">
              <h3 className="font-semibold">Xem tài liệu</h3>
              <button onClick={() => setViewFileUrl(null)} className="p-2 text-gray-400 hover:text-gray-600">✕</button>
            </div>
            <iframe
              src={viewFileUrl}
              className="w-full h-[80vh] border-0"
              title="Document preview"
              sandbox="allow-scripts allow-same-origin"
            />
          </div>
        </div>
      )}
    </main>
  );
}