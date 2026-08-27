package com.carebridge.backend.family;

import com.carebridge.backend.family.service.InviteTokenGenerator;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class InviteTokenGeneratorTest {

    private final InviteTokenGenerator generator = new InviteTokenGenerator();

    @Test
    void generate_producesUniqueTokens() {
        Set<String> tokens = new HashSet<>();
        for (int i = 0; i < 10_000; i++) {
            tokens.add(generator.generate());
        }
        assertThat(tokens).hasSize(10_000);
    }

    @Test
    void generate_tokenLengthWithinBound() {
        for (int i = 0; i < 100; i++) {
            String token = generator.generate();
            assertThat(token.length()).isLessThanOrEqualTo(64);
        }
    }

    @Test
    void generate_tokenMatchesSafeCharset() {
        for (int i = 0; i < 100; i++) {
            String token = generator.generate();
            assertThat(token).matches("^[A-Za-z0-9_-]+$");
        }
    }
}
