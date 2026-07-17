import { useEffect, useState } from 'react';
import { approveExpert, getIdentityFileUrl, getPendingIdentityReviews, reviewIdentity, type IdentityAttemptResponse } from '../services/expertApi';

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

  const load = async () => {
    setLoading(true); setError(null);
    try { setItems(await getPendingIdentityReviews()); }
    catch { setError('Không thể tải hàng đợi định danh.'); }
    finally { setLoading(false); }
  };
  useEffect(() => { void load(); }, []);

  const open = async (item: IdentityAttemptResponse) => {
    setSelected(item); setUrls(null); setReason(''); setError(null);
    if (!item.selfieFileId || !item.identityFrontFileId || !item.identityBackFileId) { setError('Bộ bằng chứng thiếu tham chiếu tệp.'); return; }
    try {
      const [selfie, front, back] = await Promise.all([getIdentityFileUrl(item.selfieFileId), getIdentityFileUrl(item.identityFrontFileId), getIdentityFileUrl(item.identityBackFileId)]);
      setUrls({ selfie, front, back });
    } catch { setError('Không thể tạo URL xem tệp. Hãy tải lại để nhận URL mới.'); }
  };

  const decide = async (status: 'APPROVED' | 'REJECTED') => {
    if (!selected?.identityVerificationId) return;
    if (status === 'REJECTED' && !reason.trim()) { setError('Lý do từ chối là bắt buộc.'); return; }
    setBusy(true); setError(null);
    try {
      await reviewIdentity(selected.identityVerificationId, status, reason.trim());
      setItems((current) => current.filter((item) => item.identityVerificationId !== selected.identityVerificationId));
      if (status === 'APPROVED' && selected.expertProfileId) setFinalProfileId(selected.expertProfileId);
      setSelected(null); setUrls(null); setReason('');
    } catch (caught: unknown) {
      const apiError = caught as { response?: { data?: { message?: string } } };
      setError(apiError.response?.data?.message ?? 'Không thể lưu quyết định.');
    } finally { setBusy(false); }
  };

  const finalize = async () => {
    if (!finalProfileId) return;
    setBusy(true); setError(null);
    try { await approveExpert(finalProfileId); setFinalProfileId(null); }
    catch (caught: unknown) {
      const apiError = caught as { response?: { data?: { message?: string } } };
      setError(apiError.response?.data?.message ?? 'Chưa thể phê duyệt cuối. Hãy duyệt chứng chỉ chuyên môn trước.');
    } finally { setBusy(false); }
  };

  return (
    <div className="mx-auto max-w-7xl p-6">
      <div className="mb-6 flex flex-wrap items-center justify-between gap-3"><div><h1 className="text-2xl font-bold">Xét duyệt định danh chuyên gia</h1><p className="mt-1 text-sm text-gray-500">Đối chiếu thủ công ảnh chân dung và hai mặt CCCD. Điểm khuôn mặt chỉ là tín hiệu hỗ trợ.</p></div><a href="/admin/expert-verification-queue" className="rounded-full border border-primary px-4 py-2 text-sm font-semibold text-primary">Sang hàng đợi chứng chỉ</a></div>
      {error && <div className="mb-4 rounded-xl bg-red-50 p-4 text-sm text-red-700">{error}</div>}
      {finalProfileId && <div className="mb-5 flex flex-wrap items-center justify-between gap-3 rounded-xl border border-green-200 bg-green-50 p-4 text-sm text-green-900"><span>Định danh đã duyệt. Chỉ hoàn tất hồ sơ nếu chứng chỉ chuyên môn cũng đã được duyệt.</span><button disabled={busy} onClick={() => void finalize()} className="rounded-full bg-green-700 px-4 py-2 font-semibold text-white">Phê duyệt chuyên gia cuối cùng</button></div>}
      <div className="grid gap-6 lg:grid-cols-[340px_1fr]">
        <section className="rounded-2xl border bg-white p-3">
          {loading && <p className="p-5 text-sm text-gray-500">Đang tải...</p>}
          {!loading && items.length === 0 && <p className="p-5 text-sm text-gray-500">Không có bộ định danh chờ duyệt.</p>}
          {items.map((item) => <button key={item.identityVerificationId} onClick={() => void open(item)} className={`mb-2 w-full rounded-xl border p-4 text-left ${selected?.identityVerificationId === item.identityVerificationId ? 'border-primary bg-primary/5' : 'border-gray-200'}`}><p className="font-semibold">Hồ sơ {item.expertProfileId?.slice(0, 8)}</p><p className="mt-1 text-xs text-gray-500">{item.createdAt ? new Date(item.createdAt).toLocaleString('vi-VN') : ''}</p><p className="mt-2 text-xs">Face: {item.faceStatus ?? 'N/A'} {item.faceSimilarity != null ? `· ${(item.faceSimilarity * 100).toFixed(1)}%` : ''}</p></button>)}
        </section>
        <section className="rounded-2xl border bg-white p-5">
          {!selected && <div className="py-20 text-center text-gray-500">Chọn một hồ sơ để xem bộ ảnh riêng tư.</div>}
          {selected && <><div className="mb-4 grid gap-3 sm:grid-cols-3">{urls ? <><Evidence title="Ảnh chân dung" url={urls.selfie} /><Evidence title="CCCD mặt trước" url={urls.front} /><Evidence title="CCCD mặt sau" url={urls.back} /></> : <p className="col-span-3 py-16 text-center text-sm text-gray-500">Đang tạo URL xem có thời hạn...</p>}</div><div className="rounded-xl bg-gray-50 p-4 text-sm"><p>Trạng thái nhà cung cấp: <strong>{selected.faceStatus ?? 'N/A'}</strong></p><p className="mt-1">Similarity / threshold: <strong>{selected.faceSimilarity ?? 'N/A'} / {selected.faceThreshold ?? 'N/A'}</strong></p>{selected.providerErrorCode && <p className="mt-1 text-amber-700">Mã lỗi: {selected.providerErrorCode}</p>}</div><label className="mt-4 grid gap-2 text-sm font-semibold">Lý do/ghi chú<textarea rows={3} value={reason} onChange={(event) => setReason(event.target.value)} className="rounded-xl border p-3 font-normal" placeholder="Bắt buộc khi từ chối" /></label><div className="mt-4 flex gap-3"><button disabled={busy} onClick={() => void decide('APPROVED')} className="rounded-full bg-green-700 px-5 py-2.5 font-semibold text-white disabled:opacity-50">Duyệt định danh</button><button disabled={busy} onClick={() => void decide('REJECTED')} className="rounded-full bg-red-700 px-5 py-2.5 font-semibold text-white disabled:opacity-50">Từ chối</button></div></>}
        </section>
      </div>
    </div>
  );
}

function Evidence({ title, url }: { title: string; url: string }) { return <figure><figcaption className="mb-2 text-sm font-semibold">{title}</figcaption><a href={url} target="_blank" rel="noreferrer"><img src={url} alt={title} className="h-64 w-full rounded-xl bg-gray-100 object-contain" /></a></figure>; }
