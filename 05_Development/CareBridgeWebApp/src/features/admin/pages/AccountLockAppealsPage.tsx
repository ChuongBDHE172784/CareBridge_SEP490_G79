import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import type { AccountLockAppeal, AccountLockAppealStatus } from '../models/adminUser';
import { getAccountLockAppeals } from '../services/adminUserApi';

export default function AccountLockAppealsPage() {
  const [status, setStatus] = useState<AccountLockAppealStatus>('PENDING');
  const [appeals, setAppeals] = useState<AccountLockAppeal[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    setLoading(true);
    getAccountLockAppeals(status)
      .then((result) => setAppeals(result.content))
      .catch(() => setError('Không thể tải danh sách khiếu nại.'))
      .finally(() => setLoading(false));
  }, [status]);

  return <div className="p-6 md:p-8 font-sans">
    <div className="mb-6">
      <h1 className="m-0 text-2xl font-bold text-on-surface">Khiếu nại khóa tài khoản</h1>
      <p className="mt-1 text-sm text-on-surface-variant">Xem xét yêu cầu mở khóa của tài khoản bị System Admin khóa thủ công.</p>
    </div>
    <div className="mb-5 flex gap-2">
      {(['PENDING', 'APPROVED', 'REJECTED', 'CANCELLED'] as const).map((value) =>
        <button key={value} onClick={() => setStatus(value)} className={`rounded-full px-4 py-2 text-sm font-semibold ${status === value ? 'bg-primary text-on-primary' : 'bg-surface-container-low text-on-surface-variant'}`}>
          {value === 'PENDING' ? 'Chờ xử lý' : value === 'APPROVED' ? 'Đã duyệt' : value === 'REJECTED' ? 'Từ chối' : 'Đã đóng'}
        </button>)}
    </div>
    {error && <p className="text-error">{error}</p>}
    <div className="overflow-hidden rounded-2xl border border-surface-container-highest bg-surface shadow-sm">
      {loading ? <p className="p-8 text-center text-outline">Đang tải...</p> : appeals.length === 0 ? <p className="p-8 text-center text-outline">Không có khiếu nại ở trạng thái này.</p> :
        <table className="w-full border-collapse text-left text-sm"><thead className="bg-surface-container-low text-xs uppercase text-outline"><tr><th className="p-4">Tài khoản</th><th className="p-4">Nội dung</th><th className="p-4">Gửi lúc</th><th className="p-4">Thao tác</th></tr></thead>
          <tbody>{appeals.map((appeal) => <tr key={appeal.id} className="border-t border-surface-container-highest"><td className="p-4"><strong>{appeal.userName ?? 'Người dùng'}</strong><div className="text-xs text-outline">{appeal.userEmail}</div></td><td className="max-w-md p-4 text-on-surface-variant">{appeal.reason}</td><td className="p-4 text-on-surface-variant">{new Date(appeal.submittedAt).toLocaleString('vi-VN')}</td><td className="p-4"><Link to={`/admin/account-lock-appeals/${appeal.id}`} className="font-semibold text-primary no-underline">Xem xét</Link></td></tr>)}</tbody>
        </table>}
    </div>
  </div>;
}
