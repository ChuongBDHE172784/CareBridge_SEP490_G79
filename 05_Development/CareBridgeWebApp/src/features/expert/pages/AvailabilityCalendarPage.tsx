import { useEffect, useMemo, useState, useCallback } from 'react';
import { getMyAvailability, replaceAvailability } from '../services/expertApi';
import type { AvailabilityResponse } from '../services/expertApi';

const HOURS = Array.from({ length: 14 }, (_, index) => index + 7);
const WEEKDAYS = ['T2', 'T3', 'T4', 'T5', 'T6', 'T7', 'CN'];
type ApplyMode = 'DAY' | 'WEEK' | 'MONTH' | 'WEEKDAYS' | 'MONTH_DAYS';

const dateKey = (date: Date) =>
  `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;

const parseDateKey = (key: string) => {
  const [year, month, day] = key.split('-').map(Number);
  return new Date(year, month - 1, day);
};

function monthCells(month: Date) {
  const first = new Date(month.getFullYear(), month.getMonth(), 1);
  const gridStart = new Date(first);
  gridStart.setDate(first.getDate() - ((first.getDay() + 6) % 7));
  return Array.from({ length: 42 }, (_, index) => {
    const date = new Date(gridStart);
    date.setDate(gridStart.getDate() + index);
    return date;
  });
}

function resolveTargetDates(
  selected: Date,
  mode: ApplyMode,
  weekdays: number[],
  monthDays: number[]
) {
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  let candidates: Date[] = [];
  if (mode === 'DAY') candidates = [selected];
  if (mode === 'WEEK') {
    const monday = new Date(selected);
    monday.setDate(selected.getDate() - ((selected.getDay() + 6) % 7));
    candidates = Array.from({ length: 7 }, (_, index) => {
      const date = new Date(monday);
      date.setDate(monday.getDate() + index);
      return date;
    });
  }
  if (mode === 'MONTH' || mode === 'WEEKDAYS' || mode === 'MONTH_DAYS') {
    const total = new Date(selected.getFullYear(), selected.getMonth() + 1, 0).getDate();
    candidates = Array.from({ length: total }, (_, index) =>
      new Date(selected.getFullYear(), selected.getMonth(), index + 1)
    );
    if (mode === 'WEEKDAYS') {
      candidates = candidates.filter(date => weekdays.includes((date.getDay() + 6) % 7));
    }
    if (mode === 'MONTH_DAYS') {
      candidates = candidates.filter(date => monthDays.includes(date.getDate()));
    }
  }
  return [...new Set(candidates.filter(date => date >= today).map(dateKey))];
}

/**
 * Gio da troi qua thi khong mo lich duoc nua. Server bo qua nhung khung gio nam
 * trong qua khu, nen truoc khi co man chan nay, chon 07:00 luc 21:00 se xoa sach
 * lich con lai cua hom do va van bao luu thanh cong.
 */
function isHourPast(dateKeyValue: string, hour: number, now: Date) {
  const start = parseDateKey(dateKeyValue);
  start.setHours(hour, 0, 0, 0);
  return start.getTime() <= now.getTime();
}

function translateError(error: any) {
  const message = error.response?.data?.message || error.message || '';
  if (message.includes('verified')) return 'Hồ sơ chuyên gia cần được duyệt trước khi thiết lập lịch làm việc.';
  if (message.includes('time zone')) return 'Múi giờ không hợp lệ. Vui lòng tải lại trang.';
  if (message.includes('already passed'))
    return 'Những khung giờ bạn chọn đều đã trôi qua. Hãy chọn giờ còn lại trong ngày hoặc một ngày khác.';
  return message || 'Không thể lưu lịch làm việc. Vui lòng thử lại.';
}

export default function AvailabilityCalendarPage() {
  const [slots, setSlots] = useState<AvailabilityResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [toastMessage, setToastMessage] = useState<string | null>(null);
  const [month, setMonth] = useState(() => new Date(new Date().getFullYear(), new Date().getMonth(), 1));
  const [editingDate, setEditingDate] = useState<string | null>(null);
  const [selectedHours, setSelectedHours] = useState<number[]>([]);
  const [mode, setMode] = useState<ApplyMode>('DAY');
  const [weekdays, setWeekdays] = useState<number[]>([]);
  const [monthDays, setMonthDays] = useState<number[]>([]);

  // Gio da qua chi bi khoa khi hom nay la NGAY DUY NHAT bi anh huong. Ap dung ca
  // tuan hay ca thang thi 07:00 van hop le voi nhung ngay sau, khong duoc khoa.
  const pastHours = useMemo(() => {
    if (!editingDate) return [] as number[];
    const targetDates = resolveTargetDates(parseDateKey(editingDate), mode, weekdays, monthDays);
    const now = new Date();
    if (targetDates.length !== 1 || targetDates[0] !== dateKey(now)) return [] as number[];
    return HOURS.filter(hour => isHourPast(targetDates[0], hour, now));
  }, [editingDate, mode, weekdays, monthDays]);

  // Bo cac khung gio vua tro thanh qua khu khi doi ngay hoac doi pham vi ap dung,
  // de nut Luu khong gui di thu ma nguoi dung khong con nhin thay minh dang chon.
  useEffect(() => {
    if (pastHours.length === 0) return;
    setSelectedHours(prev =>
      prev.some(hour => pastHours.includes(hour))
        ? prev.filter(hour => !pastHours.includes(hour))
        : prev
    );
  }, [pastHours]);

  const load = useCallback(async () => {
    try {
      setError(null);
      const res = await getMyAvailability();
      setSlots(res ?? []);
    } catch (cause) {
      setError(translateError(cause));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  const slotsByDate = useMemo(() => {
    const map = new Map<string, AvailabilityResponse[]>();
    for (const slot of slots.filter(slot => slot.status === 'AVAILABLE')) {
      const key = dateKey(new Date(slot.startAt));
      map.set(key, [...(map.get(key) ?? []), slot]);
    }
    return map;
  }, [slots]);

  // Calculated overview stats
  const stats = useMemo(() => {
    const now = new Date();
    const availableSlots = slots.filter(s => s.status === 'AVAILABLE' && new Date(s.endAt) > now);
    
    // Slots in current week (next 7 days)
    const sevenDaysLater = new Date();
    sevenDaysLater.setDate(now.getDate() + 7);
    const thisWeekCount = availableSlots.filter(s => {
      const d = new Date(s.startAt);
      return d >= now && d <= sevenDaysLater;
    }).length;

    // Unique days scheduled in current displayed month
    const currentMonthKeys = new Set<string>();
    for (const s of availableSlots) {
      const d = new Date(s.startAt);
      if (d.getFullYear() === month.getFullYear() && d.getMonth() === month.getMonth()) {
        currentMonthKeys.add(dateKey(d));
      }
    }

    return {
      upcomingCount: availableSlots.length,
      thisWeekCount,
      scheduledDaysCount: currentMonthKeys.size,
    };
  }, [slots, month]);

  const openDate = (date: Date) => {
    const key = dateKey(date);
    setEditingDate(key);
    setSelectedHours((slotsByDate.get(key) ?? []).map(slot => new Date(slot.startAt).getHours()));
    setMode('DAY');
    setWeekdays([(date.getDay() + 6) % 7]);
    setMonthDays([date.getDate()]);
    setError(null);
  };

  const jumpToToday = () => {
    const today = new Date();
    setMonth(new Date(today.getFullYear(), today.getMonth(), 1));
  };

  // Quick Preset Helper
  const applyPreset = (preset: 'MORNING' | 'AFTERNOON' | 'EVENING' | 'ALL' | 'CLEAR') => {
    if (preset === 'MORNING') {
      const morningHours = [7, 8, 9, 10, 11];
      setSelectedHours(prev => Array.from(new Set([...prev, ...morningHours])));
    } else if (preset === 'AFTERNOON') {
      const afternoonHours = [12, 13, 14, 15, 16, 17];
      setSelectedHours(prev => Array.from(new Set([...prev, ...afternoonHours])));
    } else if (preset === 'EVENING') {
      const eveningHours = [18, 19, 20];
      setSelectedHours(prev => Array.from(new Set([...prev, ...eveningHours])));
    } else if (preset === 'ALL') {
      setSelectedHours([...HOURS]);
    } else if (preset === 'CLEAR') {
      setSelectedHours([]);
    }
  };

  const save = async () => {
    if (!editingDate) return;
    const targetDates = resolveTargetDates(parseDateKey(editingDate), mode, weekdays, monthDays);
    if (targetDates.length === 0) {
      setError('Không có ngày hợp lệ trong phạm vi đã chọn.');
      return;
    }
    const usableHours = selectedHours.filter(hour => !pastHours.includes(hour));
    if (usableHours.length === 0 && selectedHours.length > 0) {
      setError('Những khung giờ bạn chọn đều đã trôi qua. Hãy chọn giờ còn lại trong ngày hoặc một ngày khác.');
      return;
    }
    setSaving(true);
    try {
      await replaceAvailability({
        targetDates,
        timeZone: Intl.DateTimeFormat().resolvedOptions().timeZone || 'Asia/Ho_Chi_Minh',
        channelType: 'ONLINE_CHAT',
        slots: usableHours.sort((a, b) => a - b).map(hour => ({
          startTime: `${String(hour).padStart(2, '0')}:00`,
        })),
      });
      setEditingDate(null);
      setToastMessage(`Đã lưu thành công lịch làm việc cho ${targetDates.length} ngày!`);
      setTimeout(() => setToastMessage(null), 4000);
      await load();
    } catch (cause) {
      setError(translateError(cause));
    } finally {
      setSaving(false);
    }
  };

  const cells = monthCells(month);
  const todayKey = dateKey(new Date());
  const monthTitle = month.toLocaleDateString('vi-VN', { month: 'long', year: 'numeric' });

  if (loading) {
    return (
      <div className="flex h-96 items-center justify-center">
        <div className="flex items-center gap-3 text-on-surface-variant font-medium">
          <span className="material-symbols-outlined animate-spin text-primary text-2xl">progress_activity</span>
          Đang tải lịch làm việc…
        </div>
      </div>
    );
  }

  return (
    <div className="p-8 font-sans space-y-8">
      {/* Toast notification banner */}
      {toastMessage && (
        <div className="fixed bottom-6 right-6 z-50 flex items-center gap-3 rounded-2xl bg-primary px-5 py-3.5 text-on-primary shadow-xl animate-bounce">
          <span className="material-symbols-outlined">check_circle</span>
          <span className="text-sm font-semibold">{toastMessage}</span>
        </div>
      )}

      {/* Page Header */}
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <h1 className="text-[26px] font-bold text-on-surface m-0">Lịch rảnh làm việc</h1>
          <p className="text-on-surface-variant text-sm mt-1">
            Thiết lập các ca tư vấn trực tuyến khả dụng để mẹ bầu có thể chủ động đặt lịch hẹn
          </p>
        </div>

        <div className="flex items-center gap-3">
          <button
            onClick={jumpToToday}
            className="flex items-center gap-2 py-2.5 px-4 rounded-full border border-outline-variant bg-transparent text-primary text-xs font-semibold hover:bg-surface-container-low cursor-pointer transition-colors"
          >
            <span className="material-symbols-outlined text-base">today</span>
            Đến hôm nay
          </button>
          <button
            onClick={load}
            className="flex items-center gap-2 py-2.5 px-4 rounded-full bg-primary text-on-primary border-0 text-xs font-semibold hover:brightness-110 cursor-pointer transition-all shadow-xs"
          >
            <span className="material-symbols-outlined text-base">refresh</span>
            Tải lại
          </button>
        </div>
      </div>

      {/* Error alert banner */}
      {error && (
        <div className="flex items-start gap-3 rounded-2xl border border-error/30 bg-error-container/40 p-4 text-on-error-container">
          <span className="material-symbols-outlined text-error shrink-0 mt-0.5">error</span>
          <div className="text-sm font-medium">{error}</div>
        </div>
      )}

      {/* Stat Cards Row */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <div className="bg-surface rounded-3xl p-6 shadow-md flex items-center gap-4 border border-outline-variant/50">
          <div className="w-12 h-12 rounded-full flex items-center justify-center shrink-0 bg-primary/10 text-primary">
            <span className="material-symbols-outlined text-2xl">calendar_clock</span>
          </div>
          <div className="overflow-hidden">
            <div className="text-[26px] font-bold text-on-surface truncate">{stats.upcomingCount}</div>
            <div className="text-[13px] text-outline mt-0.5 truncate">Ca khả dụng sắp tới</div>
            <div className="text-[11px] text-primary font-medium mt-0.5">Đang mở đặt lịch</div>
          </div>
        </div>

        <div className="bg-surface rounded-3xl p-6 shadow-md flex items-center gap-4 border border-outline-variant/50">
          <div className="w-12 h-12 rounded-full flex items-center justify-center shrink-0 bg-[#137333]/10 text-[#137333]">
            <span className="material-symbols-outlined text-2xl">date_range</span>
          </div>
          <div className="overflow-hidden">
            <div className="text-[26px] font-bold text-on-surface truncate">{stats.thisWeekCount}</div>
            <div className="text-[13px] text-outline mt-0.5 truncate">Ca rảnh tuần này</div>
            <div className="text-[11px] text-on-surface-variant font-medium mt-0.5">Trong 7 ngày tới</div>
          </div>
        </div>

        <div className="bg-surface rounded-3xl p-6 shadow-md flex items-center gap-4 border border-outline-variant/50">
          <div className="w-12 h-12 rounded-full flex items-center justify-center shrink-0 bg-[#7C3AED]/10 text-[#7C3AED]">
            <span className="material-symbols-outlined text-2xl">event_available</span>
          </div>
          <div className="overflow-hidden">
            <div className="text-[26px] font-bold text-on-surface truncate">{stats.scheduledDaysCount}</div>
            <div className="text-[13px] text-outline mt-0.5 truncate">Số ngày đã mở lịch</div>
            <div className="text-[11px] text-on-surface-variant font-medium mt-0.5">Trong tháng này</div>
          </div>
        </div>

        <div className="bg-surface rounded-3xl p-6 shadow-md flex items-center gap-4 border border-outline-variant/50">
          <div className="w-12 h-12 rounded-full flex items-center justify-center shrink-0 bg-[#0061A4]/10 text-[#0061A4]">
            <span className="material-symbols-outlined text-2xl">video_chat</span>
          </div>
          <div className="overflow-hidden">
            <div className="text-[26px] font-bold text-on-surface truncate">Trực tuyến</div>
            <div className="text-[13px] text-outline mt-0.5 truncate">Hình thức tư vấn</div>
            <div className="text-[11px] text-on-surface-variant font-medium mt-0.5">Trò chuyện & Gọi thoại</div>
          </div>
        </div>
      </div>

      {/* Main Calendar Section */}
      <section className="bg-surface rounded-3xl p-6 md:p-8 shadow-md border border-outline-variant/70 space-y-6">
        {/* Navigation & Controls bar */}
        <div className="flex flex-wrap items-center justify-between gap-4 border-b border-surface-container-highest pb-5">
          <div className="flex items-center gap-3">
            <button
              aria-label="Tháng trước"
              onClick={() => setMonth(new Date(month.getFullYear(), month.getMonth() - 1, 1))}
              className="flex h-10 w-10 items-center justify-center rounded-full border border-outline-variant bg-surface text-on-surface-variant hover:bg-surface-container-low hover:text-on-surface transition-colors cursor-pointer"
            >
              <span className="material-symbols-outlined text-xl">chevron_left</span>
            </button>
            <h2 className="m-0 text-xl font-bold capitalize text-on-surface">{monthTitle}</h2>
            <button
              aria-label="Tháng sau"
              onClick={() => setMonth(new Date(month.getFullYear(), month.getMonth() + 1, 1))}
              className="flex h-10 w-10 items-center justify-center rounded-full border border-outline-variant bg-surface text-on-surface-variant hover:bg-surface-container-low hover:text-on-surface transition-colors cursor-pointer"
            >
              <span className="material-symbols-outlined text-xl">chevron_right</span>
            </button>
          </div>

          {/* Legend Badges */}
          <div className="flex flex-wrap items-center gap-4 text-xs font-semibold text-on-surface-variant">
            <div className="flex items-center gap-1.5">
              <span className="h-3 w-3 rounded-full border-2 border-primary bg-primary-container"></span>
              <span>Hôm nay</span>
            </div>
            <div className="flex items-center gap-1.5">
              <span className="h-3 w-3 rounded-full bg-primary-container border border-primary"></span>
              <span>Có ca rảnh</span>
            </div>
            <div className="flex items-center gap-1.5">
              <span className="h-3 w-3 rounded-full bg-surface-container-low border border-outline-variant/40 opacity-50"></span>
              <span>Đã qua</span>
            </div>
          </div>
        </div>

        {/* Calendar Grid */}
        <div className="grid grid-cols-7 gap-2 md:gap-3">
          {WEEKDAYS.map(day => (
            <div key={day} className="py-2.5 text-center text-xs font-semibold uppercase tracking-wider text-outline">
              {day}
            </div>
          ))}

          {cells.map(date => {
            const key = dateKey(date);
            const count = slotsByDate.get(key)?.length ?? 0;
            const outside = date.getMonth() !== month.getMonth();
            const past = key < todayKey;
            const isToday = key === todayKey;

            return (
              <button
                key={key}
                disabled={past}
                onClick={() => openDate(date)}
                className={`relative min-h-[96px] rounded-2xl border p-2.5 text-left transition-all duration-200 flex flex-col justify-between ${
                  outside
                    ? 'bg-surface-container-low/50 border-transparent text-outline/50'
                    : 'bg-surface border-outline-variant/60'
                } ${
                  past
                    ? 'cursor-not-allowed opacity-40 bg-surface-container-low border-outline-variant/30'
                    : 'hover:-translate-y-0.5 hover:border-primary hover:shadow-md cursor-pointer'
                } ${
                  isToday ? 'border-2 border-primary bg-primary-container/20 shadow-xs' : ''
                }`}
              >
                <div className="flex items-center justify-between">
                  <span
                    className={`flex h-7 w-7 items-center justify-center rounded-full text-xs font-bold ${
                      isToday ? 'bg-primary text-on-primary' : 'text-on-surface'
                    }`}
                  >
                    {date.getDate()}
                  </span>
                  {count > 0 && (
                    <span className="material-symbols-outlined text-sm text-primary">schedule</span>
                  )}
                </div>

                {count > 0 ? (
                  <span className="mt-2 block rounded-full bg-primary-container text-primary px-2.5 py-1 text-center text-[11px] font-semibold border border-primary/20 truncate">
                    {count} ca rảnh
                  </span>
                ) : (
                  !past && !outside && (
                    <span className="mt-2 block text-center text-[10px] text-outline/60 opacity-0 hover:opacity-100 transition-opacity">
                      + Thêm lịch
                    </span>
                  )
                )}
              </button>
            );
          })}
        </div>
      </section>

      {/* Slot Editor Modal Dialog (MD3 Style) */}
      {editingDate && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center bg-on-surface/40 backdrop-blur-xs p-4 overflow-y-auto"
          role="dialog"
          aria-modal="true"
        >
          <div className="my-8 max-h-[90vh] w-full max-w-2xl overflow-y-auto rounded-3xl border border-outline-variant/60 bg-surface p-6 shadow-2xl space-y-6 md:p-8">
            {/* Modal Header */}
            <div className="flex items-start justify-between gap-4 border-b border-surface-container-highest pb-4">
              <div>
                <p className="m-0 text-xs font-semibold uppercase tracking-wider text-primary">Thiết lập lịch làm việc</p>
                <h2 className="mt-1 text-xl font-bold text-on-surface capitalize m-0">
                  {parseDateKey(editingDate).toLocaleDateString('vi-VN', {
                    weekday: 'long',
                    day: '2-digit',
                    month: '2-digit',
                    year: 'numeric',
                  })}
                </h2>
              </div>
              <button
                aria-label="Đóng"
                onClick={() => setEditingDate(null)}
                className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full border border-outline-variant bg-surface text-on-surface-variant hover:bg-surface-container-low hover:text-on-surface transition-colors cursor-pointer"
              >
                <span className="material-symbols-outlined text-lg">close</span>
              </button>
            </div>

            {/* Quick Presets Toolbar */}
            <div>
              <div className="flex items-center justify-between mb-2">
                <span className="text-sm font-semibold text-on-surface">Chọn khung giờ rảnh</span>
                <span className="text-xs text-outline font-medium">Đã chọn: {selectedHours.length} khung giờ</span>
              </div>

              <div className="flex flex-wrap gap-2 mb-3">
                <button
                  type="button"
                  onClick={() => applyPreset('MORNING')}
                  className="px-3 py-1.5 rounded-full border border-outline-variant bg-surface-container-low text-on-surface-variant text-xs font-semibold hover:border-primary hover:text-primary transition-colors cursor-pointer"
                >
                  🌅 Sáng (07-12h)
                </button>
                <button
                  type="button"
                  onClick={() => applyPreset('AFTERNOON')}
                  className="px-3 py-1.5 rounded-full border border-outline-variant bg-surface-container-low text-on-surface-variant text-xs font-semibold hover:border-primary hover:text-primary transition-colors cursor-pointer"
                >
                  ☀️ Chiều (13-18h)
                </button>
                <button
                  type="button"
                  onClick={() => applyPreset('EVENING')}
                  className="px-3 py-1.5 rounded-full border border-outline-variant bg-surface-container-low text-on-surface-variant text-xs font-semibold hover:border-primary hover:text-primary transition-colors cursor-pointer"
                >
                  🌙 Tối (18-21h)
                </button>
                <button
                  type="button"
                  onClick={() => applyPreset('ALL')}
                  className="px-3 py-1.5 rounded-full border border-outline-variant bg-surface-container-low text-on-surface-variant text-xs font-semibold hover:border-primary hover:text-primary transition-colors cursor-pointer"
                >
                  ✨ Chọn tất cả
                </button>
                <button
                  type="button"
                  onClick={() => applyPreset('CLEAR')}
                  className="px-3 py-1.5 rounded-full border border-outline-variant bg-surface-container-low text-error text-xs font-semibold hover:bg-error-container transition-colors cursor-pointer"
                >
                  🧹 Xóa chọn
                </button>
              </div>

              {/* Hours Grid */}
              <div className="grid grid-cols-2 gap-2 sm:grid-cols-3 md:grid-cols-4">
                {HOURS.map(hour => {
                  const active = selectedHours.includes(hour);
                  const past = pastHours.includes(hour);
                  return (
                    <button
                      key={hour}
                      type="button"
                      disabled={past}
                      title={past ? 'Khung giờ này đã trôi qua' : undefined}
                      onClick={() =>
                        setSelectedHours(
                          active ? selectedHours.filter(value => value !== hour) : [...selectedHours, hour]
                        )
                      }
                      className={`flex items-center justify-center gap-1.5 rounded-xl border py-2.5 px-3 text-xs font-semibold transition-all ${
                        past
                          ? 'border-outline-variant bg-surface-container text-outline line-through cursor-not-allowed opacity-60'
                          : active
                            ? 'border-primary bg-primary text-on-primary shadow-xs cursor-pointer'
                            : 'border-outline-variant bg-surface-container-low text-on-surface hover:border-primary/50 cursor-pointer'
                      }`}
                    >
                      <span className="material-symbols-outlined text-sm">
                        {past ? 'history' : active ? 'check_circle' : 'schedule'}
                      </span>
                      {String(hour).padStart(2, '0')}:00–{String(hour + 1).padStart(2, '0')}:00
                    </button>
                  );
                })}
              </div>
            </div>

            {/* Apply Mode Section */}
            <div className="space-y-3">
              <span className="text-sm font-semibold text-on-surface block">Áp dụng lịch này cho</span>
              <div className="grid gap-2 sm:grid-cols-2">
                {(
                  [
                    ['DAY', 'Chỉ ngày này'],
                    ['WEEK', 'Tất cả ngày trong tuần'],
                    ['MONTH', 'Tất cả ngày trong tháng'],
                    ['WEEKDAYS', 'Các thứ tùy chọn trong tháng'],
                    ['MONTH_DAYS', 'Các ngày tùy chọn trong tháng'],
                  ] as Array<[ApplyMode, string]>
                ).map(([value, label]) => (
                  <button
                    key={value}
                    type="button"
                    onClick={() => setMode(value)}
                    className={`rounded-xl border-2 p-3 text-left font-semibold text-xs transition-all cursor-pointer ${
                      mode === value
                        ? 'border-primary bg-primary-container text-primary'
                        : 'border-outline-variant/60 bg-surface hover:border-primary/40 text-on-surface-variant'
                    }`}
                  >
                    {label}
                  </button>
                ))}
              </div>

              {mode === 'WEEKDAYS' && (
                <div className="mt-3 space-y-2 bg-surface-container-low p-3 rounded-2xl border border-outline-variant/40">
                  <span className="text-xs font-semibold text-outline">Chọn các thứ trong tuần:</span>
                  <div className="flex flex-wrap gap-2">
                    {WEEKDAYS.map((day, index) => (
                      <button
                        key={day}
                        type="button"
                        onClick={() =>
                          setWeekdays(
                            weekdays.includes(index) ? weekdays.filter(value => value !== index) : [...weekdays, index]
                          )
                        }
                        className={`h-9 min-w-9 rounded-full px-3 text-xs font-bold transition-colors cursor-pointer ${
                          weekdays.includes(index)
                            ? 'bg-primary text-on-primary'
                            : 'bg-surface border border-outline-variant text-on-surface-variant hover:bg-surface-container-high'
                        }`}
                      >
                        {day}
                      </button>
                    ))}
                  </div>
                </div>
              )}

              {mode === 'MONTH_DAYS' && (
                <div className="mt-3 space-y-2 bg-surface-container-low p-3 rounded-2xl border border-outline-variant/40">
                  <span className="text-xs font-semibold text-outline">Chọn ngày cụ thể trong tháng:</span>
                  <div className="grid grid-cols-7 gap-1.5">
                    {Array.from(
                      {
                        length: new Date(
                          parseDateKey(editingDate).getFullYear(),
                          parseDateKey(editingDate).getMonth() + 1,
                          0
                        ).getDate(),
                      },
                      (_, index) => index + 1
                    ).map(day => (
                      <button
                        key={day}
                        type="button"
                        onClick={() =>
                          setMonthDays(
                            monthDays.includes(day) ? monthDays.filter(value => value !== day) : [...monthDays, day]
                          )
                        }
                        className={`h-8 rounded-full text-xs font-bold transition-colors cursor-pointer ${
                          monthDays.includes(day)
                            ? 'bg-primary text-on-primary'
                            : 'bg-surface border border-outline-variant text-on-surface-variant hover:bg-surface-container-high'
                        }`}
                      >
                        {day}
                      </button>
                    ))}
                  </div>
                </div>
              )}
            </div>

            {/* Modal Actions Footer */}
            <div className="flex justify-end gap-3 border-t border-surface-container-highest pt-4">
              <button
                type="button"
                onClick={() => setEditingDate(null)}
                className="py-2.5 px-5 rounded-full border border-outline-variant bg-transparent text-on-surface-variant text-xs font-semibold hover:bg-surface-container-low cursor-pointer transition-colors"
              >
                Hủy
              </button>
              <button
                type="button"
                disabled={saving}
                onClick={save}
                className="flex items-center gap-2 py-2.5 px-6 rounded-full bg-primary text-on-primary border-0 text-xs font-semibold hover:brightness-110 cursor-pointer shadow-xs disabled:opacity-50 transition-all"
              >
                {saving ? (
                  <>
                    <span className="material-symbols-outlined text-base animate-spin">progress_activity</span>
                    Đang lưu…
                  </>
                ) : (
                  <>
                    <span className="material-symbols-outlined text-base">save</span>
                    Lưu lịch làm việc
                  </>
                )}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

