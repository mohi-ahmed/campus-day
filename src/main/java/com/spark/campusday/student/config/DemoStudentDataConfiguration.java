package com.spark.campusday.student.config;

import com.spark.campusday.student.model.AcademicYear;
import com.spark.campusday.student.repository.StudentRepository;
import com.spark.campusday.student.service.StudentService;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.List;

@Configuration(proxyBeanMethods = false)
@Profile("!prod")
@ConditionalOnProperty(name = "app.demo.seed-students", havingValue = "true")
public class DemoStudentDataConfiguration {
    private static final List<DemoStudent> DEMO_STUDENTS = List.of(
            new DemoStudent("jimc-24018", "Nusrat Jahan",
                    "nusrat.jahan@campusday.test", AcademicYear.THIRD_YEAR),
            new DemoStudent("jimc-22034", "Samiul Hasan",
                    "samiul.hasan@campusday.test", AcademicYear.FINAL_YEAR),
            new DemoStudent("jimc-23011", "Arafat Rahman",
                    "arafat.rahman@campusday.test", AcademicYear.FOURTH_YEAR),
            new DemoStudent("jimc-23012", "Tahmid Islam",
                    "tahmid.islam@campusday.test", AcademicYear.FOURTH_YEAR)
    );

    @Bean
    ApplicationRunner demoStudentSeeder(StudentRepository students,
                                         StudentService studentService) {
        return arguments -> DEMO_STUDENTS.forEach(demo -> {
            if (students.findByStudentId(demo.studentId()).isEmpty()
                    && students.findByEmail(demo.email()).isEmpty()) {
                studentService.createStudent(demo.studentId(), demo.name(),
                        demo.email(), demo.academicYear());
            }
        });
    }

    private record DemoStudent(
            String studentId,
            String name,
            String email,
            AcademicYear academicYear
    ) {
    }
}
