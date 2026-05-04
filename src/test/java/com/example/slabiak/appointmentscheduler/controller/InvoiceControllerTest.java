package com.example.slabiak.appointmentscheduler.controller;

import com.example.slabiak.appointmentscheduler.service.InvoiceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import java.io.File;
import java.io.FileNotFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvoiceControllerTest {

    @Mock
    private InvoiceService invoiceService;

    private InvoiceController controller;

    @BeforeEach
    void setUp() {
        controller = new InvoiceController(invoiceService);
    }

    @Test
    void shouldShowAllInvoicesAndChangeStatus() {
        PageRequest pageable = PageRequest.of(0, 20);
        when(invoiceService.getInvoiceList(pageable)).thenReturn(Page.empty(pageable));
        Model model = new ExtendedModelMap();

        assertThat(controller.showAllInvoices(model, pageable)).isEqualTo("invoices/listInvoices");
        assertThat(model.getAttribute("invoices")).isEqualTo(Page.empty(pageable));
        assertThat(controller.changeStatusToPaid(7)).isEqualTo("redirect:/invoices/all");
        assertThat(controller.issueInvoicesManually(new ExtendedModelMap())).isEqualTo("redirect:/invoices/all");

        verify(invoiceService).changeInvoiceStatusToPaid(7);
        verify(invoiceService).issueInvoicesForConfirmedAppointments();
    }

    @Test
    void shouldDownloadInvoicePdf() throws Exception {
        File pdf = File.createTempFile("invoice-", ".pdf");
        pdf.deleteOnExit();
        when(invoiceService.generatePdfForInvoice(7)).thenReturn(pdf);

        ResponseEntity<InputStreamResource> response = controller.downloadInvoice(7, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentType().toString()).isEqualTo("application/pdf");
        assertThat(response.getHeaders().getContentLength()).isEqualTo(pdf.length());
        assertThat(response.getHeaders().getContentDisposition().getFilename()).isEqualTo(pdf.getName());
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    void shouldReturnServerErrorWhenInvoicePdfCannotBeOpened() throws Exception {
        File missing = new File("target/missing-invoice.pdf");
        when(invoiceService.generatePdfForInvoice(7)).thenReturn(missing);

        ResponseEntity<InputStreamResource> response = controller.downloadInvoice(7, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
