import { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import apiClient from '../../../shared/api/apiClient';

interface Note { id: number; content: string; authorId: string; createdAt: string; }

const card = 'bg-surface rounded-2xl p-6 shadow-sm';

export default function SecurityIncidentInvestigationPage() {
  const { eventId } = useParams<{ eventId: string }>();
  const [notes, setNotes] = useState<Note[]>([]);
  const [newNote, setNewNote] = useState('');
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    (async () => {
      try {
        const [notesRes] = await Promise.all([
          apiClient.get(`/api/v1/admin/security-events/${eventId}/notes`),
        ]);
        setNotes(notesRes.data.data);
      } catch { /* */ } finally { setIsLoading(false); }
    })();
  }, [eventId]);

  const addNote = async () => {
    if (!newNote.trim()) return;
    try {
      const res = await apiClient.post(`/api/v1/admin/security-events/${eventId}/notes`, { content: newNote });
      setNotes(prev => [res.data.data, ...prev]);
      setNewNote('');
    } catch { /* */ }
  };

  return (
    <div>
      <div className="text-sm text-on-surface-variant mb-2">An toàn &gt; Quản lý sự cố &gt; CB-{eventId}</div>

      <div className="flex items-center gap-4 mb-6">
        <span className="material-symbols-outlined bg-error-container text-error rounded-full p-2 text-[32px]">warning</span>
        <div>
          <h1 className="text-[22px] font-bold text-on-surface m-0">Sự cố CB-{eventId}</h1>
          <p className="text-sm text-on-surface-variant mt-1 mb-0">Điều tra sự cố bảo mật</p>
        </div>
        <div className="ml-auto flex gap-3">
          <button className="px-5 py-2.5 rounded-full bg-primary text-on-primary border-none text-sm font-semibold cursor-pointer">Phân công</button>
          <button className="px-5 py-2.5 rounded-full bg-surface text-primary border border-outline-variant text-sm font-semibold cursor-pointer">Xuất báo cáo</button>
        </div>
      </div>

      <div className="grid grid-cols-[2fr_1fr] gap-6">
        <div>
          <div className={card}>
            <div className="flex justify-between items-center mb-4">
              <h2 className="text-lg font-semibold text-on-surface m-0">Lịch trình bằng chứng</h2>
              <button
                onClick={addNote}
                className="px-4 py-1.5 rounded-full border border-primary bg-surface text-primary text-[13px] font-semibold cursor-pointer"
              >+ Thêm bằng chứng</button>
            </div>

            {isLoading ? (
              <p className="text-outline">Đang tải...</p>
            ) : notes.length === 0 ? (
              <p className="text-outline">Chưa có ghi chú.</p>
            ) : (
              <div className="flex flex-col gap-4">
                {notes.map(n => (
                  <div key={n.id} className="border-l-[3px] border-primary-container pl-4 pb-2">
                    <div className="flex justify-between text-xs text-outline mb-1">
                      <span>{n.authorId}</span>
                      <span>{new Date(n.createdAt).toLocaleTimeString('vi-VN')}</span>
                    </div>
                    <p className="text-sm text-on-surface m-0">{n.content}</p>
                  </div>
                ))}
              </div>
            )}
          </div>

          <div className={`${card} mt-6`}>
            <h2 className="text-lg font-semibold text-on-surface mt-0 mb-4">Giả thuyết &amp; Ghi chú</h2>
            <textarea
              value={newNote}
              onChange={e => setNewNote(e.target.value)}
              rows={3}
              placeholder="Thêm ghi chú điều tra..."
              className="w-full px-4 py-4 rounded-xl border border-outline-variant text-sm resize-y"
            />
            <button
              onClick={addNote}
              className="mt-3 px-6 py-2.5 rounded-full bg-primary text-on-primary border-none text-sm font-semibold cursor-pointer"
            >
              Ghi chú mới
            </button>
          </div>
        </div>

        <div className="flex flex-col gap-6">
          <div className={card}>
            <h3 className="text-base font-semibold text-on-surface mt-0 mb-4">Trạng thái khoanh vùng</h3>
            {['Khóa tài khoản liên quan', 'Chặn IP nguồn', 'Đặt lại toàn bộ Session'].map((action, i) => (
              <div
                key={i}
                className={`flex justify-between items-center py-3 ${i < 2 ? 'border-b border-surface-container-highest' : ''}`}
              >
                <span className="text-sm text-on-surface">{action}</span>
                <span className={`px-3 py-1 rounded-full text-xs font-semibold ${i < 2 ? 'bg-primary text-on-primary' : 'bg-surface-container-low text-primary'}`}>
                  {i < 2 ? 'ĐÃ THỰC HIỆN' : 'CHƯA XỬ LÝ'}
                </span>
              </div>
            ))}
          </div>

          <div className={card}>
            <h3 className="text-base font-semibold text-on-surface mt-0 mb-4">Tài khoản bị ảnh hưởng</h3>
            <p className="text-sm text-outline">Dữ liệu sẽ hiển thị khi có correlationId.</p>
          </div>
        </div>
      </div>
    </div>
  );
}
