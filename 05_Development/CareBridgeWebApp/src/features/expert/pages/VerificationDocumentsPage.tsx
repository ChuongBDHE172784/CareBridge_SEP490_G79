import { useState, useEffect } from 'react';
import { getMyCredentials, submitCredential, deleteCredential } from '../services/expertApi';

function CredentialFileViewModal({ url, fileName, onClose }: { url: string; fileName?: string; onClose: () => void }) {
  if (!url) return null;
  const ext = url.split('.').pop()?.toLowerCase();
  const isImage = ['jpg', 'jpeg', 'png', 'gif', 'webp'].includes(ext || '');
  const isPdf = ext === 'pdf';

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4" onClick={onClose}>
      <div className="bg-white rounded-xl shadow-2xl max-w-4xl w-full max-h-[90vh] flex flex-col" onClick={(e) => e.stopPropagation()}>
        <div className="flex items-center justify-between p-4 border-b">
          <h3 className="font-semibold text-gray-800 truncate">{fileName || 'Xem tài liệu'}</h3>
          <button onClick={onClose} className="text-gray-400 hover:text-gray-600 text-2xl leading-none">&times;</button>
        </div>
        <div className="flex-1 overflow-auto p-4 flex items-center justify-center bg-gray-50">
          {isImage ? (
            <img src={url} alt={fileName} className="max-w-full max-h-[70vh] object-contain rounded" />
          ) : isPdf ? (
            <iframe src={url} className="w-full h-[70vh] rounded" title={fileName} />
          ) : (
            <div className="text-center py-12">
              <svg className="w-16 h-16 mx-auto text-gray-300 mb-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
              </svg>
              <p className="text-gray-500 mb-4">Không thể xem trựcếp. Tải xuống để xem.</p>
              <a href={url} download className="px-4 py-2 bg-primary text-white rounded-lg text-sm font-medium hover:bg-primary/90">
                Tải xuống
              </a>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

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
  });

  const [selectedFile, setSelectedFile] = useState<File | null>(null);
const [viewFileUrl, setViewFileUrl] = useState<string | null>(null);

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
      await submitCredential({ body: form, file: selectedFile! });
      setShowUpload(false);
      setForm({ credentialType: '', credentialNumber: '', issuer: '', issuedDate: '', expiryDate: '' });
      setSelectedFile(null);
      await load();
    } catch (e: any) {
      setError(e.response?.data?.message ?? 'Tải lên thất bại');
    } finally {
      setSubmitting(false);
    }
  };

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0] ?? null;
    setSelectedFile(file);
    if (file) {
      setError(null);
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

          <Field label="Tài liệu đính kèm (PDF, JPG, PNG - tối đa 20MB)">
            <input type="file" accept=".pdf,.jpg,.jpeg,.png,.gif" className="mt-1 block w-full text-sm"
              onChange={handleFileChange} />
            {selectedFile && (
              <p className="mt-1 text-xs text-gray-500">Đã chọn: {selectedFile.name} ({(selectedFile.size / 1024 / 1024).toFixed(2)} MB)</p>
            )}
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
        {credentials.length === 0 && !loading && (
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
          <div className="flex flex-col items-end gap-2 ml-4">
            {cred.fileUrl && (
              <button
                onClick={() => setViewFileUrl(cred.fileUrl)}
                className="text-sm text-primary hover:text-primary/80 font-medium"
              >
                Xem tài liệu
              </button>
            )}
            <button
              onClick={() => handleDelete(cred.credentialId)}
              className="text-sm text-red-600 hover:text-red-800"
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
