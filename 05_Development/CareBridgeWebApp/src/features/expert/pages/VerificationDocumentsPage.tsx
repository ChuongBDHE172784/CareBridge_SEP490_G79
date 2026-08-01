import { useState, useEffect } from 'react';
import { getMyCredentials, submitCredential, deleteCredential } from '../services/expertApi';

/* ── Vietnam medical licensing authorities ────────────────────────────── */
const ISSUERS = [
  'Bộ Y tế',
  'Sở Y tế Hà Nội',
  'Sở Y tế TP Hồ Chí Minh',
  'Sở Y tế Đà Nẵng',
  'Sở Y tế khác',
  'Hội đồng Y khoa Việt Nam',
  'Trường Đại học Y Hà Nội',
  'Trường Đại học Y dược TP Hồ Chí Minh',
  'Trường Đại học Y dược Huế',
  'Trường Đại học Y dược Cần Thơ',
  'Học viện Y học cổ truyền Việt Nam',
  'Bộ Giáo dục và Đào tạo',
  'Trường Đại học khác',
  'Cơ quan đăng ký hành nghề y tế',
  'UBND tỉnh / thành phố',
  'Hội nghề nghiệp y tế',
  'Tổ chức y tế quốc tế',
  'Khác',
];

const TYPE_LABELS: Record<string, string> = {
  MEDICAL_LICENSE: 'Giấy phép hành nghề y',
  DEGREE: 'Bằng cấp chuyên môn',
  CERTIFICATE: 'Chứng chỉ đào tạo',
  IDENTITY_DOCUMENT: 'Giấy tờ định danh',
  PROFESSIONAL_LICENSE: 'Giấy phép hành nghề',
};

function getFileExt(url: string): string {
  const clean = url.split('?')[0];
  const dot = clean.lastIndexOf('.');
  return dot >= 0 ? clean.substring(dot + 1).toLowerCase() : '';
}

function isImageFile(url: string): boolean {
  return ['jpg', 'jpeg', 'png', 'gif', 'webp', 'bmp'].includes(getFileExt(url));
}

