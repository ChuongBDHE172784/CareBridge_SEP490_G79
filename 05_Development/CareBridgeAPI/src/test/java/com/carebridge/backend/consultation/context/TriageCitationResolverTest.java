package com.carebridge.backend.consultation.context;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.carebridge.backend.consultation.context.dto.SharedCitationResponse;
import com.carebridge.backend.consultation.context.service.TriageCitationResolver;
import com.carebridge.backend.triage.entity.EvidenceSource;
import com.carebridge.backend.triage.repository.EvidenceSourceRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TriageCitationResolverTest {

    private static final Instant REVIEWED_AT = Instant.parse("2026-07-01T00:00:00Z");

    @Mock private EvidenceSourceRepository repository;

    private TriageCitationResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new TriageCitationResolver(repository);
    }

    @Test
    void longestLabelBoundaryMatchWinsAndOnlyRegistryMetadataIsReturned() {
        EvidenceSource parent = source("who.int", "https://who.int", "WHO");
        EvidenceSource longest = source(
                "guidance.who.int", "https://guidance.who.int", "WHO Guidance");
        stubLockedSources(parent, longest);

        List<SharedCitationResponse> result = resolver.resolveForCreate(
                List.of(Map.of(
                        "url", "https://sub.guidance.who.int/path?patient=ignored#fragment",
                        "title", "Client title must be ignored",
                        "sourceStatus", "APPROVED",
                        "excerpt", "Never copy this")),
                "POSTPARTUM");

        assertThat(result).containsExactly(new SharedCitationResponse(
                longest.getId(), "WHO Guidance", "https://guidance.who.int", REVIEWED_AT));
        verify(repository).findAllByNormalizedDomainsForUpdate(anyCollection());
    }

    @Test
    void invalidUnreviewedUnapprovedOrWrongStageSourcesFailClosed() {
        EvidenceSource unapproved = source("unapproved.test", "https://unapproved.test", "U");
        unapproved.setStatus("PENDING");
        EvidenceSource unreviewed = source("unreviewed.test", "https://unreviewed.test", "R");
        unreviewed.setReviewedAt(null);
        EvidenceSource wrongStage = source("infant.test", "https://infant.test", "I");
        wrongStage.setApplicableStages("INFANT,TODDLER");
        EvidenceSource badBase = source("badbase.test", "http://badbase.test", "B");
        stubLockedSources(unapproved, unreviewed, wrongStage, badBase);

        List<SharedCitationResponse> result = resolver.resolveForCreate(
                List.of(
                        Map.of("url", "http://unapproved.test/path"),
                        Map.of("url", "https://unapproved.test/path"),
                        Map.of("url", "https://unreviewed.test/path"),
                        Map.of("url", "https://infant.test/path"),
                        Map.of("url", "https://badbase.test/path"),
                        Map.of("url", "https://user:secret@unapproved.test/path")),
                "POSTPARTUM");

        assertThat(result).isEmpty();
    }

    @Test
    void duplicateRegistrySourceIsReturnedOnceInFirstCitationOrder() {
        EvidenceSource first = source("one.test", "https://one.test", "One");
        EvidenceSource second = source("two.test", "https://two.test", "Two");
        stubLockedSources(second, first);

        List<SharedCitationResponse> result = resolver.resolveForCreate(
                List.of(
                        Map.of("url", "https://two.test/a"),
                        Map.of("url", "https://one.test/a"),
                        Map.of("url", "https://two.test/b")),
                "POSTPARTUM");

        assertThat(result).extracting(SharedCitationResponse::evidenceSourceId)
                .containsExactly(second.getId(), first.getId());
    }

    @Test
    void publicSuffixAndRegistryBaseUrlDomainMismatchFailClosed() {
        EvidenceSource singleLabelSuffix = source("com", "https://com", "Public suffix");
        EvidenceSource multiLabelSuffix = source("co.uk", "https://co.uk", "Public suffix");
        EvidenceSource schoolSuffix = source(
                "k12.ca.us", "https://k12.ca.us", "Public suffix");
        EvidenceSource privateSuffix = source(
                "blogspot.com", "https://blogspot.com", "Private suffix");
        EvidenceSource mismatchedBase = source(
                "trusted.example", "https://other.example", "Mismatched base");
        stubLockedSources(
                singleLabelSuffix,
                multiLabelSuffix,
                schoolSuffix,
                privateSuffix,
                mismatchedBase);

        List<SharedCitationResponse> result = resolver.resolveForCreate(
                List.of(
                        Map.of("url", "https://subdomain.example.com/path"),
                        Map.of("url", "https://co.uk/path"),
                        Map.of("url", "https://unrelated.co.uk/path"),
                        Map.of("url", "https://school.k12.ca.us/path"),
                        Map.of("url", "https://tenant.blogspot.com/path"),
                        Map.of("url", "https://trusted.example/path")),
                "POSTPARTUM");

        assertThat(result).isEmpty();
    }

    @Test
    void registryDomainsAreNormalizedBeforeCandidateRowsAreLocked() {
        EvidenceSource wwwAndDot = source(
                " WWW.Example.com. ", "https://example.com", "Example");
        EvidenceSource unicode = source(
                "b\u00fccher.example", "https://xn--bcher-kva.example", "Unicode");
        stubLockedSources(wwwAndDot, unicode);

        List<SharedCitationResponse> result = resolver.resolveForCreate(
                List.of(
                        Map.of("url", "https://example.com/path"),
                        Map.of("url", "https://xn--bcher-kva.example/path")),
                "POSTPARTUM");

        assertThat(result).extracting(SharedCitationResponse::evidenceSourceId)
                .containsExactly(wwwAndDot.getId(), unicode.getId());
        @SuppressWarnings("unchecked")
        ArgumentCaptor<java.util.Collection<String>> domains =
                ArgumentCaptor.forClass(java.util.Collection.class);
        verify(repository).findAllByNormalizedDomainsForUpdate(domains.capture());
        assertThat(domains.getValue()).contains(
                "example.com",
                "www.example.com.",
                "xn--bcher-kva.example",
                "b\u00fccher.example");
    }

    private void stubLockedSources(EvidenceSource... sources) {
        List<EvidenceSource> rows = List.of(sources);
        when(repository.findAllByNormalizedDomainsForUpdate(anyCollection())).thenReturn(rows);
    }

    private static EvidenceSource source(String domain, String baseUrl, String organization) {
        return EvidenceSource.builder()
                .id(UUID.randomUUID())
                .domain(domain)
                .baseUrl(baseUrl)
                .organization(organization)
                .status("APPROVED")
                .applicableStages("PRECONCEPTION,PREGNANCY,POSTPARTUM")
                .reviewedAt(REVIEWED_AT)
                .build();
    }
}
