import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { listMyContributions, checkContributionEligibility, deleteContribution, type PaginatedContributionResponse, type ContributionResponse, type ContributionStatus } from '../services/expertApi';

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

export default function ContributionListPage() {
  const [contributions, setContributions] = useState<ContributionResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [eligible, setEligible] = useState(false);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const size = 10;

  useEffect(() => {
    loadEligibility();
    loadContributions();
  }, [page]);

  async function loadEligibility() {
    try {
      const e = await checkContributionEligibility();
      setEligible(e);
    } catch {
      setEligible(false);
    }
  }

  async function loadContributions() {
    setLoading(true);
    try {
      const res: PaginatedContributionResponse = await listMyContributions({ page, size });
      setContributions(res.content);
      setTotalPages(res.totalPages);
      setTotalElements(res.totalElements);
    } catch (err) {
      console.error('Failed to load contributions', err);
    } finally {
      setLoading(false);
    }
  }

  async function handleDelete(id: string) {
    if (!window.confirm('Xóa bản nháp này? Không thể hoàn tác.')) return;
    try {
      await deleteContribution(id);
      loadContributions();
    } catch (err) {
      alert('Xóa thất bại');
    }
  }

  if (loading) return <div className="flex justify-center py-12">Đang tải...</div>;

  return (
    <div className="max-w-6xl mx-auto p-6">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 mb-6">
        <div>
          <h1 className="text-2xl font-bold text-on-surface">Đóng góp y khoa của tôi</h1>
          <p className="text-gray-500 mt-1">Quản lý bài viết, tài liệu y khoa cá nhân</p>
        </div>
        {eligible && (
          <Link
            to="/expert/contributions/new"
            className="px-5 py-2.5 rounded bg-primary text-white font-medium hover:bg-primary/90 transition"
          >
            + Tạo bài viết mới
          </Link>
        )}
        {!eligible && (
          <div className="px-4 py-2 rounded bg-yellow-50 text-yellow-700 border border-yellow-200 text-sm">
            Bạn chưa đủ điều kiện tạo bài viết (cần xác minh APPROVED và trust ACTIVE).
          </div>
        )}
      </div>

      {totalElements === 0 ? (
        <div className="bg-white rounded-lg border border-gray-200 p-12 text-center">
          <div className="text-4xl mb-3">📝</div>
          <h3 className="text-lg font-semibold text-gray-900 mb-2">Chưa có bài viết nào</h3>
          <p className="text-gray-500 mb-6">
            {eligible
              ? 'Bắt đầu chia sẻ kiến thức y khoa bằng cách tạo bài viết đầu tiên.'
              : 'Hoàn tất quy trình xác minh chuyên gia để có thể đóng góp bài viết.'}
          </p>
          {eligible && (
            <Link
              to="/expert/contributions/new"
              className="inline-flex items-center gap-2 px-5 py-2.5 rounded bg-primary text-white font-medium hover:bg-primary/90"
            >
              + Tạo bài viết đầu tiên
            </Link>
          )}
        </div>
      ) : (
        <>
          <div className="bg-white rounded-lg border border-gray-200 overflow-hidden">
            <table className="w-full">
              <thead className="bg-gray-50">
                <tr>
                  <th className="px-4 py-3 text-left text-xs font-semibold text-gray-500 uppercase tracking-wider">Tiêu đề</th>
                  <th className="px-4 py-3 text-left text-xs font-semibold text-gray-500 uppercase tracking-wider">Trạng thái</th>
                  <th className="px-4 py-3 text-left text-xs font-semibold text-gray-500 uppercase tracking-wider">Chuyên khoa</th>
                  <th className="px-4 py-3 text-left text-xs font-semibold text-gray-500 uppercase tracking-wider">Bệnh viện</th>
                  <th className="px-4 py-3 text-left text-xs font-semibold text-gray-500 uppercase tracking-wider">Cập nhật</th>
                  <th className="px-4 py-3 text-right text-xs font-semibold text-gray-500 uppercase tracking-wider">Hành động</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {contributions.map((c) => (
                  <tr key={c.id} className="hover:bg-gray-50">
                    <td className="px-4 py-4">
                      <Link to={`/expert/contributions/${c.id}`} className="font-medium text-gray-900 hover:text-primary">
                        {c.title}
                      </Link>
                      <p className="text-sm text-gray-500 truncate max-w-xs mt-1">{c.content.substring(0, 80)}...</p>
                    </td>
                    <td className="px-4 py-4">
                      <span className={`inline-flex px-2 py-1 text-xs font-medium rounded-full border ${STATUS_CLASSES[c.status] || ''}`}>
                        {STATUS_LABELS[c.status] || c.status}
                      </span>
                      {c.status === 'REJECTED' && c.rejectionReason && (
                        <p className="text-xs text-red-600 mt-1 truncate max-w-xs" title={c.rejectionReason}>
                          Lý do: {c.rejectionReason}
                        </p>
                      )}
                    </td>
                    <td className="px-4 py-4 text-gray-600 text-sm">{c.specialtyId || '—'}</td>
                    <td className="px-4 py-4 text-gray-600 text-sm">{c.hospitalId || '—'}</td>
                    <td className="px-4 py-4 text-gray-500 text-sm">
                      {new Date(c.updatedAt).toLocaleString('vi-VN')}
                    </td>
                    <td className="px-4 py-4 text-right">
                      <div className="flex items-center justify-end gap-2">
                        <Link
                          to={`/expert/contributions/${c.id}`}
                          className="px-3 py-1.5 text-sm rounded border border-gray-300 text-gray-700 hover:bg-gray-50"
                        >
                          {c.status === 'DRAFT' ? 'Chỉnh sửa' : 'Xem'}
                        </Link>
                        {c.status === 'DRAFT' && (
                          <button
                            onClick={() => handleDelete(c.id)}
                            className="px-3 py-1.5 text-sm rounded border border-red-300 text-red-600 hover:bg-red-50"
                          >
                            Xóa
                          </button>
                        )}
                        {c.status === 'DRAFT' && (
                          <Link
                            to={`/expert/contributions/${c.id}?mode=submit`}
                            className="px-3 py-1.5 text-sm rounded bg-primary text-white hover:bg-primary/90"
                          >
                            Gửi duyệt
                          </Link>
                        )}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {totalPages > 1 && (
            <div className="flex items-center justify-center gap-2 mt-6">
              <button
                onClick={() => setPage(p => Math.max(0, p - 1))}
                disabled={page === 0}
                className="px-4 py-2 rounded border border-gray-300 disabled:opacity-50"
              >
                Trước
              </button>
              <span className="text-sm text-gray-600">
                Trang {page + 1} / {totalPages} · Tổng: {totalElements}
              </span>
              <button
                onClick={() => setPage(p => Math.min(totalPages - 1, p + 1))}
                disabled={page >= totalPages - 1}
                className="px-4 py-2 rounded border border-gray-300 disabled:opacity-50"
              >
                Tiếp
              </button>
            </div>
          )}
        </>
      )}
    </div>
  );
}