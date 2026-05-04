package com.example.slabiak.appointmentscheduler.service.exchange;

import com.example.slabiak.appointmentscheduler.dao.AppointmentRepository;
import com.example.slabiak.appointmentscheduler.dao.ExchangeRequestRepository;
import com.example.slabiak.appointmentscheduler.entity.Appointment;
import com.example.slabiak.appointmentscheduler.entity.AppointmentStatus;
import com.example.slabiak.appointmentscheduler.entity.ExchangeRequest;
import com.example.slabiak.appointmentscheduler.entity.ExchangeStatus;
import com.example.slabiak.appointmentscheduler.entity.Work;
import com.example.slabiak.appointmentscheduler.entity.user.customer.Customer;
import com.example.slabiak.appointmentscheduler.entity.user.provider.Provider;
import com.example.slabiak.appointmentscheduler.service.NotificationService;
import com.example.slabiak.appointmentscheduler.service.impl.ExchangeServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@org.junit.jupiter.api.extension.ExtendWith(MockitoExtension.class)
public class ExchangeServiceTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private ExchangeRequestRepository exchangeRequestRepository;

    private ExchangeServiceImpl exchangeService;
    @BeforeEach
    public void setUp() {
        exchangeService = new ExchangeServiceImpl(appointmentRepository, notificationService, exchangeRequestRepository);
    }

    @Test
    public void shouldRequestExchangeAndMarkRequestorAppointmentAsExchangeRequested() {
        Appointment oldAppointment = appointment(10, 3, 2, LocalDateTime.now().plusDays(5));
        Appointment newAppointment = appointment(11, 1001, 2, LocalDateTime.now().plusDays(6));
        when(appointmentRepository.findById(oldAppointment.getId())).thenReturn(Optional.of(oldAppointment));
        when(appointmentRepository.findById(newAppointment.getId())).thenReturn(Optional.of(newAppointment));

        boolean requested = exchangeService.requestExchange(oldAppointment.getId(), newAppointment.getId(), 3);

        assertThat(requested).isTrue();
        assertThat(oldAppointment.getStatus()).isEqualTo(AppointmentStatus.EXCHANGE_REQUESTED);
        verify(appointmentRepository).save(oldAppointment);
        verify(exchangeRequestRepository).save(org.mockito.ArgumentMatchers.argThat(exchangeRequest ->
                exchangeRequest.getRequestor() == oldAppointment
                        && exchangeRequest.getRequested() == newAppointment
                        && exchangeRequest.getStatus() == ExchangeStatus.PENDING));
        verify(notificationService).newExchangeRequestedNotification(oldAppointment, newAppointment, true);
    }

    @Test
    public void shouldAcceptExchangeAndSwapAppointmentCustomers() {
        Appointment requestor = appointment(10, 3, 2, LocalDateTime.now().plusDays(5));
        Appointment requested = appointment(11, 1001, 2, LocalDateTime.now().plusDays(6));
        ExchangeRequest exchangeRequest = new ExchangeRequest(requestor, requested, ExchangeStatus.PENDING);
        exchangeRequest.setId(20);
        when(exchangeRequestRepository.findById(exchangeRequest.getId())).thenReturn(Optional.of(exchangeRequest));

        boolean accepted = exchangeService.acceptExchange(exchangeRequest.getId(), 1001);

        assertThat(accepted).isTrue();
        assertThat(exchangeRequest.getStatus()).isEqualTo(ExchangeStatus.ACCEPTED);
        assertThat(requestor.getStatus()).isEqualTo(AppointmentStatus.SCHEDULED);
        assertThat(requestor.getCustomer().getId()).isEqualTo(1001);
        assertThat(requested.getCustomer().getId()).isEqualTo(3);
        verify(exchangeRequestRepository).save(exchangeRequest);
        verify(appointmentRepository).save(requested);
        verify(appointmentRepository).save(requestor);
        verify(notificationService).newExchangeAcceptedNotification(exchangeRequest, true);
    }

    @Test
    public void shouldRejectExchangeAndRestoreRequestorAppointmentStatus() {
        Appointment requestor = appointment(10, 3, 2, LocalDateTime.now().plusDays(5));
        requestor.setStatus(AppointmentStatus.EXCHANGE_REQUESTED);
        Appointment requested = appointment(11, 1001, 2, LocalDateTime.now().plusDays(6));
        ExchangeRequest exchangeRequest = new ExchangeRequest(requestor, requested, ExchangeStatus.PENDING);
        exchangeRequest.setId(20);
        when(exchangeRequestRepository.findById(exchangeRequest.getId())).thenReturn(Optional.of(exchangeRequest));

        boolean rejected = exchangeService.rejectExchange(exchangeRequest.getId(), 1001);

        assertThat(rejected).isTrue();
        assertThat(exchangeRequest.getStatus()).isEqualTo(ExchangeStatus.REJECTED);
        assertThat(requestor.getStatus()).isEqualTo(AppointmentStatus.SCHEDULED);
        verify(exchangeRequestRepository).save(exchangeRequest);
        verify(appointmentRepository).save(requestor);
        verify(notificationService).newExchangeRejectedNotification(exchangeRequest, true);
    }

    @Test
    public void shouldDenyExchangeDecisionByUserWhoDoesNotOwnRequestedAppointment() {
        Appointment requestor = appointment(10, 3, 2, LocalDateTime.now().plusDays(5));
        Appointment requested = appointment(11, 1001, 2, LocalDateTime.now().plusDays(6));
        ExchangeRequest exchangeRequest = new ExchangeRequest(requestor, requested, ExchangeStatus.PENDING);
        exchangeRequest.setId(20);
        when(exchangeRequestRepository.findById(exchangeRequest.getId())).thenReturn(Optional.of(exchangeRequest));

        assertThatThrownBy(() -> exchangeService.acceptExchange(exchangeRequest.getId(), 3))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    public void shouldDenyExchangeRequestByUserWhoDoesNotOwnOldAppointment() {
        Appointment oldAppointment = appointment(10, 3, 2, LocalDateTime.now().plusDays(5));
        Appointment newAppointment = appointment(11, 1001, 2, LocalDateTime.now().plusDays(6));
        when(appointmentRepository.findById(oldAppointment.getId())).thenReturn(Optional.of(oldAppointment));
        when(appointmentRepository.findById(newAppointment.getId())).thenReturn(Optional.of(newAppointment));

        assertThatThrownBy(() -> exchangeService.requestExchange(oldAppointment.getId(), newAppointment.getId(), 1001))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    public void shouldNotRequestExchangeWhenOldAppointmentIsNotScheduled() {
        Appointment oldAppointment = appointment(10, 3, 2, LocalDateTime.now().plusDays(5));
        oldAppointment.setStatus(AppointmentStatus.CANCELED);
        Appointment newAppointment = appointment(11, 1001, 2, LocalDateTime.now().plusDays(6));
        when(appointmentRepository.findById(oldAppointment.getId())).thenReturn(Optional.of(oldAppointment));
        when(appointmentRepository.findById(newAppointment.getId())).thenReturn(Optional.of(newAppointment));

        boolean requested = exchangeService.requestExchange(oldAppointment.getId(), newAppointment.getId(), 3);

        assertThat(requested).isFalse();
        verify(exchangeRequestRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    public void shouldNotRequestExchangeWhenNewAppointmentIsNotScheduled() {
        Appointment oldAppointment = appointment(10, 3, 2, LocalDateTime.now().plusDays(5));
        Appointment newAppointment = appointment(11, 1001, 2, LocalDateTime.now().plusDays(6));
        newAppointment.setStatus(AppointmentStatus.CANCELED);
        when(appointmentRepository.findById(oldAppointment.getId())).thenReturn(Optional.of(oldAppointment));
        when(appointmentRepository.findById(newAppointment.getId())).thenReturn(Optional.of(newAppointment));

        boolean requested = exchangeService.requestExchange(oldAppointment.getId(), newAppointment.getId(), 3);

        assertThat(requested).isFalse();
        verify(exchangeRequestRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    public void shouldNotAcceptAlreadyRejectedExchange() {
        Appointment requestor = appointment(10, 3, 2, LocalDateTime.now().plusDays(5));
        Appointment requested = appointment(11, 1001, 2, LocalDateTime.now().plusDays(6));
        ExchangeRequest exchangeRequest = new ExchangeRequest(requestor, requested, ExchangeStatus.REJECTED);
        exchangeRequest.setId(20);
        when(exchangeRequestRepository.findById(exchangeRequest.getId())).thenReturn(Optional.of(exchangeRequest));

        boolean accepted = exchangeService.acceptExchange(exchangeRequest.getId(), 1001);

        assertThat(accepted).isFalse();
        assertThat(requestor.getCustomer().getId()).isEqualTo(3);
        assertThat(requested.getCustomer().getId()).isEqualTo(1001);
        verify(notificationService, never()).newExchangeAcceptedNotification(exchangeRequest, true);
    }

    @Test
    public void shouldNotRejectAlreadyAcceptedExchange() {
        Appointment requestor = appointment(10, 3, 2, LocalDateTime.now().plusDays(5));
        Appointment requested = appointment(11, 1001, 2, LocalDateTime.now().plusDays(6));
        ExchangeRequest exchangeRequest = new ExchangeRequest(requestor, requested, ExchangeStatus.ACCEPTED);
        exchangeRequest.setId(20);
        when(exchangeRequestRepository.findById(exchangeRequest.getId())).thenReturn(Optional.of(exchangeRequest));

        boolean rejected = exchangeService.rejectExchange(exchangeRequest.getId(), 1001);

        assertThat(rejected).isFalse();
        assertThat(exchangeRequest.getStatus()).isEqualTo(ExchangeStatus.ACCEPTED);
        verify(notificationService, never()).newExchangeRejectedNotification(exchangeRequest, true);
    }

    private Appointment appointment(int appointmentId, int customerId, int providerId, LocalDateTime start) {
        Customer customer = new Customer();
        customer.setId(customerId);
        Provider provider = new Provider();
        provider.setId(providerId);
        Work work = new Work();
        work.setId(1);
        Appointment appointment = new Appointment(start, start.plusHours(1), customer, provider, work);
        appointment.setId(appointmentId);
        appointment.setStatus(AppointmentStatus.SCHEDULED);
        return appointment;
    }
}
