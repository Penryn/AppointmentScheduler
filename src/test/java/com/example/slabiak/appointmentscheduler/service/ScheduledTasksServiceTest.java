// 测试说明：验证定时任务服务会触发预约状态更新。
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

        // 检查点：验证该测试用例的预期结果。
        verify(appointmentService).updateAppointmentsStatusesWithExpiredExchangeRequest();
        verify(appointmentService).updateAllAppointmentsStatuses();
    }

    @Test
    void shouldRethrowAppointmentStatusUpdateFailures() {
        RuntimeException failure = new RuntimeException("status failure");
        doThrow(failure).when(appointmentService).updateAppointmentsStatusesWithExpiredExchangeRequest();

        // 检查点：验证该测试用例的预期结果。
        assertThatThrownBy(() -> service.updateAllAppointmentsStatuses()).isSameAs(failure);
    }

    @Test
    void shouldIssueInvoicesAndRethrowFailures() {
        service.issueInvoicesForCurrentMonth();
        // 检查点：验证该测试用例的预期结果。
        verify(invoiceService).issueInvoicesForConfirmedAppointments();

        RuntimeException failure = new RuntimeException("invoice failure");
        doThrow(failure).when(invoiceService).issueInvoicesForConfirmedAppointments();
        assertThatThrownBy(() -> service.issueInvoicesForCurrentMonth()).isSameAs(failure);
    }
}
