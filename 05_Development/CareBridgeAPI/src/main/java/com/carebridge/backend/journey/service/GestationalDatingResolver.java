package com.carebridge.backend.journey.service;

import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.journey.dto.CreateJourneyRequest;
import com.carebridge.backend.journey.dto.UpdateJourneyRequest;
import com.carebridge.backend.journey.entity.GestationalDatingBasis;
import com.carebridge.backend.journey.entity.JourneyStatus;
import com.carebridge.backend.journey.entity.JourneyType;
import com.carebridge.backend.journey.entity.MotherJourney;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/**
 * =========================================================================================
 * CORE BUSINESS ENGINE: TÍNH TOÁN TUẦN THAI & ĐỊNH THỜI GIAN THAI KỲ
 * (GESTATIONAL DATING)
 * =========================================================================================
 * 
 * Mục tiêu y tế / nghiệp vụ:
 * - Chuẩn hóa mốc thời gian thai kỳ (Gestational Age) dựa trên ngày kinh cuối
 * (LMP) hoặc ngày dự sinh (EDD).
 * - Sử dụng quy tắc chuẩn sản khoa Naegele (280 ngày ~ 40 tuần).
 * - Tính toán số tuần đã hoàn thành (completedGestationalWeek: 0..42+), số ngày
 * lẻ (completedGestationalDays: 0..6),
 * tuần hiển thị người dùng (sourceWeekNumber: 1-based), và phân bổ vào 8 giai
 * đoạn khám thai theo WHO Plan.
 */
@Component
public class GestationalDatingResolver {

    /** Phiên bản hợp đồng V1 (Tương thích ngược dữ liệu cũ) */
    public static final int V1 = 1;
    /** Phiên bản hợp đồng V2 (Yêu cầu chỉ định rõ cơ sở LMP hoặc EDD - XOR rule) */
    public static final int V2 = 2;
    /**
     * Chu kỳ thai kỳ tiêu chuẩn sản khoa: 280 ngày (40 tuần) kể từ ngày đầu của chu
     * kỳ kinh cuối (LMP)
     */
    public static final int GESTATION_DAYS = 280;

    /**
     * Kiểm tra xem Hành trình thai kỳ (MotherJourney) đã có quyền thẩm quyền thời
     * gian hợp lệ (Resolved Authority) chưa.
     * 
     * @param journey Đối tượng hành trình của mẹ
     * @return true nếu hành trình đang ở giai đoạn PREGNANCY, trạng thái ACTIVE, có
     *         cơ sở tính tuần thai, có phiên bản > 0 và không bị cách ly/lỗi
     *         (quarantined).
     */
    public static boolean hasResolvedAuthority(MotherJourney journey) {
        return journey != null
                && journey.getJourneyType() == JourneyType.PREGNANCY
                && journey.getStatus() == JourneyStatus.ACTIVE
                && journey.getGestationalDatingBasis() != null
                && journey.getGestationalDatingRevision() != null
                && journey.getGestationalDatingRevision() > 0
                && journey.getGestationalDatingEffectiveAt() != null
                && journey.getGestationalDatingQuarantineReasonCode() == null;
    }

    /**
     * [BƯỚC 1 & 2: Tiếp nhận Request & Validate Nghiệp vụ khi Tạo Hành Trình]
     * Xác định và tính toán tuần thai ban đầu từ CreateJourneyRequest.
     * 
     * @param request         Dữ liệu yêu cầu tạo mới hành trình
     * @param contractVersion Phiên bản hợp đồng API (V1 hoặc V2)
     * @param serverToday     Ngày hiện tại theo múi giờ hệ thống nghiệp vụ
     *                        (Asia/Ho_Chi_Minh)
     * @return Kết quả định thời gian thai kỳ GestationalDatingResolution
     */
    public GestationalDatingResolution resolveCreate(
            CreateJourneyRequest request,
            int contractVersion,
            LocalDate serverToday) {
        // [BƯỚC 1: Tiếp nhận Request]
        Objects.requireNonNull(request, "request");
        JourneyType stage = request.getJourneyType();
        boolean hasDating = hasDating(request.getLastMenstrualDate(), request.getEstimatedDueDate(),
                request.getDatingBasis());

        // [BƯỚC 2: Kiểm tra quy tắc nghiệp vụ (Business Rules)]
        // Quy tắc: Chỉ giai đoạn mang thai (PREGNANCY) mới được phép cung cấp dữ liệu
        // tính tuần thai
        if (stage != JourneyType.PREGNANCY) {
            if (hasDating) {
                throw stageInapplicable();
            }
            return GestationalDatingResolution.unresolved(null, null, false);
        }

        // [BƯỚC 3: Xử lý định dạng & Thuật toán tính toán tuần thai]
        GestationalDatingResolution resolved = resolveShape(
                request.getLastMenstrualDate(),
                request.getEstimatedDueDate(),
                request.getDatingBasis(),
                contractVersion,
                null,
                null,
                serverToday,
                hasDating,
                false,
                false);
        return resolved;
    }

