package com.carebridge.backend.aimoderation.service;

import com.carebridge.backend.aimoderation.entity.AiClassification;
import com.carebridge.backend.aimoderation.entity.AiContentAssessment;
import com.carebridge.backend.aimoderation.policy.AiContentHasher;
import com.carebridge.backend.community.entity.AnswerStatus;
import com.carebridge.backend.community.entity.CommunityAnswer;
import com.carebridge.backend.community.entity.CommunityQuestion;
import com.carebridge.backend.community.entity.QuestionStatus;
import com.carebridge.backend.community.repository.CommunityAnswerRepository;
import com.carebridge.backend.community.repository.CommunityQuestionRepository;
import com.carebridge.backend.content.entity.ReportTargetType;
import com.carebridge.backend.expert.handler.IExpertEventHandler;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service chuyên trách áp dụng phán quyết kiểm duyệt của AI vào thực thể nội dung cộng đồng (Câu hỏi / Câu trả lời).
 *
 * Nhiệm vụ chính:
 * 1. Khóa và kiểm tra tính toàn vẹn phiên bản nội dung (Content Hash) trước khi cập nhật.
 * 2. Nếu AI đánh giá SAFE (An toàn) -> Tự động duyệt APPROVED (công khai trên Feed).
 * 3. Nếu AI đánh giá VIOLATION/UNCERTAIN hoặc hệ thống gặp sự cố -> Chuyển PENDING và gửi vào hàng đợi duyệt thủ công của Moderator.
 */
@Service
@RequiredArgsConstructor
public class AiModerationOutcomeApplier {

    /**
     * Kết quả khóa mục tiêu kiểm duyệt:
     * - READY: Bản ghi hợp lệ, phiên bản nội dung khớp hoàn toàn, sẵn sàng áp dụng phán quyết.
     * - TARGET_GONE: Bản ghi không còn tồn tại trong Database.
     * - SUPERSEDED: Bản ghi đã bị chỉnh sửa (hash thay đổi) hoặc đã bị xóa/thay đổi trạng thái.
     */
    public enum TargetLockResult {
        READY,
        TARGET_GONE,
        SUPERSEDED
    }

    private final CommunityQuestionRepository questionRepository;
    private final CommunityAnswerRepository answerRepository;
    private final IExpertEventHandler expertEventHandler;

    /**
     * Khóa bi quan (Pessimistic Lock) và kiểm tra tính toàn vẹn phiên bản nội dung trước khi ghi nhận kết quả kiểm duyệt.
     * Đảm bảo không áp dụng kết quả của phiên bản cũ nếu người dùng đã chỉnh sửa bài viết trong lúc AI đang quét.
     *
     * @param targetType Loại nội dung (QUESTION, ANSWER, CONTENT)
     * @param targetId UUID của bài viết/câu trả lời
     * @param expectedHash Mã băm SHA-256 nội dung tại thời điểm bắt đầu quét
     * @param forceRescan Cờ quét lại bắt buộc (do Admin yêu cầu)
     * @return Trạng thái khóa TargetLockResult
     */
    @Transactional
    public TargetLockResult acquireTargetLock(
            ReportTargetType targetType, UUID targetId, String expectedHash, boolean forceRescan) {
        return switch (targetType) {
            case QUESTION -> questionRepository.findByIdForModerationUpdate(targetId)
                    .map(question -> (forceRescan || question.getStatus() == QuestionStatus.AI_PENDING)
                            && question.getStatus() != QuestionStatus.DELETED
                            && AiContentHasher.sha256Hex(AiScanTargetResolver.joinTitleAndBody(
                                    question.getTitle(), question.getBody())).equals(expectedHash)
                            ? TargetLockResult.READY
                            : TargetLockResult.SUPERSEDED)
                    .orElse(TargetLockResult.TARGET_GONE);
            case ANSWER -> answerRepository.findByIdForModerationUpdate(targetId)
                    .map(answer -> (forceRescan || answer.getStatus() == AnswerStatus.AI_PENDING)
                            && answer.getStatus() != AnswerStatus.DELETED
                            && AiContentHasher.sha256Hex(answer.getBody()).equals(expectedHash)
                            ? TargetLockResult.READY
                            : TargetLockResult.SUPERSEDED)
                    .orElse(TargetLockResult.TARGET_GONE);
            default -> TargetLockResult.READY; // Tài liệu thư viện CONTENT giữ luồng vòng đời riêng
        };
    }

    /**
     * Áp dụng kết quả đánh giá AI đã hoàn tất.
     * Nếu phân loại là SAFE -> Đánh dấu safe = true để tự động duyệt công khai.
     *
     * @param assessment Bản ghi đánh giá kiểm duyệt từ AI
     */
    @Transactional
    public void applyCompleted(AiContentAssessment assessment) {
        // Kiểm tra xem AI có đánh giá nội dung này an toàn tuyệt đối hay không
        boolean safe = assessment.getClassification() == AiClassification.SAFE;
        apply(assessment.getTargetType(), assessment.getTargetId(), assessment.getContentHash(), safe);
    }

