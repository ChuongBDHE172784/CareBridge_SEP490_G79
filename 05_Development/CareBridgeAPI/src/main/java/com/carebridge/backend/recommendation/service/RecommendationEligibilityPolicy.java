package com.carebridge.backend.recommendation.service;

import com.carebridge.backend.content.entity.ContentItem;
import com.carebridge.backend.content.entity.ContentStage;
import com.carebridge.backend.content.entity.ContentStatus;
import com.carebridge.backend.content.entity.ContentType;
import org.springframework.stereotype.Component;

@Component
public class RecommendationEligibilityPolicy {

    /**
     * Kiểm tra điều kiện lọc cứng (Hard Eligibility) của bài viết:
     * 1. Phải là bài viết (ARTICLE), đã được duyệt (APPROVED).
     * 2. Phải khớp chính xác giai đoạn thai kỳ (Stage) của mẹ.
     * 3. Trong thai kỳ: Tuần thai hiện tại của mẹ phải nằm trong khoảng [eligibleFromWeek, eligibleToWeek],
     *    hoặc bài viết áp dụng cho toàn thai kỳ (cả 2 trường đều null).
     */
    public boolean isHardEligible(ContentItem item, RecommendationContext context) {
        if (item == null || item.getType() != ContentType.ARTICLE || item.getStatus() != ContentStatus.APPROVED
                || item.getStage() != context.stage()) return false;
        Short from = item.getEligibleFromWeek();
        Short to = item.getEligibleToWeek();
        if (context.stage() != ContentStage.PREGNANCY) return from == null && to == null;
        if (from == null || to == null) return from == null && to == null;
        if (context.weekState() != RecommendationContext.WeekState.KNOWN) return false;
        return context.pregnancyWeek() >= from && context.pregnancyWeek() <= to;
    }

    /**
     * Kiểm tra xem bài viết có áp dụng chung cho toàn bộ giai đoạn thai kỳ hay không (không giới hạn tuần).
     */
    public boolean isStageWide(ContentItem item) {
        return item.getEligibleFromWeek() == null && item.getEligibleToWeek() == null;
    }

    /**
     * Tính độ rộng khoảng tuần thai của bài viết (số tuần).
     * Khoảng tuần càng hẹp thì bài viết càng nhắm mục tiêu chính xác hơn (được ưu tiên xếp hạng cao hơn).
     */
    public int inclusiveWidth(ContentItem item) {
        if (isStageWide(item)) return 43;
        return item.getEligibleToWeek() - item.getEligibleFromWeek() + 1;
    }
}
