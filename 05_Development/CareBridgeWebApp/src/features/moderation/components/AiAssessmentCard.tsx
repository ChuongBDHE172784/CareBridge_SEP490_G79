import { useState } from 'react';
import type { AiAssessment, AiFeedbackVerdict } from '../models/moderation';
import {
  AI_CLASSIFICATION_LABELS,
  AI_RECOMMENDED_ACTION_LABELS,
  AI_SEVERITY_LABELS,
} from '../models/moderation';
import { submitAiFeedback } from '../services/moderationApi';

function formatDateTime(iso: string): string {
  return new Date(iso).toLocaleString('vi-VN', { dateStyle: 'short', timeStyle: 'short' });
}

const CLASSIFICATION_STYLES: Record<string, string> = {
  SAFE: 'bg-primary-container text-on-primary-container',
  VIOLATION: 'bg-error-container text-error',
  UNCERTAIN: 'bg-surface-container-high text-on-surface-variant',
};

const SEVERITY_STYLES: Record<string, string> = {
  LOW: 'bg-surface-container-high text-on-surface-variant',
  MEDIUM: 'bg-surface-container-high text-on-surface-variant',
  HIGH: 'bg-error-container text-error',
  CRITICAL: 'bg-error text-on-primary',
};

/**
 * CB-MOD-IMP-016: read-only AI evidence panel + agree/disagree feedback. Feedback is stored
 * for audit/precision measurement only — it never changes the case or any policy by itself.
 */
