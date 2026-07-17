package com.carebridge.backend.integration.firebase;

import java.time.Instant;
import java.util.Map;

/**
 * Thin boundary around the Firebase Admin SDK's Cloud Firestore client — isolates the
 * hard-to-unit-test static-factory SDK calls (mocking chained final SDK types has poor
 * signal) so {@link ConversationEventPublisherImpl}'s actual business logic (recipient
 * resolution, enable/disable flag, payload shape) stays fully unit-testable via this
 * plain interface.
 */
public interface IFirebaseRealtimeGateway {

    /** May throw — caller is responsible for the best-effort/no-rollback contract (BR-DCC-007). */
    void write(String path, Map<String, Object> payload);

    /** ADR-DCC-006: deletes inbox event documents older than cutoff in bounded batches. */
    void purgeEventsOlderThan(Instant cutoff);
}
