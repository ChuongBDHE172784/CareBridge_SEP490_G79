import { useEffect, useState, useCallback, useMemo } from 'react';
import type {
  ConsultationCallAdminSummary,
  CallType,
  CallStatus,
} from '../models/consultationCall';
import { searchConsultationCalls } from '../services/consultationCallApi';
import CallRecordingPlayerModal from '../components/CallRecordingPlayerModal';

function formatDuration(seconds: number | null): string {
  if (!seconds || seconds <= 0) return '00:00';
  const m = Math.floor(seconds / 60);
  const s = seconds % 60;
  return `${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`;
}

function formatDateTime(iso: string | null): string {
  if (!iso) return '—';
  const d = new Date(iso);
  return d.toLocaleString('vi-VN', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
}

export default function ConsultationCallListPage() {
  const [calls, setCalls] = useState<ConsultationCallAdminSummary[]>([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(0);
  const [pageSize] = useState(10);
  const [keyword, setKeyword] = useState('');
  const [callTypeFilter, setCallTypeFilter] = useState<CallType | ''>('');
  const [callStatusFilter, setCallStatusFilter] = useState<CallStatus | ''>('');
  const [hasRecordingFilter, setHasRecordingFilter] = useState<'' | 'true' | 'false'>('');
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Selected call for playback modal
  const [selectedCallForPlayback, setSelectedCallForPlayback] = useState<ConsultationCallAdminSummary | null>(null);

  const fetchCalls = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      const res = await searchConsultationCalls({
        keyword: keyword.trim() || undefined,
        callType: callTypeFilter || undefined,
        callStatus: callStatusFilter || undefined,
        hasRecording:
          hasRecordingFilter === 'true'
            ? true
            : hasRecordingFilter === 'false'
            ? false
            : undefined,
        page,
        size: pageSize,
      });
      setCalls(res.content || []);
      setTotal(res.totalElements || 0);
    } catch (err) {
      console.error('[ConsultationCallListPage] fetchCalls error:', err);
      setError('Không thể tải danh sách cuộc gọi tư vấn. Vui lòng thử lại sau.');
    } finally {
      setIsLoading(false);
    }
  }, [keyword, callTypeFilter, callStatusFilter, hasRecordingFilter, page, pageSize]);

  useEffect(() => {
    fetchCalls();
  }, [fetchCalls]);

  // Statistics calculation for KPI cards
  const stats = useMemo(() => {
    const recordedCount = calls.filter((c) => !!c.recordingFileId).length;
    const totalSecs = calls.reduce((acc, c) => acc + (c.durationSeconds || 0), 0);
    const totalMins = Math.round(totalSecs / 60);
    return {
      totalCalls: total,
      recordedInPage: recordedCount,
      totalMinutes: totalMins,
    };
  }, [calls, total]);

  const totalPages = Math.ceil(total / pageSize) || 1;

  return (
    <div className="p-6 md:p-8 space-y-6 max-w-7xl mx-auto font-sans text-on-surface">
      {/* Page Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <div className="flex items-center gap-2.5">
            <span className="material-symbols-outlined text-3xl text-primary">record_voice_over</span>
            <h1 className="text-2xl font-bold text-on-surface tracking-tight">
              Quản lý Bản ghi Tư vấn 1-1
            </h1>
          </div>
          <p className="text-xs text-on-surface-variant mt-1">
            Giám sát cuộc gọi Video/Thoại giữa Mẹ và Bác sĩ, kiểm tra chất lượng chuyên môn & lưu trữ bảo mật PDPA
          </p>
        </div>
        <button
          onClick={() => fetchCalls()}
          className="flex items-center gap-2 rounded-xl border border-outline-variant/60 bg-surface px-4 py-2 text-xs font-semibold text-on-surface-variant hover:bg-surface-variant hover:text-on-surface transition-colors cursor-pointer shadow-sm self-start sm:self-auto"
        >
          <span className="material-symbols-outlined text-base">refresh</span>
          Làm mới
        </button>
      </div>

      {/* KPI Stats Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <div className="rounded-2xl border border-outline-variant/50 bg-surface-container-low p-5 shadow-sm">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-on-surface-variant uppercase tracking-wider">Tổng cuộc gọi</span>
            <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-primary/10 text-primary">
              <span className="material-symbols-outlined text-xl">phone_in_talk</span>
            </div>
          </div>
          <div className="mt-3">
            <div className="text-2xl font-bold text-on-surface">{stats.totalCalls}</div>
            <p className="text-[11px] text-outline mt-0.5">Tất cả phiên kết nối trong hệ thống</p>
          </div>
        </div>

        <div className="rounded-2xl border border-outline-variant/50 bg-surface-container-low p-5 shadow-sm">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-emerald-800 uppercase tracking-wider">Bản ghi khả dụng</span>
            <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-emerald-100 text-emerald-700">
              <span className="material-symbols-outlined text-xl">videocam</span>
            </div>
          </div>
          <div className="mt-3">
            <div className="text-2xl font-bold text-emerald-700">{stats.recordedInPage} <span className="text-xs font-normal text-slate-500">trên trang</span></div>
            <p className="text-[11px] text-outline mt-0.5">Đã lưu trữ an toàn tại Cloudflare R2</p>
          </div>
        </div>

        <div className="rounded-2xl border border-outline-variant/50 bg-surface-container-low p-5 shadow-sm">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-blue-800 uppercase tracking-wider">Thời lượng tư vấn</span>
            <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-blue-100 text-blue-700">
              <span className="material-symbols-outlined text-xl">schedule</span>
            </div>
          </div>
          <div className="mt-3">
            <div className="text-2xl font-bold text-blue-700">{stats.totalMinutes} <span className="text-xs font-normal text-slate-500">phút</span></div>
            <p className="text-[11px] text-outline mt-0.5">Tổng thời gian kết nối của các cuộc gọi</p>
          </div>
        </div>

        <div className="rounded-2xl border border-outline-variant/50 bg-surface-container-low p-5 shadow-sm">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-purple-800 uppercase tracking-wider">Tuân thủ PDPA</span>
            <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-purple-100 text-purple-700">
              <span className="material-symbols-outlined text-xl">verified_user</span>
            </div>
          </div>
          <div className="mt-3">
            <div className="text-2xl font-bold text-purple-700">100%</div>
            <p className="text-[11px] text-outline mt-0.5">Đồng thuận ghi âm ghi hình bảo mật</p>
          </div>
        </div>
      </div>

      {/* Filter & Search Bar */}
      <div className="rounded-2xl border border-outline-variant/50 bg-surface-container-low p-4 shadow-sm space-y-3">
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3">
          {/* Keyword Search */}
          <div className="relative">
            <span className="material-symbols-outlined absolute left-3 top-2.5 text-lg text-outline">search</span>
            <input
              type="text"
              placeholder="Tìm theo Mẹ, Chuyên gia, SĐT..."
              value={keyword}
              onChange={(e) => {
                setKeyword(e.target.value);
                setPage(0);
              }}
              className="w-full rounded-xl border border-outline-variant/60 bg-surface py-2 pl-9 pr-3 text-xs text-on-surface placeholder:text-outline focus:border-primary focus:outline-none"
            />
          </div>

          {/* Call Type Filter */}
          <div>
            <select
              value={callTypeFilter}
              onChange={(e) => {
                setCallTypeFilter(e.target.value as CallType | '');
                setPage(0);
              }}
              className="w-full rounded-xl border border-outline-variant/60 bg-surface py-2 px-3 text-xs text-on-surface focus:border-primary focus:outline-none"
            >
              <option value="">Tất cả hình thức cuộc gọi</option>
              <option value="VIDEO">Cuộc gọi Video (Camera)</option>
              <option value="AUDIO">Cuộc gọi Thoại (Voice)</option>
            </select>
          </div>

          {/* Call Status Filter */}
          <div>
            <select
              value={callStatusFilter}
              onChange={(e) => {
                setCallStatusFilter(e.target.value as CallStatus | '');
                setPage(0);
              }}
              className="w-full rounded-xl border border-outline-variant/60 bg-surface py-2 px-3 text-xs text-on-surface focus:border-primary focus:outline-none"
            >
              <option value="">Tất cả trạng thái</option>
              <option value="ANSWERED">Đã trả lời / Đang diễn ra</option>
              <option value="ENDED">Đã kết thúc thành công</option>
              <option value="MISSED">Cuộc gọi nhỡ (Missed)</option>
              <option value="DECLINED">Từ chối (Declined)</option>
              <option value="CANCELLED">Người gọi đã hủy</option>
            </select>
          </div>

          {/* Has Recording Filter */}
          <div>
            <select
              value={hasRecordingFilter}
              onChange={(e) => {
                setHasRecordingFilter(e.target.value as '' | 'true' | 'false');
                setPage(0);
              }}
              className="w-full rounded-xl border border-outline-variant/60 bg-surface py-2 px-3 text-xs text-on-surface focus:border-primary focus:outline-none"
            >
              <option value="">Tất cả bản ghi</option>
              <option value="true">Có bản ghi âm / ghi hình</option>
              <option value="false">Không có bản ghi</option>
            </select>
          </div>
        </div>
      </div>

      {/* Main Table */}
      <div className="overflow-hidden rounded-2xl border border-outline-variant/50 bg-surface shadow-sm">
        {isLoading ? (
          <div className="flex h-64 flex-col items-center justify-center gap-3">
            <div className="h-8 w-8 animate-spin rounded-full border-2 border-primary border-t-transparent" />
            <p className="text-xs text-on-surface-variant">Đang tải danh sách cuộc gọi tư vấn...</p>
          </div>
        ) : error ? (
          <div className="flex h-64 flex-col items-center justify-center gap-2 p-6 text-center">
            <span className="material-symbols-outlined text-4xl text-rose-500">error</span>
            <p className="text-sm font-semibold text-rose-800">{error}</p>
            <button
              onClick={() => fetchCalls()}
              className="mt-2 rounded-lg bg-primary px-3 py-1.5 text-xs font-semibold text-white cursor-pointer"
            >
              Thử lại
            </button>
          </div>
        ) : calls.length === 0 ? (
          <div className="flex h-64 flex-col items-center justify-center gap-2 p-6 text-center">
            <span className="material-symbols-outlined text-4xl text-outline">call_end</span>
            <p className="text-sm font-semibold text-on-surface">Không tìm thấy cuộc gọi nào</p>
            <p className="text-xs text-on-surface-variant">
              Thử thay đổi bộ lọc hoặc từ khóa tìm kiếm.
            </p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="border-b border-outline-variant/50 bg-surface-container-low text-[11px] font-bold uppercase tracking-wider text-on-surface-variant">
                <tr>
                  <th className="py-3.5 pl-6 pr-3">Mã cuộc gọi & Thời gian</th>
                  <th className="py-3.5 px-3">Người Mẹ (Mother)</th>
                  <th className="py-3.5 px-3">Chuyên gia (Expert)</th>
                  <th className="py-3.5 px-3">Hình thức</th>
                  <th className="py-3.5 px-3">Thời lượng</th>
                  <th className="py-3.5 px-3">Trạng thái</th>
                  <th className="py-3.5 px-3">PDPA Consent</th>
                  <th className="py-3.5 pl-3 pr-6 text-right">Bản ghi & Thao tác</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-outline-variant/40">
                {calls.map((call) => {
                  const hasRecord = !!call.recordingFileId;
                  const isVideo = call.callType === 'VIDEO';

                  return (
                    <tr
                      key={call.callId}
                      className="hover:bg-surface-container-lowest/70 transition-colors"
                    >
                      {/* Call ID & Initiated Time */}
                      <td className="py-4 pl-6 pr-3 font-medium">
                        <div className="font-mono text-[11px] text-on-surface font-semibold">
                          {call.callId.slice(0, 8)}...
                        </div>
                        <div className="text-[11px] text-on-surface-variant mt-0.5">
                          {formatDateTime(call.initiatedAt)}
                        </div>
                      </td>

                      {/* Mother Info */}
                      <td className="py-4 px-3">
                        <div className="font-semibold text-on-surface">{call.motherName}</div>
                        <div className="text-[11px] text-on-surface-variant mt-0.5">
                          {call.motherPhone || call.motherEmail || '—'}
                        </div>
                      </td>

                      {/* Expert Info */}
                      <td className="py-4 px-3">
                        <div className="font-semibold text-on-surface">{call.expertName}</div>
                        <div className="text-[11px] text-on-surface-variant mt-0.5">
                          {call.expertSpecialization ? `CK: ${call.expertSpecialization}` : (call.expertHospital || 'Chuyên gia y tế')}
                        </div>
                      </td>

                      {/* Call Type */}
                      <td className="py-4 px-3">
                        <span
                          className={`inline-flex items-center gap-1 rounded-full px-2.5 py-0.5 text-[11px] font-semibold ${
                            isVideo
                              ? 'bg-blue-50 text-blue-700 border border-blue-200'
                              : 'bg-purple-50 text-purple-700 border border-purple-200'
                          }`}
                        >
                          <span className="material-symbols-outlined text-[14px]">
                            {isVideo ? 'videocam' : 'mic'}
                          </span>
                          {isVideo ? 'Video Call' : 'Audio Call'}
                        </span>
                      </td>

                      {/* Duration */}
                      <td className="py-4 px-3 font-mono font-medium text-on-surface">
                        {formatDuration(call.durationSeconds)}
                      </td>

                      {/* Status */}
                      <td className="py-4 px-3">
                        <span
                          className={`inline-flex items-center gap-1 rounded-md px-2 py-0.5 text-[11px] font-semibold ${
                            call.callStatus === 'ENDED' || call.callStatus === 'ANSWERED'
                              ? 'bg-emerald-50 text-emerald-700 border border-emerald-200'
                              : call.callStatus === 'MISSED'
                              ? 'bg-amber-50 text-amber-700 border border-amber-200'
                              : 'bg-slate-100 text-slate-700 border border-slate-200'
                          }`}
                        >
                          {call.callStatus === 'ENDED'
                            ? 'Hoàn thành'
                            : call.callStatus === 'ANSWERED'
                            ? 'Đã kết nối'
                            : call.callStatus === 'MISSED'
                            ? 'Cuộc gọi nhỡ'
                            : call.callStatus === 'DECLINED'
                            ? 'Từ chối'
                            : call.callStatus}
                        </span>
                      </td>

                      {/* PDPA Consent Badge */}
                      <td className="py-4 px-3">
                        <span className="inline-flex items-center gap-1 text-[11px] font-medium text-emerald-700 bg-emerald-50 border border-emerald-200 rounded-md px-2 py-0.5">
                          <span className="material-symbols-outlined text-[13px]">check_circle</span>
                          Đã đồng thuận
                        </span>
                      </td>

                      {/* Action Button */}
                      <td className="py-4 pl-3 pr-6 text-right">
                        {hasRecord ? (
                          <button
                            type="button"
                            onClick={() => setSelectedCallForPlayback(call)}
                            className="inline-flex items-center gap-1.5 rounded-lg bg-primary px-3 py-1.5 text-xs font-semibold text-white hover:bg-primary/90 transition-all shadow-sm cursor-pointer"
                          >
                            <span className="material-symbols-outlined text-[16px]">play_circle</span>
                            Phát bản ghi
                          </button>
                        ) : (
                          <span className="text-[11px] text-outline italic">Không có bản ghi</span>
                        )}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}

        {/* Pagination Bar */}
        {!isLoading && calls.length > 0 && (
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 border-t border-outline-variant/50 bg-surface-container-low px-6 py-3 text-xs text-on-surface-variant">
            <div>
              Hiển thị <strong>{calls.length}</strong> trên tổng số <strong>{total}</strong> cuộc gọi
            </div>
            <div className="flex items-center gap-2">
              <button
                type="button"
                disabled={page <= 0}
                onClick={() => setPage((p) => Math.max(0, p - 1))}
                className="rounded-lg border border-outline-variant/60 bg-surface px-3 py-1 text-xs font-semibold text-on-surface disabled:opacity-40 hover:bg-surface-variant cursor-pointer disabled:cursor-not-allowed"
              >
                Trước
              </button>
              <span className="px-2">
                Trang <strong>{page + 1}</strong> / <strong>{totalPages}</strong>
              </span>
              <button
                type="button"
                disabled={page >= totalPages - 1}
                onClick={() => setPage((p) => p + 1)}
                className="rounded-lg border border-outline-variant/60 bg-surface px-3 py-1 text-xs font-semibold text-on-surface disabled:opacity-40 hover:bg-surface-variant cursor-pointer disabled:cursor-not-allowed"
              >
                Sau
              </button>
            </div>
          </div>
        )}
      </div>

      {/* Playback Modal */}
      {selectedCallForPlayback && (
        <CallRecordingPlayerModal
          call={selectedCallForPlayback}
          onClose={() => setSelectedCallForPlayback(null)}
        />
      )}
    </div>
  );
}
