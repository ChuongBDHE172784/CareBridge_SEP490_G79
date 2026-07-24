package com.carebridge.backend.consultation.context.service;

import com.carebridge.backend.consultation.context.dto.SharedCitationResponse;
import com.carebridge.backend.triage.entity.EvidenceSource;
import com.carebridge.backend.triage.repository.EvidenceSourceRepository;
import com.google.common.net.InternetDomainName;
import java.net.IDN;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class TriageCitationResolver {

    private final EvidenceSourceRepository repository;

    public TriageCitationResolver(EvidenceSourceRepository repository) {
        this.repository = repository;
    }

    public List<SharedCitationResponse> resolveForCreate(
            List<Map<String, Object>> citations, String stage) {
        return resolve(citations, stage, true);
    }

    public List<SharedCitationResponse> resolveForPreview(
            List<Map<String, Object>> citations, String stage) {
        return resolve(citations, stage, false);
    }

    private List<SharedCitationResponse> resolve(
            List<Map<String, Object>> citations, String stage, boolean lockForShare) {
        if (citations == null || citations.isEmpty() || stage == null) {
            return List.of();
        }

        List<CitationHost> hosts = new ArrayList<>();
        Set<String> allCandidates = new LinkedHashSet<>();
        for (Map<String, Object> citation : citations) {
            CitationHost host = citationHost(citation == null ? null : citation.get("url"));
            if (host != null) {
                hosts.add(host);
                allCandidates.addAll(host.candidates());
            }
        }
        if (allCandidates.isEmpty()) {
            return List.of();
        }

        Set<String> lookupDomains = registryLookupVariants(allCandidates);
        List<EvidenceSource> locked = lockForShare
                ? repository.findAllByNormalizedDomainsForUpdate(lookupDomains)
                : repository.findAllByNormalizedDomains(lookupDomains);
        Map<String, List<EvidenceSource>> byDomain = new HashMap<>();
        for (EvidenceSource source : locked) {
            String domain = normalizeRegistryDomain(source.getDomain());
            if (domain != null && allCandidates.contains(domain)) {
                byDomain.computeIfAbsent(domain, ignored -> new ArrayList<>()).add(source);
            }
        }

        Map<UUID, SharedCitationResponse> ordered = new LinkedHashMap<>();
        for (CitationHost host : hosts) {
            EvidenceSource selected = selectLongestUnambiguous(host.candidates(), byDomain);
            SharedCitationResponse safe = snapshotIfEligible(selected, stage);
            if (safe != null) {
                ordered.putIfAbsent(safe.evidenceSourceId(), safe);
            }
        }
        return List.copyOf(ordered.values());
    }

    private static CitationHost citationHost(Object rawUrl) {
        if (!(rawUrl instanceof String url) || url.isBlank()) {
            return null;
        }
        try {
            URI uri = new URI(url);
            if (!uri.isAbsolute()
                    || !"https".equalsIgnoreCase(uri.getScheme())
                    || uri.getRawUserInfo() != null
                    || uri.getHost() == null) {
                return null;
            }
            String host = normalizeHost(uri.getHost());
            if (host == null || !host.contains(".") || isPublicSuffix(host)) {
                return null;
            }
            List<String> candidates = new ArrayList<>();
            candidates.add(host);
            for (int dot = host.indexOf('.'); dot >= 0; dot = host.indexOf('.', dot + 1)) {
                if (dot + 1 < host.length()) {
                    String suffix = host.substring(dot + 1);
                    if (!suffix.contains(".")) {
                        break;
                    }
                    if (!isPublicSuffix(suffix)) {
                        candidates.add(suffix);
                    }
                }
            }
            return new CitationHost(List.copyOf(candidates));
        } catch (IllegalArgumentException | URISyntaxException ignored) {
            return null;
        }
    }

    private static boolean isPublicSuffix(String host) {
        try {
            return InternetDomainName.from(host).isPublicSuffix();
        } catch (IllegalArgumentException ignored) {
            return true;
        }
    }

    private static Set<String> registryLookupVariants(Collection<String> candidates) {
        Set<String> variants = new LinkedHashSet<>();
        for (String candidate : candidates) {
            addLookupVariants(variants, candidate);
            String unicode = IDN.toUnicode(candidate).toLowerCase(Locale.ROOT);
            if (!unicode.equals(candidate)) {
                addLookupVariants(variants, unicode);
            }
        }
        return variants;
    }

    private static void addLookupVariants(Set<String> variants, String domain) {
        variants.add(domain);
        variants.add("www." + domain);
        variants.add(domain + ".");
        variants.add("www." + domain + ".");
    }

    private static EvidenceSource selectLongestUnambiguous(
            List<String> candidates, Map<String, List<EvidenceSource>> byDomain) {
        for (String candidate : candidates) {
            List<EvidenceSource> matches = byDomain.getOrDefault(candidate, List.of());
            if (matches.size() == 1) {
                return matches.getFirst();
            }
            if (matches.size() > 1) {
                return null;
            }
        }
        return null;
    }

    private static SharedCitationResponse snapshotIfEligible(
            EvidenceSource source, String stage) {
        if (source == null
                || source.getId() == null
                || !"APPROVED".equals(source.getStatus())
                || source.getReviewedAt() == null
                || source.getOrganization() == null
                || source.getOrganization().isBlank()
                || !isApplicable(source.getApplicableStages(), stage)) {
            return null;
        }
        String canonicalBaseUrl = canonicalHttpsBaseUrl(source.getBaseUrl());
        if (canonicalBaseUrl == null) {
            return null;
        }
        String registryDomain = normalizeRegistryDomain(source.getDomain());
        String canonicalBaseHost = normalizeHost(URI.create(canonicalBaseUrl).getHost());
        if (!Objects.equals(registryDomain, canonicalBaseHost)) {
            return null;
        }
        return new SharedCitationResponse(
                source.getId(),
                source.getOrganization().trim(),
                canonicalBaseUrl,
                source.getReviewedAt());
    }

    private static boolean isApplicable(String applicableStages, String stage) {
        if (applicableStages == null || stage == null) {
            return false;
        }
        String canonicalStage = stage.trim().toUpperCase(Locale.ROOT);
        for (String candidate : applicableStages.split(",")) {
            if (canonicalStage.equals(candidate.trim().toUpperCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private static String canonicalHttpsBaseUrl(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            URI uri = new URI(raw.trim());
            if (!uri.isAbsolute()
                    || !"https".equalsIgnoreCase(uri.getScheme())
                    || uri.getRawUserInfo() != null
                    || uri.getHost() == null) {
                return null;
            }
            String host = normalizeHost(uri.getHost());
            if (host == null) {
                return null;
            }
            String path = uri.getRawPath();
            if (path == null || "/".equals(path)) {
                path = "";
            }
            return new URI("https", null, host, uri.getPort(), path, null, null).toASCIIString();
        } catch (IllegalArgumentException | URISyntaxException ignored) {
            return null;
        }
    }

    private static String normalizeRegistryDomain(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return normalizeHost(raw.trim());
    }

    private static String normalizeHost(String raw) {
        try {
            String host = IDN.toASCII(raw, IDN.USE_STD3_ASCII_RULES)
                    .toLowerCase(Locale.ROOT);
            if (host.endsWith(".")) {
                host = host.substring(0, host.length() - 1);
            }
            if (host.startsWith("www.")) {
                host = host.substring(4);
            }
            return host.isBlank() ? null : host;
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private record CitationHost(List<String> candidates) {
    }
}
