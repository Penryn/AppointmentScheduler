package com.example.slabiak.appointmentscheduler.service.workingplan;

import com.example.slabiak.appointmentscheduler.dao.WorkingPlanRepository;
import com.example.slabiak.appointmentscheduler.entity.WorkingPlan;
import com.example.slabiak.appointmentscheduler.model.DayPlan;
import com.example.slabiak.appointmentscheduler.model.TimePeroid;
import jakarta.persistence.EntityManager;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("integration-test")
public class WorkingPlanPersistenceIT {

    @Autowired
    private WorkingPlanRepository workingPlanRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @Transactional
    public void shouldRoundTripJsonDayPlanColumns() {
        WorkingPlan workingPlan = workingPlanRepository.findById(2)
                .orElseThrow(RuntimeException::new);

        DayPlan monday = new DayPlan();
        monday.setWorkingHours(new TimePeroid(LocalTime.of(9, 0), LocalTime.of(17, 0)));
        monday.addBreak(new TimePeroid(LocalTime.of(12, 0), LocalTime.of(12, 30)));
        monday.addBreak(new TimePeroid(LocalTime.of(15, 0), LocalTime.of(15, 15)));

        workingPlan.setMonday(monday);
        workingPlanRepository.saveAndFlush(workingPlan);
        entityManager.clear();

        WorkingPlan reloaded = workingPlanRepository.findById(2)
                .orElseThrow(RuntimeException::new);

        assertThat(reloaded.getMonday()).isEqualTo(monday);
        assertThat(reloaded.getMonday().getBreaks())
                .containsExactly(
                        new TimePeroid(LocalTime.of(12, 0), LocalTime.of(12, 30)),
                        new TimePeroid(LocalTime.of(15, 0), LocalTime.of(15, 15)));
    }
}
