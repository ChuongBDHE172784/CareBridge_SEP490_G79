import { useState } from 'react';

// MOCK — no GET endpoint lists content in PENDING_REVIEW status. POST /admin/content/{id}/decision
// exists and is real, but with no real id source here the action buttons stay disabled to avoid
// firing decisions against fabricated ids (which would just 404).
interface QueueCard {
  id: string;
  kind: 'Bài viết' | 'FAQ' | 'Checklist';
  icon: string;
  title: string;
  excerpt: string;
  reviewer: string;
  reviewerRole: string;
  timeAgo: string;
  note: string | null;
  noteTone: 'ok' | 'warn';
}

const MOCK_QUEUE: QueueCard[] = [
  {
    id: 'mock-1',
    kind: 'Bài viết',
    icon: 'article',
    title: 'Hướng dẫn chi tiết về dinh dưỡng cho bé 6 tháng tuổi',
    excerpt: 'Bài viết cung cấp thông tin toàn diện về các loại thực phẩm phù hợp, cách chế biến và lịch ăn dặm khoa học cho trẻ bắt đầu làm quen với thức ăn đặc.',
    reviewer: 'BS. Nguyễn Trần Bảo',
    reviewerRole: 'Chuyên gia Dinh dưỡng',
    timeAgo: '2 giờ trước',
    note: 'Đầy đủ nguồn',
    noteTone: 'ok',
  },
  {
    id: 'mock-2',
    kind: 'FAQ',
    icon: 'quiz',
    title: 'Làm sao để biết bé sơ sinh đã bú đủ no?',
    excerpt: '"Các dấu hiệu nhận biết trẻ đã bú đủ sữa mẹ bao gồm: bé tự nhả bầu vú, tay chân thả lỏng, giấc ngủ sâu kéo dài 2-3 tiếng, và tã..."',
    reviewer: 'Điều dưỡng Lê Mai',
    reviewerRole: '',
    timeAgo: '4 giờ trước',
    note: 'Cần xem lại trích dẫn',
    noteTone: 'warn',
  },
  {
    id: 'mock-3',
    kind: 'Checklist',
    icon: 'checklist',
    title: 'Danh sách đồ đi sinh chuẩn bị đón bé yêu',
    excerpt: 'Giấy tờ tùy thân & Hồ sơ thai kỳ, Quần áo cho mẹ (3-4 bộ), Đồ dùng cho bé (Tã, khăn, quần áo)… và 15 mục khác',
    reviewer: '',
    reviewerRole: '',
    timeAgo: 'Hôm qua',
    note: 'Hoàn thiện 100%',
    noteTone: 'ok',
  },
];

export default function ContentApprovalQueuePage() {
  const [decided, setDecided] = useState<Set<string>>(new Set());

  return (
    <div className="p-8 font-sans">
      <div className="bg-error-container rounded-2xl p-4 mb-5 text-error text-sm flex items-center gap-2">
        <span className="material-symbols-outlined text-lg">info</span>
        Dữ liệu mẫu (MOCK) — backend chưa có endpoint liệt kê nội dung đang chờ duyệt (PENDING_REVIEW). Nút
        Duyệt/Từ chối bị khoá vì id mẫu không tồn tại thật trong hệ thống.
      </div>

      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-2xl font-bold text-on-surface m-0">Hàng đợi phê duyệt</h1>
          <p className="text-sm text-outline mt-1">Danh sách nội dung đang chờ kiểm duyệt cuối cùng.</p>
        </div>
        <div className="flex gap-2.5">
          <button disabled className="flex items-center gap-1.5 py-2.5 px-5 rounded-full border border-outline-variant bg-transparent text-on-surface-variant text-sm font-semibold opacity-50 cursor-not-allowed">
            <span className="material-symbols-outlined text-lg">filter_list</span>
            Lọc
          </button>
          <button disabled className="flex items-center gap-1.5 py-2.5 px-5 rounded-full border border-outline-variant bg-transparent text-on-surface-variant text-sm font-semibold opacity-50 cursor-not-allowed">
            <span className="material-symbols-outlined text-lg">sort</span>
            Sắp xếp
          </button>
        </div>
      </div>

      <div className="grid grid-cols-3 gap-5">
        {MOCK_QUEUE.map((card) => {
          const isDecided = decided.has(card.id);
          return (
            <div key={card.id} className="bg-surface rounded-2xl p-5 shadow-md flex flex-col">
              <div className="flex items-center justify-between mb-3">
                <span className="inline-flex items-center gap-1 py-1 px-3 rounded-full bg-surface-container-low text-primary text-xs font-semibold">
                  <span className="material-symbols-outlined text-sm">{card.icon}</span>
                  {card.kind}
                </span>
                <span className="text-xs text-outline">{card.timeAgo}</span>
              </div>

              <h3 className="text-base font-bold text-on-surface mb-2 leading-snug">{card.title}</h3>
              <p className="text-sm text-on-surface-variant flex-1 line-clamp-4 mb-4">{card.excerpt}</p>

              {card.reviewer && (
                <div className="flex items-center gap-2.5 mb-4">
                  <div className="w-8 h-8 rounded-full bg-primary-container flex items-center justify-center text-on-primary-container text-xs font-bold">
                    {card.reviewer.charAt(0)}
                  </div>
                  <div>
                    <div className="text-sm font-semibold text-on-surface">{card.reviewer}</div>
                    {card.reviewerRole && <div className="text-xs text-outline">{card.reviewerRole}</div>}
                  </div>
                </div>
              )}

              <div className="flex items-center justify-between pt-3 border-t border-surface-container-highest">
                <span className={`flex items-center gap-1 text-xs font-medium ${card.noteTone === 'ok' ? 'text-primary' : 'text-error'}`}>
                  <span className="material-symbols-outlined text-sm">{card.noteTone === 'ok' ? 'check_circle' : 'warning'}</span>
                  {card.note}
                </span>
                <div className="flex gap-1.5">
                  <button
                    disabled
                    title="Backend chưa có id thật cho mục này (dữ liệu mẫu)"
                    onClick={() => setDecided((prev) => new Set(prev).add(card.id))}
                    className="w-9 h-9 rounded-full bg-error-container text-error flex items-center justify-center opacity-50 cursor-not-allowed"
                  >
                    <span className="material-symbols-outlined text-lg">close</span>
                  </button>
                  <button
                    disabled
                    title="Backend chưa có id thật cho mục này (dữ liệu mẫu)"
                    className="w-9 h-9 rounded-full bg-primary text-on-primary flex items-center justify-center opacity-50 cursor-not-allowed"
                  >
                    <span className="material-symbols-outlined text-lg">check</span>
                  </button>
                </div>
              </div>
              {isDecided && <p className="text-xs text-outline mt-2">Đã xử lý (mô phỏng).</p>}
            </div>
          );
        })}
      </div>
    </div>
  );
}
