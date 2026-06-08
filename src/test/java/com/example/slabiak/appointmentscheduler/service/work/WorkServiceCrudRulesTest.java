// 测试说明：验证服务项目的创建、更新、删除和业务约束规则。
package com.example.slabiak.appointmentscheduler.service.work;

import com.example.slabiak.appointmentscheduler.dao.WorkRepository;
import com.example.slabiak.appointmentscheduler.entity.Work;
import com.example.slabiak.appointmentscheduler.service.impl.WorkServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkServiceCrudRulesTest {

    @Mock
    private WorkRepository workRepository;

    @InjectMocks
    private WorkServiceImpl workService;

    private Work work;
    private Optional<Work> workOptional;
    private List<Work> workList;

    @BeforeEach
    void setUp() {
        work = new Work();
        work.setId(1);

        workOptional = Optional.of(work);
        workList = List.of(work);
    }

    @Test
    void R1_shouldCreateWork() {
        workService.createNewWork(work);

        // 检查点：验证该测试用例的预期结果。
        verify(workRepository, times(1)).save(work);
    }

    @Test
    void R2_shouldFindWorkById() {
        when(workRepository.findById(1)).thenReturn(workOptional);

        Work result = workService.getWorkById(1);

        // 检查点：验证该测试用例的预期结果。
        assertEquals(work, result);
        verify(workRepository, times(1)).findById(1);
    }

    @Test
    void R3_shouldFindAllWorks() {
        when(workRepository.findAll()).thenReturn(workList);

        List<Work> result = workService.getAllWorks();

        // 检查点：验证该测试用例的预期结果。
        assertEquals(workList, result);
        verify(workRepository, times(1)).findAll();
    }

    @Test
    void R4_shouldDeleteWorkById() {
        workService.deleteWorkById(1);

        // 检查点：验证该测试用例的预期结果。
        verify(workRepository, times(1)).deleteById(1);
    }
}
