package com.example.slabiak.appointmentscheduler.controller;

import com.example.slabiak.appointmentscheduler.dao.AppointmentRepository;
import com.example.slabiak.appointmentscheduler.entity.Appointment;
import com.example.slabiak.appointmentscheduler.entity.AppointmentStatus;
import com.example.slabiak.appointmentscheduler.service.NotificationService;
import com.example.slabiak.appointmentscheduler.service.UserService;
import com.example.slabiak.appointmentscheduler.service.WorkService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.Persistence;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("integration-test")
public class AjaxControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private WorkService workService;

    @Autowired
    private NotificationService notificationService;

    @Test
    @Transactional
    @WithUserDetails("customer_r")
    public void shouldReturnOnlyAppointmentsInsideRequestedCalendarWindow() throws Exception {
        Appointment inWindow = new Appointment(
                LocalDateTime.of(2030, 1, 10, 10, 0),
                LocalDateTime.of(2030, 1, 10, 11, 0),
                userService.getCustomerById(3),
                userService.getProviderById(2),
                workService.getWorkById(1)
        );
        inWindow.setStatus(AppointmentStatus.SCHEDULED);
        appointmentRepository.save(inWindow);

        Appointment outOfWindow = new Appointment(
                LocalDateTime.of(2030, 2, 10, 10, 0),
                LocalDateTime.of(2030, 2, 10, 11, 0),
                userService.getCustomerById(3),
                userService.getProviderById(2),
                workService.getWorkById(1)
        );
        outOfWindow.setStatus(AppointmentStatus.SCHEDULED);
        appointmentRepository.save(outOfWindow);

        assertThat(Persistence.getPersistenceUtil().isLoaded(
                appointmentRepository.findCalendarByCustomerId(
                        3,
                        LocalDateTime.of(2030, 1, 1, 0, 0),
                        LocalDateTime.of(2030, 1, 31, 23, 59))
                        .get(0)
                        .getWork()))
                .isTrue();

        mockMvc.perform(get("/api/user/3/appointments")
                        .param("start", "2030-01-01T00:00:00Z")
                        .param("end", "2030-01-31T23:59:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("English lesson"));
    }

    @Test
    @WithUserDetails("customer_r")
    public void shouldRejectAppointmentCalendarRequestsWithoutWindowParameters() throws Exception {
        mockMvc.perform(get("/api/user/3/appointments"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithUserDetails("customer_r")
    public void shouldRejectAppointmentCalendarRequestsWithInvalidWindowParameters() throws Exception {
        mockMvc.perform(get("/api/user/3/appointments")
                        .param("start", "not-a-date")
                        .param("end", "2030-01-31T23:59:00Z"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithUserDetails("customer_r")
    public void shouldRejectCustomerCalendarAccessForAnotherCustomer() throws Exception {
        mockMvc.perform(get("/api/user/1001/appointments")
                        .param("start", "2030-01-01T00:00:00Z")
                        .param("end", "2030-01-31T23:59:00Z"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails("provider")
    public void shouldRejectProviderCalendarAccessForAnotherProvider() throws Exception {
        mockMvc.perform(get("/api/user/1101/appointments")
                        .param("start", "2030-01-01T00:00:00Z")
                        .param("end", "2030-01-31T23:59:00Z"))
                .andExpect(status().isForbidden());
    }

    @Test
    @Transactional
    @WithUserDetails("customer_r")
    public void shouldReturnUnreadNotificationCountWithoutLoadingNotificationList() throws Exception {
        notificationService.newNotification("n1", "m1", "/appointments/1", userService.getUserById(3));
        notificationService.newNotification("n2", "m2", "/appointments/1", userService.getUserById(3));

        mockMvc.perform(get("/api/user/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(2));
    }

    @Test
    @WithUserDetails("customer_r")
    public void shouldKeepAvailableHoursApiContractStable() throws Exception {
        mockMvc.perform(get("/api/availableHours/2/1/2032-01-20"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].workId").value(1))
                .andExpect(jsonPath("$[0].providerId").value(2))
                .andExpect(jsonPath("$[0].start").exists())
                .andExpect(jsonPath("$[0].end").exists());
    }

    @Test
    @WithUserDetails("customer_r")
    public void shouldKeepCalendarApiContractStable() throws Exception {
        mockMvc.perform(get("/api/user/3/appointments")
                        .param("start", "2030-01-01T00:00:00Z")
                        .param("end", "2030-01-31T23:59:00Z"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    public void shouldRequireAuthenticationForApiEndpoints() throws Exception {
        mockMvc.perform(get("/api/user/notifications"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithUserDetails("provider")
    public void shouldRejectProviderAccessToCustomerAvailableHoursContract() throws Exception {
        mockMvc.perform(get("/api/availableHours/2/1/2032-01-20"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails("customer_r")
    public void shouldRejectAvailableHoursRequestsWithInvalidDate() throws Exception {
        mockMvc.perform(get("/api/availableHours/2/1/not-a-date"))
                .andExpect(status().isBadRequest());
    }
}
