package com.example.slabiak.appointmentscheduler.dao.user.provider;

import com.example.slabiak.appointmentscheduler.dao.user.CommonUserRepository;
import com.example.slabiak.appointmentscheduler.entity.Work;
import com.example.slabiak.appointmentscheduler.entity.user.provider.Provider;
import com.example.slabiak.appointmentscheduler.model.ProviderListItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ProviderRepository extends CommonUserRepository<Provider> {
    @Override
    @EntityGraph(attributePaths = {"works", "workingPlan", "roles"})
    Optional<Provider> findById(Integer id);

    @Query(value = """
            select new com.example.slabiak.appointmentscheduler.model.ProviderListItem(
                p,
                (select count(a) from Appointment a where a.provider = p),
                (select count(w) from Work w join w.providers provider where provider.id = p.id)
            )
            from Provider p
            """,
            countQuery = "select count(p) from Provider p")
    Page<ProviderListItem> findListPage(Pageable pageable);

    List<Provider> findByWorks(Work work);

    @Query("select distinct p from Provider p join fetch p.works w where w.targetCustomer = 'retail'")
    List<Provider> findAllWithRetailWorks();

    @Query("select distinct p from Provider p join fetch p.works w where w.targetCustomer = 'corporate'")
    List<Provider> findAllWithCorporateWorks();
}
