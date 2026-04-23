package com.example.slabiak.appointmentscheduler.security;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("integration-test")
public class SecurityAndActuatorIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    @Qualifier("mailExecutor")
    private ThreadPoolTaskExecutor mailExecutor;

    @Autowired
    @Qualifier("taskScheduler")
    private TaskScheduler taskScheduler;

    @Test
    public void shouldExposeRegistrationPageWithoutBypassingSecurityFilterChain() throws Exception {
        mockMvc.perform(get("/customers/new/retail"))
                .andExpect(status().isOk());
    }

    @Test
    public void shouldExposeActuatorHealthEndpoint() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    public void shouldRegisterAsyncInfrastructureBeans() {
        assertThat(mailExecutor).isNotNull();
        assertThat(taskScheduler).isNotNull();
    }
}
