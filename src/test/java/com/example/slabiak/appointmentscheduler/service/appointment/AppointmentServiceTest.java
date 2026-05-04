package com.example.slabiak.appointmentscheduler.service.appointment;

import com.example.slabiak.appointmentscheduler.dao.AppointmentRepository;
import com.example.slabiak.appointmentscheduler.entity.Appointment;
import com.example.slabiak.appointmentscheduler.entity.Work;
import com.example.slabiak.appointmentscheduler.entity.WorkingPlan;
import com.example.slabiak.appointmentscheduler.entity.user.customer.Customer;
import com.example.slabiak.appointmentscheduler.entity.user.provider.Provider;
import com.example.slabiak.appointmentscheduler.service.EmailService;
import com.example.slabiak.appointmentscheduler.service.NotificationService;
import com.example.slabiak.appointmentscheduler.service.UserService;
import com.example.slabiak.appointmentscheduler.service.WorkService;
import com.example.slabiak.appointmentscheduler.service.impl.AppointmentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

@org.junit.jupiter.api.extension.ExtendWith(MockitoExtension.class)
public class AppointmentServiceTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private WorkService workService;

    @Mock
    private EmailService emailService;

    @Mock
    private UserService userService;

    @Mock
    private NotificationService notificationService;


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
        optionalAppointment = Optional.of(appointment);
        appointments = new ArrayList<>();
        appointments.add(appointment);

    }

    @Test
    public void shouldBookAppointmentWhenAllConditionsMet() {
        LocalDateTime startOfNewAppointment = LocalDateTime.of(2019, 01, 01, 6, 0);

        when(workService.isWorkForCustomer(workId, customerId)).thenReturn(true);
        when(workService.getWorkById(workId)).thenReturn(work);
        when(userService.getProviderById(providerId)).thenReturn(provider);
        when(userService.getCustomerById(customerId)).thenReturn(customer);

        ArgumentCaptor<Appointment> argumentCaptor = ArgumentCaptor.forClass(Appointment.class);
        appointmentService.createNewAppointment(workId, providerId, customerId, startOfNewAppointment);

        verify(appointmentRepository, times(1)).save(argumentCaptor.capture());
    }

    @Test
    public void shouldNotBookAppointmentWhenAppointmentStartIsNotWithinProviderWorkingHours() {
        LocalDateTime startOfNewAppointment = LocalDateTime.of(2019, 01, 01, 5, 59);

        when(workService.isWorkForCustomer(workId, customerId)).thenReturn(true);
        when(workService.getWorkById(workId)).thenReturn(work);
        when(userService.getProviderById(providerId)).thenReturn(provider);

        assertThrows(RuntimeException.class,
                () -> appointmentService.createNewAppointment(workId, providerId, customerId, startOfNewAppointment));
    }

    @Test
    public void shouldNotBookNewAppointmentWhenCollidingWithProviderAlreadyBookedAppointments() {
        LocalDateTime startOfNewAppointment = LocalDateTime.of(2019, 01, 01, 6, 0);

        Appointment existingAppointment = new Appointment();
        LocalDateTime startOfExistingAppointment = LocalDateTime.of(2019, 01, 01, 6, 0);
        LocalDateTime endOfExistingAppointment = LocalDateTime.of(2019, 01, 01, 7, 0);
        existingAppointment.setStart(startOfExistingAppointment);
        existingAppointment.setEnd(endOfExistingAppointment);
        List<Appointment> providerBookedAppointments = new ArrayList<>();
        providerBookedAppointments.add(existingAppointment);

        when(workService.isWorkForCustomer(workId, customerId)).thenReturn(true);
        when(appointmentRepository.findByProviderIdWithStartInPeroid(providerId, startOfNewAppointment.toLocalDate().atStartOfDay(), startOfNewAppointment.toLocalDate().atStartOfDay().plusDays(1))).thenReturn(providerBookedAppointments);
        when(workService.getWorkById(workId)).thenReturn(work);
        when(userService.getProviderById(providerId)).thenReturn(provider);

        assertThrows(RuntimeException.class,
                () -> appointmentService.createNewAppointment(workId, providerId, customerId, startOfNewAppointment));
    }

    @Test
    public void shouldNotBookNewAppointmentWhenCollidingWithCustomerAlreadyBookedAppointments() {
        LocalDateTime startOfNewAppointment = LocalDateTime.of(2019, 01, 01, 6, 0);

        Appointment existingAppointment = new Appointment();
        LocalDateTime startOfExistingAppointment = LocalDateTime.of(2019, 01, 01, 6, 0);
        LocalDateTime endOfExistingAppointment = LocalDateTime.of(2019, 01, 01, 7, 0);
        existingAppointment.setStart(startOfExistingAppointment);
        existingAppointment.setEnd(endOfExistingAppointment);
        List<Appointment> customerBookedAppointments = new ArrayList<>();
        customerBookedAppointments.add(existingAppointment);

        when(workService.isWorkForCustomer(workId, customerId)).thenReturn(true);
        when(appointmentRepository.findByCustomerIdWithStartInPeroid(customerId, startOfNewAppointment.toLocalDate().atStartOfDay(), startOfNewAppointment.toLocalDate().atStartOfDay().plusDays(1))).thenReturn(customerBookedAppointments);
        when(workService.getWorkById(workId)).thenReturn(work);
        when(userService.getProviderById(providerId)).thenReturn(provider);

        assertThrows(RuntimeException.class,
                () -> appointmentService.createNewAppointment(workId, providerId, customerId, startOfNewAppointment));
    }


    @Test
    public void shouldFindAppointmentById() {
        when(appointmentRepository.findById(1)).thenReturn(optionalAppointment);
        assertEquals(optionalAppointment.get().getId(), appointmentService.getAppointmentByIdWithAuthorization(1).getId());
        verify(appointmentRepository, times(1)).findById(1);
    }

    @Test
    public void shouldFindAllAppointments() {
        PageRequest pageable = PageRequest.of(0, 10);
        Page<Appointment> appointmentPage = new PageImpl<>(appointments, pageable, appointments.size());
        when(appointmentRepository.findListPage(null, pageable)).thenReturn(appointmentPage);

        assertEquals(appointmentPage, appointmentService.getAllAppointments(null, pageable));
        verify(appointmentRepository).findListPage(null, pageable);
    }

    @Test
    public void shouldDeleteAppointmentById() {
        appointmentService.deleteAppointmentById(1);
        verify(appointmentRepository).deleteById(1);
    }


}
