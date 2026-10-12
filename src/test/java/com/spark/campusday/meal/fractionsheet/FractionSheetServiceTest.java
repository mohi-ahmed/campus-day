package com.spark.campusday.meal.fractionsheet;

import com.spark.campusday.common.exception.InvalidBusinessStateException;
import com.spark.campusday.meal.access.MealManagerAccess;
import com.spark.campusday.meal.fractionsheet.dto.FractionSheetResponse.RowStatus;
import com.spark.campusday.meal.fractionsheet.service.FractionSheetService;
import com.spark.campusday.meal.order.dto.OrderCutoffResponse;
import com.spark.campusday.meal.order.model.FinalYearMealOrder;
import com.spark.campusday.meal.order.model.MealChoiceKind;
import com.spark.campusday.meal.order.repository.FinalYearMealOrderRepository;
import com.spark.campusday.meal.order.service.OrderCutoffService;
import com.spark.campusday.student.model.AcademicYear;
import com.spark.campusday.student.model.Student;
import com.spark.campusday.student.repository.StudentRepository;
import com.spark.campusday.student.service.StudentService;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class FractionSheetServiceTest {
    private static final LocalDate DATE = LocalDate.of(2026, 9, 25);
    private final StudentService studentService = mock(StudentService.class);
    private final StudentRepository students = mock(StudentRepository.class);
    private final FinalYearMealOrderRepository orders = mock(FinalYearMealOrderRepository.class);
    private final OrderCutoffService cutoffs = mock(OrderCutoffService.class);
    private final MealManagerAccess access = mock(MealManagerAccess.class);

    @Test
    void submittedCancelledAndNoOrderRowsProduceOperationalTotals() {
        var sheet = service("2026-09-24T06:00:00Z", true).get(DATE, "M-1");

        assertEquals(List.of(RowStatus.SUBMITTED, RowStatus.SUBMITTED, RowStatus.CANCELLED,
                RowStatus.NOT_EATING), sheet.students().stream().map(row -> row.status()).toList());
        assertEquals(2, sheet.totals().submitted());
        assertEquals(1, sheet.totals().cancelled());
        assertEquals(1, sheet.totals().notEating());
        assertEquals(1, sheet.totals().breakfastPackages());
        assertEquals(1, sheet.totals().lunchFullMeals());
        assertEquals(0, sheet.totals().dinnerFullMeals());
        assertEquals(3, sheet.totals().dinnerItems().getFirst().quantity());
        assertEquals(2, sheet.totals().lunchItems().getFirst().quantity());
        assertEquals(false, sheet.students().get(2).breakfast());
    }

    @Test
    void submittedRowsLockAfterEditAndCancelCutoffsWithoutPersistedStatus() {
        var sheet = service("2026-09-24T13:00:00Z", true).get(DATE, "M-1");

        assertEquals(RowStatus.LOCKED, sheet.students().getFirst().status());
        assertEquals(2, sheet.totals().locked());
    }

    @Test
    void rejectsStudentWithoutCurrentManagerAssignment() {
        assertThrows(InvalidBusinessStateException.class,
                () -> service("2026-09-24T06:00:00Z", false).get(DATE, "M-1"));
        verifyNoInteractions(students, orders);
    }

    private FractionSheetService service(String now, boolean managerAssigned) {
        Student manager = student("manager-db", "M-1", "Manager");
        when(studentService.getByStudentId("M-1")).thenReturn(manager);
        when(access.hasCurrentAccess("manager-db")).thenReturn(managerAssigned);
        if (managerAssigned) {
            var roster = List.of(
                    student("s1", "FY-1", "A"), student("s2", "FY-2", "B"),
                    student("s3", "FY-3", "C"), student("s4", "FY-4", "D"));
            when(students.findByAcademicYearOrderByNameAsc(AcademicYear.FINAL_YEAR)).thenReturn(roster);
            var rice = new FinalYearMealOrder.SelectedItem("rice", "Rice", 2);
            var fish = new FinalYearMealOrder.SelectedItem("fish", "Fish", 2);
            var first = order("s1", true, choice(MealChoiceKind.FULL_MEAL),
                    choice(MealChoiceKind.ITEMS, fish));
            var second = order("s2", false, choice(MealChoiceKind.ITEMS, rice),
                    choice(MealChoiceKind.ITEMS, new FinalYearMealOrder.SelectedItem("fish", "Fish", 1)));
            var cancelled = order("s3", true, choice(MealChoiceKind.FULL_MEAL),
                    choice(MealChoiceKind.FULL_MEAL));
            cancelled.cancel();
            when(orders.findByStudentIdInAndMealDate(anyCollection(), org.mockito.ArgumentMatchers.eq(DATE)))
                    .thenReturn(List.of(first, second, cancelled));
            when(cutoffs.get(DATE)).thenReturn(new OrderCutoffResponse(DATE, null, null, null,
                    null, Instant.parse("2026-09-24T05:00:00Z"),
                    Instant.parse("2026-09-24T12:00:00Z"), null));
        }
        return new FractionSheetService(studentService, students, orders, cutoffs, access,
                Clock.fixed(Instant.parse(now), ZoneId.of("Asia/Dhaka")));
    }

    private Student student(String id, String registrationId, String name) {
        Student student = mock(Student.class);
        when(student.getId()).thenReturn(id);
        if (!registrationId.equals("M-1")) {
            when(student.getStudentId()).thenReturn(registrationId);
            when(student.getName()).thenReturn(name);
        }
        return student;
    }

    private FinalYearMealOrder order(String studentId, boolean breakfast,
                                    FinalYearMealOrder.MealChoice lunch,
                                    FinalYearMealOrder.MealChoice dinner) {
        return new FinalYearMealOrder(studentId, DATE, "menu", breakfast, lunch, dinner);
    }

    private FinalYearMealOrder.MealChoice choice(MealChoiceKind kind, FinalYearMealOrder.SelectedItem... items) {
        return new FinalYearMealOrder.MealChoice(kind, List.of(items));
    }
}
