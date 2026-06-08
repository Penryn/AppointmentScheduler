// 测试说明：验证通知控制器的列表展示和已读状态处理行为。
package com.example.slabiak.appointmentscheduler.controller;

import com.example.slabiak.appointmentscheduler.entity.Notification;
import com.example.slabiak.appointmentscheduler.security.CustomUserDetails;
import com.example.slabiak.appointmentscheduler.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    @Mock
    private NotificationService notificationService;

    private NotificationController controller;

    @BeforeEach
    void setUp() {
        controller = new NotificationController(notificationService);
    }

    @Test
    void shouldShowUserNotificationList() {
        PageRequest pageable = PageRequest.of(0, 20);
        when(notificationService.getAll(3, pageable)).thenReturn(Page.empty(pageable));
        Model model = new ExtendedModelMap();

        // 检查点：验证该测试用例的预期结果。
        assertThat(controller.showUserNotificationList(model, user(3), pageable))
                .isEqualTo("notifications/listNotifications");
        assertThat(model.getAttribute("notifications")).isEqualTo(Page.empty(pageable));
    }

    @Test
    void shouldMarkNotificationAsReadAndRedirectToTargetUrl() {
        Notification notification = new Notification();
        notification.setUrl("/appointments/10");
        when(notificationService.getNotificationById(7)).thenReturn(notification);

        // 检查点：验证该测试用例的预期结果。
        assertThat(controller.showNotification(7, user(3))).isEqualTo("redirect:/appointments/10");
        verify(notificationService).markAsRead(7, 3);
    }

    @Test
    void shouldMarkAllNotificationsAsRead() {
        // 检查点：验证该测试用例的预期结果。
        assertThat(controller.processMarkAllAsRead(user(3))).isEqualTo("redirect:/notifications");
        verify(notificationService).markAllAsRead(3);
    }

    private CustomUserDetails user(int id) {
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