    /**
     * [BƯỚC 1 & 2 & 3: Xử lý Cập nhật Tuần Thai / Thay Đổi Cơ Sở Định Thời Kỳ]
     * 
     * @param current           Bản ghi hành trình hiện tại trong DB
     * @param request           Dữ liệu cập nhật từ người dùng
     * @param contractVersion   Phiên bản hợp đồng (V1/V2)
     * @param serverToday       Ngày hiện hành tại máy chủ
     * @param enteringPregnancy Cờ đánh dấu có đang chuyển đổi từ giai đoạn khác
     *                          sang PREGNANCY hay không
     * @return GestationalDatingResolution chứa tuần thai, ngày chuẩn hóa và WHO
     *         Plan cập nhật
     */
    public GestationalDatingResolution resolveUpdate(
            MotherJourney current,
            UpdateJourneyRequest request,
            int contractVersion,
            LocalDate serverToday,
            boolean enteringPregnancy) {
        // [BƯỚC 1: Tiếp nhận Request & Đối tượng hiện tại]
        Objects.requireNonNull(current, "current");
        Objects.requireNonNull(request, "request");
        JourneyType resultingStage = request.getJourneyType() == null
                ? current.getJourneyType()
                : request.getJourneyType();
        boolean hasDating = hasDating(request.getLastMenstrualDate(), request.getEstimatedDueDate(),
                request.getDatingBasis());
        boolean datingScope = hasDating || enteringPregnancy;

        // [BƯỚC 2: Validate trạng thái áp dụng]
        if (resultingStage != JourneyType.PREGNANCY) {
            if (hasDating) {
                throw stageInapplicable();
            }
            return GestationalDatingResolution.unresolved(
                    current.getLastMenstrualDate(), current.getEstimatedDueDate(), false);
        }

        // [BƯỚC 3.1: Nếu request không thay đổi thông tin ngày thai kỳ, tái sử dụng dữ
        // liệu đã có]
        if (!datingScope) {
            return currentResolution(current, serverToday);
        }

        // [BƯỚC 3.2: Bắt đầu một chu kỳ thai kỳ mới (enteringPregnancy) - không kế thừa
        // ngày cũ]
        if (enteringPregnancy) {
            return resolveShape(
                    request.getLastMenstrualDate(),
                    request.getEstimatedDueDate(),
                    request.getDatingBasis(),
                    contractVersion,
                    null,
                    null,
                    serverToday,
                    hasDating || enteringPregnancy,
                    true,
                    false);
        }

        // [BƯỚC 3.3: Tính toán lại tuần thai với các tham số điều chỉnh mới]
        GestationalDatingResolution resolved = resolveShape(
                request.getLastMenstrualDate(),
                request.getEstimatedDueDate(),
                request.getDatingBasis(),
                contractVersion,
                current.getGestationalDatingBasis(),
                canonicalLmp(
                        current.getGestationalDatingQuarantineReasonCode() == null
                                ? current.getGestationalDatingBasis()
                                : null,
                        current.getLastMenstrualDate(),
                        current.getEstimatedDueDate()),
                serverToday,
                hasDating,
                false,
                hasResolvedAuthority(current));

        // [BƯỚC 3.4: Kiểm tra tính đẳng trị (Idempotent / No-op) nếu dữ liệu tính toán
        // không thay đổi]
        if (contractVersion == V2
                && hasResolvedAuthority(current)
                && resolved.resolved()
                && resolved.basis() == current.getGestationalDatingBasis()
                && Objects.equals(
                        resolved.canonicalLmp(),
                        canonicalLmp(
                                current.getGestationalDatingBasis(),
                                current.getLastMenstrualDate(),
                                current.getEstimatedDueDate()))) {
            return GestationalDatingResolution.noOp(
                    resolved.basis(),
                    resolved.lastMenstrualDate(),
                    resolved.estimatedDueDate(),
                    resolved.canonicalLmp(),
                    resolved.completedGestationalWeek(),
                    resolved.completedGestationalDays(),
                    resolved.sourceWeekNumber(),
                    resolved.plan());
        }
        return resolved;
    }

