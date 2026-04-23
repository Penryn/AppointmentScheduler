package com.example.slabiak.appointmentscheduler.model;

import com.example.slabiak.appointmentscheduler.entity.user.customer.Customer;

public record CustomerListItem(Customer customer, long appointmentCount) {
}
