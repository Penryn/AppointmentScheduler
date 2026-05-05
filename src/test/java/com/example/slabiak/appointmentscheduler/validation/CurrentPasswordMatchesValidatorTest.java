package com.example.slabiak.appointmentscheduler.validation;

import com.example.slabiak.appointmentscheduler.entity.user.User;
import com.example.slabiak.appointmentscheduler.model.ChangePasswordForm;
import com.example.slabiak.appointmentscheduler.service.UserService;
import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CurrentPasswordMatchesValidatorTest {

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserService userService;

    @Mock
    private ConstraintValidatorContext context;

    @Mock
    private ConstraintValidatorContext.ConstraintViolationBuilder violationBuilder;

    @Mock
    private ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderCustomizableContext nodeBuilder;

    private CurrentPasswordMatchesValidator validator;

    @BeforeEach
    void setUp() {
        validator = new CurrentPasswordMatchesValidator();
        ReflectionTestUtils.setField(validator, "passwordEncoder", passwordEncoder);
        ReflectionTestUtils.setField(validator, "userService", userService);
    }

    @Test
    void shouldAcceptMatchingCurrentPassword() {
        ChangePasswordForm form = form();
        User user = user();
        when(userService.getUserById(3)).thenReturn(user);
        when(passwordEncoder.matches("current", "encoded")).thenReturn(true);

        assertThat(validator.isValid(form, context)).isTrue();

        verify(context, never()).disableDefaultConstraintViolation();
    }

    @Test
    void shouldAttachViolationToCurrentPasswordWhenPasswordDoesNotMatch() {
        ChangePasswordForm form = form();
        User user = user();
        when(userService.getUserById(3)).thenReturn(user);
        when(passwordEncoder.matches("current", "encoded")).thenReturn(false);
        when(context.getDefaultConstraintMessageTemplate()).thenReturn("当前密码不正确");
        when(context.buildConstraintViolationWithTemplate("当前密码不正确")).thenReturn(violationBuilder);
        when(violationBuilder.addPropertyNode("currentPassword")).thenReturn(nodeBuilder);

        assertThat(validator.isValid(form, context)).isFalse();

        verify(context).disableDefaultConstraintViolation();
        verify(context).buildConstraintViolationWithTemplate("当前密码不正确");
        verify(violationBuilder).addPropertyNode("currentPassword");
        verify(nodeBuilder).addConstraintViolation();
    }

    private ChangePasswordForm form() {
        ChangePasswordForm form = new ChangePasswordForm(3);
        form.setCurrentPassword("current");
        form.setPassword("newpass");
        form.setMatchingPassword("newpass");
        return form;
    }

    private User user() {
        User user = new User();
        user.setId(3);
        user.setPassword("encoded");
        return user;
    }
}
