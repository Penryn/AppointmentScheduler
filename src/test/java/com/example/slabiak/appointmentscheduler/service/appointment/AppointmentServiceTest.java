// 测试说明：验证预约服务的创建、查询、聊天、通知和状态更新行为。
package com.example.slabiak.appointmentscheduler.service.appointment;

import com.example.slabiak.appointmentscheduler.dao.AppointmentRepository;
import com.example.slabiak.appointmentscheduler.dao.ChatMessageRepository;
import com.example.slabiak.appointmentscheduler.entity.Appointment;
import com.example.slabiak.appointmentscheduler.entity.AppointmentStatus;
import com.example.slabiak.appointmentscheduler.entity.ChatMessage;
import com.example.slabiak.appointmentscheduler.entity.Work;
import com.example.slabiak.appointmentscheduler.entity.WorkingPlan;
import com.example.slabiak.appointmentscheduler.entity.user.customer.Customer;
import com.example.slabiak.appointmentscheduler.entity.user.provider.Provider;
import com.example.slabiak.appointmentscheduler.exception.AppointmentNotFoundException;
import com.example.slabiak.appointmentscheduler.service.EmailService;
import com.example.slabiak.appointmentscheduler.service.NotificationService;
import com.example.slabiak.appointmentscheduler.service.UserService;
import com.example.slabiak.appointmentscheduler.service.WorkService;
import com.example.slabiak.appointmentscheduler.service.impl.AppointmentServiceImpl;
import com.example.slabiak.appointmentscheduler.service.impl.JwtTokenServiceImpl;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

