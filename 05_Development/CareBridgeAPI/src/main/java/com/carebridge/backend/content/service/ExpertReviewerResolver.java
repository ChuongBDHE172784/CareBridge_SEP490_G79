package com.carebridge.backend.content.service;

import com.carebridge.backend.content.dto.response.ExpertReviewerResponse;
import com.carebridge.backend.expert.entity.ExpertProfile;
import com.carebridge.backend.expert.repository.ExpertProfileRepository;
import com.carebridge.backend.security.entity.User;
import com.carebridge.backend.security.repository.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExpertReviewerResolver {

    private final ExpertProfileRepository expertProfileRepository;
    private final UserRepository userRepository;

    public ExpertReviewerResponse resolve(UUID approvedBy, UUID assignedExpertId, Instant approvedAt) {
        UUID targetId = approvedBy != null ? approvedBy : assignedExpertId;

        if (targetId != null) {
            ExpertReviewerResponse response = buildFromUserId(targetId, approvedAt);
            if (response != null) {
                return response;
            }
        }

        // Fallback: resolve primary verified expert from the database
        try {
            List<ExpertProfile> verifiedList = expertProfileRepository.findVerifiedPublic();
            if (!verifiedList.isEmpty()) {
                ExpertProfile fallback = verifiedList.get(0);
                return buildFromProfile(fallback, approvedAt != null ? approvedAt : Instant.now());
            }
        } catch (Exception ex) {
            log.warn("Failed to retrieve fallback verified expert from database: {}", ex.getMessage());
        }

        return null;
    }

    private ExpertReviewerResponse buildFromUserId(UUID userId, Instant approvedAt) {
        try {
            User user = userRepository.findById(userId).orElse(null);
            if (user == null) {
                return null;
            }

            ExpertProfile profile = expertProfileRepository.findByUserId(userId).orElse(null);

            String name = user.getName() != null && !user.getName().isBlank()
                    ? user.getName()
                    : user.getDisplayName();

            String title = profile != null && profile.getProfessionalTitle() != null && !profile.getProfessionalTitle().isBlank()
                    ? profile.getProfessionalTitle()
                    : "Bác sĩ";

            String specialty = profile != null ? profile.getSpecialty() : null;
            String workplace = profile != null ? profile.getWorkplace() : null;
            String bio = profile != null ? profile.getConsultationScope() : null;
            String avatarUrl = user.getAvatarUrl();

            return ExpertReviewerResponse.builder()
                    .expertId(userId)
                    .name(name)
                    .professionalTitle(title)
                    .specialty(specialty)
                    .workplace(workplace)
                    .bio(bio)
                    .avatarUrl(avatarUrl)
                    .approvedAt(approvedAt)
                    .verificationStatus("Đã kiểm duyệt nội dung")
                    .build();
        } catch (Exception ex) {
            log.warn("Error building expert reviewer response for user {}: {}", userId, ex.getMessage());
            return null;
        }
    }

    private ExpertReviewerResponse buildFromProfile(ExpertProfile profile, Instant approvedAt) {
        UUID userId = profile.getUserId();
        User user = profile.getUser() != null ? profile.getUser() : userRepository.findById(userId).orElse(null);

        String name = user != null && user.getName() != null && !user.getName().isBlank()
                ? user.getName()
                : (user != null && user.getDisplayName() != null ? user.getDisplayName() : "Bác sĩ Chuyên khoa");

        String title = profile.getProfessionalTitle() != null && !profile.getProfessionalTitle().isBlank()
                ? profile.getProfessionalTitle()
                : "Bác sĩ";

        return ExpertReviewerResponse.builder()
                .expertId(userId)
                .name(name)
                .professionalTitle(title)
                .specialty(profile.getSpecialty())
                .workplace(profile.getWorkplace())
                .bio(profile.getConsultationScope())
                .avatarUrl(user != null ? user.getAvatarUrl() : null)
                .approvedAt(approvedAt)
                .verificationStatus("Đã kiểm duyệt nội dung")
                .build();
    }
}
