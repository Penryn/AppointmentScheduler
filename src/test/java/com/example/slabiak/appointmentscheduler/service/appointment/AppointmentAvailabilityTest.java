package com.example.slabiak.appointmentscheduler.service.appointment;

import com.example.slabiak.appointmentscheduler.dao.AppointmentRepository;
import com.example.slabiak.appointmentscheduler.entity.Appointment;
import com.example.slabiak.appointmentscheduler.entity.WorkingPlan;
import com.example.slabiak.appointmentscheduler.entity.Work;
import com.example.slabiak.appointmentscheduler.entity.user.provider.Provider;
import com.example.slabiak.appointmentscheduler.model.DayPlan;
import com.example.slabiak.appointmentscheduler.model.TimePeroid;
import com.example.slabiak.appointmentscheduler.service.NotificationService;
import com.example.slabiak.appointmentscheduler.service.UserService;
import com.example.slabiak.appointmentscheduler.service.WorkService;
import com.example.slabiak.appointmentscheduler.service.impl.AppointmentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@org.junit.jupiter.api.extension.ExtendWith(MockitoExtension.class)
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
    @BeforeEach
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

    @Test
    public void shouldCreateSlotThatEndsExactlyAtAvailablePeriodEnd() {
        Work twoHourWork = new Work();
        twoHourWork.setDuration(120);

        List<TimePeroid> slots = appointmentService.calculateAvailableHours(
                List.of(new TimePeroid(LocalTime.of(8, 0), LocalTime.of(10, 0))),
                twoHourWork);

        assertThat(slots).containsExactly(new TimePeroid(LocalTime.of(8, 0), LocalTime.of(10, 0)));
    }

    @Test
    public void shouldReturnNoSlotsWhenWorkDurationExceedsAvailablePeriod() {
        Work twoHourWork = new Work();
        twoHourWork.setDuration(120);

        List<TimePeroid> slots = appointmentService.calculateAvailableHours(
                List.of(new TimePeroid(LocalTime.of(8, 0), LocalTime.of(9, 0))),
                twoHourWork);

        assertThat(slots).isEmpty();
    }

    @Test
    public void shouldKeepSlotStartingExactlyWhenBreakEnds() {
        DayPlan dayPlan = new DayPlan();
        dayPlan.setWorkingHours(new TimePeroid(LocalTime.of(8, 0), LocalTime.of(12, 0)));
        dayPlan.setBreaks(new ArrayList<>(List.of(new TimePeroid(LocalTime.of(8, 0), LocalTime.of(9, 0)))));

        List<TimePeroid> periods = dayPlan.timePeroidsWithBreaksExcluded();
        List<TimePeroid> slots = appointmentService.calculateAvailableHours(periods, oneHourWork);

        assertThat(slots).contains(
                new TimePeroid(LocalTime.of(9, 0), LocalTime.of(10, 0)),
                new TimePeroid(LocalTime.of(11, 0), LocalTime.of(12, 0)));
    }

    @Test
    public void shouldExcludeBreakTimeFromWorkingDay() {
        DayPlan dayPlan = new DayPlan();
        dayPlan.setWorkingHours(new TimePeroid(LocalTime.of(8, 0), LocalTime.of(12, 0)));
        dayPlan.setBreaks(new ArrayList<>(List.of(new TimePeroid(LocalTime.of(9, 0), LocalTime.of(10, 0)))));

        List<TimePeroid> periods = dayPlan.timePeroidsWithBreaksExcluded();

        assertThat(periods).containsExactly(
                new TimePeroid(LocalTime.of(8, 0), LocalTime.of(9, 0)),
                new TimePeroid(LocalTime.of(10, 0), LocalTime.of(12, 0)));
    }

    @Test
    public void shouldReportStartUnavailableWhenProviderAlreadyHasAppointment() {
        LocalDateTime start = LocalDateTime.of(2032, 1, 19, 10, 0);
        Provider provider = providerWithWorkingHours(LocalTime.of(8, 0), LocalTime.of(12, 0));
        Work work = work(60);
        when(workService.isWorkForCustomer(1, 3)).thenReturn(true);
        when(workService.getWorkById(1)).thenReturn(work);
        when(userService.getProviderById(2)).thenReturn(provider);
        when(appointmentRepository.findByProviderIdWithStartInPeroid(2, start.toLocalDate().atStartOfDay(), start.toLocalDate().atStartOfDay().plusDays(1)))
                .thenReturn(appointments(appointmentAt(LocalTime.of(10, 0), LocalTime.of(11, 0))));
        when(appointmentRepository.findByCustomerIdWithStartInPeroid(3, start.toLocalDate().atStartOfDay(), start.toLocalDate().atStartOfDay().plusDays(1)))
                .thenReturn(new ArrayList<>());

        boolean available = appointmentService.isAvailable(1, 2, 3, start);

        assertThat(available).isFalse();
    }

    @Test
    public void shouldReportStartUnavailableWhenCustomerAlreadyHasAppointment() {
        LocalDateTime start = LocalDateTime.of(2032, 1, 20, 10, 0);
        Provider provider = providerWithWorkingHours(LocalTime.of(8, 0), LocalTime.of(12, 0));
        Work work = work(60);
        when(workService.isWorkForCustomer(1, 3)).thenReturn(true);
        when(workService.getWorkById(1)).thenReturn(work);
        when(userService.getProviderById(2)).thenReturn(provider);
        when(appointmentRepository.findByProviderIdWithStartInPeroid(2, start.toLocalDate().atStartOfDay(), start.toLocalDate().atStartOfDay().plusDays(1)))
                .thenReturn(new ArrayList<>());
        when(appointmentRepository.findByCustomerIdWithStartInPeroid(3, start.toLocalDate().atStartOfDay(), start.toLocalDate().atStartOfDay().plusDays(1)))
                .thenReturn(appointments(appointmentAt(LocalTime.of(10, 0), LocalTime.of(11, 0))));

        boolean available = appointmentService.isAvailable(1, 2, 3, start);

        assertThat(available).isFalse();
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

    private Work work(int duration) {
        Work work = new Work();
        work.setDuration(duration);
        return work;
    }

    private Provider providerWithWorkingHours(LocalTime start, LocalTime end) {
        Provider provider = new Provider();
        provider.setId(2);
        DayPlan dayPlan = new DayPlan();
        dayPlan.setWorkingHours(new TimePeroid(start, end));
        WorkingPlan workingPlan = new WorkingPlan();
        workingPlan.setMonday(dayPlan);
        workingPlan.setTuesday(dayPlan);
        workingPlan.setWednesday(dayPlan);
        workingPlan.setThursday(dayPlan);
        workingPlan.setFriday(dayPlan);
        workingPlan.setSaturday(dayPlan);
        workingPlan.setSunday(dayPlan);
        provider.setWorkingPlan(workingPlan);
        return provider;
    }
}
