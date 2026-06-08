// 测试说明：验证预约控制器的页面跳转、表单处理和权限相关行为。
package com.example.slabiak.appointmentscheduler.controller;

import com.example.slabiak.appointmentscheduler.entity.Appointment;
import com.example.slabiak.appointmentscheduler.entity.AppointmentStatus;
import com.example.slabiak.appointmentscheduler.entity.Work;
import com.example.slabiak.appointmentscheduler.entity.user.provider.Provider;
import com.example.slabiak.appointmentscheduler.security.CustomUserDetails;
import com.example.slabiak.appointmentscheduler.service.AppointmentService;
import com.example.slabiak.appointmentscheduler.service.ExchangeService;
import com.example.slabiak.appointmentscheduler.service.UserService;
import com.example.slabiak.appointmentscheduler.service.WorkService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AppointmentControllerTest {

    @Mock
    private WorkService workService;

    @Mock
    private UserService userService;

    @Mock
    private AppointmentService appointmentService;

    @Mock
    private ExchangeService exchangeService;

    private AppointmentController controller;

    @BeforeEach
    public void setUp() {
        controller = new AppointmentController(workService, userService, appointmentService, exchangeService);
    }

    @Test
    public void shouldShowCustomerAppointmentsAndPaginationQuery() {
        Model model = new ExtendedModelMap();
        PageRequest pageable = PageRequest.of(0, 20);
        Page<Appointment> page = new PageImpl<>(List.of(new Appointment()), pageable, 1);
        when(appointmentService.getAppointmentByCustomerId(3, AppointmentStatus.SCHEDULED, pageable)).thenReturn(page);

        String view = controller.showAllAppointments(model, user(3, "ROLE_CUSTOMER"), AppointmentStatus.SCHEDULED, pageable);

        // 检查点：验证该测试用例的预期结果。
        assertThat(view).isEqualTo("appointments/listAppointments");
        assertThat(model.getAttribute("appointments")).isEqualTo(page);
        assertThat(model.getAttribute("paginationQuery")).isEqualTo("status=SCHEDULED");
    }

    @Test
    public void shouldShowProviderAndAdminAppointments() {
        PageRequest pageable = PageRequest.of(0, 20);
        Page<Appointment> page = Page.empty(pageable);
        when(appointmentService.getAppointmentByProviderId(2, null, pageable)).thenReturn(page);
        when(appointmentService.getAllAppointments(null, pageable)).thenReturn(page);

        String providerView = controller.showAllAppointments(new ExtendedModelMap(), user(2, "ROLE_PROVIDER"), null, pageable);
        String adminView = controller.showAllAppointments(new ExtendedModelMap(), user(1, "ROLE_ADMIN"), null, pageable);

        // 检查点：验证该测试用例的预期结果。
        assertThat(providerView).isEqualTo("appointments/listAppointments");
        assertThat(adminView).isEqualTo("appointments/listAppointments");
    }

    @Test
    public void shouldShowAppointmentDetailWithRemainingRejectionTime() {
        Appointment appointment = new Appointment();
        appointment.setEnd(LocalDateTime.now().plusHours(2));
        when(appointmentService.getAppointmentByIdWithAuthorization(10)).thenReturn(appointment);
        when(appointmentService.isCustomerAllowedToRejectAppointment(3, 10)).thenReturn(true);
        when(appointmentService.isProviderAllowedToAcceptRejection(3, 10)).thenReturn(false);
        when(exchangeService.checkIfEligibleForExchange(3, 10)).thenReturn(true);
        when(appointmentService.getCancelNotAllowedReason(3, 10)).thenReturn(null);
        Model model = new ExtendedModelMap();

        String view = controller.showAppointmentDetail(10, model, user(3, "ROLE_CUSTOMER"));

        // 检查点：验证该测试用例的预期结果。
        assertThat(view).isEqualTo("appointments/appointmentDetail");
        assertThat(model.getAttribute("allowedToRequestRejection")).isEqualTo(true);
        assertThat(model.getAttribute("allowedToExchange")).isEqualTo(true);
        assertThat(model.getAttribute("allowedToCancel")).isEqualTo(true);
        // 检查点：验证该测试用例的预期结果。
        assertThat(model.getAttribute("remainingTime")).isNotNull();
    }

    @Test
    public void shouldHandleRejectionActionsAndChatMessage() {
        Model model = new ExtendedModelMap();
        when(appointmentService.requestAppointmentRejection(10, 3)).thenReturn(true);
        when(appointmentService.requestAppointmentRejection("token")).thenReturn(true);
        when(appointmentService.acceptRejection(10, 2)).thenReturn(true);
        when(appointmentService.acceptRejection("accept-token")).thenReturn(true);

        // 检查点：验证该测试用例的预期结果。
        assertThat(controller.processAppointmentRejectionRequest(10, user(3, "ROLE_CUSTOMER"), model)).isEqualTo("appointments/rejectionConfirmation");
        assertThat(controller.processAppointmentRejectionRequest("token", new ExtendedModelMap())).isEqualTo("appointments/rejectionConfirmation");
        assertThat(controller.acceptAppointmentRejectionRequest(10, user(2, "ROLE_PROVIDER"), new ExtendedModelMap())).isEqualTo("appointments/rejectionConfirmation");
        assertThat(controller.acceptAppointmentRejectionRequest("accept-token", new ExtendedModelMap())).isEqualTo("appointments/rejectionConfirmation");

        // 检查点：验证该测试用例的预期结果。
        assertThat(controller.addNewChatMessage(new com.example.slabiak.appointmentscheduler.entity.ChatMessage(), 10, user(3, "ROLE_CUSTOMER")))
                .isEqualTo("redirect:/appointments/10");
        verify(appointmentService).addMessageToAppointmentChat(anyInt(), anyInt(), any());
    }

    @Test
    public void shouldDriveCustomerBookingFlow() {
        Model model = new ExtendedModelMap();
        CustomUserDetails retailCustomer = user(3, "ROLE_CUSTOMER", "ROLE_CUSTOMER_RETAIL");
        when(workService.isWorkForCustomer(1, 3)).thenReturn(true);
        when(appointmentService.isAvailable(1, 2, 3, LocalDateTime.parse("2032-01-20T10:00"))).thenReturn(true);
        Work work = new Work();
        work.setDuration(60);
        when(workService.getWorkById(1)).thenReturn(work);
        Provider provider = new Provider();
        provider.setFirstName("Provider");
        provider.setLastName("One");
        when(userService.getProviderById(2)).thenReturn(provider);

        // 检查点：验证该测试用例的预期结果。
        assertThat(controller.selectProvider(model, retailCustomer)).isEqualTo("appointments/selectProvider");
        assertThat(controller.selectService(2, model, retailCustomer)).isEqualTo("appointments/selectService");
        assertThat(controller.selectDate(1, 2, model, retailCustomer)).isEqualTo("appointments/selectDate");
        assertThat(controller.showNewAppointmentSummary(1, 2, "2032-01-20T10:00", model, retailCustomer))
                .isEqualTo("appointments/newAppointmentSummary");
        // 检查点：验证该测试用例的预期结果。
        assertThat(controller.bookAppointment(1, 2, "2032-01-20T10:00", retailCustomer)).isEqualTo("redirect:/appointments/all");

        verify(appointmentService).createNewAppointment(1, 2, 3, LocalDateTime.parse("2032-01-20T10:00"));
    }

    @Test
    public void shouldRedirectWhenCustomerCannotSelectWorkOrTime() {
        CustomUserDetails customer = user(3, "ROLE_CUSTOMER");
        when(workService.isWorkForCustomer(1, 3)).thenReturn(false);
        when(appointmentService.isAvailable(1, 2, 3, LocalDateTime.parse("2032-01-20T10:00"))).thenReturn(false);

        // 检查点：验证该测试用例的预期结果。
        assertThat(controller.selectDate(1, 2, new ExtendedModelMap(), customer)).isEqualTo("redirect:/appointments/new");
        assertThat(controller.showNewAppointmentSummary(1, 2, "2032-01-20T10:00", new ExtendedModelMap(), customer))
                .isEqualTo("redirect:/appointments/new");
    }

    @Test
    public void shouldCancelAppointmentAndFormatDuration() {
        CustomUserDetails customer = user(3, "ROLE_CUSTOMER");

        // 检查点：验证该测试用例的预期结果。
        assertThat(controller.cancelAppointment(10, customer)).isEqualTo("redirect:/appointments/all");
        assertThat(AppointmentController.formatDuration(Duration.ofMinutes(125))).isEqualTo("2h05m");
        verify(appointmentService).cancelUserAppointmentById(10, 3);
    }

    private CustomUserDetails user(int id, String... roles) {
        return new CustomUserDetails(
                id,
                "First",
                "Last",
                "user" + id,
                "user" + id + "@example.com",
                "password",
                java.util.Arrays.stream(roles).map(SimpleGrantedAuthority::new).toList()
        );
    }
}
