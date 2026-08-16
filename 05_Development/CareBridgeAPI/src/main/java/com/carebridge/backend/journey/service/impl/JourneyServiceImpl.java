package com.carebridge.backend.journey.service.impl;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.family.entity.InviteStatus;
import com.carebridge.backend.family.repository.CareGroupMemberRepository;
import com.carebridge.backend.family.repository.CareGroupRepository;
import com.carebridge.backend.journey.dto.CreateJourneyRequest;
import com.carebridge.backend.journey.dto.CreateJourneyResponse;
import com.carebridge.backend.journey.dto.JourneyDashboardResponse;
import com.carebridge.backend.journey.dto.JourneyResponse;
import com.carebridge.backend.journey.dto.JourneyTransitionPageResponse;
import com.carebridge.backend.journey.dto.UpdateJourneyRequest;
import com.carebridge.backend.journey.dto.JourneyTransitionResponse;
import com.carebridge.backend.journey.entity.DashboardStatus;
import com.carebridge.backend.journey.entity.JourneyStatus;
import com.carebridge.backend.journey.entity.JourneyType;
import com.carebridge.backend.journey.entity.MotherJourney;
import com.carebridge.backend.journey.entity.PregnancyOutcomeType;
import com.carebridge.backend.journey.entity.GestationalDatingBasis;
import com.carebridge.backend.journey.policy.JourneyTransitionPolicy;
import com.carebridge.backend.journey.repository.MotherJourneyRepository;
import com.carebridge.backend.journey.repository.PregnancyOutcomeEvidenceRepository;
import com.carebridge.backend.journey.service.IJourneyService;
import com.carebridge.backend.journey.service.IJourneyTransitionService;
import com.carebridge.backend.journey.service.GestationalDatingResolution;
import com.carebridge.backend.journey.service.GestationalDatingResolver;
import com.carebridge.backend.security.rbac.Role;
import com.carebridge.backend.security.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.UUID;

@Service
@Transactional
public class JourneyServiceImpl implements IJourneyService {

        private final MotherJourneyRepository journeyRepository;
        private final UserRepository userRepository;
        private final AuditService auditService;
        private final CareGroupMemberRepository careGroupMemberRepository;
        private final CareGroupRepository careGroupRepository;
        private final Clock clock;
        private final IJourneyTransitionService transitionService;
        private final PregnancyOutcomeEvidenceRepository outcomeEvidenceRepository;
        private final GestationalDatingResolver datingResolver;

        private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

        @Autowired
        public JourneyServiceImpl(
                        MotherJourneyRepository journeyRepository,
                        UserRepository userRepository,
                        AuditService auditService,
                        CareGroupMemberRepository careGroupMemberRepository,
                        CareGroupRepository careGroupRepository,
                        IJourneyTransitionService transitionService,
                        PregnancyOutcomeEvidenceRepository outcomeEvidenceRepository,
                        GestationalDatingResolver datingResolver) {
                this(
                                journeyRepository,
                                userRepository,
                                auditService,
                                careGroupMemberRepository,
                                careGroupRepository,
                                Clock.systemDefaultZone(),
                                requireTransitionService(transitionService),
                                outcomeEvidenceRepository,
                                datingResolver);
        }

        /**
         * @deprecated Test-only compatibility constructor. Production wiring must
         *             use the canonical transition service constructor above.
         */
        @Deprecated(forRemoval = true)
        public JourneyServiceImpl(
                        MotherJourneyRepository journeyRepository,
                        UserRepository userRepository,
                        AuditService auditService,
                        CareGroupMemberRepository careGroupMemberRepository,
                        CareGroupRepository careGroupRepository) {
                this(
                                journeyRepository,
                                userRepository,
                                auditService,
                                careGroupMemberRepository,
                                careGroupRepository,
                                Clock.systemDefaultZone(),
                                null,
                                null,
                                new GestationalDatingResolver());
        }

