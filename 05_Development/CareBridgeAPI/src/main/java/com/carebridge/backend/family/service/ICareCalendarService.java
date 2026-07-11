package com.carebridge.backend.family.service;

import com.carebridge.backend.family.dto.SharedCareCalendarResponse;

import java.time.Instant;
import java.util.UUID;

public interface ICareCalendarService {

    /**
     * Returns care tasks within the given date range for a care group, filtered by the
     * caller's permissions (ADR-FAM-003): OWNER sees all tasks; non-owner FAMILY members
     * see tasks only if their permission_json contains {"calendar": true}.
     *
     * @param groupId    the care group UUID
     * @param callerId   the authenticated user UUID
     * @param rangeStart inclusive start of the date range (UTC)
     * @param rangeEnd   inclusive end of the date range (UTC)
     * @return filtered calendar response — never null; items may be empty list
     *
     * @throws com.carebridge.backend.common.exception.BusinessException (FAM-005/404) care group not found
     * @throws com.carebridge.backend.common.exception.BusinessException (FAM-003/403) caller not ACCEPTED member
     * @throws com.carebridge.backend.common.exception.BusinessException (FAM-040/400) rangeEnd before rangeStart
     * @throws com.carebridge.backend.common.exception.BusinessException (FAM-041/403) non-owner without calendar permission
     */
    SharedCareCalendarResponse getCalendar(UUID groupId, UUID callerId, Instant rangeStart, Instant rangeEnd);
}
