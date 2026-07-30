package com.carebridge.backend.family.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.AssertTrue;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JoinCareGroupRequest {
    @NotBlank(message = "Mã mời không được để trống")
    private String code;

    @NotBlank(message = "Vai trò trong gia đình không được để trống")
    private String familyRelationshipRole;

    private String customFamilyRelationshipRole;

    @AssertTrue(message = "Vai trò tùy chỉnh là bắt buộc khi chọn Khác")
    public boolean isCustomRoleValid() {
        return !"KHAC".equalsIgnoreCase(familyRelationshipRole)
                || (customFamilyRelationshipRole != null && !customFamilyRelationshipRole.isBlank());
    }
}
