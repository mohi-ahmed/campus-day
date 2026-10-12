package com.spark.campusday.meal.price;

import com.spark.campusday.common.exception.InvalidBusinessStateException;
import com.spark.campusday.meal.access.MealManagerAccess;
import com.spark.campusday.meal.menu.dto.MenuResponse;
import com.spark.campusday.meal.menu.model.DailyMealMenu;
import com.spark.campusday.meal.menu.model.MenuStatus;
import com.spark.campusday.meal.menu.service.MenuService;
import com.spark.campusday.meal.price.dto.SetPriceRequest;
import com.spark.campusday.meal.price.model.MealPriceVersion;
import com.spark.campusday.meal.price.model.PriceTarget;
import com.spark.campusday.meal.price.repository.MealPriceVersionRepository;
import com.spark.campusday.meal.price.service.MealPriceService;
import com.spark.campusday.student.model.Student;
import com.spark.campusday.student.service.StudentService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class MealPriceServiceTest {
    private static final LocalDate DATE = LocalDate.of(2026, 9, 25);
    private final MealPriceVersionRepository prices = mock(MealPriceVersionRepository.class);
    private final MenuService menus = mock(MenuService.class);
    private final StudentService students = mock(StudentService.class);
    private final MealManagerAccess access = mock(MealManagerAccess.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-09-24T10:00:00Z"), ZoneId.of("Asia/Dhaka"));
    private final MealPriceService service = new MealPriceService(prices, menus, students, access, clock);

    @Test
    void assignedManagerSetsApplicablePriceImmediately() {
        authorized();
        when(prices.save(any(MealPriceVersion.class))).thenAnswer(call -> call.getArgument(0));
        var saved = service.setPrice("MANAGER-1", request(PriceTarget.LUNCH_MENU_ITEM, "rice", "20.50"));
        when(prices.findForMenuNewestFirst("menu")).thenReturn(List.of(version("rice", "20.50", saved.effectiveAt())));

        assertEquals(new BigDecimal("20.50"), saved.amount());
        var rice = service.applicable(DATE).prices().stream()
                .filter(entry -> entry.target() == PriceTarget.LUNCH_MENU_ITEM).findFirst().orElseThrow();
        assertEquals(new BigDecimal("20.50"), rice.amount());
        assertTrue(!rice.pricePending());
    }

    @Test
    void fourthYearWithoutCurrentAssignmentCannotSetPrice() {
        Student student = mock(Student.class);
        when(student.getId()).thenReturn("unassigned-fourth-year");
        when(students.getByStudentId("MANAGER-1")).thenReturn(student);
        when(access.hasCurrentAccess("unassigned-fourth-year")).thenReturn(false);

        assertThrows(InvalidBusinessStateException.class,
                () -> service.setPrice("MANAGER-1", request(PriceTarget.LUNCH_MENU_ITEM, "rice", "20.00")));
        verifyNoInteractions(menus, prices);
    }

    @Test
    void rejectsInvalidAmountAndIndividualBreakfastReference() {
        authorized();
        assertThrows(InvalidBusinessStateException.class,
                () -> service.setPrice("MANAGER-1", request(PriceTarget.LUNCH_MENU_ITEM, "rice", "-1.00")));
        assertThrows(InvalidBusinessStateException.class,
                () -> service.setPrice("MANAGER-1", request(PriceTarget.LUNCH_MENU_ITEM, "rice", "1.234")));
        assertThrows(InvalidBusinessStateException.class,
                () -> service.setPrice("MANAGER-1", request(PriceTarget.BREAKFAST_PACKAGE, "rice", "10.00")));
        assertThrows(InvalidBusinessStateException.class,
                () -> service.setPrice("MANAGER-1", request(PriceTarget.DINNER_MENU_ITEM, "rice", "10.00")));
    }

    @Test
    void missingPriceIsPendingInsteadOfAnError() {
        when(menus.getPublished(DATE)).thenReturn(menu());
        var response = service.applicable(DATE);

        assertEquals(5, response.prices().size());
        assertTrue(response.prices().stream().allMatch(entry -> entry.pricePending() && entry.amount() == null));
    }

    @Test
    void oldRevisionRemainsAvailableAfterNewPrice() {
        authorized();
        Instant earlier = Instant.parse("2026-09-23T10:00:00Z");
        MealPriceVersion previous = version("rice", "20.00", earlier);
        when(prices.findLatest("menu", PriceTarget.LUNCH_MENU_ITEM, "rice"))
                .thenReturn(Optional.of(previous));
        when(prices.save(any(MealPriceVersion.class))).thenAnswer(call -> call.getArgument(0));
        var saved = service.setPrice("MANAGER-1", request(PriceTarget.LUNCH_MENU_ITEM, "rice", "22.00"));
        when(prices.findHistory("menu", PriceTarget.LUNCH_MENU_ITEM, "rice"))
                .thenReturn(List.of(version("rice", "22.00", saved.effectiveAt()), previous));
        when(prices.findAsOf("menu", PriceTarget.LUNCH_MENU_ITEM, "rice", earlier))
                .thenReturn(Optional.of(previous));

        assertEquals(List.of(new BigDecimal("22.00"), new BigDecimal("20.00")),
                service.history(DATE, PriceTarget.LUNCH_MENU_ITEM, "rice").stream()
                        .map(price -> price.amount()).toList());
        assertEquals(new BigDecimal("20.00"),
                service.applicableAsOf(DATE, PriceTarget.LUNCH_MENU_ITEM, "rice", earlier)
                        .orElseThrow().amount());
        assertNull(service.applicable(DATE).prices().stream()
                .filter(entry -> entry.target() == PriceTarget.LUNCH_MENU_ITEM)
                .findFirst().orElseThrow().amount());
    }

    private void authorized() {
        Student student = mock(Student.class);
        when(student.getId()).thenReturn("db-manager");
        when(students.getByStudentId("MANAGER-1")).thenReturn(student);
        when(access.hasCurrentAccess("db-manager")).thenReturn(true);
        when(menus.getPublished(DATE)).thenReturn(menu());
    }

    private SetPriceRequest request(PriceTarget target, String itemId, String amount) {
        return new SetPriceRequest(DATE, target, itemId, new BigDecimal(amount));
    }

    private MealPriceVersion version(String itemId, String amount, Instant effectiveAt) {
        return new MealPriceVersion("menu", DATE, PriceTarget.LUNCH_MENU_ITEM, itemId,
                new BigDecimal(amount), "db-manager", effectiveAt);
    }

    private MenuResponse menu() {
        return new MenuResponse("menu", DATE, MenuStatus.PUBLISHED,
                new DailyMealMenu.Breakfast("Package", true, true),
                new DailyMealMenu.MainMeal("Lunch", true, true, true, "Full lunch",
                        List.of(new DailyMealMenu.Item("rice", "Rice", null, null, true))),
                new DailyMealMenu.MainMeal("Dinner", true, true, true, "Full dinner",
                        List.of(new DailyMealMenu.Item("fish", "Fish", null, null, true))), null, null);
    }
}
