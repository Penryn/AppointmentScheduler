package com.example.slabiak.appointmentscheduler.service.invoice;

import com.example.slabiak.appointmentscheduler.dao.AppointmentRepository;
import com.example.slabiak.appointmentscheduler.dao.InvoiceRepository;
import com.example.slabiak.appointmentscheduler.entity.Appointment;
import com.example.slabiak.appointmentscheduler.entity.AppointmentStatus;
import com.example.slabiak.appointmentscheduler.entity.Invoice;
import com.example.slabiak.appointmentscheduler.security.CustomUserDetails;
import com.example.slabiak.appointmentscheduler.service.InvoiceService;
import com.example.slabiak.appointmentscheduler.service.NotificationService;
import com.example.slabiak.appointmentscheduler.service.UserService;
import com.example.slabiak.appointmentscheduler.service.WorkService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@RunWith(SpringRunner.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("integration-test")
public class InvoiceServiceIT {

    @Autowired
    private InvoiceService invoiceService;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private UserService userService;

    @Autowired
    private WorkService workService;

    @Test
    @Transactional
    @WithUserDetails("admin")
    public void shouldIssueInvoiceForConfirmedAppointmentsAndMarkThemAsInvoiced() {
        Appointment confirmed = appointment(2032, 1, 15, 10, 0, 3);
        confirmed.setStatus(AppointmentStatus.CONFIRMED);
        appointmentRepository.saveAndFlush(confirmed);

        invoiceService.issueInvoicesForConfirmedAppointments();

        Appointment reloadedAppointment = appointmentRepository.findById(confirmed.getId()).orElseThrow();
        Invoice invoice = invoiceRepository.findByAppointmentId(confirmed.getId());

        assertThat(reloadedAppointment.getStatus()).isEqualTo(AppointmentStatus.INVOICED);
        assertThat(invoice).isNotNull();
        assertThat(invoice.getStatus()).isEqualTo("issued");
        assertThat(invoice.getNumber()).startsWith("FV/");
        assertThat(invoice.getTotalAmount()).isEqualTo(100.00);
        assertThat(notificationService.getAll(3))
                .anySatisfy(notification -> {
                    assertThat(notification.getTitle()).isEqualTo("新发票");
                    assertThat(notification.getUrl()).isEqualTo("/invoices/" + invoice.getId());
                });
    }

    @Test
    @Transactional
    @WithUserDetails("admin")
    public void shouldAllowAdminToMarkInvoiceAsPaid() {
        Invoice invoice = invoiceRepository.saveAndFlush(new Invoice("FV/test/1", "issued", LocalDateTime.now(), java.util.List.of(appointment(2032, 1, 16, 10, 0, 3))));

        invoiceService.changeInvoiceStatusToPaid(invoice.getId());

        assertThat(invoiceRepository.findById(invoice.getId()).orElseThrow().getStatus()).isEqualTo("paid");
    }

    @Test
    @Transactional
    @WithUserDetails("customer_r")
    public void shouldDenyCustomerFromMarkingInvoiceAsPaid() {
        Invoice invoice = invoiceRepository.saveAndFlush(new Invoice("FV/test/2", "issued", LocalDateTime.now(), java.util.List.of(appointment(2032, 1, 17, 10, 0, 3))));

        assertThatThrownBy(() -> invoiceService.changeInvoiceStatusToPaid(invoice.getId()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @Transactional
    @WithUserDetails("admin")
    public void shouldAllowAdminToDownloadAnyInvoice() {
        Invoice invoice = invoiceWithAppointment(3);

        boolean allowed = invoiceService.isUserAllowedToDownloadInvoice(user(1, "ROLE_ADMIN"), invoice);

        assertThat(allowed).isTrue();
    }

    @Test
    @Transactional
    @WithUserDetails("admin")
    public void shouldAllowInvoiceCustomerAndProviderToDownloadInvoice() {
        Invoice invoice = invoiceWithAppointment(3);

        assertThat(invoiceService.isUserAllowedToDownloadInvoice(user(3, "ROLE_CUSTOMER"), invoice)).isTrue();
        assertThat(invoiceService.isUserAllowedToDownloadInvoice(user(2, "ROLE_PROVIDER"), invoice)).isTrue();
    }

    @Test
    @Transactional
    @WithUserDetails("admin")
    public void shouldDenyUnrelatedUserFromDownloadingInvoice() {
        Invoice invoice = invoiceWithAppointment(3);

        boolean allowed = invoiceService.isUserAllowedToDownloadInvoice(user(1001, "ROLE_CUSTOMER"), invoice);

        assertThat(allowed).isFalse();
    }

    private Appointment appointment(int year, int month, int day, int hour, int minute, int customerId) {
        LocalDateTime start = LocalDateTime.of(year, month, day, hour, minute);
        Appointment appointment = new Appointment(
                start,
                start.plusHours(1),
                userService.getCustomerById(customerId),
                userService.getProviderById(2),
                workService.getWorkById(1)
        );
        appointment.setStatus(AppointmentStatus.SCHEDULED);
        return appointment;
    }

    private Invoice invoiceWithAppointment(int customerId) {
        return new Invoice("FV/test/download", "issued", LocalDateTime.now(), List.of(appointment(2032, 1, 18, 10, 0, customerId)));
    }

    private CustomUserDetails user(int userId, String role) {
        return new CustomUserDetails(
                userId,
                "Test",
                "User",
                "test" + userId,
                "test" + userId + "@example.com",
                "password",
                List.of(new SimpleGrantedAuthority(role)));
    }
}
