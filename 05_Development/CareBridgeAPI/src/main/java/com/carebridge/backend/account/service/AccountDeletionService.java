package com.carebridge.backend.account.service;

import com.carebridge.backend.account.dto.request.RequestAccountDeletionRequest;
import com.carebridge.backend.account.dto.response.AccountDeletionRequestResponse;
import java.util.UUID;

public interface AccountDeletionService {

    AccountDeletionRequestResponse requestDeletion(UUID userId, RequestAccountDeletionRequest request);

    AccountDeletionRequestResponse cancelDeletion(UUID userId);
}
