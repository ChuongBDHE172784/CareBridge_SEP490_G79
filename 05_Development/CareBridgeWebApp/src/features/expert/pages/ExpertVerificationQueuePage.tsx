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
        className="flex max-h-[90vh] w-full max-w-5xl flex-col rounded-lg border border-outline-variant/80 bg-surface shadow-[0_16px_40px_rgba(15,23,42,0.18)]"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="flex items-center justify-between border-b border-outline-variant/70 px-5 py-3">
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
                className="portal-primary-button"
              >
                <span className="material-symbols-outlined text-[18px]">download</span>
                Tải về
              </a>
            )}
            <button
              onClick={onClose}
              className="flex h-8 w-8 items-center justify-center rounded-md text-xl leading-none text-outline hover:bg-surface-container-low hover:text-on-surface"
            >
              ×
            </button>
          </div>
        </div>

        <div className="flex flex-1 items-center justify-center overflow-auto bg-surface-container-low p-4">
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
    return <p className="py-4 text-xs text-error">Không thể tải ảnh</p>;
  }
  return (
    <img
      src={url}
      alt={alt}
      className="max-h-[55vh] max-w-full cursor-pointer rounded-lg border border-outline-variant object-contain"
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
      <div className="flex items-center gap-3 rounded-md border border-outline-variant/60 bg-surface-container-low p-3">
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
          className="portal-secondary-button"
          >
            Xem trước
          </button>
          <a
            href={url}
            download
            className="portal-primary-button"
          >
            <span className="material-symbols-outlined text-[14px]">download</span>
            Tải về
          </a>
        </div>
      </div>
    );
  };

  return (
    <main className="portal-page px-5 py-5 md:px-6 md:py-6">
      <div className="portal-contained">
        <header className="portal-header">
          <div>
            <p className="portal-eyebrow">Quản trị chuyên gia</p>
            <h1 className="portal-title">Hàng đợi xác minh chuyên gia</h1>
            <p className="portal-subtitle">Kiểm tra giấy tờ và đưa ra quyết định xác minh hồ sơ chuyên gia.</p>
          </div>
        </header>

        <div className="flex flex-col gap-5 lg:flex-row lg:items-start">
          <div
            className={`flex flex-col rounded-lg border border-outline-variant/70 bg-surface ${selectedId ? 'w-full lg:w-1/2' : 'w-full'}`}
          >
            <div className="p-4 pb-3 space-y-3">
              <h2 className="text-sm font-semibold text-on-surface">Hồ sơ chờ xử lý</h2>
              <div className="flex gap-2">
                <input
                  type="text"
                  placeholder="Lọc theo loại bằng cấp…"
                  value={typeFilter}
                  onChange={(e) => setTypeFilter(e.target.value)}
                  onKeyDown={(e) => e.key === 'Enter' && fetchQueue()}
                  className="portal-field flex-1"
                />
                <button
                  onClick={fetchQueue}
                  className="portal-primary-button"
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
                <div className="rounded-lg border border-error-container bg-error-container/60 p-4 text-sm text-error">
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
                    className={`w-full rounded-md border p-3 text-left transition-colors ${
                      active
                        ? 'border-primary bg-primary-container/30'
                        : 'border-outline-variant/60 bg-surface hover:border-primary/30 hover:bg-surface-container-low'
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
                            <span className="rounded px-2 py-0.5 bg-surface-container-low text-xs text-on-surface-variant">
                              {item.issuer}
                            </span>
                          )}
                          {item.specialty && (
                            <span className="rounded px-2 py-0.5 bg-sky-50 text-xs text-sky-700 font-medium">
                              {item.specialty}
                            </span>
                          )}
                          <span className="rounded px-2 py-0.5 bg-amber-100 text-xs font-semibold text-amber-700">
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
                <div className="portal-empty m-4">
                  <span className="material-symbols-outlined text-[48px] block mb-3 opacity-40">verified</span>
                  <p className="text-base font-medium">Không có hồ sơ chờ xác minh</p>
                </div>
              )}
            </div>
          </div>

          {selected && (
            <div className="flex w-full flex-col rounded-lg border border-outline-variant/70 bg-surface-container-lowest lg:w-1/2">
              <div className="flex items-center justify-between border-b border-outline-variant/70 bg-surface p-4">
                <h3 className="text-sm font-semibold leading-snug text-on-surface">Chi tiết hồ sơ xác minh</h3>
                <button
                  onClick={() => { setSelectedId(null); setNoteText(''); }}
                  className="rounded-md px-2 text-xl leading-none text-on-surface-variant hover:bg-surface-container-low hover:text-on-surface"
                >
                  ×
                </button>
              </div>

              <div className="flex-1 p-5 space-y-4 overflow-y-auto">
                <div className="space-y-3 rounded-md border border-outline-variant/60 bg-surface p-4">
                  <h4 className="mb-2 text-sm font-semibold text-primary">Thông tin chuyên gia</h4>
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

                <div className="space-y-2 rounded-md border border-outline-variant/60 bg-surface p-4">
                  <h4 className="mb-2 text-sm font-semibold text-primary">Thông tin chứng chỉ</h4>
                  <p className="text-sm"><strong>Loại:</strong> {credentialTypeLabel(selected.credentialType)}</p>
                  {selected.credentialNumber && <p className="text-sm"><strong>Số hiệu:</strong> {selected.credentialNumber}</p>}
                  {selected.issuer && <p className="text-sm"><strong>Cơ quan cấp:</strong> {selected.issuer}</p>}
                  <p className="text-sm"><strong>Ngày cấp:</strong> {fmtDate(selected.issuedDate)}</p>
                  {selected.expiryDate && <p className="text-sm"><strong>Ngày hết hạn:</strong> {fmtDate(selected.expiryDate)}</p>}
                </div>

                {selected.fileUrl && (
                  <div className="space-y-2 rounded-md border border-outline-variant/60 bg-surface p-4">
                    <h4 className="mb-2 text-sm font-semibold text-primary">Tài liệu đính kèm</h4>
                    {renderFilePreview(selected)}
                  </div>
                )}

                {selected.reviewNote && (
                  <div className="space-y-2 rounded-md border border-outline-variant/60 bg-surface p-4">
                    <p className="text-sm"><strong>Ghi chú đánh giá trước:</strong></p>
                    <p className="text-sm text-gray-700 whitespace-pre-wrap">{selected.reviewNote}</p>
                  </div>
                )}

                <div>
                  <label className="portal-label">Ghi chú (tuỳ chọn)</label>
                  <textarea
                    rows={3}
                    value={noteText}
                    onChange={(e) => setNoteText(e.target.value)}
                    placeholder="Nhập ghi chú cho quyết định…"
                    className="w-full resize-none rounded-md border border-outline-variant bg-surface p-3 text-sm leading-relaxed focus:outline-none focus:ring-2 focus:ring-primary/10"
                  />
                </div>
              </div>

              <div className="flex gap-3 border-t border-outline-variant/70 bg-surface p-4">
                <button
                  onClick={() => reviewCredential(selected.credentialId, 'APPROVED')}
                  disabled={actionId === selected.credentialId}
                  className="flex-1 rounded-md bg-emerald-600 px-3 py-2 text-sm font-semibold text-white hover:bg-emerald-700 disabled:opacity-50"
                >
                  {actionId === selected.credentialId ? 'Đang xử lý…' : 'Duyệt'}
                </button>
                <button
                  onClick={() => reviewCredential(selected.credentialId, 'REJECTED')}
                  disabled={actionId === selected.credentialId}
                  className="flex-1 rounded-md bg-error px-3 py-2 text-sm font-semibold text-on-error hover:bg-error/90 disabled:opacity-50"
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
