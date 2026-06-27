package com.carebridge.backend.common.dev;

import com.carebridge.backend.security.entity.User;
import com.carebridge.backend.security.rbac.Role;
import com.carebridge.backend.security.repository.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Seeds one test account per role on dev startup.
 * Only active when spring.profiles.active=dev.
 *
 * Credentials (same password for all accounts):
 *   Password : Test@1234
 *
 * Accounts:
 *   admin@carebridge.dev      → SYSTEM_ADMIN
 *   moderator@carebridge.dev  → MODERATOR
 *   content@carebridge.dev    → CONTENT_ADMIN
 *   expert@carebridge.dev     → EXPERT
 *   partner@carebridge.dev    → PARTNER
 *   mother@carebridge.dev     → MOTHER
 *   family@carebridge.dev     → FAMILY
 *
 * OTP: printed to console by MockEmailService during login.
 *   Look for: [MOCK EMAIL] To: <email>, OTP: <code>
 */
@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class DevDataSeeder implements ApplicationRunner {

    private static final String TEST_PASSWORD = "Test@1234";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    record SeedAccount(String email, String fullName, Role role) {}

    private static final List<SeedAccount> SEED_ACCOUNTS = List.of(
        new SeedAccount("admin@carebridge.dev",     "Admin Test",     Role.SYSTEM_ADMIN),
        new SeedAccount("moderator@carebridge.dev", "Moderator Test", Role.MODERATOR),
        new SeedAccount("content@carebridge.dev",   "Content Test",   Role.CONTENT_ADMIN),
        new SeedAccount("expert@carebridge.dev",    "Expert Test",    Role.EXPERT),
        new SeedAccount("partner@carebridge.dev",   "Partner Test",   Role.PARTNER),
        new SeedAccount("mother@carebridge.dev",    "Mother Test",    Role.MOTHER),
        new SeedAccount("family@carebridge.dev",    "Family Test",    Role.FAMILY)
    );

    @Override
    public void run(ApplicationArguments args) {
        String passwordHash = passwordEncoder.encode(TEST_PASSWORD);
        int created = 0;

        for (SeedAccount seed : SEED_ACCOUNTS) {
            if (userRepository.existsByEmail(seed.email())) {
                continue;
            }
            User user = User.builder()
                .email(seed.email())
                .name(seed.fullName())
                .passwordHash(passwordHash)
                .role(seed.role())
                .enabled(true)
                .locked(false)
                .emailVerified(true)
                .phoneVerified(false)
                .build();
            userRepository.save(user);
            created++;
        }

        if (created > 0) {
            log.info("");
            log.info("╔══════════════════════════════════════════════════════════╗");
            log.info("║           CareBridge — Dev Seed Accounts Created          ║");
            log.info("╠══════════════════════════════════════════════════════════╣");
            log.info("║  Password (all accounts) : Test@1234                      ║");
            log.info("║  OTP: check console for [MOCK EMAIL] To: <email>, OTP:... ║");
            log.info("╠══════════════════════════════════════════════════════════╣");
            log.info("║  admin@carebridge.dev      → SYSTEM_ADMIN                 ║");
            log.info("║  moderator@carebridge.dev  → MODERATOR                    ║");
            log.info("║  content@carebridge.dev    → CONTENT_ADMIN                ║");
            log.info("║  expert@carebridge.dev     → EXPERT                       ║");
            log.info("║  partner@carebridge.dev    → PARTNER                      ║");
            log.info("║  mother@carebridge.dev     → MOTHER                       ║");
            log.info("║  family@carebridge.dev     → FAMILY                       ║");
            log.info("╚══════════════════════════════════════════════════════════╝");
            log.info("");
        } else {
            log.debug("[DevDataSeeder] All seed accounts already exist — skipped.");
        }
    }
}
