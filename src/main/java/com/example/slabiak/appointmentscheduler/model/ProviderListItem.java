package com.example.slabiak.appointmentscheduler.model;

import com.example.slabiak.appointmentscheduler.entity.user.provider.Provider;

public record ProviderListItem(Provider provider, long appointmentCount, long workCount) {
}
