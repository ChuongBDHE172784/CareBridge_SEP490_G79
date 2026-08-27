package com.carebridge.backend.identity.admin.repository;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.entity.AuditLog;
import com.carebridge.backend.identity.entity.UserSession;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
public class AdminUserMonitoringRepositoryImpl implements AdminUserMonitoringRepository {

    private static final Set<AuditAction> IDENTITY_GOVERNANCE_ACTIONS = EnumSet.of(
            AuditAction.USER_ACCOUNT_STATUS_CHANGED,
            AuditAction.STAFF_ACCOUNT_CREATED,
            AuditAction.ROLE_PERMISSION_UPDATED);

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Page<UserSession> findSessions(UUID userId, Pageable pageable) {
        TypedQuery<UserSession> query = entityManager.createQuery("""
                select s from UserSession s
                where s.userId = :userId
                order by coalesce(s.lastActivityAt, s.createdAt) desc
                """, UserSession.class);
        query.setParameter("userId", userId);
        query.setFirstResult(Math.toIntExact(pageable.getOffset()));
        query.setMaxResults(pageable.getPageSize());

        Long total = entityManager.createQuery(
                        "select count(s) from UserSession s where s.userId = :userId", Long.class)
                .setParameter("userId", userId)
                .getSingleResult();
        return new PageImpl<>(query.getResultList(), pageable, total);
    }

    @Override
    public Page<AuditLog> findActivity(UUID userId, Pageable pageable) {
        TypedQuery<AuditLog> query = entityManager.createQuery("""
                select a from AuditLog a
                where upper(a.entityType) = 'USER'
                  and a.entityId = :userId
                  and a.action in :actions
                order by a.createdAt desc
                """, AuditLog.class);
        query.setParameter("userId", userId);
        query.setParameter("actions", IDENTITY_GOVERNANCE_ACTIONS);
        query.setFirstResult(Math.toIntExact(pageable.getOffset()));
        query.setMaxResults(pageable.getPageSize());

        Long total = entityManager.createQuery("""
                        select count(a) from AuditLog a
                        where upper(a.entityType) = 'USER'
                          and a.entityId = :userId
                          and a.action in :actions
                        """, Long.class)
                .setParameter("userId", userId)
                .setParameter("actions", IDENTITY_GOVERNANCE_ACTIONS)
                .getSingleResult();
        return new PageImpl<>(query.getResultList(), pageable, total);
    }
}
