import { useEffect, useState } from 'react';
import { deleteCredential, getMyCredentials, submitCredential, type CredentialResponse } from '../services/expertApi';

const EMPTY_FORM = { credentialType: 'MEDICAL_LICENSE', credentialNumber: '', issuer: '', issuedDate: '', expiryDate: '' };

export default function VerificationDocumentsPage() {
  const [credentials, setCredentials] = useState<CredentialResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [showUpload, setShowUpload] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [deletingId, setDeletingId] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [form, setForm] = useState(EMPTY_FORM);
  const [file, setFile] = useState<File | null>(null);

  const load = async () => {
    try { setCredentials(await getMyCredentials()); }
    catch (caught: unknown) {
      const apiError = caught as { response?: { data?: { message?: string } } };
      setError(apiError.response?.data?.message ?? 'Không thể tải chứng chỉ.');
    } finally { setLoading(false); }
  };

  useEffect(() => { void load(); }, []);

  const submit = async (event: React.FormEvent) => {
    event.preventDefault();
    if (!file) { setError('Vui lòng chọn tệp chứng chỉ.'); return; }
    setSubmitting(true); setError(null);
    try {
      await submitCredential({ body: form, file });
      setForm(EMPTY_FORM); setFile(null); setShowUpload(false);
      await load();
    } catch (caught: unknown) {
      const apiError = caught as { response?: { data?: { message?: string } } };
      setError(apiError.response?.data?.message ?? 'Không thể tải chứng chỉ.');
    } finally { setSubmitting(false); }
  };

  const remove = async (credentialId: string) => {
    setDeletingId(credentialId); setError(null);
    try { await deleteCredential(credentialId); setCredentials((items) => items.filter((item) => item.credentialId !== credentialId)); }
    catch (caught: unknown) {
      const apiError = caught as { response?: { data?: { message?: string } } };
      setError(apiError.response?.data?.message ?? 'Không thể xóa chứng chỉ.');
    } finally { setDeletingId(null); }
  };

  if (loading) return <div className="flex min-h-[400px] items-center justify-center"><div className="h-8 w-8 animate-spin rounded-full border-2 border-primary border-t-transparent" /></div>;

  return (
    <div className="mx-auto max-w-4xl p-6">
      <div className="mb-6 flex flex-wrap items-center justify-between gap-3"><div><h1 className="text-2xl font-bold text-on-surface">Chứng chỉ chuyên môn</h1><p className="mt-1 text-sm text-gray-500">Các tệp được lưu riêng tư và URL xem có thời hạn ngắn.</p></div><button onClick={() => setShowUpload(true)} className="rounded-full bg-primary px-5 py-2.5 text-sm font-semibold text-white">Thêm chứng chỉ</button></div>
      {error && <div className="mb-4 rounded-xl border border-red-200 bg-red-50 p-3 text-sm text-red-700">{error}</div>}
      {showUpload && (
        <form onSubmit={submit} className="mb-6 grid gap-4 rounded-2xl border bg-white p-5 sm:grid-cols-2">
          <label className="grid gap-1 text-sm font-medium">Loại chứng chỉ *<select className="rounded-lg border p-2.5" value={form.credentialType} onChange={(event) => setForm({ ...form, credentialType: event.target.value })}><option value="MEDICAL_LICENSE">Giấy phép hành nghề y</option><option value="DEGREE">Bằng cấp</option><option value="CERTIFICATE">Chứng chỉ đào tạo</option><option value="PROFESSIONAL_LICENSE">Giấy phép chuyên môn</option></select></label>
          <Field label="Số chứng chỉ *" value={form.credentialNumber} onChange={(value) => setForm({ ...form, credentialNumber: value })} />
          <Field label="Đơn vị cấp *" value={form.issuer} onChange={(value) => setForm({ ...form, issuer: value })} />
          <Field label="Ngày cấp *" type="date" value={form.issuedDate} onChange={(value) => setForm({ ...form, issuedDate: value })} />
          <Field label="Ngày hết hạn" type="date" value={form.expiryDate} onChange={(value) => setForm({ ...form, expiryDate: value })} />
          <label className="grid gap-1 text-sm font-medium">Tệp ảnh/PDF *<input required type="file" accept="image/jpeg,image/png,application/pdf" className="rounded-lg border border-dashed p-2" onChange={(event) => setFile(event.target.files?.[0] ?? null)} /></label>
          <div className="flex gap-3 sm:col-span-2"><button disabled={submitting} className="rounded-full bg-primary px-5 py-2.5 font-semibold text-white disabled:opacity-50">{submitting ? 'Đang gửi...' : 'Gửi xét duyệt'}</button><button type="button" className="rounded-full border px-5 py-2.5" onClick={() => setShowUpload(false)}>Hủy</button></div>
        </form>
      )}
      <div className="grid gap-4">
        {credentials.length === 0 && <div className="rounded-2xl border border-dashed p-10 text-center text-gray-500">Chưa có chứng chỉ nào.</div>}
        {credentials.map((credential) => (
          <article key={credential.credentialId} className="flex flex-wrap items-center justify-between gap-4 rounded-2xl border bg-white p-5 shadow-sm">
            <div><p className="font-semibold text-on-surface">{typeLabel(credential.credentialType)}</p><p className="mt-1 text-sm text-gray-500">{credential.credentialNumber || 'Không có số'} · {credential.issuer || 'Không có đơn vị cấp'}</p>{credential.reviewNote && <p className="mt-2 text-sm text-red-700">Phản hồi: {credential.reviewNote}</p>}</div>
            <div className="flex items-center gap-3"><span className={`rounded-full px-3 py-1 text-xs font-semibold ${statusClass(credential.reviewStatus)}`}>{credential.reviewStatus}</span>{credential.fileUrl && <a className="text-sm font-semibold text-primary" href={credential.fileUrl} target="_blank" rel="noreferrer">Xem tệp</a>}<button disabled={deletingId === credential.credentialId} onClick={() => void remove(credential.credentialId)} className="text-sm font-semibold text-red-600 disabled:opacity-50">{deletingId === credential.credentialId ? 'Đang xóa...' : 'Xóa'}</button></div>
          </article>
        ))}
      </div>
    </div>
  );
}

function Field({ label, type = 'text', value, onChange }: { label: string; type?: string; value: string; onChange: (value: string) => void }) { return <label className="grid gap-1 text-sm font-medium">{label}<input required={label.includes('*')} type={type} value={value} onChange={(event) => onChange(event.target.value)} className="rounded-lg border p-2.5 font-normal" /></label>; }
function typeLabel(value: string) { return ({ MEDICAL_LICENSE: 'Giấy phép hành nghề y', DEGREE: 'Bằng cấp chuyên môn', CERTIFICATE: 'Chứng chỉ đào tạo', PROFESSIONAL_LICENSE: 'Giấy phép chuyên môn' } as Record<string, string>)[value] ?? value; }
function statusClass(value: string) { if (value === 'APPROVED') return 'bg-green-100 text-green-800'; if (value === 'REJECTED') return 'bg-red-100 text-red-800'; return 'bg-amber-100 text-amber-800'; }
