package com.example.slabiak.appointmentscheduler.service.appointment;

import com.example.slabiak.appointmentscheduler.dao.AppointmentRepository;
import com.example.slabiak.appointmentscheduler.entity.Appointment;
import com.example.slabiak.appointmentscheduler.entity.AppointmentStatus;
import com.example.slabiak.appointmentscheduler.entity.Work;
import com.example.slabiak.appointmentscheduler.entity.user.User;
import com.example.slabiak.appointmentscheduler.entity.user.customer.Customer;
import com.example.slabiak.appointmentscheduler.entity.user.provider.Provider;
import com.example.slabiak.appointmentscheduler.service.NotificationService;
import com.example.slabiak.appointmentscheduler.service.UserService;
import com.example.slabiak.appointmentscheduler.service.WorkService;
import com.example.slabiak.appointmentscheduler.service.impl.AppointmentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@org.junit.jupiter.api.extension.ExtendWith(MockitoExtension.class)
public class AppointmentStatusTransitionTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private UserService userService;

    @Mock
    private WorkService workService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private AppointmentServiceImpl appointmentService;

    private Customer customer;
    private Provider provider;
    private Work editableWork;
    @BeforeEach
    public void setUp() {
        customer = new Customer();
        customer.setId(1);
        provider = new Provider();
        provider.setId(2);
        editableWork = new Work();
        editableWork.setEditable(true);
    }

    @Test
    public void shouldMarkPastScheduledAppointmentAsFinishedForUser() {
        Appointment scheduled = appointment(AppointmentStatus.SCHEDULED, LocalDateTime.now().minusHours(2));
        when(appointmentRepository.findScheduledByUserIdWithEndBeforeDate(org.mockito.ArgumentMatchers.any(LocalDateTime.class), org.mockito.ArgumentMatchers.eq(customer.getId())))
                .thenReturn(List.of(scheduled));
        when(appointmentRepository.findFinishedByUserIdWithEndBeforeDate(org.mockito.ArgumentMatchers.any(LocalDateTime.class), org.mockito.ArgumentMatchers.eq(customer.getId())))
                .thenReturn(List.of());

        appointmentService.updateUserAppointmentsStatuses(customer.getId());

        assertThat(scheduled.getStatus()).isEqualTo(AppointmentStatus.FINISHED);
        verify(appointmentRepository).save(scheduled);
    }

    @Test
    public void shouldMarkOldFinishedAppointmentAsInvoicedForUser() {
        Appointment finished = appointment(AppointmentStatus.FINISHED, LocalDateTime.now().minusDays(2));
        when(appointmentRepository.findScheduledByUserIdWithEndBeforeDate(org.mockito.ArgumentMatchers.any(LocalDateTime.class), org.mockito.ArgumentMatchers.eq(customer.getId())))
                .thenReturn(List.of());
        when(appointmentRepository.findFinishedByUserIdWithEndBeforeDate(org.mockito.ArgumentMatchers.any(LocalDateTime.class), org.mockito.ArgumentMatchers.eq(customer.getId())))
                .thenReturn(List.of(finished));

        appointmentService.updateUserAppointmentsStatuses(customer.getId());

        assertThat(finished.getStatus()).isEqualTo(AppointmentStatus.INVOICED);
        verify(appointmentRepository).save(finished);
    }

    @Test
    public void shouldCancelAppointmentWhenCustomerOwnsIt() {
        Appointment scheduled = appointment(AppointmentStatus.SCHEDULED, LocalDateTime.now().plusDays(3));
        scheduled.setId(10);
        when(appointmentRepository.findById(scheduled.getId())).thenReturn(Optional.of(scheduled));
        User canceler = new User();
        canceler.setId(customer.getId());
        when(userService.getUserById(customer.getId())).thenReturn(canceler);

        appointmentService.cancelUserAppointmentById(scheduled.getId(), customer.getId());

        assertThat(scheduled.getStatus()).isEqualTo(AppointmentStatus.CANCELED);
        assertThat(scheduled.getCanceler()).isEqualTo(canceler);
        assertThat(scheduled.getCanceledAt()).isNotNull();
        verify(appointmentRepository).save(scheduled);
        verify(notificationService).newAppointmentCanceledByCustomerNotification(scheduled, true);
    }

    @Test
    public void shouldCancelAppointmentWhenProviderOwnsIt() {
        Appointment scheduled = appointment(AppointmentStatus.SCHEDULED, LocalDateTime.now().plusDays(3));
        scheduled.setId(15);
        when(appointmentRepository.findById(scheduled.getId())).thenReturn(Optional.of(scheduled));
        User canceler = new User();
        canceler.setId(provider.getId());
        when(userService.getUserById(provider.getId())).thenReturn(canceler);

        appointmentService.cancelUserAppointmentById(scheduled.getId(), provider.getId());

        assertThat(scheduled.getStatus()).isEqualTo(AppointmentStatus.CANCELED);
        assertThat(scheduled.getCanceler()).isEqualTo(canceler);
        verify(appointmentRepository).save(scheduled);
        verify(notificationService).newAppointmentCanceledByProviderNotification(scheduled, true);
    }

    @Test
    public void shouldDenyCancelWhenUserDoesNotBelongToAppointment() {
        Appointment scheduled = appointment(AppointmentStatus.SCHEDULED, LocalDateTime.now().plusDays(3));
        scheduled.setId(16);
        when(appointmentRepository.findById(scheduled.getId())).thenReturn(Optional.of(scheduled));

        assertThatThrownBy(() -> appointmentService.cancelUserAppointmentById(scheduled.getId(), 999))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    public void shouldExplainCustomerCannotCancelLessThanTwentyFourHoursBeforeStart() {
        Appointment scheduled = appointment(AppointmentStatus.SCHEDULED, LocalDateTime.now().plusHours(2));
        scheduled.setId(17);
        when(appointmentRepository.findById(scheduled.getId())).thenReturn(Optional.of(scheduled));

        String reason = appointmentService.getCancelNotAllowedReason(customer.getId(), scheduled.getId());

        assertThat(reason).isEqualTo("距离开始不足 24 小时的预约不能取消。");
    }

    @Test
    public void shouldExplainCustomerCannotCancelAfterMonthlyLimit() {
        Appointment scheduled = appointment(AppointmentStatus.SCHEDULED, LocalDateTime.now().plusDays(3));
        scheduled.setId(18);
        when(appointmentRepository.findById(scheduled.getId())).thenReturn(Optional.of(scheduled));
        when(appointmentRepository.findByCustomerIdCanceledAfterDate(org.mockito.ArgumentMatchers.eq(customer.getId()), org.mockito.ArgumentMatchers.any(LocalDateTime.class)))
                .thenReturn(List.of(appointment(AppointmentStatus.CANCELED, LocalDateTime.now().minusDays(1))));

        String reason = appointmentService.getCancelNotAllowedReason(customer.getId(), scheduled.getId());

        assertThat(reason).isEqualTo("本月取消次数已达上限，无法取消该预约。");
    }

    @Test
    public void shouldExplainOnlyScheduledAppointmentsCanBeCanceled() {
        Appointment finished = appointment(AppointmentStatus.FINISHED, LocalDateTime.now().minusHours(1));
        finished.setId(19);
        when(appointmentRepository.findById(finished.getId())).thenReturn(Optional.of(finished));

        String reason = appointmentService.getCancelNotAllowedReason(customer.getId(), finished.getId());

        assertThat(reason).isEqualTo("只有已预约状态的预约可以取消。");
    }

    @Test
    public void shouldAllowCustomerToRequestRejectionOnlyForRecentlyFinishedAppointment() {
        Appointment finished = appointment(AppointmentStatus.FINISHED, LocalDateTime.now().minusHours(2));
        finished.setId(11);
        when(appointmentRepository.findById(finished.getId())).thenReturn(Optional.of(finished));

        boolean requested = appointmentService.requestAppointmentRejection(finished.getId(), customer.getId());

        assertThat(requested).isTrue();
        assertThat(finished.getStatus()).isEqualTo(AppointmentStatus.REJECTION_REQUESTED);
        verify(notificationService).newAppointmentRejectionRequestedNotification(finished, true);
        verify(appointmentRepository).save(finished);
    }

    @Test
    public void shouldRejectCustomerRejectionRequestAfterOneDayWindow() {
        Appointment finished = appointment(AppointmentStatus.FINISHED, LocalDateTime.now().minusDays(2));
        finished.setId(12);
        when(appointmentRepository.findById(finished.getId())).thenReturn(Optional.of(finished));

        boolean requested = appointmentService.requestAppointmentRejection(finished.getId(), customer.getId());

        assertThat(requested).isFalse();
        assertThat(finished.getStatus()).isEqualTo(AppointmentStatus.FINISHED);
    }

    @Test
    public void shouldAllowProviderToAcceptRejectionRequest() {
        Appointment requested = appointment(AppointmentStatus.REJECTION_REQUESTED, LocalDateTime.now().minusHours(2));
        requested.setId(13);
        when(appointmentRepository.findById(requested.getId())).thenReturn(Optional.of(requested));

        boolean accepted = appointmentService.acceptRejection(requested.getId(), provider.getId());

        assertThat(accepted).isTrue();
        assertThat(requested.getStatus()).isEqualTo(AppointmentStatus.REJECTED);
        verify(notificationService).newAppointmentRejectionAcceptedNotification(requested, true);
        verify(appointmentRepository).save(requested);
    }

    @Test
    public void shouldNotAllowWrongProviderToAcceptRejectionRequest() {
        Appointment requested = appointment(AppointmentStatus.REJECTION_REQUESTED, LocalDateTime.now().minusHours(2));
        requested.setId(14);
        when(appointmentRepository.findById(requested.getId())).thenReturn(Optional.of(requested));

        boolean accepted = appointmentService.acceptRejection(requested.getId(), 999);

        assertThat(accepted).isFalse();
        assertThat(requested.getStatus()).isEqualTo(AppointmentStatus.REJECTION_REQUESTED);
    }

    private Appointment appointment(AppointmentStatus status, LocalDateTime end) {
        Appointment appointment = new Appointment();
        appointment.setCustomer(customer);
        appointment.setProvider(provider);
        appointment.setWork(editableWork);
        appointment.setStatus(status);
        appointment.setStart(end.minusHours(1));
        appointment.setEnd(end);
        return appointment;
    }
}
