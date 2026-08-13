package com.carebridge.backend.common.dev;

import com.carebridge.backend.baby.entity.BabyProfile;
import com.carebridge.backend.baby.entity.BabyProfileStatus;
import com.carebridge.backend.baby.entity.Gender;
import com.carebridge.backend.baby.repository.BabyProfileRepository;
import com.carebridge.backend.expert.entity.ExpertProfile;
import com.carebridge.backend.expert.repository.ExpertProfileRepository;
import com.carebridge.backend.expert.truststatus.TrustStatus;
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
import com.carebridge.backend.journey.entity.JourneyDateSource;
import com.carebridge.backend.journey.entity.JourneyStatus;
import com.carebridge.backend.journey.entity.JourneyType;
import com.carebridge.backend.journey.entity.MotherJourney;
import com.carebridge.backend.journey.entity.PregnancyOutcomeEvidence;
import com.carebridge.backend.journey.entity.PregnancyOutcomeType;
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
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
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
 * Opt-in synthetic development data seeder.
 * Only seeds user accounts and basic user profile data (MotherJourney,
 * BabyProfile, CareGroup, ExpertProfile).
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
    private final ExpertAvailabilityRepository expertAvailabilityRepository;
    private final JdbcTemplate jdbcTemplate;

    @Value("${carebridge.dev-seed.password:" + DEFAULT_TEST_PASSWORD + "}")
    private String testPassword;

    record SeedAccount(
            String email,
            String fullName,
            Role role,
            String phone,
            LocalDate dateOfBirth,
            String area) {
    }

    private static final List<SeedAccount> SEED_ACCOUNTS = List.of(
            new SeedAccount("admin@carebridge.dev", "System Admin", Role.SYSTEM_ADMIN,
                    "+84901000001", LocalDate.of(1990, 1, 10), "TP Hồ Chí Minh"),
            new SeedAccount("moderator@carebridge.dev", "Moderator", Role.MODERATOR,
                    "+84901000002", LocalDate.of(1992, 5, 18), "TP Hồ Chí Minh"),
            new SeedAccount("content@carebridge.dev", "Content Admin", Role.CONTENT_ADMIN,
                    "+84901000003", LocalDate.of(1991, 9, 22), "TP Hồ Chí Minh"),
            new SeedAccount("expert@carebridge.dev", "BS Đỗ Hải Long", Role.EXPERT,
                    "+84902000001", LocalDate.of(1982, 3, 12), "TP Hồ Chí Minh"),
            new SeedAccount("mother@carebridge.dev", "Mẹ Bầu 1", Role.MOTHER,
                    "+84911000001", LocalDate.of(1995, 2, 14), "TP Hồ Chí Minh"),
            new SeedAccount("family@carebridge.dev", "Chồng Mẹ Bầu 1", Role.FAMILY,
                    "+84912000001", LocalDate.of(1992, 7, 9), "TP Hồ Chí Minh"),
            new SeedAccount("mother2@carebridge.dev", "Mẹ Bầu 2", Role.MOTHER,
                    "+84911000002", LocalDate.of(1994, 6, 20), "Hà Nội"),
            new SeedAccount("mother3@carebridge.dev", "Mẹ bầu 3", Role.MOTHER,
                    "+84911000003", LocalDate.of(1996, 11, 3), "Đà Nẵng"),
            new SeedAccount("mother4@carebridge.dev", "Mẹ bầu 4", Role.MOTHER,
                    "+84911000004", LocalDate.of(1993, 4, 27), "TP Hồ Chí Minh"),
            new SeedAccount("mother5@carebridge.dev", "Mẹ bầu 5", Role.MOTHER,
                    "+84911000005", LocalDate.of(1997, 8, 16), "Cần Thơ"),
            new SeedAccount("mother6@carebridge.dev", "Mẹ bầu 6", Role.MOTHER,
                    "+84911000006", LocalDate.of(1995, 12, 8), "Đồng Nai"),
            new SeedAccount("family2@carebridge.dev", "Chồng Mẹ Bầu 2", Role.FAMILY,
                    "+84912000002", LocalDate.of(1991, 1, 25), "Hà Nội"),
            new SeedAccount("family3@carebridge.dev", "Chồng Mẹ Bầu 3", Role.FAMILY,
                    "+84912000003", LocalDate.of(1993, 10, 11), "Đà Nẵng"),
            new SeedAccount("family4@carebridge.dev", "Chồng Mẹ Bầu 4", Role.FAMILY,
                    "+84912000004", LocalDate.of(1990, 3, 30), "TP Hồ Chí Minh"),
            new SeedAccount("family5@carebridge.dev", "Chồng Mẹ Bầu 5", Role.FAMILY,
                    "+84912000005", LocalDate.of(1994, 5, 19), "Cần Thơ"),
            new SeedAccount("family6@carebridge.dev", "Chồng Mẹ Bầu 6", Role.FAMILY,
                    "+84912000006", LocalDate.of(1992, 12, 2), "Đồng Nai"),
            new SeedAccount("expert2@carebridge.dev", "BS Trần Thị Thu Nga", Role.EXPERT,
                    "+84902000002", LocalDate.of(1985, 6, 15), "TP Hồ Chí Minh"),
            new SeedAccount("expert3@carebridge.dev", "BS Trần Văn Hoàng", Role.EXPERT,
                    "+84902000003", LocalDate.of(1987, 10, 8), "TP Hồ Chí Minh"),
            new SeedAccount("expert4@carebridge.dev", "BS Nguyễn Văn Minh", Role.EXPERT,
                    "+84902000004", LocalDate.of(1980, 4, 21), "Hà Nội"),
            new SeedAccount("expert5@carebridge.dev", "BS Nguyễn Thị Lan", Role.EXPERT,
                    "+84902000005", LocalDate.of(1988, 1, 17), "Đà Nẵng"),
            new SeedAccount("expert6@carebridge.dev", "BS Lê Văn Bình", Role.EXPERT,
                    "+84902000006", LocalDate.of(1984, 9, 5), "Cần Thơ"));

    private static final Set<String> SEED_EMAILS = SEED_ACCOUNTS.stream()
            .map(SeedAccount::email)
            .collect(java.util.stream.Collectors.toUnmodifiableSet());

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        validateSeedPassword(testPassword);
        String passwordHash = passwordEncoder.encode(testPassword);
        retireObsoleteSeedAccounts();

        Map<String, User> savedUsers = new HashMap<>();
        int changed = 0;

        for (SeedAccount seed : SEED_ACCOUNTS) {
            var existing = userRepository.findByEmail(seed.email());
            if (existing.isPresent()) {
                User user = existing.get();
                boolean accountChanged = synchronizeSeedAccount(user, seed);
                if (!passwordEncoder.matches(testPassword, user.getPasswordHash())) {
                    user.setPasswordHash(passwordHash);
                    accountChanged = true;
                    log.info("Reset password for existing seed account: {}", seed.email());
                }
                if (accountChanged) {
                    user = userRepository.save(user);
                    changed++;
                }
                savedUsers.put(seed.email(), user);
                continue;
            }

            User user = User.builder()
                    .email(seed.email())
                    .name(seed.fullName())
                    .displayName(seed.fullName())
                    .phone(seed.phone())
                    .dateOfBirth(seed.dateOfBirth())
                    .area(seed.area())
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
            changed++;
        }

        if (changed > 0) {
            log.info("Created/updated {} synthetic dev seed accounts", changed);
        } else {
            log.debug("[DevDataSeeder] All seed accounts already exist - skipped.");
        }

        seedVerifiedProfileData(savedUsers);
    }

    private boolean synchronizeSeedAccount(User user, SeedAccount seed) {
        boolean changed = false;
        changed |= setIfDifferent(user.getName(), seed.fullName(), user::setName);
        changed |= setIfDifferent(user.getDisplayName(), seed.fullName(), user::setDisplayName);
        if (user.getPerson() != null
                && !java.util.Objects.equals(user.getPerson().getDisplayName(), seed.fullName())) {
            user.getPerson().setDisplayName(seed.fullName());
            changed = true;
        }
        changed |= setIfDifferent(user.getPhone(), seed.phone(), user::setPhone);
        changed |= setIfDifferent(user.getDateOfBirth(), seed.dateOfBirth(), user::setDateOfBirth);
        changed |= setIfDifferent(user.getArea(), seed.area(), user::setArea);

        if (user.getRole() != seed.role()) {
            user.setRole(seed.role());
            changed = true;
        }
        if (!user.isEnabled()) {
            user.setEnabled(true);
            changed = true;
        }
        if (user.isLocked()) {
            user.setLocked(false);
            user.setLockedAt(null);
            user.setLockType(null);
            user.setLockReason(null);
            user.setLockedBy(null);
            user.setLockEpisodeId(null);
            changed = true;
        }
        if (!"ACTIVE".equals(user.getAccountStatus())) {
            user.setAccountStatus("ACTIVE");
            user.setDeactivatedAt(null);
            user.setDeactivationReason(null);
            user.setDeactivatedBy(null);
            changed = true;
        }
        if (!Boolean.TRUE.equals(user.getEmailVerified())) {
            user.setEmailVerified(true);
            changed = true;
        }
        if (!Boolean.TRUE.equals(user.getPhoneVerified())) {
            user.setPhoneVerified(true);
            changed = true;
        }
        return changed;
    }

    private <T> boolean setIfDifferent(T current, T expected, java.util.function.Consumer<T> setter) {
        if (java.util.Objects.equals(current, expected)) {
            return false;
        }
        setter.accept(expected);
        return true;
    }

    private void retireObsoleteSeedAccounts() {
        userRepository.findAll().stream()
                .filter(user -> user.getEmail() != null)
                .filter(user -> user.getEmail().endsWith("@carebridge.dev"))
                .filter(user -> !SEED_EMAILS.contains(user.getEmail()))
                .toList()
                .forEach(user -> {
                    String retiredEmail = user.getEmail();
                    if (!hasUserReferences(user.getId())) {
                        userRepository.delete(user);
                        log.info("Removed obsolete unreferenced dev seed account: {}", retiredEmail);
                        return;
                    }

                    user.setEmail("retired+" + user.getId() + "@invalid.carebridge.dev");
                    user.setPhone(null);
                    user.setEnabled(false);
                    user.setLocked(false);
                    user.setAccountStatus("DEACTIVATED");
                    user.setDeactivatedAt(Instant.now());
                    user.setDeactivationReason("Retired obsolete synthetic dev seed account");
                    userRepository.save(user);
                    log.warn("Retired referenced dev seed account instead of deleting it: {}", retiredEmail);
                });
    }

    private boolean hasUserReferences(UUID userId) {
        List<Map<String, Object>> foreignKeys = jdbcTemplate.queryForList("""
                SELECT tc.table_schema, tc.table_name, kcu.column_name
                  FROM information_schema.table_constraints tc
                  JOIN information_schema.key_column_usage kcu
                    ON tc.constraint_name = kcu.constraint_name
                   AND tc.constraint_schema = kcu.constraint_schema
                  JOIN information_schema.referential_constraints rc
                    ON tc.constraint_name = rc.constraint_name
                   AND tc.constraint_schema = rc.constraint_schema
                  JOIN information_schema.constraint_column_usage ccu
                    ON rc.unique_constraint_name = ccu.constraint_name
                   AND rc.unique_constraint_schema = ccu.constraint_schema
                 WHERE tc.constraint_type = 'FOREIGN KEY'
                   AND ccu.table_schema = current_schema()
                   AND ccu.table_name = 'users'
                   AND ccu.column_name = 'user_id'
                """);

        return foreignKeys.stream().anyMatch(foreignKey -> {
            String schema = quoteIdentifier(foreignKey.get("table_schema").toString());
            String table = quoteIdentifier(foreignKey.get("table_name").toString());
            String column = quoteIdentifier(foreignKey.get("column_name").toString());
            Boolean referenced = jdbcTemplate.queryForObject(
                    "SELECT EXISTS (SELECT 1 FROM " + schema + "." + table
                            + " WHERE " + column + " = ?)",
                    Boolean.class,
                    userId);
            return Boolean.TRUE.equals(referenced);
        });
    }

    private String quoteIdentifier(String identifier) {
        return '"' + identifier.replace("\"", "\"\"") + '"';
    }

    static void validateSeedPassword(String candidate) {
        if (candidate == null || candidate.isBlank()) {
            throw new IllegalStateException(
                    "Dev seed requires a non-blank CAREBRIDGE_DEV_SEED_PASSWORD");
        }
    }

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

        seedVerifiedExpert(savedUsers.get("expert@carebridge.dev"), admin,
                "Sản khoa", "BS.CKII", 15, "Bệnh viện Từ Dũ",
                "Theo dõi thai kỳ nguy cơ cao, tư vấn tiền sản và chăm sóc sau sinh.", 350_000L);
        seedVerifiedExpert(savedUsers.get("expert2@carebridge.dev"), admin,
                "Sản khoa", "Bác sĩ", 8, "Bệnh viện Từ Dũ",
                "Tư vấn sức khỏe thai kỳ, chuẩn bị sinh và phục hồi sau sinh.", 280_000L);
        seedVerifiedExpert(savedUsers.get("expert3@carebridge.dev"), admin,
                "Nhi khoa", "Bác sĩ", 6, "Bệnh viện Nhi Đồng 1",
                "Tư vấn chăm sóc trẻ sơ sinh, dinh dưỡng và các bệnh lý nhi khoa thường gặp.", 250_000L);
        seedVerifiedExpert(savedUsers.get("expert4@carebridge.dev"), admin,
                "Dinh dưỡng", "Thạc sĩ - Bác sĩ", 12, "Bệnh viện Bạch Mai",
                "Xây dựng chế độ dinh dưỡng cho mẹ mang thai, sau sinh và trẻ nhỏ.", 320_000L);
        seedVerifiedExpert(savedUsers.get("expert5@carebridge.dev"), admin,
                "Tâm lý", "Chuyên gia Tâm lý", 9, "Bệnh viện Phụ sản - Nhi Đà Nẵng",
                "Hỗ trợ tâm lý thai kỳ, phòng ngừa trầm cảm sau sinh và tư vấn gia đình.", 300_000L);
        seedVerifiedExpert(savedUsers.get("expert6@carebridge.dev"), admin,
                "Nhi khoa", "BS.CKI", 10, "Bệnh viện Nhi đồng Cần Thơ",
                "Tư vấn chăm sóc, tiêm chủng và theo dõi phát triển trẻ sơ sinh, trẻ nhỏ.", 270_000L);
    }

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
        if (existing.stream().anyMatch(baby -> nickname.equals(baby.getNickname())))
            return;

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
                .findByOwnerUserIdAndJourneyTypeAndStatusOrderByCreatedAtAsc(mother.getId(), type,
                        JourneyStatus.ACTIVE);
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
            if (baby.getBirthWeightKg() == null)
                baby.setBirthWeightKg(new BigDecimal("3.40"));
            if (baby.getBirthLengthCm() == null)
                baby.setBirthLengthCm(new BigDecimal("50.0"));
            return babyProfileRepository.saveAndFlush(baby);
        }
        return babyProfileRepository.saveAndFlush(BabyProfile.builder()
                .ownerUserId(mother.getId())
                .nickname("Bé " + mother.getName())
                .birthDate(journey.getDeliveryDate() != null
                        ? journey.getDeliveryDate()
                        : journey.getStartDate().plusMonths(1))
                .gender(Gender.FEMALE)
                .birthWeightKg(new BigDecimal("3.40"))
                .birthLengthCm(new BigDecimal("50.0"))
                .status(BabyProfileStatus.ACTIVE)
                .active(true)
                .build());
    }

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
            String professionalTitle, int experienceYears, String workplace,
            String consultationScope, long consultationFeeVnd) {
        ExpertProfile profile = expertProfileRepository.findByUserId(expertUser.getId())
                .orElseGet(() -> insertExpertProfileRow(expertUser, admin, specialty, professionalTitle,
                        experienceYears, workplace, consultationScope, consultationFeeVnd));

        boolean profileChanged = false;
        profileChanged |= setIfDifferent(profile.getSpecialty(), specialty, profile::setSpecialty);
        profileChanged |= setIfDifferent(profile.getProfessionalTitle(), professionalTitle,
                profile::setProfessionalTitle);
        profileChanged |= setIfDifferent(profile.getExperienceYears(), experienceYears,
                profile::setExperienceYears);
        profileChanged |= setIfDifferent(profile.getWorkplace(), workplace, profile::setWorkplace);
        profileChanged |= setIfDifferent(profile.getConsultationScope(), consultationScope,
                profile::setConsultationScope);
        profileChanged |= setIfDifferent(profile.getConsultationFeeVnd(), consultationFeeVnd,
                profile::setConsultationFeeVnd);
        profileChanged |= setIfDifferent(profile.getTrustStatus(), TrustStatus.ACTIVE,
                profile::setTrustStatus);

        if (profile.getVerificationStatus() != VerificationStatus.APPROVED
                || profile.getVerifiedAt() == null
                || !admin.getId().equals(profile.getVerifiedBy())) {
            profile.setVerificationStatus(VerificationStatus.APPROVED);
            profile.setVerifiedAt(LocalDateTime.now());
            profile.setVerifiedBy(admin.getId());
            profileChanged = true;
        }
        if (profileChanged) {
            profile = expertProfileRepository.save(profile);
        }

        if (!expertCredentialRepository.existsByExpertProfileIdAndCredentialType(profile.getExpertProfileId(),
                "MEDICAL_LICENSE")) {
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

    private ExpertProfile insertExpertProfileRow(User expertUser, User admin, String specialty,
            String professionalTitle, int experienceYears, String workplace,
            String consultationScope, long consultationFeeVnd) {
        return expertProfileRepository.save(ExpertProfile.builder()
                .expertProfileId(expertUser.getId())
                .specialty(specialty)
                .professionalTitle(professionalTitle)
                .experienceYears(experienceYears)
                .workplace(workplace)
                .consultationScope(consultationScope)
                .consultationFeeVnd(consultationFeeVnd)
                .verificationStatus(VerificationStatus.APPROVED)
                .trustStatus(TrustStatus.ACTIVE)
                .verifiedAt(LocalDateTime.now())
                .verifiedBy(admin.getId())
                .build());
    }
}