export default function AiAssessmentCard({ assessment, onFeedbackSubmitted }: {
  assessment: AiAssessment;
  onFeedbackSubmitted?: () => void;
}) {
  const [feedbackNote, setFeedbackNote] = useState('');
  const [feedbackSubmitting, setFeedbackSubmitting] = useState<AiFeedbackVerdict | null>(null);
  const [feedbackError, setFeedbackError] = useState('');
  const [localVerdict, setLocalVerdict] = useState<AiFeedbackVerdict | null>(assessment.myFeedbackVerdict);

  const sendFeedback = async (verdict: AiFeedbackVerdict) => {
    setFeedbackSubmitting(verdict);
    setFeedbackError('');
    try {
      await submitAiFeedback(assessment.assessmentId, verdict, feedbackNote.trim() || undefined);
      setLocalVerdict(verdict);
      onFeedbackSubmitted?.();
    } catch (err: unknown) {
      const message = (err as { response?: { data?: { message?: string } } })?.response?.data?.message;
      setFeedbackError(message || 'Không gửi được đánh giá, vui lòng thử lại.');
    } finally {
      setFeedbackSubmitting(null);
    }
  };

  return (
    <div className="bg-surface rounded-2xl p-6 shadow-sm border border-surface-container-highest">
      <div className="mb-3 flex items-center gap-2">
        <span className="material-symbols-outlined text-xl text-primary">smart_toy</span>
        <h2 className="m-0 text-base font-bold text-on-surface">Đánh giá của AI</h2>
      </div>

      {assessment.status === 'FAILED' ? (
        <div className="rounded-md bg-surface-container-low p-4 text-sm text-on-surface-variant">
          <p className="m-0">
            Lần quét gần nhất <strong className="text-error">thất bại</strong>
            {assessment.errorCode ? ` (mã lỗi: ${assessment.errorCode})` : ''} — kết quả này không có
            nghĩa nội dung an toàn hay vi phạm.
          </p>
        </div>
      ) : (
        <>
          <div className="mb-3 flex flex-wrap items-center gap-2">
            {assessment.classification && (
              <span className={`rounded-md px-2.5 py-1 text-xs font-semibold ${CLASSIFICATION_STYLES[assessment.classification] ?? ''}`}>
                {AI_CLASSIFICATION_LABELS[assessment.classification]}
              </span>
            )}
            {assessment.overallSeverity && (
              <span className={`rounded-md px-2 py-0.5 text-xs font-semibold ${SEVERITY_STYLES[assessment.overallSeverity] ?? ''}`}>
                Mức độ: {AI_SEVERITY_LABELS[assessment.overallSeverity]}
              </span>
            )}
            {assessment.confidence != null && (
              <span className="text-xs text-on-surface-variant">
                Độ tin cậy: {Math.round(assessment.confidence * 100)}%
              </span>
            )}
          </div>

          {assessment.recommendedAction && (
            <p className="mb-2 text-xs text-on-surface-variant">
              Khuyến nghị của AI (chỉ tham khảo): <strong>{AI_RECOMMENDED_ACTION_LABELS[assessment.recommendedAction]}</strong>
            </p>
          )}

          {assessment.explanation && (
            <p className="mb-3 rounded-md bg-surface-container-low p-3 text-sm text-on-surface">
              {assessment.explanation}
            </p>
          )}

          {assessment.matches.length > 0 && (
            <div className="mb-3">
              <p className="mb-2 text-[11px] font-semibold uppercase tracking-[0.05em] text-outline">
                Chính sách khớp
              </p>
              <div className="flex flex-col gap-2">
                {assessment.matches.map((match) => (
                  <div key={match.policyCode} className="rounded-md border border-outline-variant p-3">
                    <div className="mb-1 flex flex-wrap items-center gap-2">
                      <span className="text-xs font-semibold text-on-surface">{match.policyCode}</span>
                      <span className={`rounded-md px-2 py-0.5 text-[11px] font-semibold ${SEVERITY_STYLES[match.severity] ?? ''}`}>
                        {AI_SEVERITY_LABELS[match.severity]}
                      </span>
                      <span className="text-[11px] text-on-surface-variant">
                        {Math.round(match.confidence * 100)}%
                      </span>
                    </div>
                    {match.explanation && (
                      <p className="m-0 mb-1 text-xs text-on-surface-variant">{match.explanation}</p>
                    )}
                    {match.evidence.map((quote) => (
                      <p key={quote} className="m-0 mt-1 border-l-2 border-outline-variant pl-2 text-xs italic text-on-surface">
                        “{quote}”
                      </p>
                    ))}
                  </div>
                ))}
              </div>
            </div>
          )}
        </>
      )}

      <p className="mb-3 text-[11px] text-outline">
        Mô hình: {assessment.model} · Quét lúc: {formatDateTime(assessment.createdAt)} · Phiên bản chính
        sách: {assessment.policySetHash.slice(0, 8)}. AI chỉ hỗ trợ đánh giá và ưu tiên — quyết định cuối
        cùng luôn thuộc về kiểm duyệt viên.
      </p>

      {assessment.status === 'COMPLETED' && (
        <div className="border-t border-outline-variant pt-3">
          <p className="mb-2 text-[11px] font-semibold uppercase tracking-[0.05em] text-outline">
            Bạn đánh giá kết quả AI thế nào?
          </p>
          {localVerdict ? (
            <p className="m-0 text-xs text-on-surface-variant">
              Bạn đã ghi nhận: <strong>{localVerdict === 'AGREE' ? 'Đồng ý với AI' : 'Không đồng ý với AI'}</strong>
            </p>
          ) : (
            <>
              <textarea
                value={feedbackNote}
                onChange={(e) => setFeedbackNote(e.target.value)}
                placeholder="Ghi chú (tuỳ chọn)..."
                rows={2}
                maxLength={500}
                className="mb-2 w-full resize-none rounded-md border border-outline-variant p-2 font-sans text-xs outline-none"
              />
              <div className="flex gap-2">
                <button
                  type="button"
                  disabled={feedbackSubmitting !== null}
                  onClick={() => sendFeedback('AGREE')}
                  className="flex h-8 flex-1 items-center justify-center gap-1 rounded-md bg-primary-container text-xs font-semibold text-on-primary-container disabled:opacity-60"
                >
                  <span className="material-symbols-outlined text-base">thumb_up</span>
                  {feedbackSubmitting === 'AGREE' ? 'Đang gửi...' : 'Đồng ý'}
                </button>
                <button
                  type="button"
                  disabled={feedbackSubmitting !== null}
                  onClick={() => sendFeedback('DISAGREE')}
                  className="flex h-8 flex-1 items-center justify-center gap-1 rounded-md bg-surface-container-highest text-xs font-semibold text-on-surface disabled:opacity-60"
                >
                  <span className="material-symbols-outlined text-base">thumb_down</span>
                  {feedbackSubmitting === 'DISAGREE' ? 'Đang gửi...' : 'Không đồng ý'}
                </button>
              </div>
              {feedbackError && <p className="mt-2 text-xs text-error">{feedbackError}</p>}
            </>
          )}
        </div>
      )}
    </div>
  );
}
