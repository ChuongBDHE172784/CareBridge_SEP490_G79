import { useEffect, useState, useRef } from 'react';
import type { ConsultationCallAdminSummary } from '../models/consultationCall';
import { getCallRecordingPresignedUrl } from '../services/consultationCallApi';

interface CallRecordingPlayerModalProps {
  call: ConsultationCallAdminSummary | null;
  onClose: () => void;
}

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
    second: '2-digit',
  });
}

export default function CallRecordingPlayerModal({
  call,
  onClose,
}: CallRecordingPlayerModalProps) {
  const [streamUrl, setStreamUrl] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [playbackRate, setPlaybackRate] = useState<number>(1);
  const videoRef = useRef<HTMLVideoElement>(null);
  const audioRef = useRef<HTMLAudioElement>(null);

  useEffect(() => {
    if (!call) return;
    if (!call.recordingFileId) {
      setError('Cuộc gọi này không có bản ghi âm/ghi hình.');
      return;
    }

    let isMounted = true;
    setIsLoading(true);
    setError(null);
    setStreamUrl(null);

    getCallRecordingPresignedUrl(call.callId)
      .then((url) => {
        if (isMounted) {
          setStreamUrl(url);
        }
      })
      .catch((err) => {
        if (isMounted) {
          setError(
            err.response?.data?.message ||
              'Không thể tải liên kết bản ghi từ hệ thống lưu trữ bảo mật.'
          );
        }
      })
      .finally(() => {
        if (isMounted) setIsLoading(false);
      });

    return () => {
      isMounted = false;
    };
  }, [call]);

  const handlePlaybackRateChange = (rate: number) => {
    setPlaybackRate(rate);
    if (videoRef.current) videoRef.current.playbackRate = rate;
    if (audioRef.current) audioRef.current.playbackRate = rate;
  };

  if (!call) return null;

  const isVideo = call.callType === 'VIDEO';

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4 backdrop-blur-sm animate-fadeIn"
      onClick={onClose}
    >
      <div
        className="relative w-full max-w-3xl overflow-hidden rounded-2xl border border-outline-variant/60 bg-surface shadow-2xl transition-all"
        onClick={(e) => e.stopPropagation()}
      >
        {/* Header */}
        <div className="flex items-center justify-between border-b border-outline-variant/50 bg-surface-container-low px-6 py-4">
          <div className="flex items-center gap-3">
            <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-primary-container text-primary">
              <span className="material-symbols-outlined text-2xl">
                {isVideo ? 'videocam' : 'mic'}
              </span>
            </div>
            <div>
              <h3 className="text-base font-bold text-on-surface">
                Bản ghi tư vấn: {isVideo ? 'Cuộc gọi Video' : 'Cuộc gọi Thoại'}
              </h3>
              <p className="text-xs text-on-surface-variant">
                Mã cuộc gọi: <span className="font-mono">{call.callId}</span> • {formatDateTime(call.initiatedAt)}
              </p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="flex h-8 w-8 items-center justify-center rounded-full text-on-surface-variant hover:bg-surface-variant hover:text-on-surface transition-colors cursor-pointer"
          >
            <span className="material-symbols-outlined text-xl">close</span>
          </button>
        </div>

        {/* Body */}
        <div className="space-y-5 p-6">
          {/* Participants Info Cards */}
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            {/* Mother Card */}
            <div className="rounded-xl border border-outline-variant/50 bg-surface-container-lowest p-4">
              <div className="flex items-center gap-2 mb-2">
                <span className="material-symbols-outlined text-pink-600 text-lg">pregnant_woman</span>
                <span className="text-xs font-semibold uppercase tracking-wider text-pink-700">Người Mẹ (Mother)</span>
              </div>
              <p className="font-semibold text-on-surface text-sm">{call.motherName}</p>
              <div className="mt-1 text-xs text-on-surface-variant space-y-0.5">
                {call.motherPhone && <p className="flex items-center gap-1"><span className="material-symbols-outlined text-xs">call</span> {call.motherPhone}</p>}
                {call.motherEmail && <p className="flex items-center gap-1"><span className="material-symbols-outlined text-xs">mail</span> {call.motherEmail}</p>}
              </div>
            </div>

            {/* Expert Card */}
            <div className="rounded-xl border border-outline-variant/50 bg-surface-container-lowest p-4">
              <div className="flex items-center gap-2 mb-2">
                <span className="material-symbols-outlined text-blue-600 text-lg">medical_services</span>
                <span className="text-xs font-semibold uppercase tracking-wider text-blue-700">Chuyên gia y tế (Expert)</span>
              </div>
              <p className="font-semibold text-on-surface text-sm">{call.expertName}</p>
              <div className="mt-1 text-xs text-on-surface-variant space-y-0.5">
                {call.expertSpecialization && <p className="flex items-center gap-1"><span className="material-symbols-outlined text-xs">school</span> Chuyên khoa: {call.expertSpecialization}</p>}
                {call.expertHospital && <p className="flex items-center gap-1"><span className="material-symbols-outlined text-xs">local_hospital</span> {call.expertHospital}</p>}
              </div>
            </div>
          </div>

          {/* Media Player Player Area */}
          <div className="rounded-2xl border border-outline-variant/40 bg-black/95 p-4 text-white">
            {isLoading && (
              <div className="flex h-56 flex-col items-center justify-center gap-3">
                <div className="h-8 w-8 animate-spin rounded-full border-2 border-primary border-t-transparent" />
                <p className="text-sm text-slate-300">Đang chuẩn bị luồng phát trực tiếp bảo mật...</p>
              </div>
            )}

            {error && !isLoading && (
              <div className="flex h-56 flex-col items-center justify-center gap-2 text-center p-4">
                <span className="material-symbols-outlined text-4xl text-amber-400">warning</span>
                <p className="text-sm text-slate-200">{error}</p>
              </div>
            )}

            {streamUrl && !isLoading && !error && (
              <div className="space-y-3">
                {isVideo ? (
                  <div className="overflow-hidden rounded-xl bg-black flex justify-center max-h-[380px]">
                    <video
                      ref={videoRef}
                      src={streamUrl}
                      controls
                      autoPlay
                      className="w-full h-full object-contain rounded-xl max-h-[380px]"
                    />
                  </div>
                ) : (
                  <div className="flex flex-col items-center justify-center py-8 gap-4">
                    <div className="flex h-20 w-20 items-center justify-center rounded-full bg-primary/20 text-primary-light">
                      <span className="material-symbols-outlined text-4xl">headphones</span>
                    </div>
                    <audio
                      ref={audioRef}
                      src={streamUrl}
                      controls
                      autoPlay
                      className="w-full max-w-lg"
                    />
                  </div>
                )}

                {/* Player Controls Bar */}
                <div className="flex flex-wrap items-center justify-between gap-3 pt-2 text-xs text-slate-300 border-t border-slate-800">
                  <div className="flex items-center gap-2">
                    <span className="text-slate-400 font-medium">Tốc độ:</span>
                    {[0.75, 1, 1.25, 1.5, 2].map((rate) => (
                      <button
                        key={rate}
                        type="button"
                        onClick={() => handlePlaybackRateChange(rate)}
                        className={`rounded px-2 py-1 font-semibold transition-colors cursor-pointer ${
                          playbackRate === rate
                            ? 'bg-primary text-white'
                            : 'bg-slate-800 text-slate-300 hover:bg-slate-700'
                        }`}
                      >
                        {rate}x
                      </button>
                    ))}
                  </div>

                  <div className="flex items-center gap-3">
                    <span>
                      Thời lượng:{' '}
                      <strong className="text-white">
                        {formatDuration(call.recordedDurationSeconds ?? call.durationSeconds)}
                      </strong>
                    </span>
                    <a
                      href={streamUrl}
                      download={`call_${call.callId}.${isVideo ? 'mp4' : 'm4a'}`}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="flex items-center gap-1 rounded bg-slate-800 px-3 py-1 text-xs font-medium text-slate-200 hover:bg-slate-700 hover:text-white transition-colors"
                    >
                      <span className="material-symbols-outlined text-sm">download</span>
                      Tải file
                    </a>
                  </div>
                </div>
              </div>
            )}
          </div>

          {/* PDPA Notice Banner */}
          <div className="flex items-start gap-2.5 rounded-xl border border-emerald-200 bg-emerald-50/80 p-3.5 text-xs text-emerald-900">
            <span className="material-symbols-outlined text-emerald-600 text-lg shrink-0">verified_user</span>
            <div>
              <p className="font-semibold text-emerald-950">Tuân thủ Pháp lý & Quyền riêng tư Y tế (PDPA Consent)</p>
              <p className="text-emerald-800 mt-0.5">
                Cuộc gọi đã được xác nhận đồng thuận ghi âm/ghi hình từ cả 2 phía. Bản ghi được mã hóa và lưu trữ tại Cloudflare R2 Private Bucket, chỉ Quản trị viên hệ thống có thẩm quyền giám sát mới được truy cập phát lại.
              </p>
            </div>
          </div>
        </div>

        {/* Footer */}
        <div className="flex items-center justify-end border-t border-outline-variant/50 bg-surface-container-low px-6 py-3.5">
          <button
            type="button"
            onClick={onClose}
            className="rounded-lg border border-outline-variant bg-surface px-4 py-2 text-xs font-semibold text-on-surface hover:bg-surface-variant transition-colors cursor-pointer"
          >
            Đóng
          </button>
        </div>
      </div>
    </div>
  );
}
