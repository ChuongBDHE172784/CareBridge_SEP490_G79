package com.carebridge.backend.family.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.audit.service.RequiredAuditEvent;
import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.checklist.model.ChecklistCareContextType;
import com.carebridge.backend.checklist.model.ChecklistTemplateReviewStatus;
import com.carebridge.backend.family.entity.CareGroup;
import com.carebridge.backend.family.entity.CareGroupStatus;
import com.carebridge.backend.family.repository.CareGroupRepository;
import com.carebridge.backend.family.service.impl.CareGroupServiceImpl;
import com.carebridge.backend.journey.entity.JourneyStatus;
import com.carebridge.backend.journey.entity.JourneyType;
import com.carebridge.backend.journey.entity.MotherJourney;
import com.carebridge.backend.journey.repository.MotherJourneyRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CareGroupJourneyRelinkServiceTest {

    private static final UUID GROUP = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID OWNER = UUID.fromString("10000000-0000-0000-0000-000000000002");
    private static final UUID OTHER = UUID.fromString("10000000-0000-0000-0000-000000000003");
    private static final UUID OLD_JOURNEY = UUID.fromString("10000000-0000-0000-0000-000000000004");
    private static final UUID NEW_JOURNEY = UUID.fromString("10000000-0000-0000-0000-000000000005");

    @Mock private CareGroupRepository groupRepository;
    @Mock private MotherJourneyRepository journeyRepository;
    @Mock private AuditService auditService;
    @InjectMocks private CareGroupServiceImpl service;

    @Test
    void ownerRelinksActiveGroupToOwnedActiveJourneyAndWritesRequiredTypedAudit() {
        CareGroup group = group(OWNER, CareGroupStatus.ACTIVE);
        when(groupRepository.findByIdAndOwnerUserIdForUpdate(GROUP, OWNER)).thenReturn(Optional.of(group));
        when(journeyRepository.findByIdAndOwnerUserIdForUpdate(NEW_JOURNEY, OWNER))
                .thenReturn(Optional.of(journey(OWNER, JourneyStatus.ACTIVE)));
        when(groupRepository.saveAndFlush(group)).thenReturn(group);

        var response = service.relinkJourney(GROUP, NEW_JOURNEY, OWNER);

        assertThat(response.getGroupId()).isEqualTo(GROUP);
        assertThat(response.getPreviousJourneyId()).isEqualTo(OLD_JOURNEY);
        assertThat(response.getJourneyId()).isEqualTo(NEW_JOURNEY);
        assertThat(response.getRelinkedAt()).isNotNull();
        assertThat(response.getCorrelationId()).isNotNull();
        assertThat(group.getLinkedJourneyId()).isEqualTo(NEW_JOURNEY);
        verify(groupRepository).saveAndFlush(group);

        ArgumentCaptor<RequiredAuditEvent> audit = ArgumentCaptor.forClass(RequiredAuditEvent.class);
        verify(auditService).logRequired(audit.capture());
        assertThat(audit.getValue().action()).isEqualTo(AuditAction.CARE_GROUP_CONTEXT_RELINKED);
        assertThat(audit.getValue().actorUserId()).isEqualTo(OWNER);
        assertThat(audit.getValue().actorType()).isEqualTo("USER");
        assertThat(audit.getValue().actorService()).isNull();
        assertThat(audit.getValue().subjectUserId()).isEqualTo(OWNER);
        assertThat(audit.getValue().resourceType()).isEqualTo("CARE_GROUP_CONTEXT");
        assertThat(audit.getValue().resourceId()).isEqualTo(GROUP);
        assertThat(audit.getValue().careContextId()).isEqualTo(NEW_JOURNEY);
        assertThat(audit.getValue().beforePayload()).containsEntry("careContextId", OLD_JOURNEY);
        assertThat(audit.getValue().afterPayload()).containsEntry("careContextId", NEW_JOURNEY);
        assertThat(audit.getValue().correlationId()).isEqualTo(response.getCorrelationId());
    }

    @Test
    void relinkDoesNotRequireChecklistReplacementAuthorityProjection() {
        CareGroup group = group(OWNER, CareGroupStatus.ACTIVE);
        when(groupRepository.findByIdAndOwnerUserIdForUpdate(GROUP, OWNER)).thenReturn(Optional.of(group));
        when(journeyRepository.findByIdAndOwnerUserIdForUpdate(NEW_JOURNEY, OWNER))
                .thenReturn(Optional.of(journey(OWNER, JourneyStatus.ACTIVE)));
        when(groupRepository.saveAndFlush(group)).thenReturn(group);

        var response = service.relinkJourney(GROUP, NEW_JOURNEY, OWNER);

        assertThat(response.getJourneyId()).isEqualTo(NEW_JOURNEY);
        assertThat(group.getLinkedJourneyId()).isEqualTo(NEW_JOURNEY);
        verify(groupRepository).saveAndFlush(group);
        verify(auditService).logRequired(any());
    }

    @Test
    void wrongOwnerIsIndistinguishableFromUnknownGroup() {
        assertNotFound(() -> service.relinkJourney(GROUP, NEW_JOURNEY, OTHER), "CARE_GROUP_NOT_FOUND");
        assertNotFound(() -> service.relinkJourney(UUID.randomUUID(), NEW_JOURNEY, OTHER),
                "CARE_GROUP_NOT_FOUND");
        verify(groupRepository, never()).saveAndFlush(any());
        verify(auditService, never()).logRequired(any());
    }

    @Test
    void unauthorizedRelinkMustNotAcquireTheVictimGroupWriteLock() {
        assertNotFound(() -> service.relinkJourney(GROUP, NEW_JOURNEY, OTHER), "CARE_GROUP_NOT_FOUND");
        verify(groupRepository, never()).findByIdForUpdate(GROUP);
    }

    @Test
    void relinkMustLockJourneyBeforeCheckingItsActiveStatus() {
        when(groupRepository.findByIdAndOwnerUserIdForUpdate(GROUP, OWNER))
                .thenReturn(Optional.of(group(OWNER, CareGroupStatus.ACTIVE)));

        assertNotFound(() -> service.relinkJourney(GROUP, NEW_JOURNEY, OWNER), "JOURNEY_NOT_FOUND");
        verify(journeyRepository, never()).findById(NEW_JOURNEY);
    }

    @Test
    void initialContextLinkIsNotAcceptedAsRelink() {
        CareGroup unlinked = group(OWNER, CareGroupStatus.ACTIVE);
        unlinked.setLinkedJourneyId(null);
        when(groupRepository.findByIdAndOwnerUserIdForUpdate(GROUP, OWNER)).thenReturn(Optional.of(unlinked));

        assertThatThrownBy(() -> service.relinkJourney(GROUP, NEW_JOURNEY, OWNER))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo("CARE_GROUP_CONTEXT_MISSING"));
        verify(groupRepository, never()).saveAndFlush(any());
        verify(auditService, never()).logRequired(any());
    }

    @Test
    void wrongJourneyOwnerIsIndistinguishableFromUnknownJourney() {
        when(groupRepository.findByIdAndOwnerUserIdForUpdate(GROUP, OWNER))
                .thenReturn(Optional.of(group(OWNER, CareGroupStatus.ACTIVE)));

        assertNotFound(() -> service.relinkJourney(GROUP, NEW_JOURNEY, OWNER), "JOURNEY_NOT_FOUND");
        verify(groupRepository, never()).saveAndFlush(any());
        verify(auditService, never()).logRequired(any());
    }

    @Test
    void inactiveGroupAndJourneyAreRejectedWithoutMutationOrAudit() {
        when(groupRepository.findByIdAndOwnerUserIdForUpdate(GROUP, OWNER))
                .thenReturn(Optional.of(group(OWNER, CareGroupStatus.ARCHIVED)));
        assertThatThrownBy(() -> service.relinkJourney(GROUP, NEW_JOURNEY, OWNER))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo("CARE_GROUP_INACTIVE"));

        when(groupRepository.findByIdAndOwnerUserIdForUpdate(GROUP, OWNER))
                .thenReturn(Optional.of(group(OWNER, CareGroupStatus.ACTIVE)));
        when(journeyRepository.findByIdAndOwnerUserIdForUpdate(NEW_JOURNEY, OWNER))
                .thenReturn(Optional.of(journey(OWNER, JourneyStatus.COMPLETED)));
        assertThatThrownBy(() -> service.relinkJourney(GROUP, NEW_JOURNEY, OWNER))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo("JOURNEY_INACTIVE"));
        verify(groupRepository, never()).saveAndFlush(any());
        verify(auditService, never()).logRequired(any());
    }

    @Test
    void dualLinkedBabyGroupIsRejectedWithoutMutationOrAudit() {
        CareGroup group = group(OWNER, CareGroupStatus.ACTIVE);
        group.setLinkedBabyProfileId(UUID.fromString("10000000-0000-0000-0000-000000000006"));
        when(groupRepository.findByIdAndOwnerUserIdForUpdate(GROUP, OWNER)).thenReturn(Optional.of(group));

        assertThatThrownBy(() -> service.relinkJourney(GROUP, NEW_JOURNEY, OWNER))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getHttpStatus().value()).isEqualTo(409);
                    assertThat(exception.getCode()).isEqualTo("CARE_GROUP_CONTEXT_AMBIGUOUS");
                });

        assertThat(group.getLinkedJourneyId()).isEqualTo(OLD_JOURNEY);
        verify(journeyRepository, never()).findByIdAndOwnerUserIdForUpdate(any(), any());
        verify(groupRepository, never()).saveAndFlush(any());
        verify(auditService, never()).logRequired(any());
    }

    private static CareGroup group(UUID owner, CareGroupStatus status) {
        return CareGroup.builder().id(GROUP).ownerUserId(owner).groupName("Relink test")
                .status(status).linkedJourneyId(OLD_JOURNEY).build();
    }

    private static MotherJourney journey(UUID owner, JourneyStatus status) {
        return MotherJourney.builder().id(NEW_JOURNEY).ownerUserId(owner)
                .journeyType(JourneyType.PREGNANCY).status(status).build();
    }

    private static void assertNotFound(
            org.assertj.core.api.ThrowableAssert.ThrowingCallable call,
            String code) {
        assertThatThrownBy(call).isInstanceOfSatisfying(BusinessException.class, exception -> {
            assertThat(exception.getHttpStatus().value()).isEqualTo(404);
            assertThat(exception.getCode()).isEqualTo(code);
        });
    }
}
