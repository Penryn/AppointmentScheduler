// 测试说明：验证 Spring Boot 应用上下文可以正常启动。
package com.example.slabiak.appointmentscheduler;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("integration-test")
public class AppointmentschedulerApplicationIT {

	@Autowired
	private ApplicationContext applicationContext;

	@Test
	public void contextLoads() {
		// 检查点：验证该测试用例的预期结果。
		assertNotNull(applicationContext);
	}

}
