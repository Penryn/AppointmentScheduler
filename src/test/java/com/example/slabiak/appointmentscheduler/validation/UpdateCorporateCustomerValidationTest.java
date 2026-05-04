package com.example.slabiak.appointmentscheduler.validation;

import com.example.slabiak.appointmentscheduler.model.UserForm;
import com.example.slabiak.appointmentscheduler.validation.groups.UpdateCorporateCustomer;
import com.example.slabiak.appointmentscheduler.validation.groups.UpdateUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class UpdateCorporateCustomerValidationTest {

    private ValidatorFactory factory;
    private Validator validator;

    @BeforeEach
    public void stup() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    public void shouldHave10ViolationsForEmptyFormWhenUpdateCorporateCustomer() {
        UserForm form = new UserForm();
        Set<ConstraintViolation<UserForm>> violations = validator.validate(form, UpdateUser.class, UpdateCorporateCustomer.class);
        assertEquals(violations.size(), 10);
    }

    @Test
    public void shouldAcceptChineseUnifiedSocialCreditCodeWhenUpdateCorporateCustomer() {
        UserForm form = validCorporateCustomerForm();

        Set<ConstraintViolation<UserForm>> violations = validator.validate(form, UpdateUser.class, UpdateCorporateCustomer.class);

        assertTrue(violations.isEmpty());
    }

    @Test
    public void shouldRejectLegacyVatNumberWhenUpdateCorporateCustomer() {
        UserForm form = validCorporateCustomerForm();
        form.setVatNumber("1234567890");

        Set<ConstraintViolation<UserForm>> violations = validator.validate(form, UpdateUser.class, UpdateCorporateCustomer.class);

        assertEquals(1, violations.size());
    }

    private UserForm validCorporateCustomerForm() {
        UserForm form = new UserForm();
        form.setId(1);
        form.setUserName("company01");
        form.setFirstName("San");
        form.setLastName("Zhang");
        form.setEmail("contact@example.com");
        form.setMobile("13800138000");
        form.setStreet("北京市朝阳区示例路1号");
        form.setPostcode("100000");
        form.setCity("北京");
        form.setCompanyName("示例科技有限公司");
        form.setVatNumber("91110000000000000X");
        return form;
    }
}
