package com.example.slabiak.appointmentscheduler.service.appointment;

import com.example.slabiak.appointmentscheduler.entity.Appointment;
import com.example.slabiak.appointmentscheduler.entity.AppointmentStatus;
import com.example.slabiak.appointmentscheduler.entity.Work;
import com.example.slabiak.appointmentscheduler.entity.user.customer.Customer;
import com.example.slabiak.appointmentscheduler.entity.user.provider.Provider;
import com.example.slabiak.appointmentscheduler.service.UserService;
import com.example.slabiak.appointmentscheduler.service.WorkService;
import com.example.slabiak.appointmentscheduler.service.AppointmentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("integration-test")
public class AppointmentServiceIT {

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private com.example.slabiak.appointmentscheduler.dao.AppointmentRepository appointmentRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private WorkService workService;

    @Test
    @Transactional
    @WithUserDetails("admin")
    public void shouldSaveNewRetailCustomer() {
        appointmentService.createNewAppointment(1, 2, 3, LocalDateTime.of(2020, 2, 9, 12, 0, 0));

        Page<Appointment> appointments = appointmentService.getAllAppointments(null, PageRequest.of(0, 10));
        assertThat(appointments).hasSize(1);
        assertEquals(AppointmentStatus.SCHEDULED, appointments.getContent().get(0).getStatus());

    }

    @Test
    @Transactional
    @WithUserDetails("admin")
    public void shouldRejectDuplicateProviderStartAtDatabaseLevel() {
        LocalDateTime start = LocalDateTime.of(2031, 1, 10, 10, 0);
        Appointment first = appointment(3, 2, start);
        Appointment duplicate = appointment(1001, 2, start);

        appointmentRepository.saveAndFlush(first);

        assertThatThrownBy(() -> appointmentRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @WithUserDetails("admin")
    public void shouldAllowOnlyOneConcurrentInsertForTheSameProviderAndStart() throws Exception {
        LocalDateTime start = LocalDateTime.of(2031, 1, 11, 10, 0);
        Customer customer = userService.getCustomerById(3);
        Provider provider = userService.getProviderById(2);
        Work work = workService.getWorkById(1);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch startTogether = new CountDownLatch(1);
        AtomicInteger successfulInserts = new AtomicInteger();

        Runnable insertAppointment = () -> {
            ready.countDown();
            try {
                startTogether.await(5, TimeUnit.SECONDS);
                Appointment appointment = new Appointment(start, start.plusHours(1), customer, provider, work);
                appointment.setStatus(AppointmentStatus.SCHEDULED);
                appointmentRepository.saveAndFlush(appointment);
                successfulInserts.incrementAndGet();
            } catch (DataIntegrityViolationException expected) {
                // The database uniqueness guard should reject the racing insert.
            } catch (Exception exception) {
                throw new IllegalStateException(exception);
            }
        };

        Thread firstThread = new Thread(insertAppointment);
        Thread secondThread = new Thread(insertAppointment);
        firstThread.start();
        secondThread.start();

        assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
        startTogether.countDown();
        firstThread.join(10_000);
        secondThread.join(10_000);

        assertThat(successfulInserts.get()).isEqualTo(1);
        assertThat(appointmentRepository.findByProviderIdWithStartInPeroid(2, start.toLocalDate().atStartOfDay(), start.toLocalDate().atStartOfDay().plusDays(1)))
                .filteredOn(appointment -> appointment.getStart().equals(start))
                .hasSize(1);
    }

    @Test
    @WithUserDetails("admin")
    public void shouldAllowOnlyOneConcurrentInsertForTheSameCustomerAndStart() throws Exception {
        LocalDateTime start = LocalDateTime.of(2031, 1, 12, 10, 0);
        Customer customer = userService.getCustomerById(3);
        Work work = workService.getWorkById(1);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch startTogether = new CountDownLatch(1);
        AtomicInteger successfulInserts = new AtomicInteger();

        Runnable insertAppointment = () -> {
            ready.countDown();
            try {
                startTogether.await(5, TimeUnit.SECONDS);
                Appointment appointment = new Appointment(start, start.plusHours(1), customer, userService.getProviderById(2), work);
                appointment.setStatus(AppointmentStatus.SCHEDULED);
                appointmentRepository.saveAndFlush(appointment);
                successfulInserts.incrementAndGet();
            } catch (DataIntegrityViolationException expected) {
                // The customer/start uniqueness guard should reject the racing insert.
            } catch (Exception exception) {
                throw new IllegalStateException(exception);
            }
        };

        Thread firstThread = new Thread(insertAppointment);
        Thread secondThread = new Thread(insertAppointment);
        firstThread.start();
        secondThread.start();

        assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
        startTogether.countDown();
        firstThread.join(10_000);
        secondThread.join(10_000);

        assertThat(successfulInserts.get()).isEqualTo(1);
        assertThat(appointmentRepository.findByCustomerIdWithStartInPeroid(3, start.toLocalDate().atStartOfDay(), start.toLocalDate().atStartOfDay().plusDays(1)))
                .filteredOn(appointment -> appointment.getStart().equals(start))
                .hasSize(1);
    }

    private Appointment appointment(int customerId, int providerId, LocalDateTime start) {
        Appointment appointment = new Appointment(
                start,
                start.plusHours(1),
                userService.getCustomerById(customerId),
                userService.getProviderById(providerId),
                workService.getWorkById(1)
        );
        appointment.setStatus(AppointmentStatus.SCHEDULED);
        return appointment;
    }
}
