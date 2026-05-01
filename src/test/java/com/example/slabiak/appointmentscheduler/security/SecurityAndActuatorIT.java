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
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.util.StringUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
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
        MvcResult result = mockMvc.perform(get("/customers/new/retail"))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(StringUtils.countOccurrencesOf(result.getResponse().getContentAsString(), "<body")).isEqualTo(1);
        assertThat(result.getResponse().getContentAsString()).contains("_csrf");
    }

    @Test
    @WithUserDetails("customer_r")
    public void shouldRejectStateChangingRequestWithoutCsrfToken() throws Exception {
        mockMvc.perform(post("/notifications/markAllAsRead"))
                .andExpect(status().isForbidden());
    }

    @Test
    public void shouldRedirectAnonymousUsersFromAppointmentPagesToLogin() throws Exception {
        mockMvc.perform(get("/appointments/new"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @WithUserDetails("customer_r")
    public void shouldDenyCustomerAccessToAdminCustomerList() throws Exception {
        mockMvc.perform(get("/customers/all"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails("provider")
    public void shouldDenyProviderAccessToCustomerPages() throws Exception {
        mockMvc.perform(get("/customers/3"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails("provider")
    public void shouldDenyProviderAccessToCustomerAppointmentBookingFlow() throws Exception {
        mockMvc.perform(get("/appointments/new"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails("load_customer_01")
    public void shouldDenyCustomerAccessToAnotherCustomerProfile() throws Exception {
        mockMvc.perform(get("/customers/3"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails("customer_r")
    public void shouldAcceptStateChangingRequestWithCsrfToken() throws Exception {
        mockMvc.perform(post("/notifications/markAllAsRead").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/notifications*"));
    }

    @Test
    @WithUserDetails("admin")
    public void shouldRenderServerPaginationForAdminLists() throws Exception {
        mockMvc.perform(get("/customers/all").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("分页")))
                .andExpect(content().string(containsString("下一页")));
    }

    @Test
    public void shouldExposeActuatorHealthEndpoint() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    public void shouldExposeActuatorPrometheusEndpoint() throws Exception {
        mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("# HELP")));
    }

    @Test
    public void shouldRegisterAsyncInfrastructureBeans() {
        assertThat(mailExecutor).isNotNull();
        assertThat(taskScheduler).isNotNull();
    }
}
