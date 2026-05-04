package com.example.slabiak.appointmentscheduler.controller;

import com.example.slabiak.appointmentscheduler.entity.Work;
import com.example.slabiak.appointmentscheduler.entity.WorkingPlan;
import com.example.slabiak.appointmentscheduler.entity.user.provider.Provider;
import com.example.slabiak.appointmentscheduler.model.ChangePasswordForm;
import com.example.slabiak.appointmentscheduler.model.TimePeroid;
import com.example.slabiak.appointmentscheduler.model.UserForm;
import com.example.slabiak.appointmentscheduler.security.CustomUserDetails;
import com.example.slabiak.appointmentscheduler.service.AppointmentService;
import com.example.slabiak.appointmentscheduler.service.UserService;
import com.example.slabiak.appointmentscheduler.service.WorkService;
import com.example.slabiak.appointmentscheduler.service.WorkingPlanService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProviderControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private WorkService workService;

    @Mock
    private WorkingPlanService workingPlanService;

    @Mock
    private AppointmentService appointmentService;

    @Mock
    private BindingResult bindingResult;

    private ProviderController controller;

    @BeforeEach
    void setUp() {
        controller = new ProviderController(userService, workService, workingPlanService, appointmentService);
    }

    @Test
    void shouldShowAllProviders() {
        PageRequest pageable = PageRequest.of(0, 20);
        when(userService.getProviderList(pageable)).thenReturn(Page.empty(pageable));
        Model model = new ExtendedModelMap();

        String view = controller.showAllProviders(model, pageable);

        assertThat(view).isEqualTo("users/listProviders");
        assertThat(model.getAttribute("providers")).isEqualTo(Page.empty(pageable));
    }

    @Test
    void shouldShowProviderDetailsForOwnerOrAdmin() {
        Provider provider = provider(2);
        Work work = new Work();
        when(userService.getProviderById(2)).thenReturn(provider);
        when(workService.getAllWorks()).thenReturn(List.of(work));
        when(appointmentService.getNumberOfScheduledAppointmentsForUser(2)).thenReturn(3);
        when(appointmentService.getNumberOfCanceledAppointmentsForUser(2)).thenReturn(1);
        Model model = new ExtendedModelMap();

        String view = controller.showProviderDetails(2, model, user(2, "ROLE_PROVIDER"));

        assertThat(view).isEqualTo("users/updateUserForm");
        assertThat(model.getAttribute("user")).isInstanceOf(UserForm.class);
        assertThat(model.getAttribute("passwordChange")).isInstanceOf(ChangePasswordForm.class);
        assertThat(model.getAttribute("account_type")).isEqualTo("provider");
        assertThat(model.getAttribute("allWorks")).isEqualTo(List.of(work));
        assertThat(model.getAttribute("numberOfScheduledAppointments")).isEqualTo(3);
        assertThat(model.getAttribute("numberOfCanceledAppointments")).isEqualTo(1);
    }

    @Test
    void shouldDenyProviderDetailsForDifferentNonAdminUser() {
        assertThatThrownBy(() -> controller.showProviderDetails(2, new ExtendedModelMap(), user(3, "ROLE_PROVIDER")))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void shouldHandleProviderProfileUpdateSuccessAndValidationFailure() {
        UserForm form = providerForm();
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();
        when(bindingResult.hasErrors()).thenReturn(true, false);

        assertThat(controller.processProviderUpdate(form, bindingResult, redirectAttributes))
                .isEqualTo("redirect:/providers/2");
        assertThat(redirectAttributes.getFlashAttributes()).containsKey("user");
        verify(userService, never()).updateProviderProfile(form);

        assertThat(controller.processProviderUpdate(form, bindingResult, new RedirectAttributesModelMap()))
                .isEqualTo("redirect:/providers/2");
        verify(userService).updateProviderProfile(form);
    }

    @Test
    void shouldShowAndProcessProviderRegistration() {
        UserForm form = providerForm();
        Work work = new Work();
        when(workService.getAllWorks()).thenReturn(List.of(work));
        Model model = new ExtendedModelMap();

        assertThat(controller.showProviderRegistrationForm(model)).isEqualTo("users/createUserForm");
        assertThat(model.getAttribute("account_type")).isEqualTo("provider");
        assertThat(model.getAttribute("registerAction")).isEqualTo("/providers/new");
        assertThat(model.getAttribute("allWorks")).isEqualTo(List.of(work));

        when(bindingResult.hasErrors()).thenReturn(true, false);
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();
        assertThat(controller.processProviderRegistrationForm(form, bindingResult, redirectAttributes))
                .isEqualTo("redirect:/providers/new");
        assertThat(redirectAttributes.getFlashAttributes()).containsKey("user");

        assertThat(controller.processProviderRegistrationForm(form, bindingResult, new RedirectAttributesModelMap()))
                .isEqualTo("redirect:/providers/all");
        verify(userService).saveNewProvider(form);
    }

    @Test
    void shouldDeleteProviderAndManageAvailability() {
        WorkingPlan plan = new WorkingPlan();
        plan.setId(7);
        TimePeroid timePeroid = new TimePeroid();
        when(workingPlanService.getWorkingPlanByProviderId(2)).thenReturn(plan);
        Model model = new ExtendedModelMap();

        assertThat(controller.processDeleteProviderRequest(2)).isEqualTo("redirect:/providers/all");
        assertThat(controller.showProviderAvailability(model, user(2, "ROLE_PROVIDER"))).isEqualTo("users/showOrUpdateProviderAvailability");
        assertThat(controller.processProviderWorkingPlanUpdate(plan)).isEqualTo("redirect:/providers/availability");
        assertThat(controller.processProviderAddBreak(timePeroid, 7, "MONDAY")).isEqualTo("redirect:/providers/availability");
        assertThat(controller.processProviderDeleteBreak(timePeroid, 7, "MONDAY")).isEqualTo("redirect:/providers/availability");

        verify(userService).deleteUserById(2);
        verify(workingPlanService).updateWorkingPlan(plan);
        verify(workingPlanService).addBreakToWorkingPlan(timePeroid, 7, "MONDAY");
        verify(workingPlanService).deleteBreakFromWorkingPlan(timePeroid, 7, "MONDAY");
        assertThat(model.getAttribute("plan")).isSameAs(plan);
        assertThat(model.getAttribute("breakModel")).isInstanceOf(TimePeroid.class);
    }

    @Test
    void shouldHandleProviderPasswordUpdateSuccessAndValidationFailure() {
        ChangePasswordForm form = new ChangePasswordForm(2);
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();
        when(bindingResult.hasErrors()).thenReturn(true, false);

        assertThat(controller.processProviderPasswordUpate(form, bindingResult, redirectAttributes))
                .isEqualTo("redirect:/providers/2");
        assertThat(redirectAttributes.getFlashAttributes()).containsKey("passwordChange");
        verify(userService, never()).updateUserPassword(form);

        assertThat(controller.processProviderPasswordUpate(form, bindingResult, new RedirectAttributesModelMap()))
                .isEqualTo("redirect:/providers/2");
        verify(userService).updateUserPassword(form);
    }

    private Provider provider(int id) {
        Provider provider = new Provider();
        provider.setId(id);
        provider.setFirstName("Alan");
        provider.setLastName("Turing");
        provider.setEmail("alan@example.com");
        provider.setMobile("13800138000");
        provider.setStreet("123 Test Street");
        provider.setPostcode("100000");
        provider.setCity("Beijing");
        provider.setWorks(List.of(new Work()));
        return provider;
    }

    private UserForm providerForm() {
        UserForm form = new UserForm();
        form.setId(2);
        form.setUserName("provider");
        form.setPassword("secret");
        form.setWorks(List.of(new Work()));
        return form;
    }

    private CustomUserDetails user(int id, String... roles) {
        return new CustomUserDetails(
                id,
                "First",
                "Last",
                "user" + id,
                "user" + id + "@example.com",
                "password",
                java.util.Arrays.stream(roles).map(SimpleGrantedAuthority::new).toList()
        );
    }
}
