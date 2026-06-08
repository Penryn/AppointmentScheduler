// 测试说明：验证服务项目控制器的列表、创建、更新和删除流程。
package com.example.slabiak.appointmentscheduler.controller;

import com.example.slabiak.appointmentscheduler.entity.Work;
import com.example.slabiak.appointmentscheduler.service.WorkService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkControllerTest {

    @Mock
    private WorkService workService;

    private WorkController controller;

    @BeforeEach
    void setUp() {
        controller = new WorkController(workService);
    }

    @Test
    void shouldShowAllWorks() {
        PageRequest pageable = PageRequest.of(0, 20);
        when(workService.getWorkList(pageable)).thenReturn(Page.empty(pageable));
        Model model = new ExtendedModelMap();

        String view = controller.showAllWorks(model, pageable);

        // 检查点：验证该测试用例的预期结果。
        assertThat(view).isEqualTo("works/list");
        assertThat(model.getAttribute("works")).isEqualTo(Page.empty(pageable));
    }

    @Test
    void shouldShowCreateAndUpdateForms() {
        Work existing = work(7);
        when(workService.getWorkById(7)).thenReturn(existing);
        Model updateModel = new ExtendedModelMap();
        Model createModel = new ExtendedModelMap();

        // 检查点：验证该测试用例的预期结果。
        assertThat(controller.showFormForUpdateWork(7, updateModel)).isEqualTo("works/createOrUpdateWorkForm");
        assertThat(updateModel.getAttribute("work")).isSameAs(existing);
        assertThat(controller.showFormForAddWork(createModel)).isEqualTo("works/createOrUpdateWorkForm");
        assertThat(createModel.getAttribute("work")).isInstanceOf(Work.class);
    }

    @Test
    void shouldCreateUpdateAndDeleteWork() {
        Work newWork = work(null);
        Work existing = work(7);

        // 检查点：验证该测试用例的预期结果。
        assertThat(controller.saveWork(newWork)).isEqualTo("redirect:/works/all");
        assertThat(controller.saveWork(existing)).isEqualTo("redirect:/works/all");
        assertThat(controller.deleteWork(7)).isEqualTo("redirect:/works/all");

        verify(workService).createNewWork(newWork);
        // 检查点：验证该测试用例的预期结果。
        verify(workService).updateWork(existing);
        verify(workService).deleteWorkById(7);
    }

    private Work work(Integer id) {
        Work work = new Work();
        work.setId(id);
        work.setName("Work");
        return work;
    }
}
