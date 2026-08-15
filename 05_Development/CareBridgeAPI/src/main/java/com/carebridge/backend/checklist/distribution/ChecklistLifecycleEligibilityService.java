package com.carebridge.backend.checklist.distribution;

import com.carebridge.backend.checklist.model.ChecklistAnchorType;
import com.carebridge.backend.checklist.model.ChecklistRangeUnit;
import com.carebridge.backend.content.entity.ContentStage;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/**
 * Service tính toán và đánh giá tính đủ điều kiện theo Vòng đời y tế (Giai đoạn Thai kỳ, Sau sinh, Chăm con).
 * Áp dụng thuật toán mốc neo lịch chuẩn (Named-Anchor Calendar Algorithm) để xác định tuần thai hoặc ngày tuổi của bé.
 */
public class ChecklistLifecycleEligibilityService {

    private static final long PREGNANCY_TERM_DAYS = 280; // Chu kỳ thai chuẩn: 280 ngày (40 tuần)

    /**
     * Đánh giá xem tại ngày chỉ định (effectiveDate), người dùng có nằm trong cửa sổ áp dụng của Checklist hay không.
     *
     * @param stage Giai đoạn (PRE_PREGNANCY, PREGNANCY, POSTPARTUM)
     * @param substage Cấu hình khoảng tuần/ngày áp dụng (ví dụ: Tuần 4 - Tuần 8 thai kỳ)
     * @param dates Các mốc ngày y tế (LMP - Kỳ kinh cuối, EDD - Ngày dự sinh, BirthDate - Ngày sinh em bé)
     * @param effectiveDate Ngày cần kiểm tra hiệu lực
     * @return ChecklistEligibilityDecision Quyết định đủ điều kiện cùng cửa sổ thời gian (windowStart, windowEnd)
     */
    public ChecklistEligibilityDecision evaluate(
            ContentStage stage,
            ChecklistLifecycleEligibility substage,
            ChecklistLifecycleDates dates,
            LocalDate effectiveDate) {
        Objects.requireNonNull(dates, "Lifecycle dates are required");
        Objects.requireNonNull(effectiveDate, "Effective date is required");

        // Nếu không yêu cầu ràng buộc giai đoạn -> Mặc định hợp lệ
        if (stage == null && substage == null) {
            return ChecklistEligibilityDecision.neutral();
        }

        // Giai đoạn chuẩn bị mang thai (Pre-Pregnancy) không cần mốc neo thai kỳ
        if (stage == ContentStage.PRE_PREGNANCY) {
            if (substage == null || !Boolean.TRUE.equals(substage.getActive())
                    || !stage.name().equals(substage.getStage())
                    || substage.getAnchorType() != ChecklistAnchorType.NONE) {
                throw new IllegalArgumentException("PRE_PREGNANCY requires the neutral lifecycle substage");
            }
            return ChecklistEligibilityDecision.neutral();
        }

        // Validate tính hợp lệ của các mốc ngày y tế
        validateLifecycleDates(stage, dates);
        validate(stage, substage);

        // Lấy ngày mốc neo (Ví dụ: Ngày LMP hoặc Ngày sinh của bé)
        LocalDate anchor = anchorDate(substage.getAnchorType(), dates);
        if (anchor == null) {
            return ChecklistEligibilityDecision.failure("LIFECYCLE_ANCHOR_MISSING");
        }

        // Tính ngày bắt đầu và kết thúc của cửa sổ hiệu lực
        LocalDate start = add(anchor, substage.getRangeUnit(), substage.getStartInclusive());
        boolean openEnded = substage.getEndInclusive() == Integer.MAX_VALUE;
        LocalDate end = openEnded ? null
                : add(anchor, substage.getRangeUnit(), substage.getEndInclusive());

        // Tính toán vị trí hiện tại (ví dụ: đang ở tuần thứ mấy sau mốc neo)
        long position = completedUnits(anchor, effectiveDate, substage.getRangeUnit());
        boolean eligible = position >= substage.getStartInclusive()
                && (openEnded || position <= substage.getEndInclusive());

        return eligible
                ? ChecklistEligibilityDecision.eligible(anchor, start, end)
                : ChecklistEligibilityDecision.outside(anchor, start, end);
    }

    public Instant dueAt(
            ChecklistAnchorType anchorType,
            ChecklistLifecycleDates dates,
            int offsetDays,
            ZoneId zoneId) {
        return dueAt(anchorType, dates, offsetDays, ChecklistRangeUnit.DAY, zoneId);
    }

