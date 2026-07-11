import { useState, useEffect } from 'react';
import { createAvailability, getMyAvailability, deleteAvailability } from '../services/expertApi';

export default function AvailabilityCalendarPage() {
  const [slots, setSlots] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const [form, setForm] = useState({
    startAt: '',
    endAt: '',
    channelType: 'ONLINE_CHAT',
    status: 'AVAILABLE',
  });

  const load = async () => {
    try {
      const data = await getMyAvailability();
      setSlots(data);
    } catch (e: any) {
      setError(e.response?.data?.message ?? 'Không thể tải lịch rảnh');
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
      await createAvailability(form);
      setShowForm(false);
      setForm({ startAt: '', endAt: '', channelType: 'ONLINE_CHAT', status: 'AVAILABLE' });
      await load();
    } catch (e: any) {
      setError(e.response?.data?.message ?? 'Lưu thất bại');
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = async (id: string) => {
    if (!confirm('Xóa khung giờ này?')) return;
    try {
      await deleteAvailability(id);
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

  const channelLabel: Record<string, string> = {
    ONLINE_CHAT: '💬 Chat',
    VIDEO_CALL: '📹 Video call',
    VOICE_CALL: '📞 Gọi thoại',
    HOME_VISIT: '🏠 Tận nhà',
  };

  const statusLabel: Record<string, string> = {
    AVAILABLE: '🟢 Sẵn sàng',
    BUSY: '🟡 Bận',
    OFFLINE: '⚫ Offline',
    EXPIRED: '🔴 Đã hết hạn',
  };

  return (
    <div className="max-w-3xl mx-auto p-6">
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold text-on-surface">Lịch rảnh</h1>
        <button
          onClick={() => setShowForm(!showForm)}
          className="px-4 py-2 rounded bg-primary text-white text-sm font-medium hover:bg-primary/90"
        >
          Thêm khung giờ
        </button>
      </div>

      {error && (
        <div className="mb-4 p-3 rounded bg-red-50 border border-red-200 text-red-700 text-sm">{error}</div>
      )}

      {showForm && (
        <form onSubmit={onSubmit} className="mb-6 p-5 bg-white rounded-lg border border-gray-200 shadow-sm space-y-4">
          <h3 className="font-medium text-gray-800">Thêm khung giờ rảnh</h3>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700">Bắt đầu *</label>
              <input type="datetime-local" required
                className="mt-1 block w-full rounded border border-gray-300 px-3 py-2"
                value={form.startAt}
                onChange={(e) => setForm({ ...form, startAt: e.target.value })}
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700">Kết thúc *</label>
              <input type="datetime-local" required
                className="mt-1 block w-full rounded border border-gray-300 px-3 py-2"
                value={form.endAt}
                onChange={(e) => setForm({ ...form, endAt: e.target.value })}
              />
            </div>
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700">Kênh</label>
              <select className="mt-1 block w-full rounded border border-gray-300 px-3 py-2"
                value={form.channelType} onChange={(e) => setForm({ ...form, channelType: e.target.value })}>
                {Object.entries(channelLabel).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
              </select>
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700">Trạng thái</label>
              <select className="mt-1 block w-full rounded border border-gray-300 px-3 py-2"
                value={form.status} onChange={(e) => setForm({ ...form, status: e.target.value })}>
                {Object.entries(statusLabel).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
              </select>
            </div>
          </div>
          <div className="flex justify-end gap-3 pt-2">
            <button type="button" onClick={() => setShowForm(false)}
              className="px-4 py-2 rounded border border-gray-300 text-gray-700 hover:bg-gray-50">
              Hủy
            </button>
            <button type="submit" disabled={submitting}
              className="px-4 py-2 rounded bg-primary text-white font-medium disabled:opacity-50">
              {submitting ? 'Đang lưu...' : 'Thêm'}
            </button>
          </div>
        </form>
      )}

      <div className="space-y-3">
        {slots.length === 0 && (
          <div className="p-8 text-center text-gray-500 bg-white rounded-lg border border-gray-200">
            Chưa có khung giờ nào. Nhấn "Thêm khung giờ" để bắt đầu.
          </div>
        )}

        {slots
          .sort((a, b) => new Date(a.startAt).getTime() - new Date(b.startAt).getTime())
          .map((slot) => {
            const start = new Date(slot.startAt);
            const end = new Date(slot.endAt);
            const isExpired = end < new Date();
            return (
              <div key={slot.availabilityId}
                className={`bg-white rounded-lg border shadow-sm p-5 ${isExpired ? 'opacity-60' : ''}`}>
                <div className="flex items-center justify-between">
                  <div>
                    <div className="flex items-center gap-3 mb-1">
                      <span className="text-lg font-semibold text-gray-900">
                        {start.toLocaleDateString('vi-VN', { weekday: 'short', day: 'numeric', month: 'short' })}
                      </span>
                      <span className={`px-2 py-0.5 rounded text-xs font-medium ${statusLabel[slot.status] ? 'bg-gray-100' : 'bg-gray-100'}`}>
                        {statusLabel[slot.status] || slot.status}
                      </span>
                      {isExpired && <span className="text-xs text-red-500">Đã hết hạn</span>}
                    </div>
                    <p className="text-sm text-gray-600">
                      {start.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })}
                      {' → '}
                      {end.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })}
                    </p>
                    <p className="text-sm text-gray-500 mt-0.5">
                      {channelLabel[slot.channelType] || slot.channelType}
                    </p>
                  </div>
                  <button
                    onClick={() => handleDelete(slot.availabilityId)}
                    className="text-sm text-red-600 hover:text-red-800 px-3 py-1 rounded hover:bg-red-50">
                    Xóa
                  </button>
                </div>
              </div>
            );
          })}
      </div>
    </div>
  );
}
