package com.example.slabiak.appointmentscheduler.service;

import com.example.slabiak.appointmentscheduler.entity.Work;
import com.example.slabiak.appointmentscheduler.model.WorkListItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface WorkService {
    void createNewWork(Work work);

    Work getWorkById(int workId);

    List<Work> getAllWorks();

    Page<WorkListItem> getWorkList(Pageable pageable);

    List<Work> getWorksByProviderId(int providerId);

    List<Work> getRetailCustomerWorks();

    List<Work> getCorporateCustomerWorks();

    List<Work> getWorksForRetailCustomerByProviderId(int providerId);

    List<Work> getWorksForCorporateCustomerByProviderId(int providerId);

    void updateWork(Work work);

    void deleteWorkById(int workId);

    boolean isWorkForCustomer(int workId, int customerId);
}
