import { useState, useEffect } from 'react';
import { Search, ZoomIn, CheckCircle, XCircle, AlertCircle, User } from 'lucide-react';
import {
  getPendingIdentityReviews,
  getIdentityFileUrl,
  reviewIdentity,
  approveExpert,
  type IdentityAttemptResponse
} from '../services/expertApi';

type EvidenceUrls = { selfie: string; front: string; back: string };

export default function AdminExpertIdentityReviewPage() {
  const [items, setItems] = useState<IdentityAttemptResponse[]>([]);
  const [selected, setSelected] = useState<IdentityAttemptResponse | null>(null);
  const [urls, setUrls] = useState<EvidenceUrls | null>(null);
  const [reason, setReason] = useState('');
  const [busy, setBusy] = useState(false);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [finalProfileId, setFinalProfileId] = useState<string | null>(null);
  const [searchTerm, setSearchTerm] = useState('');

  const load = async () => {
    setLoading(true); setError(null);
    try { setItems(await getPendingIdentityReviews()); }
    catch { setError('Không thể tải danh sách xét duyệt.'); }
    finally { setLoading(false); }
  };

  useEffect(() => { void load(); }, []);

  const open = async (item: IdentityAttemptResponse) => {
    setSelected(item); setUrls(null); setReason(''); setError(null);
    if (!item.selfieFileId || !item.identityFrontFileId || !item.identityBackFileId) {
      setError('Hồ sơ thiếu tệp bằng chứng.');
      return;
    }
    try {
      const [selfie, front, back] = await Promise.all([
        getIdentityFileUrl(item.selfieFileId),
        getIdentityFileUrl(item.identityFrontFileId),
        getIdentityFileUrl(item.identityBackFileId)
      ]);
      setUrls({ selfie, front, back });
    } catch { setError('Không thể tải ảnh. Vui lòng thử lại sau.'); }
  };

  const decide = async (status: 'APPROVED' | 'REJECTED') => {
    if (!selected?.identityVerificationId) return;
    if (status === 'REJECTED' && !reason.trim()) { setError('Vui lòng nhập lý do từ chối.'); return; }
    setBusy(true); setError(null);
    try {
      await reviewIdentity(selected.identityVerificationId, status, reason.trim());
      setItems((current) => current.filter((item) => item.identityVerificationId !== selected.identityVerificationId));
      if (status === 'APPROVED' && selected.expertProfileId) setFinalProfileId(selected.expertProfileId);
      setSelected(null); setUrls(null); setReason('');
    } catch (caught: any) {
      setError(caught.response?.data?.message ?? 'Lỗi khi lưu quyết định.');
    } finally { setBusy(false); }
  };

  const finalize = async () => {
    if (!finalProfileId) return;
    setBusy(true); setError(null);
    try { await approveExpert(finalProfileId); setFinalProfileId(null); }
    catch (caught: any) {
      setError(caught.response?.data?.message ?? 'Lỗi phê duyệt cuối.');
    } finally { setBusy(false); }
  };

  const filteredItems = items.filter(item =>
    item.expertProfileId?.toLowerCase().includes(searchTerm.toLowerCase())
  );

  return (
    <div className="mx-auto max-w-[1600px] p-6">
      <header className="mb-8 flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold text-gray-900">Trung tâm Đối soát Định danh Chuyên gia</h1>
          <p className="text-gray-500">Đối chiếu thông tin khai báo với ảnh CCCD và kết quả AI CompreFace.</p>
        </div>
        <div className="relative w-64">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" size={18} />
          <input
            type="text"
            placeholder="Tìm ID hồ sơ..."
            value={searchTerm}
            onChange={e => setSearchTerm(e.target.value)}
            className="w-full rounded-full border border-gray-300 pl-10 pr-4 py-2 text-sm focus:border-primary outline-none"
          />
        </div>
      </header>

      {error && <div className="mb-6 rounded-xl bg-red-50 p-4 text-sm text-red-700 border border-red-200">{error}</div>}

      {finalProfileId && (
        <div className="mb-8 flex items-center justify-between rounded-2xl border border-green-200 bg-green-50 p-6 shadow-sm">
          <div className="flex items-center gap-3 text-green-800">
            <CheckCircle className="text-green-600" />
            <div>
              <p className="font-bold">Định danh đã được duyệt!</p>
              <p className="text-sm opacity-80">Hãy hoàn tất phê duyệt chứng chỉ chuyên môn để kích hoạt tài khoản chuyên gia.</p>
            </div>
          </div>
          <button
            disabled={busy}
            onClick={() => void finalize()}
            className="rounded-full bg-green-700 px-6 py-2.5 font-semibold text-white hover:bg-green-800 transition-colors"
          >
            Phê duyệt chuyên gia cuối cùng
          </button>
        </div>
      )}

      <div className="grid gap-8 lg:grid-cols-[400px_1fr]">
        {/* Left Column: List of pending reviews */}
        <section className="flex flex-col gap-4">
          <h2 className="text-lg font-bold flex items-center gap-2">
            Hàng đợi xét duyệt <span className="bg-primary text-white text-xs px-2 py-1 rounded-full">{items.length}</span>
          </h2>
          <div className="grid gap-3 max-h-[calc(100vh-200px)] overflow-y-auto pr-2">
            {loading ? <p className="text-center py-10 text-gray-400">Đang tải hồ sơ...</p> :
             filteredItems.length === 0 ? <p className="text-center py-10 text-gray-400">Không có hồ sơ chờ duyệt.</p> :
             filteredItems.map((item) => (
               <button
                key={item.identityVerificationId}
                onClick={() => void open(item)}
                className={`p-4 text-left rounded-2xl border transition-all ${selected?.identityVerificationId === item.identityVerificationId ? 'border-primary bg-primary/10 shadow-md ring-1 ring-primary' : 'border-gray-200 bg-white hover:border-gray-300 hover:bg-gray-50'}`}
               >
                 <div className="flex justify-between items-start mb-2">
                   <p className="font-bold text-gray-800">Hồ sơ #{item.expertProfileId?.slice(0, 8)}</p>
                   <span className={`text-[10px] px-2 py-0.5 rounded-full font-bold uppercase ${item.faceStatus === 'MATCHED' ? 'bg-green-100 text-green-700' : 'bg-red-100 text-red-700'}`}>
                     {item.faceStatus || 'Unknown'}
                   </span>
                 </div>
                 <div className="flex items-center gap-2 text-xs text-gray-500">
                   <span>📅 {item.createdAt ? new Date(item.createdAt).toLocaleDateString('vi-VN') : 'N/A'}</span>
                   <span>•</span>
                   <span>Similarity: {(item.faceSimilarity ? (item.faceSimilarity * 100).toFixed(1) : '0')}%</span>
                 </div>
               </button>
             ))}
          </div>
        </section>

        {/* Right Column: Detailed Review Workspace */}
        <section className="rounded-3xl border bg-white shadow-sm overflow-hidden">
          {!selected ? (
            <div className="h-full flex flex-col items-center justify-center py-32 text-gray-400">
              <User size={64} className="mb-4 opacity-20" />
              <p>Chọn một hồ sơ từ danh sách để bắt đầu đối soát chi tiết.</p>
            </div>
          ) : (
            <div className="flex flex-col h-full">
              {/* Workspace Header */}
              <div className="border-b p-6 bg-gray-50 flex items-center justify-between">
                <div className="flex items-center gap-4">
                  <div className="h-12 w-12 rounded-full bg-primary/20 flex items-center justify-center text-primary font-bold">
                    {selected.expertProfileId?.slice(0, 1).toUpperCase()}
                  </div>
                  <div>
                    <h3 className="font-bold text-lg">Chi tiết đối soát: #{selected.expertProfileId?.slice(0, 8)}</h3>
                    <p className="text-xs text-gray-500">Vui lòng đối chiếu thông tin khai báo với hình ảnh CCCD</p>
                  </div>
                </div>
                <div className="flex items-center gap-3">
                   <div className={`flex items-center gap-1 px-3 py-1 rounded-full text-xs font-bold ${selected.faceStatus === 'MATCHED' ? 'bg-green-100 text-green-700' : 'bg-red-100 text-red-700'}`}>
                     {selected.faceStatus === 'MATCHED' ? <CheckCircle size={14} /> : <XCircle size={14} />}
                     AI Score: {(selected.faceSimilarity ? (selected.faceSimilarity * 100).toFixed(1) : '0')}%
                   </div>
                </div>
              </div>

              <div className="p-6 grid gap-8 lg:grid-cols-2">
                {/* Information Panel */}
                <div className="space-y-6">
                  <h4 className="text-sm font-bold uppercase text-gray-400 flex items-center gap-2">
                    <User size={16} /> Thông tin khai báo
                  </h4>
                  <div className="grid grid-cols-2 gap-y-4 gap-x-6">
                    <InfoField label="ID Hồ sơ" value={selected.expertProfileId} />
                    <InfoField label="Trạng thái AI" value={selected.faceStatus} isStatus />
                    <InfoField label="Điểm tương đồng" value={`${(selected.faceSimilarity ? (selected.faceSimilarity * 100).toFixed(2) : '0')}%`} />
                    <InfoField label="Ngưỡng chấp nhận" value={`${(selected.faceThreshold ? (selected.faceThreshold * 100).toFixed(2) : '0')}%`} />
                    <InfoField label="Ngày gửi" value={selected.createdAt ? new Date(selected.createdAt).toLocaleString('vi-VN') : 'N/A'} />
                  </div>

                  <div className="mt-8 p-4 rounded-2xl bg-amber-50 border border-amber-100 text-amber-800 text-xs leading-relaxed">
                    <div className="flex items-center gap-2 font-bold mb-2">
                      <AlertCircle size={14} /> Lưu ý đối soát
                    </div>
                    <p>1. So sánh ảnh chân dung với ảnh trên thẻ CCCD.</p>
                    <p>2. Kiểm tra số CCCD, Họ tên trên thẻ có khớp với tài khoản không.</p>
                    <p>3. Kết quả AI là tín hiệu hỗ trợ, quyết định cuối cùng thuộc về Admin.</p>
                  </div>

                  <div className="mt-8 space-y-4">
                    <label className="grid gap-2 text-sm font-semibold">
                      Ghi chú/Lý do từ chối
                      <textarea
                        rows={3}
                        value={reason}
                        onChange={e => setReason(e.target.value)}
                        className="rounded-xl border border-gray-300 p-3 text-sm font-normal focus:border-primary outline-none"
                        placeholder="Nhập lý do nếu từ chối hồ sơ này..."
                      />
                    </label>
                    <div className="flex gap-3">
                      <button
                        disabled={busy}
                        onClick={() => void decide('REJECTED')}
                        className="flex-1 rounded-full bg-red-600 px-5 py-3 font-semibold text-white hover:bg-red-700 transition-colors disabled:opacity-50"
                      >
                        Từ chối
                      </button>
                      <button
                        disabled={busy}
                        onClick={() => void decide('APPROVED')}
                        className="flex-1 rounded-full bg-green-700 px-5 py-3 font-semibold text-white hover:bg-green-800 transition-colors disabled:opacity-50"
                      >
                        Duyệt định danh
                      </button>
                    </div>
                  </div>
                </div>

                {/* Visual Evidence Panel */}
                <div className="space-y-6">
                  <h4 className="text-sm font-bold uppercase text-gray-400 flex items-center gap-2">
                    <ZoomIn size={16} /> Bằng chứng hình ảnh
                  </h4>
                  <div className="grid gap-6">
                    {urls ? (
                      <>
                        <EvidenceImage title="Ảnh chân dung (Selfie)" url={urls.selfie} />
                        <EvidenceImage title="CCCD Mặt trước" url={urls.front} />
                        <EvidenceImage title="CCCD Mặt sau" url={urls.back} />
                      </>
                    ) : (
                      <div className="h-96 flex items-center justify-center rounded-3xl bg-gray-50 border border-dashed border-gray-300 text-gray-400 text-sm">
                        Đang chuẩn bị hình ảnh...
                      </div>
                    )}
                  </div>
                </div>
              </div>
            </div>
          )}
        </section>
      </div>
    </div>
  );
}

