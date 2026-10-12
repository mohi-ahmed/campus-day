package com.spark.campusday.meal.price;

import com.spark.campusday.meal.managementperiod.dto.ManagementPeriodRequest;
import com.spark.campusday.meal.managementperiod.repository.MealManagementPeriodRepository;
import com.spark.campusday.meal.managementperiod.service.ManagementPeriodService;
import com.spark.campusday.meal.menu.dto.MenuRequest;
import com.spark.campusday.meal.menu.repository.DailyMealMenuRepository;
import com.spark.campusday.meal.menu.service.MenuService;
import com.spark.campusday.meal.price.dto.SetPriceRequest;
import com.spark.campusday.meal.price.model.MealPriceVersion;
import com.spark.campusday.meal.price.model.PriceTarget;
import com.spark.campusday.meal.price.repository.MealPriceVersionRepository;
import com.spark.campusday.meal.price.service.MealPriceService;
import com.spark.campusday.student.model.AcademicYear;
import com.spark.campusday.student.repository.StudentRepository;
import com.spark.campusday.student.service.StudentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@EnabledIfEnvironmentVariable(named = "CAMPUS_DAY_MONGO_TEST_URI", matches = ".+")
@SpringBootTest(properties = {
        "spring.mongodb.database=CampusDayPart6Test",
        "spring.data.mongodb.auto-index-creation=true"
})
class MealPricePersistenceIntegrationTest {
    @DynamicPropertySource
    static void mongo(DynamicPropertyRegistry registry) {
        registry.add("spring.mongodb.uri", () -> System.getenv("CAMPUS_DAY_MONGO_TEST_URI"));
    }

    @Autowired private StudentService students;
    @Autowired private StudentRepository studentRepository;
    @Autowired private ManagementPeriodService periods;
    @Autowired private MealManagementPeriodRepository periodRepository;
    @Autowired private MenuService menus;
    @Autowired private DailyMealMenuRepository menuRepository;
    @Autowired private MealPriceService prices;
    @Autowired private MealPriceVersionRepository priceRepository;
    @Autowired private MongoTemplate mongo;

    @Test
    void assignedCurrentManagerSavesImmediatelyApplicablePrice() {
        String suffix = UUID.randomUUID().toString();
        var first = students.createStudent("PART6-A-" + suffix, "First Manager",
                "part6-a-" + suffix + "@campusday.test", AcademicYear.FOURTH_YEAR);
        var second = students.createStudent("PART6-B-" + suffix, "Second Manager",
                "part6-b-" + suffix + "@campusday.test", AcademicYear.FOURTH_YEAR);
        String periodId = null;
        String menuId = null;
        try {
            YearMonth month = YearMonth.from(LocalDate.now(ZoneId.of("Asia/Dhaka")));
            periodId = periods.create(new ManagementPeriodRequest(month.getYear(), month.getMonthValue(),
                    month.atDay(1), month.atEndOfMonth(), List.of(first.getId(), second.getId()))).id();
            LocalDate date = month.atDay(1);
            var createdMenu = menus.create(new MenuRequest(date,
                    new MenuRequest.BreakfastRequest("Package", true, true),
                    new MenuRequest.MainMealRequest("Lunch", true, true, true, "Full lunch",
                            List.of(new MenuRequest.ItemRequest(null, "Rice", null, null, true))),
                    new MenuRequest.MainMealRequest("Dinner", true, true, true, "Full dinner", List.of())));
            menuId = createdMenu.id();
            var menu = menus.publish(menuId);
            String itemId = menu.lunch().items().getFirst().id();

            var saved = prices.setPrice(first.getStudentId(),
                    new SetPriceRequest(date, PriceTarget.LUNCH_MENU_ITEM, itemId, new BigDecimal("20.50")));
            var lookup = prices.applicable(date);

            assertNotNull(priceRepository.findById(saved.id()).orElseThrow().getCreatedAt());
            assertEquals(new BigDecimal("20.50"), lookup.prices().stream()
                    .filter(entry -> entry.target() == PriceTarget.LUNCH_MENU_ITEM)
                    .findFirst().orElseThrow().amount());
            assertTrue(lookup.prices().stream().anyMatch(entry -> entry.target() == PriceTarget.BREAKFAST_PACKAGE
                    && entry.pricePending()));
            assertEquals(1, prices.history(date, PriceTarget.LUNCH_MENU_ITEM, itemId).size());
            var changed = prices.setPrice(second.getStudentId(),
                    new SetPriceRequest(date, PriceTarget.LUNCH_MENU_ITEM, itemId, new BigDecimal("21.00")));
            assertEquals(2, prices.history(date, PriceTarget.LUNCH_MENU_ITEM, itemId).size());
            assertEquals(new BigDecimal("20.50"), prices.applicableAsOf(date, PriceTarget.LUNCH_MENU_ITEM,
                    itemId, saved.effectiveAt()).orElseThrow().amount());
            assertEquals(new BigDecimal("21.00"), prices.applicable(date).prices().stream()
                    .filter(entry -> entry.target() == PriceTarget.LUNCH_MENU_ITEM).findFirst().orElseThrow().amount());
            assertNotNull(priceRepository.findById(changed.id()).orElseThrow().getCreatedAt());
            assertTrue(mongo.indexOps(MealPriceVersion.class).getIndexInfo().stream()
                    .anyMatch(index -> index.isUnique() && index.getName().equals("price_target_versions")));
        } finally {
            if (menuId != null) {
                priceRepository.deleteAll(priceRepository.findForMenuNewestFirst(menuId));
                menuRepository.deleteById(menuId);
            }
            if (periodId != null) periodRepository.deleteById(periodId);
            studentRepository.deleteAllById(List.of(first.getId(), second.getId()));
        }
    }
}
