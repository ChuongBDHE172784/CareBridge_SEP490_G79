package com.carebridge.backend.masterdata;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.carebridge.backend.common.config.JpaAuditingConfig;
import com.carebridge.backend.config.MockMvcSecurityBuilderConfig;
import com.carebridge.backend.masterdata.controller.MasterDataController;
import com.carebridge.backend.masterdata.dto.response.WardResponse;
import com.carebridge.backend.masterdata.service.IMasterDataService;
import com.carebridge.backend.security.config.SecurityConfig;
import com.carebridge.backend.security.jwt.JwtTokenProvider;
import com.carebridge.backend.security.repository.UserRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
        value = MasterDataController.class,
        excludeFilters = @Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JpaAuditingConfig.class))
@Import({SecurityConfig.class, MockMvcSecurityBuilderConfig.class})
class MasterDataControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private IMasterDataService service;
    @MockitoBean private JwtTokenProvider jwtTokenProvider;
    @MockitoBean private UserRepository userRepository;

    @Test
    void unauthenticatedWardGetRemainsPublicAndReturnsCanonicalContract() throws Exception {
        when(service.getWardsByDistrict("0101")).thenReturn(List.of(WardResponse.builder()
                .wardId("01001")
                .districtId("0101")
                .provinceId("01")
                .name("Phúc Xá")
                .nameEn("Phuc Xa")
                .build()));

        mockMvc.perform(get("/api/v1/master-data/wards").param("districtId", "0101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].wardId").value("01001"))
                .andExpect(jsonPath("$.data[0].districtId").value("0101"))
                .andExpect(jsonPath("$.data[0].provinceId").value("01"))
                .andExpect(jsonPath("$.data[0].name").value("Phúc Xá"))
                .andExpect(jsonPath("$.data[0].nameEn").value("Phuc Xa"));

        verify(service).getWardsByDistrict("0101");
    }

    @Test
    void unauthenticatedNonGetUnderMasterDataStillRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/master-data/wards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }
}
