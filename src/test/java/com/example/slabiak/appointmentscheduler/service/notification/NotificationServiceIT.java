// 测试说明：验证通知服务在真实持久化环境中的未读计数和通知创建行为。
package com.example.slabiak.appointmentscheduler.service.notification;

import com.example.slabiak.appointmentscheduler.dao.AppointmentRepository;
import com.example.slabiak.appointmentscheduler.entity.Appointment;
import com.example.slabiak.appointmentscheduler.entity.AppointmentStatus;
import com.example.slabiak.appointmentscheduler.entity.ChatMessage;
import com.example.slabiak.appointmentscheduler.entity.Notification;
import com.example.slabiak.appointmentscheduler.service.EmailService;
import com.example.slabiak.appointmentscheduler.service.WorkService;
import com.example.slabiak.appointmentscheduler.service.NotificationService;
import com.example.slabiak.appointmentscheduler.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@SpringBootTest(properties = "mailing.enabled=true")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("integration-test")
public class NotificationServiceIT {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private UserService userService;

    @Autowired
    private WorkService workService;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @MockBean
    private EmailService emailService;

    @Test
    @Transactional
    @WithUserDetails("customer_r")
    public void shouldMarkAllNotificationsAsReadWithBulkUpdate() {
        notificationService.newNotification("n1", "m1", "/appointments/1", userService.getUserById(3));
        notificationService.newNotification("n2", "m2", "/appointments/1", userService.getUserById(3));

        // 检查点：验证该测试用例的预期结果。
        assertThat(notificationService.countUnreadNotifications(3)).isEqualTo(2);

        notificationService.markAllAsRead(3);

        assertThat(notificationService.countUnreadNotifications(3)).isZero();
        assertThat(notificationService.getAll(3)).allMatch(notification -> notification.isRead());
    }

    @Test
    @Transactional
    @WithUserDetails("customer_r")
    public void shouldNotMarkOtherUsersNotificationAsRead() {
        notificationService.newNotification("provider", "m", "/appointments/1", userService.getProviderById(2));
        Notification providerNotification = notificationService.getAll(2).get(0);

        // 检查点：验证该测试用例的预期结果。
        assertThatThrownBy(() -> notificationService.markAsRead(providerNotification.getId(), 3))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        assertThat(notificationService.getNotificationById(providerNotification.getId()).isRead()).isFalse();
    }

    @Test
    @Transactional
    @WithUserDetails("admin")
    public void shouldMarkAllAsReadOnlyForRequestedUser() {
        notificationService.newNotification("customer", "m", "/appointments/1", userService.getCustomerById(3));
        notificationService.newNotification("provider", "m", "/appointments/1", userService.getProviderById(2));

        notificationService.markAllAsRead(3);

        // 检查点：验证该测试用例的预期结果。
        assertThat(notificationService.countUnreadNotifications(3)).isZero();
        assertThat(notificationService.countUnreadNotifications(2)).isEqualTo(1);
    }

    @Test
    @Transactional
    @WithUserDetails("admin")
    public void shouldCreateProviderNotificationWhenAppointmentIsScheduled() {
        Appointment appointment = appointment();
        appointmentRepository.saveAndFlush(appointment);

        notificationService.newNewAppointmentScheduledNotification(appointment, true);

        // 检查点：验证该测试用例的预期结果。
        assertThat(notificationService.getAll(2))
                .anySatisfy(notification -> {
                    assertThat(notification.getTitle()).isEqualTo("新的预约");
                    assertThat(notification.getUrl()).isEqualTo("/appointments/" + appointment.getId());
                    // 检查点：验证该测试用例的预期结果。
                    assertThat(notification.isRead()).isFalse();
                });
    }

    @Test
    @Transactional
    @WithUserDetails("admin")
    public void shouldPersistScheduledAppointmentNotificationAndUseMockedEmailService() {
        Appointment appointment = appointment();
        appointmentRepository.saveAndFlush(appointment);

        notificationService.newNewAppointmentScheduledNotification(appointment, true);

        // 检查点：真实通知服务和数据库仍然会保存服务人员收到的新预约通知。
        assertThat(notificationService.getAll(2))
                .anySatisfy(notification -> {
                    assertThat(notification.getTitle()).isEqualTo("新的预约");
                    assertThat(notification.getUrl()).isEqualTo("/appointments/" + appointment.getId());
                });
        // 检查点：邮件组件假设不可用时，由 mock 接收新预约邮件发送调用。
        verify(emailService).sendNewAppointmentScheduledNotification(appointment);
    }

