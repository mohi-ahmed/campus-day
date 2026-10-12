package com.spark.campusday.meal.config;

import com.spark.campusday.meal.access.MealManagerAccess;
import com.spark.campusday.meal.menu.dto.MenuResponse;
import com.spark.campusday.meal.menu.model.DailyMealMenu;
import com.spark.campusday.meal.menu.model.MenuStatus;
import com.spark.campusday.meal.menu.repository.DailyMealMenuRepository;
import com.spark.campusday.meal.menu.service.MenuService;
import com.spark.campusday.meal.order.repository.FinalYearMealOrderRepository;
import com.spark.campusday.meal.price.dto.SetPriceRequest;
import com.spark.campusday.meal.price.model.MealPriceVersion;
import com.spark.campusday.meal.price.model.PriceTarget;
import com.spark.campusday.meal.price.repository.MealPriceVersionRepository;
import com.spark.campusday.meal.price.service.MealPriceService;
import com.spark.campusday.student.model.Student;
import com.spark.campusday.student.service.StudentService;
import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MealReadCacheTest {
    private static final LocalDate DATE = LocalDate.of(2026, 9, 25);

    @Test
    void publishedMenuReadUsesProxyCacheAndPublishEvictsItsDate() {
        DailyMealMenu published = menu();
        published.publish();
        LocalDate nextDate = DATE.plusDays(1);
        DailyMealMenu draft = menu(nextDate);
        DailyMealMenuRepository repository = mock(DailyMealMenuRepository.class);
        when(repository.findByMealDateAndStatus(DATE, MenuStatus.PUBLISHED))
                .thenReturn(Optional.of(published));
        when(repository.findByMealDateAndStatus(nextDate, MenuStatus.PUBLISHED))
                .thenReturn(Optional.of(draft));
        when(repository.findById("draft")).thenReturn(Optional.of(draft));
        when(repository.save(draft)).thenReturn(draft);

        try (var context = new AnnotationConfigApplicationContext()) {
            context.register(MealReadCacheConfiguration.class, TestCachingConfiguration.class);
            context.registerBean(MenuService.class, () -> new MenuService(repository,
                    mock(FinalYearMealOrderRepository.class), mock(MealPriceVersionRepository.class)));
            context.refresh();
            MenuService service = context.getBean(MenuService.class);

            service.getPublished(DATE);
            service.getPublished(DATE);
            verify(repository, times(1)).findByMealDateAndStatus(DATE, MenuStatus.PUBLISHED);

            CacheManager caches = context.getBean(CacheManager.class);
            caches.getCache(MealReadCacheConfiguration.PUBLISHED_MENUS)
                    .put(nextDate, menuResponse(nextDate));
            service.publish("draft");
            assertNull(caches.getCache(MealReadCacheConfiguration.PUBLISHED_MENUS).get(nextDate));
            assertNotNull(caches.getCache(MealReadCacheConfiguration.PUBLISHED_MENUS).get(DATE));
            service.getPublished(nextDate);
            verify(repository, times(1)).findByMealDateAndStatus(nextDate, MenuStatus.PUBLISHED);
        }
    }

    @Test
    void applicablePriceReadUsesProxyCacheAndSuccessfulSaveRefreshesPendingPrice() {
        MealPriceVersionRepository prices = mock(MealPriceVersionRepository.class);
        MenuService menus = mock(MenuService.class);
        StudentService students = mock(StudentService.class);
        MealManagerAccess access = mock(MealManagerAccess.class);
        Student manager = mock(Student.class);
        when(manager.getId()).thenReturn("manager-id");
        when(students.getByStudentId("MANAGER-1")).thenReturn(manager);
        when(access.hasCurrentAccess("manager-id")).thenReturn(true);
        when(menus.getPublished(DATE)).thenReturn(menuResponse());
        AtomicReference<List<MealPriceVersion>> stored = new AtomicReference<>(List.of());
        when(prices.findForMenuNewestFirst("menu-id")).thenAnswer(call -> stored.get());
        when(prices.save(any(MealPriceVersion.class))).thenAnswer(call -> {
            MealPriceVersion saved = call.getArgument(0);
            stored.set(List.of(saved));
            return saved;
        });
        Clock clock = Clock.fixed(Instant.parse("2026-09-24T10:00:00Z"), ZoneId.of("Asia/Dhaka"));

        try (var context = new AnnotationConfigApplicationContext()) {
            context.register(MealReadCacheConfiguration.class, TestCachingConfiguration.class);
            context.registerBean(MealPriceService.class,
                    () -> new MealPriceService(prices, menus, students, access, clock));
            context.refresh();
            MealPriceService service = context.getBean(MealPriceService.class);

            assertTrue(service.applicable(DATE).prices().getFirst().pricePending());
            service.applicable(DATE);
            verify(prices, times(1)).findForMenuNewestFirst("menu-id");

            service.setPrice("MANAGER-1", new SetPriceRequest(DATE, PriceTarget.BREAKFAST_PACKAGE,
                    null, new BigDecimal("20.00")));
            assertNull(context.getBean(CacheManager.class)
                    .getCache(MealReadCacheConfiguration.APPLICABLE_MEAL_PRICES).get(DATE));
            var refreshed = service.applicable(DATE).prices().getFirst();
            assertEquals(new BigDecimal("20.00"), refreshed.amount());
            assertNotNull(refreshed.effectiveAt());
            verify(prices, times(2)).findForMenuNewestFirst("menu-id");
        }
    }

    private DailyMealMenu menu() {
        return menu(DATE);
    }

    private DailyMealMenu menu(LocalDate date) {
        return new DailyMealMenu(date,
                new DailyMealMenu.Breakfast("Package", true, true),
                new DailyMealMenu.MainMeal("Lunch", true, false, false, null, List.of()),
                new DailyMealMenu.MainMeal("Dinner", true, false, false, null, List.of()));
    }

    private MenuResponse menuResponse() {
        return menuResponse(DATE);
    }

    private MenuResponse menuResponse(LocalDate date) {
        DailyMealMenu published = menu(date);
        published.publish();
        return new MenuResponse("menu-id", date, MenuStatus.PUBLISHED, published.getBreakfast(),
                published.getLunch(), published.getDinner(), null, null);
    }

    @Configuration
    @EnableCaching
    static class TestCachingConfiguration {}
}
