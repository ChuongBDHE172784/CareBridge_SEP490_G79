package com.carebridge.backend.integration;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.carebridge.backend.common.config.JpaAuditingConfig;
import com.carebridge.backend.config.MockMvcSecurityBuilderConfig;
import com.carebridge.backend.partner.controller.PartnerServiceController;
import com.carebridge.backend.partner.dto.response.SubmitServiceListingResponse;
import com.carebridge.backend.partner.entity.ServiceApprovalStatus;
import com.carebridge.backend.partner.exception.PartnerException;
import com.carebridge.backend.partner.service.PartnerServiceService;
import com.carebridge.backend.security.config.SecurityConfig;
import com.carebridge.backend.security.jwt.JwtTokenProvider;
import com.carebridge.backend.security.repository.UserRepository;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.*;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(value=PartnerServiceController.class,excludeFilters=@ComponentScan.Filter(type=FilterType.ASSIGNABLE_TYPE,classes=JpaAuditingConfig.class))
@Import({SecurityConfig.class,MockMvcSecurityBuilderConfig.class})
class SubmitServiceListingIntegrationTest {
    static final UUID OWNER=UUID.fromString("f3000000-0000-0000-0000-000000000001"), ORG=UUID.fromString("f4000000-0000-0000-0000-000000000001"), ID=UUID.fromString("f5000000-0000-0000-0000-000000000001");
    @Autowired MockMvc mockMvc; @MockitoBean PartnerServiceService service; @MockitoBean JwtTokenProvider jwt; @MockitoBean UserRepository users;
    @Test @WithMockUser(username="f3000000-0000-0000-0000-000000000001",roles="PARTNER") void pslTcInt001_fullPostReturnsPendingForResolvedOrg() throws Exception {
        when(service.submitService(any(),eq(OWNER))).thenReturn(new SubmitServiceListingResponse(ID,ORG,"Care",null,null,"VND",null,ServiceApprovalStatus.PENDING,Instant.now()));
        mockMvc.perform(post("/api/v1/partner/services").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"serviceName\":\"Care\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.data.partnerId").value(ORG.toString())).andExpect(jsonPath("$.data.approvalStatus").value("PENDING"));
    }
    @Test @WithMockUser(username="f3000000-0000-0000-0000-000000000001",roles="PARTNER") void pslTcInt002_nonApprovedOrgReturns409() throws Exception {
        when(service.submitService(any(),eq(OWNER))).thenThrow(PartnerException.organizationNotApproved());
        mockMvc.perform(post("/api/v1/partner/services").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"serviceName\":\"Care\"}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("PTR-011"));
    }
}
