package com.spark.campusday.meal.order;

import com.spark.campusday.common.exception.InvalidBusinessStateException;
import com.spark.campusday.meal.menu.dto.MenuResponse;
import com.spark.campusday.meal.menu.model.DailyMealMenu;
import com.spark.campusday.meal.menu.model.MenuStatus;
import com.spark.campusday.meal.menu.service.MenuService;
import com.spark.campusday.meal.order.dto.OrderRequest;
import com.spark.campusday.meal.order.model.FinalYearMealOrder;
import com.spark.campusday.meal.order.model.MealChoiceKind;
import com.spark.campusday.meal.order.model.OrderCutoffConfiguration;
import com.spark.campusday.meal.order.model.OrderStatus;
import com.spark.campusday.meal.order.repository.FinalYearMealOrderRepository;
import com.spark.campusday.meal.order.repository.OrderCutoffConfigurationRepository;
import com.spark.campusday.meal.order.service.FinalYearOrderService;
import com.spark.campusday.meal.order.service.OrderCutoffService;
import com.spark.campusday.student.model.AcademicYear;
import com.spark.campusday.student.model.Student;
import com.spark.campusday.student.service.StudentService;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FinalYearOrderServiceTest {
    private static final LocalDate DATE = LocalDate.of(2026, 9, 25);
    private final FinalYearMealOrderRepository orders = mock(FinalYearMealOrderRepository.class);
    private final StudentService students = mock(StudentService.class);
    private final MenuService menus = mock(MenuService.class);
    private final OrderCutoffConfigurationRepository configs = mock(OrderCutoffConfigurationRepository.class);

    @Test
    void submitsFinalYearOrderWithoutAnyPrice() {
        FinalYearOrderService service = service("2026-09-24T04:00:00Z", AcademicYear.FINAL_YEAR);
        var result = service.create("FY-1", request("rice", 2));

        assertEquals(OrderStatus.SUBMITTED, result.status());
        assertEquals(2, result.lunch().items().getFirst().quantity());
        assertEquals("Rice", result.lunch().items().getFirst().name());
    }

    @Test
    void rejectsNonFinalYearAndUnavailableItem() {
        FinalYearOrderService regular = service("2026-09-24T04:00:00Z", AcademicYear.FOURTH_YEAR);
        assertThrows(InvalidBusinessStateException.class, () -> regular.create("FY-1", request("rice", 1)));

        FinalYearOrderService finalYear = service("2026-09-24T04:00:00Z", AcademicYear.FINAL_YEAR);
        assertThrows(InvalidBusinessStateException.class, () -> finalYear.create("FY-1", request("missing", 1)));
        assertThrows(InvalidBusinessStateException.class, () -> finalYear.create("FY-1", request("rice", 0)));
    }

    @Test
    void updatesAndCancelsBeforeCutoff() {
        FinalYearOrderService service = service("2026-09-24T04:00:00Z", AcademicYear.FINAL_YEAR);
        FinalYearMealOrder existing = existing();
        when(orders.findById("order")).thenReturn(Optional.of(existing));

        assertEquals(3, service.update("FY-1", "order", request("rice", 3))
                .lunch().items().getFirst().quantity());
        assertEquals(OrderStatus.CANCELLED, service.cancel("FY-1", "order").status());
        assertThrows(InvalidBusinessStateException.class, () -> service.cancel("FY-1", "order"));
    }

    @Test
    void rejectsCreateUpdateAndCancelAfterTheirCutoffs() {
        FinalYearOrderService service = service("2026-09-24T17:00:00Z", AcademicYear.FINAL_YEAR);
        when(orders.findById("order")).thenReturn(Optional.of(existing()));

        assertThrows(InvalidBusinessStateException.class, () -> service.create("FY-1", request("rice", 1)));
        assertThrows(InvalidBusinessStateException.class,
                () -> service.update("FY-1", "order", request("rice", 1)));
        assertThrows(InvalidBusinessStateException.class, () -> service.cancel("FY-1", "order"));
    }

    private FinalYearOrderService service(String now, AcademicYear year) {
        Student student = mock(Student.class);
        when(student.getId()).thenReturn("db-student");
        when(student.getAcademicYear()).thenReturn(year);
        when(students.getByStudentId("FY-1")).thenReturn(student);
        var rule = new OrderCutoffConfiguration.CutoffRule(1, LocalTime.of(22, 30));
        when(configs.findById(OrderCutoffConfiguration.ID))
                .thenReturn(Optional.of(new OrderCutoffConfiguration(rule, rule, rule)));
        when(menus.getPublished(DATE)).thenReturn(menu());
        when(orders.save(any(FinalYearMealOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));
        Clock clock = Clock.fixed(Instant.parse(now), ZoneId.of("Asia/Dhaka"));
        return new FinalYearOrderService(orders, students, menus, new OrderCutoffService(configs, clock));
    }

    private FinalYearMealOrder existing() {
        return new FinalYearMealOrder("db-student", DATE, "menu", false,
                new FinalYearMealOrder.MealChoice(MealChoiceKind.FULL_MEAL, List.of()),
                new FinalYearMealOrder.MealChoice(MealChoiceKind.NOT_EATING, List.of()));
    }

    private OrderRequest request(String itemId, int quantity) {
        return new OrderRequest(DATE, false,
                new OrderRequest.MealChoiceRequest(MealChoiceKind.ITEMS,
                        List.of(new OrderRequest.ItemQuantity(itemId, quantity))),
                new OrderRequest.MealChoiceRequest(MealChoiceKind.NOT_EATING, List.of()));
    }

    private MenuResponse menu() {
        var breakfast = new DailyMealMenu.Breakfast("Package", true, true);
        var lunch = new DailyMealMenu.MainMeal("Lunch", true, true, true, "Full lunch",
                List.of(new DailyMealMenu.Item("rice", "Rice", null, null, true)));
        var dinner = new DailyMealMenu.MainMeal("Dinner", true, true, true, "Full dinner", List.of());
        return new MenuResponse("menu", DATE, MenuStatus.PUBLISHED, breakfast, lunch, dinner, null, null);
    }
}
