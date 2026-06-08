package com.example.slabiak.appointmentscheduler.decisiontable;

import com.example.slabiak.appointmentscheduler.model.UserForm;
import com.example.slabiak.appointmentscheduler.validation.groups.UpdateProvider;
import com.example.slabiak.appointmentscheduler.validation.groups.UpdateUser;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UserFormDecisionTableTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void shouldHaveNoViolationsWhenNoValidationGroupIsUsed() {
        UserForm form = new UserForm();

        Set<ConstraintViolation<UserForm>> violations = validator.validate(form);

        assertEquals(0, violations.size());
    }

    @Test
    void shouldHaveUserViolationsWhenUpdateUserGroupIsUsed() {
        UserForm form = new UserForm();

        Set<ConstraintViolation<UserForm>> violations = validator.validate(form, UpdateUser.class);

        assertEquals(8, violations.size());
    }

    @Test
    void shouldHaveProviderViolationWhenUpdateProviderGroupIsUsed() {
        UserForm form = new UserForm();

        Set<ConstraintViolation<UserForm>> violations = validator.validate(form, UpdateProvider.class);

        assertEquals(1, violations.size());
    }

    @Test
    void shouldHaveUserAndProviderViolationsWhenBothGroupsAreUsed() {
        UserForm form = new UserForm();

        Set<ConstraintViolation<UserForm>> violations = validator.validate(form, UpdateUser.class, UpdateProvider.class);

        assertEquals(9, violations.size());
    }
}
