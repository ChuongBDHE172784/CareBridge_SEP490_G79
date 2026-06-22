package com.carebridge.backend.expert.policy;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import java.util.UUID;

@Component
public class AvailabilityPolicy {

    public void checkCanEditAvailability(UUID userId, UUID expertProfileId) {
        // User must own the expert profile to edit availability
        if (!userId.equals(expertProfileId)) {
            throw new AccessDeniedException("Cannot edit availability for other experts");
        }
    }
}