    public Instant dueAt(
            ChecklistAnchorType anchorType,
            ChecklistLifecycleDates dates,
            int offset,
            ChecklistRangeUnit unit,
            ZoneId zoneId) {
        if (anchorType == null || anchorType == ChecklistAnchorType.NONE) {
            throw new IllegalArgumentException("A named due anchor is required");
        }
        if (offset < 0) {
            throw new IllegalArgumentException("Due offset must be non-negative");
        }
        Objects.requireNonNull(unit, "Due offset unit is required");
        LocalDate anchor = anchorDate(anchorType, Objects.requireNonNull(dates, "Lifecycle dates are required"));
        if (anchor == null) {
            throw new IllegalArgumentException("Named due anchor is missing");
        }
        return add(anchor, unit, offset)
                .atStartOfDay(Objects.requireNonNull(zoneId, "Timezone is required"))
                .toInstant();
    }

    public boolean hasAnchor(ChecklistAnchorType anchorType, ChecklistLifecycleDates dates) {
        if (anchorType == null || anchorType == ChecklistAnchorType.NONE || dates == null) {
            return false;
        }
        return anchorDate(anchorType, dates) != null;
    }

    private static void validate(ContentStage stage, ChecklistLifecycleEligibility substage) {
        if (stage == null || substage == null || substage.getStage() == null
                || !stage.name().equals(substage.getStage())) {
            throw new IllegalArgumentException("Lifecycle stage and substage must match");
        }
        if (!Boolean.TRUE.equals(substage.getActive())) {
            throw new IllegalArgumentException("Lifecycle substage must be active");
        }
        if (substage.getStartInclusive() == null || substage.getEndInclusive() == null
                || substage.getStartInclusive() < 0
                || substage.getEndInclusive() < substage.getStartInclusive()) {
            throw new IllegalArgumentException("Lifecycle range is malformed");
        }
        ChecklistAnchorType anchor = Objects.requireNonNull(substage.getAnchorType(), "Anchor is required");
        boolean valid = switch (stage) {
            case PREGNANCY -> anchor == ChecklistAnchorType.LMP || anchor == ChecklistAnchorType.EDD;
            case POSTPARTUM -> anchor == ChecklistAnchorType.DELIVERY_DATE;
            case BABY_CARE -> anchor == ChecklistAnchorType.BIRTH_DATE;
            default -> false;
        };
        if (!valid) {
            throw new IllegalArgumentException("Named anchor contradicts lifecycle stage");
        }
        Objects.requireNonNull(substage.getRangeUnit(), "Range unit is required");
    }

    private static void validateLifecycleDates(ContentStage stage, ChecklistLifecycleDates dates) {
        if (stage == ContentStage.PREGNANCY
                && dates.lastMenstrualDate() != null
                && dates.estimatedDueDate() != null
                && dates.estimatedDueDate().isBefore(dates.lastMenstrualDate())) {
            throw new IllegalArgumentException("EDD cannot be earlier than LMP");
        }
    }

    private static LocalDate anchorDate(ChecklistAnchorType type, ChecklistLifecycleDates dates) {
        return switch (type) {
            case LMP -> dates.lastMenstrualDate() != null
                    ? dates.lastMenstrualDate()
                    : deriveLmp(dates.estimatedDueDate());
            case EDD -> dates.estimatedDueDate();
            case DELIVERY_DATE -> dates.deliveryDate();
            case BIRTH_DATE -> dates.birthDate();
            case NONE -> null;
        };
    }

    private static LocalDate deriveLmp(LocalDate estimatedDueDate) {
        return estimatedDueDate == null ? null : estimatedDueDate.minusDays(PREGNANCY_TERM_DAYS);
    }

    private static LocalDate add(LocalDate anchor, ChecklistRangeUnit unit, int amount) {
        return switch (unit) {
            case DAY -> anchor.plusDays(amount);
            case WEEK -> anchor.plusWeeks(amount);
            case MONTH -> anchor.plusMonths(amount);
        };
    }

    private static long completedUnits(LocalDate anchor, LocalDate effectiveDate, ChecklistRangeUnit unit) {
        if (effectiveDate.isBefore(anchor)) {
            return -1;
        }
        return switch (unit) {
            case DAY -> ChronoUnit.DAYS.between(anchor, effectiveDate);
            case WEEK -> ChronoUnit.DAYS.between(anchor, effectiveDate) / 7;
            case MONTH -> completedMonths(anchor, effectiveDate);
        };
    }

    private static long completedMonths(LocalDate anchor, LocalDate effectiveDate) {
        long months = ChronoUnit.MONTHS.between(YearMonth.from(anchor), YearMonth.from(effectiveDate));
        LocalDate anniversary = anchor.plusMonths(months);
        return effectiveDate.isBefore(anniversary) ? months - 1 : months;
    }
}