        /**
         * @deprecated Test-only compatibility constructor. Production wiring must
         *             use the canonical transition service constructor above.
         */
        @Deprecated(forRemoval = true)
        public JourneyServiceImpl(
                        MotherJourneyRepository journeyRepository,
                        UserRepository userRepository,
                        AuditService auditService,
                        CareGroupMemberRepository careGroupMemberRepository,
                        CareGroupRepository careGroupRepository,
                        Clock clock) {
                this(
                                journeyRepository,
                                userRepository,
                                auditService,
                                careGroupMemberRepository,
                                careGroupRepository,
                                clock,
                                null,
                                null,
                                new GestationalDatingResolver());
        }

        /**
         * @deprecated Test-only compatibility constructor. Production wiring must
         *             use the canonical transition service constructor above.
         */
        @Deprecated(forRemoval = true)
        public JourneyServiceImpl(
                        MotherJourneyRepository journeyRepository,
                        UserRepository userRepository,
                        AuditService auditService) {
                this(journeyRepository, userRepository, auditService, null, null, Clock.systemDefaultZone(), null, null,
                                new GestationalDatingResolver());
        }

        /**
         * @deprecated Test-only compatibility constructor. Production wiring must
         *             use the canonical transition service constructor above.
         */
        @Deprecated(forRemoval = true)
        public JourneyServiceImpl(
                        MotherJourneyRepository journeyRepository,
                        UserRepository userRepository,
                        AuditService auditService,
                        Clock clock) {
                this(journeyRepository, userRepository, auditService, null, null, clock, null, null,
                                new GestationalDatingResolver());
        }

        public JourneyServiceImpl(
                        MotherJourneyRepository journeyRepository,
                        UserRepository userRepository,
                        AuditService auditService,
                        IJourneyTransitionService transitionService) {
                this(journeyRepository, userRepository, auditService, null, null, Clock.systemDefaultZone(),
                                requireTransitionService(transitionService), null,
                                new GestationalDatingResolver());
        }

        /**
         * @deprecated Test-only compatibility constructor. Production wiring must
         *             use the canonical transition service constructor above.
         */
        @Deprecated(forRemoval = true)
        public JourneyServiceImpl(
                        MotherJourneyRepository journeyRepository,
                        UserRepository userRepository,
                        AuditService auditService,
                        Clock clock,
                        PregnancyOutcomeEvidenceRepository outcomeEvidenceRepository) {
                this(journeyRepository, userRepository, auditService, null, null, clock, null,
                                outcomeEvidenceRepository,
                                new GestationalDatingResolver());
        }

        private JourneyServiceImpl(
                        MotherJourneyRepository journeyRepository,
                        UserRepository userRepository,
                        AuditService auditService,
                        CareGroupMemberRepository careGroupMemberRepository,
                        CareGroupRepository careGroupRepository,
                        Clock clock,
                        IJourneyTransitionService transitionService,
                        PregnancyOutcomeEvidenceRepository outcomeEvidenceRepository,
                        GestationalDatingResolver datingResolver) {
                this.journeyRepository = journeyRepository;
                this.userRepository = userRepository;
                this.auditService = auditService;
                this.careGroupMemberRepository = careGroupMemberRepository;
                this.careGroupRepository = careGroupRepository;
                this.clock = clock;
                this.transitionService = transitionService;
                this.outcomeEvidenceRepository = outcomeEvidenceRepository;
                this.datingResolver = datingResolver;
        }

        private static IJourneyTransitionService requireTransitionService(
                        IJourneyTransitionService transitionService) {
                return Objects.requireNonNull(
                                transitionService,
                                "Canonical journey transition service is required for production wiring");
        }

        // ─────────────────────────────────────────────────────────────
        // UC22 — Create Mother Journey (Khởi tạo hành trình thai kỳ)
        // ─────────────────────────────────────────────────────────────