    @Test
    @Transactional
    @WithUserDetails("admin")
    public void shouldPersistNotificationAndUseMockedEmailServiceWhenMailingComponentIsUnavailable() {
        Appointment appointment = appointment();
        appointmentRepository.saveAndFlush(appointment);

        notificationService.newAppointmentCanceledByCustomerNotification(appointment, true);

        // 检查点：真实通知服务和数据库仍然会保存服务人员收到的取消通知。
        assertThat(notificationService.getAll(2))
                .anySatisfy(notification -> {
                    assertThat(notification.getTitle()).isEqualTo("预约已取消");
                    assertThat(notification.getUrl()).isEqualTo("/appointments/" + appointment.getId());
                });
        // 检查点：邮件组件假设不可用时，由 mock 接收发送调用，避免访问真实 SMTP。
        verify(emailService).sendAppointmentCanceledByCustomerNotification(appointment);
    }

    @Test
    @Transactional
    @WithUserDetails("admin")
    public void shouldCreateCustomerNotificationWhenProviderCancelsAppointment() {
        Appointment appointment = appointment();
        appointmentRepository.saveAndFlush(appointment);

        notificationService.newAppointmentCanceledByProviderNotification(appointment, true);

        // 检查点：验证该测试用例的预期结果。
        assertThat(notificationService.getAll(3))
                .anySatisfy(notification -> {
                    assertThat(notification.getTitle()).isEqualTo("预约已取消");
                    assertThat(notification.getUrl()).isEqualTo("/appointments/" + appointment.getId());
                    // 检查点：验证该测试用例的预期结果。
                    assertThat(notification.isRead()).isFalse();
                });
    }

    @Test
    @Transactional
    @WithUserDetails("admin")
    public void shouldSkipMockedEmailServiceWhenSendEmailFlagIsFalse() {
        Appointment appointment = appointment();
        appointmentRepository.saveAndFlush(appointment);

        notificationService.newAppointmentCanceledByProviderNotification(appointment, false);

        // 检查点：即使不发送邮件，客户仍然会收到数据库中的取消通知。
        assertThat(notificationService.getAll(3))
                .anySatisfy(notification -> {
                    assertThat(notification.getTitle()).isEqualTo("预约已取消");
                    assertThat(notification.getUrl()).isEqualTo("/appointments/" + appointment.getId());
                });
        // 检查点：sendEmail 为 false 时，mock 邮件服务不应收到发送调用。
        verify(emailService, never()).sendAppointmentCanceledByProviderNotification(appointment);
    }

    @Test
    @Transactional
    @WithUserDetails("admin")
    public void shouldSendChatMessageNotificationToOtherAppointmentParty() {
        Appointment appointment = appointment();
        appointmentRepository.saveAndFlush(appointment);
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setAuthor(appointment.getCustomer());
        chatMessage.setAppointment(appointment);
        chatMessage.setCreatedAt(java.time.LocalDateTime.now());

        notificationService.newChatMessageNotification(chatMessage, true);

        // 检查点：验证该测试用例的预期结果。
        assertThat(notificationService.getAll(2))
                .anySatisfy(notification -> {
                    assertThat(notification.getTitle()).isEqualTo("新的预约消息");
                    assertThat(notification.getUrl()).isEqualTo("/appointments/" + appointment.getId());
                });
        // 检查点：验证该测试用例的预期结果。
        assertThat(notificationService.getAll(3))
                .noneMatch(notification -> notification.getTitle().equals("新的预约消息")
                        && notification.getUrl().equals("/appointments/" + appointment.getId()));
    }

    @Test
    @Transactional
    @WithUserDetails("admin")
    public void shouldPersistChatMessageNotificationAndUseMockedEmailService() {
        Appointment appointment = appointment();
        appointmentRepository.saveAndFlush(appointment);
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setAuthor(appointment.getProvider());
        chatMessage.setAppointment(appointment);
        chatMessage.setCreatedAt(java.time.LocalDateTime.now());

        notificationService.newChatMessageNotification(chatMessage, true);

        // 检查点：真实通知服务和数据库仍然会把聊天消息通知发给另一方客户。
        assertThat(notificationService.getAll(3))
                .anySatisfy(notification -> {
                    assertThat(notification.getTitle()).isEqualTo("新的预约消息");
                    assertThat(notification.getUrl()).isEqualTo("/appointments/" + appointment.getId());
                });
        // 检查点：邮件组件假设不可用时，由 mock 接收聊天消息邮件发送调用。
        verify(emailService).sendNewChatMessageNotification(chatMessage);
    }

    private Appointment appointment() {
        Appointment appointment = new Appointment(
                java.time.LocalDateTime.of(2031, 2, 1, 10, 0),
                java.time.LocalDateTime.of(2031, 2, 1, 11, 0),
                userService.getCustomerById(3),
                userService.getProviderById(2),
                workService.getWorkById(1)
        );
        appointment.setStatus(AppointmentStatus.SCHEDULED);
        return appointment;
    }
}
