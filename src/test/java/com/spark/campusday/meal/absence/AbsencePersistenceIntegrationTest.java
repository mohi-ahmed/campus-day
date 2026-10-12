package com.spark.campusday.meal.absence;

import com.spark.campusday.common.exception.ConflictException;
import com.spark.campusday.meal.absence.dto.AbsenceNoticeRequest;
import com.spark.campusday.meal.absence.model.MealAbsenceNotice;
import com.spark.campusday.meal.absence.repository.MealAbsenceNoticeRepository;
import com.spark.campusday.meal.absence.service.AbsenceNoticeService;
import com.spark.campusday.meal.model.MealType;
import com.spark.campusday.student.model.AcademicYear;
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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@EnabledIfEnvironmentVariable(named = "CAMPUS_DAY_MONGO_TEST_URI", matches = ".+")
@SpringBootTest(properties = {
        "spring.mongodb.database=CampusDayPart5Test",
        "spring.data.mongodb.auto-index-creation=true"
})
class AbsencePersistenceIntegrationTest {
    @DynamicPropertySource
    static void mongo(DynamicPropertyRegistry registry) {
        registry.add("spring.mongodb.uri", () -> System.getenv("CAMPUS_DAY_MONGO_TEST_URI"));
    }

    @Autowired private StudentService students;
    @Autowired private StudentRepository studentRepository;
    @Autowired private AbsenceNoticeService service;
    @Autowired private MealAbsenceNoticeRepository notices;
    @Autowired private MongoTemplate mongo;

    @Test
    void storesNoticesAndRejectsOverlappingSameMeal() {
        var student = students.createStudent("PART5-" + UUID.randomUUID(), "Absence Test",
                "part5-" + UUID.randomUUID() + "@campusday.test", AcademicYear.SECOND_YEAR);
        LocalDate start = LocalDate.of(2040, 1, 17);
        List<String> ids = new ArrayList<>();
        try {
            var first = service.create(student.getStudentId(),
                    new AbsenceNoticeRequest(start, start.plusDays(1), List.of(MealType.BREAKFAST), null));
            ids.add(first.id());
            var second = service.create(student.getStudentId(),
                    new AbsenceNoticeRequest(start.plusDays(1), start.plusDays(1), List.of(MealType.LUNCH), null));
            ids.add(second.id());

            assertEquals(2, service.list(student.getStudentId(), start, start.plusDays(1)).size());
            assertNotNull(notices.findById(first.id()).orElseThrow().getCreatedAt());
            assertThrows(ConflictException.class, () -> service.create(student.getStudentId(),
                    new AbsenceNoticeRequest(start.plusDays(1), start.plusDays(2),
                            List.of(MealType.BREAKFAST), null)));
            assertTrue(mongo.indexOps(MealAbsenceNotice.class).getIndexInfo().stream()
                    .anyMatch(index -> index.getName().equals("absence_student_dates")));
        } finally {
            notices.deleteAllById(ids);
            studentRepository.deleteById(student.getId());
        }
    }
}