    /**
     * =========================================================================================
     * THUẬT TOÁN CỐT LÕI 1: TÍNH ĐIỂM NEO KINH CUỐI CHUẨN HÓA (CANONICAL LMP)
     * =========================================================================================
     * 
     * Công thức y tế:
     * 1. Nếu cơ sở là LMP (Last Menstrual Period): Canonical LMP = LMP
     * 2. Nếu cơ sở là EDD (Estimated Due Date): Canonical LMP = EDD - 280 ngày (Quy
     * tắc Naegele ngược)
     * 
     * @param basis             Cơ sở tính toán (LMP, EDD hoặc LMP_DERIVED_FROM_EDD)
     * @param lastMenstrualDate Ngày đầu của chu kỳ kinh nguyệt cuối
     * @param estimatedDueDate  Ngày dự sinh
     * @return Ngày LMP chuẩn hóa làm mốc tính tuổi thai
     */
    public static LocalDate canonicalLmp(
            GestationalDatingBasis basis,
            LocalDate lastMenstrualDate,
            LocalDate estimatedDueDate) {
        if (basis == null) {
            return null;
        }
        return switch (basis) {
            case LMP -> lastMenstrualDate;
            case EDD, LMP_DERIVED_FROM_EDD -> estimatedDueDate == null
                    ? null
                    : estimatedDueDate.minusDays(GESTATION_DAYS);
        };
    }

    /**
     * =========================================================================================
     * THUẬT TOÁN CỐT LÕI 2: TÍNH SỐ TUẦN THAI ĐÃ HOÀN THÀNH (COMPLETED WEEKS -
     * 0-BASED)
     * =========================================================================================
     * 
     * Công thức:
     * - Số ngày thai (Gestational Days) = ChronoUnit.DAYS.between(canonicalLmp,
     * serverToday)
     * - Số tuần hoàn thành = Số ngày thai / 7 (phép chia nguyên)
     * 
     * Ví dụ: Thai 6 tuần 5 ngày -> completedGestationalWeek = 6.
     * 
     * @param canonicalLmp Ngày kinh cuối chuẩn hóa
     * @param serverToday  Ngày hiện tại theo múi giờ hệ thống
     * @return Số tuần thai đã trôi qua trọn vẹn (0, 1, 2, ... 40+)
     */
    public static int completedGestationalWeek(LocalDate canonicalLmp, LocalDate serverToday) {
        if (canonicalLmp == null || serverToday == null) {
            return -1;
        }
        // Tính tổng số ngày kể từ ngày kinh cuối đến hôm nay
        long days = ChronoUnit.DAYS.between(canonicalLmp, serverToday);
        // Chặn lỗi logic y tế: Ngày kinh cuối không thể ở tương lai so với ngày hiện
        // tại
        if (days < 0) {
            throw futureLmp();
        }
        // Chia lấy phần nguyên cho 7 để ra số tuần trọn vẹn
        return Math.toIntExact(days / 7);
    }

