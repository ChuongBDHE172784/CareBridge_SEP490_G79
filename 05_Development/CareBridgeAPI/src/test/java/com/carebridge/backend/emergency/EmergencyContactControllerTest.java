package com.carebridge.backend.emergency;

import com.carebridge.backend.common.config.JpaAuditingConfig;
import com.carebridge.backend.config.MockMvcSecurityBuilderConfig;
import com.carebridge.backend.emergency.controller.EmergencyContactController;
import com.carebridge.backend.emergency.service.IEmergencyContactService;
import com.carebridge.backend.security.config.SecurityConfig;
import com.carebridge.backend.security.jwt.JwtTokenProvider;
import com.carebridge.backend.security.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        value = EmergencyContactController.class,
        excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JpaAuditingConfig.class)
)
@Import({SecurityConfig.class, MockMvcSecurityBuilderConfig.class})
class EmergencyContactControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IEmergencyContactService emergencyContactService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private UserRepository userRepository;

    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000010", roles = "MOTHER")
    void upsertContact_shouldReturnOk() throws Exception {
        mockMvc.perform(put("/api/v1/emergency/contact")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Lan Nguyen\",\"phone\":\"+84901234567\",\"relationship\":\"Sister\",\"primaryContact\":true}"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000010", roles = "MOTHER")
    void getContact_shouldReturnOk() throws Exception {
        mockMvc.perform(get("/api/v1/emergency/contact"))
                .andExpect(status().isOk());
    }
}
