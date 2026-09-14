import React, { useEffect, useState } from 'react';
import {
  type BabyGrowthPoint,
  type BabyGrowthShareData,
  fetchBabyGrowthChart,
  formatBabyAgeAt,
  formatIsoDateVi,
} from '../../expert/services/expertSharedRecordsService';
import { BabyGrowthTrendChart } from './BabyGrowthTrendChart';

interface Props {
  data: BabyGrowthShareData;
  isOwn?: boolean;
}

type LoadStatus = 'loading' | 'loaded' | 'failed';

const SERIES = [
  {
    key: 'weight',
    title: 'Xu hướng cân nặng',
    unit: 'kg',
    empty: 'Chưa có dữ liệu cân nặng.',
    color: '#C98C7B',
    pick: (p: BabyGrowthPoint) => p.weightKg,
  },
  {
    key: 'height',
    title: 'Xu hướng chiều cao',
    unit: 'cm',
    empty: 'Chưa có dữ liệu chiều cao.',
    color: '#5B8E7D',
    pick: (p: BabyGrowthPoint) => p.heightCm,
  },
  {
    key: 'head',
    title: 'Xu hướng vòng đầu',
    unit: 'cm',
    empty: 'Chưa có dữ liệu vòng đầu.',
    color: '#D48B47',
    pick: (p: BabyGrowthPoint) => p.headCircumferenceCm,
  },
] as const;

const formatValue = (value: number | null | undefined, unit: string) =>
  value == null ? '—' : `${value.toFixed(1)} ${unit}`;

const formatValues = (p: {
  weightKg?: number | null;
  heightCm?: number | null;
  headCircumferenceCm?: number | null;
}) => `${formatValue(p.weightKg, 'kg')} · ${formatValue(p.heightCm, 'cm')} · ${formatValue(p.headCircumferenceCm, 'cm')}`;

