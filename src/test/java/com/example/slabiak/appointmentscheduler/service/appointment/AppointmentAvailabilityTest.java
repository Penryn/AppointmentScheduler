package com.example.slabiak.appointmentscheduler.service.appointment;

import com.example.slabiak.appointmentscheduler.dao.AppointmentRepository;
import com.example.slabiak.appointmentscheduler.entity.Appointment;
import com.example.slabiak.appointmentscheduler.entity.Work;
import com.example.slabiak.appointmentscheduler.model.TimePeroid;
import com.example.slabiak.appointmentscheduler.service.NotificationService;
import com.example.slabiak.appointmentscheduler.service.UserService;
import com.example.slabiak.appointmentscheduler.service.WorkService;
import com.example.slabiak.appointmentscheduler.service.impl.AppointmentServiceImpl;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(MockitoJUnitRunner.class)
public class AppointmentAvailabilityTest {

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

    private Work oneHourWork;

    @Before
    public void setUp() {
        oneHourWork = new Work();
        oneHourWork.setDuration(60);
    }

    @Test
    public void shouldSplitWorkingPeriodIntoSlotsMatchingWorkDuration() {
        List<TimePeroid> slots = appointmentService.calculateAvailableHours(
                List.of(new TimePeroid(LocalTime.of(6, 0), LocalTime.of(9, 0))),
                oneHourWork);

        assertThat(slots).containsExactly(
                new TimePeroid(LocalTime.of(6, 0), LocalTime.of(7, 0)),
                new TimePeroid(LocalTime.of(7, 0), LocalTime.of(8, 0)),
                new TimePeroid(LocalTime.of(8, 0), LocalTime.of(9, 0)));
    }

    @Test
    public void shouldNotCreateSlotThatEndsAfterAvailablePeriod() {
        Work ninetyMinuteWork = new Work();
        ninetyMinuteWork.setDuration(90);

        List<TimePeroid> slots = appointmentService.calculateAvailableHours(
                List.of(new TimePeroid(LocalTime.of(6, 0), LocalTime.of(8, 0))),
                ninetyMinuteWork);

        assertThat(slots).containsExactly(
                new TimePeroid(LocalTime.of(6, 0), LocalTime.of(7, 30)));
    }

    @Test
    public void shouldExcludeAppointmentAtBeginningOfAvailablePeriod() {
        List<TimePeroid> periods = new ArrayList<>();
        periods.add(new TimePeroid(LocalTime.of(6, 0), LocalTime.of(10, 0)));

        List<TimePeroid> result = appointmentService.excludeAppointmentsFromTimePeroids(periods, appointments(
                appointmentAt(LocalTime.of(6, 0), LocalTime.of(7, 0))));

        assertThat(result).containsExactly(
                new TimePeroid(LocalTime.of(7, 0), LocalTime.of(10, 0)));
    }

    @Test
    public void shouldExcludeAppointmentInMiddleOfAvailablePeriod() {
        List<TimePeroid> periods = new ArrayList<>();
        periods.add(new TimePeroid(LocalTime.of(6, 0), LocalTime.of(10, 0)));

        List<TimePeroid> result = appointmentService.excludeAppointmentsFromTimePeroids(periods, appointments(
                appointmentAt(LocalTime.of(8, 0), LocalTime.of(9, 0))));

        assertThat(result).containsExactly(
                new TimePeroid(LocalTime.of(6, 0), LocalTime.of(8, 0)),
                new TimePeroid(LocalTime.of(9, 0), LocalTime.of(10, 0)));
    }

    @Test
    public void shouldExcludeAppointmentAtEndOfAvailablePeriod() {
        List<TimePeroid> periods = new ArrayList<>();
        periods.add(new TimePeroid(LocalTime.of(6, 0), LocalTime.of(10, 0)));

        List<TimePeroid> result = appointmentService.excludeAppointmentsFromTimePeroids(periods, appointments(
                appointmentAt(LocalTime.of(9, 0), LocalTime.of(10, 0))));

        assertThat(result).containsExactly(
                new TimePeroid(LocalTime.of(6, 0), LocalTime.of(9, 0)));
    }

    private Appointment appointmentAt(LocalTime start, LocalTime end) {
        Appointment appointment = new Appointment();
        appointment.setStart(LocalDateTime.of(2026, 4, 30, start.getHour(), start.getMinute()));
        appointment.setEnd(LocalDateTime.of(2026, 4, 30, end.getHour(), end.getMinute()));
        return appointment;
    }

    private List<Appointment> appointments(Appointment appointment) {
        List<Appointment> appointments = new ArrayList<>();
        appointments.add(appointment);
        return appointments;
    }
}
