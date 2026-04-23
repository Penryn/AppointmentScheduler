package com.example.slabiak.appointmentscheduler.model;

import com.example.slabiak.appointmentscheduler.entity.Work;

public record WorkListItem(Work work, long providerCount) {
}
