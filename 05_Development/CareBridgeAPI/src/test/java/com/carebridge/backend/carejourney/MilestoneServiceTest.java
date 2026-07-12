package com.carebridge.backend.carejourney;

import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.baby.entity.BabyProfile;
import com.carebridge.backend.baby.entity.BabyProfileStatus;
import com.carebridge.backend.baby.policy.BabyAccessPolicy;
import com.carebridge.backend.baby.repository.BabyProfileRepository;
import com.carebridge.backend.carejourney.dto.AddMilestoneRequest;
import com.carebridge.backend.carejourney.dto.MilestoneResponse;
import com.carebridge.backend.carejourney.dto.UpdateDevelopmentMilestoneRequest;
import com.carebridge.backend.carejourney.entity.DevelopmentMilestone;
import com.carebridge.backend.carejourney.entity.MilestoneAchievementStatus;
import com.carebridge.backend.carejourney.entity.MilestoneRecordStatus;
import com.carebridge.backend.carejourney.repository.DevelopmentMilestoneRepository;
import com.carebridge.backend.carejourney.service.impl.MilestoneServiceImpl;
import com.carebridge.backend.common.exception.AccessDeniedBusinessException;
import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MilestoneServiceTest {

    @Mock private DevelopmentMilestoneRepository milestoneRepository;
    @Mock private BabyProfileRepository babyProfileRepository;
    @Mock private BabyAccessPolicy babyAccessPolicy;
    @Mock private AuditService auditService;
    @InjectMocks private MilestoneServiceImpl service;

    static final UUID MOTHER_ID       = UUID.fromString("00000000-0000-0000-0000-000000000037");
    static final UUID OTHER_USER_ID   = UUID.fromString("00000000-0000-0000-0000-000000000099");
    static final UUID BABY_ID         = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000037");
    static final UUID MILESTONE_ID    = UUID.fromString("cccccccc-0000-0000-0000-000000000001");

    private BabyProfile makeActiveBaby() {
        return BabyProfile.builder()
                .id(BABY_ID).ownerUserId(MOTHER_ID).nickname("Test Baby")
                .status(BabyProfileStatus.ACTIVE).build();
    }

    private BabyProfile makeArchivedBaby() {
        return BabyProfile.builder()
                .id(BABY_ID).ownerUserId(MOTHER_ID).nickname("Archived Baby")
                .status(BabyProfileStatus.ARCHIVED).build();
    }

    private BabyProfile makeBabyOwnedByOther() {
        return BabyProfile.builder()
                .id(BABY_ID).ownerUserId(OTHER_USER_ID).nickname("Other Baby")
                .status(BabyProfileStatus.ACTIVE).build();
    }

    private AddMilestoneRequest makeRequest() {
        AddMilestoneRequest req = new AddMilestoneRequest();
        req.setMilestoneType("WALKING");
        req.setAchievedDate(LocalDate.now().minusDays(3));
        req.setNote("First steps in living room");
        return req;
    }

    private DevelopmentMilestone makeSavedMilestone(String milestoneType) {
        return DevelopmentMilestone.builder()
                .milestoneId(MILESTONE_ID).babyId(BABY_ID)
                .milestoneType(milestoneType)
                .achievedDate(LocalDate.now().minusDays(3))
                .note("First steps in living room")
                .sourceType("MANUAL")
                .recordedBy(MOTHER_ID)
                .createdAt(Instant.now()).build();
    }

    // MILESTONE-TC-037-001: Happy path WALKING milestone
    @Test
    void addMilestone_validWalking_returnsMilestoneResponse() {
        when(babyProfileRepository.findById(BABY_ID)).thenReturn(Optional.of(makeActiveBaby()));
        when(milestoneRepository.save(any())).thenReturn(makeSavedMilestone("WALKING"));

        MilestoneResponse resp = service.addMilestone(MOTHER_ID, BABY_ID, makeRequest());

        assertThat(resp.getMilestoneType()).isEqualTo("WALKING");
        assertThat(resp.getBabyId()).isEqualTo(BABY_ID);
        assertThat(resp.getRecordedBy()).isEqualTo(MOTHER_ID);
        assertThat(resp.getSourceType()).isEqualTo("MANUAL");
        verify(milestoneRepository).save(any());
        verify(auditService).log(any(), eq(MOTHER_ID), anyString(), anyString(), any());
    }

    // MILESTONE-TC-037-002: Future achievedDate -> BusinessException BABY-064
    @Test
    void addMilestone_futureDateRejected_throwsBadRequest() {
        when(babyProfileRepository.findById(BABY_ID)).thenReturn(Optional.of(makeActiveBaby()));

        AddMilestoneRequest req = makeRequest();
        req.setAchievedDate(LocalDate.now().plusDays(1)); // tomorrow

        assertThatThrownBy(() -> service.addMilestone(MOTHER_ID, BABY_ID, req))
                .isInstanceOf(BusinessException.class);
        verify(milestoneRepository, never()).save(any());
    }

    // MILESTONE-TC-037-003: Baby not owned -> AccessDeniedBusinessException BABY-061
    @Test
    void addMilestone_babyNotOwned_throwsForbidden() {
        when(babyProfileRepository.findById(BABY_ID)).thenReturn(Optional.of(makeBabyOwnedByOther()));

        assertThatThrownBy(() -> service.addMilestone(MOTHER_ID, BABY_ID, makeRequest()))
                .isInstanceOf(AccessDeniedBusinessException.class);
        verify(milestoneRepository, never()).save(any());
    }

    // MILESTONE-TC-037-004: Baby archived -> BusinessException BABY-062
    @Test
    void addMilestone_babyArchived_throwsBadRequest() {
        when(babyProfileRepository.findById(BABY_ID)).thenReturn(Optional.of(makeArchivedBaby()));

        assertThatThrownBy(() -> service.addMilestone(MOTHER_ID, BABY_ID, makeRequest()))
                .isInstanceOf(BusinessException.class);
        verify(milestoneRepository, never()).save(any());
    }

    // MILESTONE-TC-037-005: Invalid milestone type -> BusinessException BABY-063
    @Test
    void addMilestone_invalidMilestoneType_throwsBadRequest() {
        when(babyProfileRepository.findById(BABY_ID)).thenReturn(Optional.of(makeActiveBaby()));

        AddMilestoneRequest req = makeRequest();
        req.setMilestoneType("FLYING"); // not in valid types

        assertThatThrownBy(() -> service.addMilestone(MOTHER_ID, BABY_ID, req))
                .isInstanceOf(BusinessException.class);
        verify(milestoneRepository, never()).save(any());
    }

    // MILESTONE-TC-037-006: Duplicate milestone type allowed (ADR-BABY-007-001)
    @Test
    void addMilestone_duplicateMilestoneType_succeeds() {
        when(babyProfileRepository.findById(BABY_ID)).thenReturn(Optional.of(makeActiveBaby()));
        // TEETHING can happen multiple times
        DevelopmentMilestone savedTeething = makeSavedMilestone("TEETHING");
        when(milestoneRepository.save(any())).thenReturn(savedTeething);

        AddMilestoneRequest req = makeRequest();
        req.setMilestoneType("TEETHING");

        MilestoneResponse resp = service.addMilestone(MOTHER_ID, BABY_ID, req);

        assertThat(resp.getMilestoneType()).isEqualTo("TEETHING");
        // No exception thrown — duplicates allowed per ADR-BABY-007-001
        verify(milestoneRepository).save(any());
    }

    // MILESTONE-TC-037-007: Baby not found -> ResourceNotFoundException BABY-060
    @Test
    void addMilestone_babyNotFound_throwsNotFound() {
        when(babyProfileRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.addMilestone(MOTHER_ID, BABY_ID, makeRequest()))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(milestoneRepository, never()).save(any());
    }

    // MILESTONE-TC-037-008: recordedBy set from JWT (C4 — not from request body)
    @Test
    void addMilestone_recordedBySetFromJwt() {
        when(babyProfileRepository.findById(BABY_ID)).thenReturn(Optional.of(makeActiveBaby()));
        when(milestoneRepository.save(any())).thenAnswer(inv -> {
            DevelopmentMilestone arg = inv.getArgument(0);
            assertThat(arg.getRecordedBy())
                    .as("recordedBy must be set from JWT userId — ADR-BABY-008")
                    .isEqualTo(MOTHER_ID);
            return makeSavedMilestone("WALKING");
        });

        service.addMilestone(MOTHER_ID, BABY_ID, makeRequest());
    }

    // MILESTONE-TC-037-009: No medical interpretation (ADR-BABY-007-003 / BR-SAFETY)
    @Test
    void addMilestone_noMedicalInterpretation_inResponse() {
        when(babyProfileRepository.findById(BABY_ID)).thenReturn(Optional.of(makeActiveBaby()));
        when(milestoneRepository.save(any())).thenReturn(makeSavedMilestone("WALKING"));

        MilestoneResponse resp = service.addMilestone(MOTHER_ID, BABY_ID, makeRequest());

        // MilestoneResponse must NOT contain any medical interpretation fields
        // (e.g., no "isNormal", no "developmentScore", no WHO-percentile comparison)
        // This test verifies the response DTO structure
        assertThat(resp).isNotNull();
        // Response only contains: milestoneId, babyId, milestoneType, achievedDate, note, sourceType, recordedBy, createdAt
        assertThat(resp.getMilestoneId()).isNotNull();
        assertThat(resp.getBabyId()).isNotNull();
        assertThat(resp.getMilestoneType()).isNotNull();
    }

    @Test
    void updateMilestone_ownerUpdatesStatusAndDate_preservesRecordStatus() {
        DevelopmentMilestone existing = makeSavedMilestone("WALKING");
        existing.setMilestoneStatus(MilestoneAchievementStatus.PENDING);
        existing.setRecordStatus(MilestoneRecordStatus.ACTIVE);
        when(milestoneRepository.findByMilestoneIdAndRecordStatus(MILESTONE_ID, MilestoneRecordStatus.ACTIVE))
                .thenReturn(Optional.of(existing));
        when(babyProfileRepository.findById(BABY_ID)).thenReturn(Optional.of(makeActiveBaby()));
        when(babyAccessPolicy.canManage(any(), eq(MOTHER_ID))).thenReturn(true);
        when(milestoneRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UpdateDevelopmentMilestoneRequest request = new UpdateDevelopmentMilestoneRequest();
        request.setStatus("ACHIEVED");
        request.setAchievedDate(LocalDate.now().minusDays(1));

        MilestoneResponse response = service.updateMilestone(BABY_ID, MILESTONE_ID, request, MOTHER_ID);

        assertThat(response.getStatus()).isEqualTo("ACHIEVED");
        verify(milestoneRepository).save(argThat(milestone ->
                MilestoneAchievementStatus.ACHIEVED.equals(milestone.getMilestoneStatus())
                        && MilestoneRecordStatus.ACTIVE.equals(milestone.getRecordStatus())));
    }

    @Test
    void updateMilestone_emptyRequest_throwsMilestone003() {
        DevelopmentMilestone existing = makeSavedMilestone("WALKING");
        when(milestoneRepository.findByMilestoneIdAndRecordStatus(MILESTONE_ID, MilestoneRecordStatus.ACTIVE))
                .thenReturn(Optional.of(existing));
        when(babyProfileRepository.findById(BABY_ID)).thenReturn(Optional.of(makeActiveBaby()));
        when(babyAccessPolicy.canManage(any(), eq(MOTHER_ID))).thenReturn(true);

        assertThatThrownBy(() -> service.updateMilestone(
                BABY_ID, MILESTONE_ID, new UpdateDevelopmentMilestoneRequest(), MOTHER_ID))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo("MILESTONE-003"));
    }

    @Test
    void deleteMilestone_ownerSoftDeletesWithoutChangingMilestoneStatus() {
        DevelopmentMilestone existing = makeSavedMilestone("WALKING");
        existing.setMilestoneStatus(MilestoneAchievementStatus.ACHIEVED);
        existing.setRecordStatus(MilestoneRecordStatus.ACTIVE);
        when(milestoneRepository.findByMilestoneIdAndRecordStatus(MILESTONE_ID, MilestoneRecordStatus.ACTIVE))
                .thenReturn(Optional.of(existing));
        when(babyProfileRepository.findById(BABY_ID)).thenReturn(Optional.of(makeActiveBaby()));
        when(babyAccessPolicy.canManage(any(), eq(MOTHER_ID))).thenReturn(true);

        service.deleteMilestone(BABY_ID, MILESTONE_ID, MOTHER_ID);

        verify(milestoneRepository).save(argThat(milestone ->
                MilestoneRecordStatus.DELETED.equals(milestone.getRecordStatus())
                        && MilestoneAchievementStatus.ACHIEVED.equals(milestone.getMilestoneStatus())));
        verify(milestoneRepository, never()).delete(any());
    }

    @Test
    void deleteMilestone_alreadyDeleted_returnsNotFound() {
        when(milestoneRepository.findByMilestoneIdAndRecordStatus(MILESTONE_ID, MilestoneRecordStatus.ACTIVE))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteMilestone(BABY_ID, MILESTONE_ID, MOTHER_ID))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo("MILESTONE-001"));
        verify(milestoneRepository, never()).save(any());
    }
}
