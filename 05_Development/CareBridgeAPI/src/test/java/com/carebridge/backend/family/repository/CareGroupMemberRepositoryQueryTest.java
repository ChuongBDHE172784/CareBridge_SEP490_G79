package com.carebridge.backend.family.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Query;

class CareGroupMemberRepositoryQueryTest {

    @Test
    void emergencyContactQueryEnforcesEligibilityAndDeterministicPriorityOrder() throws Exception {
        Method method = CareGroupMemberRepository.class.getMethod(
                "findEmergencyContactUserIds", UUID.class);

        Query query = method.getAnnotation(Query.class);

        assertThat(query).isNotNull();
        assertThat(query.nativeQuery()).isTrue();
        assertThat(normalize(query.value()))
                .contains("join care_group_members cgm on cgm.care_group_id = cg.care_group_id")
                .contains("cg.owner_user_id = :owneruserid")
                .contains("cg.status = 'active'")
                .contains("cgm.invitation_status = 'accepted'")
                .contains("cgm.is_emergency_contact = true")
                .contains("group by cgm.user_id")
                .contains("order by min(cgm.emergency_contact_priority) asc nulls last, cgm.user_id asc");
    }

    private static String normalize(String sql) {
        return sql.replaceAll("\\s+", " ").trim().toLowerCase();
    }
}
