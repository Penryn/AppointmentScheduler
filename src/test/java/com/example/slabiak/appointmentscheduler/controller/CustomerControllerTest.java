package com.example.slabiak.appointmentscheduler.controller;

import com.example.slabiak.appointmentscheduler.entity.user.Role;
import com.example.slabiak.appointmentscheduler.entity.user.customer.CorporateCustomer;
import com.example.slabiak.appointmentscheduler.entity.user.customer.Customer;
import com.example.slabiak.appointmentscheduler.entity.user.customer.RetailCustomer;
import com.example.slabiak.appointmentscheduler.model.ChangePasswordForm;
import com.example.slabiak.appointmentscheduler.model.UserForm;
import com.example.slabiak.appointmentscheduler.security.CustomUserDetails;
import com.example.slabiak.appointmentscheduler.service.AppointmentService;
import com.example.slabiak.appointmentscheduler.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private AppointmentService appointmentService;

    @Mock
    private BindingResult bindingResult;

    private CustomerController controller;

    @BeforeEach
    void setUp() {
        controller = new CustomerController(userService, appointmentService);
    }

    @Test
    void shouldShowAllCustomers() {
        PageRequest pageable = PageRequest.of(0, 20);
        when(userService.getCustomerList(pageable)).thenReturn(Page.empty(pageable));
        Model model = new ExtendedModelMap();

        String view = controller.showAllCustomers(model, pageable);

        assertThat(view).isEqualTo("users/listCustomers");
        assertThat(model.getAttribute("customers")).isEqualTo(Page.empty(pageable));
    }

    @Test
    void shouldShowCorporateCustomerDetails() {
        CorporateCustomer corporateCustomer = corporateCustomer(4);
        when(userService.getCustomerById(4)).thenReturn(corporateCustomer);
        when(userService.getCorporateCustomerById(4)).thenReturn(corporateCustomer);
        when(appointmentService.getNumberOfScheduledAppointmentsForUser(4)).thenReturn(2);
        when(appointmentService.getNumberOfCanceledAppointmentsForUser(4)).thenReturn(1);
        Model model = new ExtendedModelMap();

        String view = controller.showCustomerDetails(4, model);

        assertThat(view).isEqualTo("users/updateUserForm");
        assertThat(model.getAttribute("user")).isInstanceOf(UserForm.class);
        assertThat(model.getAttribute("passwordChange")).isInstanceOf(ChangePasswordForm.class);
        assertThat(model.getAttribute("account_type")).isEqualTo("customer_corporate");
        assertThat(model.getAttribute("formActionProfile")).isEqualTo("/customers/corporate/update/profile");
        assertThat(model.getAttribute("numberOfScheduledAppointments")).isEqualTo(2);
        assertThat(model.getAttribute("numberOfCanceledAppointments")).isEqualTo(1);
    }

    @Test
    void shouldShowRetailCustomerDetails() {
        RetailCustomer retailCustomer = retailCustomer(3);
        when(userService.getCustomerById(3)).thenReturn(retailCustomer);
        when(userService.getRetailCustomerById(3)).thenReturn(retailCustomer);
        Model model = new ExtendedModelMap();

        String view = controller.showCustomerDetails(3, model);

        assertThat(view).isEqualTo("users/updateUserForm");
        assertThat(model.getAttribute("account_type")).isEqualTo("customer_retail");
        assertThat(model.getAttribute("formActionProfile")).isEqualTo("/customers/retail/update/profile");
    }

    @Test
    void shouldHandleCustomerProfileUpdates() {
        UserForm form = userForm();
        when(bindingResult.hasErrors()).thenReturn(true, false, true, false);
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        assertThat(controller.processCorporateCustomerProfileUpdate(form, bindingResult, redirectAttributes))
                .isEqualTo("redirect:/customers/3");
        assertThat(redirectAttributes.getFlashAttributes()).containsKey("user");
        verify(userService, never()).updateCorporateCustomerProfile(form);

        assertThat(controller.processCorporateCustomerProfileUpdate(form, bindingResult, new RedirectAttributesModelMap()))
                .isEqualTo("redirect:/customers/3");
        verify(userService).updateCorporateCustomerProfile(form);

        RedirectAttributesModelMap retailRedirectAttributes = new RedirectAttributesModelMap();
        assertThat(controller.processRetailCustomerProfileUpdate(form, bindingResult, retailRedirectAttributes))
                .isEqualTo("redirect:/customers/3");
        assertThat(retailRedirectAttributes.getFlashAttributes()).containsKey("user");
        verify(userService, never()).updateRetailCustomerProfile(form);

        assertThat(controller.processRetailCustomerProfileUpdate(form, bindingResult, new RedirectAttributesModelMap()))
                .isEqualTo("redirect:/customers/3");
        verify(userService).updateRetailCustomerProfile(form);
    }

    @Test
    void shouldShowRegistrationFormByCustomerType() {
        Model corporateModel = new ExtendedModelMap();
        Model retailModel = new ExtendedModelMap();

        assertThat(controller.showCustomerRegistrationForm("corporate", corporateModel, null))
                .isEqualTo("users/createUserForm");
        assertThat(corporateModel.getAttribute("account_type")).isEqualTo("customer_corporate");
        assertThat(corporateModel.getAttribute("registerAction")).isEqualTo("/customers/new/corporate");

        assertThat(controller.showCustomerRegistrationForm("retail", retailModel, null))
                .isEqualTo("users/createUserForm");
        assertThat(retailModel.getAttribute("account_type")).isEqualTo("customer_retail");
        assertThat(retailModel.getAttribute("registerAction")).isEqualTo("/customers/new/retail");

        assertThat(controller.showCustomerRegistrationForm("retail", new ExtendedModelMap(), user(3, "ROLE_CUSTOMER")))
                .isEqualTo("redirect:/");
        assertThatThrownBy(() -> controller.showCustomerRegistrationForm("unknown", new ExtendedModelMap(), null))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void shouldProcessCustomerRegistrationSuccessAndValidationFailure() {
        UserForm form = userForm();
        when(bindingResult.hasErrors()).thenReturn(true, false, true, false);
        Model retailModel = new ExtendedModelMap();
        Model corporateModel = new ExtendedModelMap();

        assertThat(controller.processReatilCustomerRegistration(form, bindingResult, retailModel))
                .isEqualTo("users/createUserForm");
        assertThat(retailModel.getAttribute("account_type")).isEqualTo("customer_retail");
        verify(userService, never()).saveNewRetailCustomer(form);

        assertThat(controller.processReatilCustomerRegistration(form, bindingResult, new ExtendedModelMap()))
                .isEqualTo("users/login");
        verify(userService).saveNewRetailCustomer(form);

        assertThat(controller.processCorporateCustomerRegistration(form, bindingResult, corporateModel))
                .isEqualTo("users/createUserForm");
        assertThat(corporateModel.getAttribute("account_type")).isEqualTo("customer_corporate");
        verify(userService, never()).saveNewCorporateCustomer(form);

        Model successModel = new ExtendedModelMap();
        assertThat(controller.processCorporateCustomerRegistration(form, bindingResult, successModel))
                .isEqualTo("users/login");
        assertThat(successModel.getAttribute("createdUserName")).isEqualTo(form.getUserName());
        verify(userService).saveNewCorporateCustomer(form);
    }

    @Test
    void shouldHandlePasswordUpdate() {
        ChangePasswordForm form = new ChangePasswordForm(3);
        when(bindingResult.hasErrors()).thenReturn(true, false);
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();
        CustomUserDetails currentUser = user(3, "ROLE_CUSTOMER");

        assertThat(controller.processCustomerPasswordUpate(form, bindingResult, currentUser, redirectAttributes))
                .isEqualTo("redirect:/customers/3");
        assertThat(redirectAttributes.getFlashAttributes()).containsKey("passwordChange");
        verify(userService, never()).updateUserPassword(form);

        assertThat(controller.processCustomerPasswordUpate(form, bindingResult, currentUser, new RedirectAttributesModelMap()))
                .isEqualTo("redirect:/customers/3");
        verify(userService).updateUserPassword(form);
    }

    @Test
    void shouldRestrictCustomerDeletionToAdmins() throws Exception {
        Method method = CustomerController.class.getMethod("processDeleteCustomerRequest", int.class);

        assertThat(method.getAnnotation(PreAuthorize.class).value()).isEqualTo("hasRole('ADMIN')");
    }

    @Test
    void shouldPopulateRegistrationModel() {
        Model model = new ExtendedModelMap();
        UserForm form = userForm();

        assertThat(controller.populateModel(model, form, "customer_retail", "/customers/new/retail", "error"))
                .isSameAs(model);
        assertThat(model.getAttribute("user")).isSameAs(form);
        assertThat(model.getAttribute("registrationError")).isEqualTo("error");
    }

    private UserForm userForm() {
        UserForm form = new UserForm();
        form.setId(3);
        form.setUserName("customer");
        form.setPassword("secret");
        return form;
    }

    private RetailCustomer retailCustomer(int id) {
        RetailCustomer customer = new RetailCustomer();
        fillCustomer(customer, id, new Role("ROLE_CUSTOMER_RETAIL"));
        return customer;
    }

    private CorporateCustomer corporateCustomer(int id) {
        CorporateCustomer customer = new CorporateCustomer();
        fillCustomer(customer, id, new Role("ROLE_CUSTOMER_CORPORATE"));
        customer.setCompanyName("Acme");
        customer.setVatNumber("123456789012345678");
        return customer;
    }

    private void fillCustomer(Customer customer, int id, Role specificRole) {
        customer.setId(id);
        customer.setFirstName("Ada");
        customer.setLastName("Lovelace");
        customer.setEmail("ada@example.com");
        customer.setMobile("13800138000");
        customer.setStreet("123 Test Street");
        customer.setPostcode("100000");
        customer.setCity("Beijing");
        customer.setRoles(List.of(specificRole, new Role("ROLE_CUSTOMER")));
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
