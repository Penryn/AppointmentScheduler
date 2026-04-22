package com.example.slabiak.appointmentscheduler.service.impl;


import com.example.slabiak.appointmentscheduler.service.AppointmentService;
import com.example.slabiak.appointmentscheduler.service.InvoiceService;
import com.example.slabiak.appointmentscheduler.service.ScheduledTasksService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ScheduledTasksServiceImpl implements ScheduledTasksService {

    private final AppointmentService appointmentService;
    private final InvoiceService invoiceService;

    public ScheduledTasksServiceImpl(AppointmentService appointmentService, InvoiceService invoiceService) {
        this.appointmentService = appointmentService;
        this.invoiceService = invoiceService;
    }

    // runs every 30 minutes
    @Scheduled(fixedDelay = 30 * 60 * 1000)
    @Override
    public void updateAllAppointmentsStatuses() {
        log.debug("Starting scheduled appointment status update");
        try {
            appointmentService.updateAppointmentsStatusesWithExpiredExchangeRequest();
            appointmentService.updateAllAppointmentsStatuses();
        } catch (RuntimeException ex) {
            log.error("Scheduled appointment status update failed", ex);
            throw ex;
        }
    }

    // runs on the first day of each month
    @Scheduled(cron = "0 0 0 1 * ?")
    @Override
    public void issueInvoicesForCurrentMonth() {
        log.debug("Starting scheduled invoice issuing task");
        try {
            invoiceService.issueInvoicesForConfirmedAppointments();
        } catch (RuntimeException ex) {
            log.error("Scheduled invoice issuing task failed", ex);
            throw ex;
        }
    }


}
