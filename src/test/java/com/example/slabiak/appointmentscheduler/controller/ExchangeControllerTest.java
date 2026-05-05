package com.example.slabiak.appointmentscheduler.controller;

import com.example.slabiak.appointmentscheduler.entity.Appointment;
import com.example.slabiak.appointmentscheduler.security.CustomUserDetails;
import com.example.slabiak.appointmentscheduler.service.AppointmentService;
import com.example.slabiak.appointmentscheduler.service.ExchangeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExchangeControllerTest {

    @Mock
    private ExchangeService exchangeService;

    @Mock
    private AppointmentService appointmentService;

    private ExchangeController controller;

    @BeforeEach
    void setUp() {
        controller = new ExchangeController(exchangeService, appointmentService);
    }

    @Test
    void shouldShowEligibleAppointmentsOrRedirectWhenNotEligible() {
        CustomUserDetails user = user(3);
        List<Appointment> eligibleAppointments = List.of(new Appointment(), new Appointment());
        when(exchangeService.checkIfEligibleForExchange(3, 10)).thenReturn(true);
        when(exchangeService.getEligibleAppointmentsForExchange(10)).thenReturn(eligibleAppointments);
        Model model = new ExtendedModelMap();

        assertThat(controller.showEligibleAppointmentsToExchange(10, model, user)).isEqualTo("exchange/listProposals");
        assertThat(model.getAttribute("appointmentId")).isEqualTo(10);
        assertThat(model.getAttribute("numberOfEligibleAppointments")).isEqualTo(2);
        assertThat(model.getAttribute("eligibleAppointments")).isEqualTo(eligibleAppointments);

        when(exchangeService.checkIfEligibleForExchange(3, 11)).thenReturn(false);
        assertThat(controller.showEligibleAppointmentsToExchange(11, new ExtendedModelMap(), user))
                .isEqualTo("redirect:/appointments/all");
    }

    @Test
    void shouldShowExchangeSummaryOrRedirectWhenExchangeIsImpossible() {
        CustomUserDetails user = user(3);
        Appointment oldAppointment = new Appointment();
        Appointment newAppointment = new Appointment();
        when(exchangeService.checkIfExchangeIsPossible(10, 20, 3)).thenReturn(true);
        when(appointmentService.getAppointmentByIdWithAuthorization(10)).thenReturn(oldAppointment);
        when(appointmentService.getAppointmentById(20)).thenReturn(newAppointment);
        Model model = new ExtendedModelMap();

        assertThat(controller.showExchangeSummaryScreen(10, 20, model, user)).isEqualTo("exchange/exchangeSummary");
        assertThat(model.getAttribute("oldAppointment")).isSameAs(oldAppointment);
        assertThat(model.getAttribute("newAppointment")).isSameAs(newAppointment);

        when(exchangeService.checkIfExchangeIsPossible(10, 21, 3)).thenReturn(false);
        assertThat(controller.showExchangeSummaryScreen(10, 21, new ExtendedModelMap(), user))
                .isEqualTo("redirect:/appointments/all");
    }

    @Test
    void shouldProcessExchangeRequestAndModerationActions() {
        CustomUserDetails user = user(3);
        Model successModel = new ExtendedModelMap();
        Model failureModel = new ExtendedModelMap();
        when(exchangeService.requestExchange(10, 20, 3)).thenReturn(true);
        when(exchangeService.requestExchange(10, 21, 3)).thenReturn(false);

        assertThat(controller.processExchangeRequest(10, 20, successModel, user)).isEqualTo("exchange/requestConfirmation");
        assertThat(successModel.getAttribute("message")).isEqualTo("换约请求已发送。");
        assertThat(controller.processExchangeRequest(10, 21, failureModel, user)).isEqualTo("exchange/requestConfirmation");
        assertThat(failureModel.getAttribute("message")).isEqualTo("换约请求发送失败。");
        assertThat(controller.processExchangeAcceptation(5, new ExtendedModelMap(), user)).isEqualTo("redirect:/appointments/all");
        assertThat(controller.processExchangeRejection(6, new ExtendedModelMap(), user)).isEqualTo("redirect:/appointments/all");

        verify(exchangeService).acceptExchange(5, 3);
        verify(exchangeService).rejectExchange(6, 3);
    }

    private CustomUserDetails user(int id) {
        return new CustomUserDetails(
                id,
                "First",
                "Last",
                "user" + id,
                "user" + id + "@example.com",
                "password",
                List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))
        );
    }
}
