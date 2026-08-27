package com.carebridge.backend.family.service;

import com.carebridge.backend.family.dto.FamilyAlertListResponse;

import java.util.UUID;

public interface IFamilyAlertService {

    /**
     * UC-86: Returns a paginated list of family alerts (EMERGENCY notifications) for the caller.
     * Caller must be authenticated (BR-RBAC). Consent-minimized: only title and body surfaced.
     *
     * @throws com.carebridge.backend.common.exception.BusinessException (FAM-003/403) caller not ACCEPTED member of any group
     */
    FamilyAlertListResponse listFamilyAlerts(UUID callerId, int page, int size);
}
