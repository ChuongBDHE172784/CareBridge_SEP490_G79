import { useState, useEffect } from 'react';
import apiClient from '../../../shared/api/apiClient';

interface CommunityQuestion {
  questionId: string;
  authorId: string;
  topicId: string;
  title: string;
  content: string;
  tags: string[];
  upvoteCount: number;
  answerCount: number;
  createdAt: string;
}

export default function ExpertQuestionQueuePage() {
  const [questions, setQuestions] = useState<CommunityQuestion[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [expandedId, setExpandedId] = useState<string | null>(null);
  const [answerText, setAnswerText] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const load = async () => {
    try {
      const { data } = await apiClient.get('/api/v1/community/questions?status=NEEDS_EXPERT&size=20');
      setQuestions(data.data?.content ?? data.data ?? []);
    } catch (e: any) {
      setError(e.response?.data?.message ?? 'Không thể tải danh sách câu hỏi');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); }, []);

  const postAnswer = async (questionId: string) => {
    if (!answerText.trim()) return;
    setSubmitting(true);
    try {
      await apiClient.post(`/api/v1/community/questions/${questionId}/answers`, { content: answerText });
      setAnswerText('');
      setExpandedId(null);
      await load();
    } catch (e: any) {
      alert(e.response?.data?.message ?? 'Gửi câu trả lời thất bại');
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-[400px]">
        <div className="animate-spin rounded-full h-8 w-8 border-2 border-primary border-t-transparent" />
      </div>
    );
  }

  return (
    <div className="max-w-3xl mx-auto p-6">
      <h1 className="text-2xl font-bold text-on-surface mb-6">Hàng đợi câu hỏi</h1>

      {error && (
        <div className="mb-4 p-3 rounded bg-red-50 border border-red-200 text-red-700 text-sm">{error}</div>
      )}

      {questions.length === 0 && (
        <div className="p-10 text-center text-gray-500 bg-white rounded-lg border border-gray-200">
          Không có câu hỏi nào đang chờ trả lời chuyên gia.
        </div>
      )}

      <div className="space-y-4">
        {questions.map((q) => (
          <div key={q.questionId} className="bg-white rounded-lg border border-gray-200 shadow-sm p-5">
            <h3 className="font-semibold text-gray-900 mb-2">{q.title}</h3>
            <p className="text-sm text-gray-600 line-clamp-3 mb-3">{q.content}</p>

            <div className="flex items-center gap-4 text-xs text-gray-500 mb-3">
              <span>👍 {q.upvoteCount}</span>
              <span>💬 {q.answerCount} trả lời</span>
              <span>{new Date(q.createdAt).toLocaleDateString('vi-VN')}</span>
            </div>

            {q.tags?.length > 0 && (
              <div className="flex flex-wrap gap-1.5 mb-3">
                {q.tags.map((t) => (
                  <span key={t} className="px-2 py-0.5 rounded-full bg-primary/10 text-primary text-xs">{t}</span>
                ))}
              </div>
            )}

            {expandedId === q.questionId ? (
              <div className="mt-3 pt-3 border-t border-gray-100">
                <textarea
                  rows={4}
                  className="w-full rounded border border-gray-300 px-3 py-2 text-sm focus:border-primary focus:ring-1 focus:ring-primary"
                  placeholder="Viết câu trả lời chuyên gia của bạn..."
                  value={answerText}
                  onChange={(e) => setAnswerText(e.target.value)}
                />
                <div className="flex justify-end gap-2 mt-2">
                  <button onClick={() => setExpandedId(null)}
                    className="px-4 py-2 rounded border border-gray-300 text-gray-700 text-sm hover:bg-gray-50">
                    Hủy
                  </button>
                  <button
                    onClick={() => postAnswer(q.questionId)}
                    disabled={submitting || !answerText.trim()}
                    className="px-4 py-2 rounded bg-primary text-white text-sm font-medium disabled:opacity-50">
                    {submitting ? 'Đang gửi...' : 'Gửi câu trả lời'}
                  </button>
                </div>
              </div>
            ) : (
              <button
                onClick={() => setExpandedId(q.questionId)}
                className="mt-2 px-4 py-1.5 rounded border border-primary text-primary text-sm hover:bg-primary/5 font-medium">
                Trả lời
              </button>
            )}
          </div>
        ))}
      </div>
    </div>
  );
}
