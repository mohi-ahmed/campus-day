package com.spark.campusday.meal.managementperiod;

import com.spark.campusday.common.exception.ConflictException;
import com.spark.campusday.meal.access.MealManagerAccess;
import com.spark.campusday.meal.managementperiod.dto.ManagementPeriodRequest;
import com.spark.campusday.meal.managementperiod.model.MealManagementPeriod;
import com.spark.campusday.meal.managementperiod.repository.MealManagementPeriodRepository;
import com.spark.campusday.meal.managementperiod.service.ManagementPeriodService;
import com.spark.campusday.student.model.AcademicYear;
import com.spark.campusday.student.model.Student;
import com.spark.campusday.student.repository.StudentRepository;
import com.spark.campusday.student.service.StudentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@EnabledIfEnvironmentVariable(named = "CAMPUS_DAY_MONGO_TEST_URI", matches = ".+")
@SpringBootTest(properties = {
        "spring.mongodb.database=CampusDayPart2Test",
        "spring.data.mongodb.auto-index-creation=true"
})
class ManagementPeriodPersistenceIntegrationTest {
    @DynamicPropertySource
    static void mongo(DynamicPropertyRegistry registry) {
        registry.add("spring.mongodb.uri", () -> System.getenv("CAMPUS_DAY_MONGO_TEST_URI"));
    }

    @Autowired private StudentService studentService;
    @Autowired private StudentRepository students;
    @Autowired private ManagementPeriodService periodService;
    @Autowired private MealManagementPeriodRepository periods;
    @Autowired private MealManagerAccess managerAccess;
    @Autowired private MongoTemplate mongoTemplate;

    @Test
    void persistsCurrentPeriodAndItsManagerAssignments() {
        String suffix = UUID.randomUUID().toString();
        Student first = studentService.createStudent("PART2-A-" + suffix, "First Manager",
                "part2-a-" + suffix + "@campusday.test", AcademicYear.FOURTH_YEAR);
        Student second = studentService.createStudent("PART2-B-" + suffix, "Second Manager",
                "part2-b-" + suffix + "@campusday.test", AcademicYear.FOURTH_YEAR);
        String periodId = null;
        try {
            YearMonth month = YearMonth.from(LocalDate.now(ZoneId.of("Asia/Dhaka")));
            var created = periodService.create(new ManagementPeriodRequest(month.getYear(), month.getMonthValue(),
                    month.atDay(1), month.atEndOfMonth(), List.of(first.getId(), second.getId())));
            periodId = created.id();

            MealManagementPeriod stored = periods.findById(periodId).orElseThrow();
            assertNotNull(stored.getCreatedAt());
            assertNotNull(stored.getUpdatedAt());
            assertEquals(2, periodService.getCurrent().managers().size());
            assertTrue(managerAccess.hasCurrentAccess(first.getId()));
            assertThrows(ConflictException.class, () -> periodService.create(new ManagementPeriodRequest(
                    month.getYear(), month.getMonthValue(), month.atDay(1), month.atEndOfMonth(),
                    List.of(first.getId(), second.getId()))));
            assertTrue(mongoTemplate.indexOps(MealManagementPeriod.class).getIndexInfo().stream()
                    .anyMatch(index -> index.isUnique() && index.getName().equals("period_month_unique")));
            periodService.close(periodId);
            assertFalse(managerAccess.hasCurrentAccess(first.getId()));
        } finally {
            if (periodId != null) periods.deleteById(periodId);
            students.deleteAllById(List.of(first.getId(), second.getId()));
        }
    }
}
