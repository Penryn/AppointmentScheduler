package com.example.slabiak.appointmentscheduler.service;

import com.example.slabiak.appointmentscheduler.service.impl.ScheduledTasksServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ScheduledTasksServiceTest {

    @Mock
    private AppointmentService appointmentService;

    @Mock
    private InvoiceService invoiceService;

    private ScheduledTasksServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ScheduledTasksServiceImpl(appointmentService, invoiceService);
    }

    @Test
    void shouldUpdateExpiredExchangesAndAppointmentStatuses() {
        service.updateAllAppointmentsStatuses();

        verify(appointmentService).updateAppointmentsStatusesWithExpiredExchangeRequest();
        verify(appointmentService).updateAllAppointmentsStatuses();
    }

    @Test
    void shouldRethrowAppointmentStatusUpdateFailures() {
        RuntimeException failure = new RuntimeException("status failure");
        doThrow(failure).when(appointmentService).updateAppointmentsStatusesWithExpiredExchangeRequest();

        assertThatThrownBy(() -> service.updateAllAppointmentsStatuses()).isSameAs(failure);
    }

    @Test
    void shouldIssueInvoicesAndRethrowFailures() {
        service.issueInvoicesForCurrentMonth();
        verify(invoiceService).issueInvoicesForConfirmedAppointments();

        RuntimeException failure = new RuntimeException("invoice failure");
        doThrow(failure).when(invoiceService).issueInvoicesForConfirmedAppointments();
        assertThatThrownBy(() -> service.issueInvoicesForCurrentMonth()).isSameAs(failure);
    }
}
