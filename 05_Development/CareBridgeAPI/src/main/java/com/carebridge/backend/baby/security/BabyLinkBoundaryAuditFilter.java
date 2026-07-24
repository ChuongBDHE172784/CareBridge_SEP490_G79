package com.carebridge.backend.baby.security;

import com.carebridge.backend.baby.service.BabyLinkRejectionAuditService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;

/** Audits linkage attempts rejected before a controller/service method can execute. */
@Component
@RequiredArgsConstructor
public class BabyLinkBoundaryAuditFilter extends OncePerRequestFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(BabyLinkBoundaryAuditFilter.class);
    private static final String CONTROLLER_ENTERED = BabyLinkBoundaryAuditFilter.class.getName() + ".entered";
    private static final Pattern LINK_PATH = Pattern.compile(
            "^/api/v1/babies/([^/]+)/journey-link$");
    private static final Pattern LIST_PATH = Pattern.compile(
            "^/api/v1/journeys/([^/]+)/babies$");

    private final BabyLinkRejectionAuditService rejectionAuditService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        ContentCachingRequestWrapper wrapped = new ContentCachingRequestWrapper(request, 16 * 1024);
        filterChain.doFilter(wrapped, response);

        if (response.getStatus() < 400 || response.getStatus() >= 500
                || Boolean.TRUE.equals(wrapped.getAttribute(CONTROLLER_ENTERED))) {
            return;
        }
        UUID actor = authenticatedActor();
        BoundaryAttempt attempt = classify(wrapped, response.getStatus());
        if (actor != null && attempt != null) {
            try {
                rejectionAuditService.record(actor, attempt.opaqueTarget(), attempt.reason());
            } catch (RuntimeException auditFailure) {
                LOGGER.error(
                        "Failed to persist baby-link boundary rejection audit: actor={}, reason={}",
                        actor,
                        attempt.reason(),
                        auditFailure);
            }
        }
    }

    public static void markControllerEntered(HttpServletRequest request) {
        request.setAttribute(CONTROLLER_ENTERED, Boolean.TRUE);
    }

    private BoundaryAttempt classify(ContentCachingRequestWrapper request, int status) {
        String path = request.getRequestURI();
        Matcher link = LINK_PATH.matcher(path);
        if (link.matches() && "PUT".equals(request.getMethod())) {
            return new BoundaryAttempt(uuidOrNull(link.group(1)), "BOUNDARY_HTTP_" + status);
        }
        Matcher list = LIST_PATH.matcher(path);
        if (list.matches() && "GET".equals(request.getMethod())) {
            return new BoundaryAttempt(uuidOrNull(list.group(1)), "BOUNDARY_HTTP_" + status);
        }
        if (status == 400 && "/api/v1/babies".equals(path) && "POST".equals(request.getMethod())) {
            String body = new String(request.getContentAsByteArray(), StandardCharsets.UTF_8);
            if (body.contains("\"relatedJourneyId\"")) {
                return new BoundaryAttempt(null, "BOUNDARY_VALIDATION");
            }
        }
        return null;
    }

    private UUID authenticatedActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        return uuidOrNull(authentication.getName());
    }

    private UUID uuidOrNull(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private record BoundaryAttempt(UUID opaqueTarget, String reason) {}
}
