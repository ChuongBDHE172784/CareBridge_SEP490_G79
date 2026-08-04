package com.carebridge.backend.recommendation.service;

import com.carebridge.backend.content.entity.ContentItem;
import com.carebridge.backend.content.entity.ContentStage;
import com.carebridge.backend.content.entity.ContentStatus;
import com.carebridge.backend.content.entity.ContentType;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class RecommendationEligibilityPolicy {

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

    public boolean isStageWide(ContentItem item) {
        return item.getEligibleFromWeek() == null && item.getEligibleToWeek() == null;
    }

    public int inclusiveWidth(ContentItem item) {
        if (isStageWide(item)) return 43;
        return item.getEligibleToWeek() - item.getEligibleFromWeek() + 1;
    }
}