    /**
     * =========================================================================================
     * THUẬT TOÁN CỐT LÕI 3: TÍNH SỐ NGÀY LẺ TRONG TUẦN HIỆN TẠI (COMPLETED DAYS:
     * 0..6)
     * =========================================================================================
     * 
     * Công thức:
     * - Số ngày lẻ = Số ngày thai % 7 (phép chia lấy dư)
     * 
     * Ví dụ: Thai 47 ngày = 6 tuần 5 ngày -> completedGestationalDays = 5.
     * 
     * @param canonicalLmp Ngày kinh cuối chuẩn hóa
     * @param serverToday  Ngày hiện tại theo múi giờ hệ thống
     * @return Số ngày lẻ trong tuần thai hiện tại (từ 0 đến 6 ngày)
     */
    public static int completedGestationalDays(LocalDate canonicalLmp, LocalDate serverToday) {
        if (canonicalLmp == null || serverToday == null) {
            return -1;
        }
        long days = ChronoUnit.DAYS.between(canonicalLmp, serverToday);
        if (days < 0) {
            throw futureLmp();
        }
        // Lấy phần dư cho 7 để xác định số ngày lẻ
        return Math.toIntExact(days % 7);
    }

    /**
     * =========================================================================================
     * THUẬT TOÁN CỐT LÕI 4: TÍNH TUẦN THAI HIỂN THỊ CHO NGƯỜI DÙNG (SOURCE WEEK -
     * 1-BASED)
     * =========================================================================================
     * 
     * Quy ước giao diện & Y tế:
     * - Tuần thai hiển thị = completedGestationalWeek + 1.
     * - Ví dụ: Đang ở tuần hoàn thành 0 (ngày 0..6) -> Tuần thai thứ 1 (Tuần 1).
     * Đang ở 6 tuần 5 ngày -> Đang ở Tuần thai thứ 7.
     * 
     * @param completedGestationalWeek Số tuần thai trọn vẹn đã hoàn thành (0-based)
     * @return Số thứ tự tuần thai hiện tại (1-based)
     */
    public static int sourceWeekNumber(int completedGestationalWeek) {
        return completedGestationalWeek < 0 ? -1 : completedGestationalWeek + 1;
    }

    /**
     * =========================================================================================
     * THUẬT TOÁN CỐT LÕI 5: PHÂN BỔ THEO 8 GIAI ĐOẠN KHÁM THAI THEO CHUẨN WHO (WHO
     * PLAN 1..8)
     * =========================================================================================
     * 
     * Khung phân chia kế hoạch chăm sóc thai sản theo khuyến nghị của Tổ chức Y tế
     * Thế giới (WHO):
     * - Plan 1: Tuần 1 – 20 (Khám lần 1 & Sàng lọc quý 1)
     * - Plan 2: Tuần 21 – 25 (Khám lần 2 & Siêu âm hình thái)
     * - Plan 3: Tuần 26 – 29 (Khám lần 3 & Nghiệm pháp dung nạp đường)
     * - Plan 4: Tuần 30 – 33 (Khám lần 4)
     * - Plan 5: Tuần 34 – 35 (Khám lần 5)
     * - Plan 6: Tuần 36 – 37 (Khám lần 6 & Đánh giá ngôi thai)
     * - Plan 7: Tuần 38 – 39 (Khám lần 7 & Chuẩn bị chuyển dạ)
     * - Plan 8: Tuần 40+ (Khám lần 8 & Theo dõi quá ngày dự sinh)
     * 
     * @param sourceWeekNumber Số tuần thai hiển thị (1-based, ví dụ: tuần 1 đến
     *                         40+)
     * @return Chỉ số Plan tương ứng từ 1 đến 8
     */
    public static int planForSourceWeek(int sourceWeekNumber) {
        if (sourceWeekNumber < 1) {
            throw new IllegalArgumentException("sourceWeekNumber must be positive");
        }
        if (sourceWeekNumber <= 20)
            return 1;
        if (sourceWeekNumber <= 25)
            return 2;
        if (sourceWeekNumber <= 29)
            return 3;
        if (sourceWeekNumber <= 33)
            return 4;
        if (sourceWeekNumber <= 35)
            return 5;
        if (sourceWeekNumber <= 37)
            return 6;
        if (sourceWeekNumber <= 39)
            return 7;
        return 8;
    }

