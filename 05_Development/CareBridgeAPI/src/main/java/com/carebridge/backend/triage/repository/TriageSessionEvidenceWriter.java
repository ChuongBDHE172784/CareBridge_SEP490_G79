package com.carebridge.backend.triage.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Writes immutable, idempotent evidence rows from the validated triage result. */
@Repository
public class TriageSessionEvidenceWriter {

    private static final String INSERT_SQL = """
            INSERT INTO triage_session_evidence (
                evidence_id, triage_session_id, evidence_type, claim_code, claim_text,
                knowledge_source_id, citation_url, citation_domain, source_version,
                source_snapshot_jsonb, content_hash, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, CAST(? AS jsonb), ?, now())
            ON CONFLICT (triage_session_id, evidence_type, content_hash) DO NOTHING
            """;

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public TriageSessionEvidenceWriter(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper.copy()
                .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
    }

    public int writeValidated(
            UUID triageSessionId,
            List<Map<String, Object>> citations,
            List<Map<String, Object>> claims) {
        Map<String, CitationContext> citationBySourceId = new LinkedHashMap<>();
        int inserted = 0;
        for (Map<String, Object> citation : citations) {
            String sourceId = text(citation.get("sourceId"));
            String url = text(citation.get("url"));
            String domain = canonicalDomain(text(citation.get("domain")), url);
            UUID knowledgeSourceId = resolveKnowledgeSource(sourceId, domain);
            String snapshot = snapshot(citation);
            String claimText = text(citation.get("excerpt"));
            if (claimText == null) {
                claimText = text(citation.get("title"));
            }
            inserted += insert(
                    triageSessionId,
                    "CITATION",
                    sourceId,
                    claimText,
                    knowledgeSourceId,
                    url,
                    domain,
                    defaulted(text(citation.get("sourceVersion")), "legacy-unknown"),
                    snapshot);
            if (sourceId != null) {
                citationBySourceId.put(sourceId, new CitationContext(
                        knowledgeSourceId,
                        url,
                        domain,
                        defaulted(text(citation.get("sourceVersion")), "legacy-unknown")));
            }
        }

        for (Map<String, Object> claim : claims) {
            String claimCode = text(claim.get("claimId"));
            String claimText = text(claim.get("text"));
            CitationContext linked = linkedCitation(claim.get("evidenceIds"), citationBySourceId);
            String snapshot = snapshot(claim);
            inserted += insert(
                    triageSessionId,
                    "CLAIM",
                    claimCode,
                    claimText,
                    linked == null ? null : linked.knowledgeSourceId(),
                    linked == null ? null : linked.url(),
                    linked == null ? null : linked.domain(),
                    linked == null ? null : linked.sourceVersion(),
                    snapshot);
        }
        return inserted;
    }

    private int insert(
            UUID sessionId,
            String evidenceType,
            String claimCode,
            String claimText,
            UUID knowledgeSourceId,
            String citationUrl,
            String citationDomain,
            String sourceVersion,
            String snapshot) {
        if (claimText == null) {
            return 0;
        }
        return jdbcTemplate.update(
                INSERT_SQL,
                UUID.randomUUID(),
                sessionId,
                evidenceType,
                claimCode,
                claimText,
                knowledgeSourceId,
                citationUrl,
                citationDomain,
                sourceVersion,
                snapshot,
                sha256(snapshot));
    }

    private UUID resolveKnowledgeSource(String sourceId, String domain) {
        UUID parsed = uuid(sourceId);
        if (parsed != null) {
            List<UUID> exact = jdbcTemplate.query(
                    "SELECT knowledge_source_id FROM knowledge_sources "
                            + "WHERE knowledge_source_id = ? LIMIT 1",
                    (result, row) -> result.getObject("knowledge_source_id", UUID.class),
                    parsed);
            if (!exact.isEmpty()) {
                return exact.getFirst();
            }
        }
        if (domain == null) {
            return null;
        }
        List<UUID> byDomain = jdbcTemplate.query(
                "SELECT knowledge_source_id FROM knowledge_sources "
                        + "WHERE lower(domain) = lower(?) ORDER BY knowledge_source_id LIMIT 1",
                (result, row) -> result.getObject("knowledge_source_id", UUID.class),
                domain);
        return byDomain.isEmpty() ? null : byDomain.getFirst();
    }

    private CitationContext linkedCitation(
            Object evidenceIds, Map<String, CitationContext> citationBySourceId) {
        if (!(evidenceIds instanceof Iterable<?> values)) {
            return null;
        }
        for (Object value : values) {
            CitationContext context = citationBySourceId.get(text(value));
            if (context != null) {
                return context;
            }
        }
        return null;
    }

    private String snapshot(Map<String, Object> value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to serialize triage evidence snapshot", exception);
        }
    }

    private String canonicalDomain(String supplied, String url) {
        if (supplied != null) {
            return supplied.toLowerCase(java.util.Locale.ROOT).replaceFirst("^www\\.", "");
        }
        try {
            String host = URI.create(url).getHost();
            return host == null ? null
                    : host.toLowerCase(java.util.Locale.ROOT).replaceFirst("^www\\.", "");
        } catch (IllegalArgumentException | NullPointerException exception) {
            return null;
        }
    }

    private static UUID uuid(String value) {
        try {
            return value == null ? null : UUID.fromString(value);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private static String text(Object value) {
        return value == null || String.valueOf(value).isBlank() ? null : String.valueOf(value);
    }

    private static String defaulted(String value, String fallback) {
        return value == null ? fallback : value;
    }

    private static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private record CitationContext(
            UUID knowledgeSourceId,
            String url,
            String domain,
            String sourceVersion) {
    }
}
