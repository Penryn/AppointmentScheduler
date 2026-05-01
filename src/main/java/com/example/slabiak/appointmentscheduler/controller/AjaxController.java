package com.example.slabiak.appointmentscheduler.controller;

import com.example.slabiak.appointmentscheduler.entity.Appointment;
import com.example.slabiak.appointmentscheduler.model.AppointmentRegisterForm;
import com.example.slabiak.appointmentscheduler.security.CustomUserDetails;
import com.example.slabiak.appointmentscheduler.service.AppointmentService;
import com.example.slabiak.appointmentscheduler.service.NotificationService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.stream.Collectors;

@RequestMapping("/api")
@RestController
public class AjaxController {

    private final AppointmentService appointmentService;
    private final NotificationService notificationService;

    public AjaxController(AppointmentService appointmentService, NotificationService notificationService) {
        this.appointmentService = appointmentService;
        this.notificationService = notificationService;
    }


    @GetMapping("/user/{userId}/appointments")
    public List<Appointment> getAppointmentsForUser(@PathVariable("userId") int userId,
                                                    @AuthenticationPrincipal CustomUserDetails currentUser,
                                                    @RequestParam(value = "start", required = false) String start,
                                                    @RequestParam(value = "end", required = false) String end) {
        if (start == null || end == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Calendar requests require start and end parameters");
        }

        LocalDateTime windowStart = parseDateTime(start);
        LocalDateTime windowEnd = parseDateTime(end);
        if (currentUser.hasRole("ROLE_CUSTOMER")) {
            return appointmentService.getAppointmentCalendarByCustomerId(userId, windowStart, windowEnd);
        } else if (currentUser.hasRole("ROLE_PROVIDER")) {
            return appointmentService.getAppointmentCalendarByProviderId(userId, windowStart, windowEnd);
        } else if (currentUser.hasRole("ROLE_ADMIN")) {
            return appointmentService.getAppointmentCalendar(windowStart, windowEnd);
        }
        return List.of();
    }

    @GetMapping("/availableHours/{providerId}/{workId}/{date}")
    public List<AppointmentRegisterForm> getAvailableHours(@PathVariable("providerId") int providerId, @PathVariable("workId") int workId, @PathVariable("date") String date, @AuthenticationPrincipal CustomUserDetails currentUser) {
        if (!currentUser.hasRole("ROLE_CUSTOMER")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only customers can query appointment availability");
        }
        LocalDate localDate = LocalDate.parse(date);
        return appointmentService.getAvailableHours(providerId, currentUser.getId(), workId, localDate)
                .stream()
                .map(timePeriod -> new AppointmentRegisterForm(workId, providerId, timePeriod.getStart().atDate(localDate), timePeriod.getEnd().atDate(localDate)))
                .collect(Collectors.toList());
    }

    @GetMapping("/user/notifications")
    public long getUnreadNotificationsCount(@AuthenticationPrincipal CustomUserDetails currentUser) {
        return notificationService.countUnreadNotifications(currentUser.getId());
    }

    private LocalDateTime parseDateTime(String value) {
        try {
            return OffsetDateTime.parse(value).toLocalDateTime();
        } catch (DateTimeParseException ignored) {
            try {
                return LocalDateTime.parse(value);
            } catch (DateTimeParseException exception) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid calendar date-time parameter", exception);
            }
        }
    }

}
