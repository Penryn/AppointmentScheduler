package com.example.slabiak.appointmentscheduler.validation;

import com.example.slabiak.appointmentscheduler.model.UserForm;
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

public class UpdateUserValidationTest {

    private ValidatorFactory factory;
    private Validator validator;

    @BeforeEach
    public void stup() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    public void shouldHave8ViolationsForEmptyFormWhenUpdateUser() {
        UserForm form = new UserForm();
        Set<ConstraintViolation<UserForm>> violations = validator.validate(form, UpdateUser.class);
        assertEquals(violations.size(), 8);
    }

    @Test
    public void shouldAcceptChineseMobileAndPostcodeWhenUpdateUser() {
        UserForm form = validUserForm();

        Set<ConstraintViolation<UserForm>> violations = validator.validate(form, UpdateUser.class);

        assertTrue(violations.isEmpty());
    }

    @Test
    public void shouldRejectNonChineseMobileAndPostcodeWhenUpdateUser() {
        UserForm form = validUserForm();
        form.setMobile("123456789");
        form.setPostcode("12-345");

        Set<ConstraintViolation<UserForm>> violations = validator.validate(form, UpdateUser.class);

        assertEquals(2, violations.size());
    }

    private UserForm validUserForm() {
        UserForm form = new UserForm();
        form.setId(1);
        form.setUserName("zhangsan");
        form.setFirstName("San");
        form.setLastName("Zhang");
        form.setEmail("zhangsan@example.com");
        form.setMobile("13800138000");
        form.setStreet("北京市朝阳区示例路1号");
        form.setPostcode("100000");
        form.setCity("北京");
        return form;
    }

}
