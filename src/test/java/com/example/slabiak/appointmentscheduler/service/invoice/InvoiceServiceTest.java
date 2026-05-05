package com.example.slabiak.appointmentscheduler.service.invoice;

import com.example.slabiak.appointmentscheduler.dao.InvoiceRepository;
import com.example.slabiak.appointmentscheduler.entity.Appointment;
import com.example.slabiak.appointmentscheduler.entity.AppointmentStatus;
import com.example.slabiak.appointmentscheduler.entity.Invoice;
import com.example.slabiak.appointmentscheduler.entity.Work;
import com.example.slabiak.appointmentscheduler.entity.user.customer.Customer;
import com.example.slabiak.appointmentscheduler.entity.user.provider.Provider;
import com.example.slabiak.appointmentscheduler.model.InvoiceListItem;
import com.example.slabiak.appointmentscheduler.security.CustomUserDetails;
import com.example.slabiak.appointmentscheduler.service.AppointmentService;
import com.example.slabiak.appointmentscheduler.service.NotificationService;
import com.example.slabiak.appointmentscheduler.service.UserService;
import com.example.slabiak.appointmentscheduler.service.impl.InvoiceServiceImpl;
import com.example.slabiak.appointmentscheduler.util.PdfGeneratorUtil;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvoiceServiceTest {

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private PdfGeneratorUtil pdfGeneratorUtil;

    @Mock
    private UserService userService;

    @Mock
    private AppointmentService appointmentService;

    @Mock
    private NotificationService notificationService;

    private InvoiceServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new InvoiceServiceImpl(invoiceRepository, pdfGeneratorUtil, userService, appointmentService, notificationService);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldGenerateInvoiceNumberFromCurrentMonthCount() {
        when(invoiceRepository.findAllIssuedInCurrentMonth(any(LocalDateTime.class)))
                .thenReturn(List.of(new Invoice(), new Invoice()));

        String number = service.generateInvoiceNumber();

        assertThat(number).matches("FV/\\d{4}/\\d{1,2}/3");
    }

    @Test
    void shouldDelegateBasicInvoiceRepositoryOperations() {
        PageRequest pageable = PageRequest.of(0, 10);
        Invoice invoice = invoice(11, appointment(3, 2));
        Page<InvoiceListItem> page = new PageImpl<>(List.of(new InvoiceListItem(11, "FV/1", LocalDateTime.now(), "issued", 100, "Ada Lovelace")));
        when(invoiceRepository.findByAppointmentId(7)).thenReturn(invoice);
        when(invoiceRepository.findById(11)).thenReturn(Optional.of(invoice));
        when(invoiceRepository.findAll()).thenReturn(List.of(invoice));
        when(invoiceRepository.findListPage(pageable)).thenReturn(page);

        service.createNewInvoice(invoice);

        assertThat(service.getInvoiceByAppointmentId(7)).isSameAs(invoice);
        assertThat(service.getInvoiceById(11)).isSameAs(invoice);
        assertThat(service.getAllInvoices()).containsExactly(invoice);
        assertThat(service.getInvoiceList(pageable)).isSameAs(page);
        verify(invoiceRepository).save(invoice);
    }

    @Test
    void shouldThrowWhenInvoiceDoesNotExist() {
        when(invoiceRepository.findById(99)).thenReturn(Optional.empty());
        authenticate(user(1, "ROLE_ADMIN"));

        assertThatThrownBy(() -> service.generatePdfForInvoice(99))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Invoice not found");
        assertThatThrownBy(() -> service.changeInvoiceStatusToPaid(99))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Invoice not found");
    }

    @Test
    void shouldChangeInvoiceStatusToPaid() {
        Invoice invoice = invoice(11, appointment(3, 2));
        when(invoiceRepository.findById(11)).thenReturn(Optional.of(invoice));

        service.changeInvoiceStatusToPaid(11);

        assertThat(invoice.getStatus()).isEqualTo("paid");
        verify(invoiceRepository).save(invoice);
    }

    @Test
    void shouldGeneratePdfOnlyForAllowedUsers() throws Exception {
        Invoice invoice = invoice(11, appointment(3, 2));
        File pdf = File.createTempFile("invoice-service-", ".pdf");
        pdf.deleteOnExit();
        when(invoiceRepository.findById(11)).thenReturn(Optional.of(invoice));
        when(pdfGeneratorUtil.generatePdfFromInvoice(invoice)).thenReturn(pdf);
        authenticate(user(3, "ROLE_CUSTOMER"));

        assertThat(service.generatePdfForInvoice(11)).isSameAs(pdf);

        authenticate(user(4, "ROLE_CUSTOMER"));
        assertThatThrownBy(() -> service.generatePdfForInvoice(11))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void shouldEvaluateInvoiceDownloadAuthorization() {
        Invoice invoice = invoice(11, appointment(3, 2));

        assertThat(service.isUserAllowedToDownloadInvoice(user(1, "ROLE_ADMIN"), invoice)).isTrue();
        assertThat(service.isUserAllowedToDownloadInvoice(user(3, "ROLE_CUSTOMER"), invoice)).isTrue();
        assertThat(service.isUserAllowedToDownloadInvoice(user(2, "ROLE_PROVIDER"), invoice)).isTrue();
        assertThat(service.isUserAllowedToDownloadInvoice(user(4, "ROLE_CUSTOMER"), invoice)).isFalse();
    }

    @Test
    void shouldIssueInvoicesOnlyForCustomersWithConfirmedAppointments() {
        Customer customerWithAppointments = customer(3);
        Customer emptyCustomer = customer(4);
        Appointment confirmed = appointment(3, 2);
        confirmed.setStatus(AppointmentStatus.CONFIRMED);
        when(userService.getAllCustomers()).thenReturn(List.of(customerWithAppointments, emptyCustomer));
        when(appointmentService.getConfirmedAppointmentsByCustomerId(3)).thenReturn(List.of(confirmed));
        when(appointmentService.getConfirmedAppointmentsByCustomerId(4)).thenReturn(List.of());
        when(invoiceRepository.findAllIssuedInCurrentMonth(any(LocalDateTime.class))).thenReturn(List.of());

        service.issueInvoicesForConfirmedAppointments();

        assertThat(confirmed.getStatus()).isEqualTo(AppointmentStatus.INVOICED);
        verify(appointmentService).updateAppointment(confirmed);
        ArgumentCaptor<Invoice> captor = ArgumentCaptor.forClass(Invoice.class);
        verify(invoiceRepository).save(captor.capture());
        Invoice saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo("issued");
        assertThat(saved.getAppointments()).containsExactly(confirmed);
        assertThat(saved.getTotalAmount()).isEqualTo(100);
        verify(notificationService).newInvoice(saved, true);
        verify(notificationService, never()).newInvoice(any(), org.mockito.ArgumentMatchers.eq(false));
    }

    private Invoice invoice(int id, Appointment appointment) {
        Invoice invoice = new Invoice("FV/test", "issued", LocalDateTime.now(), List.of(appointment));
        invoice.setId(id);
        return invoice;
    }

    private Appointment appointment(int customerId, int providerId) {
        Work work = new Work();
        work.setId(1);
        work.setPrice(100);
        Customer customer = customer(customerId);
        Provider provider = new Provider();
        provider.setId(providerId);
        Appointment appointment = new Appointment(LocalDateTime.now(), LocalDateTime.now().plusHours(1), customer, provider, work);
        appointment.setId(7);
        return appointment;
    }

    private Customer customer(int id) {
        Customer customer = new Customer();
        customer.setId(id);
        return customer;
    }

    private CustomUserDetails user(int id, String role) {
        return new CustomUserDetails(
                id,
                "First",
                "Last",
                "user" + id,
                "user" + id + "@example.com",
                "password",
                List.of(new SimpleGrantedAuthority(role))
        );
    }

    private void authenticate(CustomUserDetails user) {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(user, null));
    }
}
