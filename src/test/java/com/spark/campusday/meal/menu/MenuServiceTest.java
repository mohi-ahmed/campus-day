package com.spark.campusday.meal.menu;

import com.spark.campusday.common.exception.ConflictException;
import com.spark.campusday.common.exception.InvalidBusinessStateException;
import com.spark.campusday.common.exception.ResourceNotFoundException;
import com.spark.campusday.meal.menu.dto.MenuRequest;
import com.spark.campusday.meal.menu.model.DailyMealMenu;
import com.spark.campusday.meal.menu.model.MenuStatus;
import com.spark.campusday.meal.menu.repository.DailyMealMenuRepository;
import com.spark.campusday.meal.menu.service.MenuService;
import com.spark.campusday.meal.order.model.OrderStatus;
import com.spark.campusday.meal.order.repository.FinalYearMealOrderRepository;
import com.spark.campusday.meal.price.repository.MealPriceVersionRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MenuServiceTest {
    private static final LocalDate DATE = LocalDate.of(2026, 9, 24);
    private final DailyMealMenuRepository repository = mock(DailyMealMenuRepository.class);
    private final FinalYearMealOrderRepository orders = mock(FinalYearMealOrderRepository.class);
    private final MealPriceVersionRepository prices = mock(MealPriceVersionRepository.class);
    private final MenuService service = new MenuService(repository, orders, prices);

    @Test
    void createsDraftWithBreakfastPackageAndSelectableLunchDinnerItems() {
        when(repository.save(any(DailyMealMenu.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var created = service.create(request());

        assertEquals(MenuStatus.DRAFT, created.status());
        assertEquals("Bread and egg", created.breakfast().packageDescription());
        assertTrue(created.lunch().fullMealAvailable());
        assertEquals("Rice", created.lunch().items().getFirst().name());
        assertFalse(created.dinner().fullMealAvailable());
        assertEquals(1, created.dinner().items().size());
    }

    @Test
    void rejectsDuplicateDate() {
        when(repository.existsByMealDate(DATE)).thenReturn(true);
        assertThrows(ConflictException.class, () -> service.create(request()));
    }

    @Test
    void rejectsDuplicateItemsAndContradictoryAvailability() {
        MenuRequest input = request();
        var duplicateLunch = new MenuRequest.MainMealRequest("Rice and curry", true, true, true,
                "Rice and curry", List.of(item("Rice"), item("rice")));
        assertThrows(InvalidBusinessStateException.class, () -> service.create(
                new MenuRequest(DATE, input.breakfast(), duplicateLunch, input.dinner())));
        assertThrows(InvalidBusinessStateException.class, () -> service.create(
                new MenuRequest(DATE, new MenuRequest.BreakfastRequest("Package", false, true),
                        input.lunch(), input.dinner())));
    }

    @Test
    void publishesAndReadsOnlyPublishedMenu() {
        DailyMealMenu menu = new DailyMealMenu(DATE,
                new DailyMealMenu.Breakfast("Bread and egg", true, true),
                new DailyMealMenu.MainMeal("Lunch", true, true, true, "Rice", List.of()),
                new DailyMealMenu.MainMeal("Dinner", true, true, false, null, List.of()));
        when(repository.findById("menu")).thenReturn(Optional.of(menu));
        when(repository.save(menu)).thenReturn(menu);
        when(repository.findByMealDateAndStatus(DATE, MenuStatus.PUBLISHED)).thenReturn(Optional.of(menu));

        assertEquals(MenuStatus.PUBLISHED, service.publish("menu").status());
        assertEquals(DATE, service.getPublished(DATE).mealDate());
        assertThrows(InvalidBusinessStateException.class, () -> service.publish("menu"));
        when(orders.existsByMenuIdAndStatus("menu", OrderStatus.SUBMITTED)).thenReturn(true);
        assertThrows(InvalidBusinessStateException.class, () -> service.update("menu", request()));
        assertThrows(ResourceNotFoundException.class, () -> service.getPublished(DATE.plusDays(1)));
    }

    private MenuRequest request() {
        return new MenuRequest(DATE, new MenuRequest.BreakfastRequest("Bread and egg", true, true),
                new MenuRequest.MainMealRequest("Rice and curry", true, true, true, "Rice and curry",
                        List.of(item("Rice"))),
                new MenuRequest.MainMealRequest("Dinner items", true, true, false, null,
                        List.of(item("Fish"))));
    }

    private MenuRequest.ItemRequest item(String name) {
        return new MenuRequest.ItemRequest(null, name, null, null, true);
    }
}
