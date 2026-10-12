package com.spark.campusday.meal.managementperiod;

import com.spark.campusday.common.exception.ConflictException;
import com.spark.campusday.common.exception.InvalidBusinessStateException;
import com.spark.campusday.meal.managementperiod.dto.ManagementPeriodRequest;
import com.spark.campusday.meal.managementperiod.model.ManagementPeriodStatus;
import com.spark.campusday.meal.managementperiod.model.MealManagementPeriod;
import com.spark.campusday.meal.managementperiod.repository.MealManagementPeriodRepository;
import com.spark.campusday.meal.managementperiod.service.AssignedMealManagerAccess;
import com.spark.campusday.meal.managementperiod.service.ManagementPeriodService;
import com.spark.campusday.meal.managerapplication.service.MealManagerApplicationService;
import com.spark.campusday.student.model.AcademicYear;
import com.spark.campusday.student.model.Student;
import com.spark.campusday.student.repository.StudentRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ManagementPeriodServiceTest {
    private static final LocalDate START = LocalDate.of(2026, 9, 1);
    private static final LocalDate END = LocalDate.of(2026, 9, 30);

    private final MealManagementPeriodRepository periods = mock(MealManagementPeriodRepository.class);
    private final StudentRepository students = mock(StudentRepository.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-09-24T00:00:00Z"), ZoneId.of("Asia/Dhaka"));
    private final ManagementPeriodService service = new ManagementPeriodService(periods, students, clock);

    @Test
    void createsPeriodWithTwoFourthYearStudents() {
        stubStudents(AcademicYear.FOURTH_YEAR, AcademicYear.FOURTH_YEAR);
        when(periods.save(any(MealManagementPeriod.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var created = service.create(request(List.of("one", "two")));

        assertEquals(ManagementPeriodStatus.ACTIVE, created.status());
        assertEquals(2, created.managers().size());
        assertEquals("one", created.managers().get(0).id());
    }

    @Test
    void rejectsWrongManagerCount() {
        for (List<String> ids : List.of(List.<String>of(), List.of("one"), List.of("one", "two", "three"))) {
            assertThrows(InvalidBusinessStateException.class, () -> service.create(request(ids)));
        }
        verifyNoInteractions(students, periods);
    }

    @Test
    void rejectsDuplicateManager() {
        assertThrows(InvalidBusinessStateException.class,
                () -> service.create(request(List.of("one", "one"))));
        verifyNoInteractions(students, periods);
    }

    @Test
    void rejectsNonFourthYearManager() {
        stubStudents(AcademicYear.FOURTH_YEAR, AcademicYear.FINAL_YEAR);

        assertThrows(InvalidBusinessStateException.class,
                () -> service.create(request(List.of("one", "two"))));
    }

    @Test
    void rejectsOverlappingActivePeriod() {
        stubStudents(AcademicYear.FOURTH_YEAR, AcademicYear.FOURTH_YEAR);
        when(periods.existsByStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqualAndIdNot(
                ManagementPeriodStatus.ACTIVE, END, START, "")).thenReturn(true);

        assertThrows(ConflictException.class, () -> service.create(request(List.of("one", "two"))));
    }

    @Test
    void validatesDatesAgainstMonth() {
        assertThrows(InvalidBusinessStateException.class,
                () -> service.create(new ManagementPeriodRequest(2026, 9, END, START, List.of("one", "two"))));
        assertThrows(InvalidBusinessStateException.class,
                () -> service.create(new ManagementPeriodRequest(2026, 9, START, LocalDate.of(2026, 10, 1),
                        List.of("one", "two"))));
    }

    @Test
    void closesPeriodWithoutAllowingReopen() {
        MealManagementPeriod period = new MealManagementPeriod(2026, 9, START, END, List.of("one", "two"));
        when(periods.findById("period")).thenReturn(Optional.of(period));
        when(periods.save(period)).thenReturn(period);
        stubStudents(AcademicYear.FOURTH_YEAR, AcademicYear.FOURTH_YEAR);

        assertEquals(ManagementPeriodStatus.CLOSED, service.close("period").status());
        assertThrows(InvalidBusinessStateException.class, () -> service.close("period"));
        assertThrows(InvalidBusinessStateException.class,
                () -> service.update("period", request(List.of("one", "two"))));
    }

    @Test
    void accessDependsOnCurrentAssignment() {
        MealManagementPeriod period = new MealManagementPeriod(2026, 9, START, END, List.of("one", "two"));
        when(periods.findFirstByStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                eq(ManagementPeriodStatus.ACTIVE), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(Optional.of(period));
        AssignedMealManagerAccess access = new AssignedMealManagerAccess(
                periods, mock(MealManagerApplicationService.class), clock);

        assertTrue(access.hasCurrentAccess("one"));
        assertFalse(access.hasCurrentAccess("another-fourth-year-student"));
        period.close();
        when(periods.findFirstByStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                eq(ManagementPeriodStatus.ACTIVE), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(Optional.empty());
        assertFalse(access.hasCurrentAccess("one"));
    }

    @Test
    void findsPreviousCurrentAndNextTeamsByDate() {
        MealManagementPeriod current = new MealManagementPeriod(2026, 9, START, END, List.of("one", "two"));
        MealManagementPeriod previous = new MealManagementPeriod(2026, 8,
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31), List.of("one", "two"));
        previous.close();
        MealManagementPeriod next = new MealManagementPeriod(2026, 10,
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31), List.of("one", "two"));
        when(periods.findFirstByStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                eq(ManagementPeriodStatus.ACTIVE), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(Optional.of(current));
        when(periods.findFirstByStartDateLessThanOrderByStartDateDesc(START)).thenReturn(Optional.of(previous));
        when(periods.findFirstByStartDateGreaterThanOrderByStartDateAsc(START)).thenReturn(Optional.of(next));
        stubStudents(AcademicYear.FOURTH_YEAR, AcademicYear.FOURTH_YEAR);

        var context = service.context();

        assertEquals(9, context.current().month());
        assertEquals(8, context.previous().month());
        assertEquals(10, context.next().month());
    }

    private ManagementPeriodRequest request(List<String> ids) {
        return new ManagementPeriodRequest(2026, 9, START, END, ids);
    }

    private void stubStudents(AcademicYear firstYear, AcademicYear secondYear) {
        Student one = student("one", firstYear);
        Student two = student("two", secondYear);
        when(students.findAllById(any())).thenReturn(List.of(one, two));
    }

    private Student student(String id, AcademicYear year) {
        Student student = mock(Student.class);
        when(student.getId()).thenReturn(id);
        when(student.getStudentId()).thenReturn("JIMC-" + id);
        when(student.getName()).thenReturn(id);
        when(student.getAcademicYear()).thenReturn(year);
        return student;
    }
}
