package com.example.slabiak.appointmentscheduler.service;

import com.example.slabiak.appointmentscheduler.entity.Invoice;
import com.example.slabiak.appointmentscheduler.model.InvoiceListItem;
import com.example.slabiak.appointmentscheduler.security.CustomUserDetails;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.io.File;
import java.util.List;

public interface InvoiceService {
    void createNewInvoice(Invoice invoice);

    Invoice getInvoiceByAppointmentId(int appointmentId);

    Invoice getInvoiceById(int invoiceId);

    List<Invoice> getAllInvoices();

    Page<InvoiceListItem> getInvoiceList(Pageable pageable);

    void changeInvoiceStatusToPaid(int invoiceId);

    void issueInvoicesForConfirmedAppointments();

    String generateInvoiceNumber();

    File generatePdfForInvoice(int invoiceId);

    boolean isUserAllowedToDownloadInvoice(CustomUserDetails user, Invoice invoice);
}
