package com.carebridge.backend.family.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class GroupMemberRoleConverterTest {

    private final GroupMemberRoleConverter converter = new GroupMemberRoleConverter();

    @Test
    void legacyCaregiverLabelsMapToCanonicalRoles() {
        assertThat(converter.convertToEntityAttribute("PRIMARY_CAREGIVER"))
                .isEqualTo(GroupMemberRole.OWNER);
        assertThat(converter.convertToEntityAttribute("CO_CAREGIVER"))
                .isEqualTo(GroupMemberRole.MEMBER);
    }

    @Test
    void unknownLegacyRelationshipDefaultsToLeastPrivilegeViewer() {
        assertThat(converter.convertToEntityAttribute("relative"))
                .isEqualTo(GroupMemberRole.VIEWER);
        assertThat(converter.convertToDatabaseColumn(GroupMemberRole.VIEWER))
                .isEqualTo("VIEWER");
    }
}
