// 测试说明：验证用户服务在真实持久化环境中的查询和资料更新行为。
package com.example.slabiak.appointmentscheduler.service.user;

import com.example.slabiak.appointmentscheduler.model.UserForm;
import com.example.slabiak.appointmentscheduler.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("integration-test")
public class UserServiceIT {

    @Autowired
    private UserService userService;

    @Test
    public void shouldSaveNewRetailCustomer() {
        UserForm userForm = UserFactoryTest.prepareSampleUserForm();
        int customerCountBeforeSave = userService.getAllRetailCustomers().size();

        userService.saveNewRetailCustomer(userForm);

        // 检查点：验证该测试用例的预期结果。
        assertThat(userService.getAllRetailCustomers())
                .hasSize(customerCountBeforeSave + 1)
                .anyMatch(customer -> customer.getUserName().equals(userForm.getUserName()));
    }

}
