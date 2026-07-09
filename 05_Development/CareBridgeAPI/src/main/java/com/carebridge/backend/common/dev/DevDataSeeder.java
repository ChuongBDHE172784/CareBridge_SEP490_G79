package com.carebridge.backend.common.dev;

import com.carebridge.backend.baby.entity.BabyProfile;
import com.carebridge.backend.baby.entity.BabyProfileStatus;
import com.carebridge.backend.baby.entity.Gender;
import com.carebridge.backend.baby.repository.BabyProfileRepository;
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
}
