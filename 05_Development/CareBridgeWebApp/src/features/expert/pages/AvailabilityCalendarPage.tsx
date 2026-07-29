import { useState, useEffect } from 'react';
import { createAvailability, getMyAvailability, deleteAvailability } from '../services/expertApi';

const TIME_PRESETS = [
  { label: '🌅 Ca Sáng', start: '08:00', end: '11:30', desc: '08:00 - 11:30' },
  { label: '☀️ Ca Chiều', start: '13:30', end: '17:00', desc: '13:30 - 17:00' },
  { label: '🌙 Ca Tối', start: '18:00', end: '20:30', desc: '18:00 - 20:30' },
];

function translateError(err: any): string {
  const rawMsg = err.response?.data?.message || err.response?.data?.error || err.message || '';
  if (rawMsg.includes('not verified')) {
    return 'Hồ sơ chuyên gia của bạn chưa được xét duyệt (Cần có trạng thái ĐÃ DUYỆT để thiết lập lịch rảnh).';
  }
  if (rawMsg.includes('not found')) {
    return 'Tài khoản chưa tạo Hồ sơ chuyên môn. Vui lòng hoàn tất Hồ sơ chuyên môn trước.';
  }
  if (rawMsg.includes('overlap')) {
    return 'Khung giờ rảnh này trùng lặp với một khung giờ bạn đã tạo trước đó!';
  }
  if (rawMsg.includes('past')) {
    return 'Thời gian bắt đầu không được ở trong quá khứ!';
  }
  if (rawMsg.includes('endAt must be after')) {
    return 'Thời gian kết thúc phải diễn ra sau thời gian bắt đầu!';
  }
  return rawMsg || 'Không thể thực hiện thao tác. Vui lòng kiểm tra lại.';
}

function parseToIso(dateStr: string, timeStr: string): string {
  if (!dateStr || !timeStr) throw new Error('Vui lòng chọn đầy đủ ngày và giờ');

  const [yearStr, monthStr, dayStr] = dateStr.split('-');
  if (!yearStr || !monthStr || !dayStr) throw new Error('Ngày không hợp lệ');

  const parts = timeStr.trim().split(':');
  if (parts.length < 2) throw new Error('Giờ không hợp lệ');

  let hours = parseInt(parts[0], 10);
  let minutes = parseInt(parts[1], 10);

  const isPM = timeStr.toUpperCase().includes('PM');
  const isAM = timeStr.toUpperCase().includes('AM');

  if (isPM && hours < 12) hours += 12;
  if (isAM && hours === 12) hours = 0;

  const d = new Date(parseInt(yearStr, 10), parseInt(monthStr, 10) - 1, parseInt(dayStr, 10), hours, minutes, 0);
  if (isNaN(d.getTime())) {
    throw new Error('Thời gian không hợp lệ');
  }
  return d.toISOString();
}

