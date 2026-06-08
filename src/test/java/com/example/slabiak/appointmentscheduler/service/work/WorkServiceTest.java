// 测试说明：验证服务项目服务的查询、保存、删除和客户适配判断行为。
package com.example.slabiak.appointmentscheduler.service.work;

import com.example.slabiak.appointmentscheduler.dao.WorkRepository;
import com.example.slabiak.appointmentscheduler.entity.Work;
import com.example.slabiak.appointmentscheduler.entity.user.Role;
import com.example.slabiak.appointmentscheduler.entity.user.customer.Customer;
import com.example.slabiak.appointmentscheduler.exception.WorkNotFoundException;
import com.example.slabiak.appointmentscheduler.model.WorkListItem;
import com.example.slabiak.appointmentscheduler.service.UserService;
import com.example.slabiak.appointmentscheduler.service.impl.WorkServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

@org.junit.jupiter.api.extension.ExtendWith(MockitoExtension.class)
public class WorkServiceTest {

    @Mock
    private WorkRepository workRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private WorkServiceImpl workService;

    private Work work;
    private Optional<Work> workOptional;
    private List<Work> works;
    @BeforeEach
    public void initObjects() {
        work = new Work();
        workOptional = Optional.of(work);
    }

    @Test
    public void shouldSaveWork() {
        workService.createNewWork(work);
        // 检查点：验证该测试用例的预期结果。
        verify(workRepository, times(1)).save(work);
    }

    @Test
    public void shouldFindWorkById() {
        when(workRepository.findById(1)).thenReturn(workOptional);
        // 检查点：验证该测试用例的预期结果。
        assertEquals(workOptional.get(), workService.getWorkById(1));
        verify(workRepository, times(1)).findById(1);
    }

    @Test
    public void shouldFindAllWorks() {
        when(workRepository.findAll()).thenReturn(works);
        // 检查点：验证该测试用例的预期结果。
        assertEquals(works, workService.getAllWorks());
        verify(workRepository, times(1)).findAll();
    }

    @Test
    public void shouldDeleteWorkById() {
        workService.deleteWorkById(1);
        // 检查点：验证该测试用例的预期结果。
        verify(workRepository).deleteById(1);
    }

    @Test
    public void shouldUpdateExistingWork() {
        Work existing = work(1, "Old", "old", 100, 60, true, "retail");
        Work update = work(1, "New", "new", 250, 45, false, "corporate");
        when(workRepository.findById(1)).thenReturn(Optional.of(existing));

        workService.updateWork(update);

        // 检查点：验证该测试用例的预期结果。
        assertEquals("New", existing.getName());
        assertEquals("new", existing.getDescription());
        assertEquals(250, existing.getPrice());
        assertEquals(45, existing.getDuration());
        // 检查点：验证该测试用例的预期结果。
        assertFalse(existing.getEditable());
        assertEquals("corporate", existing.getTargetCustomer());
        verify(workRepository).save(existing);
    }

    @Test
    public void shouldThrowWhenWorkDoesNotExist() {
        when(workRepository.findById(99)).thenReturn(Optional.empty());

        // 检查点：验证该测试用例的预期结果。
        assertThrows(WorkNotFoundException.class, () -> workService.getWorkById(99));
    }

    @Test
    public void shouldDelegateWorkListAndTargetQueries() {
        PageRequest pageable = PageRequest.of(0, 10);
        Page<WorkListItem> page = new PageImpl<>(List.of(new WorkListItem(work(1, "A", "d", 1, 10, true, "retail"), 2)));
        List<Work> workList = List.of(work(2, "B", "d", 1, 10, true, "corporate"));
        when(workRepository.findListPage(pageable)).thenReturn(page);
        when(workRepository.findByProviderId(2)).thenReturn(workList);
        when(workRepository.findByTargetCustomer("retail")).thenReturn(workList);
        when(workRepository.findByTargetCustomer("corporate")).thenReturn(workList);
        when(workRepository.findByTargetCustomerAndProviderId("retail", 2)).thenReturn(workList);
        when(workRepository.findByTargetCustomerAndProviderId("corporate", 2)).thenReturn(workList);

        // 检查点：验证该测试用例的预期结果。
        assertEquals(page, workService.getWorkList(pageable));
        assertEquals(workList, workService.getWorksByProviderId(2));
        assertEquals(workList, workService.getRetailCustomerWorks());
        assertEquals(workList, workService.getCorporateCustomerWorks());
        // 检查点：验证该测试用例的预期结果。
        assertEquals(workList, workService.getWorksForRetailCustomerByProviderId(2));
        assertEquals(workList, workService.getWorksForCorporateCustomerByProviderId(2));
    }

    @Test
    public void shouldCheckWorkEligibilityForRetailAndCorporateCustomers() {
        Customer retailCustomer = customer("ROLE_CUSTOMER_RETAIL");
        Customer corporateCustomer = customer("ROLE_CUSTOMER_CORPORATE");
        Work retailWork = work(1, "Retail", "d", 1, 10, true, "retail");
        Work corporateWork = work(2, "Corporate", "d", 1, 10, true, "corporate");
        when(userService.getCustomerById(3)).thenReturn(retailCustomer);
        when(userService.getCustomerById(4)).thenReturn(corporateCustomer);
        when(workRepository.findById(1)).thenReturn(Optional.of(retailWork));
        when(workRepository.findById(2)).thenReturn(Optional.of(corporateWork));

        // 检查点：验证该测试用例的预期结果。
        assertTrue(workService.isWorkForCustomer(1, 3));
        assertFalse(workService.isWorkForCustomer(2, 3));
        assertTrue(workService.isWorkForCustomer(2, 4));
        assertFalse(workService.isWorkForCustomer(1, 4));
    }

    private Work work(int id, String name, String description, double price, int duration, boolean editable, String target) {
        Work work = new Work();
        work.setId(id);
        work.setName(name);
        work.setDescription(description);
        work.setPrice(price);
        work.setDuration(duration);
        work.setEditable(editable);
        work.setTargetCustomer(target);
        return work;
    }

    private Customer customer(String roleName) {
        Customer customer = new Customer();
        customer.setId(3);
        customer.setRoles(List.of(new Role(roleName), new Role("ROLE_CUSTOMER")));
        return customer;
    }
}
