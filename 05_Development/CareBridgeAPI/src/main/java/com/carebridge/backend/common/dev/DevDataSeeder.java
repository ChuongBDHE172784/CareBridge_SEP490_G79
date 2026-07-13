package com.carebridge.backend.common.dev;

import com.carebridge.backend.baby.entity.BabyProfile;
import com.carebridge.backend.baby.entity.BabyProfileStatus;
import com.carebridge.backend.baby.entity.Gender;
import com.carebridge.backend.baby.repository.BabyProfileRepository;
import com.carebridge.backend.community.entity.AnswerStatus;
import com.carebridge.backend.community.entity.CommunityAnswer;
import com.carebridge.backend.community.entity.CommunityAnswerLike;
import com.carebridge.backend.community.entity.CommunityBookmark;
import com.carebridge.backend.community.entity.CommunityProfile;
import com.carebridge.backend.community.entity.CommunityQuestion;
import com.carebridge.backend.community.entity.CommunityQuestionLike;
import com.carebridge.backend.community.entity.CommunityTopic;
import com.carebridge.backend.community.entity.PregnancyStage;
import com.carebridge.backend.community.entity.QuestionStatus;
import com.carebridge.backend.community.entity.UrgencyLevel;
import com.carebridge.backend.community.repository.CommunityAnswerLikeRepository;
import com.carebridge.backend.community.repository.CommunityAnswerRepository;
import com.carebridge.backend.community.repository.CommunityBookmarkRepository;
import com.carebridge.backend.community.repository.CommunityProfileRepository;
import com.carebridge.backend.community.repository.CommunityQuestionLikeRepository;
import com.carebridge.backend.community.repository.CommunityQuestionRepository;
import com.carebridge.backend.community.repository.CommunityTopicRepository;
import com.carebridge.backend.content.entity.ContentReport;
import com.carebridge.backend.content.entity.ModerationAction;
import com.carebridge.backend.content.entity.ModerationActionType;
import com.carebridge.backend.content.entity.ReportStatus;
import com.carebridge.backend.content.entity.ReportTargetType;
import com.carebridge.backend.content.repository.ContentReportRepository;
import com.carebridge.backend.content.repository.ModerationActionRepository;
import com.carebridge.backend.expert.entity.ExpertProfile;
import com.carebridge.backend.expert.repository.ExpertProfileRepository;
import com.carebridge.backend.expert.verificationstatus.VerificationStatus;
import com.carebridge.backend.expertavailability.availabilitystatus.AvailabilityStatus;
import com.carebridge.backend.expertavailability.entity.ExpertAvailability;
import com.carebridge.backend.expertavailability.repository.ExpertAvailabilityRepository;
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
import com.carebridge.backend.journey.repository.MotherJourneyRepository;
import com.carebridge.backend.security.entity.User;
import com.carebridge.backend.security.rbac.Role;
import com.carebridge.backend.security.repository.UserRepository;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seeds test accounts per role when explicitly enabled.
 *
 * Credentials (same password for all accounts):
 *   Password : carebridge.dev-seed.password (default Test@1234)
 *
 * Base accounts (one per role):
 *   admin@carebridge.dev      -> SYSTEM_ADMIN
 *   moderator@carebridge.dev  -> MODERATOR
 *   content@carebridge.dev    -> CONTENT_ADMIN
 *   expert@carebridge.dev     -> EXPERT
 *   partner@carebridge.dev    -> PARTNER
 *   mother@carebridge.dev     -> MOTHER
 *   family@carebridge.dev     -> FAMILY
 *
 * Extra fully-verified/accepted accounts (two more each for MOTHER, FAMILY, EXPERT),
 * used for multi-account manual test scenarios (e.g. two mothers each paired with
 * their own accepted family member, plus two admin-approved experts). Note:
 * "mother2@carebridge.dev" is skipped - it already exists as a teammate's manual test
 * account in the shared dev database, so seeding continues from mother3/mother4:
 *   mother3@carebridge.dev / mother4@carebridge.dev   -> MOTHER  (own mother_journeys + baby_profiles rows)
 *   family2@carebridge.dev / family3@carebridge.dev   -> FAMILY  (ACCEPTED care_group_members of mother3/mother4's group)
 *   expert2@carebridge.dev / expert3@carebridge.dev   -> EXPERT  (expert_profiles APPROVED + expert_credentials APPROVED + availability slot)
 *
 * OTP: dev uses MockEmailService logs; supabase uses configured SMTP.
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "carebridge.dev-seed", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class DevDataSeeder implements ApplicationRunner {

    private static final String DEFAULT_TEST_PASSWORD = "Test@1234";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final MotherJourneyRepository motherJourneyRepository;
    private final BabyProfileRepository babyProfileRepository;
    private final CareGroupRepository careGroupRepository;
    private final CareGroupMemberRepository careGroupMemberRepository;
    private final ExpertProfileRepository expertProfileRepository;
    private final ExpertCredentialRepository expertCredentialRepository;
    private final ExpertAvailabilityRepository expertAvailabilityRepository;
    private final CommunityTopicRepository communityTopicRepository;
    private final CommunityQuestionRepository communityQuestionRepository;
    private final CommunityAnswerRepository communityAnswerRepository;
    private final CommunityProfileRepository communityProfileRepository;
    private final CommunityQuestionLikeRepository communityQuestionLikeRepository;
    private final CommunityAnswerLikeRepository communityAnswerLikeRepository;
    private final CommunityBookmarkRepository communityBookmarkRepository;
    private final ContentReportRepository contentReportRepository;
    private final ModerationActionRepository moderationActionRepository;
    private final JdbcTemplate jdbcTemplate;

    @Value("${carebridge.dev-seed.password:" + DEFAULT_TEST_PASSWORD + "}")
    private String testPassword;

    record SeedAccount(String email, String fullName, Role role) {}

    private static final List<SeedAccount> SEED_ACCOUNTS = List.of(
        new SeedAccount("admin@carebridge.dev", "Admin Test", Role.SYSTEM_ADMIN),
        new SeedAccount("moderator@carebridge.dev", "Moderator Test", Role.MODERATOR),
        new SeedAccount("content@carebridge.dev", "Content Test", Role.CONTENT_ADMIN),
        new SeedAccount("expert@carebridge.dev", "Expert Test", Role.EXPERT),
        new SeedAccount("partner@carebridge.dev", "Partner Test", Role.PARTNER),
        new SeedAccount("mother@carebridge.dev", "Mother Test", Role.MOTHER),
        new SeedAccount("family@carebridge.dev", "Family Test", Role.FAMILY),
        new SeedAccount("mebau@carebridge.dev", "Mẹ Bầu Mới", Role.MOTHER),
        new SeedAccount("mother3@carebridge.dev", "Mother Test 3", Role.MOTHER),
        new SeedAccount("mother4@carebridge.dev", "Mother Test 4", Role.MOTHER),
        new SeedAccount("family2@carebridge.dev", "Family Test 2", Role.FAMILY),
        new SeedAccount("family3@carebridge.dev", "Family Test 3", Role.FAMILY),
        new SeedAccount("expert2@carebridge.dev", "Expert Test 2", Role.EXPERT),
        new SeedAccount("expert3@carebridge.dev", "Expert Test 3", Role.EXPERT)
    );

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
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
            log.info("Created/updated {} seed accounts for roles using password {}", created, testPassword);
        } else {
            log.debug("[DevDataSeeder] All seed accounts already exist - skipped.");
        }

        seedVerifiedProfileData(savedUsers);
        seedCommunitySampleData(savedUsers);
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

        BabyProfile mother4Baby = seedBabyProfile(
            savedUsers.get("mother4@carebridge.dev"), mother4Journey);

        seedAcceptedCareGroup(
            savedUsers.get("mother3@carebridge.dev"), savedUsers.get("family2@carebridge.dev"),
            mother3Journey.getId(), null, "Mother Test 3's Care Group");
        seedAcceptedCareGroup(
            savedUsers.get("mother4@carebridge.dev"), savedUsers.get("family3@carebridge.dev"),
            mother4Journey.getId(), mother4Baby.getId(), "Mother Test 4's Care Group");

        seedVerifiedExpert(savedUsers.get("expert2@carebridge.dev"), admin,
            "Sản khoa", "Bác sĩ Sản khoa", 8, "Bệnh viện Từ Dũ");
        seedVerifiedExpert(savedUsers.get("expert3@carebridge.dev"), admin,
            "Nhi khoa", "Bác sĩ Nhi khoa", 6, "Bệnh viện Nhi Đồng 1");
    }

    private MotherJourney seedMotherJourney(User mother, JourneyType type, LocalDate deliveryDate) {
        List<MotherJourney> existing = motherJourneyRepository
            .findByOwnerUserIdAndJourneyTypeAndStatusOrderByCreatedAtAsc(mother.getId(), type, JourneyStatus.ACTIVE);
        if (!existing.isEmpty()) {
            return existing.get(0);
        }
        return motherJourneyRepository.save(MotherJourney.builder()
            .ownerUserId(mother.getId())
            .journeyType(type)
            .startDate(LocalDate.now().minusMonths(3))
            .lastMenstrualDate(LocalDate.now().minusMonths(5))
            .estimatedDueDate(LocalDate.now().plusMonths(4))
            .deliveryDate(deliveryDate)
            .status(JourneyStatus.ACTIVE)
            .notes("Seeded verified test journey")
            .build());
    }

    private BabyProfile seedBabyProfile(User mother, MotherJourney journey) {
        List<BabyProfile> existing = babyProfileRepository
            .findByOwnerUserIdAndStatusOrderByCreatedAtAsc(mother.getId(), BabyProfileStatus.ACTIVE);
        if (!existing.isEmpty()) {
            return existing.get(0);
        }
        return babyProfileRepository.save(BabyProfile.builder()
            .ownerUserId(mother.getId())
            .relatedJourneyId(journey.getId())
            .nickname("Bé " + mother.getName())
            .birthDate(journey.getStartDate().plusMonths(1))
            .gender(Gender.FEMALE)
            .status(BabyProfileStatus.ACTIVE)
            .build());
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

        careGroupMemberRepository.findByCareGroupIdAndUserId(group.getId(), familyMember.getId())
            .orElseGet(() -> careGroupMemberRepository.save(CareGroupMember.builder()
                .careGroupId(group.getId())
                .userId(familyMember.getId())
                .memberRole(GroupMemberRole.MEMBER)
                .inviteStatus(InviteStatus.ACCEPTED)
                .joinedAt(Instant.now())
                .build()));
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

        if (expertAvailabilityRepository.findByExpertProfileId(profile.getExpertProfileId()).isEmpty()) {
            expertAvailabilityRepository.save(ExpertAvailability.builder()
                .expertProfileId(profile.getExpertProfileId())
                .startAt(Instant.now().plus(java.time.Duration.ofDays(1)))
                .endAt(Instant.now().plus(java.time.Duration.ofDays(1)).plusSeconds(3600))
                .channelType("VIDEO_CALL")
                .status(AvailabilityStatus.AVAILABLE)
                .build());
        }
    }

    /**
     * The live expert_profiles table carries extra legacy NOT NULL columns
     * (display_name, years_of_experience, consultation_fee_vnd, ...) left over from a remote
     * migration that are not mapped by ExpertProfile.java. A plain JPA save() on a fresh row
     * fails on those columns, so the very first insert is done via JDBC, letting Postgres
     * defaults fill the unmapped columns; every read/update afterwards goes through JPA as usual.
     */
    private ExpertProfile insertExpertProfileRow(User expertUser, User admin, String specialty,
                                                  String professionalTitle, int experienceYears, String workplace) {
        jdbcTemplate.update(
            "INSERT INTO expert_profiles (user_id, display_name, years_of_experience, consultation_fee_vnd, "
                + "specialty, professional_title, experience_years, workplace, consultation_scope, "
                + "verification_status, verified_at, verified_by) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            expertUser.getId(), expertUser.getName(), experienceYears, 0L,
            specialty, professionalTitle, experienceYears, workplace, "Tư vấn thai sản và chăm sóc mẹ bé",
            VerificationStatus.APPROVED.name(), Timestamp.valueOf(LocalDateTime.now()), admin.getId());

        return expertProfileRepository.findByUserId(expertUser.getId())
            .orElseThrow(() -> new IllegalStateException(
                "Failed to read back seeded expert_profiles row for " + expertUser.getEmail()));
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
            PregnancyStage.BABY_CARE, null, (short) 5, UrgencyLevel.NORMAL, false, QuestionStatus.APPROVED, 2);

        CommunityQuestion q5 = seedQuestion(mother, topicIdByName.get("Tâm lý & Cảm xúc"),
            "Mang thai tuần 10 hay khóc vô cớ, có phải trầm cảm thai kỳ không?",
            "Dạo này em hay xúc động và khóc không rõ lý do, không biết có phải dấu hiệu trầm cảm thai kỳ không ạ?",
            PregnancyStage.PREGNANCY, (short) 10, null, UrgencyLevel.NORMAL, false, QuestionStatus.HIDDEN, 0);

        CommunityQuestion q6 = seedQuestion(family2, topicIdByName.get("Chăm sóc bé sơ sinh"),
            "Có nên dùng phấn rôm cho bé sơ sinh 1 tháng tuổi không?",
            "Nhà em nghe nói phấn rôm không tốt cho bé sơ sinh, mọi người cho em xin ý kiến với ạ.",
            PregnancyStage.BABY_CARE, null, (short) 1, UrgencyLevel.LOW, false, QuestionStatus.LOCKED, 1);

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

        // 2 public community profiles, exercising the anonymous/display-name resolver
        seedCommunityProfile(mother3, "Mẹ Bé Sâu",
            "Mẹ bỉm sữa đang mang thai lần 2, thích chia sẻ kinh nghiệm dinh dưỡng.",
            "PREGNANCY", true, "TP. Hồ Chí Minh");
        seedCommunityProfile(family2, "Dì Ba",
            "Người thân đồng hành cùng mẹ và bé, quan tâm chăm sóc trẻ sơ sinh.",
            "BABY_CARE", true, "Hà Nội");

        // Likes + bookmarks, kept consistent with each item's seeded like_count above
        seedQuestionLike(family, q3.getId());
        seedQuestionLike(expert, q4.getId());
        seedQuestionLike(mother3, q4.getId());

        seedAnswerLike(mother, a1.getId());
        seedAnswerLike(family, a3.getId());
        seedAnswerLike(mother4, a6.getId());

        seedBookmark(family, q3.getId());
        seedBookmark(mother3, q4.getId());

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

    private void seedCommunityProfile(UUID userId, String displayName, String bio, String interestStage,
            boolean visible, String region) {
        if (communityProfileRepository.existsByUserId(userId)) {
            return;
        }
        Instant now = Instant.now();
        communityProfileRepository.save(CommunityProfile.builder()
            .userId(userId)
            .displayName(displayName)
            .bio(bio)
            .interestStage(interestStage)
            .visible(visible)
            .region(region)
            .createdAt(now)
            .updatedAt(now)
            .build());
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
}
