package com.example.slabiak.appointmentscheduler.service.work;

import com.example.slabiak.appointmentscheduler.dao.WorkRepository;
import com.example.slabiak.appointmentscheduler.entity.Work;
import com.example.slabiak.appointmentscheduler.entity.user.Role;
import com.example.slabiak.appointmentscheduler.entity.user.customer.CorporateCustomer;
import com.example.slabiak.appointmentscheduler.entity.user.customer.Customer;
import com.example.slabiak.appointmentscheduler.entity.user.customer.RetailCustomer;
import com.example.slabiak.appointmentscheduler.service.UserService;
import com.example.slabiak.appointmentscheduler.service.impl.WorkServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@org.junit.jupiter.api.extension.ExtendWith(MockitoExtension.class)
public class WorkCustomerEligibilityTest {

    @Mock
    private WorkRepository workRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private WorkServiceImpl workService;

    private RetailCustomer retailCustomer;
    private CorporateCustomer corporateCustomer;
    @BeforeEach
    public void setUp() {
        retailCustomer = new RetailCustomer();
        retailCustomer.setId(1);
        retailCustomer.setRoles(List.of(new Role("ROLE_CUSTOMER_RETAIL")));

        corporateCustomer = new CorporateCustomer();
        corporateCustomer.setId(2);
        corporateCustomer.setRoles(List.of(new Role("ROLE_CUSTOMER_CORPORATE")));
    }

    @Test
    public void shouldAllowRetailCustomerToUseRetailWork() {
        assertThat(isWorkForCustomer(retailWork(), retailCustomer)).isTrue();
    }

    @Test
    public void shouldRejectRetailCustomerForCorporateWork() {
        assertThat(isWorkForCustomer(corporateWork(), retailCustomer)).isFalse();
    }

    @Test
    public void shouldAllowCorporateCustomerToUseCorporateWork() {
        assertThat(isWorkForCustomer(corporateWork(), corporateCustomer)).isTrue();
    }

    @Test
    public void shouldRejectCorporateCustomerForRetailWork() {
        assertThat(isWorkForCustomer(retailWork(), corporateCustomer)).isFalse();
    }

    private boolean isWorkForCustomer(Work work, Customer customer) {
        work.setId(10);
        when(userService.getCustomerById(customer.getId())).thenReturn(customer);
        when(workRepository.findById(work.getId())).thenReturn(Optional.of(work));

        return workService.isWorkForCustomer(work.getId(), customer.getId());
    }

    private Work retailWork() {
        Work work = new Work();
        work.setTargetCustomer("retail");
        return work;
    }

    private Work corporateWork() {
        Work work = new Work();
        work.setTargetCustomer("corporate");
        return work;
    }
}
