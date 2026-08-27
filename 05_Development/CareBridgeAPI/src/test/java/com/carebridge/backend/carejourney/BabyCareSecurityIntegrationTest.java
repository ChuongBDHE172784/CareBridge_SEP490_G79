package com.carebridge.backend.carejourney;

import com.carebridge.backend.baby.entity.BabyProfile;
import com.carebridge.backend.baby.entity.BabyProfileStatus;
import com.carebridge.backend.baby.repository.BabyProfileRepository;
import com.carebridge.backend.carejourney.entity.BabyDailyLog;
import com.carebridge.backend.carejourney.entity.BabyDailyLogStatus;
import com.carebridge.backend.carejourney.entity.GrowthMeasurement;
import com.carebridge.backend.carejourney.repository.BabyDailyLogRepository;
import com.carebridge.backend.carejourney.repository.GrowthMeasurementStore;
import com.carebridge.backend.family.entity.CareGroup;
import com.carebridge.backend.family.entity.CareGroupMember;
import com.carebridge.backend.family.entity.CareGroupStatus;
import com.carebridge.backend.family.entity.GroupMemberRole;
import com.carebridge.backend.family.entity.InviteStatus;
import com.carebridge.backend.family.repository.CareGroupMemberRepository;
import com.carebridge.backend.family.repository.CareGroupRepository;
import com.carebridge.backend.security.entity.User;
import com.carebridge.backend.security.jwt.JwtTokenProvider;
import com.carebridge.backend.security.rbac.Role;
import com.carebridge.backend.security.repository.UserRepository;
import com.carebridge.backend.testsupport.AbstractPostgresIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@Transactional
class BabyCareSecurityIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private BabyProfileRepository babyProfileRepository;
    @Autowired private BabyDailyLogRepository babyDailyLogRepository;
    @Autowired private GrowthMeasurementStore growthMeasurementStore;
    @Autowired private CareGroupRepository careGroupRepository;
    @Autowired private CareGroupMemberRepository memberRepository;
    @Autowired private JwtTokenProvider jwtTokenProvider;

    private User ownerA;
    private User ownerB;
    private User caregiver;
    private BabyProfile babyA;
    private BabyProfile babyB;

    @BeforeEach
    void setUp() {
        ownerA = saveUser("uc242.owner.a@test.com", Role.MOTHER);
        ownerB = saveUser("uc242.owner.b@test.com", Role.MOTHER);
        caregiver = saveUser("uc242.caregiver@test.com", Role.FAMILY);
        babyA = saveBaby(ownerA, "Baby A");
        babyB = saveBaby(ownerB, "Baby B");
    }

    @Test
    void acceptedFamilyCaregiver_canWriteThenRevocationIsRechecked() throws Exception {
        CareGroup group = careGroupRepository.saveAndFlush(CareGroup.builder()
                .ownerUserId(ownerA.getId())
                .groupName("Baby A care group")
                .linkedBabyProfileId(babyA.getId())
                .status(CareGroupStatus.ACTIVE)
                .build());
        CareGroupMember member = memberRepository.saveAndFlush(CareGroupMember.builder()
                .careGroupId(group.getId())
                .userId(caregiver.getId())
                .memberRole(GroupMemberRole.MEMBER)
                .inviteStatus(InviteStatus.ACCEPTED)
                .joinedAt(Instant.now())
                .permissionJson("{\"baby_view\":true,\"baby_journal_write\":true}")
                .build());

        mockMvc.perform(post("/api/v1/babies/{babyId}/daily-logs", babyA.getId())
                        .header("Authorization", bearer(caregiver))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"logType\":\"DIAPER\"}"))
                .andExpect(status().isCreated());
        long countAfterAllowedWrite = babyDailyLogRepository.findByBabyId(babyA.getId()).size();

        member.setPermissionJson("{\"baby_view\":true,\"baby_journal_write\":false}");
        memberRepository.saveAndFlush(member);

        mockMvc.perform(post("/api/v1/babies/{babyId}/daily-logs", babyA.getId())
                        .header("Authorization", bearer(caregiver))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"logType\":\"DIAPER\"}"))
                .andExpect(status().isForbidden());
        assertThat(babyDailyLogRepository.findByBabyId(babyA.getId())).hasSize((int) countAfterAllowedWrite);
    }

    @Test
    void journalChildIdFromBabyB_isNotReadableOrDeletableThroughBabyAPath() throws Exception {
        BabyDailyLog logB = babyDailyLogRepository.saveAndFlush(BabyDailyLog.builder()
                .babyId(babyB.getId())
                .logType("DIAPER")
                .recordedBy(ownerB.getId())
                .build());

        mockMvc.perform(get("/api/v1/babies/{babyId}/daily-logs/{logId}", babyA.getId(), logB.getBabyLogId())
                        .header("Authorization", bearer(ownerA)))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/v1/babies/{babyId}/daily-logs/{logId}", babyA.getId(), logB.getBabyLogId())
                        .header("Authorization", bearer(ownerA)))
                .andExpect(status().isNotFound());

        assertThat(babyDailyLogRepository.findById(logB.getBabyLogId()))
                .get()
                .extracting(BabyDailyLog::getStatus)
                .isEqualTo(BabyDailyLogStatus.ACTIVE);
    }

    @Test
    void jwtSubject_remainsRecordedByWhenIdentityHeaderIsSpoofed() throws Exception {
        mockMvc.perform(post("/api/v1/babies/{babyId}/daily-logs", babyA.getId())
                        .header("Authorization", bearer(ownerA))
                        .header("X-User-Id", ownerB.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"logType\":\"DIAPER\"}"))
                .andExpect(status().isCreated());

        assertThat(babyDailyLogRepository.findByBabyId(babyA.getId()))
                .singleElement()
                .extracting(BabyDailyLog::getRecordedBy)
                .isEqualTo(ownerA.getId());
    }

    @Test
    void growthChildIdFromBabyB_cannotBeMutatedThroughBabyAPath() throws Exception {
        GrowthMeasurement measurementB = growthMeasurementStore.save(GrowthMeasurement.builder()
                .babyId(babyB.getId())
                .measuredDate(LocalDate.of(2026, 7, 1))
                .weightKg(new BigDecimal("6.20"))
                .sourceType("HOME")
                .build());

        mockMvc.perform(patch("/api/v1/babies/{babyId}/growth-measurements/{measurementId}",
                        babyA.getId(), measurementB.getGrowthMeasurementId())
                        .header("Authorization", bearer(ownerA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"weightKg\":7.10}"))
                .andExpect(status().isForbidden());

        assertThat(growthMeasurementStore.findById(measurementB.getGrowthMeasurementId()))
                .get()
                .extracting(GrowthMeasurement::getWeightKg)
                .isEqualTo(new BigDecimal("6.20"));
    }

    @Test
    void growthFromBabyB_isNotDeletedOrExposedThroughBabyAHistory() throws Exception {
        GrowthMeasurement measurementB = growthMeasurementStore.save(GrowthMeasurement.builder()
                .babyId(babyB.getId())
                .measuredDate(LocalDate.of(2026, 7, 2))
                .heightCm(new BigDecimal("61.50"))
                .sourceType("HOME")
                .build());

        mockMvc.perform(delete("/api/v1/babies/{babyId}/growth-measurements/{measurementId}",
                        babyA.getId(), measurementB.getGrowthMeasurementId())
                        .header("Authorization", bearer(ownerA)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/babies/{babyId}/growth-measurements", babyA.getId())
                        .header("Authorization", bearer(ownerA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(0));
        mockMvc.perform(get("/api/v1/babies/{babyId}/growth-measurements", babyB.getId())
                        .header("Authorization", bearer(ownerA)))
                .andExpect(status().isForbidden());

        assertThat(growthMeasurementStore.findById(measurementB.getGrowthMeasurementId()))
                .get()
                .extracting(GrowthMeasurement::getDeletedAt)
                .isNull();
    }

    @Test
    void dailyLogSummary_isScopedToRequestedBabyAndRejectsForeignBaby() throws Exception {
        babyDailyLogRepository.saveAndFlush(BabyDailyLog.builder()
                .babyId(babyA.getId())
                .logType("DIAPER")
                .recordedBy(ownerA.getId())
                .build());
        babyDailyLogRepository.saveAndFlush(BabyDailyLog.builder()
                .babyId(babyB.getId())
                .logType("DIAPER")
                .recordedBy(ownerB.getId())
                .build());

        mockMvc.perform(get("/api/v1/babies/{babyId}/daily-logs/summary", babyA.getId())
                        .param("period", "24h")
                        .header("Authorization", bearer(ownerA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.babyId").value(babyA.getId().toString()))
                .andExpect(jsonPath("$.data.summaries.DIAPER.count").value(1));
        mockMvc.perform(get("/api/v1/babies/{babyId}/daily-logs/summary", babyB.getId())
                        .param("period", "24h")
                        .header("Authorization", bearer(ownerA)))
                .andExpect(status().isForbidden());
    }

    @Test
    void dailyLogCollection_returnsOnlyActiveLogsForAuthorizedBaby() throws Exception {
        babyDailyLogRepository.saveAndFlush(BabyDailyLog.builder()
                .babyId(babyA.getId())
                .logType("SYMPTOM")
                .recordedBy(ownerA.getId())
                .build());
        babyDailyLogRepository.saveAndFlush(BabyDailyLog.builder()
                .babyId(babyA.getId())
                .logType("DIAPER")
                .recordedBy(ownerA.getId())
                .status(BabyDailyLogStatus.DELETED)
                .build());

        mockMvc.perform(get("/api/v1/babies/{babyId}/daily-logs", babyA.getId())
                        .header("Authorization", bearer(ownerA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].logType").value("SYMPTOM"));
    }

    @ParameterizedTest
    @EnumSource(value = InviteStatus.class, names = {"PENDING", "EXPIRED"})
    void nonAcceptedFamilyCaregiver_cannotWriteJournal(InviteStatus inviteStatus) throws Exception {
        saveCaregiverMembership(inviteStatus,
                "{\"baby_view\":true,\"baby_journal_write\":true,\"baby_growth_write\":true}");

        mockMvc.perform(post("/api/v1/babies/{babyId}/daily-logs", babyA.getId())
                        .header("Authorization", bearer(caregiver))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"logType\":\"DIAPER\"}"))
                .andExpect(status().isForbidden());

        assertThat(babyDailyLogRepository.findByBabyId(babyA.getId())).isEmpty();
    }

    @Test
    void compositeReadModels_areBabyScoped() throws Exception {
        mockMvc.perform(get("/api/v1/babies/{babyId}/care-overview", babyA.getId())
                        .header("Authorization", bearer(ownerA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.babyId").value(babyA.getId().toString()));
        mockMvc.perform(get("/api/v1/babies/{babyId}/care-timeline", babyA.getId())
                        .header("Authorization", bearer(ownerA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.babyId").value(babyA.getId().toString()));
        mockMvc.perform(get("/api/v1/babies/{babyId}/appointment-preparation-summary", babyA.getId())
                        .header("Authorization", bearer(ownerA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.babyId").value(babyA.getId().toString()));
        mockMvc.perform(get("/api/v1/babies/{babyId}/care-overview", babyB.getId())
                        .header("Authorization", bearer(ownerA)))
                .andExpect(status().isForbidden());
    }

    private CareGroupMember saveCaregiverMembership(InviteStatus inviteStatus, String permissions) {
        CareGroup group = careGroupRepository.saveAndFlush(CareGroup.builder()
                .ownerUserId(ownerA.getId())
                .groupName("Baby A " + inviteStatus + " group")
                .linkedBabyProfileId(babyA.getId())
                .status(CareGroupStatus.ACTIVE)
                .build());
        return memberRepository.saveAndFlush(CareGroupMember.builder()
                .careGroupId(group.getId())
                .userId(caregiver.getId())
                .memberRole(GroupMemberRole.MEMBER)
                .inviteStatus(inviteStatus)
                .inviteExpiresAt(inviteStatus == InviteStatus.EXPIRED
                        ? Instant.now().minusSeconds(60)
                        : Instant.now().plusSeconds(3600))
                .permissionJson(permissions)
                .build());
    }

    private User saveUser(String email, Role role) {
        return userRepository.saveAndFlush(User.builder()
                .email(email)
                .role(role)
                .accountStatus("ACTIVE")
                .emailVerified(true)
                .phoneVerified(false)
                .enabled(true)
                .locked(false)
                .build());
    }

    private BabyProfile saveBaby(User owner, String nickname) {
        return babyProfileRepository.saveAndFlush(BabyProfile.builder()
                .ownerUserId(owner.getId())
                .nickname(nickname)
                .birthDate(LocalDate.of(2026, 1, 1))
                .status(BabyProfileStatus.ACTIVE)
                .active(true)
                .build());
    }

    private String bearer(User user) {
        return "Bearer " + jwtTokenProvider.generateAccessToken(user);
    }
}
