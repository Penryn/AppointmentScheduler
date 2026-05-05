package com.example.slabiak.appointmentscheduler.model;

import com.example.slabiak.appointmentscheduler.entity.Work;
import com.example.slabiak.appointmentscheduler.entity.WorkingPlan;
import com.example.slabiak.appointmentscheduler.entity.user.Role;
import com.example.slabiak.appointmentscheduler.entity.user.User;
import com.example.slabiak.appointmentscheduler.entity.user.provider.Provider;
import com.example.slabiak.appointmentscheduler.security.CustomUserDetails;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DomainModelTest {

    @Test
    void shouldSplitDayPlanAroundBreaksAndClampBreaksToWorkingHours() {
        DayPlan dayPlan = new DayPlan();
        dayPlan.setWorkingHours(new TimePeroid(LocalTime.of(9, 0), LocalTime.of(17, 0)));
        dayPlan.addBreak(new TimePeroid(LocalTime.of(8, 0), LocalTime.of(10, 0)));
        dayPlan.addBreak(new TimePeroid(LocalTime.of(12, 0), LocalTime.of(13, 0)));
        dayPlan.addBreak(new TimePeroid(LocalTime.of(16, 0), LocalTime.of(18, 0)));

        assertThat(dayPlan.timePeroidsWithBreaksExcluded())
                .containsExactly(
                        new TimePeroid(LocalTime.of(10, 0), LocalTime.of(12, 0)),
                        new TimePeroid(LocalTime.of(13, 0), LocalTime.of(16, 0))
                );

        dayPlan.removeBreak(new TimePeroid(LocalTime.of(12, 0), LocalTime.of(13, 0)));
        assertThat(dayPlan.getBreaks()).hasSize(2);
    }

    @Test
    void shouldResolveWorkingPlanDaysAndDefaults() {
        WorkingPlan workingPlan = WorkingPlan.generateDefaultWorkingPlan();

        assertThat(workingPlan.getDay("monday")).isSameAs(workingPlan.getMonday());
        assertThat(workingPlan.getDay("tuesday")).isSameAs(workingPlan.getTuesday());
        assertThat(workingPlan.getDay("wednesday")).isSameAs(workingPlan.getWednesday());
        assertThat(workingPlan.getDay("thursday")).isSameAs(workingPlan.getThursday());
        assertThat(workingPlan.getDay("friday")).isSameAs(workingPlan.getFriday());
        assertThat(workingPlan.getDay("saturday")).isSameAs(workingPlan.getSaturday());
        assertThat(workingPlan.getDay("sunday")).isSameAs(workingPlan.getSunday());
        assertThat(workingPlan.getDay("unknown")).isNull();
        assertThat(workingPlan.getMonday().getWorkingHours())
                .isEqualTo(new TimePeroid(LocalTime.of(6, 0), LocalTime.of(18, 0)));
    }

    @Test
    void shouldCreateAndUpdateProviderWithTargetedWorks() {
        Work retail = work(1, "retail");
        Work corporate = work(2, "corporate");
        UserForm form = userForm(List.of(retail, corporate));
        WorkingPlan workingPlan = WorkingPlan.generateDefaultWorkingPlan();
        Provider provider = new Provider(form, "encoded", List.of(new Role("ROLE_PROVIDER")), workingPlan);
        provider.setId(7);

        assertThat(provider.getUserName()).isEqualTo("provider");
        assertThat(provider.getPassword()).isEqualTo("encoded");
        assertThat(provider.getWorkingPlan()).isSameAs(workingPlan);
        assertThat(workingPlan.getProvider()).isSameAs(provider);
        assertThat(provider.getRetailWorks()).containsExactly(retail);
        assertThat(provider.getCorporateWorks()).containsExactly(corporate);

        Work updatedRetail = work(3, "retail");
        UserForm update = userForm(List.of(updatedRetail));
        update.setFirstName("Updated");
        provider.update(update);

        assertThat(provider.getFirstName()).isEqualTo("Updated");
        assertThat(provider.getWorks()).containsExactly(updatedRetail);
        assertThat(provider).isEqualTo(provider);
        assertThat(provider).isNotEqualTo(new User());
        Provider sameId = new Provider();
        sameId.setId(7);
        assertThat(provider).isEqualTo(sameId);
    }

    @Test
    void shouldExposeAppointmentRegisterFormProperties() {
        LocalDateTime start = LocalDateTime.of(2031, 1, 1, 10, 0);
        LocalDateTime end = start.plusHours(1);
        AppointmentRegisterForm form = new AppointmentRegisterForm(1, 2, start, end);

        form.setCustomerId(3);
        form.setWorkId(4);
        form.setProviderId(5);
        form.setStart(start.plusDays(1));
        form.setEnd(end.plusDays(1));

        assertThat(form.getCustomerId()).isEqualTo(3);
        assertThat(form.getWorkId()).isEqualTo(4);
        assertThat(form.getProviderId()).isEqualTo(5);
        assertThat(form.getStart()).isEqualTo(start.plusDays(1));
        assertThat(form.getEnd()).isEqualTo(end.plusDays(1));
    }

    @Test
    void shouldCreateCustomUserDetailsFromUserAndCheckRoles() {
        User user = new User();
        user.setId(3);
        user.setFirstName("Ada");
        user.setLastName("Lovelace");
        user.setUserName("ada");
        user.setEmail("ada@example.com");
        user.setPassword("secret");
        user.setRoles(List.of(new Role("ROLE_ADMIN")));

        CustomUserDetails details = CustomUserDetails.create(user);
        CustomUserDetails sameId = new CustomUserDetails(
                3,
                "Other",
                "User",
                "other",
                "other@example.com",
                "password",
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );

        assertThat(details.getId()).isEqualTo(3);
        assertThat(details.getFirstName()).isEqualTo("Ada");
        assertThat(details.getLastName()).isEqualTo("Lovelace");
        assertThat(details.getUsername()).isEqualTo("ada");
        assertThat(details.getEmail()).isEqualTo("ada@example.com");
        assertThat(details.getPassword()).isEqualTo("secret");
        assertThat(details.hasRole("ROLE_ADMIN")).isTrue();
        assertThat(details.hasRole("ROLE_PROVIDER")).isFalse();
        assertThat(details.isAccountNonExpired()).isTrue();
        assertThat(details.isAccountNonLocked()).isTrue();
        assertThat(details.isCredentialsNonExpired()).isTrue();
        assertThat(details.isEnabled()).isTrue();
        assertThat(details).isEqualTo(sameId);
        assertThat(details).isNotEqualTo(null);
    }

    private UserForm userForm(List<Work> works) {
        UserForm form = new UserForm();
        form.setUserName("provider");
        form.setFirstName("First");
        form.setLastName("Last");
        form.setEmail("provider@example.com");
        form.setMobile("13800138000");
        form.setStreet("123 Test Street");
        form.setPostcode("100000");
        form.setCity("Beijing");
        form.setWorks(works);
        return form;
    }

    private Work work(int id, String target) {
        Work work = new Work();
        work.setId(id);
        work.setName(target);
        work.setTargetCustomer(target);
        return work;
    }
}
