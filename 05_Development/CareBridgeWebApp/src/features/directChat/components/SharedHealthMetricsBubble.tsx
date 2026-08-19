import React, { useEffect, useState } from 'react';
import {
  type HealthMetricsShareData,
  syncLiveHealthMetrics,
} from '../../expert/services/expertSharedRecordsService';

interface Props {
  data: HealthMetricsShareData;
  isOwn?: boolean;
}

export const SharedHealthMetricsBubble: React.FC<Props> = ({ data }) => {
  const [liveData, setLiveData] = useState<HealthMetricsShareData>(data);
  const [expandedCodes, setExpandedCodes] = useState<Set<string>>(new Set());

  useEffect(() => {
    setLiveData(data);
    if (data.isLiveSync !== false && data.journeyId) {
      syncLiveHealthMetrics(data).then((updated) => {
        setLiveData(updated);
      });
    }
  }, [data]);

  const toggleExpand = (code: string) => {
    setExpandedCodes((prev) => {
      const next = new Set(prev);
      if (next.has(code)) {
        next.delete(code);
      } else {
        next.add(code);
      }
      return next;
    });
  };

  const getStatusBadge = (status: string) => {
    switch (status) {
      case 'CRITICAL':
        return <span className="px-1.5 py-0.5 text-[10px] font-bold rounded bg-rose-100 text-rose-700">Nguy hiểm</span>;
      case 'WARNING':
        return <span className="px-1.5 py-0.5 text-[10px] font-bold rounded bg-amber-100 text-amber-800">Cần lưu ý</span>;
      default:
        return <span className="px-1.5 py-0.5 text-[10px] font-bold rounded bg-emerald-100 text-emerald-700">Bình thường</span>;
    }
  };

  const getMetricIcon = (code: string) => {
    switch (code) {
      case 'BLOOD_PRESSURE':
        return 'favorite';
      case 'BLOOD_GLUCOSE':
        return 'water_drop';
      case 'BMI':
      case 'WEIGHT':
        return 'monitor_weight';
      case 'MATERNAL_HEART_RATE':
      case 'HEART_RATE':
        return 'monitor_heart';
      case 'TEMPERATURE':
        return 'thermostat';
      case 'FETAL_MOVEMENT':
      case 'FETAL_MOVEMENT_SESSION':
        return 'child_care';
      case 'SPO2':
        return 'air';
      default:
        return 'health_and_safety';
    }
  };

  const totalPoints = liveData.metrics.reduce((acc, m) => acc + (m.history?.length || 1), 0);

  return (
    <div className="w-[320px] max-w-full rounded-2xl bg-surface border border-outline-variant/70 shadow-sm overflow-hidden text-on-surface">
      {/* Header */}
      <div className="bg-primary/10 border-b border-outline-variant/50 p-3.5 flex items-center gap-3">
        <div className="w-9 h-9 rounded-xl bg-primary text-white flex items-center justify-center shadow-xs shrink-0">
          <span className="material-symbols-outlined text-lg">monitor_heart</span>
        </div>
        <div className="min-w-0 flex-1">
          <div className="flex items-center justify-between gap-1">
            <h4 className="m-0 text-xs font-bold text-on-surface truncate">{liveData.title}</h4>
            {liveData.isLiveSync !== false && (
              <span className="inline-flex items-center gap-0.5 px-1.5 py-0.2 rounded bg-emerald-100 text-emerald-800 text-[9px] font-bold shrink-0">
                <span className="w-1.5 h-1.5 rounded-full bg-emerald-600 animate-pulse" />
                Live
              </span>
            )}
          </div>
          <p className="m-0 text-[11px] text-on-surface-variant">
            {[
              liveData.gestationalWeek ? `Tuần thai ${liveData.gestationalWeek}` : null,
              liveData.timeRangeLabel ? liveData.timeRangeLabel : null,
              `${totalPoints} bản ghi`,
            ]
              .filter(Boolean)
              .join(' · ')}
          </p>
        </div>
      </div>

      {/* Metrics List */}
      <div className="p-3 space-y-2">
        {liveData.metrics.map((metric) => {
          const hasHistory = (metric.history && metric.history.length > 1);
          const isExpanded = expandedCodes.has(metric.code);

          return (
            <div
              key={metric.code}
              className="rounded-xl bg-surface-container-low/60 border border-outline-variant/40 overflow-hidden transition-colors"
            >
              <div
                onClick={() => hasHistory && toggleExpand(metric.code)}
                className={`p-2.5 flex items-center justify-between gap-2 ${hasHistory ? 'cursor-pointer hover:bg-surface-container-low' : ''}`}
              >
                <div className="flex items-center gap-2 min-w-0">
                  <span className="material-symbols-outlined text-base text-primary shrink-0">{getMetricIcon(metric.code)}</span>
                  <div className="min-w-0">
                    <p className="m-0 text-xs font-semibold text-on-surface truncate">{metric.name}</p>
                    {metric.measuredTime && (
                      <p className="m-0 text-[10px] text-on-surface-variant">Gần nhất: {metric.measuredTime}</p>
                    )}
                  </div>
                </div>

                <div className="flex items-center gap-1.5 shrink-0">
                  <span className="text-xs font-bold text-on-surface">
                    {metric.value} <span className="text-[10px] font-normal text-on-surface-variant">{metric.unit}</span>
                  </span>
                  {getStatusBadge(metric.status)}
                  {hasHistory && (
                    <span className="material-symbols-outlined text-sm text-outline">
                      {isExpanded ? 'expand_less' : 'expand_more'}
                    </span>
                  )}
                </div>
              </div>

              {/* Collapsible History Table */}
              {hasHistory && isExpanded && (
                <div className="px-3 py-2 bg-surface border-t border-outline-variant/30 text-[11px] space-y-1.5">
                  <div className="text-[10px] font-bold text-on-surface-variant uppercase tracking-wider mb-1">
                    Lịch sử các lần đo ({metric.history!.length}):
                  </div>
                  {metric.history!.map((rec, idx) => (
                    <div key={idx} className="flex items-center justify-between py-1 border-b border-outline-variant/20 last:border-0">
                      <span className="text-on-surface-variant">{rec.measuredAt}</span>
                      <div className="flex items-center gap-1.5">
                        <span className="font-semibold text-on-surface">
                          {rec.value} {rec.unit}
                        </span>
                        {getStatusBadge(rec.status)}
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          );
        })}
      </div>

      {/* Note if present */}
      {liveData.note && (
        <div className="m-3 mt-0 p-2.5 rounded-xl bg-surface-container-low/40 border border-outline-variant/40 flex items-start gap-2 text-xs">
          <span className="material-symbols-outlined text-sm text-outline shrink-0 mt-0.5">chat_bubble</span>
          <p className="m-0 italic text-on-surface-variant">{liveData.note}</p>
        </div>
      )}
    </div>
  );
};
