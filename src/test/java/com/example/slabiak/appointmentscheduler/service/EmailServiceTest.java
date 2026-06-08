// 测试说明：验证邮件服务的模板渲染、发送调用和异常处理行为。
package com.example.slabiak.appointmentscheduler.service;

import com.example.slabiak.appointmentscheduler.entity.Appointment;
import com.example.slabiak.appointmentscheduler.entity.ChatMessage;
import com.example.slabiak.appointmentscheduler.entity.ExchangeRequest;
import com.example.slabiak.appointmentscheduler.entity.Invoice;
import com.example.slabiak.appointmentscheduler.entity.Work;
import com.example.slabiak.appointmentscheduler.entity.user.customer.Customer;
import com.example.slabiak.appointmentscheduler.entity.user.provider.Provider;
import com.example.slabiak.appointmentscheduler.service.impl.EmailServiceImpl;
import com.example.slabiak.appointmentscheduler.service.impl.JwtTokenServiceImpl;
import com.example.slabiak.appointmentscheduler.util.PdfGeneratorUtil;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.io.File;
import java.util.List;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class EmailServiceTest {

    @Mock
    private JavaMailSender javaMailSender;

    @Mock
    private SpringTemplateEngine templateEngine;

    @Mock
    private JwtTokenServiceImpl jwtTokenService;

    @Mock
    private PdfGeneratorUtil pdfGeneratorUtil;

    private EmailServiceImpl emailService;

    @BeforeEach
    public void setUp() {
        emailService = new EmailServiceImpl(javaMailSender, templateEngine, jwtTokenService, pdfGeneratorUtil, "http://localhost:8080");
    }

    @Test
    public void shouldRenderTemplateAndSendMimeMessageWithoutAttachment() {
        MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
        when(javaMailSender.createMimeMessage()).thenReturn(message);
        when(templateEngine.process(eq("email/newAppointmentScheduled"), any(Context.class))).thenReturn("<html>ok</html>");

        emailService.sendEmail("provider@example.com", "新的预约", "newAppointmentScheduled", new Context(), null);

        // 检查点：验证该测试用例的预期结果。
        verify(javaMailSender).send(message);
    }

    @Test
    public void shouldSendAppointmentLifecycleNotificationsWithExpectedRecipientsAndTemplates() {
        EmailServiceImpl spyService = spy(emailService);
        doNothing().when(spyService).sendEmail(anyString(), anyString(), anyString(), any(Context.class), any());

        Appointment appointment = appointment();
        when(jwtTokenService.generateAppointmentRejectionToken(appointment)).thenReturn("reject-token");
        when(jwtTokenService.generateAcceptRejectionToken(appointment)).thenReturn("accept-token");

        spyService.sendAppointmentFinishedNotification(appointment);
        spyService.sendAppointmentRejectionRequestedNotification(appointment);
        spyService.sendNewAppointmentScheduledNotification(appointment);
        spyService.sendAppointmentCanceledByCustomerNotification(appointment);
        spyService.sendAppointmentCanceledByProviderNotification(appointment);
        spyService.sendAppointmentRejectionAcceptedNotification(appointment);

        // 检查点：验证该测试用例的预期结果。
        verify(spyService).sendEmail(eq("customer@example.com"), eq("预约完成摘要"), eq("appointmentFinished"), any(Context.class), eq(null));
        verify(spyService).sendEmail(eq("provider@example.com"), eq("预约申诉待确认"), eq("appointmentRejectionRequested"), any(Context.class), eq(null));
        verify(spyService).sendEmail(eq("provider@example.com"), eq("新的预约"), eq("newAppointmentScheduled"), any(Context.class), eq(null));
        verify(spyService).sendEmail(eq("provider@example.com"), eq("客户取消了预约"), eq("appointmentCanceled"), any(Context.class), eq(null));
        // 检查点：验证该测试用例的预期结果。
        verify(spyService).sendEmail(eq("customer@example.com"), eq("服务人员取消了预约"), eq("appointmentCanceled"), any(Context.class), eq(null));
        verify(spyService).sendEmail(eq("customer@example.com"), eq("预约申诉已确认"), eq("appointmentRejectionAccepted"), any(Context.class), eq(null));
    }

    @Test
    public void shouldSendInvoiceWithGeneratedPdfAttachment() throws Exception {
        EmailServiceImpl spyService = spy(emailService);
        doNothing().when(spyService).sendEmail(anyString(), anyString(), anyString(), any(Context.class), any());
        Invoice invoice = new Invoice();
        invoice.setAppointments(List.of(appointment()));
        File invoicePdf = File.createTempFile("invoice", ".pdf");
        doReturn(invoicePdf).when(pdfGeneratorUtil).generatePdfFromInvoice(invoice);

        spyService.sendInvoice(invoice);

        // 检查点：验证该测试用例的预期结果。
        verify(spyService).sendEmail(eq("customer@example.com"), eq("预约发票"), eq("appointmentInvoice"), any(Context.class), eq(invoicePdf));
    }

    @Test
    public void shouldNotSendInvoiceWhenPdfGenerationFails() throws Exception {
        EmailServiceImpl spyService = spy(emailService);
        Invoice invoice = new Invoice();
        invoice.setAppointments(List.of(appointment()));
        doThrow(new RuntimeException("pdf error")).when(pdfGeneratorUtil).generatePdfFromInvoice(invoice);

        spyService.sendInvoice(invoice);

        // 检查点：验证该测试用例的预期结果。
        verify(spyService, never()).sendEmail(anyString(), anyString(), anyString(), any(Context.class), any());
    }

    @Test
    public void shouldSendChatAndExchangeNotificationsToExpectedRecipients() {
        EmailServiceImpl spyService = spy(emailService);
        doNothing().when(spyService).sendEmail(anyString(), anyString(), anyString(), any(Context.class), any());
        Appointment appointment = appointment();
        ChatMessage providerMessage = new ChatMessage();
        providerMessage.setAuthor(appointment.getProvider());
        providerMessage.setAppointment(appointment);

        Appointment newAppointment = appointment();
        newAppointment.setId(99);
        ExchangeRequest exchangeRequest = new ExchangeRequest();
        exchangeRequest.setRequested(appointment);
        exchangeRequest.setRequestor(newAppointment);

        spyService.sendNewChatMessageNotification(providerMessage);
        spyService.sendNewExchangeRequestedNotification(appointment, newAppointment);
        spyService.sendExchangeRequestAcceptedNotification(exchangeRequest);
        spyService.sendExchangeRequestRejectedNotification(exchangeRequest);

        // 检查点：验证该测试用例的预期结果。
        verify(spyService).sendEmail(eq("customer@example.com"), eq("新的预约消息"), eq("newChatMessage"), any(Context.class), eq(null));
        verify(spyService).sendEmail(eq("customer@example.com"), eq("新的换约请求"), eq("newExchangeRequest"), any(Context.class), eq(null));
        verify(spyService).sendEmail(eq("customer@example.com"), eq("换约请求已接受"), eq("exchangeRequestAccepted"), any(Context.class), eq(null));
        verify(spyService).sendEmail(eq("customer@example.com"), eq("换约请求已拒绝"), eq("exchangeRequestRejected"), any(Context.class), eq(null));
    }

    @Test
    public void shouldUseCustomerAsChatRecipientWhenProviderWritesMessage() {
        EmailServiceImpl spyService = spy(emailService);
        doNothing().when(spyService).sendEmail(anyString(), anyString(), anyString(), any(Context.class), any());
        Appointment appointment = appointment();
        ChatMessage message = new ChatMessage();
        message.setAuthor(appointment.getProvider());
        message.setAppointment(appointment);

        spyService.sendNewChatMessageNotification(message);

        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
        // 检查点：验证该测试用例的预期结果。
        verify(spyService).sendEmail(eq("customer@example.com"), eq("新的预约消息"), eq("newChatMessage"), contextCaptor.capture(), eq(null));
        assertThat(contextCaptor.getValue().getVariable("recipent")).isEqualTo(appointment.getCustomer());
    }

    private Appointment appointment() {
        Customer customer = new Customer();
        customer.setId(3);
        customer.setFirstName("Customer");
        customer.setLastName("One");
        customer.setEmail("customer@example.com");

        Provider provider = new Provider();
        provider.setId(2);
        provider.setFirstName("Provider");
        provider.setLastName("One");
        provider.setEmail("provider@example.com");

        Work work = new Work();
        work.setName("English lesson");

        Appointment appointment = new Appointment();
        appointment.setId(10);
        appointment.setCustomer(customer);
        appointment.setProvider(provider);
        appointment.setWork(work);
        return appointment;
    }
}