export default function AvailabilityCalendarPage() {
  const [slots, setSlots] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Form State
  const [selectedDate, setSelectedDate] = useState<string>(
    new Date().toISOString().split('T')[0]
  );
  const [startTime, setStartTime] = useState<string>('08:00');
  const [endTime, setEndTime] = useState<string>('11:30');
  const [activePreset, setActivePreset] = useState<string | null>('🌅 Ca Sáng');

  const load = async () => {
    try {
      setError(null);
      const data = await getMyAvailability();
      setSlots(data ?? []);
    } catch (e: any) {
      setError(translateError(e));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); }, []);

  const handleApplyPreset = (preset: typeof TIME_PRESETS[0]) => {
    setStartTime(preset.start);
    setEndTime(preset.end);
    setActivePreset(preset.label);
  };

  const onSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setSubmitting(true);
    setError(null);
    try {
      const startAt = parseToIso(selectedDate, startTime);
      const endAt = parseToIso(selectedDate, endTime);

      if (new Date(endAt).getTime() <= new Date(startAt).getTime()) {
        setError('Thời gian kết thúc phải sau thời gian bắt đầu!');
        setSubmitting(false);
        return;
      }

      await createAvailability({
        startAt,
        endAt,
        channelType: 'ONLINE_CHAT',
        status: 'AVAILABLE',
      });

      setShowForm(false);
      await load();
    } catch (e: any) {
      setError(translateError(e));
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = async (id: string) => {
    if (!confirm('Xóa khung giờ rảnh này?')) return;
    try {
      await deleteAvailability(id);
      await load();
    } catch (e: any) {
      alert(translateError(e));
    }
  };

  const getDateOffset = (offsetDays: number) => {
    const d = new Date();
    d.setDate(d.getDate() + offsetDays);
    return d.toISOString().split('T')[0];
  };

  const quickDates = [
    { label: 'Hôm nay', val: getDateOffset(0) },
    { label: 'Ngày mai', val: getDateOffset(1) },
    { label: 'Ngày kia', val: getDateOffset(2) },
  ];

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-[400px]">
        <div className="animate-spin rounded-full h-8 w-8 border-2 border-primary border-t-transparent" />
      </div>
    );
  }

  return (
    <div className="max-w-3xl mx-auto p-6">
      {/* Header */}
      <div className="flex items-center justify-between mb-8">
        <div>
          <h1 className="text-2xl font-bold text-gray-900 tracking-tight">Lịch rảnh làm việc</h1>
          <p className="text-sm text-gray-500 mt-1">Cài đặt khoảng thời gian rảnh để mẹ bầu chủ động đặt lịch tư vấn</p>
        </div>
        <button
          onClick={() => {
            setError(null);
            setShowForm(!showForm);
          }}
          className="inline-flex items-center gap-2 px-5 py-2.5 rounded-xl bg-primary text-white text-sm font-semibold shadow-sm hover:brightness-110 active:scale-95 transition-all"
        >
          {showForm ? '✖ Đóng' : '➕ Thêm khung giờ rảnh'}
        </button>
      </div>

      {error && (
        <div className="mb-6 p-4 rounded-xl bg-red-50 border border-red-200 text-red-700 text-sm flex items-center justify-between shadow-sm">
          <span className="font-medium">⚠️ {error}</span>
          <button onClick={() => setError(null)} className="text-red-600 font-bold hover:underline ml-4 text-xs">Đóng</button>
        </div>
      )}

      {/* Modern Add Slot Form */}
      {showForm && (
        <form onSubmit={onSubmit} className="mb-8 p-6 bg-white rounded-2xl border border-gray-200 shadow-md space-y-6 animate-fadeIn">
          <div className="flex items-center justify-between pb-3 border-b border-gray-100">
            <h3 className="font-bold text-gray-900 text-lg flex items-center gap-2">
              <span>📅</span> Thiết lập khung giờ rảnh mới
            </h3>
            <span className="text-xs px-3 py-1 bg-green-50 text-green-700 font-semibold rounded-full border border-green-200">
              🟢 Trạng thái: Sẵn sàng
            </span>
          </div>

          {/* 1. Pick Date */}
          <div className="space-y-2">
            <label className="block text-sm font-semibold text-gray-800">1. Chọn ngày rảnh</label>
            <div className="flex flex-wrap items-center gap-2 mb-2">
              {quickDates.map((qd) => (
                <button
                  key={qd.val}
                  type="button"
                  onClick={() => setSelectedDate(qd.val)}
                  className={`px-3.5 py-1.5 rounded-lg text-xs font-semibold border transition-all ${
                    selectedDate === qd.val
                      ? 'bg-primary text-white border-primary shadow-sm'
                      : 'bg-gray-50 text-gray-700 border-gray-200 hover:bg-gray-100'
                  }`}
                >
                  {qd.label} ({new Date(qd.val).toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit' })})
                </button>
              ))}
            </div>
            <input
              type="date"
              required
              value={selectedDate}
              min={getDateOffset(0)}
              onChange={(e) => setSelectedDate(e.target.value)}
              className="w-full sm:w-auto px-4 py-2.5 rounded-xl border border-gray-300 text-sm font-medium text-gray-800 focus:ring-2 focus:ring-primary/20 focus:border-primary outline-none transition-all"
            />
          </div>

          {/* 2. Pick Time Presets */}
          <div className="space-y-2">
            <label className="block text-sm font-semibold text-gray-800">2. Chọn ca rảnh nhanh</label>
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
              {TIME_PRESETS.map((preset) => (
                <button
                  key={preset.label}
                  type="button"
                  onClick={() => handleApplyPreset(preset)}
                  className={`p-3.5 rounded-xl border text-left transition-all ${
                    activePreset === preset.label
                      ? 'bg-primary/5 border-primary ring-2 ring-primary/20'
                      : 'bg-white border-gray-200 hover:border-gray-300 hover:bg-gray-50'
                  }`}
                >
                  <div className="font-bold text-sm text-gray-900">{preset.label}</div>
                  <div className="text-xs text-gray-500 mt-1 font-medium">{preset.desc}</div>
                </button>
              ))}
            </div>
          </div>

          {/* 3. Custom Time Inputs */}
          <div className="space-y-2 pt-2">
            <label className="block text-sm font-semibold text-gray-800">hoặc tùy chỉnh khung giờ:</label>
            <div className="grid grid-cols-2 gap-4 bg-gray-50 p-4 rounded-xl border border-gray-200/80">
              <div>
                <span className="block text-xs font-medium text-gray-500 mb-1">Giờ bắt đầu</span>
                <input
                  type="time"
                  required
                  value={startTime}
                  onChange={(e) => {
                    setStartTime(e.target.value);
                    setActivePreset(null);
                  }}
                  className="w-full px-3 py-2 bg-white rounded-lg border border-gray-300 text-sm font-bold text-gray-800 outline-none focus:border-primary"
                />
              </div>
              <div>
                <span className="block text-xs font-medium text-gray-500 mb-1">Giờ kết thúc</span>
                <input
                  type="time"
                  required
                  value={endTime}
                  onChange={(e) => {
                    setEndTime(e.target.value);
                    setActivePreset(null);
                  }}
                  className="w-full px-3 py-2 bg-white rounded-lg border border-gray-300 text-sm font-bold text-gray-800 outline-none focus:border-primary"
                />
              </div>
            </div>
          </div>

          {/* Action buttons */}
          <div className="flex items-center justify-end gap-3 pt-4 border-t border-gray-100">
            <button
              type="button"
              onClick={() => setShowForm(false)}
              className="px-5 py-2.5 rounded-xl border border-gray-300 text-gray-700 text-sm font-semibold hover:bg-gray-50 transition-colors"
            >
              Hủy
            </button>
            <button
              type="submit"
              disabled={submitting}
              className="px-6 py-2.5 rounded-xl bg-primary text-white text-sm font-bold shadow-md hover:brightness-110 disabled:opacity-50 transition-all"
            >
              {submitting ? 'Đang tạo...' : 'Lưu khung giờ rảnh'}
            </button>
          </div>
        </form>
      )}

      {/* Slots List */}
      <div className="space-y-3">
        {slots.length === 0 && !showForm && (
          <div className="p-12 text-center bg-white rounded-2xl border border-gray-200 shadow-sm">
            <div className="w-16 h-16 bg-primary/10 rounded-full flex items-center justify-center mx-auto text-2xl mb-3">
              📆
            </div>
            <h3 className="font-bold text-gray-900 text-base">Chưa có lịch rảnh nào</h3>
            <p className="text-xs text-gray-500 mt-1 max-w-sm mx-auto">
              Hãy nhấn nút "Thêm khung giờ rảnh" phía trên để thiết lập lịch làm việc của bạn.
            </p>
          </div>
        )}

        {slots
          .sort((a, b) => new Date(a.startAt).getTime() - new Date(b.startAt).getTime())
          .map((slot) => {
            const start = new Date(slot.startAt);
            const end = new Date(slot.endAt);
            const isExpired = end < new Date();
            return (
              <div
                key={slot.availabilityId}
                className={`bg-white rounded-2xl border border-gray-200/80 shadow-sm p-5 flex items-center justify-between hover:shadow-md transition-all ${
                  isExpired ? 'opacity-50 bg-gray-50' : ''
                }`}
              >
                <div className="flex items-center gap-4">
                  <div className="w-12 h-12 rounded-xl bg-primary/10 text-primary flex items-center justify-center font-bold text-lg">
                    📅
                  </div>
                  <div>
                    <div className="flex items-center gap-2">
                      <span className="font-bold text-gray-900 text-base">
                        {start.toLocaleDateString('vi-VN', {
                          weekday: 'long',
                          day: '2-digit',
                          month: '2-digit',
                          year: 'numeric',
                        })}
                      </span>
                      <span className="px-2.5 py-0.5 rounded-full text-xs font-semibold bg-green-100 text-green-700">
                        🟢 Sẵn sàng
                      </span>
                      {isExpired && <span className="text-xs text-red-500 font-semibold">(Đã hết hạn)</span>}
                    </div>
                    <div className="text-sm font-semibold text-primary mt-1">
                      ⏰ {start.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })}
                      {' → '}
                      {end.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })}
                    </div>
                  </div>
                </div>

                <button
                  onClick={() => handleDelete(slot.availabilityId)}
                  className="px-3.5 py-2 rounded-xl text-xs font-semibold text-red-600 bg-red-50 hover:bg-red-100 transition-colors"
                >
                  🗑️ Xóa
                </button>
              </div>
            );
          })}
      </div>
    </div>
  );
}
