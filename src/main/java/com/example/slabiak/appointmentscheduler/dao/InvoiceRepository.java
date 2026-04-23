package com.example.slabiak.appointmentscheduler.dao;

import com.example.slabiak.appointmentscheduler.entity.Invoice;
import com.example.slabiak.appointmentscheduler.model.InvoiceListItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface InvoiceRepository extends JpaRepository<Invoice, Integer> {

    @Query("select i from Invoice i where i.issued >= :beginingOfCurrentMonth")
    List<Invoice> findAllIssuedInCurrentMonth(@Param("beginingOfCurrentMonth") LocalDateTime beginingOfCurrentMonth);

    @Query("select i from Invoice i inner join i.appointments a where a.id in :appointmentId")
    Invoice findByAppointmentId(@Param("appointmentId") int appointmentId);

    @Query(value = """
            select new com.example.slabiak.appointmentscheduler.model.InvoiceListItem(
                i.id,
                i.number,
                i.issued,
                i.status,
                i.totalAmount,
                concat(c.firstName, ' ', c.lastName)
            )
            from Invoice i
            left join i.appointments a
            left join a.customer c
            group by i.id, i.number, i.issued, i.status, i.totalAmount, c.firstName, c.lastName
            """,
            countQuery = "select count(i) from Invoice i")
    Page<InvoiceListItem> findListPage(Pageable pageable);
}
