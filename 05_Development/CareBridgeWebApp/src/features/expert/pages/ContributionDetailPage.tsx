import { useEffect, useState } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { getContribution, type ContributionResponse, type ContributionStatus } from '../services/expertApi';

const STATUS_LABELS: Record<ContributionStatus, string> = {
  DRAFT: 'Bản nháp',
  SUBMITTED: 'Đã gửi duyệt',
  APPROVED: 'Đã duyệt',
  REJECTED: 'Từ chối',
};

const STATUS_CLASSES: Record<ContributionStatus, string> = {
  DRAFT: 'bg-gray-50 text-gray-700 border-gray-200',
  SUBMITTED: 'bg-yellow-50 text-yellow-700 border-yellow-200',
  APPROVED: 'bg-green-50 text-green-700 border-green-200',
  REJECTED: 'bg-red-50 text-red-700 border-red-200',
};

export default function ContributionDetailPage() {
  const { id, mode } = useParams<{ id: string; mode?: string }>();
  const navigate = useNavigate();

  const [contribution, setContribution] = useState<ContributionResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const isSubmitting = mode === 'submit';
  const isDraft = contribution?.status === 'DRAFT';

  useEffect(() => {
    if (!id) return;
    loadContribution();
  }, [id]);

  async function loadContribution() {
    setLoading(true);
    try {
      const c = await getContribution(id!);
      setContribution(c);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Không thể tải bài viết');
    } finally {
      setLoading(false);
    }
  }

  async function handleSubmit() {
    if (!id || !window.confirm('Gửi bài viết này để duyệt?')) return;
    try {
      await getContribution(id); // refresh to ensure valid state
      await fetch(`/api/v1/contributions/${id}/submit`, { method: 'POST' });
      navigate(`/expert/contributions/${id}`);
    } catch (err) {
      alert('Gửi duyệt thất bại');
    }
  }

  async function handleDelete() {
    if (!id || !window.confirm('Xóa vĩnh viễn bản nháp này? Không thể hoàn tác.')) return;
    try {
      await fetch(`/api/v1/contributions/${id}`, { method: 'DELETE' });
      navigate('/expert/contributions');
    } catch (err) {
      alert('Xóa thất bại');
    }
  }

  if (loading) return <div className="flex justify-center py-12">Đang tải...</div>;
  if (error || !contribution) {
    return (
      <div className="max-w-2xl mx-auto p-6 text-center">
        <div className="text-5xl mb-4">⚠️</div>
        <h2 className="text-xl font-semibold text-gray-900 mb-2">Không tìm thấy bài viết</h2>
        <p className="text-gray-500">{error}</p>
        <Link to="/expert/contributions" className="mt-4 inline-block text-primary hover:underline">
          ← Quay về danh sách
        </Link>
      </div>
    );
  }

  const isOwnerDraft = isDraft; // In real app, check if current user is owner

  return (
    <div className="max-w-3xl mx-auto p-6">
      <div className="flex flex-col sm:flex-row sm:items-start sm:justify-between gap-4 mb-6">
        <div>
          <Link to="/expert/contributions" className="text-sm text-gray-500 hover:underline mb-2 inline-block">
            ← Quay về danh sách
          </Link>
          <h1 className="text-2xl font-bold text-on-surface">{contribution.title}</h1>
        </div>
        <div className="flex flex-wrap gap-2">
          <span className={`inline-flex px-3 py-1.5 text-sm font-medium rounded-full border ${STATUS_CLASSES[contribution.status] || ''}`}>
            {STATUS_LABELS[contribution.status] || contribution.status}
          </span>
          {contribution.rejectionReason && (
            <span className="px-3 py-1.5 text-sm text-red-600 bg-red-50 border border-red-200 rounded-full truncate max-w-xs" title={contribution.rejectionReason}>
              Lý do: {contribution.rejectionReason}
            </span>
          )}
        </div>
      </div>

      {isSubmitting && isDraft && (
        <div className="mb-6 p-4 rounded bg-yellow-50 border border-yellow-200">
          <p className="text-yellow-800 font-medium">Xác nhận gửi duyệt bài viết?</p>
          <p className="text-sm text-yellow-700 mt-1">Sau khi gửi, bạn sẽ không thể chỉnh sửa nội dung cho đến khi được duyệt hoặc từ chối.</p>
          <div className="flex gap-2 mt-3">
            <button onClick={handleSubmit} className="px-4 py-2 rounded bg-yellow-600 text-white hover:bg-yellow-700">Gửi duyệt</button>
            <button onClick={() => navigate(`/expert/contributions/${id}`)} className="px-4 py-2 rounded border border-gray-300 text-gray-700 hover:bg-gray-50">Hủy</button>
          </div>
        </div>
      )}

      <div className="bg-white rounded-lg border border-gray-200 shadow-sm divide-y divide-gray-100">
        {/* Metadata */}
        <div className="p-6 bg-gray-50 border-b border-gray-100">
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 text-sm">
            <div>
              <p className="text-gray-500">Chuyên khoa</p>
              <p className="font-medium text-gray-900">{contribution.specialtyId || '—'}</p>
            </div>
            <div>
              <p className="text-gray-500">Bệnh viện</p>
              <p className="font-medium text-gray-900">{contribution.hospitalId || '—'}</p>
            </div>
            <div>
              <p className="text-gray-500">Tác giả</p>
              <p className="font-medium text-gray-900">{contribution.expertUserId}</p>
            </div>
            <div>
              <p className="text-gray-500">Phiên bản</p>
              <p className="font-medium text-gray-900">#{contribution.version}</p>
            </div>
          </div>
          <div className="mt-4 text-sm text-gray-500">
            <span>Tạo: {new Date(contribution.createdAt).toLocaleString('vi-VN')}</span> |
            <span className="ml-2">Cập nhật: {new Date(contribution.updatedAt).toLocaleString('vi-VN')}</span>
          </div>
        </div>

        {/* Content */}
        <div className="p-6">
          <div className="prose prose-gray max-w-none">
            <div dangerouslySetInnerHTML={{ __html: contribution.content.replace(/\n/g, '<br>') }} />
          </div>
        </div>

        {/* Attachments */}
        {(contribution.attachments?.length ?? 0) > 0 && (
          <div className="p-6 bg-gray-50 border-t border-gray-100">
            <h3 className="font-semibold text-gray-900 mb-4">Tệp đính kèm ({contribution.attachments!.length})</h3>
            <div className="flex flex-wrap gap-3">
              {contribution.attachments!.map((att) => (
                <AttachmentCard key={att.id} attachment={att} />
              ))}
            </div>
          </div>
        )}

        {/* Actions for draft owner */}
        {isOwnerDraft && contribution.status === 'DRAFT' && (
          <div className="p-6 bg-white border-t border-gray-100 flex flex-wrap gap-3">
            <Link
              to={`/expert/contributions/${id}/edit`}
              className="px-4 py-2.5 rounded border border-gray-300 text-gray-700 hover:bg-gray-50"
            >
              Chỉnh sửa
            </Link>
            <button
              onClick={handleDelete}
              className="px-4 py-2.5 rounded border border-red-300 text-red-600 hover:bg-red-50"
            >
              Xóa
            </button>
            <button
              onClick={() => navigate(`/expert/contributions/${id}?mode=submit`)}
              className="px-4 py-2.5 rounded bg-primary text-white hover:bg-primary/90"
            >
              Gửi duyệt
            </button>
          </div>
        )}
      </div>
    </div>
  );
}

