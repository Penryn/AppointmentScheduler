// 测试说明：验证决策表驱动的表单校验规则输出。
package com.example.slabiak.appointmentscheduler.decisiontable;

import com.example.slabiak.appointmentscheduler.model.UserForm;
import com.example.slabiak.appointmentscheduler.validation.groups.UpdateCorporateCustomer;
import com.example.slabiak.appointmentscheduler.validation.groups.UpdateUser;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DecisionTableValidationTest {

    private Validator validator;

    @BeforeEach
    void setup() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @ParameterizedTest
    @MethodSource("validationCases")
    void shouldValidateCorporateCustomerRulesBasedOnDecisionTable(
            boolean validVat,
            boolean validEmail,
            boolean validMobile,
            int expectedViolations
    ) {
        UserForm form = validCorporateCustomerForm();

        if (!validVat) {
            form.setVatNumber("1234567890");
        }

        if (!validEmail) {
            form.setEmail("invalid-email");
        }

        if (!validMobile) {
            form.setMobile("123");
        }

        Set<ConstraintViolation<UserForm>> violations =
                validator.validate(form, UpdateUser.class, UpdateCorporateCustomer.class);

        // 检查点：验证该测试用例的预期结果。
        assertEquals(expectedViolations, violations.size());
    }

    private static Stream<Object[]> validationCases() {
        return Stream.of(new Object[][]{
                {true, true, true, 0},
                {false, true, true, 1},
                {true, false, true, 1},
                {true, true, false, 1},
                {false, false, true, 2},
                {false, true, false, 2},
                {true, false, false, 2},
                {false, false, false, 3}
        });
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
