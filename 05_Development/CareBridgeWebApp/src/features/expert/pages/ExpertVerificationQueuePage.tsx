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

export default function ExpertVerificationQueuePage() {
  const [items, setItems] = useState<CredentialItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [typeFilter, setTypeFilter] = useState('');
  const [actionId, setActionId] = useState<string | null>(null);
  const [noteText, setNoteText] = useState('');

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
      {/* Queue list */}
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
            <div className="p-4 rounded-2xl bg-error-container text-on-error-container text-sm">
              {error}
              <button onClick={fetchQueue} className="ml-3 underline font-semibold text-on-error">Thử lại</button>
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
                      <span className="rounded-full px-2 py-0.5 bg-amber-100 text-xs text-amber-700 font-bold">
                        Chờ xác minh
                      </span>
                    </div>
                    <p className="text-xs text-on-surface-variant mt-2">
                      {timeAgo(item.createdAt)}
                    </p>
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

      {/* Detail pane */}
      {selected && (
        <div className="flex w-full flex-col rounded-3xl border border-outline-variant/40 bg-surface-container-lowest shadow-sm lg:w-1/2">
          <div className="p-5 border-b border-outline-variant/50 bg-surface flex items-center justify-between">
            <h3 className="text-base font-semibold text-on-surface leading-snug">
              Chi tiết hồ sơ xác minh
            </h3>
            <button
              onClick={() => { setSelectedId(null); setNoteText(''); }}
              className="text-on-surface-variant hover:text-on-surface text-xl leading-none"
            >
              ×
            </button>
          </div>

          <div className="flex-1 p-5 space-y-4 overflow-y-auto">
            <div className="rounded-2xl border border-outline-variant/60 bg-surface p-4 space-y-2">
              <p><strong>Loại:</strong> {credentialTypeLabel(selected.credentialType)}</p>
              {selected.credentialNumber && (
                <p><strong>Số hiệu:</strong> {selected.credentialNumber}</p>
              )}
              {selected.issuer && (
                <p><strong>Cơ quan cấp:</strong> {selected.issuer}</p>
              )}
              {selected.issuedDate && (
                <p><strong>Ngày cấp:</strong> {selected.issuedDate}</p>
              )}
              {selected.expiryDate && (
                <p><strong>Ngày hết hạn:</strong> {selected.expiryDate}</p>
              )}
              {selected.fileUrl && (
                <a
                  href={selected.fileUrl}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="inline-block mt-2 text-primary text-sm font-semibold underline"
                >
                  Xem tài liệu đính kèm
                </a>
              )}
            </div>

            <div>
              <label className="block text-sm font-semibold text-on-surface mb-2">
                Ghi chú (tuỳ chọn)
              </label>
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
              className="flex-1 h-12 rounded-full bg-error text-white font-semibold text-base hover:brightness-110 transition disabled:opacity-50"
            >
              Từ chối
            </button>
          </div>
        </div>
      )}
        </div>
      </div>
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
