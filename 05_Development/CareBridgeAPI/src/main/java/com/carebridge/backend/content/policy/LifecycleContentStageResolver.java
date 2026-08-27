package com.carebridge.backend.content.policy;

import com.carebridge.backend.content.entity.ContentStage;
import com.carebridge.backend.content.exception.ContentException;
import com.carebridge.backend.journey.entity.JourneyType;
import com.carebridge.backend.journey.entity.MotherJourney;
import com.carebridge.backend.journey.repository.MotherJourneyRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LifecycleContentStageResolver {

    private final MotherJourneyRepository motherJourneyRepository;

    public ContentStage resolve(UUID ownerId) {
        return map(motherJourneyRepository.findCanonical(ownerId)
                .orElseThrow(ContentException::lifecycleContextUnavailable).getJourneyType());
    }

    public ResolvedLifecycleContext resolveForUpdate(UUID ownerId) {
        MotherJourney journey = motherJourneyRepository.findCanonicalForUpdate(ownerId)
                .orElseThrow(ContentException::lifecycleContextUnavailable);
        return new ResolvedLifecycleContext(journey.getId(), map(journey.getJourneyType()));
    }

    private ContentStage map(JourneyType type) {
        return switch (type) {
            case PRE_PREGNANCY -> ContentStage.PRE_PREGNANCY;
            case PREGNANCY -> ContentStage.PREGNANCY;
            case POSTPARTUM -> ContentStage.POSTPARTUM;
            case BABY_CARE -> throw ContentException.lifecycleContextUnavailable();
        };
    }
}
