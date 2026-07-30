package com.carebridge.backend.identity.admin.service;

import com.carebridge.backend.identity.admin.dto.request.ReviewAccountLockAppealRequest;
import com.carebridge.backend.identity.admin.dto.request.SubmitAccountLockAppealRequest;
import com.carebridge.backend.identity.admin.dto.response.AccountLockAppealResponse;
import com.carebridge.backend.identity.admin.entity.AccountLockAppealStatus;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AccountLockAppealService {
    AccountLockAppealResponse submit(SubmitAccountLockAppealRequest request);
    Page<AccountLockAppealResponse> list(AccountLockAppealStatus status, Pageable pageable);
    AccountLockAppealResponse get(UUID appealId);
    AccountLockAppealResponse review(UUID reviewerId, UUID appealId, ReviewAccountLockAppealRequest request);
}
