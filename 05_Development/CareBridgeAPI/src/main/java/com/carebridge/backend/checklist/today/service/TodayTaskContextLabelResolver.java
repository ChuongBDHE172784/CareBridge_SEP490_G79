package com.carebridge.backend.checklist.today.service;

import com.carebridge.backend.baby.entity.BabyProfile;
import com.carebridge.backend.baby.entity.BabyProfileStatus;
import com.carebridge.backend.baby.repository.BabyProfileRepository;
import com.carebridge.backend.checklist.model.ChecklistCareContextType;
import com.carebridge.backend.checklist.today.dto.TodayTaskCandidate;
import com.carebridge.backend.family.entity.CareGroup;
import com.carebridge.backend.family.entity.CareGroupStatus;
import com.carebridge.backend.family.repository.CareGroupRepository;
import com.carebridge.backend.journey.entity.JourneyStatus;
import com.carebridge.backend.journey.entity.JourneyType;
import com.carebridge.backend.journey.entity.MotherJourney;
import com.carebridge.backend.journey.repository.MotherJourneyRepository;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Resolves display labels only for context keys already authorized by Today providers. */
@Component
@RequiredArgsConstructor
public class TodayTaskContextLabelResolver {

    private final CareGroupRepository careGroupRepository;
    private final MotherJourneyRepository journeyRepository;
    private final BabyProfileRepository babyProfileRepository;

    public Map<ContextKey, Labels> resolve(Collection<TodayTaskCandidate> candidates) {
        Set<ContextKey> requested = candidates.stream()
                .map(ContextKey::from)
                .filter(ContextKey::isComplete)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (requested.isEmpty()) {
            return Map.of();
        }

        Set<UUID> groupIds = requested.stream()
                .map(ContextKey::careGroupId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        Map<UUID, CareGroup> groups = careGroupRepository.findAllById(groupIds).stream()
                .filter(TodayTaskContextLabelResolver::isCanonicalActiveGroup)
                .collect(Collectors.toMap(CareGroup::getId, Function.identity()));

        Set<UUID> journeyIds = new LinkedHashSet<>();
        Set<UUID> babyIds = new LinkedHashSet<>();
        for (ContextKey key : requested) {
            if (key.careGroupId() == null) {
                if (key.contextType() == ChecklistCareContextType.JOURNEY) {
                    journeyIds.add(key.contextId());
                } else {
                    babyIds.add(key.contextId());
                }
                continue;
            }
            CareGroup group = groups.get(key.careGroupId());
            if (group == null) {
                continue;
            }
            if (key.contextType() == ChecklistCareContextType.JOURNEY
                    && key.contextId().equals(group.getLinkedJourneyId())) {
                journeyIds.add(key.contextId());
            } else if (key.contextType() == ChecklistCareContextType.BABY
                    && key.contextId().equals(group.getLinkedBabyProfileId())) {
                babyIds.add(key.contextId());
            }
        }

        Map<UUID, MotherJourney> journeys = journeyRepository.findAllById(journeyIds).stream()
                .collect(Collectors.toMap(MotherJourney::getId, Function.identity()));
        Map<UUID, BabyProfile> babies = babyProfileRepository.findAllById(babyIds).stream()
                .collect(Collectors.toMap(BabyProfile::getId, Function.identity()));

        Map<ContextKey, Labels> result = new HashMap<>();
        for (ContextKey key : requested) {
            if (key.careGroupId() == null) {
                String contextLabel = personalContextLabel(key, journeys, babies);
                if (contextLabel != null) {
                    result.put(key, new Labels(null, contextLabel));
                }
                continue;
            }
            CareGroup group = groups.get(key.careGroupId());
            if (group == null) {
                continue;
            }
            String contextLabel = contextLabel(key, group, journeys, babies);
            if (contextLabel != null) {
                result.put(key, new Labels(group.getGroupName(), contextLabel));
            }
        }
        return Map.copyOf(result);
    }

    private static boolean isCanonicalActiveGroup(CareGroup group) {
        boolean hasJourney = group.getLinkedJourneyId() != null;
        boolean hasBaby = group.getLinkedBabyProfileId() != null;
        return group.getStatus() == CareGroupStatus.ACTIVE && (hasJourney || hasBaby);
    }

    private static String personalContextLabel(
            ContextKey key,
            Map<UUID, MotherJourney> journeys,
            Map<UUID, BabyProfile> babies) {
        if (key.contextType() == ChecklistCareContextType.JOURNEY) {
            MotherJourney journey = journeys.get(key.contextId());
            return journey != null && journey.getStatus() == JourneyStatus.ACTIVE
                    ? journeyLabel(journey.getJourneyType()) : null;
        }
        BabyProfile baby = babies.get(key.contextId());
        return baby != null && baby.getStatus() == BabyProfileStatus.ACTIVE
                ? baby.getNickname() : null;
    }

    private static String contextLabel(
            ContextKey key,
            CareGroup group,
            Map<UUID, MotherJourney> journeys,
            Map<UUID, BabyProfile> babies) {
        if (key.contextType() == ChecklistCareContextType.JOURNEY) {
            if (!key.contextId().equals(group.getLinkedJourneyId())) {
                return null;
            }
            MotherJourney journey = journeys.get(key.contextId());
            return journey != null
                    && journey.getStatus() == JourneyStatus.ACTIVE
                    && group.getOwnerUserId().equals(journey.getOwnerUserId())
                    ? journeyLabel(journey.getJourneyType())
                    : null;
        }
        if (!key.contextId().equals(group.getLinkedBabyProfileId())) {
            return null;
        }
        BabyProfile baby = babies.get(key.contextId());
        return baby != null
                && baby.getStatus() == BabyProfileStatus.ACTIVE
                && group.getOwnerUserId().equals(baby.getOwnerUserId())
                ? baby.getNickname()
                : null;
    }

    private static String journeyLabel(JourneyType type) {
        return switch (type) {
            case PRE_PREGNANCY -> "Chuẩn bị mang thai";
            case PREGNANCY -> "Mang thai";
            case POSTPARTUM -> "Hậu sản";
            case BABY_CARE -> "Nuôi con";
        };
    }

    public record ContextKey(
            UUID careGroupId,
            ChecklistCareContextType contextType,
            UUID contextId) {

        public static ContextKey from(TodayTaskCandidate candidate) {
            return new ContextKey(
                    candidate.careGroupId(),
                    candidate.careContextType(),
                    candidate.careContextId());
        }

        private boolean isComplete() {
            return contextType != null && contextId != null;
        }
    }

    public record Labels(String careGroupLabel, String careContextLabel) {
    }
}
