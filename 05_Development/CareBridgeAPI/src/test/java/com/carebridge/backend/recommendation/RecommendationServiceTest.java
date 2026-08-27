package com.carebridge.backend.recommendation;

import static org.assertj.core.api.Assertions.assertThat;

import com.carebridge.backend.journey.entity.JourneyType;
import com.carebridge.backend.recommendation.service.RecommendationService;
import java.lang.reflect.Method;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

class RecommendationServiceTest {

    @Test
    void markStageReviewUsesAnIndependentTransaction() throws NoSuchMethodException {
        Method method = RecommendationService.class.getDeclaredMethod(
                "markStageReview", UUID.class, UUID.class, JourneyType.class);
        Transactional transactional = method.getAnnotation(Transactional.class);

        assertThat(transactional).isNotNull();
        assertThat(transactional.propagation()).isEqualTo(Propagation.REQUIRES_NEW);
    }
}
