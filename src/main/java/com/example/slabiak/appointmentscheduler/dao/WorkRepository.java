package com.example.slabiak.appointmentscheduler.dao;

import com.example.slabiak.appointmentscheduler.entity.Work;
import com.example.slabiak.appointmentscheduler.model.WorkListItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface WorkRepository extends JpaRepository<Work, Integer> {
    @Query(value = """
            select new com.example.slabiak.appointmentscheduler.model.WorkListItem(
                w,
                (select count(p) from Provider p join p.works work where work.id = w.id)
            )
            from Work w
            """,
            countQuery = "select count(w) from Work w")
    Page<WorkListItem> findListPage(Pageable pageable);

    @Query("select w from Work w inner join w.providers p where p.id in :providerId")
    List<Work> findByProviderId(@Param("providerId") int providerId);

    @Query("select w from Work w where w.targetCustomer = :target ")
    List<Work> findByTargetCustomer(@Param("target") String targetCustomer);

    @Query("select w from Work w inner join w.providers p where p.id in :providerId and w.targetCustomer = :target ")
    List<Work> findByTargetCustomerAndProviderId(@Param("target") String targetCustomer, @Param("providerId") int providerId);
}
