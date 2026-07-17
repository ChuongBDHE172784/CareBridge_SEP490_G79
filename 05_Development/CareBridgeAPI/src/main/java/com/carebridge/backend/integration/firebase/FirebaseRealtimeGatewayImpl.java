package com.carebridge.backend.integration.firebase;

import com.google.firebase.FirebaseApp;
import com.google.firebase.cloud.FirestoreClient;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.WriteBatch;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.springframework.stereotype.Component;

@Component
public class FirebaseRealtimeGatewayImpl implements IFirebaseRealtimeGateway {

    private static final int CLEANUP_BATCH_SIZE = 200;

    private final Optional<FirebaseApp> firebaseApp;

    public FirebaseRealtimeGatewayImpl(Optional<FirebaseApp> firebaseApp) {
        this.firebaseApp = firebaseApp;
    }

    @Override
    public void write(String path, Map<String, Object> payload) {
        FirebaseApp app = firebaseApp.orElseThrow(() -> new IllegalStateException("FirebaseApp bean not configured"));
        try {
            FirestoreClient.getFirestore(app).document(normalizeDocumentPath(path))
                    .set(payload).get(5, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while writing to Cloud Firestore", ex);
        } catch (ExecutionException | TimeoutException ex) {
            throw new RuntimeException("Failed to write to Cloud Firestore at " + path, ex);
        }
    }

    @Override
    public void purgeEventsOlderThan(Instant cutoff) {
        FirebaseApp app = firebaseApp.orElseThrow(() -> new IllegalStateException("FirebaseApp bean not configured"));
        Firestore firestore = FirestoreClient.getFirestore(app);
        Query query = firestore.collectionGroup("events")
                .whereLessThan("occurredAt", cutoff.toEpochMilli())
                .orderBy("occurredAt")
                .limit(CLEANUP_BATCH_SIZE);
        while (true) {
            try {
                QuerySnapshot snapshot = query.get().get(10, TimeUnit.SECONDS);
                if (snapshot.isEmpty()) {
                    return;
                }
                WriteBatch batch = firestore.batch();
                int deleteCount = 0;
                for (DocumentSnapshot document : snapshot.getDocuments()) {
                    if (isDirectChatEventPath(document.getReference().getPath())) {
                        batch.delete(document.getReference());
                        deleteCount++;
                    }
                }
                if (deleteCount > 0) {
                    batch.commit().get(10, TimeUnit.SECONDS);
                }
                if (snapshot.size() < CLEANUP_BATCH_SIZE) return;
                query = firestore.collectionGroup("events")
                        .whereLessThan("occurredAt", cutoff.toEpochMilli())
                        .orderBy("occurredAt")
                        .startAfter(snapshot.getDocuments().get(snapshot.size() - 1))
                        .limit(CLEANUP_BATCH_SIZE);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted while purging Cloud Firestore events", ex);
            } catch (ExecutionException | TimeoutException ex) {
                throw new RuntimeException("Failed to purge expired Cloud Firestore events", ex);
            }
        }
    }

    private static String normalizeDocumentPath(String path) {
        return path.startsWith("/") ? path.substring(1) : path;
    }

    static boolean isDirectChatEventPath(String path) {
        String[] segments = normalizeDocumentPath(path).split("/");
        return segments.length == 4
                && "userConversationEvents".equals(segments[0])
                && !segments[1].isBlank()
                && "events".equals(segments[2])
                && !segments[3].isBlank();
    }
}
