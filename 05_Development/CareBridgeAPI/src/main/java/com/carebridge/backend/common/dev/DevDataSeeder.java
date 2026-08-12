package com.carebridge.backend.common.dev;

import com.carebridge.backend.baby.entity.BabyProfile;
import com.carebridge.backend.baby.entity.BabyProfileStatus;
import com.carebridge.backend.baby.entity.Gender;
import com.carebridge.backend.baby.repository.BabyProfileRepository;
import com.carebridge.backend.community.entity.AnswerStatus;
import com.carebridge.backend.community.entity.CommunityAnswer;
import com.carebridge.backend.community.entity.CommunityAnswerLike;
import com.carebridge.backend.community.entity.CommunityBookmark;
import com.carebridge.backend.community.entity.CommunityQuestion;
import com.carebridge.backend.community.entity.CommunityQuestionLike;
import com.carebridge.backend.community.entity.CommunityTopic;
import com.carebridge.backend.community.entity.PregnancyStage;
import com.carebridge.backend.community.entity.QuestionStatus;
import com.carebridge.backend.community.entity.UrgencyLevel;
import com.carebridge.backend.community.entity.UserTopicFollow;
import com.carebridge.backend.community.repository.CommunityAnswerLikeRepository;
import com.carebridge.backend.community.repository.CommunityAnswerRepository;
import com.carebridge.backend.community.repository.CommunityBookmarkRepository;
import com.carebridge.backend.community.repository.CommunityQuestionLikeRepository;
import com.carebridge.backend.community.repository.CommunityQuestionRepository;
import com.carebridge.backend.community.repository.CommunityTopicRepository;
import com.carebridge.backend.community.repository.UserTopicFollowRepository;
import com.carebridge.backend.content.entity.ContentReport;
import com.carebridge.backend.content.entity.ContentItem;
import com.carebridge.backend.content.entity.ContentSource;
import com.carebridge.backend.content.entity.ContentStage;
import com.carebridge.backend.content.entity.ContentStatus;
import com.carebridge.backend.content.entity.ContentType;
import com.carebridge.backend.content.entity.ChecklistTemplate;
import com.carebridge.backend.content.entity.ChecklistTemplateStatus;
import com.carebridge.backend.content.entity.ChecklistItem;
import com.carebridge.backend.content.entity.ModerationAction;
import com.carebridge.backend.content.entity.ModerationActionType;
import com.carebridge.backend.content.entity.ReportCategory;
import com.carebridge.backend.content.entity.ReportStatus;
import com.carebridge.backend.content.entity.ReportSource;
import com.carebridge.backend.content.entity.ReportTargetType;
import com.carebridge.backend.checklist.model.ChecklistAnchorType;
import com.carebridge.backend.checklist.model.ChecklistRangeUnit;
import com.carebridge.backend.checklist.model.ChecklistRecipientScope;
import com.carebridge.backend.content.repository.ContentReportRepository;
import com.carebridge.backend.content.repository.ContentRepository;
import com.carebridge.backend.content.repository.ChecklistTemplateRepository;
import com.carebridge.backend.content.repository.ChecklistItemRepository;
import com.carebridge.backend.content.repository.ModerationActionRepository;
import com.carebridge.backend.expert.entity.ExpertProfile;
import com.carebridge.backend.expert.repository.ExpertProfileRepository;
import com.carebridge.backend.expert.verificationstatus.VerificationStatus;
import com.carebridge.backend.expertverification.entity.ExpertCredential;
import com.carebridge.backend.expertverification.repository.ExpertCredentialRepository;
import com.carebridge.backend.expertverification.reviewstatus.ReviewStatus;
import com.carebridge.backend.family.entity.CareGroup;
import com.carebridge.backend.family.entity.CareGroupMember;
import com.carebridge.backend.family.entity.CareGroupStatus;
import com.carebridge.backend.family.entity.GroupMemberRole;
import com.carebridge.backend.family.entity.InviteStatus;
import com.carebridge.backend.family.repository.CareGroupMemberRepository;
import com.carebridge.backend.family.repository.CareGroupRepository;
import com.carebridge.backend.journey.entity.JourneyStatus;
import com.carebridge.backend.journey.entity.JourneyType;
import com.carebridge.backend.journey.entity.MotherJourney;
import com.carebridge.backend.journey.entity.PregnancyOutcomeEvidence;
import com.carebridge.backend.journey.entity.PregnancyOutcomeType;
import com.carebridge.backend.journey.entity.JourneyDateSource;
import com.carebridge.backend.journey.repository.MotherJourneyRepository;
import com.carebridge.backend.journey.repository.PregnancyOutcomeEvidenceRepository;
import com.carebridge.backend.security.entity.User;
import com.carebridge.backend.security.rbac.Role;
import com.carebridge.backend.security.repository.UserRepository;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seeds test accounts per role when explicitly enabled.
 *
 * Credentials (same operator-supplied synthetic password for all accounts):
 *   Password : CAREBRIDGE_DEV_SEED_PASSWORD / carebridge.dev-seed.password
 * The value is required when seeding is enabled, is never logged, and is restricted to
 * the dev profile. The approved local fixture password (Test@1234) is accepted for this
 * synthetic seed only; blank input still fails startup closed.
 * The seeder is available only in the dev profile while prod is absent and still requires
 * the explicit carebridge.dev-seed.enabled property gate.
 *
 * Base accounts (one per role):
 *   admin@carebridge.dev      -> SYSTEM_ADMIN
 *   moderator@carebridge.dev  -> MODERATOR
 *   content@carebridge.dev    -> CONTENT_ADMIN
 *   expert@carebridge.dev     -> EXPERT
 *   mother@carebridge.dev     -> MOTHER
 *   family@carebridge.dev     -> FAMILY
 *
 * Extra fully-verified/accepted accounts (two more each for MOTHER, FAMILY, EXPERT),
 * used for multi-account manual test scenarios (e.g. two mothers each paired with
 * their own accepted family member, plus two admin-approved experts). Note:
 * "mother2@carebridge.dev" is skipped - it already exists as a teammate's manual test
 * account in the shared dev database, so seeding continues from mother3/mother4:
 *   mother3@carebridge.dev / mother4@carebridge.dev   -> MOTHER  (own mother journeys + care subjects)
 *   family2@carebridge.dev / family3@carebridge.dev   -> FAMILY  (ACCEPTED care_group_members of mother3/mother4's group)
 *   expert2@carebridge.dev / expert3@carebridge.dev   -> EXPERT  (professional profile APPROVED + expert credentials APPROVED + availability slot)
 *
 * OTP: dev uses MockEmailService logs; supabase uses configured SMTP.
 */
@Slf4j
@Component
@Profile("dev & !prod")
@ConditionalOnProperty(prefix = "carebridge.dev-seed", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class DevDataSeeder implements ApplicationRunner {

    private static final String DEFAULT_TEST_PASSWORD = "Test@1234";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final MotherJourneyRepository motherJourneyRepository;
    private final PregnancyOutcomeEvidenceRepository pregnancyOutcomeEvidenceRepository;
    private final BabyProfileRepository babyProfileRepository;
    private final CareGroupRepository careGroupRepository;
    private final CareGroupMemberRepository careGroupMemberRepository;
    private final ExpertProfileRepository expertProfileRepository;
    private final ExpertCredentialRepository expertCredentialRepository;
    private final CommunityTopicRepository communityTopicRepository;
    private final CommunityQuestionRepository communityQuestionRepository;
    private final CommunityAnswerRepository communityAnswerRepository;
    private final CommunityQuestionLikeRepository communityQuestionLikeRepository;
    private final CommunityAnswerLikeRepository communityAnswerLikeRepository;
    private final CommunityBookmarkRepository communityBookmarkRepository;
    private final ContentReportRepository contentReportRepository;
    private final ModerationActionRepository moderationActionRepository;
    private final UserTopicFollowRepository userTopicFollowRepository;
    private final ContentRepository contentRepository;
    private final ChecklistTemplateRepository checklistTemplateRepository;
    private final ChecklistItemRepository checklistItemRepository;
    private final JdbcTemplate jdbcTemplate;

    @Value("${carebridge.dev-seed.password:" + DEFAULT_TEST_PASSWORD + "}")
    private String testPassword;

    @Value("${carebridge.dev-seed.extended-content-enabled:true}")
    private boolean extendedContentEnabled;

    private UUID seedApprovalUserId;

    record SeedAccount(String email, String fullName, Role role) {}

    private static final List<SeedAccount> SEED_ACCOUNTS = List.of(
        new SeedAccount("admin@carebridge.dev", "Admin Test", Role.SYSTEM_ADMIN),
        new SeedAccount("moderator@carebridge.dev", "Moderator Test", Role.MODERATOR),
        new SeedAccount("content@carebridge.dev", "Content Test", Role.CONTENT_ADMIN),
        new SeedAccount("expert@carebridge.dev", "Expert Test", Role.EXPERT),
        new SeedAccount("mother@carebridge.dev", "Mother Test", Role.MOTHER),
        new SeedAccount("family@carebridge.dev", "Family Test", Role.FAMILY),
        new SeedAccount("mebau@carebridge.dev", "Mẹ Bầu Mới", Role.MOTHER),
        new SeedAccount("mother3@carebridge.dev", "Mother Test 3", Role.MOTHER),
        new SeedAccount("mother4@carebridge.dev", "Mother Test 4", Role.MOTHER),
        new SeedAccount("mother5@carebridge.dev", "Live Birth Add Baby Mother", Role.MOTHER),
        new SeedAccount("mother6@carebridge.dev", "Standalone Baby Profiles Mother", Role.MOTHER),
        new SeedAccount("family2@carebridge.dev", "Family Test 2", Role.FAMILY),
        new SeedAccount("family3@carebridge.dev", "Family Test 3", Role.FAMILY),
        new SeedAccount("expert2@carebridge.dev", "Expert Test 2", Role.EXPERT),
        new SeedAccount("expert3@carebridge.dev", "Expert Test 3", Role.EXPERT)
    );

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        validateSeedPassword(testPassword);
        String passwordHash = passwordEncoder.encode(testPassword);

        Map<String, User> savedUsers = new HashMap<>();
        int created = 0;

        for (SeedAccount seed : SEED_ACCOUNTS) {
            var existing = userRepository.findByEmail(seed.email());
            if (existing.isPresent()) {
                User user = existing.get();
                if (!passwordEncoder.matches(testPassword, user.getPasswordHash())) {
                    user.setPasswordHash(passwordHash);
                    user.setEnabled(true);
                    user.setLocked(false);
                    user.setAccountStatus("ACTIVE");
                    userRepository.save(user);
                    created++;
                    log.info("Reset password for existing seed account: {}", seed.email());
                }
                savedUsers.put(seed.email(), user);
                continue;
            }

            User user = User.builder()
                    .email(seed.email())
                    .name(seed.fullName())
                    .passwordHash(passwordHash)
                    .role(seed.role())
                    .enabled(true)
                    .locked(false)
                    .accountStatus("ACTIVE")
                    .emailVerified(true)
                    .phoneVerified(true)
                    .build();

            user = userRepository.save(user);
            savedUsers.put(seed.email(), user);
            created++;
        }

        if (created > 0) {
            log.info("Created/updated {} synthetic dev seed accounts", created);
        } else {
            log.debug("[DevDataSeeder] All seed accounts already exist - skipped.");
        }

        seedVerifiedProfileData(savedUsers);
        if (extendedContentEnabled) {
            seedCommunitySampleData(savedUsers);
            seedCommunitySampleDataBatch2(savedUsers);
            seedVerifiedContent(savedUsers);
        } else {
            log.info("Skipped extended dev community/content fixtures by explicit configuration");
        }
    }

    static void validateSeedPassword(String candidate) {
        if (candidate == null || candidate.isBlank()) {
            throw new IllegalStateException(
                    "Dev seed requires a non-blank CAREBRIDGE_DEV_SEED_PASSWORD");
        }
    }

    /** Small idempotent content library covering public, draft and review lifecycle states. */
    private void seedVerifiedContent(Map<String, User> users) {
        User author = users.get("content@carebridge.dev");
        seedApprovalUserId = users.get("admin@carebridge.dev").getId();
        seedContent(author, "[DEV] Checklist chuẩn bị sinh", ContentType.CHECKLIST, ContentStage.PREGNANCY, ContentStatus.DRAFT);
        seedChecklistTemplate();
        seedContentLibraryBatch(author);
    }

    private void seedContent(User author, String title, ContentType type, ContentStage stage, ContentStatus status) {
        if (contentRepository.findByTitleIgnoreCaseAndStageAndType(title, stage, type).isPresent()) return;
        Instant now = Instant.now();
        contentRepository.save(ContentItem.builder().type(type).title(title)
                .body("Nội dung mẫu dùng để kiểm thử luồng nội dung đã xác thực.")
                .stage(stage).status(status).versionNo(1).authorUserId(author.getId())
                .sourceLabel("WHO").sources(List.of(new ContentSource("WHO maternal health guidance", "https://www.who.int/health-topics/maternal-health", "WHO")))
                .publishedAt(status == ContentStatus.APPROVED ? now : null).build());
    }

    /**
     * Additional content items for CHECKLIST stage coverage.
     * Idempotent via findByTitleIgnoreCaseAndStageAndType.
     */
    private void seedContentLibraryBatch(User author) {
        seedContentItem(author, "Checklist khám sức khỏe tiền sản",
                "Danh mục các xét nghiệm và mũi tiêm cần hoàn thành trước khi mang thai.",
                ContentType.CHECKLIST, ContentStage.PRE_PREGNANCY, ContentStatus.APPROVED,
                "Bộ Y tế", "Checklist tiền sản", "https://moh.gov.vn", "Bộ Y tế");

        seedContentItem(author, "Checklist đồ dùng cho mẹ và bé khi đi sinh",
                "Danh sách vật dụng cần chuẩn bị trước ngày dự sinh cho cả mẹ và bé.",
                ContentType.CHECKLIST, ContentStage.PREGNANCY, ContentStatus.APPROVED,
                "Bộ Y tế", "Checklist đồ đi sinh", "https://moh.gov.vn", "Bộ Y tế");

        seedContentItem(author, "Checklist chăm sóc mẹ sau sinh 6 tuần đầu",
                "Các mốc theo dõi sức khỏe mẹ trong 6 tuần đầu sau sinh.",
                ContentType.CHECKLIST, ContentStage.POSTPARTUM, ContentStatus.APPROVED,
                "Bộ Y tế", "Checklist hậu sản", "https://moh.gov.vn", "Bộ Y tế");

        seedContentItem(author, "Checklist an toàn cho bé tại nhà",
                "Danh mục kiểm tra an toàn không gian sống để phòng ngừa tai nạn cho trẻ nhỏ.",
                ContentType.CHECKLIST, ContentStage.POSTPARTUM, ContentStatus.APPROVED,
                "UNICEF", "Home safety checklist", "https://www.unicef.org/nutrition", "UNICEF");
    }

    private void seedContentItem(User author, String title, String body, ContentType type, ContentStage stage,
            ContentStatus status, String sourceLabel, String sourceTitle, String sourceUrl, String publisher) {
        if (contentRepository.findByTitleIgnoreCaseAndStageAndType(title, stage, type).isPresent()) return;
        Instant now = Instant.now();
        contentRepository.save(ContentItem.builder().type(type).title(title).body(body)
                .stage(stage).status(status).versionNo(1).authorUserId(author.getId())
                .sourceLabel(sourceLabel).sources(List.of(new ContentSource(sourceTitle, sourceUrl, publisher)))
                .publishedAt(status == ContentStatus.APPROVED ? now : null).build());
    }



    private void seedChecklistTemplate() {
        ChecklistTemplate template = checklistTemplateRepository.findAll().stream()
                .filter(t -> "[DEV] Checklist chuẩn bị sinh".equals(t.getName())).findFirst()
                .orElseGet(() -> checklistTemplateRepository.save(buildSeedChecklistTemplate(
                        "[DEV] Checklist chuẩn bị sinh", ContentStage.PREGNANCY,
                        ChecklistTemplateStatus.DRAFT,
                        "Checklist mẫu tương ứng nội dung CHECKLIST đã seed.")));
        if (checklistItemRepository.findByTemplate_IdOrderByOrder(template.getId()).isEmpty()) {
            checklistItemRepository.save(ChecklistItem.builder().template(template).itemText("Chuẩn bị giấy tờ cần thiết")
                    .order(1).isRequired(true).build());
        }
        seedChecklistTemplateBatch();
    }

    private record ChecklistItemSpec(String text, boolean required) {}

    /**
     * 7 additional templates spanning every (stage, status) combination plus a zero-item
     * template, so the Content Admin "Checklist" screen (/content/checklists) has real
     * variety instead of the single PREGNANCY/DRAFT row above. Idempotent by template name.
     */
    private void seedChecklistTemplateBatch() {
        seedChecklistTemplate("Checklist khám sức khỏe tiền sản", ContentStage.PRE_PREGNANCY, ContentStatus.APPROVED,
                "Các xét nghiệm và mũi tiêm cần hoàn thành trước khi mang thai.", List.of(
                        new ChecklistItemSpec("Khám sức khỏe tổng quát", true),
                        new ChecklistItemSpec("Xét nghiệm máu và các bệnh lây truyền", true),
                        new ChecklistItemSpec("Tiêm phòng Rubella, thủy đậu", true),
                        new ChecklistItemSpec("Tư vấn di truyền nếu có tiền sử gia đình", false)));

        seedChecklistTemplate("Checklist đồ dùng cho mẹ và bé khi đi sinh", ContentStage.PREGNANCY, ContentStatus.APPROVED,
                "Danh sách vật dụng cần chuẩn bị trước ngày dự sinh cho cả mẹ và bé.", List.of(
                        new ChecklistItemSpec("Hồ sơ khám thai và giấy tờ tùy thân", true),
                        new ChecklistItemSpec("Quần áo sơ sinh và tã bỉm", true),
                        new ChecklistItemSpec("Đồ dùng vệ sinh cá nhân cho mẹ", true),
                        new ChecklistItemSpec("Nước uống và đồ ăn nhẹ", false),
                        new ChecklistItemSpec("Sạc điện thoại, máy ảnh", false)));

        seedChecklistTemplate("Checklist chuẩn bị tâm lý trước sinh", ContentStage.PREGNANCY, ContentStatus.DRAFT,
                "Bản nháp checklist tâm lý trước sinh, chưa có mục nào được thêm.", List.of());

        seedChecklistTemplate("Checklist chăm sóc mẹ sau sinh 6 tuần đầu", ContentStage.POSTPARTUM, ContentStatus.APPROVED,
                "Các mốc theo dõi sức khỏe mẹ trong 6 tuần đầu sau sinh.", List.of(
                        new ChecklistItemSpec("Theo dõi vết mổ/vết khâu hàng ngày", true),
                        new ChecklistItemSpec("Tái khám sau sinh 6 tuần", true),
                        new ChecklistItemSpec("Theo dõi dấu hiệu trầm cảm sau sinh", true),
                        new ChecklistItemSpec("Duy trì chế độ dinh dưỡng cho con bú", false)));

        seedChecklistTemplate("Checklist theo dõi dấu hiệu trầm cảm sau sinh", ContentStage.POSTPARTUM, ContentStatus.PENDING_REVIEW,
                "Bảng theo dõi cảm xúc và dấu hiệu cảnh báo trầm cảm sau sinh, đang chờ duyệt.", List.of(
                        new ChecklistItemSpec("Ghi nhận chất lượng giấc ngủ", true),
                        new ChecklistItemSpec("Ghi nhận thay đổi cảm xúc bất thường", true),
                        new ChecklistItemSpec("Liên hệ chuyên gia nếu có dấu hiệu cảnh báo", false)));

        seedChecklistTemplate("Checklist an toàn cho bé tại nhà", ContentStage.POSTPARTUM, ContentStatus.APPROVED,
                "Danh mục kiểm tra an toàn không gian sống để phòng ngừa tai nạn cho trẻ nhỏ.", List.of(
                        new ChecklistItemSpec("Che chắn ổ điện và góc cạnh sắc nhọn", true),
                        new ChecklistItemSpec("Khóa an toàn tủ đựng hóa chất, thuốc", true),
                        new ChecklistItemSpec("Lắp thanh chắn cầu thang", true),
                        new ChecklistItemSpec("Kiểm tra nhiệt độ nước tắm", true),
                        new ChecklistItemSpec("Dọn vật nhỏ dễ hóc nghẹn", false),
                        new ChecklistItemSpec("Chuẩn bị số điện thoại khẩn cấp", false)));

        seedChecklistTemplate("Checklist đồ dùng sơ sinh (bản cũ)", ContentStage.POSTPARTUM, ContentStatus.ARCHIVED,
                "Phiên bản checklist cũ, đã được thay thế bởi bản cập nhật mới hơn.", List.of(
                        new ChecklistItemSpec("Bình sữa và dụng cụ tiệt trùng", true),
                        new ChecklistItemSpec("Nôi và chăn ga cho bé", false)));
    }

    private void seedChecklistTemplate(String name, ContentStage stage, ContentStatus status, String description,
            List<ChecklistItemSpec> items) {
        ChecklistTemplate template = checklistTemplateRepository.findAll().stream()
                .filter(t -> name.equals(t.getName())).findFirst()
                .orElseGet(() -> checklistTemplateRepository.saveAndFlush(buildSeedChecklistTemplate(
                        name, stage,
                        // Build the complete version while mutable. The database guard
                        // rejects item inserts once a root is APPROVED/ARCHIVED.
                        status == ContentStatus.APPROVED || status == ContentStatus.ARCHIVED
                                ? ChecklistTemplateStatus.DRAFT
                                : ChecklistTemplateStatus.valueOf(status.name()),
                        description)));
        if (checklistItemRepository.findByTemplate_IdOrderByOrder(template.getId()).isEmpty()) {
            int order = 1;
            for (ChecklistItemSpec item : items) {
                checklistItemRepository.save(ChecklistItem.builder().template(template).itemText(item.text())
                        .order(order++).isRequired(item.required()).build());
            }
        }

        // Finalize only after all items exist. This preserves VERSION_IMMUTABLE while
        // keeping approved mandatory fixtures eligible for checklist distribution.
        ChecklistTemplateStatus requestedStatus = ChecklistTemplateStatus.valueOf(status.name());
        if (template.getStatus() != requestedStatus) {
            if (template.getStatus() == ChecklistTemplateStatus.APPROVED ||
                template.getStatus() == ChecklistTemplateStatus.ARCHIVED ||
                template.getMigrationReviewedAt() != null) {
                // Immutable templates (APPROVED, ARCHIVED, or migration-reviewed) cannot be mutated in place by DB trigger guard.
                return;
            }
            template.setStatus(requestedStatus);
            if (requestedStatus == ChecklistTemplateStatus.APPROVED) {
                template.setDistributionEnabled(true);
                template.setApprovedAt(Instant.now());
                template.setApprovedBy(seedApprovalUserId);
            } else {
                template.setDistributionEnabled(false);
                template.setApprovedAt(null);
                template.setApprovedBy(null);
            }
            checklistTemplateRepository.saveAndFlush(template);
        }
    }

    private ChecklistTemplate buildSeedChecklistTemplate(String name, ContentStage stage,
            ChecklistTemplateStatus status, String description) {
        ChecklistTemplate template = ChecklistTemplate.builder()
                .templateLineageId(UUID.randomUUID())
                .templateVersionId(UUID.randomUUID())
                .recipientScope(ChecklistRecipientScope.MOTHER)
                .name(name)
                .stage(stage)
                .status(status)
                .description(description)
                .build();
        switch (stage) {
            case PRE_PREGNANCY -> {
                template.setEligibilityAnchorType(ChecklistAnchorType.NONE);
                template.setEligibilityRangeUnit(ChecklistRangeUnit.DAY);
                template.setEligibilityStartInclusive(0);
                template.setEligibilityEndInclusive(0);
            }
            case PREGNANCY -> {
                template.setEligibilityAnchorType(ChecklistAnchorType.LMP);
                template.setEligibilityRangeUnit(ChecklistRangeUnit.WEEK);
                template.setEligibilityStartInclusive(0);
                template.setEligibilityEndInclusive(40);
            }
            case POSTPARTUM -> {
                template.setEligibilityAnchorType(ChecklistAnchorType.DELIVERY_DATE);
                template.setEligibilityRangeUnit(ChecklistRangeUnit.WEEK);
                template.setEligibilityStartInclusive(0);
                template.setEligibilityEndInclusive(6);
            }
        }
        return template;
    }

    /**
     * Populates the role-specific tables for the extra mother3/mother4, family2/family3,
     * expert2/expert3 accounts so they represent fully verified/accepted records, not just
     * bare user rows. Idempotent - safe to run on every startup.
     *
     * Note: "mother2@carebridge.dev" is intentionally NOT used here - it already exists in the
     * shared dev database as a teammate's manually-created test account (with its own journeys),
     * so seeding continues from mother3/mother4 to avoid colliding with it.
     */
    private void seedVerifiedProfileData(Map<String, User> savedUsers) {
        User admin = savedUsers.get("admin@carebridge.dev");

        MotherJourney mother3Journey = seedMotherJourney(
            savedUsers.get("mother3@carebridge.dev"), JourneyType.PREGNANCY, null);
        MotherJourney mother4Journey = seedMotherJourney(
            savedUsers.get("mother4@carebridge.dev"), JourneyType.POSTPARTUM,
            LocalDate.now().minusMonths(2));
        seedStandaloneAddBabyJourney(savedUsers.get("mother5@carebridge.dev"), "mother5");
        MotherJourney mother6Journey = seedStandaloneAddBabyJourney(
            savedUsers.get("mother6@carebridge.dev"), "mother6");

        seedStandaloneBabies(savedUsers.get("mother6@carebridge.dev"), mother6Journey);

        seedAcceptedCareGroup(
            savedUsers.get("mother3@carebridge.dev"), savedUsers.get("family2@carebridge.dev"),
            mother3Journey.getId(), null, "Mother Test 3's Care Group");
        if (mother4Journey.getJourneyType() == JourneyType.POSTPARTUM) {
            BabyProfile mother4Baby = seedBabyProfile(
                savedUsers.get("mother4@carebridge.dev"), mother4Journey);
            seedBabyJourneyViewData(savedUsers.get("mother4@carebridge.dev"), mother4Baby);
            seedAcceptedCareGroup(
                savedUsers.get("mother4@carebridge.dev"), savedUsers.get("family3@carebridge.dev"),
                mother4Journey.getId(), mother4Baby.getId(), "Mother Test 4's Care Group");
        } else {
            log.warn(
                "Skipping mother4 postpartum baby fixture because active canonical journey {} is {}",
                mother4Journey.getId(), mother4Journey.getJourneyType());
        }

        seedVerifiedExpert(savedUsers.get("expert2@carebridge.dev"), admin,
            "Sản khoa", "Bác sĩ Sản khoa", 8, "Bệnh viện Từ Dũ");
        seedVerifiedExpert(savedUsers.get("expert3@carebridge.dev"), admin,
            "Nhi khoa", "Bác sĩ Nhi khoa", 6, "Bệnh viện Nhi Đồng 1");
    }

    /**
     * Isolated standalone Add Baby manual-test fixture. It is deliberately created
     * only by the opt-in development seeder, never a migration or production bootstrap.
     */
    private MotherJourney seedStandaloneAddBabyJourney(User mother, String fixtureKey) {
        MotherJourney journey = motherJourneyRepository.findCanonical(mother.getId())
            .orElseGet(() -> saveMotherJourneyWithCanonicalSubject(
                mother,
                MotherJourney.builder()
                    .ownerUserId(mother.getId())
                    .journeyType(JourneyType.POSTPARTUM)
                    .startDate(LocalDate.now().minusDays(14))
                    .deliveryDate(LocalDate.now().minusDays(14))
                    .pregnancyOutcome(PregnancyOutcomeType.LIVE_BIRTH)
                    .pregnancyOutcomeDate(LocalDate.now().minusDays(14))
                    .dateSource(JourneyDateSource.SELF_REPORTED)
                    .status(JourneyStatus.ACTIVE)
                    .notes("[DEV][Standalone Add Baby] live-birth fixture")));

        if (journey.getJourneyType() != JourneyType.POSTPARTUM
            || journey.getPregnancyOutcome() != PregnancyOutcomeType.LIVE_BIRTH) {
            throw new IllegalStateException("Standalone Add Baby fixture must retain POSTPARTUM/LIVE_BIRTH");
        }

        UUID submissionId = UUID.nameUUIDFromBytes(
            ("standalone-baby-" + fixtureKey + "-live-birth").getBytes(StandardCharsets.UTF_8));
        UUID legacySubmissionId = UUID.nameUUIDFromBytes(
            ("story65-" + fixtureKey + "-live-birth").getBytes(StandardCharsets.UTF_8));
        boolean evidenceExists = pregnancyOutcomeEvidenceRepository
            .findByJourneyIdAndSubmissionId(journey.getId(), legacySubmissionId).isPresent()
            || pregnancyOutcomeEvidenceRepository
                .findByJourneyIdAndSubmissionId(journey.getId(), submissionId).isPresent();
        if (!evidenceExists) {
            var previousEvidence = pregnancyOutcomeEvidenceRepository
                .findFirstByJourneyIdOrderByRevisionNumberDesc(journey.getId());
            pregnancyOutcomeEvidenceRepository.save(PregnancyOutcomeEvidence.builder()
                .journeyId(journey.getId())
                .ownerUserId(mother.getId())
                .submissionId(submissionId)
                .outcomeType(PregnancyOutcomeType.LIVE_BIRTH)
                .outcomeDate(journey.getPregnancyOutcomeDate())
                .source(JourneyDateSource.SELF_REPORTED)
                .actorUserId(mother.getId())
                .reason("[DEV][Standalone Add Baby] synthetic live-birth fixture")
                .effectiveAt(Instant.now())
                .revisionNumber(previousEvidence
                    .map(PregnancyOutcomeEvidence::getRevisionNumber)
                    .orElse(0) + 1)
                .supersedesEvidenceId(previousEvidence
                    .map(PregnancyOutcomeEvidence::getId)
                    .orElse(null))
                .journeyVersion(journey.getVersion())
                .semanticHash("dev-standalone-baby-" + fixtureKey + "-live-birth")
                .correction(false)
                .build());
        }
        return journey;
    }

    private void seedStandaloneBabies(User mother, MotherJourney journey) {
        List<BabyProfile> existing = babyProfileRepository
            .findByOwnerUserIdAndStatusOrderByCreatedAtAsc(mother.getId(), BabyProfileStatus.ACTIVE);
        seedStandaloneBaby(
            existing, mother, journey,
            "[DEV][Standalone] Baby A", "[DEV][Story 6.5] Baby A", Gender.FEMALE, true);
        seedStandaloneBaby(
            existing, mother, journey,
            "[DEV][Standalone] Baby B", "[DEV][Story 6.5] Baby B", Gender.MALE, false);
    }

    private void seedStandaloneBaby(
            List<BabyProfile> existing, User mother, MotherJourney journey,
            String nickname, String legacyNickname, Gender gender,
            boolean selectedInLegacyProfileSwitcher) {
        if (existing.stream().anyMatch(baby -> nickname.equals(baby.getNickname()))) return;

        var legacyFixture = existing.stream()
            .filter(baby -> legacyNickname.equals(baby.getNickname()))
            .findFirst();
        if (legacyFixture.isPresent()) {
            BabyProfile baby = legacyFixture.get();
            baby.setNickname(nickname);
            babyProfileRepository.save(baby);
            return;
        }

        babyProfileRepository.save(BabyProfile.builder()
            .ownerUserId(mother.getId())
            .nickname(nickname)
            .birthDate(journey.getDeliveryDate())
            .gender(gender)
            .birthWeightKg(new BigDecimal("3.20"))
            .birthLengthCm(new BigDecimal("49.0"))
            .status(BabyProfileStatus.ACTIVE)
            // The legacy schema permits one selected profile per owner. Both rows
            // remain ACTIVE standalone profiles; this flag controls only the
            // legacy profile switcher selection.
            .active(selectedInLegacyProfileSwitcher)
            .build());
    }

    private MotherJourney seedMotherJourney(User mother, JourneyType type, LocalDate deliveryDate) {
        var activeCanonical = motherJourneyRepository
            .findFirstByOwnerUserIdAndStatusOrderByCreatedAtDesc(mother.getId(), JourneyStatus.ACTIVE);
        if (activeCanonical.isPresent()) {
            return activeCanonical.get();
        }
        List<MotherJourney> existing = motherJourneyRepository
            .findByOwnerUserIdAndJourneyTypeAndStatusOrderByCreatedAtAsc(mother.getId(), type, JourneyStatus.ACTIVE);
        if (!existing.isEmpty()) {
            return existing.get(0);
        }
        return saveMotherJourneyWithCanonicalSubject(
            mother,
            MotherJourney.builder()
                .ownerUserId(mother.getId())
                .journeyType(type)
                .startDate(LocalDate.now().minusMonths(3))
                .lastMenstrualDate(LocalDate.now().minusMonths(5))
                .estimatedDueDate(LocalDate.now().plusMonths(4))
                .deliveryDate(deliveryDate)
                .status(JourneyStatus.ACTIVE)
                .notes("Seeded verified test journey"));
    }

    private MotherJourney saveMotherJourneyWithCanonicalSubject(
            User mother, MotherJourney.MotherJourneyBuilder journeyBuilder) {
        UUID careSubjectId = ensureMotherCareSubject(mother.getId());
        MotherJourney journey = motherJourneyRepository.saveAndFlush(
            journeyBuilder.careSubjectId(careSubjectId).build());
        if (motherJourneyRepository.linkMotherCareSubject(careSubjectId, journey.getId()) != 1) {
            throw new IllegalStateException("Unable to link seeded mother care subject to journey");
        }
        return journey;
    }

    private UUID ensureMotherCareSubject(UUID ownerUserId) {
        UUID existing = motherJourneyRepository.findMotherCareSubjectId(ownerUserId);
        if (existing != null) {
            return existing;
        }
        motherJourneyRepository.ensureMotherCareSubject(UUID.randomUUID(), ownerUserId);
        UUID created = motherJourneyRepository.findMotherCareSubjectId(ownerUserId);
        if (created == null) {
            throw new IllegalStateException("Unable to create seeded mother care subject");
        }
        return created;
    }

    private BabyProfile seedBabyProfile(User mother, MotherJourney journey) {
        List<BabyProfile> existing = babyProfileRepository
            .findByOwnerUserIdAndStatusOrderByCreatedAtAsc(mother.getId(), BabyProfileStatus.ACTIVE);
        if (!existing.isEmpty()) {
            BabyProfile baby = existing.get(0);
            jdbcTemplate.update(
                "UPDATE care_subjects SET status = 'INACTIVE' WHERE owner_user_id = ? AND care_subject_id <> ? AND subject_type = 'BABY'",
                mother.getId(), baby.getId());
            baby.setActive(true);
            if (baby.getBirthWeightKg() == null) baby.setBirthWeightKg(new BigDecimal("3.40"));
            if (baby.getBirthLengthCm() == null) baby.setBirthLengthCm(new BigDecimal("50.0"));
            // saveAndFlush: seedBabyJourneyViewData writes child rows via raw jdbcTemplate right
            // after this returns, which needs the care_subjects row to already be visible in the
            // DB (Hibernate write-behind alone would leave it queued, tripping the FK constraint).
            return babyProfileRepository.saveAndFlush(baby);
        }
        return babyProfileRepository.saveAndFlush(BabyProfile.builder()
            .ownerUserId(mother.getId())
            .nickname("Bé " + mother.getName())
            .birthDate(journey.getDeliveryDate() != null
                ? journey.getDeliveryDate() : journey.getStartDate().plusMonths(1))
            .gender(Gender.FEMALE)
            .birthWeightKg(new BigDecimal("3.40"))
            .birthLengthCm(new BigDecimal("50.0"))
            .status(BabyProfileStatus.ACTIVE)
            .active(true)
            .build());
    }

    /**
     * Seeds deterministic MF-03 data for the official Baby Journey view.
     * Fixed IDs plus UPSERT keep startup idempotent, while refreshed timestamps keep the
     * rolling 24-hour summary useful whenever the dev environment is restarted.
     */
    private void seedBabyJourneyViewData(User recorder, BabyProfile baby) {
        Instant now = Instant.now();

        upsertDailyLog("f0300000-0000-0000-0000-000000000001", baby, recorder,
            "FEEDING", now.minusSeconds(60 * 60), new BigDecimal("90"), "ml", "[DEV][MF-03] Cữ bú 1");
        upsertDailyLog("f0300000-0000-0000-0000-000000000002", baby, recorder,
            "FEEDING", now.minusSeconds(4 * 60 * 60), new BigDecimal("100"), "ml", "[DEV][MF-03] Cữ bú 2");
        upsertDailyLog("f0300000-0000-0000-0000-000000000003", baby, recorder,
            "FEEDING", now.minusSeconds(7 * 60 * 60), new BigDecimal("85"), "ml", "[DEV][MF-03] Cữ bú 3");
        upsertDailyLog("f0300000-0000-0000-0000-000000000004", baby, recorder,
            "FEEDING", now.minusSeconds(10 * 60 * 60), new BigDecimal("95"), "ml", "[DEV][MF-03] Cữ bú 4");
        upsertDailyLog("f0300000-0000-0000-0000-000000000005", baby, recorder,
            "FEEDING", now.minusSeconds(14 * 60 * 60), new BigDecimal("90"), "ml", "[DEV][MF-03] Cữ bú 5");
        upsertDailyLog("f0300000-0000-0000-0000-000000000006", baby, recorder,
            "FEEDING", now.minusSeconds(19 * 60 * 60), new BigDecimal("80"), "ml", "[DEV][MF-03] Cữ bú 6");

        upsertDailyLog("f0300000-0000-0000-0000-000000000007", baby, recorder,
            "SLEEP", now.minusSeconds(2 * 60 * 60), new BigDecimal("3.5"), "hours", "[DEV][MF-03] Giấc ngủ sáng");
        upsertDailyLog("f0300000-0000-0000-0000-000000000008", baby, recorder,
            "SLEEP", now.minusSeconds(9 * 60 * 60), new BigDecimal("4.0"), "hours", "[DEV][MF-03] Giấc ngủ chiều");
        upsertDailyLog("f0300000-0000-0000-0000-000000000009", baby, recorder,
            "SLEEP", now.minusSeconds(17 * 60 * 60), new BigDecimal("5.5"), "hours", "[DEV][MF-03] Giấc ngủ đêm");

        upsertDailyLog("f0300000-0000-0000-0000-000000000010", baby, recorder,
            "DIAPER", now.minusSeconds(3 * 60 * 60), null, null, "[DEV][MF-03] Tã ướt");
        upsertDailyLog("f0300000-0000-0000-0000-000000000011", baby, recorder,
            "DIAPER", now.minusSeconds(8 * 60 * 60), null, null, "[DEV][MF-03] Tã ướt");
        upsertDailyLog("f0300000-0000-0000-0000-000000000012", baby, recorder,
            "DIAPER", now.minusSeconds(13 * 60 * 60), null, null, "[DEV][MF-03] Tã bẩn");
        upsertDailyLog("f0300000-0000-0000-0000-000000000013", baby, recorder,
            "DIAPER", now.minusSeconds(20 * 60 * 60), null, null, "[DEV][MF-03] Tã ướt");

        upsertGrowthMeasurement("f0310000-0000-0000-0000-000000000001", baby,
            LocalDate.now().minusDays(56), "3.40", "50.0", "34.0");
        upsertGrowthMeasurement("f0310000-0000-0000-0000-000000000002", baby,
            LocalDate.now().minusDays(42), "3.65", "52.0", "35.0");
        upsertGrowthMeasurement("f0310000-0000-0000-0000-000000000003", baby,
            LocalDate.now().minusDays(28), "3.90", "54.0", "36.0");
        upsertGrowthMeasurement("f0310000-0000-0000-0000-000000000004", baby,
            LocalDate.now().minusDays(14), "4.15", "56.0", "37.0");
        upsertGrowthMeasurement("f0310000-0000-0000-0000-000000000005", baby,
            LocalDate.now(), "4.40", "58.0", "38.0");
    }

    private void upsertDailyLog(String id, BabyProfile baby, User recorder, String logType,
                                Instant occurredAt, BigDecimal quantity, String unit, String note) {
        Timestamp timestamp = Timestamp.from(occurredAt);
        // care_logs is a compatibility VIEW (canonical store: care_tasks CARE_LOG
        // rows); ON CONFLICT is not available through INSTEAD OF triggers, so the
        // idempotent upsert targets the canonical table directly.
        jdbcTemplate.update("""
            INSERT INTO care_tasks
                (task_id, task_type, owner_user_id, creator_user_id, care_subject_id,
                 title, description, scheduled_at, status,
                 source_reference_type, source_reference_id, metadata_jsonb,
                 created_at, updated_at)
            SELECT ?, 'CARE_LOG', cs.owner_user_id, ?, cs.care_subject_id,
                   'Care log: ' || ?, ?, ?, 'ACTIVE',
                   'CARE_LOG', ?,
                   jsonb_build_object('logType', CAST(? AS text),
                                      'quantity', CAST(? AS numeric),
                                      'unit', CAST(? AS text),
                                      'recordedBy', CAST(? AS uuid)),
                   ?, ?
              FROM care_subjects cs
             WHERE cs.care_subject_id = ?
            ON CONFLICT (task_id) DO UPDATE SET
                care_subject_id = EXCLUDED.care_subject_id,
                title = EXCLUDED.title,
                description = EXCLUDED.description,
                scheduled_at = EXCLUDED.scheduled_at,
                status = 'ACTIVE',
                creator_user_id = EXCLUDED.creator_user_id,
                metadata_jsonb = EXCLUDED.metadata_jsonb,
                created_at = EXCLUDED.created_at,
                updated_at = EXCLUDED.updated_at
            """, UUID.fromString(id), recorder.getId(), logType, note, timestamp,
            UUID.fromString(id), logType, quantity, unit, recorder.getId(),
            timestamp, timestamp, baby.getId());
    }

    /**
     * Seeds one growth measuring session.
     *
     * <p>Wave 13 (V3 §3.12): growth lives in {@code health_observations} as three rows sharing
     * a {@code measurement_group_id}, the shape
     * {@link com.carebridge.backend.carejourney.repository.GrowthMeasurementStore} reads.
     * {@code growth_measurements} is frozen and awaiting its contract migration.
     *
     * <p>Upserts on {@code (legacy_source, legacy_id)} — the same key that makes the wave-13
     * backfill idempotent — so reseeding updates in place instead of duplicating.
     */
    private void upsertGrowthMeasurement(String id, BabyProfile baby, LocalDate measuredDate,
                                         String weightKg, String heightCm, String headCm) {
        UUID groupId = UUID.fromString(id);
        Timestamp now = Timestamp.from(Instant.now());
        Timestamp observedAt = Timestamp.from(
                measuredDate.atStartOfDay(ZoneId.of("Asia/Ho_Chi_Minh")).toInstant());

        upsertGrowthObservation(groupId, baby, "BABY_WEIGHT", new BigDecimal(weightKg), "kg",
                observedAt, now);
        upsertGrowthObservation(groupId, baby, "BABY_HEIGHT", new BigDecimal(heightCm), "cm",
                observedAt, now);
        upsertGrowthObservation(groupId, baby, "BABY_HEAD_CIRCUMFERENCE", new BigDecimal(headCm), "cm",
                observedAt, now);
    }

    private void upsertGrowthObservation(UUID groupId, BabyProfile baby, String observationType,
                                         BigDecimal value, String unit,
                                         Timestamp observedAt, Timestamp now) {
        jdbcTemplate.update("""
            INSERT INTO health_observations
                (health_observation_id, care_subject_id, observation_type, subject_type,
                 value_numeric, unit, observed_at, source_type, context_jsonb,
                 measurement_group_id, legacy_source, legacy_id, deleted_at, created_at, updated_at)
            VALUES (gen_random_uuid(), ?, ?, 'BABY', ?, ?, ?, 'MANUAL',
                    jsonb_build_object('measurementSetting', 'HOME',
                                       'note', '[DEV][MF-03] Dữ liệu tăng trưởng mẫu'),
                    ?, 'growth_measurements', ?, NULL, ?, ?)
            ON CONFLICT (legacy_source, legacy_id) DO UPDATE SET
                care_subject_id = EXCLUDED.care_subject_id,
                value_numeric = EXCLUDED.value_numeric,
                unit = EXCLUDED.unit,
                observed_at = EXCLUDED.observed_at,
                context_jsonb = EXCLUDED.context_jsonb,
                deleted_at = NULL,
                updated_at = EXCLUDED.updated_at
            """, baby.getId(), observationType, value, unit, observedAt,
            groupId, groupId + ":" + observationType, now, now);
    }

    private void seedAcceptedCareGroup(User mother, User familyMember, java.util.UUID journeyId,
                                        java.util.UUID babyId, String groupName) {
        List<CareGroup> groups = careGroupRepository.findByOwnerUserIdAndStatus(mother.getId(), CareGroupStatus.ACTIVE);
        CareGroup group = groups.isEmpty()
            ? careGroupRepository.save(CareGroup.builder()
                .ownerUserId(mother.getId())
                .groupName(groupName)
                .description("Seeded verified test care group")
                .linkedJourneyId(journeyId)
                .linkedBabyProfileId(babyId)
                .status(CareGroupStatus.ACTIVE)
                .build())
            : groups.get(0);

        ensureAcceptedCareGroupMember(group, mother, GroupMemberRole.OWNER, false);
        ensureAcceptedCareGroupMember(group, familyMember, GroupMemberRole.MEMBER, true);
    }

    private void ensureAcceptedCareGroupMember(
            CareGroup group, User user, GroupMemberRole role, boolean emergencyContact) {
        var existing = careGroupMemberRepository.findByCareGroupIdAndUserId(group.getId(), user.getId());
        if (existing.isEmpty()) {
            CareGroupMember member = CareGroupMember.builder()
                .careGroupId(group.getId())
                .userId(user.getId())
                .memberRole(role)
                .inviteStatus(InviteStatus.ACCEPTED)
                .joinedAt(Instant.now())
                .build();
            member.setEmergencyContact(emergencyContact);
            member.setEmergencyContactPriority(emergencyContact ? (short) 1 : null);
            careGroupMemberRepository.save(member);
            return;
        }

        CareGroupMember member = existing.get();
        if (member.getMemberRole() != role || member.getInviteStatus() != InviteStatus.ACCEPTED
                || member.getJoinedAt() == null
                || member.isEmergencyContact() != emergencyContact
                || (emergencyContact && !java.util.Objects.equals(
                        member.getEmergencyContactPriority(), (short) 1))) {
            member.setMemberRole(role);
            member.setInviteStatus(InviteStatus.ACCEPTED);
            if (member.getJoinedAt() == null) {
                member.setJoinedAt(Instant.now());
            }
            member.setEmergencyContact(emergencyContact);
            member.setEmergencyContactPriority(emergencyContact ? (short) 1 : null);
            careGroupMemberRepository.save(member);
        }
    }

    private void seedVerifiedExpert(User expertUser, User admin, String specialty,
                                     String professionalTitle, int experienceYears, String workplace) {
        ExpertProfile profile = expertProfileRepository.findByUserId(expertUser.getId())
            .orElseGet(() -> insertExpertProfileRow(expertUser, admin, specialty, professionalTitle,
                experienceYears, workplace));

        if (profile.getVerificationStatus() != VerificationStatus.APPROVED) {
            profile.setVerificationStatus(VerificationStatus.APPROVED);
            profile.setVerifiedAt(LocalDateTime.now());
            profile.setVerifiedBy(admin.getId());
            profile = expertProfileRepository.save(profile);
        }

        if (!expertCredentialRepository.existsByExpertProfileIdAndCredentialType(profile.getExpertProfileId(), "MEDICAL_LICENSE")) {
            expertCredentialRepository.save(ExpertCredential.builder()
                .expertProfileId(profile.getExpertProfileId())
                .credentialType("MEDICAL_LICENSE")
                .credentialNumber("SEED-" + expertUser.getEmail())
                .issuer("Bộ Y tế")
                .issuedDate(LocalDate.now().minusYears(2))
                .expiryDate(LocalDate.now().plusYears(3))
                .reviewStatus(ReviewStatus.APPROVED)
                .reviewNote("Seeded verified test credential")
                .reviewedBy(admin.getId())
                .reviewedAt(LocalDateTime.now())
                .build());
        }

    }

    private ExpertProfile insertExpertProfileRow(User expertUser, User admin, String specialty,
                                                  String professionalTitle, int experienceYears, String workplace) {
        return expertProfileRepository.save(ExpertProfile.builder()
            .expertProfileId(expertUser.getId())
            .specialty(specialty)
            .professionalTitle(professionalTitle)
            .experienceYears(experienceYears)
            .workplace(workplace)
            .consultationScope("Tư vấn thai sản và chăm sóc mẹ bé")
            .verificationStatus(VerificationStatus.APPROVED)
            .verifiedAt(LocalDateTime.now())
            .verifiedBy(admin.getId())
            .build());
    }

    /**
     * Seeds ~25 community sample rows (questions, answers, profiles, likes, bookmarks, reports,
     * moderation actions) spanning PENDING/APPROVED/HIDDEN/LOCKED states, so both the community
     * feed and the moderator queues (/pending-content, /queue) have realistic content to exercise
     * instead of only the one manually-created question/answer. Idempotent - matched by
     * author+title (questions), question+author (answers), and reporter+target+category (reports).
     */
    private void seedCommunitySampleData(Map<String, User> savedUsers) {
        Map<String, UUID> topicIdByName = new HashMap<>();
        for (CommunityTopic topic : communityTopicRepository.findAllByOrderBySortOrderAsc()) {
            topicIdByName.put(topic.getName(), topic.getId());
        }
        if (topicIdByName.isEmpty()) {
            log.warn("[DevDataSeeder] No community topics found - skipping community sample data.");
            return;
        }

        UUID mother = savedUsers.get("mother@carebridge.dev").getId();
        UUID mother3 = savedUsers.get("mother3@carebridge.dev").getId();
        UUID mother4 = savedUsers.get("mother4@carebridge.dev").getId();
        UUID family = savedUsers.get("family@carebridge.dev").getId();
        UUID family2 = savedUsers.get("family2@carebridge.dev").getId();
        UUID family3 = savedUsers.get("family3@carebridge.dev").getId();
        UUID expert = savedUsers.get("expert@carebridge.dev").getId();
        UUID expert2 = savedUsers.get("expert2@carebridge.dev").getId();
        UUID expert3 = savedUsers.get("expert3@carebridge.dev").getId();
        UUID moderator = savedUsers.get("moderator@carebridge.dev").getId();

        // 6 questions: 2 PENDING (feed the pending-content moderation queue), the rest APPROVED/HIDDEN/LOCKED
        seedQuestion(mother, topicIdByName.get("Dinh dưỡng thai kỳ"),
            "Bầu 20 tuần ăn gì để bổ sung canxi tự nhiên?",
            "Em đang mang thai tuần 20, muốn hỏi các mẹ có kinh nghiệm bổ sung canxi qua thực phẩm thay vì thuốc không ạ?",
            PregnancyStage.PREGNANCY, (short) 20, null, UrgencyLevel.NORMAL, false, QuestionStatus.PENDING, 0);

        seedQuestion(mother3, topicIdByName.get("Sức khỏe thai nhi"),
            "Thai 32 tuần đạp ít hơn bình thường có đáng lo không?",
            "Hai ngày nay em thấy con đạp ít hơn hẳn so với trước, em có nên đi khám ngay không ạ?",
            PregnancyStage.PREGNANCY, (short) 32, null, UrgencyLevel.URGENT, false, QuestionStatus.PENDING, 0);

        CommunityQuestion q3 = seedQuestion(mother4, topicIdByName.get("Chăm sóc sau sinh"),
            "Vết mổ sau sinh 2 tháng vẫn còn hơi đau, có bình thường không?",
            "Em sinh mổ được 2 tháng, thỉnh thoảng vết mổ vẫn hơi đau âm ỉ, có mẹ nào gặp giống em không?",
            PregnancyStage.POSTPARTUM, null, (short) 2, UrgencyLevel.NORMAL, true, QuestionStatus.APPROVED, 1);

        CommunityQuestion q4 = seedQuestion(family, topicIdByName.get("Nuôi con bằng sữa mẹ"),
            "Bé 5 tháng bú mẹ hoàn toàn nhưng tăng cân chậm, phải làm sao?",
            "Bé nhà em 5 tháng bú mẹ hoàn toàn nhưng tháng này chỉ tăng 300g, gia đình đang lo lắng.",
            PregnancyStage.POSTPARTUM, null, (short) 5, UrgencyLevel.NORMAL, false, QuestionStatus.APPROVED, 2);

        CommunityQuestion q5 = seedQuestion(mother, topicIdByName.get("Tâm lý & Cảm xúc"),
            "Mang thai tuần 10 hay khóc vô cớ, có phải trầm cảm thai kỳ không?",
            "Dạo này em hay xúc động và khóc không rõ lý do, không biết có phải dấu hiệu trầm cảm thai kỳ không ạ?",
            PregnancyStage.PREGNANCY, (short) 10, null, UrgencyLevel.NORMAL, true, QuestionStatus.HIDDEN, 0);

        CommunityQuestion q6 = seedQuestion(family2, topicIdByName.get("Chăm sóc bé sơ sinh"),
            "Có nên dùng phấn rôm cho bé sơ sinh 1 tháng tuổi không?",
            "Nhà em nghe nói phấn rôm không tốt cho bé sơ sinh, mọi người cho em xin ý kiến với ạ.",
            PregnancyStage.POSTPARTUM, null, (short) 1, UrgencyLevel.LOW, false, QuestionStatus.LOCKED, 1);

        // 6 answers: 2 PENDING (feed the pending-content moderation queue), the rest APPROVED/HIDDEN
        CommunityAnswer a1 = seedAnswer(q3.getId(), expert,
            "Đau âm ỉ nhẹ ở vết mổ sau 2 tháng vẫn có thể là bình thường do mô sẹo đang lành, "
                + "nhưng nếu đau tăng dần, sưng đỏ hoặc chảy dịch thì bạn nên tái khám ngay.",
            true, false, AnswerStatus.APPROVED, 1);

        seedAnswer(q3.getId(), family3,
            "Chị dâu mình cũng bị y vậy sau sinh mổ, đi khám thì bác sĩ nói mô sẹo cần vài tháng mới hết hẳn.",
            false, true, AnswerStatus.PENDING, 0);

        CommunityAnswer a3 = seedAnswer(q4.getId(), expert2,
            "Nếu bé vẫn bú tốt, đi tiểu đủ 6-8 lần/ngày và phát triển vận động bình thường thì tăng cân chậm "
                + "một tháng chưa đáng lo, nhưng nên đưa bé đi kiểm tra tăng trưởng định kỳ để loại trừ nguyên nhân bệnh lý.",
            true, false, AnswerStatus.APPROVED, 1);

        seedAnswer(q4.getId(), mother3,
            "Bé nhà mình tháng trước cũng tăng chậm, sau đó mình cho bú nhiều cữ đêm hơn thì tháng sau tăng lại bình thường.",
            false, true, AnswerStatus.PENDING, 0);

        seedAnswer(q5.getId(), mother4,
            "Mình cũng từng như vậy, sau đó chia sẻ với chồng và đi khám thì đỡ hẳn, bạn đừng ngại tìm hỗ trợ tâm lý nhé.",
            false, true, AnswerStatus.HIDDEN, 0);

        CommunityAnswer a6 = seedAnswer(q6.getId(), expert3,
            "Không nên dùng phấn rôm cho bé sơ sinh vì bột phấn có thể gây kích ứng đường hô hấp, "
                + "nên vệ sinh và giữ da bé khô thoáng thay vì dùng phấn.",
            true, false, AnswerStatus.APPROVED, 1);



        // Likes + bookmarks, kept consistent with each item's seeded like_count above
        seedQuestionLike(family, q3.getId());
        seedQuestionLike(expert, q4.getId());
        seedQuestionLike(mother3, q4.getId());

        seedAnswerLike(mother, a1.getId());
        seedAnswerLike(family, a3.getId());
        seedAnswerLike(mother4, a6.getId());

        seedBookmark(family, q3.getId());
        seedBookmark(mother3, q4.getId());
        seedTopicFollow(mother3, topicIdByName.get("Dinh dưỡng"));
        // q5 is anonymous; this makes the moderator queue exercise anonymous internal traceability
        // alongside a system-generated (not user-submitted) safety signal.
        seedAutomatedReport(q5.getId(), ReportTargetType.QUESTION);

        // 4 content reports: 2 PENDING (feed the report-based moderation queue), 1 RESOLVED, 1 DISMISSED
        seedContentReport(family, q4.getId(), ReportTargetType.QUESTION, "SPAM",
            "Nghi ngờ nội dung quảng cáo sản phẩm trá hình dưới dạng câu hỏi.",
            ReportStatus.PENDING, null, null);

        seedContentReport(mother3, a3.getId(), ReportTargetType.ANSWER, "UNSAFE_ADVICE",
            "Câu trả lời có thể chưa đủ căn cứ y khoa, cần chuyên gia xác nhận lại.",
            ReportStatus.PENDING, null, null);

        ContentReport r3 = seedContentReport(family2, q6.getId(), ReportTargetType.QUESTION, "HARASSMENT",
            "Một số bình luận qua lại trong phần thảo luận có ngôn từ gay gắt.",
            ReportStatus.RESOLVED, moderator, Instant.now().minus(java.time.Duration.ofDays(1)));

        seedContentReport(mother4, q3.getId(), ReportTargetType.QUESTION, "OTHER",
            "Nội dung nhạy cảm về vết mổ, đề nghị kiểm tra lại - nhưng nội dung vẫn phù hợp sau khi xem xét.",
            ReportStatus.DISMISSED, moderator, Instant.now().minus(java.time.Duration.ofDays(2)));

        // Moderation actions: one tied to the resolved report above (LOCK on q6), one proactive (HIDE on q5,
        // reportId=null per ADR-001 - UC-100 style action with no underlying user report)
        seedModerationAction(q6.getId(), ReportTargetType.QUESTION, ModerationActionType.LOCK, moderator,
            "Khóa câu hỏi do phần thảo luận phát sinh tranh cãi, cần rà soát lại.", r3.getId());
        seedModerationAction(q5.getId(), ReportTargetType.QUESTION, ModerationActionType.HIDE, moderator,
            "Ẩn câu hỏi chủ động do nội dung nhạy cảm, đã trao đổi riêng với tác giả.", null);
    }

    private CommunityQuestion seedQuestion(UUID authorId, UUID topicId, String title, String body,
            PregnancyStage stage, Short pregnancyWeek, Short babyAgeMonths, UrgencyLevel urgency,
            boolean anonymous, QuestionStatus status, int likeCount) {
        return communityQuestionRepository.findByAuthorIdAndTitle(authorId, title)
            .orElseGet(() -> communityQuestionRepository.save(CommunityQuestion.builder()
                .topicId(topicId)
                .authorId(authorId)
                .title(title)
                .body(body)
                .stage(stage)
                .pregnancyWeek(pregnancyWeek)
                .babyAgeMonths(babyAgeMonths)
                .urgency(urgency)
                .anonymous(anonymous)
                .status(status)
                .likeCount(likeCount)
                .answerCount(0)
                .build()));
    }

    private CommunityAnswer seedAnswer(UUID questionId, UUID authorId, String body,
            boolean expertLabeled, boolean personalExperience, AnswerStatus status, int likeCount) {
        return communityAnswerRepository.findFirstByQuestionIdAndAuthorIdOrderByCreatedAtAsc(questionId, authorId)
            .orElseGet(() -> {
                CommunityAnswer saved = communityAnswerRepository.save(CommunityAnswer.builder()
                    .questionId(questionId)
                    .authorId(authorId)
                    .body(body)
                    .expertLabeled(expertLabeled)
                    .personalExperience(personalExperience)
                    .status(status)
                    .likeCount(likeCount)
                    .build());
                if (status == AnswerStatus.APPROVED) {
                    communityQuestionRepository.incrementAnswerCount(questionId);
                }
                return saved;
            });
    }



    private void seedQuestionLike(UUID userId, UUID questionId) {
        if (communityQuestionLikeRepository.existsByUserIdAndQuestionId(userId, questionId)) {
            return;
        }
        communityQuestionLikeRepository.save(CommunityQuestionLike.builder()
            .userId(userId)
            .questionId(questionId)
            .build());
    }

    private void seedAnswerLike(UUID userId, UUID answerId) {
        if (communityAnswerLikeRepository.existsByUserIdAndAnswerId(userId, answerId)) {
            return;
        }
        communityAnswerLikeRepository.save(CommunityAnswerLike.builder()
            .userId(userId)
            .answerId(answerId)
            .build());
    }

    private void seedBookmark(UUID userId, UUID questionId) {
        if (communityBookmarkRepository.existsByUserIdAndQuestionId(userId, questionId)) {
            return;
        }
        communityBookmarkRepository.save(CommunityBookmark.builder()
            .userId(userId)
            .questionId(questionId)
            .build());
    }

    private void seedTopicFollow(UUID userId, UUID topicId) {
        if (topicId == null || userTopicFollowRepository.findByUserIdAndTopicId(userId, topicId).isPresent()) return;
        userTopicFollowRepository.save(UserTopicFollow.builder().userId(userId).topicId(topicId).build());
    }

    private void seedAutomatedReport(UUID targetId, ReportTargetType targetType) {
        String category = ReportCategory.UNSAFE_ADVICE.name();
        if (contentReportRepository.findByTargetIdAndCategory(targetId, category).isPresent()) return;
        Instant now = Instant.now();
        contentReportRepository.save(ContentReport.builder().targetId(targetId).targetType(targetType)
                .category(category).description("System safety classifier signal")
                .reportSource(ReportSource.AUTOMATED).status(ReportStatus.PENDING)
                .createdAt(now).updatedAt(now).build());
    }

    private ContentReport seedContentReport(UUID reporterId, UUID targetId, ReportTargetType targetType,
            String category, String description, ReportStatus status, UUID assignedModeratorId, Instant resolvedAt) {
        return contentReportRepository.findByReporterUserIdAndTargetIdAndCategory(reporterId, targetId, category)
            .orElseGet(() -> {
                Instant now = Instant.now();
                return contentReportRepository.save(ContentReport.builder()
                    .reporterUserId(reporterId)
                    .targetId(targetId)
                    .targetType(targetType)
                    .category(category)
                    .description(description)
                    .status(status)
                    .assignedModeratorId(assignedModeratorId)
                    .createdAt(now)
                    .updatedAt(now)
                    .resolvedAt(resolvedAt)
                    .build());
            });
    }

    private void seedModerationAction(UUID targetId, ReportTargetType targetType, ModerationActionType actionType,
            UUID moderatorId, String reason, UUID reportId) {
        if (moderationActionRepository.existsByTargetIdAndActionType(targetId, actionType)) {
            return;
        }
        moderationActionRepository.save(ModerationAction.builder()
            .reportId(reportId)
            .targetId(targetId)
            .targetType(targetType)
            .actionType(actionType)
            .moderatorUserId(moderatorId)
            .reason(reason)
            .actionAt(Instant.now())
            .build());
    }

    /**
     * Second wave of ~40 community sample rows (8 questions, 10 answers, 3 profiles, 5 question
     * likes, 5 answer likes, 3 bookmarks, 4 reports, 2 moderation actions), covering the two
     * remaining topics not used by {@link #seedCommunitySampleData} and continuing to feed both
     * moderation queues (PENDING questions/answers, PENDING reports). Idempotent via the same
     * author+title / question+author / reporter+target+category matching as the first batch.
     */
    private void seedCommunitySampleDataBatch2(Map<String, User> savedUsers) {
        Map<String, UUID> topicIdByName = new HashMap<>();
        for (CommunityTopic topic : communityTopicRepository.findAllByOrderBySortOrderAsc()) {
            topicIdByName.put(topic.getName(), topic.getId());
        }
        if (topicIdByName.isEmpty()) {
            log.warn("[DevDataSeeder] No community topics found - skipping community sample data batch 2.");
            return;
        }

        UUID mother = savedUsers.get("mother@carebridge.dev").getId();
        UUID mother3 = savedUsers.get("mother3@carebridge.dev").getId();
        UUID mother4 = savedUsers.get("mother4@carebridge.dev").getId();
        UUID family = savedUsers.get("family@carebridge.dev").getId();
        UUID family2 = savedUsers.get("family2@carebridge.dev").getId();
        UUID family3 = savedUsers.get("family3@carebridge.dev").getId();
        UUID expert = savedUsers.get("expert@carebridge.dev").getId();
        UUID expert2 = savedUsers.get("expert2@carebridge.dev").getId();
        UUID expert3 = savedUsers.get("expert3@carebridge.dev").getId();
        UUID moderator = savedUsers.get("moderator@carebridge.dev").getId();

        // 8 questions: 2 PENDING (feed the pending-content moderation queue), the rest APPROVED/HIDDEN/LOCKED
        CommunityQuestion q7 = seedQuestion(family3, topicIdByName.get("Giấc ngủ và thể chất"),
            "Ngủ nghiêng bên phải khi mang thai có an toàn không?",
            "Em quen ngủ nghiêng bên phải từ trước khi mang thai, nghe nói bầu nên ngủ nghiêng trái, em có cần đổi không?",
            PregnancyStage.PREGNANCY, (short) 24, null, UrgencyLevel.NORMAL, false, QuestionStatus.PENDING, 0);

        CommunityQuestion q8 = seedQuestion(mother4, topicIdByName.get("Hỏi đáp chung"),
            "Sau sinh bao lâu thì có thể quan hệ vợ chồng trở lại?",
            "Vợ chồng em muốn hỏi kinh nghiệm từ các mẹ, sau sinh thường thì nên kiêng cữ bao lâu là hợp lý ạ?",
            PregnancyStage.POSTPARTUM, null, (short) 3, UrgencyLevel.NORMAL, true, QuestionStatus.PENDING, 0);

        CommunityQuestion q9 = seedQuestion(mother, topicIdByName.get("Dinh dưỡng thai kỳ"),
            "Bầu 3 tháng đầu nghén nặng không ăn được gì, phải làm sao?",
            "Em nghén rất nặng, ngửi mùi thức ăn là buồn nôn, sụt gần 2kg rồi, các mẹ có cách nào giúp ăn được không ạ?",
            PregnancyStage.PREGNANCY, (short) 8, null, UrgencyLevel.NORMAL, false, QuestionStatus.APPROVED, 2);

        CommunityQuestion q10 = seedQuestion(family, topicIdByName.get("Sức khỏe thai nhi"),
            "Khám thai định kỳ nên siêu âm bao nhiêu lần là đủ?",
            "Gia đình em muốn hỏi lịch siêu âm định kỳ hợp lý trong thai kỳ là bao nhiêu lần để theo dõi thai nhi tốt nhất ạ?",
            PregnancyStage.PREGNANCY, (short) 28, null, UrgencyLevel.LOW, false, QuestionStatus.APPROVED, 1);

        CommunityQuestion q11 = seedQuestion(mother3, topicIdByName.get("Chăm sóc bé sơ sinh"),
            "Bé sơ sinh ngủ ngày cày đêm, làm sao chỉnh lại giờ giấc?",
            "Bé nhà em 1 tháng tuổi cứ ngủ suốt ban ngày rồi thức chơi cả đêm, em rất mệt, mẹ nào có kinh nghiệm chỉ em với.",
            PregnancyStage.POSTPARTUM, null, (short) 1, UrgencyLevel.NORMAL, false, QuestionStatus.APPROVED, 1);

        CommunityQuestion q12 = seedQuestion(family2, topicIdByName.get("Nuôi con bằng sữa mẹ"),
            "Mẹ bị tắc tia sữa phải xử lý thế nào tại nhà?",
            "Người nhà em bị tắc tia sữa 2 hôm nay, ngực căng cứng và đau, có cách nào xử lý tại nhà trước khi đi khám không ạ?",
            PregnancyStage.POSTPARTUM, null, (short) 2, UrgencyLevel.URGENT, false, QuestionStatus.HIDDEN, 0);

        CommunityQuestion q13 = seedQuestion(mother4, topicIdByName.get("Tâm lý & Cảm xúc"),
            "Làm sao để cân bằng công việc và chăm con nhỏ mà không kiệt sức?",
            "Em sắp đi làm lại sau nghỉ thai sản nhưng rất lo lắng không thể vừa làm việc vừa chăm bé chu đáo, mọi người tư vấn giúp em với.",
            PregnancyStage.POSTPARTUM, null, (short) 6, UrgencyLevel.NORMAL, false, QuestionStatus.LOCKED, 0);

        CommunityQuestion q14 = seedQuestion(family3, topicIdByName.get("Chăm sóc sau sinh"),
            "Sản dịch sau sinh kéo dài bao lâu là bình thường?",
            "Người nhà em sinh được 3 tuần mà sản dịch vẫn ra, không biết bao lâu thì hết là bình thường ạ?",
            PregnancyStage.POSTPARTUM, null, (short) 1, UrgencyLevel.NORMAL, false, QuestionStatus.APPROVED, 1);

        // 10 answers: 2 PENDING (feed the pending-content moderation queue), the rest APPROVED
        CommunityAnswer a7 = seedAnswer(q9.getId(), expert2,
            "Nghén nặng gây sụt cân cần được theo dõi sát, bạn nên chia nhỏ bữa ăn, ưu tiên món dễ tiêu và uống đủ nước; "
                + "nếu tiếp tục sụt cân hoặc nôn liên tục không giữ được nước, cần đến khám để được truyền dịch và hỗ trợ kịp thời.",
            true, false, AnswerStatus.APPROVED, 1);

        seedAnswer(q9.getId(), mother3,
            "Mình cũng nghén dữ lắm hồi đầu, sau đó ăn bánh quy mặn trước khi ngồi dậy buổi sáng thì đỡ hẳn.",
            false, true, AnswerStatus.PENDING, 0);

        CommunityAnswer a9 = seedAnswer(q10.getId(), expert3,
            "Số lần siêu âm cụ thể tùy phác đồ của từng bác sĩ, nhưng thông thường có 3 mốc quan trọng cần siêu âm hình thái/tầm soát dị tật "
                + "(12 tuần, 22 tuần, 32 tuần), ngoài ra bác sĩ có thể chỉ định thêm tùy tình trạng thai kỳ.",
            true, false, AnswerStatus.APPROVED, 1);

        seedAnswer(q10.getId(), family3,
            "Nhà mình đi khám đều đặn mỗi tháng một lần, bác sĩ siêu âm khi thấy cần thiết chứ không cố định số lần.",
            false, true, AnswerStatus.PENDING, 0);

        CommunityAnswer a11 = seedAnswer(q11.getId(), expert,
            "Bé sơ sinh chưa phân biệt ngày đêm là bình thường, mẹ nên tăng ánh sáng và tương tác vào ban ngày, giữ phòng tối yên tĩnh vào ban đêm, "
                + "việc này sẽ cải thiện dần trong khoảng 6-8 tuần đầu.",
            true, false, AnswerStatus.APPROVED, 1);

        CommunityAnswer a12 = seedAnswer(q11.getId(), mother4,
            "Bé nhà mình cũng vậy, mình tắm bé vào buổi tối và hạn chế ngủ quá nhiều vào chiều thì bé quen giờ giấc nhanh hơn.",
            false, true, AnswerStatus.APPROVED, 0);

        CommunityAnswer a13 = seedAnswer(q12.getId(), expert2,
            "Khi bị tắc tia sữa, nên chườm ấm trước khi cho bú, massage nhẹ nhàng theo hướng về núm vú và cho bé bú thường xuyên hơn ở bên tắc; "
                + "nếu sau 24-48 giờ không cải thiện hoặc sốt, cần đến cơ sở y tế ngay vì có thể tiến triển thành viêm tuyến vú.",
            true, false, AnswerStatus.APPROVED, 0);

        CommunityAnswer a14 = seedAnswer(q13.getId(), mother,
            "Mình cũng từng rất áp lực khi đi làm lại, sau đó thỏa thuận với công ty làm việc linh hoạt hơn và nhờ gia đình hỗ trợ buổi chiều thì đỡ hẳn.",
            false, true, AnswerStatus.APPROVED, 0);

        CommunityAnswer a15 = seedAnswer(q14.getId(), expert3,
            "Sản dịch thường kéo dài 2-6 tuần và giảm dần về lượng lẫn màu sắc; nếu sau 3 tuần vẫn ra nhiều, có mùi hôi hoặc kèm sốt thì cần tái khám ngay.",
            true, false, AnswerStatus.APPROVED, 1);

        CommunityAnswer a16 = seedAnswer(q14.getId(), family2,
            "Người nhà mình sinh xong sản dịch kéo dài gần 1 tháng mới hết hẳn, bác sĩ nói vẫn trong giới hạn bình thường.",
            false, true, AnswerStatus.APPROVED, 0);



        // Likes + bookmarks, kept consistent with each item's seeded like_count above
        seedQuestionLike(expert, q9.getId());
        seedQuestionLike(family3, q9.getId());
        seedQuestionLike(family2, q10.getId());
        seedQuestionLike(mother, q11.getId());
        seedQuestionLike(expert2, q14.getId());

        seedAnswerLike(family, a7.getId());
        seedAnswerLike(mother3, a9.getId());
        seedAnswerLike(family2, a11.getId());
        seedAnswerLike(mother4, a15.getId());
        seedAnswerLike(expert, a16.getId());

        seedBookmark(family, q9.getId());
        seedBookmark(mother3, q11.getId());
        seedBookmark(expert, q14.getId());

        // 4 content reports: 2 PENDING (feed the report-based moderation queue), 1 RESOLVED, 1 DISMISSED
        seedContentReport(family, q11.getId(), ReportTargetType.QUESTION, "OTHER",
            "Nghi ngờ một số thông tin trong câu hỏi chưa được kiểm chứng.",
            ReportStatus.PENDING, null, null);

        seedContentReport(mother4, a11.getId(), ReportTargetType.ANSWER, "UNSAFE_ADVICE",
            "Câu trả lời có thể chưa phù hợp với mọi trường hợp, cần chuyên gia xác nhận lại.",
            ReportStatus.PENDING, null, null);

        ContentReport r7 = seedContentReport(expert2, q13.getId(), ReportTargetType.QUESTION, "HARASSMENT",
            "Phần bình luận qua lại trong câu hỏi có ngôn từ gay gắt, cần rà soát lại.",
            ReportStatus.RESOLVED, moderator, Instant.now().minus(java.time.Duration.ofDays(3)));

        seedContentReport(family3, q14.getId(), ReportTargetType.QUESTION, "INACCURATE_INFORMATION",
            "Nội dung có thể gây hoang mang, đề nghị kiểm tra lại - nhưng nội dung vẫn phù hợp sau khi xem xét.",
            ReportStatus.DISMISSED, moderator, Instant.now().minus(java.time.Duration.ofDays(4)));

        // Moderation actions: one tied to the resolved report above (LOCK on q13), one proactive (HIDE on q12,
        // reportId=null per ADR-001 - UC-100 style action with no underlying user report)
        seedModerationAction(q13.getId(), ReportTargetType.QUESTION, ModerationActionType.LOCK, moderator,
            "Khóa câu hỏi do tranh cãi kéo dài giữa các bình luận, cần rà soát lại.", r7.getId());
        seedModerationAction(q12.getId(), ReportTargetType.QUESTION, ModerationActionType.HIDE, moderator,
            "Ẩn câu hỏi chủ động do nội dung nhạy cảm liên quan sức khỏe, đã trao đổi riêng với tác giả.", null);
    }
}
