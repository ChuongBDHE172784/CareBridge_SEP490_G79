package com.carebridge.backend;

import com.carebridge.backend.security.service.EmailService;
import com.carebridge.backend.security.service.SmsService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
class BackendApplicationTests {

	@MockitoBean
	private EmailService emailService;

	@MockitoBean
	private SmsService smsService;

	@Test
	void contextLoads() {
	}

}
