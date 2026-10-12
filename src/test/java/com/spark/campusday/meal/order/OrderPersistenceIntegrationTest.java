package com.spark.campusday.meal.order;

import com.spark.campusday.common.exception.ConflictException;
import com.spark.campusday.meal.menu.dto.MenuRequest;
import com.spark.campusday.meal.menu.repository.DailyMealMenuRepository;
import com.spark.campusday.meal.menu.service.MenuService;
import com.spark.campusday.meal.order.dto.OrderCutoffRequest;
import com.spark.campusday.meal.order.dto.OrderRequest;
import com.spark.campusday.meal.order.model.FinalYearMealOrder;
import com.spark.campusday.meal.order.model.MealChoiceKind;
import com.spark.campusday.meal.order.model.OrderCutoffConfiguration;
import com.spark.campusday.meal.order.repository.FinalYearMealOrderRepository;
import com.spark.campusday.meal.order.repository.OrderCutoffConfigurationRepository;
import com.spark.campusday.meal.order.service.FinalYearOrderService;
import com.spark.campusday.meal.order.service.OrderCutoffService;
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

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@EnabledIfEnvironmentVariable(named = "CAMPUS_DAY_MONGO_TEST_URI", matches = ".+")
@SpringBootTest(properties = {
        "spring.mongodb.database=CampusDayPart4Test",
        "spring.data.mongodb.auto-index-creation=true"
})
class OrderPersistenceIntegrationTest {
    @DynamicPropertySource
    static void mongo(DynamicPropertyRegistry registry) {
        registry.add("spring.mongodb.uri", () -> System.getenv("CAMPUS_DAY_MONGO_TEST_URI"));
    }

    @Autowired private StudentService students;
    @Autowired private StudentRepository studentRepository;
    @Autowired private MenuService menus;
    @Autowired private DailyMealMenuRepository menuRepository;
    @Autowired private OrderCutoffService cutoffs;
    @Autowired private OrderCutoffConfigurationRepository configurationRepository;
    @Autowired private FinalYearOrderService orders;
    @Autowired private FinalYearMealOrderRepository orderRepository;
    @Autowired private MongoTemplate mongo;

    @Test
    void persistsUniqueOrderAgainstPublishedMenu() {
        LocalDate date = LocalDate.of(2040, 1, 17);
        var student = students.createStudent("PART4-" + UUID.randomUUID(), "Final Year Test",
                "part4-" + UUID.randomUUID() + "@campusday.test", AcademicYear.FINAL_YEAR);
        String menuId = null;
        String orderId = null;
        try {
            var menu = menus.create(new MenuRequest(date,
                    new MenuRequest.BreakfastRequest("Package", true, true),
                    new MenuRequest.MainMealRequest("Lunch", true, true, true, "Rice and dal", List.of()),
                    new MenuRequest.MainMealRequest("Dinner", true, true, false, null,
                            List.of(new MenuRequest.ItemRequest(null, "Fish", null, null, true)))));
            menuId = menu.id();
            menus.publish(menuId);
            var rule = new OrderCutoffRequest.Rule(1, LocalTime.of(22, 30));
            cutoffs.configure(new OrderCutoffRequest(rule, rule, rule), date);
            var request = new OrderRequest(date, true,
                    new OrderRequest.MealChoiceRequest(MealChoiceKind.FULL_MEAL, List.of()),
                    new OrderRequest.MealChoiceRequest(MealChoiceKind.NOT_EATING, List.of()));
            var created = orders.create(student.getStudentId(), request);
            orderId = created.id();

            assertEquals(1, orders.history(student.getStudentId(), date, date).size());
            assertNotNull(orderRepository.findById(orderId).orElseThrow().getCreatedAt());
            assertThrows(ConflictException.class, () -> orders.create(student.getStudentId(), request));
            assertTrue(mongo.indexOps(FinalYearMealOrder.class).getIndexInfo().stream()
                    .anyMatch(index -> index.isUnique() && index.getName().equals("student_meal_date_unique")));
        } finally {
            if (orderId != null) orderRepository.deleteById(orderId);
            if (menuId != null) menuRepository.deleteById(menuId);
            configurationRepository.deleteById(OrderCutoffConfiguration.ID);
            studentRepository.deleteById(student.getId());
        }
    }
}
