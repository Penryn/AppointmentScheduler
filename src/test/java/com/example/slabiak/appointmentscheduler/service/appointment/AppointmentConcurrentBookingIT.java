package com.example.slabiak.appointmentscheduler.service.appointment;

import com.example.slabiak.appointmentscheduler.dao.AppointmentRepository;
import com.example.slabiak.appointmentscheduler.entity.Appointment;
import com.example.slabiak.appointmentscheduler.security.CustomUserDetails;
import com.example.slabiak.appointmentscheduler.service.AppointmentService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("integration-test")
public class AppointmentConcurrentBookingIT {

    private static final int WORK_ID = 1;
    private static final int PROVIDER_ID = 1101;
    private static final int FIRST_CUSTOMER_ID = 1001;
    private static final int SECOND_CUSTOMER_ID = 1002;
    private static final LocalDateTime CONCURRENT_START = LocalDateTime.of(2034, 5, 4, 10, 0);

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private UserDetailsService userDetailsService;

    @BeforeEach
    @AfterEach
    public void cleanConcurrentSlot() {
        findAppointmentsAtConcurrentSlot().forEach(appointmentRepository::delete);
    }

    @Test
    public void shouldPersistOnlyOneAppointmentWhenTwoCustomersBookSameProviderSlotConcurrently() throws Exception {
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        try {
            Future<Boolean> firstBooking = executorService.submit(bookConcurrently(FIRST_CUSTOMER_ID, ready, start));
            Future<Boolean> secondBooking = executorService.submit(bookConcurrently(SECOND_CUSTOMER_ID, ready, start));

            assertThat(ready.await(5, TimeUnit.SECONDS)).as("both booking tasks are ready").isTrue();
            start.countDown();

            int successfulRequests = 0;
            successfulRequests += firstBooking.get(5, TimeUnit.SECONDS) ? 1 : 0;
            successfulRequests += secondBooking.get(5, TimeUnit.SECONDS) ? 1 : 0;

            assertThat(successfulRequests).isEqualTo(1);
            assertThat(findAppointmentsAtConcurrentSlot()).hasSize(1);
        } finally {
            executorService.shutdownNow();
        }
    }

    private Callable<Boolean> bookConcurrently(int customerId, CountDownLatch ready, CountDownLatch start) {
        return () -> {
            ready.countDown();
            assertThat(start.await(5, TimeUnit.SECONDS)).as("booking task was released").isTrue();
            try {
                CustomUserDetails admin = (CustomUserDetails) userDetailsService.loadUserByUsername("admin");
                SecurityContextHolder.getContext().setAuthentication(
                        new UsernamePasswordAuthenticationToken(admin, admin.getPassword(), admin.getAuthorities()));
                appointmentService.createNewAppointment(WORK_ID, PROVIDER_ID, customerId, CONCURRENT_START);
                return true;
            } catch (RuntimeException exception) {
                return false;
            } finally {
                SecurityContextHolder.clearContext();
            }
        };
    }

    private List<Appointment> findAppointmentsAtConcurrentSlot() {
        return appointmentRepository.findByProviderIdWithStartInPeroid(
                        PROVIDER_ID,
                        CONCURRENT_START.toLocalDate().atStartOfDay(),
                        CONCURRENT_START.toLocalDate().atStartOfDay().plusDays(1))
                .stream()
                .filter(appointment -> CONCURRENT_START.equals(appointment.getStart()))
                .toList();
    }
}