@org.junit.jupiter.api.extension.ExtendWith(MockitoExtension.class)
public class AppointmentServiceTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private WorkService workService;

    @Mock
    private EmailService emailService;

    @Mock
    private UserService userService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private JwtTokenServiceImpl jwtTokenService;


    @InjectMocks
    private AppointmentServiceImpl appointmentService;


    private int customerId;
    private int providerId;
    private int workId;

    private Appointment appointment;
    private Optional<Appointment> optionalAppointment;
    private List<Appointment> appointments;
    private int appointmentId;
    private Work work;
    private Provider provider;
    private Customer customer;
    @BeforeEach
    public void initObjects() {

        customerId = 1;
        providerId = 2;
        workId = 3;
        work = new Work();
        work.setId(workId);
        work.setDuration(60);
        provider = new Provider();
        provider.setId(providerId);
        provider.setWorkingPlan(WorkingPlan.generateDefaultWorkingPlan());
        customer = new Customer();
        customer.setId(customerId);
        appointment = new Appointment();
        appointmentId = 1;
        appointment.setId(appointmentId);
        appointment.setCustomer(customer);
        appointment.setProvider(provider);
        appointment.setWork(work);
        appointment.setStart(LocalDateTime.now().plusDays(3));
        appointment.setEnd(LocalDateTime.now().plusDays(3).plusHours(1));
        appointment.setStatus(AppointmentStatus.SCHEDULED);
        optionalAppointment = Optional.of(appointment);
        appointments = new ArrayList<>();
        appointments.add(appointment);

    }

    @Test
    public void shouldBookAppointmentWhenAllConditionsMet() {
        LocalDateTime startOfNewAppointment = LocalDateTime.of(2019, 1, 1, 6, 0);

        when(workService.isWorkForCustomer(workId, customerId)).thenReturn(true);
        when(workService.getWorkById(workId)).thenReturn(work);
        when(userService.getProviderById(providerId)).thenReturn(provider);
        when(userService.getCustomerById(customerId)).thenReturn(customer);

        ArgumentCaptor<Appointment> argumentCaptor = ArgumentCaptor.forClass(Appointment.class);
        appointmentService.createNewAppointment(workId, providerId, customerId, startOfNewAppointment);

        // 检查点：验证该测试用例的预期结果。
        verify(appointmentRepository, times(1)).save(argumentCaptor.capture());
    }

    @Test
    public void shouldNotBookAppointmentWhenAppointmentStartIsNotWithinProviderWorkingHours() {
        LocalDateTime startOfNewAppointment = LocalDateTime.of(2019, 1, 1, 5, 59);

        when(workService.isWorkForCustomer(workId, customerId)).thenReturn(true);
        when(workService.getWorkById(workId)).thenReturn(work);
        when(userService.getProviderById(providerId)).thenReturn(provider);

        // 检查点：验证该测试用例的预期结果。
        assertThrows(RuntimeException.class,
                () -> appointmentService.createNewAppointment(workId, providerId, customerId, startOfNewAppointment));
    }

    @Test
    public void shouldNotBookNewAppointmentWhenCollidingWithProviderAlreadyBookedAppointments() {
        LocalDateTime startOfNewAppointment = LocalDateTime.of(2019, 1, 1, 6, 0);

        Appointment existingAppointment = new Appointment();
        LocalDateTime startOfExistingAppointment = LocalDateTime.of(2019, 1, 1, 6, 0);
        LocalDateTime endOfExistingAppointment = LocalDateTime.of(2019, 1, 1, 7, 0);
        existingAppointment.setStart(startOfExistingAppointment);
        existingAppointment.setEnd(endOfExistingAppointment);
        List<Appointment> providerBookedAppointments = new ArrayList<>();
        providerBookedAppointments.add(existingAppointment);

        when(workService.isWorkForCustomer(workId, customerId)).thenReturn(true);
        when(appointmentRepository.findByProviderIdWithStartInPeroid(providerId, startOfNewAppointment.toLocalDate().atStartOfDay(), startOfNewAppointment.toLocalDate().atStartOfDay().plusDays(1))).thenReturn(providerBookedAppointments);
        when(workService.getWorkById(workId)).thenReturn(work);
        when(userService.getProviderById(providerId)).thenReturn(provider);

        // 检查点：验证该测试用例的预期结果。
        assertThrows(RuntimeException.class,
                () -> appointmentService.createNewAppointment(workId, providerId, customerId, startOfNewAppointment));
    }

    @Test
    public void shouldNotBookNewAppointmentWhenCollidingWithCustomerAlreadyBookedAppointments() {
        LocalDateTime startOfNewAppointment = LocalDateTime.of(2019, 1, 1, 6, 0);

        Appointment existingAppointment = new Appointment();
        LocalDateTime startOfExistingAppointment = LocalDateTime.of(2019, 1, 1, 6, 0);
        LocalDateTime endOfExistingAppointment = LocalDateTime.of(2019, 1, 1, 7, 0);
        existingAppointment.setStart(startOfExistingAppointment);
        existingAppointment.setEnd(endOfExistingAppointment);
        List<Appointment> customerBookedAppointments = new ArrayList<>();
        customerBookedAppointments.add(existingAppointment);

        when(workService.isWorkForCustomer(workId, customerId)).thenReturn(true);
        when(appointmentRepository.findByCustomerIdWithStartInPeroid(customerId, startOfNewAppointment.toLocalDate().atStartOfDay(), startOfNewAppointment.toLocalDate().atStartOfDay().plusDays(1))).thenReturn(customerBookedAppointments);
        when(workService.getWorkById(workId)).thenReturn(work);
        when(userService.getProviderById(providerId)).thenReturn(provider);

        // 检查点：验证该测试用例的预期结果。
        assertThrows(RuntimeException.class,
                () -> appointmentService.createNewAppointment(workId, providerId, customerId, startOfNewAppointment));
    }


    @Test
    public void shouldFindAppointmentById() {
        when(appointmentRepository.findById(1)).thenReturn(optionalAppointment);
        // 检查点：验证该测试用例的预期结果。
        assertEquals(optionalAppointment.get().getId(), appointmentService.getAppointmentByIdWithAuthorization(1).getId());
        verify(appointmentRepository, times(1)).findById(1);
    }

    @Test
    public void shouldFindAllAppointments() {
        PageRequest pageable = PageRequest.of(0, 10);
        Page<Appointment> appointmentPage = new PageImpl<>(appointments, pageable, appointments.size());
        when(appointmentRepository.findListPage(null, pageable)).thenReturn(appointmentPage);

        // 检查点：验证该测试用例的预期结果。
        assertEquals(appointmentPage, appointmentService.getAllAppointments(null, pageable));
        verify(appointmentRepository).findListPage(null, pageable);
    }

    @Test
    public void shouldDeleteAppointmentById() {
        appointmentService.deleteAppointmentById(1);
        // 检查点：验证该测试用例的预期结果。
        verify(appointmentRepository).deleteById(1);
    }

    @Test
    public void shouldThrowWhenAppointmentDoesNotExist() {
        when(appointmentRepository.findById(99)).thenReturn(Optional.empty());

        // 检查点：验证该测试用例的预期结果。
        assertThrows(AppointmentNotFoundException.class, () -> appointmentService.getAppointmentById(99));
    }

    @Test
    public void shouldDelegateCalendarAndListQueries() {
        PageRequest pageable = PageRequest.of(0, 10);
        Page<Appointment> appointmentPage = new PageImpl<>(appointments, pageable, appointments.size());
        LocalDateTime start = LocalDateTime.of(2031, 1, 1, 0, 0);
        LocalDateTime end = start.plusDays(7);
        LocalDate day = start.toLocalDate();
        when(appointmentRepository.findListPageByCustomerId(customerId, AppointmentStatus.SCHEDULED, pageable)).thenReturn(appointmentPage);
        when(appointmentRepository.findListPageByProviderId(providerId, null, pageable)).thenReturn(appointmentPage);
        when(appointmentRepository.findCalendarByCustomerId(customerId, start, end)).thenReturn(appointments);
        when(appointmentRepository.findCalendarByProviderId(providerId, start, end)).thenReturn(appointments);
        when(appointmentRepository.findCalendarEntries(start, end)).thenReturn(appointments);
        when(appointmentRepository.findByProviderIdWithStartInPeroid(providerId, day.atStartOfDay(), day.atStartOfDay().plusDays(1))).thenReturn(appointments);
        when(appointmentRepository.findByCustomerIdWithStartInPeroid(customerId, day.atStartOfDay(), day.atStartOfDay().plusDays(1))).thenReturn(appointments);
        when(appointmentRepository.findByCustomerIdCanceledAfterDate(org.mockito.ArgumentMatchers.eq(customerId), any(LocalDateTime.class))).thenReturn(appointments);
        when(appointmentRepository.findCanceledByUser(customerId)).thenReturn(appointments);
        when(appointmentRepository.findScheduledByUserId(customerId)).thenReturn(appointments);

        // 检查点：验证该测试用例的预期结果。
        assertEquals(appointmentPage, appointmentService.getAppointmentByCustomerId(customerId, AppointmentStatus.SCHEDULED, pageable));
        assertEquals(appointmentPage, appointmentService.getAppointmentByProviderId(providerId, null, pageable));
        assertEquals(appointments, appointmentService.getAppointmentCalendarByCustomerId(customerId, start, end));
        assertEquals(appointments, appointmentService.getAppointmentCalendarByProviderId(providerId, start, end));
        // 检查点：验证该测试用例的预期结果。
        assertEquals(appointments, appointmentService.getAppointmentCalendar(start, end));
        assertEquals(appointments, appointmentService.getAppointmentsByProviderAtDay(providerId, day));
        assertEquals(appointments, appointmentService.getAppointmentsByCustomerAtDay(customerId, day));
        assertEquals(appointments, appointmentService.getCanceledAppointmentsByCustomerIdForCurrentMonth(customerId));
        // 检查点：验证该测试用例的预期结果。
        assertEquals(1, appointmentService.getNumberOfCanceledAppointmentsForUser(customerId));
        assertEquals(1, appointmentService.getNumberOfScheduledAppointmentsForUser(customerId));
    }

    @Test
    public void shouldAddChatMessageForAppointmentParty() {
        ChatMessage chatMessage = new ChatMessage();
        when(appointmentRepository.findById(appointmentId)).thenReturn(optionalAppointment);
        when(userService.getUserById(customerId)).thenReturn(customer);

        appointmentService.addMessageToAppointmentChat(appointmentId, customerId, chatMessage);

        // 检查点：验证该测试用例的预期结果。
        Assertions.assertThat(chatMessage.getAuthor()).isSameAs(customer);
        Assertions.assertThat(chatMessage.getAppointment()).isSameAs(appointment);
        Assertions.assertThat(chatMessage.getCreatedAt()).isNotNull();
        verify(chatMessageRepository).save(chatMessage);
        // 检查点：验证该测试用例的预期结果。
        verify(notificationService).newChatMessageNotification(chatMessage, true);
    }

    @Test
    public void shouldRejectChatMessageFromNonAppointmentParty() {
        when(appointmentRepository.findById(appointmentId)).thenReturn(optionalAppointment);

        // 检查点：验证该测试用例的预期结果。
        assertThrows(AccessDeniedException.class,
                () -> appointmentService.addMessageToAppointmentChat(appointmentId, 77, new ChatMessage()));

        verify(chatMessageRepository, never()).save(any(ChatMessage.class));
    }

    @Test
    public void shouldUpdateStatusesForUserAndAllAppointments() {
        Appointment scheduled = appointmentWithStatus(AppointmentStatus.SCHEDULED, LocalDateTime.now().minusHours(2));
        Appointment finished = appointmentWithStatus(AppointmentStatus.FINISHED, LocalDateTime.now().minusDays(2));
        Appointment recentFinished = appointmentWithStatus(AppointmentStatus.SCHEDULED, LocalDateTime.now().minusHours(2));
        when(appointmentRepository.findScheduledByUserIdWithEndBeforeDate(any(LocalDateTime.class), org.mockito.ArgumentMatchers.eq(customerId)))
                .thenReturn(List.of(scheduled));
        when(appointmentRepository.findFinishedByUserIdWithEndBeforeDate(any(LocalDateTime.class), org.mockito.ArgumentMatchers.eq(customerId)))
                .thenReturn(List.of(finished));
        when(appointmentRepository.findScheduledWithEndBeforeDate(any(LocalDateTime.class))).thenReturn(List.of(recentFinished));
        when(appointmentRepository.findFinishedWithEndBeforeDate(any(LocalDateTime.class))).thenReturn(List.of(finished));

        appointmentService.updateUserAppointmentsStatuses(customerId);
        appointmentService.updateAllAppointmentsStatuses();

        // 检查点：验证该测试用例的预期结果。
        Assertions.assertThat(scheduled.getStatus()).isEqualTo(AppointmentStatus.FINISHED);
        Assertions.assertThat(finished.getStatus()).isEqualTo(AppointmentStatus.CONFIRMED);
        Assertions.assertThat(recentFinished.getStatus()).isEqualTo(AppointmentStatus.FINISHED);
        verify(appointmentRepository, times(4)).save(any(Appointment.class));
        // 检查点：验证该测试用例的预期结果。
        verify(notificationService).newAppointmentFinishedNotification(recentFinished, true);
    }

    @Test
    public void shouldResetAppointmentsWithExpiredExchangeRequests() {
        Appointment exchangeRequested = appointmentWithStatus(AppointmentStatus.EXCHANGE_REQUESTED, LocalDateTime.now().plusHours(12));
        when(appointmentRepository.findExchangeRequestedWithStartBefore(any(LocalDateTime.class))).thenReturn(List.of(exchangeRequested));

        appointmentService.updateAppointmentsStatusesWithExpiredExchangeRequest();

        // 检查点：验证该测试用例的预期结果。
        Assertions.assertThat(exchangeRequested.getStatus()).isEqualTo(AppointmentStatus.SCHEDULED);
        verify(appointmentRepository).save(exchangeRequested);
    }

    @Test
    public void shouldCancelAppointmentByCustomerOrProviderAndNotifyOtherParty() {
        when(appointmentRepository.findById(appointmentId)).thenReturn(optionalAppointment);
        when(userService.getUserById(customerId)).thenReturn(customer);

        appointmentService.cancelUserAppointmentById(appointmentId, customerId);

        // 检查点：验证该测试用例的预期结果。
        Assertions.assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.CANCELED);
        Assertions.assertThat(appointment.getCanceler()).isSameAs(customer);
        Assertions.assertThat(appointment.getCanceledAt()).isNotNull();
        verify(notificationService).newAppointmentCanceledByCustomerNotification(appointment, true);
    }

    @Test
    public void shouldRejectCancellationByNonAppointmentParty() {
        when(appointmentRepository.findById(appointmentId)).thenReturn(optionalAppointment);

        // 检查点：验证该测试用例的预期结果。
        assertThrows(AccessDeniedException.class,
                () -> appointmentService.cancelUserAppointmentById(appointmentId, 77));

        verify(appointmentRepository, never()).save(any(Appointment.class));
    }

    @Test
    public void shouldEvaluateCancelNotAllowedReasons() {
        when(appointmentRepository.findById(appointmentId)).thenReturn(optionalAppointment);
        work.setEditable(true);

        // 检查点：验证该测试用例的预期结果。
        assertEquals(null, appointmentService.getCancelNotAllowedReason(providerId, appointmentId));
        assertEquals(null, appointmentService.getCancelNotAllowedReason(customerId, appointmentId));

        appointment.setStatus(AppointmentStatus.FINISHED);
        assertEquals("只有已预约状态的预约可以取消。", appointmentService.getCancelNotAllowedReason(providerId, appointmentId));
        // 检查点：验证该测试用例的预期结果。
        assertEquals("只有已预约状态的预约可以取消。", appointmentService.getCancelNotAllowedReason(customerId, appointmentId));

        appointment.setStatus(AppointmentStatus.SCHEDULED);
        appointment.setStart(LocalDateTime.now().plusHours(2));
        assertEquals("距离开始不足 24 小时的预约不能取消。", appointmentService.getCancelNotAllowedReason(customerId, appointmentId));

        appointment.setStart(LocalDateTime.now().plusDays(3));
        work.setEditable(false);
        // 检查点：验证该测试用例的预期结果。
        assertEquals("该类型预约只能由服务人员取消。", appointmentService.getCancelNotAllowedReason(customerId, appointmentId));

        work.setEditable(true);
        when(appointmentRepository.findByCustomerIdCanceledAfterDate(org.mockito.ArgumentMatchers.eq(customerId), any(LocalDateTime.class)))
                .thenReturn(List.of(new Appointment()));
        // 检查点：验证该测试用例的预期结果。
        assertEquals("本月取消次数已达上限，无法取消该预约。", appointmentService.getCancelNotAllowedReason(customerId, appointmentId));
        assertEquals("只有客户或服务人员可以取消预约", appointmentService.getCancelNotAllowedReason(77, appointmentId));
    }

    @Test
    public void shouldProcessAppointmentRejectionAndAcceptance() {
        appointment.setEnd(LocalDateTime.now().minusHours(1));
        appointment.setStatus(AppointmentStatus.FINISHED);
        when(appointmentRepository.findById(appointmentId)).thenReturn(optionalAppointment);

        // 检查点：验证该测试用例的预期结果。
        Assertions.assertThat(appointmentService.isCustomerAllowedToRejectAppointment(customerId, appointmentId)).isTrue();
        Assertions.assertThat(appointmentService.requestAppointmentRejection(appointmentId, customerId)).isTrue();
        Assertions.assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.REJECTION_REQUESTED);
        verify(notificationService).newAppointmentRejectionRequestedNotification(appointment, true);

        // 检查点：验证该测试用例的预期结果。
        Assertions.assertThat(appointmentService.isProviderAllowedToAcceptRejection(providerId, appointmentId)).isTrue();
        Assertions.assertThat(appointmentService.acceptRejection(appointmentId, providerId)).isTrue();
        Assertions.assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.REJECTED);
        verify(notificationService).newAppointmentRejectionAcceptedNotification(appointment, true);
    }

    @Test
    public void shouldReturnFalseForRejectedRejectionFlows() {
        appointment.setStatus(AppointmentStatus.SCHEDULED);
        when(appointmentRepository.findById(appointmentId)).thenReturn(optionalAppointment);

        // 检查点：验证该测试用例的预期结果。
        Assertions.assertThat(appointmentService.requestAppointmentRejection(appointmentId, customerId)).isFalse();
        Assertions.assertThat(appointmentService.acceptRejection(appointmentId, providerId)).isFalse();
    }

    @Test
    public void shouldProcessRejectionTokens() {
        when(jwtTokenService.validateToken("request")).thenReturn(true);
        when(jwtTokenService.getAppointmentIdFromToken("request")).thenReturn(appointmentId);
        when(jwtTokenService.getCustomerIdFromToken("request")).thenReturn(customerId);
        when(jwtTokenService.validateToken("accept")).thenReturn(true);
        when(jwtTokenService.getAppointmentIdFromToken("accept")).thenReturn(appointmentId);
        when(jwtTokenService.getProviderIdFromToken("accept")).thenReturn(providerId);
        when(jwtTokenService.validateToken("bad")).thenReturn(false);
        appointment.setEnd(LocalDateTime.now().minusHours(1));
        appointment.setStatus(AppointmentStatus.FINISHED);
        when(appointmentRepository.findById(appointmentId)).thenReturn(optionalAppointment);

        // 检查点：验证该测试用例的预期结果。
        Assertions.assertThat(appointmentService.requestAppointmentRejection("request")).isTrue();
        appointment.setStatus(AppointmentStatus.REJECTION_REQUESTED);
        Assertions.assertThat(appointmentService.acceptRejection("accept")).isTrue();
        Assertions.assertThat(appointmentService.requestAppointmentRejection("bad")).isFalse();
        // 检查点：验证该测试用例的预期结果。
        Assertions.assertThat(appointmentService.acceptRejection("bad")).isFalse();
    }

    private Appointment appointmentWithStatus(AppointmentStatus status, LocalDateTime end) {
        Appointment appointment = new Appointment();
        appointment.setId(appointmentId + 10);
        appointment.setCustomer(customer);
        appointment.setProvider(provider);
        appointment.setWork(work);
        appointment.setStart(end.minusHours(1));
        appointment.setEnd(end);
        appointment.setStatus(status);
        return appointment;
    }

}
