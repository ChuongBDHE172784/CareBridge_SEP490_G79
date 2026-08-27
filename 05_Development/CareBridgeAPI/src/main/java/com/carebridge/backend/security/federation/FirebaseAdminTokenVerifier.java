package com.carebridge.backend.security.federation;

import com.carebridge.backend.security.exception.FederatedAuthException;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;

@Component
@ConditionalOnBean(FirebaseApp.class)
@RequiredArgsConstructor
public class FirebaseAdminTokenVerifier implements FirebaseTokenVerifier {
    private final FirebaseApp firebaseApp;

    @Override
    public VerifiedFederatedIdentity verify(String idToken) {
        try {
            FirebaseToken token = FirebaseAuth.getInstance(firebaseApp).verifyIdToken(idToken, true);
            Map<String, Object> firebase = claimMap(token.getClaims().get("firebase"));
            String signInProvider = String.valueOf(firebase.get("sign_in_provider"));
            FederatedProvider provider = switch (signInProvider) {
                case "google.com" -> FederatedProvider.GOOGLE;
                case "phone" -> FederatedProvider.PHONE;
                default -> throw FederatedAuthException.unsupportedProvider();
            };
            String phone = stringClaim(token.getClaims().get("phone_number"));
            return new VerifiedFederatedIdentity(provider, token.getUid(), token.getEmail(), phone,
                    token.getName(), token.isEmailVerified(), provider == FederatedProvider.PHONE && phone != null);
        } catch (FirebaseAuthException e) {
            throw FederatedAuthException.invalidProof();
        } catch (FederatedAuthException e) {
            throw e;
        } catch (RuntimeException e) {
            throw FederatedAuthException.unavailable();
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> claimMap(Object value) {
        return value instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of();
    }

    private String stringClaim(Object value) {
        return value instanceof String text && !text.isBlank() ? text : null;
    }
}