        /**
         * [UC22: Tạo mới Hành trình Mẹ bầu & Khởi tạo tính toán tuổi thai]
         * 
         * @param request  Dữ liệu khởi tạo (LMP, EDD, loại hành trình, ghi chú)
         * @param callerId ID người dùng tạo hành trình (vai trò MOTHER)
         * @return CreateJourneyResponse chứa thông tin hành trình và các chỉ số tuổi
         *         thai
         */
        @Override
        public CreateJourneyResponse createJourney(CreateJourneyRequest request, UUID callerId) {
                // [ỦY QUYỀN CANONICAL TRANSITION SERVICE]
                // Trong môi trường Production có cấu hình transitionService, ủy quyền toàn bộ
                // luồng xử lý
                // tuổi thai phiên bản 2 (Contract V2) cho JourneyTransitionServiceImpl.
                if (transitionService != null) {
                        return transitionService.createJourney(request, callerId);
                }

                // [BƯỚC 1 & 2: Validate nghiệp vụ & Tiếp nhận Request - Chế độ
                // Standalone/Fallback]
                // 2.1 Kiểm tra sự tồn tại của người dùng và đảm bảo vai trò MOTHER
                var user = userRepository.findById(callerId)
                                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "JOURNEY-001",
                                                "User not found: " + callerId));
                if (user.getRole() == null) {
                        user.setRole(Role.MOTHER);
                        userRepository.save(user);
                } else if (user.getRole() != Role.MOTHER) {
                        throw new BusinessException(HttpStatus.FORBIDDEN, "JOURNEY-003",
                                        "Only mother accounts can create a mother journey");
                }

                // 2.2 Kiểm tra BR-JOURNEY-002: Mỗi mẹ chỉ được có tối đa 1 hành trình ACTIVE
                // cùng loại
                boolean exists = journeyRepository.existsByOwnerUserIdAndJourneyTypeAndStatus(
                                callerId, request.getJourneyType(), JourneyStatus.ACTIVE);
                if (exists) {
                        throw new BusinessException(HttpStatus.CONFLICT, "JOURNEY-002",
                                        "An active journey of type " + request.getJourneyType() + " already exists");
                }

                // [BƯỚC 3: Xử lý nghiệp vụ & Tính toán tuổi thai sơ bộ]
                // Áp dụng quy tắc Naegele: Nếu có ngày kinh cuối (LMP) mà chưa có ngày dự sinh
                // (EDD) -> EDD = LMP + 280 ngày
                LocalDate lastMenstrualDate = request.getLastMenstrualDate();
                LocalDate estimatedDueDate = request.getEstimatedDueDate();
                if (lastMenstrualDate != null && estimatedDueDate == null) {
                        estimatedDueDate = lastMenstrualDate.plusDays(280);
                }

                // [BƯỚC 4: Lưu trữ dữ liệu Database - Bảng mother_journeys]
                UUID careSubjectId = ensureMotherCareSubject(callerId);
                MotherJourney journey = MotherJourney.builder()
                                .ownerUserId(callerId)
                                .careSubjectId(careSubjectId)
                                .journeyType(request.getJourneyType())
                                .startDate(request.getStartDate())
                                .lastMenstrualDate(lastMenstrualDate)
                                .estimatedDueDate(estimatedDueDate)
                                .notes(request.getNotes())
                                .status(JourneyStatus.ACTIVE)
                                .build();

                MotherJourney saved = journeyRepository.saveAndFlush(journey);
                journeyRepository.linkMotherCareSubject(careSubjectId, saved.getId());

                // [BƯỚC 5: Ghi Audit Log, Tính toán tuổi thai chi tiết & Trả về Response]
                auditService.log(AuditAction.JOURNEY_CREATED, callerId,
                                "MotherJourney", saved.getId().toString(), "created");

                // Gọi engine GestationalDatingResolver để tính tuần thai hiện tại, điểm neo
                // canonicalLmp và kế hoạch WHO Plan
                GestationalDatingResolution dating = datingResolver == null
                                ? GestationalDatingResolution.unresolved(
                                                saved.getLastMenstrualDate(), saved.getEstimatedDueDate(), false)
                                : new GestationalDatingResolver().resolveUpdate(
                                                saved,
                                                new UpdateJourneyRequest(),
                                                GestationalDatingResolver.V1,
                                                LocalDate.now(clock.withZone(BUSINESS_ZONE)),
                                                false);
                return CreateJourneyResponse.builder()
                                .id(saved.getId())
                                .journeyType(saved.getJourneyType().name())
                                .status(saved.getStatus().name())
                                .startDate(saved.getStartDate())
                                .lastMenstrualDate(saved.getLastMenstrualDate())
                                .estimatedDueDate(saved.getEstimatedDueDate())
                                .notes(saved.getNotes())
                                .gestationalDatingBasis(saved.getGestationalDatingBasis())
                                .gestationalDatingRevision(saved.getGestationalDatingRevision())
                                .gestationalDatingEffectiveAt(saved.getGestationalDatingEffectiveAt())
                                .gestationalDatingQuarantineReasonCode(
                                                saved.getGestationalDatingQuarantineReasonCode())
                                .canonicalLmp(dating.canonicalLmp())
                                .completedGestationalWeek(dating.resolved()
                                                ? dating.completedGestationalWeek()
                                                : null)
                                .sourceWeekNumber(dating.resolved() ? dating.sourceWeekNumber() : null)
                                .plan(dating.plan())
                                .createdAt(saved.getCreatedAt())
                                .build();
        }

        private UUID ensureMotherCareSubject(UUID ownerUserId) {
                UUID existing = journeyRepository.findMotherCareSubjectId(ownerUserId);
                if (existing != null) {
                        return existing;
                }
                UUID candidate = UUID.randomUUID();
                journeyRepository.ensureMotherCareSubject(candidate, ownerUserId);
                existing = journeyRepository.findMotherCareSubjectId(ownerUserId);
                return existing == null ? candidate : existing;
        }

        // ─────────────────────────────────────────────────────────────
        // UC23 — Update Mother Journey (Cập nhật hành trình thai kỳ)
        // ─────────────────────────────────────────────────────────────

        /**
         * [UC23: Cập nhật Hành trình Mẹ bầu & Hiệu chỉnh ngày thai kỳ]
         * 
         * @param ownerId   ID người dùng sở hữu hành trình (MOTHER)
         * @param journeyId ID hành trình cần chỉnh sửa
         * @param request   Dữ liệu cập nhật mới (LMP, EDD, loại hành trình, trạng thái,
         *                  ghi chú)
         * @return JourneyResponse chứa thông tin hành trình và tuần thai cập nhật
         */
        @Override
        public JourneyResponse updateJourney(UUID ownerId, UUID journeyId, UpdateJourneyRequest request) {
                // [ỦY QUYỀN CANONICAL TRANSITION SERVICE]
                if (transitionService != null) {
                        return transitionService.updateJourney(ownerId, journeyId, request);
                }

                // [BƯỚC 1 & 2: Validate nghiệp vụ & Tiếp nhận Request]
                // 2.1 Tìm kiếm hành trình trong DB
                MotherJourney journey = journeyRepository.findById(journeyId)
                                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "JOURNEY-010",
                                                "Journey not found: " + journeyId));

                // 2.2 Kiểm tra quyền sở hữu để phòng chống lỗ hổng IDOR
                if (!journey.getOwnerUserId().equals(ownerId)) {
                        throw new BusinessException(HttpStatus.FORBIDDEN, "JOURNEY-011",
                                        "Access denied: caller does not own this journey");
                }

                // 2.3 Chỉ cho phép cập nhật khi hành trình đang ở trạng thái ACTIVE
                if (journey.getStatus() != JourneyStatus.ACTIVE) {
                        throw new BusinessException(HttpStatus.BAD_REQUEST, "JOURNEY-012",
                                        "Only ACTIVE journeys can be updated (current status: " + journey.getStatus()
                                                        + ")");
                }

                // 2.4 Trạng thái ARCHIVED chỉ được gán bởi hệ thống
                if ("ARCHIVED".equalsIgnoreCase(request.getStatus())) {
                        throw new BusinessException(HttpStatus.BAD_REQUEST, "JOURNEY-014",
                                        "Status ARCHIVED can only be set by the system");
                }

                // 2.5 Nếu hoàn tất thai kỳ (LIVE_BIRTH), bắt buộc phải có ngày sinh
                // deliveryDate
                if ("COMPLETED".equalsIgnoreCase(request.getStatus())
                                && request.getDeliveryDate() == null
                                && journey.getDeliveryDate() == null
                                && (journey.getPregnancyOutcome() == null
                                                || journey.getPregnancyOutcome() == com.carebridge.backend.journey.entity.PregnancyOutcomeType.LIVE_BIRTH)) {
                        throw new BusinessException(HttpStatus.BAD_REQUEST, "JOURNEY-013",
                                        "deliveryDate is required when completing a journey");
                }

                // [BƯỚC 3: Xử lý nghiệp vụ & Hiệu chỉnh ngày thai kỳ]
                MotherJourney.MotherJourneyBuilder builder = journey.toBuilder();
                if (request.getJourneyType() != null) {
                        builder.journeyType(request.getJourneyType());
                }
                if (request.getNotes() != null) {
                        builder.notes(request.getNotes());
                }
                // Cập nhật LMP và tự động tính lại EDD = LMP + 280 ngày
                if (request.getLastMenstrualDate() != null) {
                        builder.lastMenstrualDate(request.getLastMenstrualDate());
                        builder.estimatedDueDate(request.getLastMenstrualDate().plusDays(280));
                } else if (request.getEstimatedDueDate() != null) {
                        builder.estimatedDueDate(request.getEstimatedDueDate());
                        builder.lastMenstrualDate(null);
                }
                if (request.getDeliveryDate() != null) {
                        builder.deliveryDate(request.getDeliveryDate());
                }
                if ("COMPLETED".equalsIgnoreCase(request.getStatus())) {
                        builder.status(JourneyStatus.COMPLETED);
                }

                // [BƯỚC 4: Lưu trữ thay đổi vào Database]
                MotherJourney saved = journeyRepository.save(builder.build());

                // [BƯỚC 5: Ghi Audit Log & Trả về Response]
                auditService.log(AuditAction.JOURNEY_UPDATED, ownerId,
                                "MotherJourney", saved.getId().toString(), "updated");

                return toJourneyResponse(saved);
        }

        @Override
        @Transactional(readOnly = true)
        public JourneyTransitionPageResponse getHistory(
                        UUID ownerId, UUID journeyId, Pageable pageable) {
                if (transitionService == null) {
                        throw new IllegalStateException("Journey transition service is unavailable");
                }
                return transitionService.getHistory(ownerId, journeyId, pageable);
        }

        // ─────────────────────────────────────────────────────────────
        // UC24 — View Mother Journey Dashboard
        // ─────────────────────────────────────────────────────────────

        @Override
        @Transactional(readOnly = true)
        public JourneyDashboardResponse getDashboard(UUID userId) {
                // [BƯỚC 1 & 2: Tiếp nhận Request & Tìm kiếm hành trình đang kích hoạt]
                // Query hành trình ACTIVE của người dùng theo các giai đoạn chuẩn (PREGNANCY,
                // POSTPARTUM, BABY_CARE, PRE_PREGNANCY)
                var activeJourney = journeyRepository.findByOwnerUserIdAndStatusAndJourneyTypeIn(
                                userId, JourneyStatus.ACTIVE, JourneyTransitionPolicy.CANONICAL_STAGES);

                // Fallback: Nếu không tìm thấy, thử tìm hành trình BABY_CARE đang ACTIVE gần
                // nhất
                if (activeJourney.isEmpty()) {
                        activeJourney = journeyRepository
                                        .findFirstByOwnerUserIdAndJourneyTypeAndStatusOrderByCreatedAtDesc(
                                                        userId, JourneyType.BABY_CARE, JourneyStatus.ACTIVE);
                }

                // Hỗ trợ thành viên nhóm gia đình (CareGroup): Nếu người gọi là Family Member,
                // lấy hành trình của Mẹ sở hữu nhóm
                if (activeJourney.isEmpty() && careGroupMemberRepository != null && careGroupRepository != null) {
                        var memberships = careGroupMemberRepository.findByUserIdAndInviteStatus(userId,
                                        InviteStatus.ACCEPTED);
                        for (var member : memberships) {
                                var groupOpt = careGroupRepository.findById(member.getCareGroupId());
                                if (groupOpt.isPresent()) {
                                        var motherId = groupOpt.get().getOwnerUserId();
                                        activeJourney = journeyRepository
                                                        .findFirstByOwnerUserIdAndStatusOrderByCreatedAtDesc(
                                                                        motherId, JourneyStatus.ACTIVE);
                                        if (activeJourney.isPresent()) {
                                                break;
                                        }
                                }
                        }
                }

                // Nếu người dùng chưa tạo hành trình nào -> Trả về DashboardStatus.NO_JOURNEY
                // (Quy tắc Mobile Onboarding: không ném 404)
                if (activeJourney.isEmpty()) {
                        return JourneyDashboardResponse.builder()
                                        .status(DashboardStatus.NO_JOURNEY)
                                        .build();
                }

                MotherJourney journey = activeJourney.get();
                // Lấy ngày hiện tại của máy chủ theo múi giờ kinh doanh Việt Nam
                // (Asia/Ho_Chi_Minh)
                LocalDate today = LocalDate.now(clock.withZone(BUSINESS_ZONE));

                DashboardStatus dashboardStatus = resolveDashboardStatus(journey.getJourneyType());

                // Khởi tạo các biến chứa kết quả tính toán tuần thai
                Integer pregnancyWeek = null;
                Integer trimester = null;
                Long daysUntilDue = null;
                GestationalDatingBasis datingBasis = null;
                Long datingRevision = null;
                java.time.Instant datingEffectiveAt = null;
                LocalDate canonicalLmp = null;
                Integer completedGestationalWeek = null;
                Integer completedGestationalDays = null;
                Integer sourceWeekNumber = null;
                Integer plan = null;

                // [BƯỚC 3: Xử lý nghiệp vụ & Thuật toán tính tuần thai cốt lõi]
                // Kiểm tra xem hành trình có đủ điều kiện xác thực thời gian thai kỳ không
                // (ACTIVE, PREGNANCY, có cơ sở hợp lệ)
                if (GestationalDatingResolver.hasResolvedAuthority(journey)) {
                        // [BƯỚC 3.1: Xác định điểm neo kinh cuối chuẩn hóa (Canonical LMP)]
                        canonicalLmp = GestationalDatingResolver.canonicalLmp(
                                        journey.getGestationalDatingBasis(),
                                        journey.getLastMenstrualDate(),
                                        journey.getEstimatedDueDate());

                        // Đảm bảo ngày kinh cuối không nằm ở tương lai
                        if (canonicalLmp != null && !canonicalLmp.isAfter(today)) {
                                datingBasis = journey.getGestationalDatingBasis();
                                datingRevision = journey.getGestationalDatingRevision();
                                datingEffectiveAt = journey.getGestationalDatingEffectiveAt();

                                // [BƯỚC 3.2: Tính số tuần thai đã hoàn thành (0-based) và số ngày lẻ (0..6)]
                                completedGestationalWeek = GestationalDatingResolver.completedGestationalWeek(
                                                canonicalLmp, today);
                                completedGestationalDays = GestationalDatingResolver.completedGestationalDays(
                                                canonicalLmp, today);

                                // [BƯỚC 3.3: Tính tuần thai hiển thị người dùng (1-based: sourceWeekNumber)]
                                sourceWeekNumber = GestationalDatingResolver.sourceWeekNumber(completedGestationalWeek);

                                // [BƯỚC 3.4: Xác định kế hoạch khám thai WHO Plan (1..8)]
                                plan = GestationalDatingResolver.planForSourceWeek(sourceWeekNumber);

                                pregnancyWeek = sourceWeekNumber;

                                // [BƯỚC 3.5: Phân loại 3 tam cá nguyệt (Trimester 1, 2, 3)]
                                trimester = calculateTrimester(pregnancyWeek);

                                // [BƯỚC 3.6: Tính số ngày đếm ngược đến ngày dự sinh (Days Until Due)]
                                daysUntilDue = ChronoUnit.DAYS.between(
                                                today, canonicalLmp.plusDays(GestationalDatingResolver.GESTATION_DAYS));
                        }
                }

                // [BƯỚC 4 & 5: Đóng gói phản hồi Dashboard (Read-only không đổi DB)]
                return JourneyDashboardResponse.builder()
                                .journeyId(journey.getId())
                                .journeyType(journey.getJourneyType().name())
                                .status(dashboardStatus)
                                .pregnancyWeek(pregnancyWeek)
                                .trimester(trimester)
                                .daysUntilDue(daysUntilDue)
                                .estimatedDueDate(datingBasis == null ? null : journey.getEstimatedDueDate())
                                .lastMenstrualDate(datingBasis == null ? null : journey.getLastMenstrualDate())
                                .startDate(journey.getStartDate())
                                .version(journey.getVersion())
                                .dateSource(journey.getDateSource())
                                .dateConfidence(journey.getDateConfidence())
                                .gestationalDatingBasis(datingBasis)
                                .gestationalDatingRevision(datingRevision)
                                .gestationalDatingEffectiveAt(datingEffectiveAt)
                                .gestationalDatingQuarantineReasonCode(
                                                journey.getGestationalDatingQuarantineReasonCode())
                                .canonicalLmp(canonicalLmp)
                                .completedGestationalWeek(completedGestationalWeek)
                                .completedGestationalDays(completedGestationalDays)
                                .sourceWeekNumber(sourceWeekNumber)
                                .plan(plan)
                                .pregnancyOutcome(journey.getPregnancyOutcome())
                                .pregnancyOutcomeDate(journey.getPregnancyOutcomeDate())
                                .build();
        }

        // ─────────────────────────────────────────────────────────────
        // Private helpers
        // ─────────────────────────────────────────────────────────────

        private DashboardStatus resolveDashboardStatus(JourneyType type) {
                return switch (type) {
                        case PREGNANCY -> DashboardStatus.ACTIVE_PREGNANCY;
                        case POSTPARTUM -> DashboardStatus.ACTIVE_POSTPARTUM;
                        case BABY_CARE -> DashboardStatus.BABY_CARE;
                        case PRE_PREGNANCY -> DashboardStatus.PRE_PREGNANCY;
                };
        }

        /**
         * Phân loại 3 tam cá nguyệt (Trimesters) theo tuần thai chuẩn sản khoa:
         * - Tam cá nguyệt 1 (T1): Tuần 1 đến 13 (Giai đoạn hình thành cơ quan thai nhi)
         * - Tam cá nguyệt 2 (T2): Tuần 14 đến 26 (Giai đoạn phát triển thể chất)
         * - Tam cá nguyệt 3 (T3): Tuần 27 trở đi (Giai đoạn hoàn thiện và chuẩn bị
         * sinh)
         * 
         * @param week Tuần thai hiện tại (1-based)
         * @return 1, 2, hoặc 3
         */
        private int calculateTrimester(int week) {
                if (week <= 13)
                        return 1;
                if (week <= 26)
                        return 2;
                return 3;
        }

        private JourneyResponse toJourneyResponse(MotherJourney journey) {
                GestationalDatingResolution dating = datingResolver == null
                                ? GestationalDatingResolution.unresolved(
                                                journey.getLastMenstrualDate(), journey.getEstimatedDueDate(), false)
                                : new GestationalDatingResolver().resolveUpdate(
                                                journey,
                                                new UpdateJourneyRequest(),
                                                GestationalDatingResolver.V1,
                                                LocalDate.now(clock.withZone(BUSINESS_ZONE)),
                                                false);
                return JourneyResponse.builder()
                                .journeyId(journey.getId())
                                .ownerUserId(journey.getOwnerUserId())
                                .journeyType(journey.getJourneyType().name())
                                .startDate(journey.getStartDate())
                                .lastMenstrualDate(journey.getLastMenstrualDate())
                                .estimatedDueDate(journey.getEstimatedDueDate())
                                .deliveryDate(journey.getDeliveryDate())
                                .pregnancyOutcome(journey.getPregnancyOutcome())
                                .pregnancyOutcomeDate(journey.getPregnancyOutcomeDate())
                                .status(journey.getStatus().name())
                                .notes(journey.getNotes())
                                .gestationalDatingBasis(journey.getGestationalDatingBasis())
                                .gestationalDatingRevision(journey.getGestationalDatingRevision())
                                .gestationalDatingEffectiveAt(journey.getGestationalDatingEffectiveAt())
                                .gestationalDatingQuarantineReasonCode(
                                                journey.getGestationalDatingQuarantineReasonCode())
                                .canonicalLmp(dating.canonicalLmp())
                                .completedGestationalWeek(dating.resolved()
                                                ? dating.completedGestationalWeek()
                                                : null)
                                .completedGestationalDays(dating.resolved()
                                                ? dating.completedGestationalDays()
                                                : null)
                                .sourceWeekNumber(dating.resolved() ? dating.sourceWeekNumber() : null)
                                .plan(dating.plan())
                                .createdAt(journey.getCreatedAt())
                                .updatedAt(journey.getUpdatedAt())
                                .build();
        }
}
