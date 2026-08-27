package com.carebridge.backend.checklist.distribution;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.carebridge.backend.testsupport.AbstractEmbeddedPostgresIntegrationTest;
import com.carebridge.backend.testsupport.CanonicalUserFixture;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

class ChecklistPersonalScopeMigrationEmbeddedPostgresTest
        extends AbstractEmbeddedPostgresIntegrationTest {

    @Autowired JdbcTemplate jdbcTemplate;

    private UUID ownerId;
    private UUID familyId;
    private UUID careSubjectId;
    private UUID journeyId;
    private UUID careGroupId;

    @BeforeEach
    void setUp() {
        ownerId = UUID.randomUUID();
        familyId = UUID.randomUUID();
        careSubjectId = UUID.randomUUID();
        journeyId = UUID.randomUUID();
        careGroupId = UUID.randomUUID();

        CanonicalUserFixture.insertUser(
                jdbcTemplate, ownerId, "Personal checklist owner", uniquePhone("091"), "MOTHER");
        CanonicalUserFixture.insertUser(
                jdbcTemplate, familyId, "Personal checklist family", uniquePhone("092"), "FAMILY");
        jdbcTemplate.update("""
                insert into care_subjects (
                    care_subject_id, person_id, owner_user_id, subject_type,
                    nickname, status, created_at, updated_at)
                values (?, ?, ?, 'MOTHER', 'Personal checklist owner', 'ACTIVE', now(), now())
                """, careSubjectId, ownerId, ownerId);
        jdbcTemplate.update("""
                insert into mother_journeys (
                    journey_id, owner_user_id, care_subject_id, journey_type,
                    start_date, status, version, created_at, updated_at)
                values (?, ?, ?, 'PRE_PREGNANCY', current_date, 'ACTIVE', 0, now(), now())
                """, journeyId, ownerId, careSubjectId);
        jdbcTemplate.update("""
                insert into care_groups (
                    care_group_id, owner_user_id, group_name, linked_journey_id,
                    status, created_at, updated_at)
                values (?, ?, 'Optional family sharing', ?, 'ACTIVE', now(), now())
                """, careGroupId, ownerId, journeyId);
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.update("delete from checklist_task_instances where checklist_instance_id in "
                + "(select checklist_instance_id from checklist_instances where recipient_user_id in (?, ?))",
                ownerId, familyId);
        jdbcTemplate.update("delete from checklist_instances where recipient_user_id in (?, ?)", ownerId, familyId);
        jdbcTemplate.update("delete from checklist_care_group_contexts where care_group_id=?", careGroupId);
        jdbcTemplate.update("delete from care_group_members where care_group_id=?", careGroupId);
        jdbcTemplate.update("delete from care_groups where care_group_id=?", careGroupId);
        jdbcTemplate.update("delete from mother_journeys where journey_id=?", journeyId);
        jdbcTemplate.update("delete from care_subjects where care_subject_id=?", careSubjectId);
        jdbcTemplate.update("delete from users where user_id in (?, ?)", ownerId, familyId);
    }

    @Test
    void motherInstanceAndChildTaskCanPersistWithoutCareGroup() {
        UUID instanceId = insertPersonalMotherInstance(ownerId, journeyId, "1".repeat(64));
        UUID taskId = UUID.randomUUID();

        int inserted = jdbcTemplate.update("""
                insert into checklist_task_instances (
                    checklist_task_instance_id, checklist_instance_id, task_key, key_version,
                    title_snapshot, display_order, is_required, target_subject,
                    status, lock_version, created_at, updated_at)
                values (?, ?, ?, 'v1', 'Personal task', 1, true, 'MOTHER',
                        'PENDING', 0, now(), now())
                """, taskId, instanceId, "2".repeat(64));

        assertThat(inserted).isOne();
        assertThat(jdbcTemplate.queryForObject(
                "select care_group_id from checklist_instances where checklist_instance_id=?",
                UUID.class, instanceId)).isNull();
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from checklist_task_instances where checklist_instance_id=?",
                Integer.class, instanceId)).isOne();
    }

    @Test
    void personalInstanceCannotSpoofCanonicalContextOwner() {
        assertThatThrownBy(() -> insertPersonalMotherInstance(familyId, journeyId, "3".repeat(64)))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("checklist_instances_personal_context_authority_fk");
    }

    @Test
    void familyInstanceCannotPersistWithoutCareGroup() {
        assertThatThrownBy(() -> jdbcTemplate.update("""
                insert into checklist_instances (
                    checklist_instance_id, distribution_key, key_version, recipient_user_id,
                    recipient_role, care_group_id, care_context_type, care_context_id,
                    context_owner_user_id, origin, status, lock_version, created_at, updated_at)
                values (?, ?, 'v1', ?, 'FAMILY', null, 'JOURNEY', ?, ?,
                        'USER_CREATED', 'PENDING', 0, now(), now())
                """, UUID.randomUUID(), "4".repeat(64), familyId, journeyId, ownerId))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("CHECKLIST_FAMILY_RECIPIENT_NOT_AUTHORIZED");

        String definition = jdbcTemplate.queryForObject("""
                select pg_get_constraintdef(oid)
                from pg_constraint
                where conrelid='public.checklist_instances'::regclass
                  and conname='checklist_instances_family_group_scope_ck'
                """, String.class);
        assertThat(definition).contains("recipient_role", "FAMILY", "care_group_id IS NOT NULL");
    }

    @Test
    void changingPersonalInstanceToUnauthorizedFamilyScopeIsRejected() {
        UUID instanceId = insertPersonalMotherInstance(ownerId, journeyId, "5".repeat(64));

        assertThatThrownBy(() -> jdbcTemplate.update("""
                update checklist_instances
                   set recipient_role='FAMILY', recipient_user_id=?, care_group_id=?
                 where checklist_instance_id=?
                """, familyId, careGroupId, instanceId))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("CHECKLIST_FAMILY_RECIPIENT_NOT_AUTHORIZED");
    }

    private UUID insertPersonalMotherInstance(UUID recipientId, UUID contextId, String distributionKey) {
        UUID instanceId = UUID.randomUUID();
        jdbcTemplate.update("""
                insert into checklist_instances (
                    checklist_instance_id, distribution_key, key_version, recipient_user_id,
                    recipient_role, care_group_id, care_context_type, care_context_id,
                    context_owner_user_id, origin, status, lock_version, created_at, updated_at)
                values (?, ?, 'v1', ?, 'MOTHER', null, 'JOURNEY', ?, ?,
                        'USER_CREATED', 'PENDING', 0, now(), now())
                """, instanceId, distributionKey, recipientId, contextId, recipientId);
        return instanceId;
    }

    private String uniquePhone(String prefix) {
        return prefix + String.format("%07d", Math.floorMod(UUID.randomUUID().hashCode(), 10_000_000));
    }
}