    // [Private Helper] Tính toán lại tuần thai hiện hành từ thông tin có sẵn trong
    // MotherJourney
    private GestationalDatingResolution currentResolution(
            MotherJourney current,
            LocalDate serverToday) {
        if (!hasResolvedAuthority(current)) {
            return GestationalDatingResolution.unresolved(
                    current.getLastMenstrualDate(), current.getEstimatedDueDate(), false);
        }
        GestationalDatingBasis basis = current.getGestationalDatingBasis();
        LocalDate canonical = canonicalLmp(
                basis, current.getLastMenstrualDate(), current.getEstimatedDueDate());
        int completed = completedGestationalWeek(canonical, serverToday);
        int completedDays = completedGestationalDays(canonical, serverToday);
        int sourceWeek = sourceWeekNumber(completed);
        return GestationalDatingResolution.noOp(
                basis,
                current.getLastMenstrualDate(),
                current.getEstimatedDueDate(),
                canonical,
                completed,
                completedDays,
                sourceWeek,
                planForSourceWeek(sourceWeek),
                false);
    }

    // [Private Helper] Kiểm tra quy tắc định dạng ngày và điều hướng tính toán tuần
    // thai
    private GestationalDatingResolution resolveShape(
            LocalDate lmp,
            LocalDate edd,
            GestationalDatingBasis requestedBasis,
            int contractVersion,
            GestationalDatingBasis existingBasis,
            LocalDate existingCanonicalLmp,
            LocalDate serverToday,
            boolean datingScope,
            boolean enteringPregnancy,
            boolean existingResolved) {
        validateContractVersion(contractVersion);
        // Quy tắc V2: Bắt buộc chỉ chọn 1 trong 2 cơ sở (LMP hoặc EDD) độc quyền (XOR
        // Rule)
        if (contractVersion == V2) {
            if (requestedBasis == null
                    || (requestedBasis != GestationalDatingBasis.LMP
                            && requestedBasis != GestationalDatingBasis.EDD)
                    || (requestedBasis == GestationalDatingBasis.LMP
                            && lmp == null)
                    || (requestedBasis == GestationalDatingBasis.EDD
                            && edd == null)
                    || (requestedBasis == GestationalDatingBasis.LMP && edd != null)
                    || (requestedBasis == GestationalDatingBasis.EDD && lmp != null)) {
                throw basisRequired();
            }
        }

        if (contractVersion == V1 && existingResolved) {
            return resolveResolvedV1Correction(
                    lmp, edd, requestedBasis, existingBasis, existingCanonicalLmp,
                    serverToday);
        }

        // Cả 2 đều null -> Chưa xác định được tuổi thai
        if (lmp == null && edd == null) {
            if (contractVersion == V2) {
                throw basisRequired();
            }
            return GestationalDatingResolution.unresolved(null, null, datingScope);
        }

        // Trường hợp cung cấp cả 2 ngày: Kiểm tra xem có khớp đúng 280 ngày theo công
        // thức Naegele không
        if (lmp != null && edd != null) {
            if (edd.equals(lmp.plusDays(GESTATION_DAYS))) {
                return resolved(
                        GestationalDatingBasis.LMP,
                        lmp,
                        edd,
                        serverToday,
                        datingScope);
            }
            if (contractVersion == V2) {
                throw basisRequired();
            }
            // V1 giữ cặp ngày không khớp dưới dạng unresolved
            return GestationalDatingResolution.unresolved(lmp, edd, datingScope);
        }

        // Chỉ có LMP: Tính EDD = LMP + 280 ngày
        if (lmp != null) {
            if (contractVersion == V2 && requestedBasis != GestationalDatingBasis.LMP) {
                throw basisRequired();
            }
            return resolved(
                    GestationalDatingBasis.LMP,
                    lmp,
                    lmp.plusDays(GESTATION_DAYS),
                    serverToday,
                    datingScope);
        }

        // Chỉ có EDD: Cơ sở là EDD
        if (contractVersion == V2 && requestedBasis != GestationalDatingBasis.EDD) {
            throw basisRequired();
        }
        return resolved(
                GestationalDatingBasis.EDD,
                null,
                edd,
                serverToday,
                datingScope);
    }

