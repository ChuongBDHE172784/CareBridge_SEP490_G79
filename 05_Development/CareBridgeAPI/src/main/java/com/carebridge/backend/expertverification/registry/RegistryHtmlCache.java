package com.carebridge.backend.expertverification.registry;

import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Holds the registry result page for a short window so an admin can open the source page for the
 * credential they just checked (MF-05 Spec 05 §8.4).
 *
 * <p>Deliberately in-memory and short-lived. The result page can contain rows about other people,
 * and Decree 13/2023/NĐ-CP makes long-term retention of that data a liability we have no reason to
 * take on — the row we actually matched is already kept as audit evidence.
 */
@Component
public class RegistryHtmlCache {

    /** Bounds memory when an admin works through a long review queue. */
    private static final int MAX_ENTRIES = 50;

    private final Duration ttl;

    private final Map<UUID, Entry> entries = new LinkedHashMap<>();

    public RegistryHtmlCache(
            @Value("${carebridge.registry.hcm-medinet.cache-minutes:15}") long cacheMinutes) {
        this.ttl = Duration.ofMinutes(cacheMinutes);
    }

    /**
     * @param html     the registry result page, or null when the lookup did not produce one
     * @param response the answer already computed for that page, replayed on repeat clicks so a
     *                 second press does not hit public infrastructure again
     */
    public record Entry(
            String html,
            com.carebridge.backend.expertverification.dto.response.RegistryLookupResponse response,
            Instant queriedAt,
            Instant expiresAt) {
    }

    public synchronized void put(
            UUID credentialId,
            String html,
            com.carebridge.backend.expertverification.dto.response.RegistryLookupResponse response,
            Instant queriedAt) {
        evictExpired();
        if (entries.size() >= MAX_ENTRIES && !entries.containsKey(credentialId)) {
            Iterator<UUID> oldest = entries.keySet().iterator();
            if (oldest.hasNext()) {
                oldest.next();
                oldest.remove();
            }
        }
        entries.remove(credentialId); // reinsert so LinkedHashMap order reflects recency
        entries.put(credentialId, new Entry(html, response, queriedAt, queriedAt.plus(ttl)));
    }

    public synchronized Optional<Entry> get(UUID credentialId) {
        evictExpired();
        return Optional.ofNullable(entries.get(credentialId));
    }

    public synchronized void evict(UUID credentialId) {
        entries.remove(credentialId);
    }

    public Duration ttl() {
        return ttl;
    }

    private void evictExpired() {
        Instant now = Instant.now();
        entries.values().removeIf(entry -> entry.expiresAt().isBefore(now));
    }
}
