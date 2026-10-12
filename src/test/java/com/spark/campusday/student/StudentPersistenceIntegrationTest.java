package com.spark.campusday.student;

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

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@EnabledIfEnvironmentVariable(named = "CAMPUS_DAY_MONGO_TEST_URI", matches = ".+")
@SpringBootTest(properties = {
        "spring.mongodb.database=CampusDayPart1Test",
        "spring.data.mongodb.auto-index-creation=true"
})
class StudentPersistenceIntegrationTest {
    @DynamicPropertySource
    static void mongo(DynamicPropertyRegistry registry) {
        registry.add("spring.mongodb.uri", () -> System.getenv("CAMPUS_DAY_MONGO_TEST_URI"));
    }

    @Autowired
    private StudentService service;

    @Autowired
    private StudentRepository students;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Test
    void persistsAStudentWithAuditTimestamps() {
        String suffix = UUID.randomUUID().toString();
        Student created = service.createStudent(
                "PART1-" + suffix, "Test Student", "part1-" + suffix + "@campusday.test", AcademicYear.FOURTH_YEAR);
        try {
            Student loaded = service.getById(created.getId());
            assertEquals(created.getStudentId(), loaded.getStudentId());
            assertEquals(created.getEmail(), loaded.getEmail());
            assertNotNull(loaded.getCreatedAt());
            assertNotNull(loaded.getUpdatedAt());
            assertTrue(mongoTemplate.indexOps(Student.class).getIndexInfo().stream()
                    .anyMatch(index -> index.isUnique() && index.getName().equals("studentId")));
            assertTrue(mongoTemplate.indexOps(Student.class).getIndexInfo().stream()
                    .anyMatch(index -> index.isUnique() && index.getName().equals("email")));
        } finally {
            students.deleteById(created.getId());
        }
    }
}
