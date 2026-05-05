package com.example.slabiak.appointmentscheduler.security;

import com.example.slabiak.appointmentscheduler.service.AppointmentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomAuthenticationSuccessHandlerTest {

    @Mock
    private AppointmentService appointmentService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    private CustomAuthenticationSuccessHandler handler;

    @BeforeEach
    void setUp() {
        handler = new CustomAuthenticationSuccessHandler(appointmentService);
    }

    @Test
    void shouldUpdateAllAppointmentsWhenAdminLogsIn() throws Exception {
        when(request.getContextPath()).thenReturn("/app");

        handler.onAuthenticationSuccess(request, response, authentication(user(1, "ROLE_ADMIN")));

        verify(appointmentService).updateAllAppointmentsStatuses();
        verify(appointmentService, never()).updateUserAppointmentsStatuses(1);
        verify(response).sendRedirect("/app/");
    }

    @Test
    void shouldUpdateCurrentUsersAppointmentsWhenNonAdminLogsIn() throws Exception {
        when(request.getContextPath()).thenReturn("");

        handler.onAuthenticationSuccess(request, response, authentication(user(3, "ROLE_CUSTOMER")));

        verify(appointmentService).updateUserAppointmentsStatuses(3);
        verify(appointmentService, never()).updateAllAppointmentsStatuses();
        verify(response).sendRedirect("/");
    }

    private TestingAuthenticationToken authentication(CustomUserDetails user) {
        return new TestingAuthenticationToken(user, null);
    }

    private CustomUserDetails user(int id, String role) {
        return new CustomUserDetails(
                id,
                "First",
                "Last",
                "user" + id,
                "user" + id + "@example.com",
                "password",
                List.of(new SimpleGrantedAuthority(role))
        );
    }
}
