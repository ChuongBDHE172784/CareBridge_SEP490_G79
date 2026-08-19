import React from 'react';
import {
  type ChecklistShareData,
  getTaskOriginCategory,
} from '../../expert/services/expertSharedRecordsService';

interface Props {
  data: ChecklistShareData;
  isOwn?: boolean;
}

export const SharedChecklistBubble: React.FC<Props> = ({ data }) => {
  const percent = data.progressPercent ?? 0;
  const displayItems = data.currentItems || data.items || [...(data.historyItems || []), ...(data.futureItems || [])];

  return (
    <div className="w-[320px] max-w-full rounded-2xl bg-surface border border-outline-variant/70 shadow-sm overflow-hidden text-on-surface">
      {/* Header */}
      <div className="bg-primary/10 border-b border-outline-variant/50 p-3.5 flex items-center gap-3">
        <div className="w-9 h-9 rounded-xl bg-primary text-white flex items-center justify-center shadow-xs shrink-0">
          <span className="material-symbols-outlined text-lg">checklist_rtl</span>
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
            {[data.gestationalWeek ? `Tuần thai ${data.gestationalWeek}` : null, `Đã hoàn thành ${data.completedCount}/${data.totalCount} việc`].filter(Boolean).join(' · ')}
          </p>
        </div>
      </div>

      {/* Badges */}
      <div className="px-3.5 pt-2 flex flex-wrap gap-1.5">
        {data.historyItems && data.historyItems.length > 0 && (
          <span className="px-2 py-0.5 rounded-md text-[9px] font-bold bg-emerald-100 text-emerald-800">
            Đã làm: {data.historyItems.length}
          </span>
        )}
        {data.currentItems && data.currentItems.length > 0 && (
          <span className="px-2 py-0.5 rounded-md text-[9px] font-bold bg-sky-100 text-sky-800">
            Hiện tại: {data.currentItems.length}
          </span>
        )}
        {data.futureItems && data.futureItems.length > 0 && (
          <span className="px-2 py-0.5 rounded-md text-[9px] font-bold bg-purple-100 text-purple-800">
            Tương lai: {data.futureItems.length}
          </span>
        )}
      </div>

      {/* Progress Bar */}
      <div className="px-3.5 pt-2 pb-1">
        <div className="flex items-center justify-between text-xs mb-1.5">
          <span className="font-semibold text-on-surface-variant text-[11px]">Tiến độ chăm sóc</span>
          <span className="font-bold text-primary text-xs">{percent}%</span>
        </div>
        <div className="w-full h-2 rounded-full bg-surface-container-highest overflow-hidden">
          <div
            className="h-full bg-primary rounded-full transition-all duration-300"
            style={{ width: `${Math.min(100, Math.max(0, percent))}%` }}
          />
        </div>
      </div>

      {/* Checklist Items */}
      <div className="p-3 space-y-1.5 max-h-64 overflow-y-auto">
        {displayItems.map((item, idx) => {
          const originCat = getTaskOriginCategory(item);
          return (
            <div
              key={idx}
              className={`p-2 rounded-xl flex items-start gap-2 text-xs border transition-colors ${
                item.completed
                  ? 'bg-emerald-50/60 border-emerald-200/50 text-emerald-950'
                  : 'bg-surface-container-low/50 border-outline-variant/40 text-on-surface'
              }`}
            >
              <span
                className={`material-symbols-outlined text-base shrink-0 mt-0.5 ${
                  item.completed ? 'text-emerald-600' : 'text-outline'
                }`}
              >
                {item.completed ? 'check_circle' : 'radio_button_unchecked'}
              </span>
              <div className="min-w-0 flex-1">
                <div className="flex items-center justify-between gap-1">
                  <div className="flex items-center gap-1.5 flex-wrap">
                    <p className={`m-0 text-xs leading-snug ${item.completed ? 'font-medium' : 'text-on-surface'}`}>
                      {item.text}
                    </p>
                    {originCat === 'USER' ? (
                      <span className="inline-flex items-center gap-0.5 px-1.5 py-0.2 rounded text-[8px] font-bold bg-purple-100 text-purple-800 border border-purple-200 shrink-0">
                        <span className="material-symbols-outlined text-[10px] text-purple-600">person</span>
                        Việc cá nhân
                      </span>
                    ) : (
                      <span className="inline-flex items-center gap-0.5 px-1.5 py-0.2 rounded text-[8px] font-bold bg-sky-100 text-sky-800 border border-sky-200 shrink-0">
                        <span className="material-symbols-outlined text-[10px] text-sky-600">auto_awesome</span>
                        Gợi ý CareBridge
                      </span>
                    )}
                  </div>
                  <span
                    className={`px-1.5 py-0.2 text-[8px] font-bold rounded shrink-0 ${
                      item.completed ? 'bg-emerald-100 text-emerald-800' : 'bg-amber-100 text-amber-800'
                    }`}
                  >
                    {item.completed ? 'Xong' : 'Chờ làm'}
                  </span>
                </div>
                <div className="flex flex-wrap items-center gap-1.5 mt-1">
                  {(item.timeLabel || item.category) && (
                    <span className="px-1.5 py-0.2 text-[9px] font-semibold rounded bg-surface-container-highest text-on-surface-variant">
                      {item.timeLabel || item.category}
                    </span>
                  )}
                  {item.doctorNote && (
                    <span className="text-[9px] text-teal-700 italic flex items-center gap-0.5">
                      <span className="material-symbols-outlined text-[10px]">chat</span>
                      {item.doctorNote}
                    </span>
                  )}
                  {item.sourceUrl && (
                    <a
                      href={item.sourceUrl}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="text-[9px] text-primary hover:underline font-semibold flex items-center gap-0.5"
                    >
                      <span className="material-symbols-outlined text-[10px]">link</span>
                      Nguồn
                    </a>
                  )}
                </div>
              </div>
            </div>
          );
        })}
      </div>

      {/* Note if present */}
      {data.note && (
        <div className="m-3 mt-0 p-2.5 rounded-xl bg-surface-container-low/40 border border-outline-variant/40 flex items-start gap-2 text-xs">
          <span className="material-symbols-outlined text-sm text-outline shrink-0 mt-0.5">chat_bubble</span>
          <p className="m-0 italic text-on-surface-variant">{data.note}</p>
        </div>
      )}
    </div>
  );
};
