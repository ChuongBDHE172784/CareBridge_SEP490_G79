import { useState, useEffect, useCallback } from 'react';
import apiClient from '../../../shared/api/apiClient';

// ── Types from backend CommunityQuestionSummaryResponse ────────────────────
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

// ── Helpers ────────────────────────────────────────────────────────────────
function timeAgo(iso: string): string {
  const diff = Date.now() - new Date(iso).getTime();
  const h = Math.floor(diff / 3_600_000);
  if (h < 1) return 'Vừa xong';
  if (h < 24) return `${h} giờ trước`;
  return `${Math.floor(h / 24)} ngày trước`;
}

const URGENCY_CLS: Record<string, string> = {
  HIGH: 'bg-red-100 text-red-700',
  NORMAL: 'bg-amber-100 text-amber-700',
  LOW: 'bg-green-100 text-green-700',
};

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
      setError(
        e instanceof Error ? e.message : 'Không thể tải danh sách câu hỏi',
      );
    } finally {
      setLoading(false);
    }
  }, [keyword, filterUrgent]);

  useEffect(() => {
    fetchQuestions(0);
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  const onSearch = () => fetchQuestions(0);
  const selected = questions.find((q) => q.id === selectedId);

  const postAnswer = async () => {
    if (!answerText.trim() || !selectedId || submitting) return;
    setSubmitting(true);
    try {
      await apiClient.post(
        `/api/v1/community/questions/${selectedId}/answers`,
        { body: answerText.trim(), isPersonalExperience: false },
      );
      setAnswerText('');
      setSelectedId(null);
      setQuestions((prev) =>
        prev.map((q) =>
          q.id === selectedId ? { ...q, answerCount: q.answerCount + 1, hasExpertAnswer: true } : q,
        ),
      );
    } catch {
      alert('Gửi câu trả lời thất bại');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="flex flex-col h-[calc(100vh-64px)]">
      {/* ── Queue list pane ───────────────────────────────────────────── */}
      <div
        className={`flex flex-col border-r border-outline-variant/50 bg-surface ${selectedId ? 'w-1/2' : 'w-full'}`}>
        {/* Toolbar */}
        <div className="p-4 pb-3 space-y-3">
          <h2 className="text-xl font-bold text-on-surface">Hàng đợi câu hỏi</h2>
          <div className="flex gap-2">
            <input
              type="text"
              placeholder="Tìm kiếm câu hỏi…"
              value={keyword}
              onChange={(e) => setKeyword(e.target.value)}
              onKeyDown={(e) => e.key === 'Enter' && onSearch()}
              className="flex-1 rounded-full border border-outline-variant bg-surface-container-lowest px-4 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary/30"
            />
            <button
              onClick={() => { setFilterUrgent((v) => !v); fetchQuestions(0); }}
              className={`rounded-full px-4 py-2 text-sm font-medium border transition-colors ${
                filterUrgent
                  ? 'bg-primary text-white border-primary'
                  : 'bg-surface text-on-surface-variant border-outline-variant hover:bg-surface-container-low'
              }`}
            >
              Khẩn cấp
            </button>
          </div>
        </div>

        {/* Question list */}
        <div className="flex-1 overflow-y-auto px-4 pb-4 space-y-3">
          {loading && questions.length === 0 && (
            <div className="flex flex-col items-center gap-3 py-20">
              <span className="material-symbols-outlined animate-spin text-[36px] text-primary">
                progress_activity
              </span>
              <p className="text-sm text-on-surface-variant">Đang tải…</p>
            </div>
          )}
          {error && (
            <div className="p-4 rounded-2xl bg-error-container text-on-error-container text-sm">
              {error}
              <button
                onClick={() => fetchQuestions(0)}
                className="ml-3 underline font-semibold text-on-error"
              >
                Thử lại
              </button>
            </div>
          )}
          {questions.map((q) => {
            const active = q.id === selectedId;
            const urgencyCls = URGENCY_CLS[q.urgency] ?? 'bg-gray-100 text-gray-600';
            return (
              <button
                key={q.id}
                onClick={() => setSelectedId(q.id)}
                className={`w-full rounded-2xl p-4 text-left border transition-shadow ${
                  active
                    ? 'border-primary bg-primary-container/30 shadow-md'
                    : 'border-outline-variant/60 bg-surface hover:border-primary/30 hover:shadow-sm'
                }`}
              >
                <div className="flex items-start gap-3">
                  <div className="flex-1 min-w-0">
                    <h3 className="text-sm font-semibold text-on-surface leading-snug line-clamp-2">
                      {q.title}
                    </h3>
                    <div className="flex flex-wrap gap-2 mt-2">
                      {q.topicName && (
                        <span className="rounded-full px-2 py-0.5 bg-surface-container-low text-xs text-on-surface-variant">
                          {q.topicName}
                        </span>
                      )}
                      <span className={`rounded-full px-2 py-0.5 text-xs font-bold ${urgencyCls}`}>
                        {q.urgency}
                      </span>
                    </div>
                    <div className="flex items-center gap-3 mt-2 text-xs text-on-surface-variant">
                      <span>💬 {q.answerCount} trả lời</span>
                      <span>{timeAgo(q.createdAt)}</span>
                      {q.hasExpertAnswer && (
                        <span className="text-primary font-semibold">✓ Đã có chuyên gia</span>
                      )}
                    </div>
                  </div>
                </div>
              </button>
            );
          })}
          {hasMore && !loading && (
            <button
              onClick={() => fetchQuestions(page + 1)}
              className="w-full rounded-full py-2 text-sm font-medium text-primary hover:bg-surface-container-low transition-colors"
            >
              Xem thêm câu hỏi…
            </button>
          )}
          {questions.length === 0 && !loading && !error && (
            <div className="py-16 text-center text-on-surface-variant">
              <span className="material-symbols-outlined text-[48px] block mb-3 opacity-40">
                forum
              </span>
              <p className="text-base font-medium">Không có câu hỏi chờ</p>
              <p className="text-sm mt-1">Tất cả đã có câu trả lời chuyên gia.</p>
            </div>
          )}
        </div>
      </div>

      {/* ── Answer editor pane ─────────────────────────────────────────── */}
      {selected && (
        <div className="w-1/2 flex flex-col bg-surface-container-lowest">
          {/* Question detail header */}
          <div className="p-5 border-b border-outline-variant/50 bg-surface">
            <div className="flex items-start justify-between gap-4">
              <h3 className="text-base font-semibold text-on-surface leading-snug">
                {selected.title}
              </h3>
              <button
                onClick={() => setSelectedId(null)}
                className="text-on-surface-variant hover:text-on-surface text-xl leading-none"
              >
                ×
              </button>
            </div>
            <div className="flex flex-wrap gap-2 mt-2">
              {selected.topicName && (
                <span className="rounded-full px-2 py-0.5 bg-primary-container/40 text-xs text-primary font-medium">
                  {selected.topicName}
                </span>
              )}
              <span className="rounded-full px-2 py-0.5 bg-surface-container-low text-xs text-on-surface-variant">
                Giai đoạn: {selected.stage}
              </span>
              {selected.hasExpertAnswer && (
                <span className="rounded-full px-2 py-0.5 bg-[#e8f5e9] text-xs text-[#2e7d32] font-bold">
                  ✓ Đã có câu trả lời chuyên gia
                </span>
              )}
            </div>
          </div>

          {/* Expert badge notice */}
          <div className="mx-5 mt-4 rounded-2xl border border-primary-container/50 bg-[#fff1ec] p-4">
            <div className="flex items-start gap-3">
              <span className="material-symbols-outlined text-primary text-[20px] mt-0.5">
                verified
              </span>
              <p className="text-xs text-on-surface-variant leading-relaxed">
                Câu trả lời của bạn sẽ được gắn nhãn <strong>Chuyên gia đã xác thực</strong>.
                Vui lòng đảm bảo nội dung mang tính tham khảo — không chẩn đoán hay kê đơn.
              </p>
            </div>
          </div>

          {/* Answer editor */}
          <div className="flex-1 p-5">
            <label className="block text-sm font-semibold text-on-surface mb-2">
              Câu trả lời của bạn
            </label>
            <textarea
              rows={8}
              value={answerText}
              onChange={(e) => setAnswerText(e.target.value)}
              placeholder="Viết câu trả lời chuyên gia của bạn…"
              className="w-full rounded-2xl border border-outline-variant bg-surface p-4 text-sm leading-relaxed focus:outline-none focus:ring-2 focus:ring-primary/30 resize-none"
            />
            <p className="mt-1 text-right text-xs text-on-surface-variant">
              {answerText.length} / 2 000 ký tự
            </p>
          </div>

          {/* Submit bar */}
          <div className="p-4 border-t border-outline-variant/50 bg-surface">
            <button
              onClick={postAnswer}
              disabled={!answerText.trim() || submitting}
              className="w-full h-12 rounded-full bg-primary text-white font-semibold text-base hover:brightness-110 transition disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {submitting ? 'Đang gửi…' : 'Gửi câu trả lời'}
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
