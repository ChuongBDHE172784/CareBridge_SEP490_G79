package com.carebridge.backend.carejourney;

import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.baby.entity.BabyProfile;
import com.carebridge.backend.baby.entity.BabyProfileStatus;
import com.carebridge.backend.baby.policy.BabyAccessPolicy;
import com.carebridge.backend.baby.repository.BabyProfileRepository;
import com.carebridge.backend.carejourney.dto.AddBabyDailyLogRequest;
import com.carebridge.backend.carejourney.dto.AddBabyDailyLogResponse;
import com.carebridge.backend.carejourney.dto.BabyDailyLogResponse;
import com.carebridge.backend.carejourney.dto.UpdateBabyDailyLogRequest;
import com.carebridge.backend.carejourney.entity.BabyDailyLog;
import com.carebridge.backend.carejourney.entity.BabyDailyLogStatus;
import com.carebridge.backend.carejourney.repository.BabyDailyLogRepository;
import com.carebridge.backend.carejourney.service.impl.BabyDailyLogServiceImpl;
import com.carebridge.backend.common.exception.AccessDeniedBusinessException;
import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BabyDailyLogServiceTest {

    @Mock private BabyDailyLogRepository babyDailyLogRepository;
    @Mock private BabyProfileRepository babyProfileRepository;
    @Mock private BabyAccessPolicy babyAccessPolicy;
    @Mock private AuditService auditService;
    @InjectMocks private BabyDailyLogServiceImpl service;

    // ── UC34 Factory ────────────────────────────────────────────────

    static final UUID MOTHER_ID        = UUID.fromString("00000000-0000-0000-0000-000000000034");
    static final UUID BABY_ID          = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000034");
    static final UUID OTHER_MOTHER_ID  = UUID.fromString("00000000-0000-0000-0000-000000000099");
    static final UUID ARCHIVED_BABY_ID = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000088");
    static final UUID LOG_ID           = UUID.fromString("dddddddd-0000-0000-0000-000000000001");

    private BabyProfile makeActiveBaby() {
        return BabyProfile.builder()
                .id(BABY_ID)
                .ownerUserId(MOTHER_ID)
                .nickname("Baby Test")
                .status(BabyProfileStatus.ACTIVE)
                .build();
    }

    private BabyProfile makeArchivedBaby() {
        return BabyProfile.builder()
                .id(ARCHIVED_BABY_ID)
                .ownerUserId(MOTHER_ID)
                .nickname("Archived Baby")
                .status(BabyProfileStatus.ARCHIVED)
                .build();
    }

    private BabyProfile makeOtherMotherBaby() {
        return BabyProfile.builder()
                .id(BABY_ID)
                .ownerUserId(OTHER_MOTHER_ID)
                .nickname("Other Baby")
                .status(BabyProfileStatus.ACTIVE)
                .build();
    }

    private AddBabyDailyLogRequest makeFeedingRequest() {
        AddBabyDailyLogRequest req = new AddBabyDailyLogRequest();
        req.setLogType("FEEDING");
        req.setQuantity(new BigDecimal("120"));
        req.setUnit("ml");
        return req;
    }

    private AddBabyDailyLogRequest makeSleepRequest() {
        AddBabyDailyLogRequest req = new AddBabyDailyLogRequest();
        req.setLogType("SLEEP");
        req.setStartedAt(Instant.parse("2026-06-26T13:00:00Z"));
        req.setEndedAt(Instant.parse("2026-06-26T15:30:00Z"));
        return req;
    }

    private AddBabyDailyLogRequest makeDiaperRequest() {
        AddBabyDailyLogRequest req = new AddBabyDailyLogRequest();
        req.setLogType("DIAPER");
        return req;
    }

    private AddBabyDailyLogRequest makeFeverRequest() {
        AddBabyDailyLogRequest req = new AddBabyDailyLogRequest();
        req.setLogType("FEVER");
        req.setQuantity(new BigDecimal("38.5"));
        req.setUnit("celsius");
        req.setNote("Mild fever after vaccination");
        return req;
    }

    private BabyDailyLog makeRecentLog() {
        return BabyDailyLog.builder()
                .babyLogId(LOG_ID)
                .babyId(BABY_ID)
                .logType("FEEDING")
                .quantity(new BigDecimal("150"))
                .unit("ml")
                .recordedBy(MOTHER_ID)
                .createdAt(Instant.now().minus(Duration.ofHours(2)))
                .updatedAt(Instant.now().minus(Duration.ofHours(2)))
                .build();
    }

    private BabyDailyLog makeOldLog() {
        return BabyDailyLog.builder()
                .babyLogId(LOG_ID)
                .babyId(BABY_ID)
                .logType("SLEEP")
                .recordedBy(MOTHER_ID)
                .createdAt(Instant.now().minus(Duration.ofHours(25)))
                .updatedAt(Instant.now().minus(Duration.ofHours(25)))
                .build();
    }

    private Principal makePrincipal(UUID userId) {
        return userId::toString;
    }

    // ── UC34 Test Cases ─────────────────────────────────────────────

    // BABY-TC-034-001: Happy path FEEDING log -> 201
    @Test
    void addDailyLog_feedingWithQuantityAndUnit_returnsResponse() {
        when(babyProfileRepository.findById(BABY_ID)).thenReturn(Optional.of(makeActiveBaby()));
        BabyDailyLog saved = BabyDailyLog.builder()
                .babyLogId(LOG_ID).babyId(BABY_ID).logType("FEEDING")
                .quantity(new BigDecimal("120")).unit("ml").recordedBy(MOTHER_ID)
                .createdAt(Instant.now()).build();
        when(babyDailyLogRepository.save(any())).thenReturn(saved);

        AddBabyDailyLogResponse resp = service.addDailyLog(BABY_ID, makeFeedingRequest(), MOTHER_ID);

        assertThat(resp.getLogType()).isEqualTo("FEEDING");
        assertThat(resp.getQuantity()).isEqualByComparingTo("120");
        assertThat(resp.getUnit()).isEqualTo("ml");
        assertThat(resp.getRecordedBy()).isEqualTo(MOTHER_ID);
        verify(auditService).log(any(), eq(MOTHER_ID), anyString(), anyString(), any());
    }

    // BABY-TC-034-002: SLEEP log with time window -> 201
    @Test
    void addDailyLog_sleepWithTimeWindow_preservesTimestamps() {
        when(babyProfileRepository.findById(BABY_ID)).thenReturn(Optional.of(makeActiveBaby()));
        BabyDailyLog saved = BabyDailyLog.builder()
                .babyLogId(LOG_ID).babyId(BABY_ID).logType("SLEEP")
                .startedAt(Instant.parse("2026-06-26T13:00:00Z"))
                .endedAt(Instant.parse("2026-06-26T15:30:00Z"))
                .recordedBy(MOTHER_ID).createdAt(Instant.now()).build();
        when(babyDailyLogRepository.save(any())).thenReturn(saved);

        AddBabyDailyLogResponse resp = service.addDailyLog(BABY_ID, makeSleepRequest(), MOTHER_ID);

        assertThat(resp.getLogType()).isEqualTo("SLEEP");
        assertThat(resp.getStartedAt()).isEqualTo(Instant.parse("2026-06-26T13:00:00Z"));
        assertThat(resp.getEndedAt()).isEqualTo(Instant.parse("2026-06-26T15:30:00Z"));
    }

    // BABY-TC-034-003: DIAPER minimal quick entry -> 201
    @Test
    void addDailyLog_diaperMinimal_allOptionalFieldsNull() {
        when(babyProfileRepository.findById(BABY_ID)).thenReturn(Optional.of(makeActiveBaby()));
        BabyDailyLog saved = BabyDailyLog.builder()
                .babyLogId(LOG_ID).babyId(BABY_ID).logType("DIAPER")
                .recordedBy(MOTHER_ID).createdAt(Instant.now()).build();
        when(babyDailyLogRepository.save(any())).thenReturn(saved);

        AddBabyDailyLogResponse resp = service.addDailyLog(BABY_ID, makeDiaperRequest(), MOTHER_ID);

        assertThat(resp.getLogType()).isEqualTo("DIAPER");
        assertThat(resp.getQuantity()).isNull();
        assertThat(resp.getUnit()).isNull();
    }

    // BABY-TC-034-004: FEVER log with temperature -> 201
    @Test
    void addDailyLog_feverWithTemperature_returnsCorrectData() {
        when(babyProfileRepository.findById(BABY_ID)).thenReturn(Optional.of(makeActiveBaby()));
        BabyDailyLog saved = BabyDailyLog.builder()
                .babyLogId(LOG_ID).babyId(BABY_ID).logType("FEVER")
                .quantity(new BigDecimal("38.5")).unit("celsius")
                .note("Mild fever after vaccination")
                .recordedBy(MOTHER_ID).createdAt(Instant.now()).build();
        when(babyDailyLogRepository.save(any())).thenReturn(saved);

        AddBabyDailyLogResponse resp = service.addDailyLog(BABY_ID, makeFeverRequest(), MOTHER_ID);

        assertThat(resp.getLogType()).isEqualTo("FEVER");
        assertThat(resp.getQuantity()).isEqualByComparingTo("38.5");
        assertThat(resp.getNote()).isEqualTo("Mild fever after vaccination");
    }

    // BABY-TC-034-005: Baby not owned -> throws AccessDeniedBusinessException
    @Test
    void addDailyLog_babyNotOwned_throwsForbidden() {
        when(babyProfileRepository.findById(BABY_ID)).thenReturn(Optional.of(makeOtherMotherBaby()));

        assertThatThrownBy(() -> service.addDailyLog(BABY_ID, makeFeedingRequest(), MOTHER_ID))
                .isInstanceOf(AccessDeniedBusinessException.class);
    }

    // BABY-TC-034-006: Baby archived -> throws BusinessException BABY-032
    @Test
    void addDailyLog_babyArchived_throwsBadRequest() {
        when(babyProfileRepository.findById(ARCHIVED_BABY_ID)).thenReturn(Optional.of(makeArchivedBaby()));

        assertThatThrownBy(() -> service.addDailyLog(ARCHIVED_BABY_ID, makeFeedingRequest(), MOTHER_ID))
                .isInstanceOf(BusinessException.class);
    }

    // BABY-TC-034-007: Baby not found -> throws ResourceNotFoundException
    @Test
    void addDailyLog_babyNotFound_throwsNotFound() {
        when(babyProfileRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.addDailyLog(BABY_ID, makeFeedingRequest(), MOTHER_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // BABY-TC-034-008: recorded_by must be set from JWT (C3 constraint)
    @Test
    void addDailyLog_recordedBySetFromJwt_notFromRequestBody() {
        when(babyProfileRepository.findById(BABY_ID)).thenReturn(Optional.of(makeActiveBaby()));
        BabyDailyLog saved = BabyDailyLog.builder()
                .babyLogId(LOG_ID).babyId(BABY_ID).logType("DIAPER")
                .recordedBy(MOTHER_ID).createdAt(Instant.now()).build();
        when(babyDailyLogRepository.save(any())).thenAnswer(inv -> {
            BabyDailyLog logArg = inv.getArgument(0);
            assertThat(logArg.getRecordedBy())
                    .as("recorded_by MUST be set from JWT userId, not request body")
                    .isEqualTo(MOTHER_ID);
            return saved;
        });

        service.addDailyLog(BABY_ID, makeDiaperRequest(), MOTHER_ID);
    }

    // ── UC35 Test Cases ─────────────────────────────────────────────

    // BABY-TC-035-001: Update log within 24h -> success
    @Test
    void updateLog_withinEditWindow_returnsUpdatedResponse() {
        BabyDailyLog existing = makeRecentLog();
        when(babyProfileRepository.findById(BABY_ID)).thenReturn(Optional.of(makeActiveBaby()));
        when(babyDailyLogRepository.findById(LOG_ID)).thenReturn(Optional.of(existing));
        BabyDailyLog updated = BabyDailyLog.builder()
                .babyLogId(LOG_ID).babyId(BABY_ID).logType("FEEDING")
                .quantity(new BigDecimal("180")).unit("ml")
                .note("Updated amount").recordedBy(MOTHER_ID)
                .createdAt(existing.getCreatedAt()).updatedAt(Instant.now()).build();
        when(babyDailyLogRepository.save(any())).thenReturn(updated);

        UpdateBabyDailyLogRequest req = new UpdateBabyDailyLogRequest();
        req.setQuantity(new BigDecimal("180"));
        req.setUnit("ml");
        req.setNote("Updated amount");

        BabyDailyLogResponse resp = service.updateLog(BABY_ID, LOG_ID, req, makePrincipal(MOTHER_ID));

        assertThat(resp.getQuantity()).isEqualByComparingTo("180");
        assertThat(resp.getNote()).isEqualTo("Updated amount");
        assertThat(resp.getLogType()).isEqualTo("FEEDING");
        verify(auditService).log(any(), eq(MOTHER_ID), anyString(), anyString(), any());
    }

    // BABY-TC-035-002 / UC195: Delete log -> soft-delete success
    @Test
    void deleteLog_softDeletesSuccessfully() {
        BabyDailyLog existing = makeRecentLog();
        when(babyDailyLogRepository.findByBabyLogIdAndStatus(LOG_ID, BabyDailyLogStatus.ACTIVE))
                .thenReturn(Optional.of(existing));
        when(babyProfileRepository.findById(BABY_ID)).thenReturn(Optional.of(makeActiveBaby()));
        when(babyAccessPolicy.canManage(any(), eq(MOTHER_ID))).thenReturn(true);

        service.deleteLog(BABY_ID, LOG_ID, makePrincipal(MOTHER_ID));

        verify(babyDailyLogRepository).save(argThat(log -> BabyDailyLogStatus.DELETED.equals(log.getStatus())));
        verify(babyDailyLogRepository, never()).deleteById(LOG_ID);
        verify(auditService).log(eq(AuditAction.BABY_DAILY_LOG_DELETED), eq(MOTHER_ID),
                eq("BabyDailyLog"), eq(LOG_ID.toString()), any());
    }

    // BABY-TC-035-003: Edit window expired -> throws BusinessException BABY-042
    @Test
    void updateLog_editWindowExpired_throwsBadRequest() {
        BabyDailyLog oldLog = makeOldLog();
        when(babyProfileRepository.findById(BABY_ID)).thenReturn(Optional.of(makeActiveBaby()));
        when(babyDailyLogRepository.findById(LOG_ID)).thenReturn(Optional.of(oldLog));

        UpdateBabyDailyLogRequest req = new UpdateBabyDailyLogRequest();
        req.setQuantity(new BigDecimal("200"));

        assertThatThrownBy(() -> service.updateLog(BABY_ID, LOG_ID, req, makePrincipal(MOTHER_ID)))
                .isInstanceOf(BusinessException.class);
    }

    // BABY-TC-035-004: Log belongs to different baby -> throws ResourceNotFoundException BABY-041
    @Test
    void updateLog_logNotBelongingToBaby_throwsNotFound() {
        UUID otherBabyId = UUID.fromString("cccccccc-0000-0000-0000-000000000001");
        BabyDailyLog logForOtherBaby = BabyDailyLog.builder()
                .babyLogId(LOG_ID).babyId(otherBabyId).logType("FEEDING")
                .recordedBy(MOTHER_ID)
                .createdAt(Instant.now().minus(Duration.ofHours(1)))
                .updatedAt(Instant.now().minus(Duration.ofHours(1))).build();

        BabyProfile baby = BabyProfile.builder()
                .id(BABY_ID).ownerUserId(MOTHER_ID).status(BabyProfileStatus.ACTIVE).nickname("Test").build();
        when(babyProfileRepository.findById(BABY_ID)).thenReturn(Optional.of(baby));
        when(babyDailyLogRepository.findById(LOG_ID)).thenReturn(Optional.of(logForOtherBaby));

        UpdateBabyDailyLogRequest req = new UpdateBabyDailyLogRequest();
        assertThatThrownBy(() -> service.updateLog(BABY_ID, LOG_ID, req, makePrincipal(MOTHER_ID)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // BABY-TC-035-005: Baby not owned -> throws AccessDeniedBusinessException BABY-043
    @Test
    void updateLog_babyNotOwned_throwsForbidden() {
        when(babyProfileRepository.findById(BABY_ID)).thenReturn(Optional.of(makeOtherMotherBaby()));

        UpdateBabyDailyLogRequest req = new UpdateBabyDailyLogRequest();
        assertThatThrownBy(() -> service.updateLog(BABY_ID, LOG_ID, req, makePrincipal(MOTHER_ID)))
                .isInstanceOf(AccessDeniedBusinessException.class);
    }

    // BABY-TC-035-006: Log not found -> throws ResourceNotFoundException BABY-040
    @Test
    void updateLog_logNotFound_throwsNotFound() {
        when(babyProfileRepository.findById(BABY_ID)).thenReturn(Optional.of(makeActiveBaby()));
        when(babyDailyLogRepository.findById(LOG_ID)).thenReturn(Optional.empty());

        UpdateBabyDailyLogRequest req = new UpdateBabyDailyLogRequest();
        assertThatThrownBy(() -> service.updateLog(BABY_ID, LOG_ID, req, makePrincipal(MOTHER_ID)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // BABY-TC-035-007: log_type immutable - not changed on update (C4 constraint)
    @Test
    void updateLog_logTypeImmutable_notChangedAfterUpdate() {
        BabyDailyLog existing = makeRecentLog(); // logType = "FEEDING"
        when(babyProfileRepository.findById(BABY_ID)).thenReturn(Optional.of(makeActiveBaby()));
        when(babyDailyLogRepository.findById(LOG_ID)).thenReturn(Optional.of(existing));
        when(babyDailyLogRepository.save(any())).thenAnswer(inv -> {
            BabyDailyLog arg = inv.getArgument(0);
            assertThat(arg.getLogType())
                    .as("log_type must remain FEEDING — immutable per ADR-BABY-005-003")
                    .isEqualTo("FEEDING");
            return arg;
        });

        UpdateBabyDailyLogRequest req = new UpdateBabyDailyLogRequest();
        req.setNote("Updated note");
        service.updateLog(BABY_ID, LOG_ID, req, makePrincipal(MOTHER_ID));
    }

    // BABY-TC-035-008: recorded_by preserved on update (C6 constraint)
    @Test
    void updateLog_recordedByPreserved_notOverwrittenByCurrentUser() {
        BabyDailyLog existing = makeRecentLog(); // recordedBy = MOTHER_ID
        when(babyProfileRepository.findById(BABY_ID)).thenReturn(Optional.of(makeActiveBaby()));
        when(babyDailyLogRepository.findById(LOG_ID)).thenReturn(Optional.of(existing));
        when(babyDailyLogRepository.save(any())).thenAnswer(inv -> {
            BabyDailyLog arg = inv.getArgument(0);
            assertThat(arg.getRecordedBy())
                    .as("recorded_by must remain original — not overwritten by current user")
                    .isEqualTo(MOTHER_ID);
            return arg;
        });

        UpdateBabyDailyLogRequest req = new UpdateBabyDailyLogRequest();
        req.setNote("Updated note");
        service.updateLog(BABY_ID, LOG_ID, req, makePrincipal(MOTHER_ID));
    }

    // UC194: View active daily log detail
    @Test
    void getDailyLogDetail_ownerCanView_returnsResponse() {
        BabyDailyLog existing = makeRecentLog();
        when(babyDailyLogRepository.findByBabyLogIdAndStatus(LOG_ID, BabyDailyLogStatus.ACTIVE))
                .thenReturn(Optional.of(existing));
        when(babyProfileRepository.findById(BABY_ID)).thenReturn(Optional.of(makeActiveBaby()));
        when(babyAccessPolicy.canView(any(), eq(MOTHER_ID))).thenReturn(true);

        BabyDailyLogResponse response = service.getDailyLogDetail(BABY_ID, LOG_ID, makePrincipal(MOTHER_ID));

        assertThat(response.getBabyLogId()).isEqualTo(LOG_ID);
        assertThat(response.getBabyId()).isEqualTo(BABY_ID);
        verify(auditService, never()).log(any(), any(), anyString(), anyString(), any());
    }

    // UC195: Delete writes status and retains row
    @Test
    void deleteLog_savesDeletedStatusAndDoesNotHardDelete() {
        BabyDailyLog existing = makeRecentLog();
        when(babyDailyLogRepository.findByBabyLogIdAndStatus(LOG_ID, BabyDailyLogStatus.ACTIVE))
                .thenReturn(Optional.of(existing));
        when(babyProfileRepository.findById(BABY_ID)).thenReturn(Optional.of(makeActiveBaby()));
        when(babyAccessPolicy.canManage(any(), eq(MOTHER_ID))).thenReturn(true);

        service.deleteLog(BABY_ID, LOG_ID, makePrincipal(MOTHER_ID));

        verify(babyDailyLogRepository).save(argThat(log -> BabyDailyLogStatus.DELETED.equals(log.getStatus())));
        verify(babyDailyLogRepository, never()).delete(any());
        verify(babyDailyLogRepository, never()).deleteById(any());
    }
}
