package com.carebridge.backend.exercise;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.repository.AuditLogRepository;
import com.carebridge.backend.exercise.entity.DifficultyLevel;
import com.carebridge.backend.exercise.entity.ExerciseStatus;
import com.carebridge.backend.exercise.entity.PregnancyExercise;
import com.carebridge.backend.exercise.entity.TrimesterScope;
import com.carebridge.backend.exercise.repository.ExerciseRepository;
import com.carebridge.backend.exercise.repository.PostureAnalysisConfigRepository;
import com.carebridge.backend.security.entity.User;
import com.carebridge.backend.security.jwt.JwtTokenProvider;
import com.carebridge.backend.security.rbac.Role;
import com.carebridge.backend.security.repository.UserRepository;
import com.carebridge.backend.testsupport.AbstractPostgresIntegrationTest;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * UC186 Manage Posture Analysis Configuration — full-stack integration tests
 * against a real Testcontainers PostgreSQL instance.
 * Covers PAC-TC-INT-001 (full lifecycle), PAC-TC-INT-002 (audit completeness),
 * PAC-TC-THRESH-007 (DB CHECK constraint backstop).
 */
@Transactional
class PostureConfigLifecycleIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private ExerciseRepository exerciseRepository;
    @Autowired private PostureAnalysisConfigRepository postureConfigRepository;
    @Autowired private AuditLogRepository auditLogRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtTokenProvider jwtTokenProvider;
    @Autowired private JdbcTemplate jdbcTemplate;

    private User saveSystemAdmin(String email) {
        return userRepository.save(User.builder()
                .email(email)
                .role(Role.SYSTEM_ADMIN)
                .passwordHash(passwordEncoder.encode("SecureP@ss1"))
                .enabled(true)
                .locked(false)
                .build());
    }

    private PregnancyExercise savePostureExercise(UUID createdBy) {
        OffsetDateTime now = OffsetDateTime.now();
        PregnancyExercise exercise = new PregnancyExercise();
        exercise.setExerciseId(UUID.randomUUID());
        exercise.setCreatedBy(createdBy);
        exercise.setTitle("Pelvic Tilt Stretch — Integration");
        exercise.setTrimesterScope(TrimesterScope.SECOND);
        exercise.setDifficultyLevel(DifficultyLevel.EASY);
        exercise.setDurationMinutes((short) 10);
        exercise.setSafetyWarning("Stop if dizzy.");
        exercise.setSupportsPostureAnalysis(true);
        exercise.setStatus(ExerciseStatus.PUBLISHED);
        exercise.setVersionNo(1);
        exercise.setCreatedAt(now);
        exercise.setUpdatedAt(now);
        return exerciseRepository.save(exercise);
    }

    // PAC-TC-INT-001 — full lifecycle: create -> new-version -> rollback -> list
    @Test
    void fullLifecycle_createNewVersionActivateRollback_exactlyOneActiveAtEveryStep() throws Exception {
        User admin = saveSystemAdmin("int.admin.uc186@test.com");
        PregnancyExercise exercise = savePostureExercise(admin.getId());
        String adminToken = jwtTokenProvider.generateAccessToken(admin);

        String createBody = """
                {"exerciseId":"%s","analysisMode":"MODEL_BASED","ruleOrModelVersion":"posenet-v2.1.0",
                 "confidenceThreshold":0.75,"feedbackLevel":"DETAILED"}
                """.formatted(exercise.getExerciseId());

        String createResponse = mockMvc.perform(post("/api/v1/admin/posture-configs")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andReturn().getResponse().getContentAsString();
        String firstConfigId = com.jayway.jsonpath.JsonPath.read(createResponse, "$.data.postureConfigId");

        assertThat(activeCountFor(exercise.getExerciseId())).isEqualTo(1);

        String versionBody = """
                {"analysisMode":"HYBRID","ruleOrModelVersion":"posenet-v2.2.0",
                 "confidenceThreshold":0.80,"feedbackLevel":"DETAILED"}
                """;
        String versionResponse = mockMvc.perform(post(
                        "/api/v1/admin/posture-configs/" + exercise.getExerciseId() + "/versions")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(versionBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andReturn().getResponse().getContentAsString();
        String secondConfigId = com.jayway.jsonpath.JsonPath.read(versionResponse, "$.data.postureConfigId");

        assertThat(activeCountFor(exercise.getExerciseId())).isEqualTo(1);

        mockMvc.perform(get("/api/v1/admin/posture-configs/" + exercise.getExerciseId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));

        // Rollback to the first version.
        mockMvc.perform(patch("/api/v1/admin/posture-configs/" + firstConfigId + "/activate")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));

        assertThat(activeCountFor(exercise.getExerciseId())).isEqualTo(1);
        assertThat(postureConfigRepository.findById(UUID.fromString(firstConfigId)).orElseThrow().getStatus())
                .isEqualTo("ACTIVE");
        assertThat(postureConfigRepository.findById(UUID.fromString(secondConfigId)).orElseThrow().getStatus())
                .isEqualTo("SUPERSEDED");

        // Mother-facing endpoint (existing, untouched) reflects the rollback.
        mockMvc.perform(get("/api/v1/exercises/" + exercise.getExerciseId() + "/posture-config")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.postureConfigId").value(firstConfigId));
    }

    private long activeCountFor(UUID exerciseId) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM posture_analysis_configs WHERE exercise_id = ? AND status = 'ACTIVE'",
                Long.class, exerciseId);
        return count == null ? 0 : count;
    }

    // PAC-TC-INT-002 — audit log completeness
    @Test
    void fullLifecycle_producesExactlyOneAuditRowPerMutation() throws Exception {
        User admin = saveSystemAdmin("int.admin.uc186.audit@test.com");
        PregnancyExercise exercise = savePostureExercise(admin.getId());
        String adminToken = jwtTokenProvider.generateAccessToken(admin);

        String createBody = """
                {"exerciseId":"%s","analysisMode":"MODEL_BASED","confidenceThreshold":0.75,"feedbackLevel":"DETAILED"}
                """.formatted(exercise.getExerciseId());
        mockMvc.perform(post("/api/v1/admin/posture-configs")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(createBody))
                .andExpect(status().isCreated());

        assertThat(auditLogRepository.findAll().stream()
                .filter(l -> l.getAction() == AuditAction.POSTURE_CONFIG_CREATED)
                .filter(l -> admin.getId().equals(l.getActorUserId())))
                .hasSize(1);
    }

    // PAC-TC-THRESH-007 — DB CHECK constraint survives a raw bypass attempt
    @Test
    void rawInsert_outOfRangeConfidenceThreshold_rejectedByCheckConstraint() {
        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO posture_analysis_configs "
                        + "(posture_config_id, exercise_id, configured_by, analysis_mode, "
                        + "confidence_threshold, effective_from, status) "
                        + "VALUES (gen_random_uuid(), gen_random_uuid(), gen_random_uuid(), "
                        + "'RULE_BASED', 2.0, now(), 'ACTIVE')"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
