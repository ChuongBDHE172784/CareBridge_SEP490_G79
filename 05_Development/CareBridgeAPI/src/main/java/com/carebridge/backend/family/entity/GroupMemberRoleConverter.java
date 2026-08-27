package com.carebridge.backend.family.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Locale;

/** Maps legacy relationship labels to the canonical care-group authorization roles. */
@Converter
public class GroupMemberRoleConverter implements AttributeConverter<GroupMemberRole, String> {

    @Override
    public String convertToDatabaseColumn(GroupMemberRole role) {
        return role == null ? null : role.name();
    }

    @Override
    public GroupMemberRole convertToEntityAttribute(String value) {
        if (value == null || value.isBlank()) {
            return GroupMemberRole.VIEWER;
        }
        return switch (value.trim().toUpperCase(Locale.ROOT)) {
            case "OWNER", "PRIMARY_CAREGIVER" -> GroupMemberRole.OWNER;
            case "MEMBER", "CO_CAREGIVER" -> GroupMemberRole.MEMBER;
            case "VIEWER" -> GroupMemberRole.VIEWER;
            default -> GroupMemberRole.VIEWER;
        };
    }
}
