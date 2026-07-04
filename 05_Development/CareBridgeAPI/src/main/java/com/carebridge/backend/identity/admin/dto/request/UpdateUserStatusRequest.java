package com.carebridge.backend.identity.admin.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * UC114 Manage User Accounts — status-mutation input DTO. Nullable fields mean
 * "leave unchanged" (TDS §8.1). Never carries a `role` field — UC116's exclusive
 * write surface (TDS §1.1 non-overlap design).
 */
@Getter
@Setter
public class UpdateUserStatusRequest {

    private Boolean enabled;

    private Boolean locked;

    @Size(max = 500)
    private String reason;
}
