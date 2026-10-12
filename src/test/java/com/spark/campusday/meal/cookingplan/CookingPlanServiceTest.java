package com.spark.campusday.meal.cookingplan;

import com.spark.campusday.common.exception.InvalidBusinessStateException;
import com.spark.campusday.meal.absence.model.MealAbsenceNotice;
import com.spark.campusday.meal.absence.repository.MealAbsenceNoticeRepository;
import com.spark.campusday.meal.access.MealManagerAccess;
import com.spark.campusday.meal.cookingplan.service.CookingPlanService;
import com.spark.campusday.meal.menu.dto.MenuResponse;
import com.spark.campusday.meal.menu.model.DailyMealMenu;
import com.spark.campusday.meal.menu.model.MenuStatus;
import com.spark.campusday.meal.menu.service.MenuService;
import com.spark.campusday.meal.model.MealType;
import com.spark.campusday.meal.order.model.FinalYearMealOrder;
import com.spark.campusday.meal.order.model.MealChoiceKind;
import com.spark.campusday.meal.order.repository.FinalYearMealOrderRepository;
import com.spark.campusday.student.model.AcademicYear;
import com.spark.campusday.student.model.Student;
import com.spark.campusday.student.repository.StudentRepository;
import com.spark.campusday.student.service.StudentService;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class CookingPlanServiceTest {
    private static final LocalDate DATE = LocalDate.of(2026, 9, 25);
    private final StudentService studentService = mock(StudentService.class);
    private final StudentRepository students = mock(StudentRepository.class);
    private final MealAbsenceNoticeRepository absences = mock(MealAbsenceNoticeRepository.class);
    private final FinalYearMealOrderRepository orders = mock(FinalYearMealOrderRepository.class);
    private final MenuService menus = mock(MenuService.class);
    private final MealManagerAccess access = mock(MealManagerAccess.class);

    @Test
    void derivesBaselineAbsencesFullMealsAndIndividualQuantitiesWithoutCancelledDemand() {
        prepare(true);
        var plan = service().get(DATE, "M-1");

        assertEquals(3, plan.breakfast().yearsOneToFourBaseline());
        assertEquals(0, plan.breakfast().reportedAbsent());
        assertEquals(4, plan.breakfast().totalPackagesOrFullMeals());
        assertEquals(1, plan.lunch().reportedAbsent());
        assertEquals(2, plan.lunch().yearsOneToFourPlanned());
        assertEquals(1, plan.lunch().finalYearPackagesOrFullMeals());
        assertEquals(3, plan.lunch().totalPackagesOrFullMeals());
        assertEquals(5, plan.lunch().finalYearItems().getFirst().orderedQuantity());
        assertEquals(1, plan.dinner().reportedAbsent());
        assertEquals(1, plan.dinner().finalYearPackagesOrFullMeals());
        assertEquals(3, plan.dinner().finalYearItems().getFirst().orderedQuantity());
    }

    @Test
    void rejectsNonCurrentManagerBeforeReadingPlanningData() {
        prepare(false);
        assertThrows(InvalidBusinessStateException.class, () -> service().get(DATE, "M-1"));
        verifyNoInteractions(students, absences, orders, menus);
    }

    private CookingPlanService service() {
        return new CookingPlanService(studentService, students, absences, orders, menus, access);
    }

    private void prepare(boolean currentManager) {
        Student manager = student("manager-db");
        when(studentService.getByStudentId("M-1")).thenReturn(manager);
        when(access.hasCurrentAccess("manager-db")).thenReturn(currentManager);
        if (!currentManager) return;

        when(menus.getPublished(DATE)).thenReturn(menu());
        List<Student> fixed = List.of(student("s1"), student("s2"), student("s3"));
        List<Student> finalYear = List.of(student("f1"), student("f2"), student("f3"),
                student("f4"), student("f5"));
        when(students.findByAcademicYearIn(anyCollection())).thenReturn(fixed);
        when(students.findByAcademicYearOrderByNameAsc(AcademicYear.FINAL_YEAR)).thenReturn(finalYear);
        when(absences.findActiveForStudents(anyCollection(), org.mockito.ArgumentMatchers.eq(DATE)))
                .thenReturn(List.of(
                        new MealAbsenceNotice("s1", DATE, DATE, List.of(MealType.LUNCH), null),
                        new MealAbsenceNotice("s2", DATE, DATE, List.of(MealType.DINNER), null)));

        var first = order("f1", true, choice(MealChoiceKind.FULL_MEAL),
                choice(MealChoiceKind.ITEMS, item("fish", "Fish", 1)));
        var second = order("f2", false, choice(MealChoiceKind.ITEMS, item("rice", "Rice", 2)),
                choice(MealChoiceKind.ITEMS, item("fish", "Fish", 2)));
        var third = order("f3", false, choice(MealChoiceKind.ITEMS, item("rice", "Rice", 3)),
                choice(MealChoiceKind.FULL_MEAL));
        var cancelled = order("f4", true, choice(MealChoiceKind.FULL_MEAL),
                choice(MealChoiceKind.FULL_MEAL));
        cancelled.cancel();
        when(orders.findByStudentIdInAndMealDate(anyCollection(), org.mockito.ArgumentMatchers.eq(DATE)))
                .thenReturn(List.of(first, second, third, cancelled));
    }

    private Student student(String id) {
        Student student = mock(Student.class);
        when(student.getId()).thenReturn(id);
        return student;
    }

    private MenuResponse menu() {
        var rice = new DailyMealMenu.Item("rice", "Rice", null, "portion", true);
        var fish = new DailyMealMenu.Item("fish", "Fish", null, "piece", true);
        return new MenuResponse("menu", DATE, MenuStatus.PUBLISHED,
                new DailyMealMenu.Breakfast("Breakfast package", true, true),
                new DailyMealMenu.MainMeal("Lunch", true, true, true, "Full lunch", List.of(rice)),
                new DailyMealMenu.MainMeal("Dinner", true, true, true, "Full dinner", List.of(fish)),
                null, null);
    }

    private FinalYearMealOrder order(String studentId, boolean breakfast,
                                    FinalYearMealOrder.MealChoice lunch, FinalYearMealOrder.MealChoice dinner) {
        return new FinalYearMealOrder(studentId, DATE, "menu", breakfast, lunch, dinner);
    }

    private FinalYearMealOrder.MealChoice choice(MealChoiceKind kind, FinalYearMealOrder.SelectedItem... items) {
        return new FinalYearMealOrder.MealChoice(kind, List.of(items));
    }

    private FinalYearMealOrder.SelectedItem item(String id, String name, int quantity) {
        return new FinalYearMealOrder.SelectedItem(id, name, quantity);
    }
}
