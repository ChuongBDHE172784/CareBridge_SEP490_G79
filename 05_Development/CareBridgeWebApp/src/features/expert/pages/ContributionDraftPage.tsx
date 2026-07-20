import { useEffect, useState, useRef } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { createContribution, getContribution, updateContribution, submitContribution, checkContributionEligibility, uploadContributionFile, type CreateContributionRequest, type UpdateContributionRequest, type ContributionAttachmentRequest, type UploadFileResponse } from '../services/expertApi';

export default function ContributionDraftPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const isEditing = Boolean(id);

  const [eligible, setEligible] = useState(false);
  const [loading, setLoading] = useState(!isEditing);
  const [saving, setSaving] = useState(false);
  const [submitMode, setSubmitMode] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Form state
  const [title, setTitle] = useState('');
  const [content, setContent] = useState('');
  const [specialtyId, setSpecialtyId] = useState<string | undefined>(undefined);
  const [hospitalId, setHospitalId] = useState<string | undefined>(undefined);
  const [attachments, setAttachments] = useState<ContributionAttachmentRequest[]>([]);

  // File upload state
  const [uploadingFiles, setUploadingFiles] = useState<Set<string>>(new Set());
  const [filePreviews, setFilePreviews] = useState<Map<string, { file: File; preview: string }>>(new Map());

  useEffect(() => {
    loadEligibility();
    loadMasterData();
    if (isEditing) loadContribution();
  }, [id]);

  async function loadEligibility() {
    try {
      const e = await checkContributionEligibility();
      setEligible(e);
    } catch {
      setEligible(false);
    }
  }

  async function loadMasterData() {
    // In production, fetch from master data API
    // For now, we'll use the input fields for specialtyId/hospitalId as strings
  }

  async function loadContribution() {
    if (!id) return;
    setLoading(true);
    try {
      const c = await getContribution(id);
      setTitle(c.title);
      setContent(c.content);
      setSpecialtyId(c.specialtyId);
      setHospitalId(c.hospitalId);
      if (c.attachments) {
        setAttachments(c.attachments.map(a => ({
          fileId: a.fileId,
          kind: a.kind,
          purpose: a.purpose,
          accessMode: a.accessMode,
          displayOrder: a.displayOrder,
        })));
        // Load previews for existing attachments
        for (const att of c.attachments) {
          const origName = att.originalName;
          const presignedUrl = att.presignedUrl;
          if (origName && presignedUrl) {
            setFilePreviews(prev => new Map(prev).set(att.fileId, {
              file: new File([], origName, { type: att.mimeType }),
              preview: presignedUrl
            }));
          }
        }
      }
    } catch (err: any) {
      setError(err.response?.data?.message || 'Không thể tải bài viết');
    } finally {
      setLoading(false);
    }
  }

  async function handleFileUpload(file: File, kind: 'IMAGE' | 'DOCUMENT'): Promise<string> {
    const fileId = `temp_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
    setUploadingFiles(prev => new Set(prev).add(fileId));
    setFilePreviews(prev => new Map(prev).set(fileId, {
      file,
      preview: kind === 'IMAGE' ? URL.createObjectURL(file) : ''
    }));

    try {
      const purpose = kind === 'IMAGE' ? 'MEDICAL_CONTRIBUTION_IMAGE' : 'MEDICAL_CONTRIBUTION_DOCUMENT';
      const accessMode = 'AUTHENTICATED';

      const response: UploadFileResponse = await uploadContributionFile(file, kind, purpose, accessMode);
      const fileId = response.fileId;

      setAttachments(prev => [...prev, {
        fileId,
        kind,
        purpose,
        accessMode,
        displayOrder: prev.length,
      }]);

      setFilePreviews(prev => {
        const next = new Map(prev);
        next.delete(fileId);
        next.set(fileId, { file, preview: kind === 'IMAGE' ? URL.createObjectURL(file) : '' });
        return next;
      });

      return fileId;
    } catch (err) {
      setError('Tải file thất bại');
      throw err;
    } finally {
      setUploadingFiles(prev => {
        const next = new Set(prev);
        next.delete(fileId);
        return next;
      });
    }
  }

  function handleFileSelect(e: React.ChangeEvent<HTMLInputElement>, kind: 'IMAGE' | 'DOCUMENT') {
    const files = e.target.files;
    if (!files) return;
    Array.from(files).forEach(file => handleFileUpload(file, kind));
  }

  function removeAttachment(fileId: string) {
    setAttachments(prev => prev.filter(a => a.fileId !== fileId));
    setFilePreviews(prev => {
      const next = new Map(prev);
      const item = next.get(fileId);
      if (item?.preview) URL.revokeObjectURL(item.preview);
      next.delete(fileId);
      return next;
    });
  }

  function validateForm(): boolean {
    if (!title.trim()) {
      setError('Tiêu đề là bắt buộc');
      return false;
    }
    if (!content.trim()) {
      setError('Nội dung là bắt buộc');
      return false;
    }
    return true;
  }

  async function handleSave() {
    if (!validateForm()) return;
    setSaving(true);
    setError(null);

    try {
      const body: CreateContributionRequest & UpdateContributionRequest = {
        title,
        content,
        specialtyId,
        hospitalId,
        attachments: attachments.map((a, i) => ({ ...a, displayOrder: i })),
      };

      if (isEditing) {
        await updateContribution(id!, body);
      } else {
        await createContribution(body);
      }
      navigate('/expert/contributions');
    } catch (err: any) {
      setError(err.response?.data?.message || 'Lưu thất bại');
    } finally {
      setSaving(false);
    }
  }

  async function handleSubmit() {
    if (!validateForm()) return;
    if (!isEditing) {
      await handleSave();
      return;
    }
    setSaving(true);
    setError(null);
    try {
      await submitContribution(id!);
      navigate('/expert/contributions');
    } catch (err: any) {
      setError(err.response?.data?.message || 'Gửi duyệt thất bại');
    } finally {
      setSaving(false);
    }
  }

  if (loading) return <div className="flex justify-center py-12">Đang tải...</div>;
  if (!eligible && !isEditing) {
    return (
      <div className="max-w-2xl mx-auto p-6 text-center">
        <div className="text-5xl mb-4">🔒</div>
        <h2 className="text-xl font-semibold text-gray-900 mb-2">Chưa đủ điều kiện</h2>
        <p className="text-gray-500">
          Bạn cần được xác minh (APPROVED) và có trạng thái trust ACTIVE để tạo bài viết.
        </p>
      </div>
    );
  }

  return (
    <div className="max-w-3xl mx-auto p-6">
      <div className="mb-6">
        <h1 className="text-2xl font-bold text-on-surface">
          {isEditing ? (submitMode ? 'Gửi duyệt bài viết' : 'Chỉnh sửa bài viết') : 'Tạo bài viết mới'}
        </h1>
        <p className="text-gray-500 mt-1">
          {isEditing ? 'Cập nhật thông tin bài viết y khoa' : 'Chia sẻ kiến thức y khoa chuyên môn'}
        </p>
      </div>

      {error && (
        <div className="mb-4 p-3 rounded bg-red-50 border border-red-200 text-red-700 text-sm">{error}</div>
      )}

      <div className="bg-white rounded-lg border border-gray-200 shadow-sm p-6 space-y-6">
        {/* Basic Info */}
        <section>
          <h2 className="text-lg font-semibold text-gray-900 mb-4">Thông tin cơ bản</h2>
          <div className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Tiêu đề *</label>
              <input
                type="text"
                maxLength={255}
                className="w-full rounded border border-gray-300 px-3 py-2 focus:border-primary focus:ring-1 focus:ring-primary"
                value={title}
                onChange={e => setTitle(e.target.value)}
                placeholder="VD: Hướng dẫn chăm sóc sơ sinh đối với mẹ lần đầu"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Nội dung *</label>
              <textarea
                rows={10}
                maxLength={10000}
                className="w-full rounded border border-gray-300 px-3 py-2 focus:border-primary focus:ring-1 focus:ring-primary"
                value={content}
                onChange={e => setContent(e.target.value)}
                placeholder="Viết nội dung chi tiết bài viết y khoa tại đây..."
              />
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Chuyên khoa (ID)</label>
                <input
                  type="text"
                  className="w-full rounded border border-gray-300 px-3 py-2 focus:border-primary focus:ring-1 focus:ring-primary"
                  value={specialtyId || ''}
                  onChange={e => setSpecialtyId(e.target.value || undefined)}
                  placeholder="VD: OBG (Sản khoa), PED (Nhi khoa)..."
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Bệnh viện (ID)</label>
                <input
                  type="text"
                  className="w-full rounded border border-gray-300 px-3 py-2 focus:border-primary focus:ring-1 focus:ring-primary"
                  value={hospitalId || ''}
                  onChange={e => setHospitalId(e.target.value || undefined)}
                  placeholder="VD: HOSP001, BVTW..."
                />
              </div>
            </div>
          </div>
        </section>

        {/* Image Upload Zone */}
        <section className="pt-4 border-t border-gray-100">
          <h2 className="text-lg font-semibold text-gray-900 mb-4 flex items-center gap-2">
            🖼️ Vùng tải ảnh (Images → Cloudinary)
            <span className="text-sm text-gray-500 font-normal">JPG, PNG, WebP, HEIC, GIF · ≤ 20MB</span>
          </h2>
          <FileDropZone
            kind="IMAGE"
            attachments={attachments.filter(a => a.kind === 'IMAGE')}
            previews={filePreviews}
            uploading={uploadingFiles}
            onFileSelect={handleFileSelect}
            onRemove={removeAttachment}
          />
        </section>

        {/* Document Upload Zone */}
        <section className="pt-4 border-t border-gray-100">
          <h2 className="text-lg font-semibold text-gray-900 mb-4 flex items-center gap-2">
            📄 Vùng tải tài liệu (Documents → R2)
            <span className="text-sm text-gray-500 font-normal">PDF, DOC, DOCX · ≤ 20MB · Private (15-min presigned URL)</span>
          </h2>
          <FileDropZone
            kind="DOCUMENT"
            attachments={attachments.filter(a => a.kind === 'DOCUMENT')}
            previews={filePreviews}
            uploading={uploadingFiles}
            onFileSelect={handleFileSelect}
            onRemove={removeAttachment}
          />
        </section>

        {/* Actions */}
        <div className="pt-4 border-t border-gray-100 flex flex-wrap gap-3">
          <button
            type="button"
            onClick={handleSave}
            disabled={saving}
            className="px-5 py-2.5 rounded border border-gray-300 text-gray-700 hover:bg-gray-50 disabled:opacity-50"
          >
            {saving ? 'Đang lưu...' : isEditing ? 'Cập nhật' : 'Lưu bản nháp'}
          </button>

          {isEditing && (
            <button
              type="button"
              onClick={handleSubmit}
              disabled={saving}
              className="px-5 py-2.5 rounded bg-primary text-white font-medium hover:bg-primary/90 disabled:opacity-50"
            >
              {saving ? 'Đang gửi...' : 'Gửi duyệt'}
            </button>
          )}

          {!isEditing && !submitMode && (
            <button
              type="button"
              onClick={() => setSubmitMode(true)}
              className="px-5 py-2.5 rounded border border-primary text-primary hover:bg-primary/5"
            >
              Lưu và gửi duyêt ngay
            </button>
          )}

          <button
            type="button"
            onClick={() => navigate('/expert/contributions')}
            className="px-5 py-2.5 rounded border border-gray-300 text-gray-700 hover:bg-gray-50"
          >
            Hủy
          </button>
        </div>
      </div>
    </div>
  );
}

interface FileDropZoneProps {
  kind: 'IMAGE' | 'DOCUMENT';
  attachments: ContributionAttachmentRequest[];
  previews: Map<string, { file: File; preview: string }>;
  uploading: Set<string>;
  onFileSelect: (e: React.ChangeEvent<HTMLInputElement>, kind: 'IMAGE' | 'DOCUMENT') => void;
  onRemove: (fileId: string) => void;
}

function FileDropZone({ kind, attachments, previews, uploading, onFileSelect, onRemove }: FileDropZoneProps) {
  const inputRef = useRef<HTMLInputElement>(null);
  const isDragActive = useRef(false);

  const handleDrag = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    if (e.type === 'dragenter' || e.type === 'dragover') {
      isDragActive.current = true;
    } else if (e.type === 'dragleave') {
      isDragActive.current = false;
    }
  };

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    isDragActive.current = false;
    if (e.dataTransfer.files.length > 0) {
      onFileSelect({ target: { files: e.dataTransfer.files } } as any, kind);
    }
  };

  return (
    <div className={`relative border-2 border-dashed rounded-lg p-6 transition-colors ${isDragActive.current ? 'border-primary bg-primary/5' : 'border-gray-300'}`}>
      <input
        ref={inputRef}
        type="file"
        multiple
        accept={kind === 'IMAGE' ? 'image/*,.heic' : '.pdf,.doc,.docx'}
        onChange={e => onFileSelect(e, kind)}
        className="hidden"
        id={`file-upload-${kind.toLowerCase()}`}
      />

      <div className="text-center" onClick={() => inputRef.current?.click()}>
        <div className="text-4xl mb-2">{kind === 'IMAGE' ? '🖼️' : '📄'}</div>
        <p className="text-gray-600">
          Kéo thả file vào đây hoặc <span className="text-primary underline cursor-pointer">nhấn để chọn</span>
        </p>
        <p className="text-xs text-gray-400 mt-1">
          {kind === 'IMAGE' ? 'Hỗ trợ: JPG, PNG, WebP, HEIC, GIF' : 'Hỗ trợ: PDF, DOC, DOCX'} · Tối đa 20MB/file
        </p>
      </div>

      <div onDragEnter={handleDrag} onDragLeave={handleDrag} onDragOver={handleDrag} onDrop={handleDrop} />

      {attachments.length > 0 && (
        <div className="mt-4 space-y-2">
          <p className="text-sm font-medium text-gray-700">Đã chọn ({attachments.length}):</p>
          <div className="flex flex-wrap gap-2">
            {attachments.map((att, idx) => {
              const preview = previews.get(att.fileId);
              const isUploading = uploading.has(att.fileId);
              return (
                <div key={att.fileId} className="relative group flex items-center gap-2 bg-gray-50 rounded-lg border border-gray-200 p-2">
                  {kind === 'IMAGE' && preview?.preview && (
                    <img src={preview.preview} alt="" className="w-10 h-10 rounded object-cover" />
                  )}
                  {kind === 'DOCUMENT' && (
                    <div className="w-10 h-10 rounded bg-blue-50 flex items-center justify-center text-blue-600 font-mono text-xs">
                      📄
                    </div>
                  )}
                  <div className="flex-1 min-w-0">
                    <p className="text-sm font-medium text-gray-900 truncate">{preview?.file.name || `${kind.toLowerCase()} ${idx + 1}`}</p>
                    <p className="text-xs text-gray-500">
                      {isUploading ? 'Đang tải...' : 'Đã sẵn sàng'}
                    </p>
                  </div>
                  <button
                    onClick={() => onRemove(att.fileId)}
                    className="p-1 text-gray-400 hover:text-red-500 opacity-0 group-hover:opacity-100 transition-opacity"
                    disabled={isUploading}
                  >
                    ✕
                  </button>
                </div>
              );
            })}
          </div>
        </div>
      )}
    </div>
  );
}