interface AttachmentCardProps {
  attachment: {
    id: string;
    fileId: string;
    contributionId: string;
    kind: 'IMAGE' | 'DOCUMENT';
    purpose: string;
    accessMode: string;
    displayOrder: number;
    originalName: string | null;
    mimeType: string;
    fileSizeBytes: number;
    presignedUrl: string | null;
  };
}

function AttachmentCard({ attachment }: AttachmentCardProps) {
  const isImage = attachment.kind === 'IMAGE';
  const url = attachment.presignedUrl;

  if (!url) {
    return (
      <div className="group relative w-48 h-48 bg-white rounded-lg border border-gray-200 overflow-hidden flex flex-col">
        <div className="flex-1 w-full flex items-center justify-center bg-gray-50">
          <div className="text-center p-4 text-gray-500">
            <div className="text-3xl mb-1">{isImage ? '🖼️' : '📄'}</div>
            <p className="text-sm font-medium truncate">{attachment.originalName}</p>
            <p className="text-xs">URL not available</p>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="group relative w-48 h-48 bg-white rounded-lg border border-gray-200 overflow-hidden flex flex-col">
      {isImage ? (
        <a href={url} target="_blank" rel="noopener noreferrer">
          <img
            src={url}
            alt={attachment.originalName ?? 'attachment'}
            className="flex-1 w-full object-cover"
          />
        </a>
      ) : (
        <a href={url} target="_blank" rel="noopener noreferrer" className="flex-1 w-full flex items-center justify-center bg-blue-50">
          <div className="text-center p-4">
            <div className="text-3xl mb-1">📄</div>
            <p className="text-sm font-medium text-blue-700 truncate">{attachment.originalName ?? 'attachment'}</p>
            <p className="text-xs text-blue-500">{(attachment.fileSizeBytes / 1024).toFixed(1)} KB</p>
          </div>
        </a>
      )}
      <div className="absolute inset-0 bg-black/50 flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity">
        <a
          href={url}
          target="_blank"
          rel="noopener noreferrer"
          className="px-3 py-1.5 bg-white text-sm rounded text-gray-900 hover:bg-gray-100"
        >
          {isImage ? 'Xem ảnh' : 'Tải về'}
        </a>
      </div>
    </div>
  );
}