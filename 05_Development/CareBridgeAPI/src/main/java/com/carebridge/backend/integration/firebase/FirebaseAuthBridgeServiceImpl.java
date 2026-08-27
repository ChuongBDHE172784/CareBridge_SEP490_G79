package com.carebridge.backend.integration.firebase;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

// BR-DCC-013: the ONLY input to this class is careBridgeUserId, always sourced by the
// caller (FirebaseTokenController) from the current JWT — never from request body/param.
@Service
@RequiredArgsConstructor
public class FirebaseAuthBridgeServiceImpl implements IFirebaseAuthBridgeService {

    private final IFirebaseAuthGateway gateway;

    @Override
    public String createCustomToken(UUID careBridgeUserId) {
        return gateway.createCustomToken(careBridgeUserId.toString());
    }
}
