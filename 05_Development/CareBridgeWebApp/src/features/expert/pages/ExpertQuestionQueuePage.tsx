import { useState, useEffect, useCallback } from 'react';
import apiClient from '../../../shared/api/apiClient';

interface CommunityQuestion {
  id: string;
  title: string;
  topicName: string;
  stage: string;
  urgency: string;
  answerCount: number;
  hasExpertAnswer: boolean;
  createdAt: string;
}

function timeAgo(iso: string): string {
  if (!iso) return '—';
  const diff = Date.now() - new Date(iso).getTime();
  const mins = Math.floor(diff / 60000);
  if (mins < 60) return `${mins} phút trước`;
  const hours = Math.floor(mins / 60);
  if (hours < 24) return `${hours} giờ trước`;
  const days = Math.floor(hours / 24);
  return `${days} ngày trước`;
}

export default function ExpertQuestionQueuePage() {
  const [questions, setQuestions] = useState<CommunityQuestion[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [answerText, setAnswerText] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [keyword, setKeyword] = useState('');
  const [filterUrgent, setFilterUrgent] = useState(false);
  const [page, setPage] = useState(0);
  const [hasMore, setHasMore] = useState(true);

  const fetchQuestions = useCallback(async (p: number) => {
    try {
      setLoading(true);
      setError(null);
      const params: Record<string, string> = { size: '20', page: String(p) };
      if (keyword.trim()) params.keyword = keyword.trim();
      if (filterUrgent) params.urgency = 'HIGH';
      const qs = new URLSearchParams(params).toString();
      const { data } = await apiClient.get(`/api/v1/community/questions?${qs}`);
      const content: CommunityQuestion[] = data.data?.content ?? data.data ?? [];
      setQuestions((prev) => (p === 0 ? content : [...prev, ...content]));
      setHasMore(content.length >= 20);
      setPage(p);
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : 'Không thể tải danh sách câu hỏi cộng đồng');
    } finally {
      setLoading(false);
    }
  }, [keyword, filterUrgent]);

  useEffect(() => {
    fetchQuestions(0);
  }, [fetchQuestions]);

  const onSearch = () => fetchQuestions(0);
  const selected = questions.find((q) => q.id === selectedId);

  const postAnswer = async () => {
    if (!answerText.trim() || !selectedId || submitting) return;
    setSubmitting(true);
    try {
      await apiClient.post(`/api/v1/community/questions/${selectedId}/answers`, {
        body: answerText.trim(),
        isPersonalExperience: false,
      });
      setAnswerText('');
      setSelectedId(null);
      setQuestions((prev) =>
        prev.map((q) =>
          q.id === selectedId ? { ...q, answerCount: q.answerCount + 1, hasExpertAnswer: true } : q
        )
      );
    } catch {
      alert('Gửi câu trả lời thất bại. Vui lòng thử lại.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="flex h-[calc(100vh-64px)] p-6 font-sans gap-6 overflow-hidden">
      {/* Question Queue List Pane */}
      <div
        className={`flex flex-col rounded-2xl bg-surface border border-outline-variant/70 shadow-md overflow-hidden transition-all duration-300 ${
          selectedId ? 'w-1/2' : 'w-full'
        }`}
      >
        {/* Header Toolbar */}
        <div className="p-5 pb-3 border-b border-surface-container-highest space-y-3 bg-surface">
          <div className="flex justify-between items-center">
            <div>
              <h2 className="text-xl font-bold text-on-surface m-0 flex items-center gap-2">
                <span className="material-symbols-outlined text-primary text-2xl">forum</span>
                Hàng đợi câu hỏi cộng đồng
              </h2>
              <p className="text-xs text-outline mt-0.5">
                Các câu hỏi sức khỏe mẹ &amp; bé chưa có phản hồi chuyên môn
              </p>
            </div>
          </div>

          <div className="flex gap-2">
            <div className="relative flex-1">
              <span className="material-symbols-outlined text-outline absolute left-3 top-1/2 -translate-y-1/2 text-lg">
                search
              </span>
              <input
                type="text"
                placeholder="Tìm kiếm từ khóa câu hỏi..."
                value={keyword}
                onChange={(e) => setKeyword(e.target.value)}
                onKeyDown={(e) => e.key === 'Enter' && onSearch()}
                className="w-full py-2 pr-3 pl-9 rounded-full border border-outline-variant bg-surface text-xs text-on-surface outline-none focus:border-primary font-sans"
              />
            </div>
            <button
              onClick={() => {
                setFilterUrgent((v) => !v);
                fetchQuestions(0);
              }}
              className={`py-2 px-4 rounded-full text-xs font-semibold cursor-pointer whitespace-nowrap transition-colors flex items-center gap-1 ${
                filterUrgent
                  ? 'border-2 border-primary bg-surface-container-low text-primary'
                  : 'border border-outline-variant bg-transparent text-on-surface-variant'
              }`}
            >
              <span className="material-symbols-outlined text-base">priority_high</span>
              Khẩn cấp
            </button>
          </div>
        </div>

        {/* List Content */}
        <div className="flex-1 overflow-y-auto p-4 space-y-3">
          {loading && questions.length === 0 && (
            <div className="py-20 text-center text-outline flex flex-col items-center gap-2">
              <span className="material-symbols-outlined animate-spin text-3xl text-primary">progress_activity</span>
              <span>Đang tải danh sách câu hỏi...</span>
            </div>
          )}

          {error && (
            <div className="p-4 rounded-2xl bg-error-container text-error text-xs flex justify-between items-center">
              <span>{error}</span>
              <button onClick={() => fetchQuestions(0)} className="underline font-bold cursor-pointer">
                Thử lại
              </button>
            </div>
          )}

          {questions.map((q) => {
            const active = q.id === selectedId;
            const isHigh = q.urgency === 'HIGH';
            return (
              <div
                key={q.id}
                onClick={() => setSelectedId(q.id)}
                className={`p-4 rounded-2xl border cursor-pointer transition-all ${
                  active
                    ? 'border-2 border-primary bg-surface-container-low shadow-md'
                    : 'border-outline-variant/60 bg-surface hover:border-primary/40 hover:shadow-sm'
                }`}
              >
                <div className="flex items-start justify-between gap-3">
                  <div className="flex-1 min-w-0">
                    <h3 className="text-sm font-bold text-on-surface leading-snug line-clamp-2 m-0">
                      {q.title}
                    </h3>

                    <div className="flex flex-wrap items-center gap-2 mt-2">
                      {q.topicName && (
                        <span className="py-0.5 px-2.5 rounded-full bg-surface-container-low text-primary text-[11px] font-semibold">
                          {q.topicName}
                        </span>
                      )}
                      <span
                        className={`py-0.5 px-2.5 rounded-full text-[11px] font-semibold ${
                          isHigh ? 'bg-error-container text-error' : 'bg-[#FFF3E0] text-[#E65100]'
                        }`}
                      >
                        {isHigh ? 'Khẩn cấp' : 'Thường'}
                      </span>
                    </div>

                    <div className="flex items-center gap-3 mt-2 text-[11px] text-outline">
                      <span className="flex items-center gap-1">
                        <span className="material-symbols-outlined text-xs">chat_bubble</span>
                        {q.answerCount} trả lời
                      </span>
                      <span>• {timeAgo(q.createdAt)}</span>
                      {q.hasExpertAnswer && (
                        <span className="text-[#137333] font-semibold flex items-center gap-0.5">
                          <span className="material-symbols-outlined text-xs">verified</span>
                          Đã có bác sĩ
                        </span>
                      )}
                    </div>
                  </div>
                  <span className="material-symbols-outlined text-outline text-lg">chevron_right</span>
                </div>
              </div>
            );
          })}

          {hasMore && !loading && (
            <button
              onClick={() => fetchQuestions(page + 1)}
              className="w-full py-2.5 text-xs font-semibold text-primary hover:bg-surface-container-low rounded-xl transition-colors cursor-pointer"
            >
              Xem thêm câu hỏi...
            </button>
          )}

          {questions.length === 0 && !loading && !error && (
            <div className="py-16 text-center text-outline">
              <span className="material-symbols-outlined text-4xl block mb-2 opacity-40">task_alt</span>
              <p className="text-sm font-semibold">Không có câu hỏi chờ</p>
              <p className="text-xs mt-1">Tất cả câu hỏi đã được giải đáp.</p>
            </div>
          )}
        </div>
      </div>

      {/* Answer Editor Pane */}
      {selected ? (
        <div className="w-1/2 flex flex-col rounded-2xl bg-surface border border-outline-variant/70 shadow-md overflow-hidden">
          {/* Question detail header */}
          <div className="p-5 border-b border-surface-container-highest bg-surface">
            <div className="flex items-start justify-between gap-4">
              <h3 className="text-base font-bold text-on-surface leading-snug m-0">
                {selected.title}
              </h3>
              <button
                onClick={() => setSelectedId(null)}
                className="w-7 h-7 rounded-full border border-outline-variant flex items-center justify-center text-outline hover:text-on-surface shrink-0 cursor-pointer"
              >
                <span className="material-symbols-outlined text-base">close</span>
              </button>
            </div>
            <div className="flex flex-wrap gap-2 mt-2">
              {selected.topicName && (
                <span className="py-0.5 px-3 rounded-full bg-primary-container text-primary text-xs font-semibold">
                  {selected.topicName}
                </span>
              )}
              <span className="py-0.5 px-3 rounded-full bg-surface-container-low text-on-surface-variant text-xs font-medium">
                Giai đoạn: {selected.stage}
              </span>
              {selected.hasExpertAnswer && (
                <span className="py-0.5 px-3 rounded-full bg-[#E6F4EA] text-[#137333] text-xs font-bold flex items-center gap-1">
                  <span className="material-symbols-outlined text-xs">verified</span>
                  Đã có tư vấn chuyên gia
                </span>
              )}
            </div>
          </div>

          {/* Expert Badge Notice */}
          <div className="mx-5 mt-4 rounded-2xl border border-primary/20 bg-primary-container/20 p-4">
            <div className="flex items-start gap-3">
              <span className="material-symbols-outlined text-primary text-xl mt-0.5">verified</span>
              <p className="text-xs text-on-surface-variant leading-relaxed m-0">
                Câu trả lời của bạn sẽ được hiển thị kèm huy hiệu <strong>Chuyên gia Y tế CareBridge</strong>.
                Vui lòng đưa ra tư vấn khoa học, chính xác — không đưa ra kê đơn thuốc trực tiếp.
              </p>
            </div>
          </div>

          {/* Answer Editor Textarea */}
          <div className="flex-1 p-5 flex flex-col">
            <label className="block text-xs font-semibold text-outline uppercase tracking-wider mb-2">
              Nội dung câu trả lời chuyên gia
            </label>
            <textarea
              rows={8}
              value={answerText}
              onChange={(e) => setAnswerText(e.target.value)}
              placeholder="Viết câu trả lời tư vấn chuyên môn chi tiết cho mẹ bầu..."
              className="flex-1 w-full rounded-2xl border border-outline-variant bg-surface p-4 text-sm text-on-surface leading-relaxed outline-none focus:border-primary font-sans resize-none"
            />
            <p className="mt-1.5 text-right text-xs text-outline m-0">
              {answerText.length} / 2 000 ký tự
            </p>
          </div>

          {/* Submit Bar */}
          <div className="p-4 border-t border-surface-container-highest bg-surface flex justify-end">
            <button
              onClick={postAnswer}
              disabled={!answerText.trim() || submitting}
              className="flex items-center justify-center gap-2 py-3 px-8 rounded-full bg-primary text-on-primary font-semibold text-sm hover:brightness-110 disabled:opacity-50 cursor-pointer"
            >
              <span className="material-symbols-outlined text-lg">send</span>
              {submitting ? 'Đang gửi...' : 'Gửi câu trả lời'}
            </button>
          </div>
        </div>
      ) : (
        <div className="w-1/2 hidden md:flex flex-col items-center justify-center rounded-2xl bg-surface border border-outline-variant/70 shadow-md p-8 text-center text-outline">
          <span className="material-symbols-outlined text-5xl block mb-2 opacity-30">forum</span>
          <p className="text-base font-bold text-on-surface mb-1">Chọn một câu hỏi để trả lời</p>
          <p className="text-xs text-outline max-w-xs">
            Nhấp vào câu hỏi trong hàng đợi bên trái để xem thông tin chi tiết và viết phản hồi chuyên môn.
          </p>
        </div>
      )}
    </div>
  );
}

