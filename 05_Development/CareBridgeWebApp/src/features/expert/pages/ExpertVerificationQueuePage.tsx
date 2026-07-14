import { useState, useEffect, useCallback } from 'react';
import apiClient from '../../../shared/api/apiClient';

interface CredentialItem {
  credentialId: string;
  expertProfileId: string;
  credentialType: string;
  credentialNumber: string;
  issuer: string;
  issuedDate: string;
  expiryDate: string;
  fileUrl: string;
  reviewStatus: string;
  reviewNote: string;
  createdAt: string;
  expertName?: string;
  specialty?: string;
  professionalTitle?: string;
  experienceYears?: number;
  workplace?: string;
  phone?: string;
  email?: string;
  avatarUrl?: string;
}

const credentialTypeLabels: Record<string, string> = {
  CERTIFICATE: 'Chứng chỉ',
  DEGREE: 'Bằng cấp chuyên môn',
  IDENTITY_DOCUMENT: 'Giấy tờ định danh',
  MEDICAL_LICENSE: 'Giấy phép hành nghề',
  PROFESSIONAL_LICENSE: 'Giấy phép hành nghề',
};

const credentialTypeLabel = (credentialType: string) =>
  credentialTypeLabels[credentialType] ?? credentialType.replaceAll('_', ' ');

const FILE_EXTS = Object.freeze(['pdf','doc','docx','txt','rtf','odt','jpg','jpeg','png','gif','webp','bmp']);

function getFileExt(url: string): string {
  const m = url.split('?')[0].split('.').pop();
  return (m && FILE_EXTS.includes(m.toLowerCase())) ? m.toLowerCase() : '';
}

function isImageFile(url: string): boolean {
  return ['jpg','jpeg','png','gif','webp','bmp'].includes(getFileExt(url));
}

function isPdfFile(url: string): boolean {
  return getFileExt(url) === 'pdf';
}

function getFileName(url: string): string {
  const clean = url.split('?')[0];
  return clean.substring(clean.lastIndexOf('/') + 1) || 'Tài liệu';
}

/* ── Full-screen document viewer modal ──────────────────────────────── */

