package com.carebridge.backend.baby;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.doThrow;
import static org.assertj.core.api.Assertions.assertThat;

import com.carebridge.backend.baby.security.BabyLinkBoundaryAuditFilter;
import com.carebridge.backend.baby.service.BabyLinkRejectionAuditService;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.beans.factory.ObjectProvider;

@ExtendWith(MockitoExtension.class)
class BabyLinkBoundaryAuditFilterTest {

    @Mock BabyLinkRejectionAuditService audit;
    @Mock ObjectProvider<BabyLinkRejectionAuditService> auditProvider;
    @InjectMocks BabyLinkBoundaryAuditFilter filter;

    @BeforeEach
    void provideAuditWriter() {
        org.mockito.Mockito.lenient().when(auditProvider.getIfAvailable()).thenReturn(audit);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void auditsForbiddenLinkRejectedBeforeControllerExecution() throws Exception {
        UUID actor = UUID.randomUUID();
        UUID baby = UUID.randomUUID();
        authenticate(actor, "ROLE_EXPERT");
        var request = new MockHttpServletRequest("PUT", "/api/v1/babies/" + baby + "/journey-link");
        var response = new MockHttpServletResponse();
        jakarta.servlet.FilterChain chain = (req, res) -> ((MockHttpServletResponse) res).setStatus(403);

        filter.doFilter(request, response, chain);

        verify(audit).record(actor, baby, "BOUNDARY_HTTP_403");
    }

    @Test
    void doesNotDuplicateAuditWhenControllerAndServiceHandledTheRejection() throws Exception {
        UUID actor = UUID.randomUUID();
        UUID baby = UUID.randomUUID();
        authenticate(actor, "ROLE_MOTHER");
        var request = new MockHttpServletRequest("PUT", "/api/v1/babies/" + baby + "/journey-link");
        var response = new MockHttpServletResponse();
        jakarta.servlet.FilterChain chain = (req, res) -> {
            BabyLinkBoundaryAuditFilter.markControllerEntered((jakarta.servlet.http.HttpServletRequest) req);
            ((MockHttpServletResponse) res).setStatus(409);
        };

        filter.doFilter(request, response, chain);

        verifyNoInteractions(audit);
    }

    @Test
    void auditWriterFailureIsObservableWithoutChangingSecurityResponse() throws Exception {
        UUID actor = UUID.randomUUID();
        UUID baby = UUID.randomUUID();
        authenticate(actor, "ROLE_EXPERT");
        doThrow(new IllegalStateException("audit unavailable"))
                .when(audit).record(actor, baby, "BOUNDARY_HTTP_403");
        var request = new MockHttpServletRequest("PUT", "/api/v1/babies/" + baby + "/journey-link");
        var response = new MockHttpServletResponse();
        jakarta.servlet.FilterChain chain = (req, res) -> ((MockHttpServletResponse) res).setStatus(403);

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(403);
        verify(audit).record(actor, baby, "BOUNDARY_HTTP_403");
    }

    private void authenticate(UUID actor, String role) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        actor.toString(), "n/a", java.util.List.of(new SimpleGrantedAuthority(role))));
    }
}
