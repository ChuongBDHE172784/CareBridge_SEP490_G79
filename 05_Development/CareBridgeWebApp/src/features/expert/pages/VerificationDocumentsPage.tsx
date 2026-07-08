import { useState, useEffect } from 'react';
import { getMyCredentials, submitCredential, deleteCredential } from '../services/expertApi';

export default function VerificationDocumentsPage() {
  const [credentials, setCredentials] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [showUpload, setShowUpload] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const [form, setForm] = useState({
    credentialType: '',
    credentialNumber: '',
    issuer: '',
    issuedDate: '',
    expiryDate: '',
    fileUrl: '',
  });

  const load = async () => {
    try {
      const data = await getMyCredentials();
      setCredentials(data);
    } catch (e: any) {
      setError(e.response?.data?.message ?? 'Không thể tải chứng chỉ');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); }, []);

  const onSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setSubmitting(true);
    setError(null);
    try {
      await submitCredential({ ...form, issuedDate: form.issuedDate });
      setShowUpload(false);
      setForm({ credentialType: '', credentialNumber: '', issuer: '', issuedDate: '', expiryDate: '', fileUrl: '' });
      await load();
    } catch (e: any) {
      setError(e.response?.data?.message ?? 'Tải lên thất bại');
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = async (id: string) => {
    if (!confirm('Xóa chứng chỉ này?')) return;
    try {
      await deleteCredential(id);
      await load();
    } catch (e: any) {
      alert(e.response?.data?.message ?? 'Xóa thất bại');
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-[400px]">
        <div className="animate-spin rounded-full h-8 w-8 border-2 border-primary border-t-transparent" />
      </div>
    );
  }

  const statusStyles: Record<string, string> = {
    PENDING: 'bg-yellow-50 text-yellow-700 border-yellow-200',
    UNDER_REVIEW: 'bg-blue-50 text-blue-700 border-blue-200',
    APPROVED: 'bg-green-50 text-green-700 border-green-200',
    REJECTED: 'bg-red-50 text-red-700 border-red-200',
    EXPIRED: 'bg-gray-50 text-gray-600 border-gray-200',
  };

  return (
    <div className="max-w-3xl mx-auto p-6">
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold text-on-surface">Chứng chỉ & Giấy tờ</h1>
        <button
          onClick={() => setShowUpload(!showUpload)}
          className="px-4 py-2 rounded bg-primary text-white text-sm font-medium hover:bg-primary/90"
        >
          Tải lên chứng chỉ
        </button>
      </div>

      {error && (
        <div className="mb-4 p-3 rounded bg-red-50 border border-red-200 text-red-700 text-sm">{error}</div>
      )}

      {showUpload && (
        <form onSubmit={onSubmit} className="mb-6 p-5 bg-white rounded-lg border border-gray-200 shadow-sm space-y-4">
          <h3 className="font-medium text-gray-800">Tải lên chứng chỉ mới</h3>

          <SelectField label="Loại chứng chỉ" required value={form.credentialType} onChange={(v) => setForm({ ...form, credentialType: v })}>
            <option value="">-- Chọn loại --</option>
            <option value="LICENSE">Bằng cấp chuyên môn</option>
            <option value="CERTIFICATE">Chứng chỉ đào tạo</option>
            <option value="RELEVANT_EXPERIENCE">Kinh nghiệm liên quan</option>
            <option value="IDENTIFICATION">Giấy tờ tùy thân</option>
          </SelectField>

          <div className="grid grid-cols-2 gap-4">
            <Field label="Số chứng chỉ">
              <input className="mt-1 block w-full rounded border border-gray-300 px-3 py-2"
                value={form.credentialNumber} onChange={(e) => setForm({ ...form, credentialNumber: e.target.value })} />
            </Field>
            <Field label="Nơi cấp">
              <input className="mt-1 block w-full rounded border border-gray-300 px-3 py-2"
                value={form.issuer} onChange={(e) => setForm({ ...form, issuer: e.target.value })} />
            </Field>
          </div>

          <div className="grid grid-cols-2 gap-4">
            <Field label="Ngày cấp" required>
              <input type="date" className="mt-1 block w-full rounded border border-gray-300 px-3 py-2"
                value={form.issuedDate} onChange={(e) => setForm({ ...form, issuedDate: e.target.value })} />
            </Field>
            <Field label="Ngày hết hạn (tùy chọn)">
              <input type="date" className="mt-1 block w-full rounded border border-gray-300 px-3 py-2"
                value={form.expiryDate} onChange={(e) => setForm({ ...form, expiryDate: e.target.value })} />
            </Field>
          </div>

          <Field label="URL tài liệu (file đính kèm)">
            <input className="mt-1 block w-full rounded border border-gray-300 px-3 py-2"
              value={form.fileUrl} onChange={(e) => setForm({ ...form, fileUrl: e.target.value })} placeholder="https://..." />
          </Field>

          <div className="flex justify-end gap-3 pt-2">
            <button type="button" onClick={() => setShowUpload(false)}
              className="px-4 py-2 rounded border border-gray-300 text-gray-700 hover:bg-gray-50">
              Hủy
            </button>
            <button type="submit" disabled={submitting || !form.credentialType || !form.issuedDate}
              className="px-4 py-2 rounded bg-primary text-white font-medium disabled:opacity-50">
              {submitting ? 'Đang tải...' : 'Gửi xét duyệt'}
            </button>
          </div>
        </form>
      )}

      <div className="space-y-3">
        {credentials.length === 0 && (
          <div className="p-8 text-center text-gray-500 bg-white rounded-lg border border-gray-200">
            Chưa có chứng chỉ nào. Nhấn "Tải lên chứng chỉ" để bắt đầu.
          </div>
        )}

        {credentials.map((cred) => (
          <div key={cred.credentialId} className="bg-white rounded-lg border border-gray-200 shadow-sm p-5">
            <div className="flex items-start justify-between">
              <div className="flex-1">
                <div className="flex items-center gap-3 mb-2">
                  <span className="font-semibold text-gray-900">{cred.credentialType}</span>
                  <span className={`px-2 py-0.5 rounded text-xs font-medium border ${statusStyles[cred.reviewStatus] || 'bg-gray-50'}`}>
                    {cred.reviewStatus}
                  </span>
                </div>
                {cred.credentialNumber && <p className="text-sm text-gray-600">Số: {cred.credentialNumber}</p>}
                {cred.issuer && <p className="text-sm text-gray-600">Nơi cấp: {cred.issuer}</p>}
                <p className="text-sm text-gray-500 mt-1">
                  Ngày cấp: {cred.issuedDate}
                  {cred.expiryDate && <> · Hết hạn: {cred.expiryDate}</>}
                </p>
                {cred.reviewNote && (
                  <p className="text-sm text-gray-600 mt-2 italic">Ghi chú: {cred.reviewNote}</p>
                )}
                <p className="text-xs text-gray-400 mt-2">
                  Gửi lúc: {new Date(cred.createdAt).toLocaleString('vi-VN')}
                </p>
              </div>
              <button
                onClick={() => handleDelete(cred.credentialId)}
                className="text-sm text-red-600 hover:text-red-800 ml-4"
              >
                Xóa
              </button>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}

function Field({ label, children, required = false }: { label: string; children: React.ReactNode; required?: boolean }) {
  return (
    <div>
      <label className="block text-sm font-medium text-gray-700">{label} {required && '*'}</label>
      {children}
    </div>
  );
}

function SelectField({
  label,
  children,
  value,
  onChange,
  required = false,
}: {
  label: string;
  children: React.ReactNode;
  value: string;
  onChange: (value: string) => void;
  required?: boolean;
}) {
  return (
    <div>
      <label className="block text-sm font-medium text-gray-700">{label} {required && '*'}</label>
      <select className="mt-1 block w-full rounded border border-gray-300 px-3 py-2"
        value={value} onChange={(e) => onChange(e.target.value)}>
        {children}
      </select>
    </div>
  );
}
