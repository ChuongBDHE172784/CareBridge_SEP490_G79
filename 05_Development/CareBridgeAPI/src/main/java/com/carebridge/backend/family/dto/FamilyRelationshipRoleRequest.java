package com.carebridge.backend.family.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class FamilyRelationshipRoleRequest {
    @NotBlank(message = "Vai trò trong gia đình không được để trống")
    private String familyRelationshipRole;
    private String customFamilyRelationshipRole;

    @AssertTrue(message = "Vai trò tùy chỉnh là bắt buộc khi chọn Khác")
    public boolean isCustomRoleValid() {
        return !"KHAC".equalsIgnoreCase(familyRelationshipRole)
                || (customFamilyRelationshipRole != null && !customFamilyRelationshipRole.isBlank());
    }
}
