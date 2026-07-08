package com.carebridge.backend.common.dev;

import com.carebridge.backend.security.entity.User;
import com.carebridge.backend.security.rbac.Role;
import com.carebridge.backend.security.repository.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Seeds one test account per role when explicitly enabled.
 *
 * Credentials (same password for all accounts):
 *   Password : carebridge.dev-seed.password (default Test@1234)
 *
 * Accounts:
 *   admin@carebridge.dev      -> SYSTEM_ADMIN
 *   moderator@carebridge.dev  -> MODERATOR
 *   content@carebridge.dev    -> CONTENT_ADMIN
 *   expert@carebridge.dev     -> EXPERT
 *   partner@carebridge.dev    -> PARTNER
 *   mother@carebridge.dev     -> MOTHER
 *   family@carebridge.dev     -> FAMILY
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
        new SeedAccount("family@carebridge.dev", "Family Test", Role.FAMILY)
    );

    @Override
    public void run(ApplicationArguments args) {
        String passwordHash = passwordEncoder.encode(testPassword);
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
                .phoneVerified(false)
                .build();

            userRepository.save(user);
            created++;
        }

        if (created > 0) {
            log.info("Created {} seed accounts for roles using password {}", created, testPassword);
        } else {
            log.debug("[DevDataSeeder] All seed accounts already exist - skipped.");
        }
    }
}