    /**
     * Chuyển nội dung sang chế độ chờ Moderator (con người) duyệt thủ công khi AI nghi ngờ vi phạm hoặc gặp sự cố.
     *
     * @param targetType Loại nội dung
     * @param targetId UUID bài viết
     * @param contentHash Mã băm SHA-256 của nội dung
     */
    @Transactional
    public void applyHumanReview(ReportTargetType targetType, UUID targetId, String contentHash) {
        // safe = false: Không cho phép tự động duyệt, đẩy sang hàng đợi xét duyệt thủ công
        apply(targetType, targetId, contentHash, false);
    }

    /**
     * Điều hướng cập nhật trạng thái theo loại nội dung (Câu hỏi hoặc Câu trả lời).
     */
    private void apply(ReportTargetType targetType, UUID targetId, String expectedHash, boolean safe) {
        switch (targetType) {
            case QUESTION -> applyQuestion(targetId, expectedHash, safe);
            case ANSWER -> applyAnswer(targetId, expectedHash, safe);
            default -> {
                // Cơ chế tự động duyệt qua AI chỉ áp dụng cho Hỏi Đáp cộng đồng (Community Q&A)
            }
        }
    }

    /**
     * [Áp dụng trạng thái cho CÂU HỎI (Community Question)]
     * - Nếu safe = true: Chuyển sang APPROVED (công khai trên Feed).
     * - Nếu safe = false: Chuyển sang PENDING (chờ Moderator xử lý).
     *
     * @param questionId UUID câu hỏi
     * @param expectedHash Mã băm SHA-256 mong đợi
     * @param safe Trạng thái an toàn từ AI
     */
    private void applyQuestion(UUID questionId, String expectedHash, boolean safe) {
        // [Bước 1]: Khóa và lấy bản ghi câu hỏi từ Database với quyền cập nhật
        CommunityQuestion question = questionRepository.findByIdForModerationUpdate(questionId).orElse(null);
        if (question == null || question.getStatus() != QuestionStatus.AI_PENDING) {
            return;
        }

        // [Bước 2]: Kiểm tra lại mã băm nội dung hiện tại để chống xung đột (race condition do người dùng vừa sửa bài)
        String currentText = AiScanTargetResolver.joinTitleAndBody(question.getTitle(), question.getBody());
        if (!AiContentHasher.sha256Hex(currentText).equals(expectedHash)) {
            return;
        }

        // [Bước 3]: Cập nhật trạng thái: Nếu an toàn -> APPROVED, nếu vi phạm/nghi ngờ -> PENDING
        question.setStatus(safe ? QuestionStatus.APPROVED : QuestionStatus.PENDING);
        // [Bước 4]: Lưu trạng thái mới vào bảng 'community_content'
        questionRepository.save(question);
    }

    /**
     * [Áp dụng trạng thái cho CÂU TRẢ LỜI (Community Answer)]
     * - Cần đảm bảo CẢ câu trả lời an toàn VÀ câu hỏi gốc vẫn đang ở trạng thái APPROVED.
     * - Nếu được APPROVED: Tăng answerCount của câu hỏi và kích hoạt sự kiện thưởng điểm/thông báo cho Chuyên gia.
     *
     * @param answerId UUID câu trả lời
     * @param expectedHash Mã băm SHA-256 mong đợi
     * @param safe Trạng thái an toàn từ AI
     */
    private void applyAnswer(UUID answerId, String expectedHash, boolean safe) {
        // [Bước 1]: Khóa và lấy bản ghi câu trả lời từ Database
        CommunityAnswer answer = answerRepository.findByIdForModerationUpdate(answerId).orElse(null);
        if (answer == null || answer.getStatus() != AnswerStatus.AI_PENDING) {
            return;
        }

        // [Bước 2]: Kiểm tra tính toàn vẹn mã băm nội dung
        if (!AiContentHasher.sha256Hex(answer.getBody()).equals(expectedHash)) {
            return;
        }

        // [Bước 3]: Kiểm tra xem câu hỏi gốc (Parent Question) có còn đang ở trạng thái APPROVED hay không
        boolean parentStillApproved = safe && questionRepository
                .findByIdForModerationUpdate(answer.getQuestionId())
                .map(question -> question.getStatus() == QuestionStatus.APPROVED)
                .orElse(false);

        // Chỉ duyệt APPROVED câu trả lời nếu cả nội dung câu trả lời an toàn VÀ câu hỏi gốc vẫn APPROVED
        boolean approve = safe && parentStillApproved;
        answer.setStatus(approve ? AnswerStatus.APPROVED : AnswerStatus.PENDING);
        answerRepository.save(answer);

        if (!approve) {
            return;
        }

        // [Bước 4]: Tăng số lượng câu trả lời của câu hỏi gốc (+1 answerCount)
        questionRepository.incrementAnswerCount(answer.getQuestionId());

        // [Bước 5]: Nếu là câu trả lời từ Chuyên gia (EXPERT), kích hoạt event xử lý thưởng/thống kê chuyên gia
        if (answer.isExpertLabeled()) {
            expertEventHandler.onAnswerApproved(answer.getId().toString(), answer.getAuthorId().toString());
        }
    }
}
