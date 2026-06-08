// 测试说明：验证首页、登录页和访问拒绝页面的控制器返回结果。
package com.example.slabiak.appointmentscheduler.controller;

import com.example.slabiak.appointmentscheduler.entity.user.User;
import com.example.slabiak.appointmentscheduler.security.CustomUserDetails;
import com.example.slabiak.appointmentscheduler.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HomeControllerTest {

    @Mock
    private UserService userService;

    private HomeController controller;

    @BeforeEach
    void setUp() {
        controller = new HomeController(userService);
    }

    @Test
    void shouldShowHomeForCurrentUser() {
        User user = new User();
        user.setId(3);
        when(userService.getUserById(3)).thenReturn(user);
        Model model = new ExtendedModelMap();

        // 检查点：验证该测试用例的预期结果。
        assertThat(controller.showHome(model, userDetails(3))).isEqualTo("home");
        assertThat(model.getAttribute("user")).isSameAs(user);
    }

    @Test
    void shouldShowLoginOnlyForAnonymousUsersAndAccessDeniedPage() {
        // 检查点：验证该测试用例的预期结果。
        assertThat(controller.login(new ExtendedModelMap(), null)).isEqualTo("users/login");
        assertThat(controller.login(new ExtendedModelMap(), userDetails(3))).isEqualTo("redirect:/");
        assertThat(controller.showAccessDeniedPage()).isEqualTo("access-denied");
    }

    private CustomUserDetails userDetails(int id) {
        return new CustomUserDetails(
                id,
                "First",
                "Last",
                "user" + id,
                "user" + id + "@example.com",
                "password",
                List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))
        );
    }
}
