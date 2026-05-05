package com.example.slabiak.appointmentscheduler.validation;

import com.example.slabiak.appointmentscheduler.model.UserForm;
import com.example.slabiak.appointmentscheduler.validation.groups.UpdateProvider;
import com.example.slabiak.appointmentscheduler.validation.groups.UpdateUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Collections;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class UpdateProviderValidationTest {

    private ValidatorFactory factory;
    private Validator validator;

    @BeforeEach
    public void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    public void shouldHave9ViolationsForEmptyFormWhenUpdateProvider() {
        UserForm form = new UserForm();
        Set<ConstraintViolation<UserForm>> violations = validator.validate(form, UpdateUser.class, UpdateProvider.class);
        assertEquals(violations.size(), 9);
    }

    @Test
    public void shouldRejectProviderWithoutAssignedWorks() {
        UserForm form = validProviderForm();
        form.setWorks(Collections.emptyList());

        Set<ConstraintViolation<UserForm>> violations = validator.validate(form, UpdateUser.class, UpdateProvider.class);

        assertEquals(1, violations.size());
    }

    @Test
    public void shouldAcceptProviderWithAssignedWorks() {
        UserForm form = validProviderForm();

        Set<ConstraintViolation<UserForm>> violations = validator.validate(form, UpdateUser.class, UpdateProvider.class);

        assertTrue(violations.isEmpty());
    }

    private UserForm validProviderForm() {
        UserForm form = new UserForm();
        form.setId(2);
        form.setFirstName("Provider");
        form.setLastName("One");
        form.setEmail("provider@example.com");
        form.setMobile("13800138000");
        form.setStreet("北京市朝阳区示例路1号");
        form.setPostcode("100000");
        form.setCity("北京");
        form.setWorks(java.util.List.of(new com.example.slabiak.appointmentscheduler.entity.Work()));
        return form;
    }
}
