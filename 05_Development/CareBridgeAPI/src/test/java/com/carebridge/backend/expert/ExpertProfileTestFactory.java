package com.carebridge.backend.expert;

import com.carebridge.backend.expert.dto.request.CreateExpertProfileRequest;
import com.carebridge.backend.expert.entity.ConsultationModality;
import com.carebridge.backend.expert.entity.ExpertProfile;
import com.carebridge.backend.expert.entity.ExpertProfileStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Props Isolation Factory for ExpertProfile tests.
 * All test data is created through this factory — no inline object creation.
 */
public class ExpertProfileTestFactory {

    // ─── Request Factory ───────────────────────────────────────

    public static CreateExpertProfileRequest makeValidRequest() {
        CreateExpertProfileRequest r = new CreateExpertProfileRequest();
        r.setDisplayName("Dr. Test Expert");
        r.setBio("10 years in obstetrics");
        r.setSpecialties(List.of("obstetrics", "prenatal_care"));
        r.setYearsOfExperience(10);
        r.setConsultationFeeVnd(200000L);
        r.setConsultationModalities(List.of(ConsultationModality.VIDEO));
        return r;
    }

    public static CreateExpertProfileRequest makeMinimalRequest() {
        CreateExpertProfileRequest r = new CreateExpertProfileRequest();
        r.setDisplayName("Dr. Minimal");
        r.setSpecialties(List.of("general"));
        r.setYearsOfExperience(1);
        r.setConsultationFeeVnd(50000L);
        r.setConsultationModalities(List.of(ConsultationModality.CHAT));
        return r;
    }

    // ─── Entity Factory ────────────────────────────────────────

    public static ExpertProfile makePendingProfile(UUID userId) {
        return ExpertProfile.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .displayName("Dr. Existing")
                .bio("Existing profile")
                .specialties(List.of("cardiology"))
                .yearsOfExperience(5)
                .consultationFeeVnd(150000L)
                .consultationModalities(List.of(ConsultationModality.CHAT, ConsultationModality.VIDEO))
                .status(ExpertProfileStatus.PENDING_VERIFICATION)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    public static ExpertProfile makeVerifiedProfile(UUID userId) {
        return ExpertProfile.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .displayName("Dr. Verified")
                .bio("Verified expert")
                .specialties(List.of("obstetrics"))
                .yearsOfExperience(15)
                .consultationFeeVnd(300000L)
                .consultationModalities(List.of(ConsultationModality.VIDEO))
                .status(ExpertProfileStatus.VERIFIED)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    // ─── Utility ───────────────────────────────────────────────

    public static UUID randomUserId() {
        return UUID.randomUUID();
    }
}
