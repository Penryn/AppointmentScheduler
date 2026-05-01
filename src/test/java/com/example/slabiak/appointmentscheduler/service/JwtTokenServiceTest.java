package com.example.slabiak.appointmentscheduler.service;

import com.example.slabiak.appointmentscheduler.entity.Appointment;
import com.example.slabiak.appointmentscheduler.entity.user.customer.Customer;
import com.example.slabiak.appointmentscheduler.entity.user.provider.Provider;
import com.example.slabiak.appointmentscheduler.service.impl.JwtTokenServiceImpl;
import org.junit.Before;
import org.junit.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

public class JwtTokenServiceTest {

    private JwtTokenServiceImpl jwtTokenService;

    @Before
    public void setUp() {
        jwtTokenService = new JwtTokenServiceImpl("QXBwb2ludG1lbnRTY2hlZHVsZXItQm9vdDMtSmFrYXJ0YS1IUzUxMi1TaWduaW5nLUtleS0yMDI2LTA0LTIzLURvLU5vdC1Vc2UtSW4tUHJvZHVjdGlvbg==");
    }

    @Test
    public void shouldGenerateAppointmentRejectionTokenWithAppointmentAndCustomerClaims() {
        Appointment appointment = appointment(LocalDateTime.now().plusDays(2));

        String token = jwtTokenService.generateAppointmentRejectionToken(appointment);

        assertThat(jwtTokenService.validateToken(token)).isTrue();
        assertThat(jwtTokenService.getAppointmentIdFromToken(token)).isEqualTo(10);
        assertThat(jwtTokenService.getCustomerIdFromToken(token)).isEqualTo(3);
    }

    @Test
    public void shouldGenerateAcceptRejectionTokenWithAppointmentAndProviderClaims() {
        Appointment appointment = appointment(LocalDateTime.now().plusDays(2));

        String token = jwtTokenService.generateAcceptRejectionToken(appointment);

        assertThat(jwtTokenService.validateToken(token)).isTrue();
        assertThat(jwtTokenService.getAppointmentIdFromToken(token)).isEqualTo(10);
        assertThat(jwtTokenService.getProviderIdFromToken(token)).isEqualTo(2);
    }

    @Test
    public void shouldRejectExpiredAppointmentRejectionToken() {
        Appointment appointment = appointment(LocalDateTime.now().minusDays(2));

        String token = jwtTokenService.generateAppointmentRejectionToken(appointment);

        assertThat(jwtTokenService.validateToken(token)).isFalse();
    }

    @Test
    public void shouldRejectMalformedToken() {
        assertThat(jwtTokenService.validateToken("not-a-jwt-token")).isFalse();
    }

    private Appointment appointment(LocalDateTime end) {
        Customer customer = new Customer();
        customer.setId(3);
        Provider provider = new Provider();
        provider.setId(2);
        Appointment appointment = new Appointment();
        appointment.setId(10);
        appointment.setCustomer(customer);
        appointment.setProvider(provider);
        appointment.setStart(end.minusHours(1));
        appointment.setEnd(end);
        return appointment;
    }
}