function DocViewModal({ url, fileName, onClose }: { url: string; fileName?: string; onClose: () => void }) {
  const [imgError, setImgError] = useState(false);

  const isImage = isImageFile(url);
  const isPdf = isPdfFile(url);

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 p-4" onClick={onClose}>
      <div
        className="bg-white rounded-2xl shadow-2xl w-full max-w-5xl max-h-[90vh] flex flex-col"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="flex items-center justify-between px-5 py-3 border-b">
          <div className="flex items-center gap-2 min-w-0">
            <span className="material-symbols-outlined text-primary">
              {isImage ? 'image' : isPdf ? 'description' : 'insert_drive_file'}
            </span>
            <h3 className="font-semibold text-gray-800 truncate text-sm">{fileName || getFileName(url)}</h3>
          </div>
          <div className="flex items-center gap-2">
            {!isImage && (
              <a
                href={url}
                download
                className="flex items-center gap-1 px-3 py-1.5 rounded-lg bg-primary text-white text-sm font-medium hover:bg-primary/90"
              >
                <span className="material-symbols-outlined text-[18px]">download</span>
                Tải về
              </a>
            )}
            <button
              onClick={onClose}
              className="text-gray-400 hover:text-gray-700 text-2xl leading-none w-8 h-8 flex items-center justify-center rounded-full hover:bg-gray-100"
            >
              ×
            </button>
          </div>
        </div>

        <div className="flex-1 overflow-auto p-4 bg-gray-50 flex items-center justify-center">
          {isImage && !imgError ? (
            <img
              src={url}
              alt={fileName || 'Tài liệu'}
              className="max-w-full max-h-[72vh] object-contain rounded-lg shadow-sm"
              onError={() => setImgError(true)}
            />
          ) : isPdf ? (
            <div className="w-full h-[75vh]">
              <iframe
                src={url}
                className="w-full h-full rounded-lg border border-gray-200"
                title={fileName || 'PDF Viewer'}
              />
            </div>
          ) : (
            <div className="flex flex-col items-center gap-4 py-10">
              <svg className="w-20 h-20 text-gray-300" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
              </svg>
              <p className="text-gray-500 text-sm">Không thể xem trước tệp này.</p>
              <a
                href={url}
                download
                className="flex items-center gap-2 px-5 py-2.5 bg-primary text-white rounded-lg font-medium hover:bg-primary/90"
              >
                <span className="material-symbols-outlined text-[18px]">download</span>
                Tải tài liệu về máy
              </a>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

/* ── Tiny inline image preview (no modal) ──────────────────────────── */

function InlineImage({ url, alt }: { url: string; alt: string }) {
  const [err, setErr] = useState(false);
  if (err) {
    return <p className="text-xs text-red-500 py-4">Không thể tải ảnh</p>;
  }
  return (
    <img
      src={url}
      alt={alt}
      className="max-w-full max-h-[55vh] object-contain rounded-lg border border-outline-variant cursor-pointer"
      onClick={() => {}}
      onError={() => setErr(true)}
    />
  );
}

/* ── Main page ──────────────────────────────────────────────────────── */

export default function ExpertVerificationQueuePage() {
  const [items, setItems] = useState<CredentialItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [typeFilter, setTypeFilter] = useState('');
  const [actionId, setActionId] = useState<string | null>(null);
  const [noteText, setNoteText] = useState('');
  const [viewFileUrl, setViewFileUrl] = useState<string | null>(null);

  const fetchQueue = useCallback(async () => {
    try {
      setLoading(true);
      const params: Record<string, string> = {};
      if (typeFilter.trim()) params.credentialType = typeFilter.trim();
      const qs = new URLSearchParams(params).toString();
      const { data } = await apiClient.get(`/api/v1/expert/credentials/pending?${qs}`);
      const content: CredentialItem[] = data.data?.content ?? data.data ?? [];
      setItems(content);
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : 'Không thể tải danh sách chờ xác minh');
    } finally {
      setLoading(false);
    }
  }, [typeFilter]);

  useEffect(() => { fetchQueue(); }, [fetchQueue]);

  const reviewCredential = async (credentialId: string, status: 'APPROVED' | 'REJECTED') => {
    setActionId(credentialId);
    try {
      await apiClient.put(`/api/v1/expert/credentials/${credentialId}/review`, {
        reviewStatus: status,
        reviewNote: noteText.trim() || undefined,
      });
      setItems((prev) => prev.filter((i) => i.credentialId !== credentialId));
      setSelectedId(null);
      setNoteText('');
    } catch {
      alert('Thao tác thất bại');
    } finally {
      setActionId(null);
    }
  };

  const selected = items.find((i) => i.credentialId === selectedId);

  const fmtDate = (d?: string) => {
    if (!d) return '—';
    const date = new Date(d);
    if (isNaN(date.getTime())) return d;
    return date.toLocaleDateString('vi-VN');
  };

  const openFile = (url: string) => setViewFileUrl(url);

  const renderFilePreview = (item: CredentialItem) => {
    if (!item.fileUrl) return null;
    const url = item.fileUrl;

    if (isImageFile(url)) {
      return (
        <div
          className="cursor-pointer"
          onClick={() => setViewFileUrl(url)}
          role="button"
          tabIndex={0}
          onKeyDown={(e) => { if (e.key === 'Enter') setViewFileUrl(url); }}
        >
          <InlineImage url={url} alt="Tài liệu chứng chỉ" />
          <p className="text-xs text-gray-400 mt-1">Nhấn vào ảnh để xem toàn màn hình</p>
        </div>
      );
    }

    if (isPdfFile(url)) {
      return (
        <div className="border border-outline-variant rounded-lg overflow-hidden">
          <iframe src={url} className="w-full h-[50vh]" title="PDF tài liệu" />
        </div>
      );
    }

    return (
      <div className="flex items-center gap-3 p-4 bg-gray-50 rounded-xl">
        <span className="material-symbols-outlined text-[32px] text-gray-400 uppercase">
          description
        </span>
        <div className="flex-1 min-w-0">
          <p className="text-sm text-gray-700 font-medium truncate">{getFileName(url)}</p>
          <p className="text-xs text-gray-400">{getFileExt(url)?.toUpperCase()} · văn bản</p>
        </div>
        <div className="flex gap-2">
          <button
            onClick={() => setViewFileUrl(url)}
            className="px-3 py-1.5 text-xs rounded-lg border border-primary text-primary font-medium hover:bg-primary-container/40"
          >
            Xem trước
          </button>
          <a
            href={url}
            download
            className="flex items-center gap-1 px-3 py-1.5 text-xs rounded-lg bg-primary text-white font-medium hover:bg-primary/90"
          >
            <span className="material-symbols-outlined text-[14px]">download</span>
            Tải về
          </a>
        </div>
      </div>
    );
  };

  return (
    <main className="min-h-screen bg-[#F6F1EC] p-5 md:p-10">
      <div className="mx-auto max-w-7xl">
        <header className="mb-6 rounded-3xl border border-outline-variant/40 bg-surface px-6 py-6 shadow-sm md:px-8">
          <div className="flex items-start gap-4">
            <span className="material-symbols-outlined rounded-2xl bg-primary-container p-3 text-primary">fact_check</span>
            <div>
              <p className="text-sm font-semibold text-primary">Quản trị chuyên gia</p>
              <h1 className="mt-1 text-2xl font-bold text-on-surface">Hàng đợi xác minh chuyên gia</h1>
              <p className="mt-2 text-sm leading-6 text-on-surface-variant">Kiểm tra giấy tờ và đưa ra quyết định xác minh hồ sơ chuyên gia.</p>
            </div>
          </div>
        </header>

        <div className="flex flex-col gap-5 lg:flex-row lg:items-start">
          <div
            className={`flex flex-col rounded-3xl border border-outline-variant/40 bg-surface shadow-sm ${selectedId ? 'w-full lg:w-1/2' : 'w-full'}`}
          >
            <div className="p-4 pb-3 space-y-3">
              <h2 className="text-xl font-bold text-on-surface">Hồ sơ chờ xử lý</h2>
              <div className="flex gap-2">
                <input
                  type="text"
                  placeholder="Lọc theo loại bằng cấp…"
                  value={typeFilter}
                  onChange={(e) => setTypeFilter(e.target.value)}
                  onKeyDown={(e) => e.key === 'Enter' && fetchQueue()}
                  className="flex-1 rounded-full border border-outline-variant bg-surface-container-lowest px-4 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary/30"
                />
                <button
                  onClick={fetchQueue}
                  className="rounded-full px-4 py-2 text-sm font-medium bg-primary text-white hover:brightness-110 transition"
                >
                  Tìm
                </button>
              </div>
            </div>

            <div className="flex-1 overflow-y-auto px-4 pb-4 space-y-3">
              {loading && items.length === 0 && (
                <div className="flex flex-col items-center gap-3 py-20">
                  <span className="material-symbols-outlined animate-spin text-[36px] text-primary">progress_activity</span>
                  <p className="text-sm text-on-surface-variant">Đang tải…</p>
                </div>
              )}
              {error && (
                <div className="p-4 rounded-2xl bg-red-100 text-red-800 text-sm">
                  {error}
                  <button onClick={fetchQueue} className="ml-3 underline font-semibold">Thử lại</button>
                </div>
              )}
              {items.map((item) => {
                const active = item.credentialId === selectedId;
                return (
                  <button
                    key={item.credentialId}
                    onClick={() => { setSelectedId(item.credentialId); setNoteText(''); }}
                    className={`w-full rounded-2xl p-4 text-left border transition-shadow ${
                      active
                        ? 'border-primary bg-primary-container/30 shadow-md'
                        : 'border-outline-variant/60 bg-surface hover:border-primary/30 hover:shadow-sm'
                    }`}
                  >
                    <div className="flex items-start justify-between gap-3">
                      <div className="flex-1 min-w-0">
                        <h3 className="text-sm font-semibold text-on-surface leading-snug">{credentialTypeLabel(item.credentialType)}</h3>
                        {item.expertName && (
                          <p className="text-xs text-on-surface-variant mt-1">Chuyên gia: {item.expertName}</p>
                        )}
                        <div className="flex flex-wrap gap-2 mt-2">
                          {item.issuer && (
                            <span className="rounded-full px-2 py-0.5 bg-surface-container-low text-xs text-on-surface-variant">
                              {item.issuer}
                            </span>
                          )}
                          {item.specialty && (
                            <span className="rounded-full px-2 py-0.5 bg-blue-50 text-xs text-blue-700 font-medium">
                              {item.specialty}
                            </span>
                          )}
                          <span className="rounded-full px-2 py-0.5 bg-amber-100 text-xs text-amber-700 font-bold">
                            Chờ xác minh
                          </span>
                        </div>
                        <p className="text-xs text-on-surface-variant mt-2">{timeAgo(item.createdAt)}</p>
                      </div>
                    </div>
                  </button>
                );
              })}
              {items.length === 0 && !loading && !error && (
                <div className="py-16 text-center text-on-surface-variant">
                  <span className="material-symbols-outlined text-[48px] block mb-3 opacity-40">verified</span>
                  <p className="text-base font-medium">Không có hồ sơ chờ xác minh</p>
                </div>
              )}
            </div>
          </div>

          {selected && (
            <div className="flex w-full flex-col rounded-3xl border border-outline-variant/40 bg-surface-container-lowest shadow-sm lg:w-1/2">
              <div className="p-5 border-b border-outline-variant/50 bg-surface flex items-center justify-between">
                <h3 className="text-base font-semibold text-on-surface leading-snug">Chi tiết hồ sơ xác minh</h3>
                <button
                  onClick={() => { setSelectedId(null); setNoteText(''); }}
                  className="text-on-surface-variant hover:text-on-surface text-xl leading-none"
                >
                  ×
                </button>
              </div>

              <div className="flex-1 p-5 space-y-4 overflow-y-auto">
                <div className="rounded-2xl border border-outline-variant/60 bg-surface p-4 space-y-3">
                  <h4 className="text-sm font-bold text-primary mb-2">Thông tin chuyên gia</h4>
                  {selected.avatarUrl && (
                    <img
                      src={selected.avatarUrl}
                      alt={selected.expertName || 'Avatar'}
                      className="w-16 h-16 rounded-full object-cover border-2 border-outline-variant"
                    />
                  )}
                  <p className="text-sm"><strong>Tên:</strong> {selected.expertName ?? '—'}</p>
                  {selected.specialty && <p className="text-sm"><strong>Chuyên khoa:</strong> {selected.specialty}</p>}
                  {selected.professionalTitle && <p className="text-sm"><strong>Chức danh:</strong> {selected.professionalTitle}</p>}
                  {selected.experienceYears != null && <p className="text-sm"><strong>Kinh nghiệm:</strong> {selected.experienceYears} năm</p>}
                  {selected.workplace && <p className="text-sm"><strong>Nơi công tác:</strong> {selected.workplace}</p>}
                  {selected.phone && <p className="text-sm"><strong>Điện thoại:</strong> {selected.phone}</p>}
                  {selected.email && <p className="text-sm"><strong>Email:</strong> {selected.email}</p>}
                </div>

                <div className="rounded-2xl border border-outline-variant/60 bg-surface p-4 space-y-2">
                  <h4 className="text-sm font-bold text-primary mb-2">Thông tin chứng chỉ</h4>
                  <p className="text-sm"><strong>Loại:</strong> {credentialTypeLabel(selected.credentialType)}</p>
                  {selected.credentialNumber && <p className="text-sm"><strong>Số hiệu:</strong> {selected.credentialNumber}</p>}
                  {selected.issuer && <p className="text-sm"><strong>Cơ quan cấp:</strong> {selected.issuer}</p>}
                  <p className="text-sm"><strong>Ngày cấp:</strong> {fmtDate(selected.issuedDate)}</p>
                  {selected.expiryDate && <p className="text-sm"><strong>Ngày hết hạn:</strong> {fmtDate(selected.expiryDate)}</p>}
                </div>

                {selected.fileUrl && (
                  <div className="rounded-2xl border border-outline-variant/60 bg-surface p-4 space-y-2">
                    <h4 className="text-sm font-bold text-primary mb-2">Tài liệu đính kèm</h4>
                    {renderFilePreview(selected)}
                  </div>
                )}

                {selected.reviewNote && (
                  <div className="rounded-2xl border border-outline-variant/60 bg-surface p-4 space-y-2">
                    <p className="text-sm"><strong>Ghi chú đánh giá trước:</strong></p>
                    <p className="text-sm text-gray-700 whitespace-pre-wrap">{selected.reviewNote}</p>
                  </div>
                )}

                <div>
                  <label className="block text-sm font-semibold text-on-surface mb-2">Ghi chú (tuỳ chọn)</label>
                  <textarea
                    rows={3}
                    value={noteText}
                    onChange={(e) => setNoteText(e.target.value)}
                    placeholder="Nhập ghi chú cho quyết định…"
                    className="w-full rounded-2xl border border-outline-variant bg-surface p-4 text-sm leading-relaxed focus:outline-none focus:ring-2 focus:ring-primary/30 resize-none"
                  />
                </div>
              </div>

              <div className="p-4 border-t border-outline-variant/50 bg-surface flex gap-3">
                <button
                  onClick={() => reviewCredential(selected.credentialId, 'APPROVED')}
                  disabled={actionId === selected.credentialId}
                  className="flex-1 h-12 rounded-full bg-green-600 text-white font-semibold text-base hover:brightness-110 transition disabled:opacity-50"
                >
                  {actionId === selected.credentialId ? 'Đang xử lý…' : 'Duyệt'}
                </button>
                <button
                  onClick={() => reviewCredential(selected.credentialId, 'REJECTED')}
                  disabled={actionId === selected.credentialId}
                  className="flex-1 h-12 rounded-full bg-red-600 text-white font-semibold text-base hover:brightness-110 transition disabled:opacity-50"
                >
                  Từ chối
                </button>
              </div>
            </div>
          )}
        </div>
      </div>

      {viewFileUrl && (
        <DocViewModal url={viewFileUrl} onClose={() => setViewFileUrl(null)} />
      )}
    </main>
  );
}

function timeAgo(iso?: string): string {
  const timestamp = iso ? Date.parse(iso) : Number.NaN;
  if (Number.isNaN(timestamp)) return 'Không rõ thời gian';

  const diff = Math.max(0, Date.now() - timestamp);
  const h = Math.floor(diff / 3_600_000);
  if (h < 1) return 'Vừa xong';
  if (h < 24) return `${h} giờ trước`;
  return `${Math.floor(h / 24)} ngày trước`;
}