/* ── Attachment View Modal ───────────────────────────────────────────── */
function CredentialFileViewModal({ url, fileName, onClose }: { url: string; fileName?: string; onClose: () => void }) {
  if (!url) return null;
  const ext = getFileExt(url);
  const isImage = isImageFile(url);
  const isPdf = ext === 'pdf';

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4" onClick={onClose}>
      <div
        className="bg-surface rounded-2xl shadow-2xl max-w-4xl w-full max-h-[90vh] flex flex-col overflow-hidden"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="flex items-center justify-between p-4 border-b border-surface-container-highest">
          <div className="flex items-center gap-2 min-w-0">
            <span className="material-symbols-outlined text-primary">
              {isImage ? 'image' : isPdf ? 'description' : 'insert_drive_file'}
            </span>
            <h3 className="font-bold text-on-surface truncate text-sm">{fileName || 'Xem tài liệu chứng chỉ'}</h3>
          </div>
          <div className="flex items-center gap-2 shrink-0">
            <a
              href={url}
              download
              className="flex items-center gap-1 py-1.5 px-4 rounded-full bg-primary text-on-primary text-xs font-semibold hover:brightness-110"
            >
              <span className="material-symbols-outlined text-base">download</span>
              Tải về
            </a>
            <button
              onClick={onClose}
              className="w-8 h-8 rounded-full border border-outline-variant flex items-center justify-center text-outline hover:text-on-surface"
            >
              <span className="material-symbols-outlined text-lg">close</span>
            </button>
          </div>
        </div>

        <div className="flex-1 overflow-auto p-4 flex items-center justify-center bg-surface-container-lowest">
          {isImage ? (
            <img src={url} alt={fileName || 'Tài liệu'} className="max-w-full max-h-[70vh] object-contain rounded-xl" />
          ) : isPdf ? (
            <iframe src={url} className="w-full h-[70vh] rounded-xl" title={fileName || 'PDF'} />
          ) : (
            <div className="text-center py-12">
              <span className="material-symbols-outlined text-5xl text-outline mb-2">description</span>
              <p className="text-on-surface-variant text-sm font-semibold mb-1">Định dạng file .{ext}</p>
              <p className="text-xs text-outline mb-4">Vui lòng tải tệp về để mở bằng phần mềm phù hợp</p>
              <a href={url} download className="py-2 px-5 rounded-full bg-primary text-on-primary text-xs font-semibold">
                Tải xuống tệp
              </a>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

/* ── Main Component ─────────────────────────────────────────────────── */
export default function VerificationDocumentsPage() {
  const [credentials, setCredentials] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [showUpload, setShowUpload] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [viewFileUrl, setViewFileUrl] = useState<string | null>(null);

  const [form, setForm] = useState({
    credentialType: '',
    credentialNumber: '',
    issuer: '',
    issuedDate: '',
    expiryDate: '',
  });
  const [customIssuer, setCustomIssuer] = useState('');
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [deletingId, setDeletingId] = useState<string | null>(null);

  const load = async () => {
    try {
      const data = await getMyCredentials();
      setCredentials(data || []);
    } catch (e: any) {
      setError(e.response?.data?.message ?? 'Không thể tải danh sách chứng chỉ');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
  }, []);

  const openUploadForm = () => {
    setForm({ credentialType: '', credentialNumber: '', issuer: '', issuedDate: '', expiryDate: '' });
    setCustomIssuer('');
    setSelectedFile(null);
    setError(null);
    setShowUpload(true);
  };

  const onSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setSubmitting(true);
    setError(null);
    try {
      const body =
        form.issuer === 'Khác' && customIssuer.trim() ? { ...form, issuer: customIssuer.trim() } : form;
      await submitCredential({ body, file: selectedFile! });
      setForm({ credentialType: '', credentialNumber: '', issuer: '', issuedDate: '', expiryDate: '' });
      setCustomIssuer('');
      setSelectedFile(null);
      setShowUpload(false);
      await load();
    } catch (e: any) {
      setError(e.response?.data?.message ?? 'Tải lên chứng chỉ thất bại');
    } finally {
      setSubmitting(false);
    }
  };

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0] ?? null;
    setSelectedFile(file);
    if (file) setError(null);
  };

  const handleDelete = async (id: string) => {
    if (!window.confirm('Bạn có chắc muốn xóa chứng chỉ này?')) return;
    setDeletingId(id);
    try {
      await deleteCredential(id);
      setCredentials((prev) => prev.filter((c) => c.credentialId !== id));
    } catch {
      alert('Không thể xóa chứng chỉ. Vui lòng thử lại.');
    } finally {
      setDeletingId(null);
    }
  };

  if (loading) {
    return <div className="py-12 text-center text-outline">Đang tải danh sách chứng chỉ...</div>;
  }

  const approvedCount = credentials.filter((c) => c.reviewStatus === 'APPROVED').length;
  const pendingCount = credentials.filter((c) => c.reviewStatus === 'PENDING' || c.reviewStatus === 'UNDER_REVIEW').length;

  return (
    <div className="p-8 font-sans max-w-4xl">
      {/* Header */}
      <div className="flex justify-between items-start mb-6">
        <div>
          <h1 className="text-[26px] font-bold text-on-surface m-0">Chứng chỉ &amp; Giấy tờ</h1>
          <p className="text-on-surface-variant text-sm mt-1">
            Quản lý các bằng cấp, chứng chỉ hành nghề y tế và tài liệu xác minh năng lực
          </p>
        </div>
        <button
          onClick={openUploadForm}
          className="flex items-center gap-2 py-3 px-6 rounded-full bg-primary text-on-primary text-sm font-semibold cursor-pointer whitespace-nowrap hover:brightness-110"
        >
          <span className="material-symbols-outlined text-lg">add</span>
          Tải lên chứng chỉ mới
        </button>
      </div>

      {error && !showUpload && (
        <div className="bg-error-container rounded-2xl p-4 mb-4 text-error text-sm">{error}</div>
      )}

      {/* Summary Chips / Mini Stats */}
      <div className="grid grid-cols-3 gap-4 mb-6">
        <div className="bg-surface rounded-2xl p-4 shadow-md flex items-center gap-3">
          <div className="w-10 h-10 rounded-full bg-primary-container text-primary flex items-center justify-center shrink-0">
            <span className="material-symbols-outlined text-xl">description</span>
          </div>
          <div>
            <div className="text-xl font-bold text-on-surface">{credentials.length}</div>
            <div className="text-xs text-outline">Tổng số chứng chỉ</div>
          </div>
        </div>

        <div className="bg-surface rounded-2xl p-4 shadow-md flex items-center gap-3">
          <div className="w-10 h-10 rounded-full bg-[#E6F4EA] text-[#137333] flex items-center justify-center shrink-0">
            <span className="material-symbols-outlined text-xl">verified</span>
          </div>
          <div>
            <div className="text-xl font-bold text-on-surface">{approvedCount}</div>
            <div className="text-xs text-outline">Đã duyệt xác minh</div>
          </div>
        </div>

        <div className="bg-surface rounded-2xl p-4 shadow-md flex items-center gap-3">
          <div className="w-10 h-10 rounded-full bg-[#FFF3E0] text-[#E65100] flex items-center justify-center shrink-0">
            <span className="material-symbols-outlined text-xl">pending_actions</span>
          </div>
          <div>
            <div className="text-xl font-bold text-on-surface">{pendingCount}</div>
            <div className="text-xs text-outline">Đang chờ xét duyệt</div>
          </div>
        </div>
      </div>

      {/* Upload Form Modal/Expandable Card */}
      {showUpload && (
        <form onSubmit={onSubmit} className="bg-surface rounded-2xl p-6 shadow-md mb-6 space-y-4">
          <div className="flex justify-between items-center pb-3 border-b border-surface-container-highest">
            <h3 className="font-bold text-base text-on-surface flex items-center gap-2">
              <span className="material-symbols-outlined text-primary text-xl">upload_file</span>
              Tải lên chứng chỉ / bằng cấp mới
            </h3>
            <button
              type="button"
              onClick={() => setShowUpload(false)}
              className="w-8 h-8 rounded-full border border-outline-variant flex items-center justify-center text-outline hover:text-on-surface"
            >
              <span className="material-symbols-outlined text-lg">close</span>
            </button>
          </div>

          {error && <div className="bg-error-container rounded-2xl p-3 text-error text-xs">{error}</div>}

          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-semibold text-outline uppercase tracking-wider mb-2">
                Loại chứng chỉ <span className="text-error">*</span>
              </label>
              <select
                required
                className="w-full py-2.5 px-4 rounded-2xl border border-outline-variant bg-surface text-sm text-on-surface outline-none focus:border-primary font-sans cursor-pointer"
                value={form.credentialType}
                onChange={(e) => setForm({ ...form, credentialType: e.target.value })}
              >
                <option value="">-- Chọn loại chứng chỉ --</option>
                <option value="MEDICAL_LICENSE">Giấy phép hành nghề y</option>
                <option value="DEGREE">Bằng cấp chuyên môn</option>
                <option value="CERTIFICATE">Chứng chỉ đào tạo</option>
                <option value="IDENTITY_DOCUMENT">Giấy tờ định danh</option>
                <option value="PROFESSIONAL_LICENSE">Giấy phép hành nghề</option>
              </select>
            </div>

            <div>
              <label className="block text-xs font-semibold text-outline uppercase tracking-wider mb-2">
                Số chứng chỉ / Số hiệu
              </label>
              <input
                className="w-full py-2.5 px-4 rounded-2xl border border-outline-variant bg-surface text-sm text-on-surface outline-none focus:border-primary font-sans"
                value={form.credentialNumber}
                onChange={(e) => setForm({ ...form, credentialNumber: e.target.value })}
                placeholder="VD: 012345/BYT-CCHN..."
              />
            </div>
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-semibold text-outline uppercase tracking-wider mb-2">
                Đơn vị / Nơi cấp
              </label>
              <select
                className="w-full py-2.5 px-4 rounded-2xl border border-outline-variant bg-surface text-sm text-on-surface outline-none focus:border-primary font-sans cursor-pointer"
                value={form.issuer}
                onChange={(e) => setForm({ ...form, issuer: e.target.value })}
              >
                <option value="">-- Chọn nơi cấp --</option>
                {ISSUERS.map((s) => (
                  <option key={s} value={s}>
                    {s}
                  </option>
                ))}
              </select>
              {form.issuer === 'Khác' && (
                <input
                  className="mt-2 w-full py-2 px-3 rounded-xl border border-outline-variant bg-surface text-xs text-on-surface"
                  placeholder="Nhập tên cơ quan cấp..."
                  value={customIssuer}
                  onChange={(e) => setCustomIssuer(e.target.value)}
                />
              )}
            </div>

            <div className="grid grid-cols-2 gap-2">
              <div>
                <label className="block text-xs font-semibold text-outline uppercase tracking-wider mb-2">
                  Ngày cấp <span className="text-error">*</span>
                </label>
                <input
                  type="date"
                  required
                  className="w-full py-2.5 px-3 rounded-2xl border border-outline-variant bg-surface text-xs text-on-surface outline-none focus:border-primary"
                  value={form.issuedDate}
                  onChange={(e) => setForm({ ...form, issuedDate: e.target.value })}
                />
              </div>
              <div>
                <label className="block text-xs font-semibold text-outline uppercase tracking-wider mb-2">
                  Hết hạn (tùy chọn)
                </label>
                <input
                  type="date"
                  className="w-full py-2.5 px-3 rounded-2xl border border-outline-variant bg-surface text-xs text-on-surface outline-none focus:border-primary"
                  value={form.expiryDate}
                  onChange={(e) => setForm({ ...form, expiryDate: e.target.value })}
                />
              </div>
            </div>
          </div>

          <div>
            <label className="block text-xs font-semibold text-outline uppercase tracking-wider mb-2">
              Tệp đính kèm (PDF, JPG, PNG - tối đa 20MB) <span className="text-error">*</span>
            </label>
            <input
              type="file"
              required
              accept=".pdf,.doc,.docx,.jpg,.jpeg,.png,.gif"
              className="w-full py-2 px-4 rounded-2xl border border-outline-variant bg-surface text-xs text-on-surface cursor-pointer"
              onChange={handleFileChange}
            />
          </div>

          <div className="flex justify-end gap-3 pt-3">
            <button
              type="button"
              onClick={() => setShowUpload(false)}
              className="py-2.5 px-5 rounded-full border border-outline-variant bg-transparent text-on-surface-variant text-xs font-semibold hover:bg-surface-container-low cursor-pointer"
            >
              Hủy
            </button>
            <button
              type="submit"
              disabled={submitting || !form.credentialType || !form.issuedDate || !selectedFile}
              className="py-2.5 px-6 rounded-full bg-primary text-on-primary text-xs font-semibold cursor-pointer hover:brightness-110 disabled:opacity-50"
            >
              {submitting ? 'Đang gửi...' : 'Gửi xét duyệt'}
            </button>
          </div>
        </form>
      )}

      {/* Document List */}
      <div className="space-y-4">
        {credentials.length === 0 && !showUpload && (
          <div className="bg-surface rounded-2xl p-12 text-center text-outline shadow-md">
            <span className="material-symbols-outlined text-4xl block mb-2 opacity-50">description</span>
            Chưa có chứng chỉ nào. Nhấn "Tải lên chứng chỉ mới" để bắt đầu.
          </div>
        )}

        {credentials
          .sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())
          .map((cred) => {
            const isApproved = cred.reviewStatus === 'APPROVED';
            const isRejected = cred.reviewStatus === 'REJECTED';
            const statusCls = isApproved
              ? 'bg-[#E6F4EA] text-[#137333]'
              : isRejected
              ? 'bg-error-container text-error'
              : 'bg-[#FFF3E0] text-[#E65100]';
            const statusLabel = isApproved ? 'Đã duyệt' : isRejected ? 'Bị từ chối' : 'Chờ xét duyệt';

            return (
              <div key={cred.credentialId} className="bg-surface rounded-2xl p-5 shadow-md flex items-start justify-between hover:shadow-lg transition-shadow">
                <div className="flex items-start gap-4">
                  <div className="w-11 h-11 rounded-full bg-primary-container text-primary flex items-center justify-center shrink-0 mt-0.5">
                    <span className="material-symbols-outlined text-xl">
                      {cred.credentialType === 'DEGREE' ? 'school' : 'description'}
                    </span>
                  </div>

                  <div>
                    <div className="flex items-center gap-2 mb-1">
                      <span className="font-bold text-sm text-on-surface">
                        {TYPE_LABELS[cred.credentialType] || cred.credentialType}
                      </span>
                      <span className={`py-0.5 px-3 rounded-full text-xs font-semibold ${statusCls}`}>
                        {statusLabel}
                      </span>
                    </div>

                    {cred.credentialNumber && (
                      <p className="text-xs text-on-surface-variant">Số hiệu: <span className="font-semibold">{cred.credentialNumber}</span></p>
                    )}
                    {cred.issuer && <p className="text-xs text-outline mt-0.5">Nơi cấp: {cred.issuer}</p>}

                    <div className="flex items-center gap-3 text-xs text-outline mt-2">
                      <span>Ngày cấp: {cred.issuedDate}</span>
                      {cred.expiryDate && <span>• Hết hạn: {cred.expiryDate}</span>}
                    </div>

                    {cred.reviewNote && (
                      <p className="text-xs text-error mt-2 bg-error-container/40 p-2 rounded-xl">
                        Ghi chú phản hồi: {cred.reviewNote}
                      </p>
                    )}
                  </div>
                </div>

                <div className="flex items-center gap-2">
                  {cred.fileUrl && (
                    <button
                      onClick={() => setViewFileUrl(cred.fileUrl)}
                      className="py-1.5 px-3.5 rounded-lg border border-outline-variant bg-transparent text-primary text-xs font-semibold hover:bg-surface-container-low cursor-pointer flex items-center gap-1"
                    >
                      <span className="material-symbols-outlined text-sm">visibility</span>
                      Xem file
                    </button>
                  )}
                  <button
                    onClick={() => handleDelete(cred.credentialId)}
                    disabled={deletingId === cred.credentialId}
                    className="w-8 h-8 rounded-lg border border-outline-variant bg-transparent text-error flex items-center justify-center hover:bg-error-container/30 cursor-pointer disabled:opacity-40"
                    title="Xóa"
                  >
                    <span className="material-symbols-outlined text-base">delete</span>
                  </button>
                </div>
              </div>
            );
          })}
      </div>

      {viewFileUrl && <CredentialFileViewModal url={viewFileUrl} onClose={() => setViewFileUrl(null)} />}
    </div>
  );
}

