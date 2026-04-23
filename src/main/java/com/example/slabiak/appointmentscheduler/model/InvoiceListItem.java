package com.example.slabiak.appointmentscheduler.model;

import java.time.LocalDateTime;

public record InvoiceListItem(
        Integer id,
        String number,
        LocalDateTime issued,
        String status,
        double totalAmount,
        String customerName
) {
}
