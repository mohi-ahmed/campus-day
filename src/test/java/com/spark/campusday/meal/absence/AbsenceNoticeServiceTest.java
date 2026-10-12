package com.spark.campusday.meal.absence;

import com.spark.campusday.common.exception.ConflictException;
import com.spark.campusday.common.exception.InvalidBusinessStateException;
import com.spark.campusday.meal.absence.dto.AbsenceNoticeRequest;
import com.spark.campusday.meal.absence.model.MealAbsenceNotice;
import com.spark.campusday.meal.absence.repository.MealAbsenceNoticeRepository;
import com.spark.campusday.meal.absence.service.AbsenceNoticeService;
import com.spark.campusday.meal.model.MealType;
import com.spark.campusday.student.model.AcademicYear;
import com.spark.campusday.student.model.Student;
import com.spark.campusday.student.service.StudentService;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AbsenceNoticeServiceTest {
    private static final LocalDate START = LocalDate.of(2026, 9, 26);
    private static final LocalDate END = LocalDate.of(2026, 9, 27);
    private final MealAbsenceNoticeRepository notices = mock(MealAbsenceNoticeRepository.class);
    private final StudentService students = mock(StudentService.class);
    private final AbsenceNoticeService service = new AbsenceNoticeService(notices, students);

    @Test
    void createsPlanningNoticeForYearsOneToFour() {
        student(AcademicYear.THIRD_YEAR);
        when(notices.save(any(MealAbsenceNotice.class))).thenAnswer(call -> call.getArgument(0));

        var created = service.create("REG-1", request(START, END, List.of(MealType.LUNCH)));

        assertEquals(List.of(MealType.LUNCH), created.meals());
        assertEquals("Away", created.note());
    }

    @Test
    void rejectsFinalYearStudent() {
        student(AcademicYear.FINAL_YEAR);
        assertThrows(InvalidBusinessStateException.class,
                () -> service.create("REG-1", request(START, END, List.of(MealType.LUNCH))));
        verifyNoInteractions(notices);
    }

    @Test
    void rejectsInvalidRangeAndEmptyMeals() {
        student(AcademicYear.FIRST_YEAR);
        assertThrows(InvalidBusinessStateException.class,
                () -> service.create("REG-1", request(END, START, List.of(MealType.LUNCH))));
        assertThrows(InvalidBusinessStateException.class,
                () -> service.create("REG-1", request(START, END, List.of())));
        verifyNoInteractions(notices);
    }

    @Test
    void rejectsOverlappingSameMeal() {
        student(AcademicYear.FOURTH_YEAR);
        when(notices.existsOverlapping("db-student", START, END, List.of(MealType.LUNCH)))
                .thenReturn(true);

        assertThrows(ConflictException.class,
                () -> service.create("REG-1", request(START, END, List.of(MealType.LUNCH))));
    }

    private AbsenceNoticeRequest request(LocalDate start, LocalDate end, List<MealType> meals) {
        return new AbsenceNoticeRequest(start, end, meals, "Away");
    }

    private void student(AcademicYear year) {
        Student student = mock(Student.class);
        when(student.getId()).thenReturn("db-student");
        when(student.getAcademicYear()).thenReturn(year);
        when(students.getByStudentId("REG-1")).thenReturn(student);
    }
}
