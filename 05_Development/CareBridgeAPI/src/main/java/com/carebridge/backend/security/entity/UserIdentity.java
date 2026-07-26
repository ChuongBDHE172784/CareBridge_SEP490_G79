package com.carebridge.backend.security.entity;

import com.carebridge.backend.security.federation.FederatedProvider;
import java.time.Instant;
import java.util.UUID;
import lombok.*;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class UserIdentity {
    private UUID id;

    private User user;

    private FederatedProvider provider;

    private String providerSubject;

    private String providerEmail;

    private String providerPhone;

    private Instant createdAt;

    private Instant lastUsedAt;
}
