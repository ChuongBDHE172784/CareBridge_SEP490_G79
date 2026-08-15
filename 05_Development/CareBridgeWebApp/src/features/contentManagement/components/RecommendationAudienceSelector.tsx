import { useState, useMemo } from 'react';
import type { RecommendationTag } from '../models/content';
import {
  TAG_GROUPS,
  getGroupForTag,
  formatRecommendationTagLabel,
  formatTagOptionLabel,
} from '../pages/recommendationMetadata';

interface RecommendationAudienceSelectorProps {
  catalog: RecommendationTag[];
  selectedTagIds: string[];
  onChange: (newTagIds: string[]) => void;
  staleTagIds?: string[];
  disabled?: boolean;
}

export default function RecommendationAudienceSelector({
  catalog,
  selectedTagIds,
  onChange,
  staleTagIds = [],
  disabled = false,
}: RecommendationAudienceSelectorProps) {
  const [selectedGroupId, setSelectedGroupId] = useState<string>('BMI');
  const [selectedTagId, setSelectedTagId] = useState<string>('');

  // Group tags by group ID
  const tagsByGroup = useMemo(() => {
    const map = new Map<string, RecommendationTag[]>();
    for (const group of TAG_GROUPS) {
      map.set(group.id, []);
    }
    for (const tag of catalog) {
      const group = getGroupForTag(tag);
      if (group) {
        map.get(group.id)?.push(tag);
      }
    }
    return map;
  }, [catalog]);

  const availableTagsInSelectedGroup = tagsByGroup.get(selectedGroupId) ?? [];

  const handleAddTag = () => {
    if (!selectedTagId || disabled) return;
    const tagToAdd = catalog.find((t) => t.id === selectedTagId);
    if (!tagToAdd) return;

    const group = getGroupForTag(tagToAdd);
    const isExclusiveGroup = [
      'rec-age-',
      'rec-bmi-',
      'rec-smoking-',
      'rec-alcohol-',
      'rec-activity-',
      'rec-sleep-',
      'rec-sti-screening-information',
      'rec-sti-past-history',
      'rec-sti-current-or-treatment',
      'rec-sti-risk',
      'rec-sti-suspected-or-known',
    ].some((prefix) => tagToAdd.slug.startsWith(prefix));

    let nextIds = [...selectedTagIds];

    if (isExclusiveGroup && group) {
      // Remove any existing tag in the same exclusive group
      nextIds = nextIds.filter((id) => {
        const existing = catalog.find((t) => t.id === id);
        return !existing || getGroupForTag(existing)?.id !== group.id;
      });
    }

    if (!nextIds.includes(selectedTagId)) {
      nextIds.push(selectedTagId);
    }

    onChange(nextIds);
    setSelectedTagId('');
  };

  const handleRemoveTag = (tagIdToRemove: string) => {
    if (disabled) return;
    onChange(selectedTagIds.filter((id) => id !== tagIdToRemove));
  };

  const handleClearAll = () => {
    if (disabled) return;
    onChange([]);
  };

  return (
    <div className="space-y-4">
      {/* 2 Select Fields Row */}
      <div className="grid grid-cols-1 sm:grid-cols-12 gap-3 items-end">
        <div className="sm:col-span-5">
          <label htmlFor="rec-group-select" className="block text-xs font-semibold text-outline mb-1.5">
            Nhóm tiêu chí
          </label>
          <select
            id="rec-group-select"
            value={selectedGroupId}
            onChange={(e) => {
              setSelectedGroupId(e.target.value);
              setSelectedTagId('');
            }}
            disabled={disabled}
            className="w-full py-2.5 px-3 rounded-xl border border-outline-variant bg-surface text-sm text-on-surface focus:border-primary focus:outline-none"
          >
            {TAG_GROUPS.map((g) => {
              const count = tagsByGroup.get(g.id)?.length ?? 0;
              return (
                <option key={g.id} value={g.id}>
                  {g.name} ({count})
                </option>
              );
            })}
          </select>
        </div>

        <div className="sm:col-span-5">
          <label htmlFor="rec-tag-select" className="block text-xs font-semibold text-outline mb-1.5">
            Giá trị cụ thể
          </label>
          <select
            id="rec-tag-select"
            value={selectedTagId}
            onChange={(e) => setSelectedTagId(e.target.value)}
            disabled={disabled || availableTagsInSelectedGroup.length === 0}
            className="w-full py-2.5 px-3 rounded-xl border border-outline-variant bg-surface text-sm text-on-surface focus:border-primary focus:outline-none"
          >
            <option value="">-- Chọn giá trị --</option>
            {availableTagsInSelectedGroup.map((tag) => {
              const isSelected = selectedTagIds.includes(tag.id);
              return (
                <option key={tag.id} value={tag.id}>
                  {formatTagOptionLabel(tag)} {isSelected ? '(Đã chọn)' : ''}
                </option>
              );
            })}
          </select>
        </div>

        <div className="sm:col-span-2">
          <button
            type="button"
            onClick={handleAddTag}
            disabled={disabled || !selectedTagId}
            className="w-full py-2.5 px-4 rounded-xl bg-primary text-on-primary text-sm font-semibold hover:bg-primary/90 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
          >
            + Thêm
          </button>
        </div>
      </div>

      {/* Selected tags display */}
      <div className="rounded-xl border border-outline-variant/60 bg-surface p-3">
        <div className="flex items-center justify-between mb-2">
          <span className="text-[11px] font-semibold uppercase tracking-wider text-outline">
            Các tag đối tượng đã chọn ({selectedTagIds.length + staleTagIds.length})
          </span>
          {(selectedTagIds.length > 0 || staleTagIds.length > 0) && (
            <button
              type="button"
              onClick={handleClearAll}
              disabled={disabled}
              className="text-xs text-error hover:underline font-medium"
            >
              Xoá tất cả
            </button>
          )}
        </div>

        {selectedTagIds.length === 0 && staleTagIds.length === 0 ? (
          <p className="text-xs text-outline italic py-1">
            Chưa chọn tag đối tượng nào (Bài viết sẽ áp dụng cho tất cả người dùng trong giai đoạn).
          </p>
        ) : (
          <div className="flex flex-wrap gap-2 pt-1">
            {/* Stale tags */}
            {staleTagIds.map((tagId) => (
              <span
                key={tagId}
                className="inline-flex items-center gap-1.5 rounded-full border border-error/50 bg-error-container px-3 py-1.5 text-xs text-error font-medium"
              >
                Mã không hợp lệ: {tagId}
                <button
                  type="button"
                  aria-label={`Remove stale recommendation audience ${tagId}`}
                  onClick={() => onChange(selectedTagIds.filter((id) => id !== tagId))}
                  disabled={disabled}
                  className="font-bold hover:opacity-75 ml-0.5"
                >
                  ×
                </button>
              </span>
            ))}

            {/* Active selected tags */}
            {selectedTagIds.map((tagId) => {
              const tag = catalog.find((t) => t.id === tagId);
              if (!tag) return null;
              const group = getGroupForTag(tag);
              return (
                <span
                  key={tag.id}
                  className="inline-flex items-center gap-1.5 rounded-full border border-primary/40 bg-primary-container/30 px-3 py-1.5 text-xs text-primary font-medium shadow-sm"
                >
                  {group && (
                    <span className="font-semibold text-primary/70 mr-0.5">
                      {group.name}:
                    </span>
                  )}
                  <span>{formatTagOptionLabel(tag)}</span>
                  <button
                    type="button"
                    aria-label={`Xoá tag ${formatRecommendationTagLabel(tag)}`}
                    onClick={() => handleRemoveTag(tag.id)}
                    disabled={disabled}
                    className="font-bold text-primary hover:text-error hover:bg-surface rounded-full w-4 h-4 inline-flex items-center justify-center transition-colors ml-1"
                  >
                    ×
                  </button>
                </span>
              );
            })}
          </div>
        )}
      </div>
    </div>
  );
}
