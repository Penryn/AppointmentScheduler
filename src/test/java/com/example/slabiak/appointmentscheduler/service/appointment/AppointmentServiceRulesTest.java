// 测试说明：验证预约服务的取消、拒绝和状态流转业务规则。
package com.example.slabiak.appointmentscheduler.service.appointment;

import com.example.slabiak.appointmentscheduler.dao.AppointmentRepository;
import com.example.slabiak.appointmentscheduler.dao.ChatMessageRepository;
import com.example.slabiak.appointmentscheduler.entity.Appointment;
import com.example.slabiak.appointmentscheduler.entity.Work;
import com.example.slabiak.appointmentscheduler.entity.WorkingPlan;
import com.example.slabiak.appointmentscheduler.entity.user.customer.Customer;
import com.example.slabiak.appointmentscheduler.entity.user.provider.Provider;
import com.example.slabiak.appointmentscheduler.service.NotificationService;
import com.example.slabiak.appointmentscheduler.service.UserService;
import com.example.slabiak.appointmentscheduler.service.WorkService;
import com.example.slabiak.appointmentscheduler.service.impl.AppointmentServiceImpl;
import com.example.slabiak.appointmentscheduler.service.impl.JwtTokenServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceRulesTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private WorkService workService;

    @Mock
    private UserService userService;

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private JwtTokenServiceImpl jwtTokenService;

    @InjectMocks
    private AppointmentServiceImpl appointmentService;

    private final int customerId = 1;
    private final int providerId = 2;
    private final int workId = 3;

    private Work work;
    private Provider provider;
    private Customer customer;
    private Appointment appointment;

    @BeforeEach
    void setUp() {
        work = new Work();
        work.setId(workId);
        work.setDuration(60);

        provider = new Provider();
        provider.setId(providerId);
        provider.setWorkingPlan(WorkingPlan.generateDefaultWorkingPlan());

        customer = new Customer();
        customer.setId(customerId);

        appointment = new Appointment();
        appointment.setId(1);
    }

    @Test
    void R1_shouldSaveAppointmentSuccessfully() {
        LocalDateTime time = LocalDateTime.of(2019, 1, 1, 6, 0);
        mockAvailableAppointmentInputs(time);
        when(userService.getCustomerById(customerId)).thenReturn(customer);

        appointmentService.createNewAppointment(workId, providerId, customerId, time);

        // 检查点：验证该测试用例的预期结果。
        verify(appointmentRepository, times(1)).save(any(Appointment.class));
        verify(notificationService, times(1)).newNewAppointmentScheduledNotification(any(Appointment.class), eq(true));
    }

    @Test
    void R2_shouldThrowWhenOutOfWorkingHours() {
        LocalDateTime time = LocalDateTime.of(2019, 1, 1, 5, 59);
        mockAvailableAppointmentInputs(time);

        // 检查点：验证该测试用例的预期结果。
        assertThrows(RuntimeException.class,
                () -> appointmentService.createNewAppointment(workId, providerId, customerId, time));
    }

    @Test
    void R3_shouldThrowWhenProviderHasConflict() {
        LocalDateTime time = LocalDateTime.of(2019, 1, 1, 6, 0);
        Appointment existing = appointmentAt(time, time.plusHours(1));

        when(workService.isWorkForCustomer(workId, customerId)).thenReturn(true);
        when(workService.getWorkById(workId)).thenReturn(work);
        when(userService.getProviderById(providerId)).thenReturn(provider);
        when(appointmentRepository.findByProviderIdWithStartInPeroid(
                eq(providerId), any(LocalDateTime.class), any(LocalDateTime.class)
        )).thenReturn(appointments(existing));
        when(appointmentRepository.findByCustomerIdWithStartInPeroid(
                eq(customerId), any(LocalDateTime.class), any(LocalDateTime.class)
        )).thenReturn(emptyAppointments());

        // 检查点：验证该测试用例的预期结果。
        assertThrows(RuntimeException.class,
                () -> appointmentService.createNewAppointment(workId, providerId, customerId, time));
    }

    @Test
    void R4_shouldThrowWhenCustomerHasConflict() {
        LocalDateTime time = LocalDateTime.of(2019, 1, 1, 6, 0);
        Appointment existing = appointmentAt(time, time.plusHours(1));

        when(workService.isWorkForCustomer(workId, customerId)).thenReturn(true);
        when(workService.getWorkById(workId)).thenReturn(work);
        when(userService.getProviderById(providerId)).thenReturn(provider);
        when(appointmentRepository.findByProviderIdWithStartInPeroid(
                eq(providerId), any(LocalDateTime.class), any(LocalDateTime.class)
        )).thenReturn(emptyAppointments());
        when(appointmentRepository.findByCustomerIdWithStartInPeroid(
                eq(customerId), any(LocalDateTime.class), any(LocalDateTime.class)
        )).thenReturn(appointments(existing));

        // 检查点：验证该测试用例的预期结果。
        assertThrows(RuntimeException.class,
                () -> appointmentService.createNewAppointment(workId, providerId, customerId, time));
    }

    @Test
    void R5_shouldReturnAppointmentById() {
        when(appointmentRepository.findById(1)).thenReturn(Optional.of(appointment));

        Appointment result = appointmentService.getAppointmentByIdWithAuthorization(1);

        // 检查点：验证该测试用例的预期结果。
        assertEquals(appointment.getId(), result.getId());
        verify(appointmentRepository, times(1)).findById(1);
    }

    @Test
    void R6_shouldReturnPagedAppointments() {
        PageRequest pageable = PageRequest.of(0, 10);
        Page<Appointment> page = new PageImpl<>(List.of(appointment));
        when(appointmentRepository.findListPage(null, pageable)).thenReturn(page);

        Page<Appointment> result = appointmentService.getAllAppointments(null, pageable);

        // 检查点：验证该测试用例的预期结果。
        assertEquals(page, result);
        verify(appointmentRepository).findListPage(null, pageable);
    }

    @Test
    void R7_shouldDeleteAppointmentById() {
        appointmentService.deleteAppointmentById(1);

        // 检查点：验证该测试用例的预期结果。
        verify(appointmentRepository, times(1)).deleteById(1);
    }

    private void mockAvailableAppointmentInputs(LocalDateTime time) {
        when(workService.isWorkForCustomer(workId, customerId)).thenReturn(true);
        when(workService.getWorkById(workId)).thenReturn(work);
        when(userService.getProviderById(providerId)).thenReturn(provider);
        when(appointmentRepository.findByProviderIdWithStartInPeroid(
                eq(providerId), eq(time.toLocalDate().atStartOfDay()), eq(time.toLocalDate().atStartOfDay().plusDays(1))
        )).thenReturn(emptyAppointments());
        when(appointmentRepository.findByCustomerIdWithStartInPeroid(
                eq(customerId), eq(time.toLocalDate().atStartOfDay()), eq(time.toLocalDate().atStartOfDay().plusDays(1))
        )).thenReturn(emptyAppointments());
    }

    private Appointment appointmentAt(LocalDateTime start, LocalDateTime end) {
        Appointment appointment = new Appointment();
        appointment.setStart(start);
        appointment.setEnd(end);
        return appointment;
    }

    private List<Appointment> appointments(Appointment appointment) {
        List<Appointment> appointments = new ArrayList<>();
        appointments.add(appointment);
        return appointments;
    }

    private List<Appointment> emptyAppointments() {
        return new ArrayList<>();
    }
}
