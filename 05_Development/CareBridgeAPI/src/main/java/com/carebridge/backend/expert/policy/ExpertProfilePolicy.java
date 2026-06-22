package com.carebridge.backend.expert.policy;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import java.util.UUID;

@Component
public class ExpertProfilePolicy {

    public void checkCanEditProfile(UUID userId, UUID expertProfileId) {
        if (!userId.equals(expertProfileId)) {
            throw new AccessDeniedException("Cannot edit others' profile");
        }
    }

    public void checkCanViewPrivateInfo(UUID viewerId, UUID targetExpertId) {
        // Only the expert themselves or admin can view private/sensitive info
        if (!viewerId.equals(targetExpertId)) {
            throw new AccessDeniedException("Cannot view private information of other experts");
        }
    }
}
