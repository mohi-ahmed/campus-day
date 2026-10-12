package com.spark.campusday.student.service;

import com.spark.campusday.common.exception.ResourceNotFoundException;
import com.spark.campusday.student.model.AcademicYear;
import com.spark.campusday.student.model.Student;
import com.spark.campusday.student.repository.StudentRepository;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StudentServiceTest {
    private final StudentRepository students = mock(StudentRepository.class);
    private final StudentService service = new StudentService(
            students, Validation.buildDefaultValidatorFactory().getValidator());

    @Test
    void createsAValidStudent() {
        when(students.save(org.mockito.ArgumentMatchers.any(Student.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Student created = service.createStudent(
                "JIMC-24018", "Nusrat Jahan", "Nusrat.Jahan@Example.com", AcademicYear.THIRD_YEAR);

        assertEquals("JIMC-24018", created.getStudentId());
        assertEquals("nusrat.jahan@example.com", created.getEmail());
        assertEquals(AcademicYear.THIRD_YEAR, created.getAcademicYear());
        verify(students).save(created);
    }

    @Test
    void reportsStudentNotFound() {
        when(students.findById("missing")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getById("missing"));
    }

    @Test
    void rejectsAnInvalidEmail() {
        assertThrows(jakarta.validation.ConstraintViolationException.class,
                () -> service.createStudent("JIMC-24019", "Invalid Email", "not-an-email", AcademicYear.THIRD_YEAR));
    }
}
