// 测试说明：验证通知服务的查询、已读处理和不同业务通知生成行为。
package com.example.slabiak.appointmentscheduler.service.notification;

import com.example.slabiak.appointmentscheduler.dao.NotificationRepository;
import com.example.slabiak.appointmentscheduler.entity.Appointment;
import com.example.slabiak.appointmentscheduler.entity.ChatMessage;
import com.example.slabiak.appointmentscheduler.entity.ExchangeRequest;
import com.example.slabiak.appointmentscheduler.entity.ExchangeStatus;
import com.example.slabiak.appointmentscheduler.entity.Invoice;
import com.example.slabiak.appointmentscheduler.entity.Notification;
import com.example.slabiak.appointmentscheduler.entity.Work;
import com.example.slabiak.appointmentscheduler.entity.user.customer.RetailCustomer;
import com.example.slabiak.appointmentscheduler.entity.user.provider.Provider;
import com.example.slabiak.appointmentscheduler.service.EmailService;
import com.example.slabiak.appointmentscheduler.service.impl.NotificationServiceImpl;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private EmailService emailService;

    @Test
    void shouldCreateGenericNotification() {
        NotificationServiceImpl service = serviceWithMailing(false);
        RetailCustomer user = customer(3, "Ada", "Lovelace");

        service.newNotification("Title", "Message", "/target", user);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        // 检查点：验证该测试用例的预期结果。
        verify(notificationRepository).save(captor.capture());
        Notification saved = captor.getValue();
        assertThat(saved.getTitle()).isEqualTo("Title");
        assertThat(saved.getMessage()).isEqualTo("Message");
        // 检查点：验证该测试用例的预期结果。
        assertThat(saved.getUrl()).isEqualTo("/target");
        assertThat(saved.getUser()).isSameAs(user);
        assertThat(saved.isRead()).isFalse();
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    void shouldMarkOwnNotificationAsRead() {
        NotificationServiceImpl service = serviceWithMailing(false);
        Notification notification = notificationFor(customer(3, "Ada", "Lovelace"));
        when(notificationRepository.findById(11)).thenReturn(Optional.of(notification));

        service.markAsRead(11, 3);

        // 检查点：验证该测试用例的预期结果。
        assertThat(notification.isRead()).isTrue();
        verify(notificationRepository).save(notification);
    }

    @Test
    void shouldRejectMarkingAnotherUsersNotificationAsRead() {
        NotificationServiceImpl service = serviceWithMailing(false);
        Notification notification = notificationFor(customer(4, "Grace", "Hopper"));
        when(notificationRepository.findById(11)).thenReturn(Optional.of(notification));

        // 检查点：验证该测试用例的预期结果。
        assertThatThrownBy(() -> service.markAsRead(11, 3))
                .isInstanceOf(AccessDeniedException.class);

        assertThat(notification.isRead()).isFalse();
        verify(notificationRepository, never()).save(notification);
    }

    @Test
    void shouldThrowWhenNotificationDoesNotExist() {
        NotificationServiceImpl service = serviceWithMailing(false);
        when(notificationRepository.findById(99)).thenReturn(Optional.empty());

        // 检查点：验证该测试用例的预期结果。
        assertThatThrownBy(() -> service.getNotificationById(99))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Notification not found");
    }

    @Test
    void shouldDelegateReadQueriesToRepository() {
        NotificationServiceImpl service = serviceWithMailing(false);
        PageRequest pageable = PageRequest.of(0, 10);
        List<Notification> notifications = List.of(notificationFor(customer(3, "Ada", "Lovelace")));
        when(notificationRepository.findAllByUserIdOrderByCreatedAtDesc(3)).thenReturn(notifications);
        when(notificationRepository.findPageByUserId(3, pageable)).thenReturn(new PageImpl<>(notifications));
        when(notificationRepository.getAllUnreadNotifications(3)).thenReturn(notifications);
        when(notificationRepository.countUnreadByUserId(3)).thenReturn(2L);

        // 检查点：验证该测试用例的预期结果。
        assertThat(service.getAll(3)).isSameAs(notifications);
        assertThat(service.getAll(3, pageable).getContent()).isEqualTo(notifications);
        assertThat(service.getUnreadNotifications(3)).isSameAs(notifications);
        assertThat(service.countUnreadNotifications(3)).isEqualTo(2);

        // 检查点：验证该测试用例的预期结果。
        verify(notificationRepository).findAllByUserIdOrderByCreatedAtDesc(3);
        verify(notificationRepository).findPageByUserId(3, pageable);
        verify(notificationRepository).getAllUnreadNotifications(3);
        verify(notificationRepository).countUnreadByUserId(3);
    }

    @Test
    void shouldMarkAllNotificationsAsReadThroughBulkRepositoryUpdate() {
        NotificationServiceImpl service = serviceWithMailing(false);

        service.markAllAsRead(3);

        // 检查点：验证该测试用例的预期结果。
        verify(notificationRepository).markAllAsReadByUserId(3);
    }

    @Test
    void shouldCreateAppointmentNotificationsAndSendEmailsWhenEnabled() {
        NotificationServiceImpl service = serviceWithMailing(true);
        Appointment appointment = appointment(21);

        service.newAppointmentFinishedNotification(appointment, true);
        service.newAppointmentRejectionRequestedNotification(appointment, true);
        service.newNewAppointmentScheduledNotification(appointment, true);
        service.newAppointmentCanceledByCustomerNotification(appointment, true);
        service.newAppointmentCanceledByProviderNotification(appointment, true);
        service.newAppointmentRejectionAcceptedNotification(appointment, true);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        // 检查点：验证该测试用例的预期结果。
        verify(notificationRepository, org.mockito.Mockito.times(6)).save(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(Notification::getTitle)
                .containsExactly(
                        "预约已完成",
                        "预约申诉待确认",
                        "新的预约",
                        "预约已取消",
                        "预约已取消",
                        "申诉已确认"
                );
        // 检查点：验证该测试用例的预期结果。
        assertThat(captor.getAllValues()).allSatisfy(notification ->
                assertThat(notification.getUrl()).isEqualTo("/appointments/21"));
        verify(emailService).sendAppointmentFinishedNotification(appointment);
        verify(emailService).sendAppointmentRejectionRequestedNotification(appointment);
        // 检查点：验证该测试用例的预期结果。
        verify(emailService).sendNewAppointmentScheduledNotification(appointment);
        verify(emailService).sendAppointmentCanceledByCustomerNotification(appointment);
        verify(emailService).sendAppointmentCanceledByProviderNotification(appointment);
        verify(emailService).sendAppointmentRejectionAcceptedNotification(appointment);
    }

    @Test
    void shouldCreateInvoiceAndExchangeNotificationsAndSendEmailsWhenEnabled() {
        NotificationServiceImpl service = serviceWithMailing(true);
        Appointment requestor = appointment(31);
        Appointment requested = appointment(32);
        ExchangeRequest exchangeRequest = new ExchangeRequest(requestor, requested, ExchangeStatus.PENDING);
        Invoice invoice = new Invoice();
        invoice.setId(41);
        invoice.setAppointments(List.of(requestor));

        service.newInvoice(invoice, true);
        service.newExchangeRequestedNotification(requestor, requested, true);
        service.newExchangeAcceptedNotification(exchangeRequest, true);
        service.newExchangeRejectedNotification(exchangeRequest, true);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        // 检查点：验证该测试用例的预期结果。
        verify(notificationRepository, org.mockito.Mockito.times(4)).save(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(Notification::getTitle)
                .containsExactly("新发票", "换约请求", "换约请求已接受", "换约请求已拒绝");
        // 检查点：验证该测试用例的预期结果。
        verify(emailService).sendInvoice(invoice);
        verify(emailService).sendNewExchangeRequestedNotification(requestor, requested);
        verify(emailService).sendExchangeRequestAcceptedNotification(exchangeRequest);
        verify(emailService).sendExchangeRequestRejectedNotification(exchangeRequest);
    }

    @Test
    void shouldCreateChatNotificationForOtherAppointmentParty() {
        NotificationServiceImpl service = serviceWithMailing(true);
        Appointment appointment = appointment(21);
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setAppointment(appointment);
        chatMessage.setAuthor(appointment.getCustomer());

        service.newChatMessageNotification(chatMessage, true);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        // 检查点：验证该测试用例的预期结果。
        verify(notificationRepository).save(captor.capture());
        assertThat(captor.getValue().getTitle()).isEqualTo("新的预约消息");
        assertThat(captor.getValue().getUrl()).isEqualTo("/appointments/21");
        assertThat(captor.getValue().getUser()).isSameAs(appointment.getProvider());
        // 检查点：验证该测试用例的预期结果。
        verify(emailService).sendNewChatMessageNotification(chatMessage);
    }

    @Test
    void shouldNotSendEmailsWhenMailingIsDisabledOrSendEmailFlagIsFalse() {
        NotificationServiceImpl service = serviceWithMailing(false);
        Appointment appointment = appointment(21);

        service.newAppointmentFinishedNotification(appointment, true);
        service.newNewAppointmentScheduledNotification(appointment, false);

        // 检查点：验证该测试用例的预期结果。
        verify(notificationRepository, org.mockito.Mockito.times(2)).save(org.mockito.Mockito.any(Notification.class));
        verifyNoInteractions(emailService);
    }

    private NotificationServiceImpl serviceWithMailing(boolean mailingEnabled) {
        return new NotificationServiceImpl(mailingEnabled, notificationRepository, emailService);
    }

    private Notification notificationFor(RetailCustomer user) {
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setRead(false);
        return notification;
    }

    private Appointment appointment(int id) {
        Work work = new Work();
        work.setId(7);
        work.setName("Consulting");
        work.setPrice(100);

        Appointment appointment = new Appointment(
                LocalDateTime.of(2031, 1, 1, 10, 0),
                LocalDateTime.of(2031, 1, 1, 11, 0),
                customer(3, "Ada", "Lovelace"),
                provider(2, "Alan", "Turing"),
                work
        );
        appointment.setId(id);
        return appointment;
    }

    private RetailCustomer customer(int id, String firstName, String lastName) {
        RetailCustomer customer = new RetailCustomer();
        customer.setId(id);
        customer.setFirstName(firstName);
        customer.setLastName(lastName);
        return customer;
    }

    private Provider provider(int id, String firstName, String lastName) {
        Provider provider = new Provider();
        provider.setId(id);
        provider.setFirstName(firstName);
        provider.setLastName(lastName);
        return provider;
    }
}
