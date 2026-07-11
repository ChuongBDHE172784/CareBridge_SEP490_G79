package com.carebridge.backend.partner;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.carebridge.backend.common.config.JpaAuditingConfig;
import com.carebridge.backend.config.MockMvcSecurityBuilderConfig;
import com.carebridge.backend.partner.controller.PartnerContentRemovalController;
import com.carebridge.backend.partner.dto.response.RemovalResponse;
import com.carebridge.backend.partner.entity.PartnerContentTargetType;
import com.carebridge.backend.partner.service.PartnerContentRemovalService;
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

@WebMvcTest(value = PartnerContentRemovalController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JpaAuditingConfig.class))
@Import({SecurityConfig.class, MockMvcSecurityBuilderConfig.class})
class RemovePartnerContentControllerTest {
    static final UUID ADMIN = UUID.fromString("f0100000-0000-0000-0000-0000000000ad");
    static final UUID SERVICE = UUID.fromString("f0200000-0000-0000-0000-00000000000d");
    @Autowired MockMvc mvc;
    @MockitoBean PartnerContentRemovalService service;
    @MockitoBean JwtTokenProvider jwt;
    @MockitoBean UserRepository users;

    @Test @WithMockUser(username = "f0100000-0000-0000-0000-0000000000ad", roles = "SYSTEM_ADMIN")
    void prcTc801_fullRequestReturnsRemoval() throws Exception {
        when(service.remove(eq(PartnerContentTargetType.SERVICE), eq(SERVICE), any(), eq(ADMIN)))
                .thenReturn(new RemovalResponse(PartnerContentTargetType.SERVICE, SERVICE, true, ADMIN, "policy", Instant.now()));
        mvc.perform(post("/api/v1/admin/partner-content/SERVICE/" + SERVICE + "/remove").with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"policy\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.isRemoved").value(true));
    }

    @Test @WithMockUser(roles = "SYSTEM_ADMIN")
    void prcTc804_blankReasonReturns400() throws Exception {
        mvc.perform(post("/api/v1/admin/partner-content/SERVICE/" + SERVICE + "/remove").with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\" \"}"))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser(roles = "SYSTEM_ADMIN")
    void prcTc806_unknownTargetTypeReturns400() throws Exception {
        mvc.perform(post("/api/v1/admin/partner-content/UNKNOWN/" + SERVICE + "/remove").with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"policy\"}"))
                .andExpect(status().isBadRequest());
    }
}
