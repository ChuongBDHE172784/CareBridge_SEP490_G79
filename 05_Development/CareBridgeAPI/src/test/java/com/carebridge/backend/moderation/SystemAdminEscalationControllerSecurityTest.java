package com.carebridge.backend.moderation;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.carebridge.backend.common.config.JpaAuditingConfig;
import com.carebridge.backend.config.MockMvcSecurityBuilderConfig;
import com.carebridge.backend.content.controller.SystemAdminEscalationController;
import com.carebridge.backend.content.repository.ModerationActionRepository;
import com.carebridge.backend.security.config.SecurityConfig;
import com.carebridge.backend.security.jwt.JwtTokenProvider;
import com.carebridge.backend.security.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
@WebMvcTest(value = SystemAdminEscalationController.class, excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JpaAuditingConfig.class))
@Import({SecurityConfig.class, MockMvcSecurityBuilderConfig.class})
class SystemAdminEscalationControllerSecurityTest {
 @Autowired MockMvc mvc; @MockitoBean ModerationActionRepository actions; @MockitoBean JwtTokenProvider jwt; @MockitoBean UserRepository users;
 @Test @WithMockUser(roles="MODERATOR") void moderatorCannotReadEscalations() throws Exception { mvc.perform(get("/api/v1/admin/moderation/escalations")).andExpect(status().isForbidden()); }
}
