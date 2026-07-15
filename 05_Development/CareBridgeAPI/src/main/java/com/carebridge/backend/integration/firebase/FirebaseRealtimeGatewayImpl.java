package com.carebridge.backend.integration.firebase;

import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.ArrayList;
import java.util.List;
import com.google.api.core.ApiFuture;
import org.springframework.stereotype.Component;

@Component
public class FirebaseRealtimeGatewayImpl implements IFirebaseRealtimeGateway {

    private final Optional<FirebaseApp> firebaseApp;

    public FirebaseRealtimeGatewayImpl(Optional<FirebaseApp> firebaseApp) {
        this.firebaseApp = firebaseApp;
    }

    @Override
    public void write(String path, Map<String, Object> payload) {
        FirebaseApp app = firebaseApp.orElseThrow(() -> new IllegalStateException("FirebaseApp bean not configured"));
        DatabaseReference ref = FirebaseDatabase.getInstance(app).getReference(path);
        try {
            ref.setValueAsync(payload).get(5, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while writing to Firebase RTDB", ex);
        } catch (ExecutionException | TimeoutException ex) {
            throw new RuntimeException("Failed to write to Firebase RTDB at " + path, ex);
        }
    }

    @Override
    public void purgeEventsOlderThan(Instant cutoff) {
        FirebaseApp app = firebaseApp.orElseThrow(() -> new IllegalStateException("FirebaseApp bean not configured"));
        DatabaseReference root = FirebaseDatabase.getInstance(app).getReference("/user-conversation-events");

        CompletableFuture<DataSnapshot> future = new CompletableFuture<>();
        root.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                future.complete(snapshot);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                future.completeExceptionally(error.toException());
            }
        });

        DataSnapshot snapshot;
        try {
            snapshot = future.get(10, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while reading Firebase RTDB for retention purge", ex);
        } catch (ExecutionException | TimeoutException ex) {
            throw new RuntimeException("Failed to read Firebase RTDB for retention purge", ex);
        }

        long cutoffMillis = cutoff.toEpochMilli();
        List<ApiFuture<Void>> removals = new ArrayList<>();
        for (DataSnapshot userNode : snapshot.getChildren()) {
            for (DataSnapshot eventNode : userNode.getChildren()) {
                Object occurredAt = eventNode.child("occurredAt").getValue();
                if (occurredAt instanceof Number number && number.longValue() < cutoffMillis) {
                    removals.add(eventNode.getRef().removeValueAsync());
                }
            }
        }
        for (ApiFuture<Void> removal : removals) {
            try {
                removal.get(5, TimeUnit.SECONDS);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted while deleting expired Firebase events", ex);
            } catch (ExecutionException | TimeoutException ex) {
                throw new RuntimeException("Failed to delete expired Firebase events", ex);
            }
        }
    }
}
