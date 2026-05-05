package com.example.slabiak.appointmentscheduler.service.workingplan;

import com.example.slabiak.appointmentscheduler.dao.WorkingPlanRepository;
import com.example.slabiak.appointmentscheduler.entity.WorkingPlan;
import com.example.slabiak.appointmentscheduler.entity.user.provider.Provider;
import com.example.slabiak.appointmentscheduler.model.DayPlan;
import com.example.slabiak.appointmentscheduler.model.TimePeroid;
import com.example.slabiak.appointmentscheduler.security.CustomUserDetails;
import com.example.slabiak.appointmentscheduler.service.impl.WorkingPlanServiceImpl;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkingPlanServiceTest {

    @Mock
    private WorkingPlanRepository workingPlanRepository;

    private WorkingPlanServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new WorkingPlanServiceImpl(workingPlanRepository);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldUpdateWorkingHoursForEveryDay() {
        WorkingPlan existing = plan(2);
        WorkingPlan update = plan(2);
        update.setMonday(day(8, 16));
        update.setTuesday(day(9, 17));
        update.setWednesday(day(10, 18));
        update.setThursday(day(11, 19));
        update.setFriday(day(12, 20));
        update.setSaturday(day(13, 21));
        update.setSunday(day(14, 22));
        when(workingPlanRepository.findById(2)).thenReturn(Optional.of(existing));

        service.updateWorkingPlan(update);

        ArgumentCaptor<WorkingPlan> captor = ArgumentCaptor.forClass(WorkingPlan.class);
        verify(workingPlanRepository).save(captor.capture());
        WorkingPlan saved = captor.getValue();
        assertThat(saved.getMonday().getWorkingHours()).isEqualTo(update.getMonday().getWorkingHours());
        assertThat(saved.getTuesday().getWorkingHours()).isEqualTo(update.getTuesday().getWorkingHours());
        assertThat(saved.getWednesday().getWorkingHours()).isEqualTo(update.getWednesday().getWorkingHours());
        assertThat(saved.getThursday().getWorkingHours()).isEqualTo(update.getThursday().getWorkingHours());
        assertThat(saved.getFriday().getWorkingHours()).isEqualTo(update.getFriday().getWorkingHours());
        assertThat(saved.getSaturday().getWorkingHours()).isEqualTo(update.getSaturday().getWorkingHours());
        assertThat(saved.getSunday().getWorkingHours()).isEqualTo(update.getSunday().getWorkingHours());
    }

    @Test
    void shouldAddAndDeleteBreakWhenCurrentUserOwnsPlan() {
        authenticateAs(2);
        WorkingPlan plan = plan(2);
        TimePeroid breakTime = new TimePeroid(LocalTime.of(12, 0), LocalTime.of(12, 30));
        when(workingPlanRepository.findById(2)).thenReturn(Optional.of(plan));

        service.addBreakToWorkingPlan(breakTime, 2, "monday");
        assertThat(plan.getMonday().getBreaks()).containsExactly(breakTime);

        service.deleteBreakFromWorkingPlan(breakTime, 2, "monday");
        assertThat(plan.getMonday().getBreaks()).isEmpty();
        verify(workingPlanRepository, org.mockito.Mockito.times(2)).save(plan);
    }

    @Test
    void shouldRejectBreakChangesForDifferentProvider() {
        authenticateAs(3);
        WorkingPlan plan = plan(2);
        TimePeroid breakTime = new TimePeroid(LocalTime.of(12, 0), LocalTime.of(12, 30));
        when(workingPlanRepository.findById(2)).thenReturn(Optional.of(plan));

        assertThatThrownBy(() -> service.addBreakToWorkingPlan(breakTime, 2, "monday"))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> service.deleteBreakFromWorkingPlan(breakTime, 2, "monday"))
                .isInstanceOf(AccessDeniedException.class);
        verify(workingPlanRepository, never()).save(plan);
    }

    @Test
    void shouldFindPlanByProviderAndThrowWhenMissing() {
        WorkingPlan plan = plan(2);
        when(workingPlanRepository.getWorkingPlanByProviderId(2)).thenReturn(plan);
        when(workingPlanRepository.findById(99)).thenReturn(Optional.empty());

        assertThat(service.getWorkingPlanByProviderId(2)).isSameAs(plan);
        assertThatThrownBy(() -> service.updateWorkingPlan(missingUpdate(99)))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Working plan not found");
    }

    private WorkingPlan missingUpdate(int id) {
        WorkingPlan plan = plan(2);
        plan.setId(id);
        return plan;
    }

    private WorkingPlan plan(int providerId) {
        Provider provider = new Provider();
        provider.setId(providerId);
        WorkingPlan plan = new WorkingPlan();
        plan.setId(providerId);
        plan.setProvider(provider);
        plan.setMonday(day(6, 18));
        plan.setTuesday(day(6, 18));
        plan.setWednesday(day(6, 18));
        plan.setThursday(day(6, 18));
        plan.setFriday(day(6, 18));
        plan.setSaturday(day(6, 18));
        plan.setSunday(day(6, 18));
        return plan;
    }

    private DayPlan day(int startHour, int endHour) {
        DayPlan dayPlan = new DayPlan();
        dayPlan.setWorkingHours(new TimePeroid(LocalTime.of(startHour, 0), LocalTime.of(endHour, 0)));
        return dayPlan;
    }

    private void authenticateAs(int userId) {
        CustomUserDetails principal = new CustomUserDetails(
                userId,
                "First",
                "Last",
                "user" + userId,
                "user" + userId + "@example.com",
                "password",
                List.of(new SimpleGrantedAuthority("ROLE_PROVIDER"))
        );
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(principal, null));
    }
}
