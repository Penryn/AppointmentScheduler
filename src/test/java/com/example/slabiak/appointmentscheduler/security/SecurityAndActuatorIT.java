// 测试说明：验证安全配置、CSRF、CORS、响应头和 Actuator 暴露策略。
package com.example.slabiak.appointmentscheduler.security;

import com.example.slabiak.appointmentscheduler.dao.AppointmentRepository;
import com.example.slabiak.appointmentscheduler.entity.Appointment;
import com.example.slabiak.appointmentscheduler.entity.AppointmentStatus;
import com.example.slabiak.appointmentscheduler.entity.Work;
import com.example.slabiak.appointmentscheduler.entity.user.customer.Customer;
import com.example.slabiak.appointmentscheduler.entity.user.provider.Provider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.servlet.server.Session;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

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

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private ServerProperties serverProperties;

    @Test
    public void shouldExposeRegistrationPageWithoutBypassingSecurityFilterChain() throws Exception {
        MvcResult result = mockMvc.perform(get("/customers/new/retail"))
                // 检查点：验证该测试用例的预期结果。
                .andExpect(status().isOk())
                .andReturn();

        assertThat(StringUtils.countOccurrencesOf(result.getResponse().getContentAsString(), "<body")).isEqualTo(1);
        assertThat(result.getResponse().getContentAsString()).contains("_csrf");
    }

    @Test
    public void shouldSendSecurityHeadersOnPublicPages() throws Exception {
        MvcResult result = mockMvc.perform(get("/login").secure(true))
                // 检查点：验证该测试用例的预期结果。
                .andExpect(status().isOk())
                .andReturn();

        assertThat(result.getResponse().getHeader("Content-Security-Policy"))
                .contains("default-src 'self'", "script-src 'self'", "frame-ancestors 'none'", "object-src 'none'");
        // 检查点：验证该测试用例的预期结果。
        assertThat(result.getResponse().getHeader("X-Frame-Options")).isEqualTo("DENY");
        assertThat(result.getResponse().getHeader("X-Content-Type-Options")).isEqualTo("nosniff");
        assertThat(result.getResponse().getHeader("Referrer-Policy")).isEqualTo("same-origin");
        assertThat(result.getResponse().getHeader("Strict-Transport-Security"))
                .contains("max-age=31536000", "includeSubDomains", "preload");
        // 检查点：验证该测试用例的预期结果。
        assertThat(result.getResponse().getHeader("X-Powered-By")).isNull();
    }

    @Test
    public void shouldConfigureSecureSessionCookieAttributes() {
        Session.Cookie cookie = serverProperties.getServlet().getSession().getCookie();

        // 检查点：验证该测试用例的预期结果。
        assertThat(cookie.getHttpOnly()).isTrue();
        assertThat(cookie.getSecure()).isTrue();
        assertThat(cookie.getSameSite().attributeValue()).isEqualTo("Lax");
    }

    @Test
    public void shouldRenderSubresourceIntegrityAttributesForScriptsAndStyles() throws Exception {
        MvcResult result = mockMvc.perform(get("/login"))
                // 检查点：验证该测试用例的预期结果。
                .andExpect(status().isOk())
                .andReturn();

        assertThat(result.getResponse().getContentAsString())
                .contains("integrity=\"sha384-sRIl4kxILFvY47J16cr9ZwB07vP4J8+LH7qKQnuqkuIAvNWLzeN8tE5YBujZqJLB\"")
                .contains("integrity=\"sha384-YeVtiIqcef2Ks3IUSYAUM4bZgQB2GgJ6VHlr0vNn7PsVi5y8RiXJs7l809uRA3EB\"")
                .contains("integrity=\"sha384-FKyoEForCGlyvwx9Hj09JcYn3nv7wiPVlz7YYwJrWVcXK/BmnVDxM+D2scQbITxI\"")
                .contains("integrity=\"sha384-0loZMPILj/5xSk6XhuECab09NImwZHCThKrb77qWVgVEuL0W/Cz3Mt1zSTYdhgSe\"")
                .doesNotContain("onclick=")
                .doesNotContain("<script th:inline");
    }

    @Test
    public void shouldAllowOnlyConfiguredCorsOrigins() throws Exception {
        // 检查点：验证该测试用例的预期结果。
        mockMvc.perform(options("/api/user/notifications")
                        .header("Origin", "http://localhost:8080")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                // 检查点：验证该测试用例的预期结果。
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:8080"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));

        mockMvc.perform(options("/api/user/notifications")
                        .header("Origin", "https://evil.example")
                        .header("Access-Control-Request-Method", "GET"))
                // 检查点：验证该测试用例的预期结果。
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    @Test
    public void shouldReturnRegistrationFormWithValidationErrorsForInvalidRetailCustomer() throws Exception {
        // 检查点：验证该测试用例的预期结果。
        mockMvc.perform(post("/customers/new/retail")
                        .with(csrf())
                        .param("userName", "")
                        .param("password", "short")
                        .param("matchingPassword", "different")
                        .param("firstName", "")
                        .param("lastName", "")
                        .param("email", "not-an-email")
                        .param("mobile", "123")
                        .param("street", "x")
                        .param("postcode", "abc")
                        .param("city", ""))
                // 检查点：验证该测试用例的预期结果。
                .andExpect(status().isOk())
                .andExpect(view().name("users/createUserForm"))
                .andExpect(content().string(containsString("用户名不能为空")))
                .andExpect(content().string(containsString("邮箱格式不正确")));
    }

    @Test
    public void shouldCreateRetailCustomerFromValidRegistrationForm() throws Exception {
        String username = "ci" + Math.abs(System.nanoTime() % 1_000_000_000L);

        // 检查点：验证该测试用例的预期结果。
        mockMvc.perform(post("/customers/new/retail")
                        .with(csrf())
                        .param("userName", username)
                        .param("password", "qwerty")
                        .param("matchingPassword", "qwerty")
                        .param("firstName", "Ci")
                        .param("lastName", "User")
                        .param("email", username + "@example.com")
                        .param("mobile", "13800009999")
                        .param("street", "Test Street")
                        .param("postcode", "100000")
                        .param("city", "Shanghai"))
                // 检查点：验证该测试用例的预期结果。
                .andExpect(status().isOk())
                .andExpect(view().name("users/login"))
                .andExpect(content().string(containsString(username)));
    }

    @Test
    @WithUserDetails("customer_r")
    public void shouldRejectStateChangingRequestWithoutCsrfToken() throws Exception {
        // 检查点：验证该测试用例的预期结果。
        mockMvc.perform(post("/notifications/markAllAsRead"))
                .andExpect(status().isForbidden());
    }

    @Test
    public void shouldRedirectAnonymousUsersFromAppointmentPagesToLogin() throws Exception {
        // 检查点：验证该测试用例的预期结果。
        mockMvc.perform(get("/appointments/new"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @WithUserDetails("customer_r")
    public void shouldDenyCustomerAccessToAdminCustomerList() throws Exception {
        // 检查点：验证该测试用例的预期结果。
        mockMvc.perform(get("/customers/all"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails("provider")
    public void shouldDenyProviderAccessToCustomerPages() throws Exception {
        // 检查点：验证该测试用例的预期结果。
        mockMvc.perform(get("/customers/3"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails("provider")
    public void shouldDenyProviderAccessToCustomerAppointmentBookingFlow() throws Exception {
        // 检查点：验证该测试用例的预期结果。
        mockMvc.perform(get("/appointments/new"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails("customer_r")
    public void shouldDenyCustomerAccessToInvoiceAdministration() throws Exception {
        // 检查点：验证该测试用例的预期结果。
        mockMvc.perform(get("/invoices/all"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails("customer_r")
    public void shouldDenyCustomerFromIssuingInvoices() throws Exception {
        // 检查点：验证该测试用例的预期结果。
        mockMvc.perform(post("/invoices/issue").with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails("provider")
    public void shouldDenyProviderAccessToAnotherProviderAvailability() throws Exception {
        // 检查点：验证该测试用例的预期结果。
        mockMvc.perform(post("/providers/availability/breakes/add")
                        .with(csrf())
                        .param("planId", "1101")
                        .param("dayOfWeek", "monday")
                        .param("start", "10:00")
                        .param("end", "11:00"))
                // 检查点：验证该测试用例的预期结果。
                .andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails("customer_r")
    public void shouldDenyCustomerFromDeletingWorks() throws Exception {
        // 检查点：验证该测试用例的预期结果。
        mockMvc.perform(post("/works/delete")
                        .with(csrf())
                        .param("workId", "1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails("load_customer_01")
    public void shouldDenyCustomerAccessToAnotherCustomerProfile() throws Exception {
        // 检查点：验证该测试用例的预期结果。
        mockMvc.perform(get("/customers/3"))
                .andExpect(status().isForbidden());
    }

    @Test
    @Transactional
    @WithUserDetails("load_customer_01")
    public void shouldDenyCustomerAccessToAnotherCustomerAppointmentDetail() throws Exception {
        Appointment appointment = new Appointment(
                LocalDateTime.of(2033, 1, 10, 10, 0),
                LocalDateTime.of(2033, 1, 10, 11, 0),
                entityManager.getReference(Customer.class, 3),
                entityManager.getReference(Provider.class, 2),
                entityManager.getReference(Work.class, 1)
        );
        appointment.setStatus(AppointmentStatus.SCHEDULED);
        appointmentRepository.saveAndFlush(appointment);

        // 检查点：验证该测试用例的预期结果。
        mockMvc.perform(get("/appointments/" + appointment.getId()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails("customer_r")
    public void shouldAcceptStateChangingRequestWithCsrfToken() throws Exception {
        // 检查点：验证该测试用例的预期结果。
        mockMvc.perform(post("/notifications/markAllAsRead").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/notifications*"));
    }

    @Test
    @WithUserDetails("admin")
    public void shouldRenderServerPaginationForAdminLists() throws Exception {
        // 检查点：验证该测试用例的预期结果。
        mockMvc.perform(get("/customers/all").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("分页")))
                .andExpect(content().string(containsString("下一页")));
    }

    @Test
    public void shouldExposeActuatorHealthEndpoint() throws Exception {
        // 检查点：验证该测试用例的预期结果。
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    public void shouldNotExposeSensitiveActuatorEndpointsToAnonymousUsers() throws Exception {
        // 检查点：验证该测试用例的预期结果。
        mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));

        mockMvc.perform(get("/actuator/info"))
                // 检查点：验证该测试用例的预期结果。
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @WithUserDetails("admin")
    public void shouldDisableSensitiveActuatorEndpointMappings() throws Exception {
        // 检查点：验证该测试用例的预期结果。
        mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/actuator/info"))
                .andExpect(status().isNotFound());
    }

    @Test
    public void shouldRegisterAsyncInfrastructureBeans() {
        // 检查点：验证该测试用例的预期结果。
        assertThat(mailExecutor).isNotNull();
        assertThat(taskScheduler).isNotNull();
    }
}