const todayIso = () => {
  const now = new Date();
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`;
};

export const SharedBabyGrowthBubble: React.FC<Props> = ({ data }) => {
  const [points, setPoints] = useState<BabyGrowthPoint[]>([]);
  const [status, setStatus] = useState<LoadStatus>('loading');
  const [historyExpanded, setHistoryExpanded] = useState(false);

  useEffect(() => {
    let cancelled = false;
    setStatus('loading');
    fetchBabyGrowthChart(data.babyId)
      .then((result) => {
        if (cancelled) return;
        setPoints(result);
        setStatus('loaded');
      })
      .catch(() => {
        if (!cancelled) setStatus('failed');
      });
    return () => {
      cancelled = true;
    };
  }, [data.babyId]);

  const count = status === 'loaded' ? points.length : data.measurementCount;
  const newestFirst = [...points].reverse();

  return (
    <div className="w-[320px] max-w-full rounded-2xl bg-surface border border-outline-variant/70 shadow-sm overflow-hidden text-on-surface">
      {/* Header */}
      <div className="bg-primary/10 border-b border-outline-variant/50 p-3.5 flex items-center gap-3">
        <div className="w-9 h-9 rounded-xl bg-primary text-white flex items-center justify-center shadow-xs shrink-0">
          <span className="material-symbols-outlined text-lg" aria-hidden="true">
            child_care
          </span>
        </div>
        <div className="min-w-0 flex-1">
          <div className="flex items-center justify-between gap-1">
            <h4 className="m-0 text-xs font-bold text-on-surface truncate">{data.title}</h4>
            {data.isLiveSync !== false && (
              <span className="inline-flex items-center gap-0.5 px-1.5 py-0.2 rounded bg-emerald-100 text-emerald-800 text-[9px] font-bold shrink-0">
                <span className="w-1.5 h-1.5 rounded-full bg-emerald-600 animate-pulse" />
                Live
              </span>
            )}
          </div>
          <p className="m-0 text-[11px] text-on-surface-variant">
            {[data.babyNickname, formatBabyAgeAt(data.birthDate, todayIso()), `${count} lần đo`]
              .filter(Boolean)
              .join(' · ')}
          </p>
        </div>
      </div>

      <div className="p-3 space-y-2">
        {status === 'loading' && (
          <div className="py-8 flex justify-center">
            <div className="w-6 h-6 border-2 border-primary border-t-transparent rounded-full animate-spin" />
          </div>
        )}

        {status === 'failed' && (
          <div className="p-3 rounded-xl bg-amber-50 border border-amber-200 text-xs space-y-1">
            <p className="m-0 font-semibold text-amber-800">Không thể tải dữ liệu tăng trưởng mới nhất</p>
            {data.latest && (
              <>
                <p className="m-0 text-on-surface-variant">
                  Lần đo gần nhất khi chia sẻ: {formatIsoDateVi(data.latest.measuredDate)}
                </p>
                <p className="m-0 font-bold text-on-surface">{formatValues(data.latest)}</p>
              </>
            )}
          </div>
        )}

        {status === 'loaded' && (
          <>
            {SERIES.map((series) => {
              const seriesPoints = points
                .filter((p) => series.pick(p) != null)
                .map((p) => ({ date: p.measuredDate, value: Number(series.pick(p)) }));
              const first = seriesPoints[0];
              const last = seriesPoints[seriesPoints.length - 1];
              return (
                <div
                  key={series.key}
                  className="rounded-xl bg-surface-container-low/60 border border-outline-variant/40 p-2.5"
                >
                  <p className="m-0 text-xs font-semibold text-on-surface">{series.title}</p>
                  {seriesPoints.length === 0 ? (
                    <p className="m-0 py-3 text-center text-[11px] text-on-surface-variant">{series.empty}</p>
                  ) : (
                    <>
                      <BabyGrowthTrendChart points={seriesPoints} unit={series.unit} color={series.color} />
                      <p className="m-0 text-[11px] text-on-surface-variant">
                        {seriesPoints.length === 1
                          ? `${first.value.toFixed(1)} ${series.unit}`
                          : `${first.value.toFixed(1)} ${series.unit} – ${last.value.toFixed(1)} ${series.unit}`}
                      </p>
                    </>
                  )}
                </div>
              );
            })}

            {points.length > 0 && (
              <>
                <button
                  type="button"
                  onClick={() => setHistoryExpanded((prev) => !prev)}
                  className="inline-flex items-center gap-1 px-2 py-1 rounded-lg text-xs font-semibold text-primary hover:bg-primary/10 cursor-pointer transition-colors"
                >
                  <span className="material-symbols-outlined text-base" aria-hidden="true">
                    {historyExpanded ? 'expand_less' : 'expand_more'}
                  </span>
                  {historyExpanded ? 'Thu gọn lịch sử' : `Xem lịch sử đo (${points.length})`}
                </button>

                {historyExpanded && (
                  <div className="rounded-xl bg-surface border border-outline-variant/40 px-3 py-2 space-y-1.5">
                    {newestFirst.map((p, idx) => (
                      <div
                        key={p.growthMeasurementId ?? `${p.measuredDate}-${idx}`}
                        data-testid="baby-growth-history-row"
                        className="flex flex-col py-1 border-b border-outline-variant/20 last:border-0 text-[11px]"
                      >
                        <span className="text-on-surface-variant">
                          {`${formatIsoDateVi(p.measuredDate)} · ${formatBabyAgeAt(data.birthDate, p.measuredDate)}`}
                        </span>
                        <span className="font-semibold text-on-surface">{formatValues(p)}</span>
                      </div>
                    ))}
                  </div>
                )}
              </>
            )}
          </>
        )}
      </div>

      {data.note && (
        <div className="m-3 mt-0 p-2.5 rounded-xl bg-surface-container-low/40 border border-outline-variant/40 flex items-start gap-2 text-xs">
          <span className="material-symbols-outlined text-sm text-outline shrink-0 mt-0.5" aria-hidden="true">
            chat_bubble
          </span>
          <p className="m-0 italic text-on-surface-variant">{data.note}</p>
        </div>
      )}
    </div>
  );
};
