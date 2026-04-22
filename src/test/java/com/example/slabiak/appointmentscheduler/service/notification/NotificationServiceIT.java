package com.example.slabiak.appointmentscheduler.service.notification;

import com.example.slabiak.appointmentscheduler.service.NotificationService;
import com.example.slabiak.appointmentscheduler.service.UserService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@RunWith(SpringRunner.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("integration-test")
public class NotificationServiceIT {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private UserService userService;

    @Test
    @Transactional
    @WithUserDetails("customer_r")
    public void shouldMarkAllNotificationsAsReadWithBulkUpdate() {
        notificationService.newNotification("n1", "m1", "/appointments/1", userService.getUserById(3));
        notificationService.newNotification("n2", "m2", "/appointments/1", userService.getUserById(3));

        assertThat(notificationService.countUnreadNotifications(3)).isEqualTo(2);

        notificationService.markAllAsRead(3);

        assertThat(notificationService.countUnreadNotifications(3)).isZero();
        assertThat(notificationService.getAll(3)).allMatch(notification -> notification.isRead());
    }
}
