import { useState, useEffect } from 'react';
import { createAvailability, getMyAvailability, deleteAvailability } from '../services/expertApi';

const TIME_PRESETS = [
  { label: 'Ca Sáng', icon: 'wb_twilight', start: '08:00', end: '11:30', desc: '08:00 - 11:30' },
  { label: 'Ca Chiều', icon: 'wb_sunny', start: '13:30', end: '17:00', desc: '13:30 - 17:00' },
  { label: 'Ca Tối', icon: 'dark_mode', start: '18:00', end: '20:30', desc: '18:00 - 20:30' },
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
  const [activePreset, setActivePreset] = useState<string | null>('Ca Sáng');

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

  useEffect(() => {
    load();
  }, []);

  const handleApplyPreset = (preset: (typeof TIME_PRESETS)[0]) => {
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
        setError('Thời gian kết thúc phải diễn ra sau thời gian bắt đầu!');
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
    if (!window.confirm('Xóa khung giờ rảnh này?')) return;
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
    return <div className="py-12 text-center text-outline">Đang tải lịch rảnh làm việc...</div>;
  }

  return (
    <div className="p-8 font-sans">
      {/* Header */}
      <div className="flex justify-between items-start mb-6">
        <div>
          <h1 className="text-[26px] font-bold text-on-surface m-0">Lịch rảnh làm việc</h1>
          <p className="text-on-surface-variant text-sm mt-1">
            Cài đặt các khoảng thời gian rảnh để mẹ bầu chủ động chọn và đặt lịch tư vấn trực tuyến
          </p>
        </div>
        <button
          onClick={() => {
            setError(null);
            setShowForm(!showForm);
          }}
          className="flex items-center gap-2 py-3 px-6 rounded-full bg-primary text-on-primary text-sm font-semibold cursor-pointer whitespace-nowrap hover:brightness-110"
        >
          <span className="material-symbols-outlined text-lg">{showForm ? 'close' : 'add'}</span>
          {showForm ? 'Đóng khung tạo' : 'Thêm khung giờ rảnh'}
        </button>
      </div>

      {error && (
        <div className="bg-error-container rounded-2xl p-4 mb-4 text-error text-sm flex items-center justify-between">
          <span>⚠️ {error}</span>
          <button onClick={() => setError(null)} className="text-error font-bold text-xs underline cursor-pointer">
            Đóng
          </button>
        </div>
      )}

      {/* Add Slot Form Card */}
      {showForm && (
        <form onSubmit={onSubmit} className="bg-surface rounded-2xl p-6 shadow-md mb-8 space-y-5">
          <div className="flex items-center justify-between pb-3 border-b border-surface-container-highest">
            <h3 className="font-bold text-base text-on-surface flex items-center gap-2">
              <span className="material-symbols-outlined text-primary text-xl">event_available</span>
              Thiết lập khung giờ rảnh mới
            </h3>
            <span className="py-1 px-3.5 rounded-full text-xs font-semibold bg-[#E6F4EA] text-[#137333]">
              Sẵn sàng nhận lịch
            </span>
          </div>

          {/* 1. Pick Date */}
          <div className="space-y-2">
            <label className="block text-xs font-semibold text-outline uppercase tracking-wider">
              1. Chọn ngày rảnh
            </label>
            <div className="flex flex-wrap items-center gap-2 mb-2">
              {quickDates.map((qd) => (
                <button
                  key={qd.val}
                  type="button"
                  onClick={() => setSelectedDate(qd.val)}
                  className={`py-1.5 px-4 rounded-full text-xs font-semibold border cursor-pointer ${
                    selectedDate === qd.val
                      ? 'border-2 border-primary bg-surface-container-low text-primary'
                      : 'border border-outline-variant bg-transparent text-on-surface-variant'
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
              className="py-2.5 px-4 rounded-2xl border border-outline-variant bg-surface text-sm text-on-surface outline-none focus:border-primary font-sans"
            />
          </div>

          {/* 2. Pick Time Presets */}
          <div className="space-y-2">
            <label className="block text-xs font-semibold text-outline uppercase tracking-wider">
              2. Chọn ca rảnh nhanh
            </label>
            <div className="grid grid-cols-3 gap-3">
              {TIME_PRESETS.map((preset) => (
                <button
                  key={preset.label}
                  type="button"
                  onClick={() => handleApplyPreset(preset)}
                  className={`p-4 rounded-2xl border text-left cursor-pointer transition-all ${
                    activePreset === preset.label
                      ? 'border-2 border-primary bg-surface-container-low text-primary'
                      : 'border border-outline-variant bg-surface text-on-surface-variant hover:bg-surface-bright'
                  }`}
                >
                  <div className="flex items-center gap-2 font-bold text-sm">
                    <span className="material-symbols-outlined text-lg">{preset.icon}</span>
                    {preset.label}
                  </div>
                  <div className="text-xs text-outline mt-1 font-medium">{preset.desc}</div>
                </button>
              ))}
            </div>
          </div>

          {/* 3. Custom Time Inputs */}
          <div className="space-y-2 pt-1">
            <label className="block text-xs font-semibold text-outline uppercase tracking-wider">
              hoặc tùy chỉnh giờ bắt đầu &amp; kết thúc:
            </label>
            <div className="grid grid-cols-2 gap-4 bg-surface-container-low p-4 rounded-2xl border border-outline-variant">
              <div>
                <span className="block text-xs font-semibold text-outline mb-1">Giờ bắt đầu</span>
                <input
                  type="time"
                  required
                  value={startTime}
                  onChange={(e) => {
                    setStartTime(e.target.value);
                    setActivePreset(null);
                  }}
                  className="w-full py-2 px-3 rounded-xl border border-outline-variant bg-surface text-sm font-bold text-on-surface outline-none focus:border-primary"
                />
              </div>
              <div>
                <span className="block text-xs font-semibold text-outline mb-1">Giờ kết thúc</span>
                <input
                  type="time"
                  required
                  value={endTime}
                  onChange={(e) => {
                    setEndTime(e.target.value);
                    setActivePreset(null);
                  }}
                  className="w-full py-2 px-3 rounded-xl border border-outline-variant bg-surface text-sm font-bold text-on-surface outline-none focus:border-primary"
                />
              </div>
            </div>
          </div>

          {/* Action buttons */}
          <div className="flex items-center justify-end gap-3 pt-3 border-t border-surface-container-highest">
            <button
              type="button"
              onClick={() => setShowForm(false)}
              className="py-2.5 px-5 rounded-full border border-outline-variant bg-transparent text-on-surface-variant text-xs font-semibold hover:bg-surface-container-low cursor-pointer"
            >
              Hủy
            </button>
            <button
              type="submit"
              disabled={submitting}
              className="py-2.5 px-6 rounded-full bg-primary text-on-primary text-xs font-semibold cursor-pointer hover:brightness-110 disabled:opacity-50"
            >
              {submitting ? 'Đang tạo...' : 'Lưu khung giờ rảnh'}
            </button>
          </div>
        </form>
      )}

      {/* Slots List */}
      <div className="space-y-4">
        {slots.length === 0 && !showForm && (
          <div className="bg-surface rounded-2xl p-12 text-center text-outline shadow-md">
            <span className="material-symbols-outlined text-4xl block mb-2 opacity-50">calendar_month</span>
            <h3 className="font-bold text-on-surface text-base mb-1">Chưa có lịch rảnh nào</h3>
            <p className="text-xs text-outline max-w-sm mx-auto">
              Hãy nhấn nút "Thêm khung giờ rảnh" phía trên để cài đặt thời gian tư vấn của bạn.
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
                className={`bg-surface rounded-2xl p-5 shadow-md flex items-center justify-between hover:shadow-lg transition-shadow ${
                  isExpired ? 'opacity-50' : ''
                }`}
              >
                <div className="flex items-center gap-4">
                  <div className="w-12 h-12 rounded-full bg-primary-container text-primary flex items-center justify-center shrink-0">
                    <span className="material-symbols-outlined text-xl">calendar_month</span>
                  </div>
                  <div>
                    <div className="flex items-center gap-2">
                      <span className="font-bold text-on-surface text-base">
                        {start.toLocaleDateString('vi-VN', {
                          weekday: 'long',
                          day: '2-digit',
                          month: '2-digit',
                          year: 'numeric',
                        })}
                      </span>
                      <span className="py-0.5 px-3 rounded-full text-xs font-semibold bg-[#E6F4EA] text-[#137333]">
                        ✓ Sẵn sàng
                      </span>
                      {isExpired && <span className="text-xs text-error font-semibold">(Đã qua)</span>}
                    </div>
                    <div className="text-xs font-semibold text-primary mt-1 flex items-center gap-1">
                      <span className="material-symbols-outlined text-sm">schedule</span>
                      {start.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })}
                      {' → '}
                      {end.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })}
                    </div>
                  </div>
                </div>

                <button
                  onClick={() => handleDelete(slot.availabilityId)}
                  className="w-9 h-9 rounded-lg border border-outline-variant bg-transparent text-error flex items-center justify-center hover:bg-error-container/30 cursor-pointer"
                  title="Xóa khung giờ"
                >
                  <span className="material-symbols-outlined text-base">delete</span>
                </button>
              </div>
            );
          })}
      </div>
    </div>
  );
}

