import { useEffect, useState, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { createContent, fetchTags, fetchTopics, updateContent, uploadContentImage } from '../services/contentApi';
import type { CommunityTopic, ContentStage, ContentType } from '../models/content';
import { TYPE_LABELS, STAGE_OPTIONS } from '../models/content';
import RichTextEditor from '../components/RichTextEditor';
import { isRichTextEmpty } from '../components/richTextUtils';

type CreatableContentType = Exclude<ContentType, 'CHECKLIST'>;

interface CreateContentPageProps {
  contentType: CreatableContentType;
}

export default function CreateContentPage({ contentType }: CreateContentPageProps) {
  const navigate = useNavigate();

  const [stage, setStage] = useState<ContentStage | ''>('');
  const [topicId, setTopicId] = useState('');
  const [title, setTitle] = useState('');
  const [summary, setSummary] = useState('');
  const [body, setBody] = useState('');
  const [sourceLabel, setSourceLabel] = useState('');
  const [sourceUrl, setSourceUrl] = useState('');
  const [sourcePublisher, setSourcePublisher] = useState('');
  const [topics, setTopics] = useState<CommunityTopic[]>([]);
  const [tags, setTags] = useState<CommunityTopic[]>([]);
  const [tagIds, setTagIds] = useState<string[]>([]);
  const [submitting, setSubmitting] = useState<'draft' | 'submit' | null>(null);
  const [error, setError] = useState('');
  const [created, setCreated] = useState<{ id: string; title: string; sentForApproval: boolean } | null>(null);

  useEffect(() => {
    Promise.all([fetchTopics(), fetchTags()])
      .then(([loadedTopics, loadedTags]) => {
        setTopics(loadedTopics);
        setTags(loadedTags.filter((tag) => !tag.slug.startsWith('rec-')));
      })
      .catch(() => {
        setTopics([]);
        setTags([]);
      });
  }, []);

  const contentTypeLabel = TYPE_LABELS[contentType];
  const isValid = title.trim().length > 0 && !isRichTextEmpty(body) && stage !== '';

  const toggleTag = (tagId: string) => {
    setTagIds((selected) => selected.includes(tagId)
      ? selected.filter((id) => id !== tagId)
      : [...selected, tagId]);
  };

  const submit = useCallback(async (sendForApproval: boolean) => {
    if (title.trim().length === 0 || isRichTextEmpty(body) || stage === '') return;
    setSubmitting(sendForApproval ? 'submit' : 'draft');
    setError('');
    try {
      const result = await createContent({
        type: contentType,
        title: title.trim(),
        body,
        summary: summary.trim() || undefined,
        stage,
        topicId: topicId || undefined,
        tagIds,
        sources: sourceLabel.trim() ? [{ title: sourceLabel.trim(), url: sourceUrl.trim() || undefined, publisher: sourcePublisher.trim() || undefined }] : undefined,
      });
      if (sendForApproval || sourceLabel.trim()) {
        try {
          await updateContent(result.id, {
            title: title.trim(),
            body,
            summary: summary.trim() || undefined,
            stage,
            topicId: topicId || undefined,
            tagIds,
            status: sendForApproval ? 'PENDING_REVIEW' : 'DRAFT',
            sourceLabel: sourceLabel.trim() || undefined,
            sources: sourceLabel.trim() ? [{ title: sourceLabel.trim(), url: sourceUrl.trim() || undefined, publisher: sourcePublisher.trim() || undefined }] : undefined,
          });
        } catch {
          setError('Đã tạo bản nháp nhưng chưa thể cập nhật trạng thái. Bạn có thể mở bản nháp để tiếp tục.');
          return;
        }
      }
      setCreated({ id: result.id, title: result.title, sentForApproval: sendForApproval });
    } catch {
      setError('Không thể tạo nội dung. Vui lòng thử lại.');
    } finally {
      setSubmitting(null);
    }
  }, [contentType, stage, title, summary, body, topicId, tagIds, sourceLabel, sourceUrl, sourcePublisher]);

  if (created) {
    return (
      <div className="p-8 font-sans max-w-[700px]">
        <div className="bg-surface rounded-2xl p-8 shadow-md text-center">
          <span className="material-symbols-outlined text-primary text-5xl mb-3">check_circle</span>
          <h1 className="text-xl font-bold text-on-surface mb-2">Đã tạo {contentTypeLabel.toLowerCase()} thành công</h1>
          <p className="text-sm text-on-surface-variant mb-1">"{created.title}"</p>
          <p className="text-sm text-outline mb-6">
            Trạng thái: {created.sentForApproval ? 'Đang chờ phê duyệt' : 'Bản nháp'}. Nội dung chưa được duyệt sẽ
            chưa hiển thị ở trang chi tiết công khai (backend chỉ cho xem nội dung đã APPROVED).
          </p>
          <div className="flex justify-center gap-3">
            <button
              onClick={() => navigate('/content/dashboard')}
              className="py-3 px-6 rounded-full border border-outline-variant bg-transparent text-primary text-sm font-semibold cursor-pointer"
            >
              Về Dashboard
            </button>
            <button
              onClick={() => window.location.reload()}
              className="py-3 px-6 rounded-full bg-primary text-on-primary border-0 text-sm font-semibold cursor-pointer"
            >
              Tạo nội dung khác
            </button>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="p-8 font-sans max-w-[900px]">
      <button
        onClick={() => navigate(contentType === 'ARTICLE' ? '/content/articles' : '/content/faq')}
        className="inline-flex items-center gap-1.5 py-2 px-5 rounded-full border border-outline-variant bg-transparent text-primary text-sm font-semibold cursor-pointer mb-6"
      >
        <span className="material-symbols-outlined text-lg">arrow_back</span>
        Quay lại
      </button>

      <h1 className="text-[26px] font-bold text-on-surface m-0">Tạo {contentTypeLabel} mới</h1>
      <p className="text-on-surface-variant text-sm mt-1 mb-6">
        Điền thông tin chi tiết để thêm {contentTypeLabel.toLowerCase()} vào thư viện.
      </p>

      <div className="bg-surface rounded-2xl p-6 shadow-md mb-6">
        <div className="mb-5">
          <label className="block text-[11px] font-semibold text-outline uppercase tracking-[0.05em] mb-1.5">
            Chủ đề / Danh mục
          </label>
          <select
            value={topicId}
            onChange={(e) => setTopicId(e.target.value)}
            className="w-full py-3 px-4 rounded-2xl border border-outline-variant bg-surface text-sm text-on-surface font-sans"
          >
            <option value="">Chọn danh mục</option>
            {topics.map((t) => <option key={t.id} value={t.id}>{t.name}</option>)}
          </select>
        </div>



        <div className="mb-5">
          <div className="flex items-center justify-between mb-1.5">
            <span className="text-[11px] font-semibold text-outline uppercase tracking-[0.05em]">Thẻ tag</span>
            <span className="text-xs text-primary font-semibold">Đã chọn {tagIds.length}</span>
          </div>
          {tags.length > 0 ? (
            <div className="flex flex-wrap gap-2 rounded-2xl border border-outline-variant bg-surface p-3">
              {tags.map((tag) => {
                const selected = tagIds.includes(tag.id);
                return (
                  <label
                    key={tag.id}
                    className={`inline-flex cursor-pointer items-center gap-2 rounded-full border px-3 py-2 text-sm transition-colors ${
                      selected ? 'border-primary bg-primary-container text-primary' : 'border-outline-variant bg-surface text-on-surface'
                    }`}
                  >
                    <input
                      type="checkbox"
                      checked={selected}
                      onChange={() => toggleTag(tag.id)}
                      className="h-4 w-4 accent-primary"
                    />
                    {tag.name}
                  </label>
                );
              })}
            </div>
          ) : (
            <div className="rounded-2xl border border-dashed border-outline-variant p-4 text-sm text-outline">
              Chưa có tag hiển thị. Hãy tạo tag tại trang Quản lý Chủ đề &amp; Danh mục.
            </div>
          )}
          <p className="text-[11px] text-outline mt-1">Có thể chọn nhiều tag cho cùng một {contentTypeLabel.toLowerCase()}.</p>
        </div>

        <div className="mb-5">
          <label className="block text-[11px] font-semibold text-outline uppercase tracking-[0.05em] mb-1.5">
            Giai đoạn
          </label>
          <select
            value={stage}
            onChange={(e) => setStage(e.target.value as ContentStage)}
            className="w-full py-3 px-4 rounded-2xl border border-outline-variant bg-surface text-sm text-on-surface font-sans"
          >
            <option value="">Chọn giai đoạn</option>
            {STAGE_OPTIONS.map(({ value, label }) => <option key={value} value={value}>{label}</option>)}
          </select>
        </div>

        <div className="mb-5">
          <label className="block text-[11px] font-semibold text-outline uppercase tracking-[0.05em] mb-1.5">
            Tiêu đề nội dung <span className="text-error">*</span>
          </label>
          <input
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            placeholder="Nhập tiêu đề rõ ràng, hấp dẫn..."
            className="w-full py-3 px-4 rounded-2xl border border-outline-variant bg-surface text-sm text-on-surface font-sans"
          />
        </div>

        <div className="mb-5">
          <label className="block text-[11px] font-semibold text-outline uppercase tracking-[0.05em] mb-1.5">
            Tóm tắt ngắn
          </label>
          <textarea
            value={summary}
            onChange={(e) => setSummary(e.target.value.slice(0, 150))}
            placeholder="Đoạn mô tả ngắn hiển thị trên thẻ bài viết (tối đa 150 ký tự)..."
            rows={2}
            className="w-full py-3 px-4 rounded-2xl border border-outline-variant bg-surface text-sm text-on-surface font-sans resize-none"
          />
          <div className="flex justify-between mt-1">
            <p className="text-[11px] text-outline">Hiển thị ngắn gọn trên thẻ nội dung.</p>
            <p className="text-xs text-outline">{summary.length} / 150</p>
          </div>
        </div>

        <div className="mb-5">
          <label className="block text-[11px] font-semibold text-outline uppercase tracking-[0.05em] mb-1.5">
            Nội dung chi tiết <span className="text-error">*</span>
          </label>
          <RichTextEditor
            value={body}
            onChange={setBody}
            onImageUpload={uploadContentImage}
            placeholder="Bắt đầu viết nội dung ở đây..."
          />
        </div>

        <div>
          <label className="block text-[11px] font-semibold text-outline uppercase tracking-[0.05em] mb-1.5">
            Nguồn tham khảo / Kiểm duyệt
          </label>
          <input
            value={sourceLabel}
            onChange={(e) => setSourceLabel(e.target.value)}
            placeholder="VD: WHO, Vinmec, Bác sĩ Nguyễn Văn A..."
            className="w-full py-3 px-4 rounded-2xl border border-outline-variant bg-surface text-sm text-on-surface font-sans"
          />
          <input value={sourceUrl} onChange={e => setSourceUrl(e.target.value)} placeholder="Liên kết nguồn (https://...)" className="w-full mt-3 py-3 px-4 rounded-2xl border border-outline-variant bg-surface text-sm" />
          <input value={sourcePublisher} onChange={e => setSourcePublisher(e.target.value)} placeholder="Đơn vị xuất bản" className="w-full mt-3 py-3 px-4 rounded-2xl border border-outline-variant bg-surface text-sm" />
          <p className="text-[11px] text-outline mt-1">Tăng độ tin cậy cho bài viết với người đọc.</p>
        </div>
      </div>

      {error && <div role="alert" tabIndex={-1} className="bg-error-container rounded-2xl p-4 mb-4 text-error text-sm">{error}</div>}

      <div className="flex items-center justify-end sticky bottom-0 bg-background py-4">
        <div className="flex gap-3">
          <button
            onClick={() => submit(false)}
            disabled={!isValid || submitting !== null}
            className="py-3 px-6 rounded-full border border-outline-variant bg-surface text-on-surface-variant text-sm font-semibold cursor-pointer disabled:opacity-40 disabled:cursor-not-allowed"
          >
            {submitting === 'draft' ? 'Đang lưu...' : 'Lưu nháp'}
          </button>
          <button
            onClick={() => submit(true)}
            disabled={!isValid || submitting !== null}
            className="py-3 px-6 rounded-full bg-primary text-on-primary border-0 text-sm font-semibold cursor-pointer disabled:opacity-40 disabled:cursor-not-allowed"
          >
            {submitting === 'submit' ? 'Đang gửi...' : 'Gửi phê duyệt'}
          </button>
        </div>
      </div>
    </div>
  );
}
