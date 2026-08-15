package com.carebridge.backend.common.dev;

import com.carebridge.backend.expert.entity.ExpertProfile;
import com.carebridge.backend.expert.repository.ExpertProfileRepository;
import com.carebridge.backend.expert.truststatus.TrustStatus;
import com.carebridge.backend.expert.verificationstatus.VerificationStatus;
import com.carebridge.backend.security.entity.User;
import com.carebridge.backend.security.rbac.Role;
import com.carebridge.backend.security.repository.UserRepository;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Opt-in synthetic development data seeder.
 * Seeds the canonical development accounts/profiles and replays the idempotent community
 * moderation demo migration after those account-owned rows are available.
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
    private final ExpertProfileRepository expertProfileRepository;
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
        seedProductionReferenceData();
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

        seedVerifiedExpertProfiles(savedUsers);
        seedCommunityModerationDemoData();
    }

    /**
     * Seeds canonical baseline reference data (health metrics, knowledge sources,
     * red flag rules, community topics, ai moderation policies, vaccination schedules,
     * care item templates) from production_reference_data.sql.
     */
    private void seedProductionReferenceData() {
        ClassPathResource resource = new ClassPathResource(
                "db/data_seed/production_reference_data.sql");
        if (!resource.exists()) {
            log.warn("Production reference data script is not available on the classpath");
            return;
        }

        try (var inputStream = resource.getInputStream()) {
            String script = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            int executed = 0;
            for (String statement : splitSqlStatements(script)) {
                if (!statement.isBlank()) {
                    jdbcTemplate.execute(statement);
                    executed++;
                }
            }
            log.info("Loaded {} statements from production reference data script", executed);
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to read production reference data script", ex);
        }
    }

    /**
     * Flyway runs before this ApplicationRunner, so account-owned demo rows are skipped on a
     * clean database. Replaying the consolidated, idempotent migration after the accounts and
     * expert profiles have been flushed makes the dev dataset available on every startup.
     */
    private void seedCommunityModerationDemoData() {
        ClassPathResource resource = new ClassPathResource(
                "db/data_seed/dev_community_moderation_demo.sql");
        if (!resource.exists()) {
            resource = new ClassPathResource(
                    "db/migration/V20260813220000__seed_ai_and_community_moderation_demo.sql");
        }
        if (!resource.exists()) {
            log.warn("Community moderation demo migration is not available on the classpath");
            return;
        }

        userRepository.flush();
        expertProfileRepository.flush();
        try (var inputStream = resource.getInputStream()) {
            String script = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            int executed = 0;
            for (String statement : splitSqlStatements(script)) {
                if (!statement.isBlank()) {
                    jdbcTemplate.execute(statement);
                    executed++;
                }
            }
            log.info("Replayed {} statements from the community moderation demo migration", executed);
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to read community moderation demo migration", ex);
        }
    }

    /** Split PostgreSQL SQL while preserving semicolons inside strings and dollar-quoted blocks. */
    private List<String> splitSqlStatements(String script) {
        List<String> statements = new java.util.ArrayList<>();
        StringBuilder current = new StringBuilder();
        String dollarTag = null;
        boolean singleQuoted = false;
        boolean doubleQuoted = false;
        boolean lineComment = false;
        boolean blockComment = false;

        for (int i = 0; i < script.length(); i++) {
            char ch = script.charAt(i);
            char next = i + 1 < script.length() ? script.charAt(i + 1) : '\0';
            if (lineComment) {
                current.append(ch);
                if (ch == '\n') {
                    lineComment = false;
                }
                continue;
            }
            if (blockComment) {
                current.append(ch);
                if (ch == '*' && next == '/') {
                    current.append(next);
                    i++;
                    blockComment = false;
                }
                continue;
            }
            if (dollarTag != null) {
                if (script.startsWith(dollarTag, i)) {
                    current.append(dollarTag);
                    i += dollarTag.length() - 1;
                    dollarTag = null;
                } else {
                    current.append(ch);
                }
                continue;
            }
            if (singleQuoted) {
                current.append(ch);
                if (ch == '\'' && next == '\'') {
                    current.append(next);
                    i++;
                } else if (ch == '\'') {
                    singleQuoted = false;
                }
                continue;
            }
            if (doubleQuoted) {
                current.append(ch);
                if (ch == '"' && next == '"') {
                    current.append(next);
                    i++;
                } else if (ch == '"') {
                    doubleQuoted = false;
                }
                continue;
            }
            if (ch == '-' && next == '-') {
                current.append(ch).append(next);
                i++;
                lineComment = true;
            } else if (ch == '/' && next == '*') {
                current.append(ch).append(next);
                i++;
                blockComment = true;
            } else if (ch == '\'') {
                current.append(ch);
                singleQuoted = true;
            } else if (ch == '"') {
                current.append(ch);
                doubleQuoted = true;
            } else if (ch == '$') {
                int end = script.indexOf('$', i + 1);
                if (end > i && script.substring(i + 1, end).matches("[A-Za-z_][A-Za-z0-9_]*|")) {
                    dollarTag = script.substring(i, end + 1);
                    current.append(dollarTag);
                    i = end;
                } else {
                    current.append(ch);
                }
            } else if (ch == ';') {
                statements.add(current.toString());
                current.setLength(0);
            } else {
                current.append(ch);
            }
        }
        if (!current.toString().isBlank()) {
            statements.add(current.toString());
        }
        return statements;
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

    private void seedVerifiedExpertProfiles(Map<String, User> savedUsers) {
        User admin = savedUsers.get("admin@carebridge.dev");

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
            expertProfileRepository.save(profile);
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