function InfoField({ label, value, isStatus = false }: { label: string; value: string | number | undefined; isStatus?: boolean }) {
  return (
    <div className="flex flex-col gap-1">
      <p className="text-xs text-gray-500">{label}</p>
      <p className={`text-sm font-medium ${isStatus ? 'text-primary font-bold' : 'text-gray-800'}`}>{value || 'N/A'}</p>
    </div>
  );
}

function EvidenceImage({ title, url }: { title: string; url: string }) {
  return (
    <figure className="group relative overflow-hidden rounded-2xl border border-gray-200 bg-gray-50 transition-all hover:border-primary hover:shadow-md">
      <figcaption className="absolute top-3 left-3 z-10 rounded-lg bg-white/90 px-2 py-1 text-[11px] font-bold uppercase text-gray-600 shadow-sm">
        {title}
      </figcaption>
      <a href={url} target="_blank" rel="noreferrer" className="block overflow-hidden">
        <img src={url} alt={title} className="h-56 w-full object-contain transition-transform duration-300 group-hover:scale-105" />
        <div className="absolute inset-0 flex items-center justify-center bg-black/0 transition-colors group-hover:bg-black/10">
          <div className="scale-0 rounded-full bg-white p-2 text-gray-800 shadow-xl transition-transform group-hover:scale-100">
            <ZoomIn size={20} />
          </div>
        </div>
      </a>
    </figure>
  );
}
