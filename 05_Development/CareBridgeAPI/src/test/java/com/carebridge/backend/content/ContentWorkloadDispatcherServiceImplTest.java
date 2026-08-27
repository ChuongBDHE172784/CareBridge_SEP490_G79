package com.carebridge.backend.content;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.carebridge.backend.content.entity.ContentStage;
import com.carebridge.backend.content.entity.ContentStatus;
import com.carebridge.backend.content.entity.ContentType;
import com.carebridge.backend.content.repository.ChecklistTemplateRepository;
import com.carebridge.backend.content.repository.ContentRepository;
import com.carebridge.backend.content.service.ContentWorkloadDispatcherServiceImpl;
import com.carebridge.backend.expert.entity.ExpertProfile;
import com.carebridge.backend.expert.experttype.ExpertType;
import com.carebridge.backend.expert.repository.ExpertProfileRepository;
import com.carebridge.backend.expert.truststatus.TrustStatus;
import com.carebridge.backend.expert.verificationstatus.VerificationStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ContentWorkloadDispatcherServiceImplTest {

    @Mock
    private ExpertProfileRepository expertProfileRepository;

    @Mock
    private ContentRepository contentRepository;

    @Mock
    private ChecklistTemplateRepository checklistTemplateRepository;

    @InjectMocks
    private ContentWorkloadDispatcherServiceImpl dispatcherService;

    private UUID expertId1;
    private UUID expertId2;
    private UUID expertId3;

    private ExpertProfile expert1;
    private ExpertProfile expert2;
    private ExpertProfile expert3;

    @BeforeEach
    void setUp() {
        expertId1 = UUID.randomUUID();
        expertId2 = UUID.randomUUID();
        expertId3 = UUID.randomUUID();

        expert1 = ExpertProfile.builder()
                .userId(expertId1)
                .specialty("Sản phụ khoa")
                .expertType(ExpertType.CONTRACTED)
                .verificationStatus(VerificationStatus.APPROVED)
                .trustStatus(TrustStatus.ACTIVE)
                .ratingAvg(new BigDecimal("4.8"))
                .build();

        expert2 = ExpertProfile.builder()
                .userId(expertId2)
                .specialty("Nhi khoa")
                .expertType(ExpertType.CONTRACTED)
                .verificationStatus(VerificationStatus.APPROVED)
                .trustStatus(TrustStatus.ACTIVE)
                .ratingAvg(new BigDecimal("4.9"))
                .build();

        expert3 = ExpertProfile.builder()
                .userId(expertId3)
                .specialty("Sản khoa")
                .expertType(ExpertType.CONTRACTED)
                .verificationStatus(VerificationStatus.APPROVED)
                .trustStatus(TrustStatus.ACTIVE)
                .ratingAvg(new BigDecimal("4.5"))
                .build();
    }

    @Test
    void dispatchToOptimalExpert_noContractedExperts_returnsNull() {
        when(expertProfileRepository.findActiveContractedExperts()).thenReturn(List.of());

        UUID result = dispatcherService.dispatchToOptimalExpert(ContentType.ARTICLE, ContentStage.PREGNANCY, null);

        assertThat(result).isNull();
    }

    @Test
    void dispatchToOptimalExpert_preferredExpertValid_returnsPreferred() {
        when(expertProfileRepository.findActiveContractedExperts()).thenReturn(List.of(expert1, expert2));

        UUID result = dispatcherService.dispatchToOptimalExpert(ContentType.ARTICLE, ContentStage.PREGNANCY, expertId2);

        assertThat(result).isEqualTo(expertId2);
    }

    @Test
    void dispatchToOptimalExpert_specialtyMatching_choosesSpecialtyMatch() {
        when(expertProfileRepository.findActiveContractedExperts()).thenReturn(List.of(expert1, expert2));
        when(contentRepository.countPendingByAssignedExpertIds(eq(ContentStatus.PENDING_REVIEW), any()))
                .thenReturn(List.<Object[]>of(new Object[]{expertId1, 1L}));
        when(checklistTemplateRepository.countPendingByAssignedExpertIds(any(), any()))
                .thenReturn(List.of());

        // For BABY_CARE, expert2 (Nhi khoa) matches specialty
        UUID result = dispatcherService.dispatchToOptimalExpert(ContentType.ARTICLE, ContentStage.BABY_CARE, null);

        assertThat(result).isEqualTo(expertId2);
    }

    @Test
    void dispatchToOptimalExpert_leastLoaded_choosesLowestPendingCount() {
        when(expertProfileRepository.findActiveContractedExperts()).thenReturn(List.of(expert1, expert3));
        // expert1 has 3 pending, expert3 has 0 pending
        when(contentRepository.countPendingByAssignedExpertIds(eq(ContentStatus.PENDING_REVIEW), any()))
                .thenReturn(List.<Object[]>of(new Object[]{expertId1, 3L}));
        when(checklistTemplateRepository.countPendingByAssignedExpertIds(any(), any()))
                .thenReturn(List.of());

        UUID result = dispatcherService.dispatchToOptimalExpert(ContentType.ARTICLE, ContentStage.PREGNANCY, null);

        assertThat(result).isEqualTo(expertId3);
    }

    @Test
    void dispatchToOptimalExpert_tieBreaker_choosesLongestIdleExpert() {
        when(expertProfileRepository.findActiveContractedExperts()).thenReturn(List.of(expert1, expert3));
        // Both have 0 pending items
        when(contentRepository.countPendingByAssignedExpertIds(eq(ContentStatus.PENDING_REVIEW), any()))
                .thenReturn(List.of());
        when(checklistTemplateRepository.countPendingByAssignedExpertIds(any(), any()))
                .thenReturn(List.of());

        // expert1 was assigned 10 mins ago, expert3 has never been assigned (null)
        when(contentRepository.findLatestAssignedAtByExpertId(expertId1)).thenReturn(Instant.now().minusSeconds(600));
        when(contentRepository.findLatestAssignedAtByExpertId(expertId3)).thenReturn(null);

        UUID result = dispatcherService.dispatchToOptimalExpert(ContentType.ARTICLE, ContentStage.PREGNANCY, null);

        // expert3 is null (longest idle) -> chosen first
        assertThat(result).isEqualTo(expertId3);
    }
}
