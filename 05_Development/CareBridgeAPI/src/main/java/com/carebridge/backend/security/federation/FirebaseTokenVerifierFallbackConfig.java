package com.carebridge.backend.security.federation;

import com.carebridge.backend.security.exception.FederatedAuthException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FirebaseTokenVerifierFallbackConfig {
    @Bean
    @ConditionalOnMissingBean(FirebaseTokenVerifier.class)
    FirebaseTokenVerifier unavailableFirebaseTokenVerifier() {
        return idToken -> { throw FederatedAuthException.unavailable(); };
    }
}
