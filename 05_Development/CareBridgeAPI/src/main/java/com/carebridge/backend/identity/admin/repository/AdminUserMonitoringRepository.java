package com.carebridge.backend.identity.admin.repository;

import com.carebridge.backend.audit.entity.AuditLog;
import com.carebridge.backend.identity.entity.UserSession;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Read-only aggregate queries dedicated to the SYSTEM_ADMIN user-governance screen.
 *
 * <p>This repository intentionally isolates monitoring queries from the shared
 * UserSessionRepository and AuditLogRepository contracts, whose blast radius spans
 * authentication and auditing flows across the application.</p>
 */
public interface AdminUserMonitoringRepository {

    Page<UserSession> findSessions(UUID userId, Pageable pageable);

    Page<AuditLog> findActivity(UUID userId, Pageable pageable);
}
