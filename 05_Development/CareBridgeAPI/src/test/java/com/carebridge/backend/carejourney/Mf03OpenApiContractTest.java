package com.carebridge.backend.carejourney;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.mail.javamail.JavaMailSender;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
class Mf03OpenApiContractTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JavaMailSender javaMailSender;

    @Test
    void generatedOpenApi_containsMf03Routes() throws Exception {
        String document = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(document).contains("/api/v1/babies/{babyId}/milestones");
        assertThat(document).contains("/api/v1/vaccination/babies/{babyId}/records");
        assertThat(document).contains("/api/v1/babies/{babyId}/daily-logs");
        assertThat(document).contains("/api/v1/babies/{babyId}/care-overview");
        assertThat(document).contains("/api/v1/babies/{babyId}/care-timeline");
        assertThat(document).contains("/api/v1/babies/{babyId}/appointment-preparation-summary");
        assertThat(document).contains("MilestoneResponse");
        assertThat(document).contains("VaccinationRecordResponse");
        assertThat(document).contains("\"babyId\"");
        assertThat(document).contains("\"status\"");
    }
}
