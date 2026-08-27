package com.carebridge.backend.recommendation.service;

import com.carebridge.backend.content.entity.ContentItem;
import java.util.Comparator;

/**
 * Bộ xếp hạng (Ranker) các ứng viên bài viết gợi ý theo thứ tự ưu tiên đa tiêu chí:
 * 1. Độ ưu tiên hệ thống (priority): Giảm dần (bài ghim/ưu tiên cao đứng trước).
 * 2. Số lượng tag y tế trùng khớp (matchedCount): Giảm dần (nhiều tag khớp đứng trước).
 * 3. Phạm vi áp dụng (stageWide): Bài viết nhắm tuần cụ thể đứng trước bài chung toàn giai đoạn.
 * 4. Độ rộng khoảng tuần thai (windowWidth): Tăng dần (khoảng tuần càng hẹp/chính xác đứng trước).
 * 5. Ngày xuất bản (publishedAt): Mới nhất đứng trước (DESC).
 * 6. Tiêu chí phụ: ID bài viết (ASC) để đảm bảo tính tất định (deterministic sorting).
 */
@org.springframework.stereotype.Component
public class RecommendationRanker {
    public Comparator<Candidate> comparator() {
        return Comparator
                .comparingInt((Candidate value) -> value.priority()).reversed()
                .thenComparing(Comparator.comparingInt(Candidate::matchedCount).reversed())
                .thenComparingInt(value -> value.stageWide() ? 1 : 0)
                .thenComparingInt(Candidate::windowWidth)
                .thenComparing(value -> value.item().getPublishedAt(), Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(value -> value.item().getId().toString().toLowerCase(java.util.Locale.ROOT));
    }

    public record Candidate(ContentItem item, int matchedCount, boolean stageWide, int windowWidth, int priority) {}
}
