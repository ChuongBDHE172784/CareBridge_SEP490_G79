package com.carebridge.backend.expert.policy;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import java.util.UUID;

@Component
public class ReviewPolicy {

    public void checkCanCreateReview(UUID motherId, UUID expertId, UUID bookingId) {
        // Mother can only create review for their own completed consultation
        // Additional logic will be in service layer (check booking ownership, completion status)
        // This is a placeholder for policy enforcement
    }

    public void checkCanViewReview(UUID viewerId, UUID reviewOwnerId) {
        // Reviews are public, but editing/deleting requires ownership
    }
}