    // [Private Helper] Xử lý cập nhật / hiệu chỉnh cho dữ liệu phiên bản V1 đã xác
    // định
    private GestationalDatingResolution resolveResolvedV1Correction(
            LocalDate lmp,
            LocalDate edd,
            GestationalDatingBasis requestedBasis,
            GestationalDatingBasis existingBasis,
            LocalDate existingCanonicalLmp,
            LocalDate serverToday) {
        if (requestedBasis != null && requestedBasis != existingBasis) {
            throw v2Required();
        }
        boolean authoritativeDatePresent = existingBasis == GestationalDatingBasis.LMP
                ? lmp != null
                : edd != null;
        if (!authoritativeDatePresent) {
            throw v2Required();
        }
        if (lmp != null && edd != null && !edd.equals(lmp.plusDays(GESTATION_DAYS))) {
            throw v2Required();
        }

        LocalDate candidateCanonical = existingBasis == GestationalDatingBasis.LMP
                ? lmp
                : edd.minusDays(GESTATION_DAYS);
        boolean unchanged = Objects.equals(existingCanonicalLmp, candidateCanonical);
        LocalDate sourceLmp = existingBasis == GestationalDatingBasis.LMP
                ? candidateCanonical
                : null;
        LocalDate sourceEdd = existingBasis == GestationalDatingBasis.LMP
                ? candidateCanonical.plusDays(GESTATION_DAYS)
                : edd;
        GestationalDatingResolution resolved = resolved(
                existingBasis,
                sourceLmp,
                sourceEdd,
                serverToday,
                true);
        if (unchanged) {
            return GestationalDatingResolution.noOp(
                    resolved.basis(),
                    resolved.lastMenstrualDate(),
                    resolved.estimatedDueDate(),
                    resolved.canonicalLmp(),
                    resolved.completedGestationalWeek(),
                    resolved.completedGestationalDays(),
                    resolved.sourceWeekNumber(),
                    resolved.plan());
        }
        return resolved;
    }

    // [Private Helper] Đóng gói kết quả tính toán tuổi thai vào record
    // GestationalDatingResolution
    private GestationalDatingResolution resolved(
            GestationalDatingBasis basis,
            LocalDate lmp,
            LocalDate edd,
            LocalDate serverToday,
            boolean datingScope) {
        LocalDate canonical = canonicalLmp(basis, lmp, edd);
        if (canonical == null) {
            throw basisRequired();
        }
        int completed = completedGestationalWeek(canonical, serverToday);
        int completedDays = completedGestationalDays(canonical, serverToday);
        int sourceWeek = sourceWeekNumber(completed);
        return new GestationalDatingResolution(
                basis,
                lmp,
                edd,
                canonical,
                true,
                false,
                datingScope,
                completed,
                completedDays,
                sourceWeek,
                planForSourceWeek(sourceWeek));
    }

    // [Private Helper] Kiểm tra xem request có chứa bất kỳ thông tin ngày thai kỳ
    // nào không
    private boolean hasDating(
            LocalDate lmp,
            LocalDate edd,
            GestationalDatingBasis basis) {
        return lmp != null || edd != null || basis != null;
    }

    // [Private Helper] Kiểm tra phiên bản hợp đồng checklist có hợp lệ không (V1
    // hoặc V2)

    private void validateContractVersion(int version) {
        if (version != V1 && version != V2) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "CHECKLIST_CONTRACT_VERSION_UNSUPPORTED",
                    "Unsupported checklist contract version");
        }
    }

    private static BusinessException basisRequired() {
        return new BusinessException(
                HttpStatus.BAD_REQUEST,
                "GESTATIONAL_DATING_BASIS_REQUIRED",
                "Pregnancy dating requires exactly one matching LMP or EDD basis");
    }

    private static BusinessException v2Required() {
        return new BusinessException(
                HttpStatus.CONFLICT,
                "GESTATIONAL_DATING_V2_REQUIRED",
                "Changing resolved pregnancy dating requires contract version 2");
    }

    private static BusinessException stageInapplicable() {
        return new BusinessException(
                HttpStatus.BAD_REQUEST,
                "GESTATIONAL_DATING_STAGE_INAPPLICABLE",
                "Gestational dating is only applicable while the Journey is pregnant");
    }

    private static BusinessException futureLmp() {
        return new BusinessException(
                HttpStatus.BAD_REQUEST,
                "GESTATIONAL_DATING_DATE_IN_FUTURE",
                "Canonical LMP cannot be in the future");
    }
}
