package com.carebridge.backend.content;

import com.carebridge.backend.community.entity.AnswerStatus;
import com.carebridge.backend.community.entity.QuestionStatus;
import com.carebridge.backend.content.dto.request.DashboardFilter;
import com.carebridge.backend.content.entity.ReportStatus;
import com.carebridge.backend.security.rbac.Role;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

// CASE 2.0 Props Isolation Pattern
class CommunityDashboardTestFactory {

    static final LocalDate FROM = LocalDate.parse("2026-06-01");
    static final LocalDate TO = LocalDate.parse("2026-06-30");

    static DashboardFilter makeFilter() {
        return new DashboardFilter(FROM, TO);
    }

    static DashboardFilter makeFilter(LocalDate from, LocalDate to) {
        return new DashboardFilter(from, to);
    }

    static List<Object[]> roleCounts() {
        return List.of(
                new Object[] {Role.MOTHER, 900L},
                new Object[] {Role.FAMILY, 200L},
                new Object[] {Role.EXPERT, 40L},
                new Object[] {Role.MODERATOR, 5L},
                new Object[] {Role.CONTENT_ADMIN, 3L},
                new Object[] {Role.PARTNER, 30L},
                new Object[] {Role.SYSTEM_ADMIN, 2L});
    }

    static List<Object[]> questionStatusCounts() {
        return List.of(
                new Object[] {QuestionStatus.PENDING, 12L},
                new Object[] {QuestionStatus.APPROVED, 3300L},
                new Object[] {QuestionStatus.HIDDEN, 80L},
                new Object[] {QuestionStatus.LOCKED, 8L});
    }

    static List<Object[]> answerStatusCounts() {
        return List.of(
                new Object[] {AnswerStatus.PENDING, 30L},
                new Object[] {AnswerStatus.APPROVED, 9700L},
                new Object[] {AnswerStatus.HIDDEN, 70L});
    }

    static List<Object[]> reportStatusCounts() {
        return List.of(
                new Object[] {ReportStatus.PENDING, 5L},
                new Object[] {ReportStatus.RESOLVED, 220L},
                new Object[] {ReportStatus.DISMISSED, 60L});
    }

    static Map<String, Long> expectedRoleMap() {
        return Map.of("MOTHER", 900L, "FAMILY", 200L, "EXPERT", 40L,
                "MODERATOR", 5L, "CONTENT_ADMIN", 3L, "PARTNER", 30L, "SYSTEM_ADMIN", 2L);
    }
}
