package com.carebridge.backend.identity.admin.dto.request;

import com.carebridge.backend.security.rbac.Role;
import lombok.Getter;
import lombok.Setter;

/**
 * UC114 Manage User Accounts — search/filter query for the admin user list.
 * All fields are optional filters (equivalence-partitioned by field, per TDS §8.1).
 */
@Getter
@Setter
public class AdminUserSearchQuery {
    private String email;
    private String phone;
    private String name;
    private Role role;
    private Boolean enabled;
    private Boolean locked;
}
