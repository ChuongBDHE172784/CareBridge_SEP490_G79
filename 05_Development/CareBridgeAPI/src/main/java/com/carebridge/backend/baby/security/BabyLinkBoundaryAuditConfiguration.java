package com.carebridge.backend.baby.security;

import com.carebridge.backend.baby.service.BabyLinkRejectionAuditService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Wires the boundary filter only in the full application context. */
@Configuration(proxyBeanMethods = false)
public class BabyLinkBoundaryAuditConfiguration {

    @Bean
    BabyLinkBoundaryAuditFilter babyLinkBoundaryAuditFilter(
            BabyLinkRejectionAuditService rejectionAuditService) {
        return new BabyLinkBoundaryAuditFilter(rejectionAuditService);
    }
}
