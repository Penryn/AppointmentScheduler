package com.example.slabiak.appointmentscheduler.dao.user.customer;

import com.example.slabiak.appointmentscheduler.dao.user.CommonUserRepository;
import com.example.slabiak.appointmentscheduler.entity.user.customer.Customer;
import com.example.slabiak.appointmentscheduler.model.CustomerListItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;

public interface CustomerRepository extends CommonUserRepository<Customer> {
    @Query(value = """
            select new com.example.slabiak.appointmentscheduler.model.CustomerListItem(
                c,
                (select count(a) from Appointment a where a.customer = c)
            )
            from Customer c
            """,
            countQuery = "select count(c) from Customer c")
    Page<CustomerListItem> findListPage(Pageable pageable);
}
