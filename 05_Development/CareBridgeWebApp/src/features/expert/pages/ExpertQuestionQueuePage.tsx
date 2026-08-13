import { useState, useEffect, useCallback, useRef } from 'react';
import apiClient from '../../../shared/api/apiClient';
import { useAuthStore } from '../../../shared/auth/authStore';

interface CommunityTopic {
  id: string;
  name: string;
}

interface CommunityQuestion {
  id: string;
  title: string;
  body?: string;
  topicId?: string;
  topicName?: string;
  stage: string;
  urgency: string;
  answerCount: number;
  hasExpertAnswer: boolean;
  status?: string;
  createdAt: string;
  imageUrls?: string[];
}

interface AnswerItem {
  id: string;
  authorId: string;
  body: string;
  authorDisplay?: string;
  expertLabeled: boolean;
  expertProfileId?: string;
  personalExperience: boolean;
  imageUrls: string[];
  likeCount: number;
  liked: boolean;
  createdAt: string;
}

interface PendingAnswerImage {
  file: File;
  previewUrl: string;
}

interface CommunityQuestionDetail {
  id: string;
  topicId?: string;
  topicName?: string;
  title: string;
  body: string;
  imageUrls: string[];
  stage: string;
  pregnancyWeek?: number;
  babyAgeMonths?: number;
  urgency: string;
  anonymous: boolean;
  authorDisplay: string;
  status: string;
  answerCount: number;
  likeCount: number;
  isLiked: boolean;
  createdAt: string;
  answers?: AnswerItem[];
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

function getStageLabel(stage: string): string {
  if (stage === 'PREGNANCY') return 'Thai kỳ';
  if (stage === 'POSTPARTUM') return 'Sau sinh';
  return stage || 'Cộng đồng';
}

export default function ExpertQuestionQueuePage() {
  const [questions, setQuestions] = useState<CommunityQuestion[]>([]);
  const [topics, setTopics] = useState<CommunityTopic[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Selection & Detail
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [detail, setDetail] = useState<CommunityQuestionDetail | null>(null);
  const [detailLoading, setDetailLoading] = useState(false);
  const [previewImage, setPreviewImage] = useState<string | null>(null);

  // Filters (aligned with Mobile App)
  const [keyword, setKeyword] = useState('');
  const [selectedStage, setSelectedStage] = useState<string>(''); // '', 'PREGNANCY', 'POSTPARTUM'
  const [selectedTopicId, setSelectedTopicId] = useState<string>('');
  const [selectedHasExpertAnswer, setSelectedHasExpertAnswer] = useState<string>(''); // '', 'false', 'true'
  const [filterUrgent, setFilterUrgent] = useState(false);

  // Answer submission
  const [answerText, setAnswerText] = useState('');
  const [pendingAnswerImages, setPendingAnswerImages] = useState<PendingAnswerImage[]>([]);
  const [editingAnswerId, setEditingAnswerId] = useState<string | null>(null);
  const [existingAnswerImageUrls, setExistingAnswerImageUrls] = useState<string[]>([]);
  const [submitting, setSubmitting] = useState(false);
  const imageInputRef = useRef<HTMLInputElement>(null);
  const currentUserId = useAuthStore((state) => state.user?.id ?? null);


  // Pagination
  const [page, setPage] = useState(0);
  const [hasMore, setHasMore] = useState(true);

  // Load Topics list for filter dropdown
  useEffect(() => {
    const loadTopics = async () => {
      try {
        const { data } = await apiClient.get('/api/v1/community/topics');
        setTopics(data.data ?? []);
      } catch {
        // non-blocking
      }
    };
    void loadTopics();
  }, []);

  // Fetch Questions with active filters
  const fetchQuestions = useCallback(async (p: number) => {
    try {
      setLoading(true);
      setError(null);
      const params: Record<string, string> = { size: '20', page: String(p) };
      if (keyword.trim()) params.keyword = keyword.trim();
      if (selectedStage) params.stage = selectedStage;
      if (selectedTopicId) params.topicId = selectedTopicId;
      if (selectedHasExpertAnswer) params.hasExpertAnswer = selectedHasExpertAnswer;
      if (filterUrgent) params.urgency = 'URGENT';

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
  }, [keyword, selectedStage, selectedTopicId, selectedHasExpertAnswer, filterUrgent]);

  useEffect(() => {
    fetchQuestions(0);
  }, [fetchQuestions]);

  // Fetch Question Detail when selected
  useEffect(() => {
    if (!selectedId) {
      setDetail(null);
      return;
    }
    let active = true;
    setDetailLoading(true);
    apiClient
      .get(`/api/v1/community/questions/${selectedId}`)
      .then((res) => {
        if (active) setDetail(res.data.data);
      })
      .catch(() => {
        if (active) setDetail(null);
      })
      .finally(() => {
        if (active) setDetailLoading(false);
      });
    return () => {
      active = false;
    };
  }, [selectedId]);

  const onSearch = () => fetchQuestions(0);
  const selectedSummary = questions.find((q) => q.id === selectedId);

  const postAnswer = async () => {
    if (!answerText.trim() || !selectedId || submitting) return;
    setSubmitting(true);
    try {
      const imageUrls = await Promise.all(pendingAnswerImages.map(async ({ file }) => {
        const form = new FormData();
        form.append('file', file);
        form.append('kind', 'IMAGE');
        form.append('purpose', 'COMMUNITY_ANSWER_IMAGE');
        form.append('accessMode', 'PUBLIC');
        const { data } = await apiClient.post('/api/v1/files/upload/with-purpose', form, {
          headers: { 'Content-Type': undefined },
        });
        return data.data.presignedUrl as string;
      }));
      const payload = {
        body: answerText.trim(),
        isPersonalExperience: false,
        imageUrls: [...existingAnswerImageUrls, ...imageUrls],
      };
      const { data } = editingAnswerId
        ? await apiClient.patch(`/api/v1/community/questions/${selectedId}/answers/${editingAnswerId}`, payload)
        : await apiClient.post(`/api/v1/community/questions/${selectedId}/answers`, payload);

      const newAnswer: AnswerItem = data.data ?? {
        id: crypto.randomUUID(),
        body: answerText.trim(),
        authorDisplay: 'Chuyên gia',
        expertLabeled: true,
        personalExperience: false,
        imageUrls,
        likeCount: 0,
        liked: false,
        createdAt: new Date().toISOString(),
      };

      setAnswerText('');
      pendingAnswerImages.forEach(({ previewUrl }) => URL.revokeObjectURL(previewUrl));
      setPendingAnswerImages([]);
      setDetail((prev) => {
        if (!prev) return prev;
        if (editingAnswerId) {
          return { ...prev, answers: prev.answers?.map((answer) => answer.id === editingAnswerId
            ? { ...answer, ...newAnswer, authorDisplay: answer.authorDisplay }
            : answer) };
        }
        return { ...prev, answerCount: prev.answerCount + 1, answers: [newAnswer, ...(prev.answers || [])] };
      });
      if (!editingAnswerId) {
        setQuestions((prev) => prev.map((q) => q.id === selectedId
          ? { ...q, answerCount: q.answerCount + 1, hasExpertAnswer: true }
          : q));
      }
      setEditingAnswerId(null);
      setExistingAnswerImageUrls([]);
    } catch {
      alert('Gửi câu trả lời thất bại. Vui lòng thử lại.');
    } finally {
      setSubmitting(false);
    }
  };

  const startEditingAnswer = (answer: AnswerItem) => {
    setEditingAnswerId(answer.id);
    setAnswerText(answer.body);
    setExistingAnswerImageUrls(answer.imageUrls);
    setPendingAnswerImages([]);
  };

  const cancelEditingAnswer = () => {
    pendingAnswerImages.forEach(({ previewUrl }) => URL.revokeObjectURL(previewUrl));
    setEditingAnswerId(null);
    setAnswerText('');
    setExistingAnswerImageUrls([]);
    setPendingAnswerImages([]);
  };

  const deleteAnswer = async (answerId: string) => {
    if (!selectedId || !window.confirm('Bạn có chắc muốn xóa câu trả lời này?')) return;
    try {
      await apiClient.delete(`/api/v1/community/questions/${selectedId}/answers/${answerId}`);
      setDetail((current) => current ? {
        ...current,
        answerCount: Math.max(0, current.answerCount - 1),
        answers: current.answers?.filter((answer) => answer.id !== answerId),
      } : current);
      setQuestions((current) => current.map((question) => question.id === selectedId
        ? { ...question, answerCount: Math.max(0, question.answerCount - 1) }
        : question));
      if (editingAnswerId === answerId) cancelEditingAnswer();
    } catch {
      alert('Không thể xóa câu trả lời. Vui lòng thử lại.');
    }
  };

  const queueAnswerImage = (file: File) => {
    if (pendingAnswerImages.length >= 3) return;
    setPendingAnswerImages((current) => [...current, { file, previewUrl: URL.createObjectURL(file) }]);
    if (imageInputRef.current) imageInputRef.current.value = '';
  };

  const toggleQuestionLike = async () => {
    if (!selectedId || !detail) return;
    try {
      const { data } = await apiClient.post(`/api/v1/community/questions/${selectedId}/like`);
      setDetail((current) => current ? { ...current, likeCount: data.data.likeCount, isLiked: data.data.liked } : current);
    } catch {
      alert('Không thể cập nhật lượt tim. Vui lòng thử lại.');
    }
  };

  const toggleAnswerLike = async (answerId: string) => {
    try {
      const { data } = await apiClient.post(`/api/v1/community/answers/${answerId}/like`);
      setDetail((current) => current ? {
        ...current,
        answers: current.answers?.map((answer) => answer.id === answerId
          ? { ...answer, likeCount: data.data.likeCount, liked: data.data.liked }
          : answer),
      } : current);
    } catch {
      alert('Không thể cập nhật lượt tim. Vui lòng thử lại.');
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
        {/* Header & Filter Bar (Mobile-App Parity) */}
        <div className="p-5 pb-3 border-b border-surface-container-highest space-y-3 bg-surface">
          <div className="flex justify-between items-center">
            <div>
              <h2 className="text-xl font-bold text-on-surface m-0 flex items-center gap-2">
                <span className="material-symbols-outlined text-primary text-2xl">forum</span>
                Hàng đợi câu hỏi cộng đồng
              </h2>
              <p className="text-xs text-outline mt-0.5 m-0">
                Các câu hỏi sức khỏe mẹ &amp; bé chưa có phản hồi chuyên môn
              </p>
            </div>
            <button
              onClick={() => fetchQuestions(0)}
              className="p-2 rounded-full border border-outline-variant text-outline hover:text-primary hover:bg-surface-container-low transition-colors cursor-pointer"
              title="Làm mới hàng đợi"
            >
              <span className="material-symbols-outlined text-lg">refresh</span>
            </button>
          </div>

          {/* Search Row */}
          <div className="flex gap-2">
            <div className="relative flex-1">
              <span className="material-symbols-outlined text-outline absolute left-3 top-1/2 -translate-y-1/2 text-lg">
                search
              </span>
              <input
                type="text"
                placeholder="Tìm kiếm nội dung, từ khóa câu hỏi..."
                value={keyword}
                onChange={(e) => setKeyword(e.target.value)}
                onKeyDown={(e) => e.key === 'Enter' && onSearch()}
                className="w-full py-2 pr-3 pl-9 rounded-full border border-outline-variant bg-surface text-xs text-on-surface outline-none focus:border-primary font-sans"
              />
            </div>

            <button
              onClick={() => {
                setFilterUrgent((v) => !v);
              }}
              className={`py-2 px-3.5 rounded-full text-xs font-semibold cursor-pointer whitespace-nowrap transition-colors flex items-center gap-1 ${
                filterUrgent
                  ? 'border-2 border-primary bg-surface-container-low text-primary'
                  : 'border border-outline-variant bg-transparent text-on-surface-variant'
              }`}
            >
              <span className="material-symbols-outlined text-base">priority_high</span>
              Khẩn cấp
            </button>
          </div>

          {/* Dropdown Filters Row (Stage, Topic, Expert Status) */}
          <div className="flex flex-wrap items-center gap-2 pt-1">
            {/* Stage Filter */}
            <select
              value={selectedStage}
              onChange={(e) => {
                setSelectedStage(e.target.value);
              }}
              className="py-1.5 px-3 rounded-full border border-outline-variant bg-surface text-xs font-medium text-on-surface outline-none cursor-pointer"
            >
              <option value="">Tất cả giai đoạn</option>
              <option value="PREGNANCY">Thai kỳ</option>
              <option value="POSTPARTUM">Sau sinh</option>
            </select>

            {/* Topic Filter */}
            {topics.length > 0 && (
              <select
                value={selectedTopicId}
              onChange={(e) => {
                setSelectedTopicId(e.target.value);
              }}
                className="py-1.5 px-3 rounded-full border border-outline-variant bg-surface text-xs font-medium text-on-surface outline-none cursor-pointer max-w-[180px] truncate"
              >
                <option value="">Tất cả chủ đề</option>
                {topics.map((t) => (
                  <option key={t.id} value={t.id}>
                    {t.name}
                  </option>
                ))}
              </select>
            )}

            {/* Expert Answer Status Filter */}
            <select
              value={selectedHasExpertAnswer}
              onChange={(e) => {
                setSelectedHasExpertAnswer(e.target.value);
              }}
              className="py-1.5 px-3 rounded-full border border-outline-variant bg-surface text-xs font-medium text-on-surface outline-none cursor-pointer"
            >
              <option value="">Tất cả trạng thái bác sĩ</option>
              <option value="false">Chưa có bác sĩ tư vấn</option>
              <option value="true">Đã có bác sĩ tư vấn</option>
            </select>
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
            const isHigh = q.urgency === 'URGENT';
            const hasImages = q.imageUrls && q.imageUrls.length > 0;

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

                    {q.body && (
                      <p className="text-xs text-on-surface-variant line-clamp-2 mt-1 mb-0 leading-relaxed">
                        {q.body}
                      </p>
                    )}

                    <div className="flex flex-wrap items-center gap-2 mt-2">
                      {q.topicName && (
                        <span className="py-0.5 px-2.5 rounded-full bg-surface-container-low text-primary text-[11px] font-semibold">
                          {q.topicName}
                        </span>
                      )}
                      <span className="py-0.5 px-2.5 rounded-full bg-surface-container-low text-on-surface-variant text-[11px] font-medium">
                        {getStageLabel(q.stage)}
                      </span>
                      <span
                        className={`py-0.5 px-2.5 rounded-full text-[11px] font-semibold ${
                          isHigh ? 'bg-error-container text-error' : 'bg-[#FFF3E0] text-[#E65100]'
                        }`}
                      >
                        {isHigh ? 'Khẩn cấp' : 'Thường'}
                      </span>
                      {q.status === 'LOCKED' && (
                        <span className="py-0.5 px-2.5 rounded-full bg-error-container text-error text-[11px] font-semibold flex items-center gap-1">
                          <span className="material-symbols-outlined text-xs">lock</span>
                          Đã khóa
                        </span>
                      )}
                      {hasImages && (
                        <span className="py-0.5 px-2.5 rounded-full bg-primary-container/40 text-primary text-[11px] font-semibold flex items-center gap-1">
                          <span className="material-symbols-outlined text-xs">image</span>
                          {q.imageUrls?.length} ảnh
                        </span>
                      )}
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
              <p className="text-sm font-semibold">Không tìm thấy câu hỏi phù hợp</p>
              <p className="text-xs mt-1">Thử thay đổi bộ lọc hoặc từ khóa tìm kiếm.</p>
            </div>
          )}
        </div>
      </div>

      {/* Answer & Detail Pane */}
      {selectedId ? (
        <div className="w-1/2 flex flex-col rounded-2xl bg-surface border border-outline-variant/70 shadow-md overflow-hidden">
          {/* Header Bar */}
          <div className="p-5 border-b border-surface-container-highest bg-surface flex items-start justify-between gap-4">
            <div>
              <div className="flex items-center gap-2 mb-1">
                <span className="py-0.5 px-2.5 rounded-full bg-primary-container text-primary text-xs font-semibold">
                  {detail?.topicName || selectedSummary?.topicName || 'Chủ đề Y tế'}
                </span>
                <span className="py-0.5 px-2.5 rounded-full bg-surface-container-low text-on-surface-variant text-xs font-medium">
                  Giai đoạn: {getStageLabel(detail?.stage || selectedSummary?.stage || '')}
                </span>
                {detail?.status === 'LOCKED' && (
                  <span className="py-0.5 px-2.5 rounded-full bg-error-container text-error text-xs font-semibold flex items-center gap-1">
                    <span className="material-symbols-outlined text-xs">lock</span>
                    Đã khóa thảo luận
                  </span>
                )}
              </div>
              <h3 className="text-base font-bold text-on-surface leading-snug m-0">
                {detail?.title || selectedSummary?.title}
              </h3>
            </div>
            <button
              onClick={() => setSelectedId(null)}
              className="w-8 h-8 rounded-full border border-outline-variant flex items-center justify-center text-outline hover:text-on-surface shrink-0 cursor-pointer"
              title="Đóng chi tiết"
            >
              <span className="material-symbols-outlined text-base">close</span>
            </button>
          </div>

          {/* Scrollable Question Body, Attached Images & Existing Answers */}
          <div className="flex-1 overflow-y-auto p-5 space-y-5 bg-surface-container-low/20">
            {detailLoading ? (
              <div className="py-12 text-center text-outline flex items-center justify-center gap-2 text-xs">
                <span className="material-symbols-outlined animate-spin text-lg text-primary">progress_activity</span>
                Đang tải chi tiết bài viết &amp; hình ảnh đính kèm...
              </div>
            ) : (
              <>
                {/* Author Metadata */}
                <div className="flex items-center justify-between p-3 rounded-2xl bg-surface border border-outline-variant/50">
                  <div className="flex items-center gap-3">
                    <div className="w-9 h-9 rounded-full bg-primary/10 text-primary font-bold flex items-center justify-center text-sm">
                      {detail?.anonymous ? 'A' : (detail?.authorDisplay?.[0] || 'M')}
                    </div>
                    <div>
                      <div className="text-xs font-bold text-on-surface">
                        {detail?.anonymous ? 'Người dùng ẩn danh' : detail?.authorDisplay || 'Mẹ bầu CareBridge'}
                      </div>
                      <div className="text-[11px] text-outline">
                        {detail?.pregnancyWeek && `Tuần thai: ${detail.pregnancyWeek} `}
                        {detail?.babyAgeMonths && `Tuổi bé: ${detail.babyAgeMonths} tháng `}
                        • {timeAgo(detail?.createdAt || selectedSummary?.createdAt || '')}
                      </div>
                    </div>
                  </div>

                  <span
                    className={`py-1 px-3 rounded-full text-xs font-bold ${
                      detail?.urgency === 'URGENT' ? 'bg-error-container text-error' : 'bg-[#FFF3E0] text-[#E65100]'
                    }`}
                  >
                    {detail?.urgency === 'URGENT' ? 'Khẩn cấp' : 'Thường'}
                  </span>
                </div>

                {/* Full Question Body */}
                <div className="bg-surface rounded-2xl p-4 border border-outline-variant/60 shadow-xs space-y-2">
                  <p className="text-xs font-bold text-outline uppercase tracking-wider m-0">Nội dung chi tiết câu hỏi</p>
                  <p className="text-sm text-on-surface leading-relaxed whitespace-pre-line m-0">
                    {detail?.body || 'Không có nội dung mô tả bổ sung.'}
                  </p>
                  <button
                    onClick={toggleQuestionLike}
                    className={`flex items-center gap-1 text-xs font-semibold cursor-pointer ${detail?.isLiked ? 'text-primary' : 'text-outline hover:text-primary'}`}
                  >
                    <span className="material-symbols-outlined text-base">{detail?.isLiked ? 'favorite' : 'favorite_border'}</span>
                    {detail?.likeCount ?? 0} tim
                  </button>
                </div>

                {/* Attached Images Gallery (Uploaded from Mobile App) */}
                {detail?.imageUrls && detail.imageUrls.length > 0 && (
                  <div className="space-y-2">
                    <p className="text-xs font-bold text-outline uppercase tracking-wider m-0 flex items-center gap-1">
                      <span className="material-symbols-outlined text-sm text-primary">photo_library</span>
                      Hình ảnh đính kèm ({detail.imageUrls.length})
                    </p>
                    <div className="grid grid-cols-2 sm:grid-cols-3 gap-3">
                      {detail.imageUrls.map((url, idx) => (
                        <div
                          key={idx}
                          onClick={() => setPreviewImage(url)}
                          className="relative aspect-square rounded-2xl overflow-hidden border border-outline-variant/60 bg-surface cursor-pointer group shadow-xs hover:shadow-md transition-all"
                        >
                          <img
                            src={url}
                            alt={`Ảnh đính kèm ${idx + 1}`}
                            className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300"
                          />
                          <div className="absolute inset-0 bg-black/30 opacity-0 group-hover:opacity-100 flex items-center justify-center transition-opacity text-white">
                            <span className="material-symbols-outlined text-xl">zoom_in</span>
                          </div>
                        </div>
                      ))}
                    </div>
                  </div>
                )}

                {/* Existing Answers List */}
                {detail?.answers && detail.answers.length > 0 && (
                  <div className="space-y-3 pt-2">
                    <p className="text-xs font-bold text-outline uppercase tracking-wider m-0 flex items-center gap-1">
                      <span className="material-symbols-outlined text-sm text-primary">forum</span>
                      Các phản hồi đã có ({detail.answers.length})
                    </p>

                    <div className="space-y-3">
                      {detail.answers.map((ans) => (
                        <div
                          key={ans.id}
                          className={`p-4 rounded-2xl border space-y-1.5 ${
                            ans.expertLabeled
                              ? 'border-emerald-300 bg-emerald-50/70 text-emerald-950'
                              : 'border-outline-variant/50 bg-surface'
                          }`}
                        >
                          <div className="flex items-center justify-between text-xs">
                            <span className="font-bold flex items-center gap-1">
                              {ans.expertLabeled && (
                                <span className="material-symbols-outlined text-sm text-emerald-700">verified</span>
                              )}
                              {ans.authorDisplay || (ans.expertLabeled ? 'Chuyên gia' : 'Thành viên')}
                            </span>
                            <span className="text-[11px] opacity-75">{timeAgo(ans.createdAt)}</span>
                          </div>
                          <p className="text-xs leading-relaxed whitespace-pre-line m-0">{ans.body}</p>
                          {ans.imageUrls.length > 0 && (
                            <div className="grid grid-cols-3 gap-2 pt-1">
                              {ans.imageUrls.map((url, index) => (
                                <img key={url} src={url} alt={`Ảnh phản hồi ${index + 1}`} className="aspect-square w-full rounded-xl object-cover border border-outline-variant/50" />
                              ))}
                            </div>
                          )}
                          <button
                            onClick={() => toggleAnswerLike(ans.id)}
                            className={`flex items-center gap-1 pt-1 text-[11px] font-semibold cursor-pointer ${ans.liked ? 'text-primary' : 'text-outline hover:text-primary'}`}
                          >
                            <span className="material-symbols-outlined text-sm">{ans.liked ? 'favorite' : 'favorite_border'}</span>
                            {ans.likeCount}
                          </button>
                          {ans.authorId === currentUserId && (
                            <div className="flex gap-3 pt-1 text-[11px] font-semibold">
                              <button onClick={() => startEditingAnswer(ans)} className="cursor-pointer text-primary hover:underline">Sửa</button>
                              <button onClick={() => void deleteAnswer(ans.id)} className="cursor-pointer text-error hover:underline">Xóa</button>
                            </div>
                          )}
                        </div>
                      ))}
                    </div>
                  </div>
                )}
              </>
            )}

            {/* Expert Guidelines Banner */}
            <div className="rounded-2xl border border-primary/20 bg-primary-container/20 p-4">
              <div className="flex items-start gap-3">
                <span className="material-symbols-outlined text-primary text-xl mt-0.5">verified</span>
                <p className="text-xs text-on-surface-variant leading-relaxed m-0">
                  Câu trả lời của bạn sẽ được hiển thị kèm huy hiệu <strong>Chuyên gia Y tế CareBridge</strong>.
                  Vui lòng tư vấn y khoa chính xác, văn phong chuẩn mực và không tự ý kê đơn thuốc trực tiếp.
                </p>
              </div>
            </div>
          </div>

          {/* Answer Editor Textarea & Submit or Locked Banner */}
          {detail?.status === 'LOCKED' ? (
            <div className="p-4 border-t border-surface-container-highest bg-surface-container-low/50 flex items-center gap-3 text-error">
              <span className="material-symbols-outlined text-2xl text-error shrink-0">lock</span>
              <div>
                <p className="text-xs font-bold text-on-surface m-0">Câu hỏi đã bị khóa thảo luận</p>
                <p className="text-[11px] text-on-surface-variant m-0 mt-0.5">Kiểm duyệt viên đã khóa câu hỏi này nên không thể gửi câu trả lời mới.</p>
              </div>
            </div>
          ) : (
            <div className="p-4 border-t border-surface-container-highest bg-surface space-y-3">
              {editingAnswerId && <div className="flex items-center justify-between rounded-xl bg-primary-container/30 px-3 py-2 text-xs text-on-surface"><span>Đang chỉnh sửa câu trả lời</span><button onClick={cancelEditingAnswer} className="cursor-pointer font-semibold text-primary">Hủy</button></div>}
              <textarea
                rows={4}
                value={answerText}
                onChange={(e) => setAnswerText(e.target.value)}
                placeholder="Nhập câu trả lời tư vấn chuyên môn chi tiết cho mẹ bầu..."
                className="w-full rounded-2xl border border-outline-variant bg-surface p-3.5 text-xs text-on-surface leading-relaxed outline-none focus:border-primary font-sans resize-none"
              />
              <input ref={imageInputRef} type="file" accept="image/*" className="hidden" onChange={(event) => event.target.files?.[0] && queueAnswerImage(event.target.files[0])} />
              <div className="flex flex-wrap gap-2">
                {existingAnswerImageUrls.map((url) => <div key={url} className="relative h-16 w-16"><img src={url} alt="Ảnh hiện có" className="h-full w-full rounded-xl object-cover" /><button onClick={() => setExistingAnswerImageUrls((urls) => urls.filter((image) => image !== url))} className="absolute -right-1 -top-1 flex h-5 w-5 items-center justify-center rounded-full bg-error text-[11px] text-white cursor-pointer">×</button></div>)}
                {pendingAnswerImages.map(({ previewUrl }) => <div key={previewUrl} className="relative h-16 w-16"><img src={previewUrl} alt="Ảnh sắp gửi" className="h-full w-full rounded-xl object-cover" /><button onClick={() => setPendingAnswerImages((images) => { const removed = images.find((image) => image.previewUrl === previewUrl); if (removed) URL.revokeObjectURL(removed.previewUrl); return images.filter((image) => image.previewUrl !== previewUrl); })} className="absolute -right-1 -top-1 flex h-5 w-5 items-center justify-center rounded-full bg-error text-[11px] text-white cursor-pointer">×</button></div>)}
                {existingAnswerImageUrls.length + pendingAnswerImages.length < 3 && <button onClick={() => imageInputRef.current?.click()} disabled={submitting} className="flex h-16 items-center gap-1 rounded-xl border border-dashed border-outline-variant px-3 text-xs text-outline hover:text-primary cursor-pointer disabled:opacity-50"><span className="material-symbols-outlined text-base">add_photo_alternate</span>Thêm ảnh</button>}
              </div>
              <div className="flex items-center justify-between">
                <span className="text-[11px] text-outline">{answerText.length} / 2 000 ký tự</span>
                <button
                  onClick={postAnswer}
                  disabled={!answerText.trim() || submitting}
                  className="flex items-center gap-2 py-2.5 px-6 rounded-full bg-primary text-on-primary font-semibold text-xs hover:brightness-110 disabled:opacity-50 cursor-pointer shadow-md"
                >
                  <span className="material-symbols-outlined text-base">send</span>
                  {submitting ? 'Đang lưu...' : editingAnswerId ? 'Lưu thay đổi' : 'Gửi câu trả lời'}
                </button>
              </div>
            </div>
          )}
        </div>
      ) : (
        <div className="w-1/2 hidden md:flex flex-col items-center justify-center rounded-2xl bg-surface border border-outline-variant/70 shadow-md p-8 text-center text-outline">
          <span className="material-symbols-outlined text-5xl block mb-2 opacity-30">forum</span>
          <p className="text-base font-bold text-on-surface mb-1">Chọn một câu hỏi để trả lời</p>
          <p className="text-xs text-outline max-w-xs leading-relaxed">
            Nhấp vào câu hỏi trong hàng đợi bên trái để xem đầy đủ nội dung bài viết, hình ảnh đính kèm từ mẹ bầu và gửi tư vấn chuyên môn.
          </p>
        </div>
      )}

      {/* Lightbox Image Preview Modal */}
      {previewImage && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center bg-black/80 p-4"
          onClick={() => setPreviewImage(null)}
        >
          <div className="relative max-w-4xl max-h-[90vh] flex flex-col items-center justify-center">
            <button
              onClick={() => setPreviewImage(null)}
              className="absolute -top-10 right-0 text-white hover:text-gray-300 font-bold text-sm flex items-center gap-1 cursor-pointer"
            >
              <span className="material-symbols-outlined text-xl">close</span> Đóng
            </button>
            <img
              src={previewImage}
              alt="Ảnh đính kèm"
              className="max-w-full max-h-[85vh] rounded-2xl object-contain shadow-2xl border border-white/20"
            />
          </div>
        </div>
      )}
    </div>
  );
}
