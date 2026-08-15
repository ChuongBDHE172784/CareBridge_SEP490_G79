import { useEffect, useState, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { fetchRecommendationTags, fetchStaffContentDetail, fetchTags, updateContent, uploadContentImage } from '../services/contentApi';
import type { CommunityTopic, ContentDetail, RecommendationTag } from '../models/content';
import { STAGE_LABELS, TYPE_LABELS } from '../models/content';
import RichTextEditor from '../components/RichTextEditor';
import ReviewFeedbackNotice from '../components/ReviewFeedbackNotice';
import {
  recommendationApiErrorCode,
  recommendationApiErrorMessage,
  recommendationClassification,
  recommendationMetadataError,
  recommendationWindowLabel,
} from './recommendationMetadata';

export default function EditContentPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();

  const [detail, setDetail] = useState<ContentDetail | null>(null);
  const [title, setTitle] = useState('');
  const [body, setBody] = useState('');
  const [summary, setSummary] = useState('');
  const [tags, setTags] = useState<CommunityTopic[]>([]);
  const [recommendationTags, setRecommendationTags] = useState<RecommendationTag[]>([]);
  const [tagIds, setTagIds] = useState<string[]>([]);
  const [recommendationTagIds, setRecommendationTagIds] = useState<string[]>([]);
  const [eligibleFromWeek, setEligibleFromWeek] = useState('');
  const [eligibleToWeek, setEligibleToWeek] = useState('');
  const [recommendationPriority, setRecommendationPriority] = useState('0');
  const [sourceTitle, setSourceTitle] = useState('');
  const [sourceUrl, setSourceUrl] = useState('');
  const [changeSummary, setChangeSummary] = useState('');
  const [isLoading, setIsLoading] = useState(true);
  const [loadError, setLoadError] = useState('');
  const [submitting, setSubmitting] = useState<'draft' | 'submit' | null>(null);
  const [submitError, setSubmitError] = useState('');

  const loadDetail = useCallback(async () => {
    if (!id) return;
    setIsLoading(true);
    setLoadError('');
    try {
      const [data, loadedTags] = await Promise.all([fetchStaffContentDetail(id), fetchTags()]);
      const catalog = data.type === 'ARTICLE'
        ? await fetchRecommendationTags().catch(() => ({ catalogVersion: '', items: [] as RecommendationTag[] }))
        : { catalogVersion: '', items: [] as RecommendationTag[] };
      setDetail(data);
      setTitle(data.title);
      setBody(data.body);
      setSummary(data.summary ?? '');
      setTags(loadedTags.filter((tag) => !tag.slug.startsWith('rec-')));
      setRecommendationTags(catalog.items);
      const ordinaryTagIds = new Set(
        loadedTags.filter((tag) => !tag.slug.startsWith('rec-')).map((tag) => tag.id),
      );
      // Preserve any unknown non-ordinary ID in the recommendation selection.
      // A stale/retired catalog must stay visible as an unsaved draft until the
      // backend rejects it and the catalog can be refreshed; it must not be
      // silently dropped by the edit form.
      setTagIds((data.tagIds ?? []).filter((tagId) => ordinaryTagIds.has(tagId)));
      setRecommendationTagIds((data.tagIds ?? []).filter((tagId) => !ordinaryTagIds.has(tagId)));
      setEligibleFromWeek(data.eligibleFromWeek == null ? '' : String(data.eligibleFromWeek));
      setEligibleToWeek(data.eligibleToWeek == null ? '' : String(data.eligibleToWeek));
      setRecommendationPriority(String(data.recommendationPriority ?? 0));
      setSourceTitle(data.sources?.[0]?.title ?? '');
      setSourceUrl(data.sources?.[0]?.url ?? '');
    } catch {
      setLoadError(
        'Không thể tải nội dung để chỉnh sửa. Vui lòng thử lại hoặc kiểm tra quyền Content Admin.',
      );
    } finally {
      setIsLoading(false);
    }
  }, [id]);

  useEffect(() => { loadDetail(); }, [loadDetail]);

  const displayFrom = eligibleFromWeek === '' ? null : Number(eligibleFromWeek);
  const displayTo = eligibleToWeek === '' ? null : Number(eligibleToWeek);
  const displayPriority = Number(recommendationPriority);
  const knownRecommendationTagIds = new Set(recommendationTags.map((tag) => tag.id));
  const staleRecommendationTagIds = recommendationTagIds.filter((tagId) => !knownRecommendationTagIds.has(tagId));
  const displayMetadataError = detail
    ? recommendationMetadataError({
      type: detail.type,
      stage: detail.stage,
      from: detail.type === 'ARTICLE' && detail.stage === 'PREGNANCY' ? displayFrom : null,
      to: detail.type === 'ARTICLE' && detail.stage === 'PREGNANCY' ? displayTo : null,
      priority: detail.type === 'ARTICLE' ? displayPriority : 0,
      selectedTagIds: recommendationTagIds,
      catalog: recommendationTags,
    })
    : null;

  const submit = async (status: 'DRAFT' | 'PENDING_REVIEW') => {
    if (!detail || !id) return;
    setSubmitting(status === 'PENDING_REVIEW' ? 'submit' : 'draft');
    setSubmitError('');
    const from = eligibleFromWeek === '' ? null : Number(eligibleFromWeek);
    const to = eligibleToWeek === '' ? null : Number(eligibleToWeek);
    const priority = Number(recommendationPriority);
    const metadataError = recommendationMetadataError({
      type: detail.type,
      stage: detail.stage,
      from: detail.type === 'ARTICLE' && detail.stage === 'PREGNANCY' ? from : null,
      to: detail.type === 'ARTICLE' && detail.stage === 'PREGNANCY' ? to : null,
      priority: detail.type === 'ARTICLE' ? priority : 0,
      selectedTagIds: recommendationTagIds,
      catalog: recommendationTags,
    });
    if (metadataError) {
      setSubmitError('Khoảng tuần phải cùng xuất hiện trong 0-42 và priority phải từ 0 đến 100.');
      setSubmitting(null);
      if (metadataError) setSubmitError(metadataError);
      return;
    }
    try {
      await updateContent(id, {
        title: title.trim(),
        body,
        summary: summary.trim() || undefined,
        stage: detail.stage,
        topicId: detail.topicId || undefined,
        tagIds: [...tagIds, ...recommendationTagIds],
        eligibleFromWeek: detail.type === 'ARTICLE' && detail.stage === 'PREGNANCY' ? from : null,
        eligibleToWeek: detail.type === 'ARTICLE' && detail.stage === 'PREGNANCY' ? to : null,
        recommendationPriority: detail.type === 'ARTICLE' ? priority : 0,
        status,
        sourceLabel: detail.sourceLabel || undefined,
        sources: sourceTitle.trim() ? [{ title: sourceTitle.trim(), url: sourceUrl.trim() || undefined }] : undefined,
      });
      navigate(`/content/${id}`);
    } catch (error) {
      if (recommendationApiErrorCode(error)) {
        if (detail.type === 'ARTICLE') {
          try {
            const catalog = await fetchRecommendationTags();
            setRecommendationTags(catalog.items);
          } catch {
            // Keep the current draft when the catalog itself is unavailable.
          }
        }
        setSubmitError(recommendationApiErrorMessage(error));
        return;
      }
      setSubmitError('Cập nhật thất bại. Vui lòng thử lại.');
    } finally {
      setSubmitting(null);
    }
  };

  if (isLoading) {
    return <div className="p-8 font-sans text-center text-outline py-16">Đang tải...</div>;
  }

  if (loadError || !detail) {
    return (
      <div className="p-8 font-sans max-w-[700px]">
        <div role="alert" tabIndex={-1} className="bg-error-container rounded-2xl p-6 text-error text-sm mb-4">{loadError}</div>
        <button
          onClick={() => navigate('/content/list')}
          className="py-2.5 px-6 rounded-full border border-outline-variant bg-transparent text-primary text-sm font-semibold cursor-pointer"
        >
          Quay lại danh sách
        </button>
      </div>
    );
  }

  return (
    <div className="p-8 font-sans">
      <button
        onClick={() => navigate(-1)}
        className="inline-flex items-center gap-1.5 py-2 px-5 rounded-full border border-outline-variant bg-transparent text-primary text-sm font-semibold cursor-pointer mb-6"
      >
        <span className="material-symbols-outlined text-lg">arrow_back</span>
        Quay lại
      </button>

      <div className="flex items-center justify-between mb-6">
        <div>
          <div className="flex items-center gap-2">
            <h1 className="text-2xl font-bold text-on-surface m-0">Chỉnh sửa {TYPE_LABELS[detail.type]}</h1>
            <span className="py-1 px-3 rounded-full bg-surface-container text-primary text-xs font-semibold">
              Version v{detail.version}
            </span>
          </div>
          <p className="text-sm text-outline mt-1">Cập nhật nội dung ID: {detail.id.slice(0, 8).toUpperCase()}</p>
        </div>
        <div className="flex gap-2.5">
          <button
            onClick={() => submit('DRAFT')}
            disabled={submitting !== null}
            className="flex items-center gap-1.5 py-2.5 px-5 rounded-full bg-surface-container text-primary border-0 text-sm font-semibold cursor-pointer disabled:opacity-40"
          >
            <span className="material-symbols-outlined text-lg">save</span>
            {submitting === 'draft' ? 'Đang lưu...' : 'Lưu nháp'}
          </button>
          <button
            onClick={() => submit('PENDING_REVIEW')}
            disabled={submitting !== null}
            className="flex items-center gap-1.5 py-2.5 px-5 rounded-full bg-primary text-on-primary border-0 text-sm font-semibold cursor-pointer disabled:opacity-40"
          >
            <span className="material-symbols-outlined text-lg">play_arrow</span>
            {submitting === 'submit' ? 'Đang gửi...' : 'Gửi duyệt'}
          </button>
        </div>
      </div>

      {submitError && <div role="alert" tabIndex={-1} className="bg-error-container rounded-2xl p-4 mb-4 text-error text-sm">{submitError}</div>}
      <ReviewFeedbackNotice feedback={detail.latestReviewFeedback} />

      <div className="grid grid-cols-[1fr_320px] gap-6">
        <div className="flex flex-col gap-5">
          <div className="bg-surface rounded-2xl p-6 shadow-md">
            <div className="flex items-center gap-2 mb-4">
              <span className="material-symbols-outlined text-primary text-xl">description</span>
              <h2 className="text-base font-bold text-on-surface m-0">Thông tin cơ bản</h2>
            </div>
            <label className="block text-[11px] font-semibold text-outline uppercase tracking-[0.05em] mb-1.5">
              Tiêu đề bài viết <span className="text-error">*</span>
            </label>
            <input
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              className="w-full py-3 px-4 rounded-2xl border border-outline-variant bg-surface text-sm text-on-surface font-sans mb-4"
            />
            <label className="block text-[11px] font-semibold text-outline uppercase tracking-[0.05em] mb-1.5">
              Tóm tắt ngắn
            </label>
            <textarea
              value={summary}
              onChange={(e) => setSummary(e.target.value.slice(0, 150))}
              rows={2}
              placeholder="Đoạn mô tả ngắn hiển thị trên thẻ nội dung..."
              className="w-full py-3 px-4 rounded-2xl border border-outline-variant bg-surface text-sm text-on-surface font-sans resize-none mb-1"
            />
            <p className="text-right text-xs text-outline mb-4">{summary.length} / 150</p>
            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="block text-[11px] font-semibold text-outline uppercase tracking-[0.05em] mb-1.5">
                  Giai đoạn
                </label>
                <div className="py-3 px-4 rounded-2xl border border-outline-variant bg-surface-container-low text-sm text-on-surface-variant">
                  {STAGE_LABELS[detail.stage]}
                </div>
              </div>
              <div>
                <label className="block text-[11px] font-semibold text-outline uppercase tracking-[0.05em] mb-1.5">
                  Tác giả
                </label>
                <div className="py-3 px-4 rounded-2xl border border-outline-variant bg-surface-container-low text-sm text-outline">
                  Ẩn (BR-PRIVACY)
                </div>
              </div>
            </div>
            <label className="block text-[11px] font-semibold text-outline uppercase tracking-[0.05em] mt-4 mb-1.5">
              Thẻ tag
            </label>
            <select
              multiple
              value={tagIds}
              onChange={(e) => setTagIds(Array.from(e.currentTarget.selectedOptions, (option) => option.value))}
              className="w-full min-h-28 py-3 px-4 rounded-2xl border border-outline-variant bg-surface text-sm text-on-surface font-sans"
            >
              {tags.map((tag) => <option key={tag.id} value={tag.id}>{tag.name}</option>)}
            </select>
            {detail.type === 'ARTICLE' && (
              <div className="mt-5 rounded-2xl border border-primary/30 bg-primary-container/10 p-4">
                <p className="text-[11px] font-semibold text-outline uppercase tracking-[0.05em] mb-2">Recommendation audience</p>
                <div className="flex flex-wrap gap-2 mb-4">
                  {staleRecommendationTagIds.map((tagId) => (
                    <span key={tagId} className="inline-flex items-center gap-2 rounded-full border border-error/50 bg-error-container px-3 py-2 text-xs text-error">
                      Stale audience: {tagId}
                      <button
                        type="button"
                        aria-label={`Remove stale recommendation audience ${tagId}`}
                        onClick={() => setRecommendationTagIds((ids) => ids.filter((id) => id !== tagId))}
                        className="font-bold"
                      >
                        ×
                      </button>
                    </span>
                  ))}
                  {recommendationTags.map((tag) => {
                    const selected = recommendationTagIds.includes(tag.id);
                    return <label key={tag.id} className={`inline-flex cursor-pointer items-center gap-2 rounded-full border px-3 py-2 text-xs ${selected ? 'border-primary bg-primary-container text-primary' : 'border-outline-variant bg-surface text-on-surface'}`}>
                      <input type="checkbox" checked={selected} onChange={() => setRecommendationTagIds((ids) => selected ? ids.filter((id) => id !== tag.id) : [...ids, tag.id])} className="h-4 w-4 accent-primary" />
                      {tag.label}
                    </label>;
                  })}
                </div>
                <div className="grid grid-cols-3 gap-3">
                  <label className="text-xs text-outline">Từ tuần<input type="number" min={0} max={42} value={detail.stage === 'PREGNANCY' ? eligibleFromWeek : ''} disabled={detail.stage !== 'PREGNANCY'} onChange={(e) => setEligibleFromWeek(e.target.value)} className="mt-1 w-full rounded-xl border border-outline-variant bg-surface px-3 py-2 text-sm" /></label>
                  <label className="text-xs text-outline">Đến tuần<input type="number" min={0} max={42} value={detail.stage === 'PREGNANCY' ? eligibleToWeek : ''} disabled={detail.stage !== 'PREGNANCY'} onChange={(e) => setEligibleToWeek(e.target.value)} className="mt-1 w-full rounded-xl border border-outline-variant bg-surface px-3 py-2 text-sm" /></label>
                  <label className="text-xs text-outline">Priority<input type="number" min={0} max={100} value={recommendationPriority} onChange={(e) => setRecommendationPriority(e.target.value)} className="mt-1 w-full rounded-xl border border-outline-variant bg-surface px-3 py-2 text-sm" /></label>
                </div>
                {displayMetadataError && <p role="alert" className="mt-3 text-xs text-error">{displayMetadataError}</p>}
                {!displayMetadataError && <div className="mt-3 rounded-xl bg-surface p-3 text-xs text-on-surface-variant">
                  <p className="font-semibold">Recommendation summary</p>
                  <p>Classification: {recommendationClassification(recommendationTagIds)} · {recommendationWindowLabel(detail.stage, displayFrom, displayTo)} · Priority {displayPriority}</p>
                  <p>Audience: {recommendationTagIds.length === 0 ? 'No controlled audience tags (fallback eligible)' : recommendationTags.filter((tag) => recommendationTagIds.includes(tag.id)).map((tag) => tag.label).join(', ')}</p>
                </div>}
              </div>
            )}
            <p className="text-[11px] text-outline mt-1">Giữ Ctrl/Cmd để chọn nhiều tag.</p>
          </div>

          <div className="bg-surface rounded-2xl p-6 shadow-md">
            <div className="flex items-center justify-between mb-4">
              <div className="flex items-center gap-2">
                <span className="material-symbols-outlined text-primary text-xl">article</span>
                <h2 className="text-base font-bold text-on-surface m-0">Nội dung chi tiết</h2>
              </div>
            </div>
            <RichTextEditor
              value={body}
              onChange={setBody}
              onImageUpload={uploadContentImage}
            />
            <input value={sourceTitle} onChange={e => setSourceTitle(e.target.value)} placeholder="Nguồn tham khảo đã xác thực" className="w-full mt-4 py-3 px-4 rounded-2xl border border-outline-variant bg-surface text-sm" />
            <input value={sourceUrl} onChange={e => setSourceUrl(e.target.value)} placeholder="Liên kết nguồn" className="w-full mt-3 py-3 px-4 rounded-2xl border border-outline-variant bg-surface text-sm" />
          </div>

          <div className="bg-surface rounded-2xl p-6 shadow-md">
            <div className="flex items-center gap-2 mb-3">
              <span className="material-symbols-outlined text-primary text-xl">edit_note</span>
              <h2 className="text-base font-bold text-on-surface m-0">Tóm tắt thay đổi (Change Summary)</h2>
            </div>
            <p className="text-sm text-outline mb-3">
              Vui lòng ghi chú ngắn gọn những thay đổi chính trong phiên bản này để thuận tiện cho việc kiểm duyệt.
            </p>
            <textarea
              value={changeSummary}
              onChange={(e) => setChangeSummary(e.target.value)}
              placeholder="VD: Cập nhật thêm thực đơn mẫu cho tuần đầu tiên, sửa lỗi chính tả..."
              rows={3}
              className="w-full py-3 px-4 rounded-2xl border border-outline-variant bg-surface text-sm text-on-surface font-sans resize-none"
            />
            <p className="text-[11px] text-outline mt-1">
              Trường này chưa được backend lưu trữ (không có bảng changelog theo phiên bản).
            </p>
          </div>
        </div>

        <div className="flex flex-col gap-4">
          <div className="bg-surface rounded-2xl p-5 shadow-md opacity-60">
            <p className="text-[11px] font-semibold text-outline uppercase tracking-[0.05em] mb-2">Ảnh đại diện</p>
            <p className="text-sm text-outline">Chưa hỗ trợ — backend chưa có trường lưu ảnh cho nội dung.</p>
          </div>

          <div className="bg-surface rounded-2xl p-5 shadow-md opacity-60">
            <p className="text-[11px] font-semibold text-outline uppercase tracking-[0.05em] mb-3">Thiết lập</p>
            <div className="flex items-center justify-between mb-2">
              <span className="text-sm text-on-surface">Cho phép bình luận</span>
              <span className="text-xs text-outline">Chưa hỗ trợ</span>
            </div>
            <div className="flex items-center justify-between">
              <span className="text-sm text-on-surface">Hiển thị nổi bật</span>
              <span className="text-xs text-outline">Chưa hỗ trợ</span>
            </div>
          </div>

          <div className="bg-surface rounded-2xl p-5 shadow-md">
            <p className="text-[11px] font-semibold text-outline uppercase tracking-[0.05em] mb-3">Lịch sử phiên bản</p>
            <p className="text-sm text-on-surface mb-1">Phiên bản hiện tại: v{detail.version}</p>
            <button
              onClick={() => navigate(`/content/${id}/versions`)}
              className="text-sm text-primary font-semibold cursor-pointer"
            >
              Xem toàn bộ lịch sử
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
