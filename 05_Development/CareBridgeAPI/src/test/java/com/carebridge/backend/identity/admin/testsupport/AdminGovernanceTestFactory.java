package com.carebridge.backend.identity.admin.testsupport;

import com.carebridge.backend.identity.admin.dto.request.AdminUserSearchQuery;
import com.carebridge.backend.identity.admin.dto.request.UpdateUserRoleRequest;
import com.carebridge.backend.identity.admin.dto.request.UpdateUserStatusRequest;
import com.carebridge.backend.identity.admin.dto.request.CreateStaffAccountRequest;
import com.carebridge.backend.security.entity.User;
import com.carebridge.backend.security.rbac.Role;
import java.time.Instant;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * CASE 2.0 — Props Isolation Pattern. Shared factory across UC114/UC115/UC116
 * (Admin Governance cluster). Every @Test builds its own instances via these
 * factory methods — no shared mutable fixtures across test methods.
 */
public final class AdminGovernanceTestFactory {

    private AdminGovernanceTestFactory() {
    }

    public static User makeSystemAdmin() {
        User u = new User();
        u.setId(UUID.randomUUID());
        u.setEmail("admin+" + UUID.randomUUID() + "@carebridge.dev");
        u.setPhone("+84900000001");
        u.setName("Test System Admin");
        u.setRole(Role.SYSTEM_ADMIN);
        u.setEnabled(true);
        u.setLocked(false);
        u.setPasswordHash("$2a$10$stubHashNeverAsserted");
        u.setCreatedAt(Instant.now());
        return u;
    }

    public static User makeSystemAdmin(Consumer<User> overrides) {
        User u = makeSystemAdmin();
        overrides.accept(u);
        return u;
    }

    public static User makePeerSystemAdmin() {
        return makeSystemAdmin(u -> u.setId(UUID.randomUUID()));
    }

    public static User makeUser(Role role) {
        User u = new User();
        u.setId(UUID.randomUUID());
        u.setEmail("user+" + UUID.randomUUID() + "@carebridge.dev");
        u.setPhone("+8490" + (1000000 + Math.abs(UUID.randomUUID().hashCode() % 8999999)));
        u.setName("Test " + role.name());
        u.setRole(role);
        u.setEnabled(true);
        u.setLocked(false);
        u.setPasswordHash("$2a$10$stubHashNeverAsserted");
        u.setCreatedAt(Instant.now());
        return u;
    }

    public static User makeUser(Role role, Consumer<User> overrides) {
        User u = makeUser(role);
        overrides.accept(u);
        return u;
    }

    public static UpdateUserStatusRequest makeStatusRequest(Consumer<UpdateUserStatusRequest> overrides) {
        UpdateUserStatusRequest r = new UpdateUserStatusRequest();
        r.setEnabled(false);
        r.setLocked(null);
        r.setReason("Suspected policy violation — pending review");
        overrides.accept(r);
        return r;
    }

    public static AdminUserSearchQuery makeSearchQuery(Consumer<AdminUserSearchQuery> overrides) {
        AdminUserSearchQuery q = new AdminUserSearchQuery();
        overrides.accept(q);
        return q;
    }

    public static CreateStaffAccountRequest makeCreateStaffRequest(Consumer<CreateStaffAccountRequest> overrides) {
        CreateStaffAccountRequest r = new CreateStaffAccountRequest();
        r.setEmail("new.staff+" + UUID.randomUUID() + "@carebridge.dev");
        r.setPhone("+84901112222");
        r.setName("Test Staff Member");
        r.setRole(Role.MODERATOR);
        overrides.accept(r);
        return r;
    }

    public static UpdateUserRoleRequest makeUpdateRoleRequest(Consumer<UpdateUserRoleRequest> overrides) {
        UpdateUserRoleRequest r = new UpdateUserRoleRequest();
        r.setNewRole(Role.CONTENT_ADMIN);
        r.setLockAccessRights(null);
        r.setReason("Promoted to content moderation team");
        overrides.accept(r);
        return r;
    }
}
