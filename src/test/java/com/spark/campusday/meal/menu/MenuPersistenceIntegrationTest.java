package com.spark.campusday.meal.menu;

import com.spark.campusday.common.exception.ConflictException;
import com.spark.campusday.meal.menu.dto.MenuRequest;
import com.spark.campusday.meal.menu.model.DailyMealMenu;
import com.spark.campusday.meal.menu.model.MenuStatus;
import com.spark.campusday.meal.menu.repository.DailyMealMenuRepository;
import com.spark.campusday.meal.menu.service.MenuService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@EnabledIfEnvironmentVariable(named = "CAMPUS_DAY_MONGO_TEST_URI", matches = ".+")
@SpringBootTest(properties = {
        "spring.mongodb.database=CampusDayPart3Test",
        "spring.data.mongodb.auto-index-creation=true"
})
class MenuPersistenceIntegrationTest {
    @DynamicPropertySource
    static void mongo(DynamicPropertyRegistry registry) {
        registry.add("spring.mongodb.uri", () -> System.getenv("CAMPUS_DAY_MONGO_TEST_URI"));
    }

    @Autowired private MenuService service;
    @Autowired private DailyMealMenuRepository menus;
    @Autowired private MongoTemplate mongoTemplate;

    @Test
    void persistsPublishedMenuWithUniqueDateAndAuditFields() {
        LocalDate date = LocalDate.of(2040, 1, 17);
        String id = null;
        try {
            MenuRequest request = new MenuRequest(date,
                    new MenuRequest.BreakfastRequest("Bread and egg", true, true),
                    new MenuRequest.MainMealRequest("Lunch", true, true, true, "Rice and dal", List.of()),
                    new MenuRequest.MainMealRequest("Dinner", true, true, false, null,
                            List.of(new MenuRequest.ItemRequest(null, "Fish", null, null, true))));
            var created = service.create(request);
            id = created.id();
            assertTrue(service.publishedBetween(date, date).isEmpty());
            var published = service.publish(id);

            DailyMealMenu stored = menus.findById(id).orElseThrow();
            assertEquals(MenuStatus.PUBLISHED, published.status());
            assertEquals("Fish", service.getPublished(date).dinner().items().getFirst().name());
            assertEquals(1, service.publishedBetween(date, date).size());
            assertNotNull(stored.getCreatedAt());
            assertNotNull(stored.getUpdatedAt());
            assertThrows(ConflictException.class, () -> service.create(request));
            assertTrue(mongoTemplate.indexOps(DailyMealMenu.class).getIndexInfo().stream()
                    .anyMatch(index -> index.isUnique() && index.getIndexFields().stream()
                            .anyMatch(field -> field.getKey().equals("mealDate"))));
        } finally {
            if (id != null) menus.deleteById(id);
        }
    }
}
