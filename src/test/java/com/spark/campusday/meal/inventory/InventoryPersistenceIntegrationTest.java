package com.spark.campusday.meal.inventory;

import com.spark.campusday.common.exception.ConflictException;
import com.spark.campusday.common.exception.InvalidBusinessStateException;
import com.spark.campusday.meal.access.MealManagerAccess;
import com.spark.campusday.meal.inventory.dto.IngredientRequest;
import com.spark.campusday.meal.inventory.dto.KitchenIssueRequest;
import com.spark.campusday.meal.inventory.dto.OpeningStockRequest;
import com.spark.campusday.meal.inventory.dto.StockAdjustmentRequest;
import com.spark.campusday.meal.inventory.model.Ingredient;
import com.spark.campusday.meal.inventory.model.IngredientUnit;
import com.spark.campusday.meal.inventory.model.StockMovement;
import com.spark.campusday.meal.inventory.model.StockMovementType;
import com.spark.campusday.meal.inventory.repository.IngredientRepository;
import com.spark.campusday.meal.inventory.repository.StockMovementRepository;
import com.spark.campusday.meal.inventory.service.InventoryService;
import com.spark.campusday.meal.model.MealType;
import com.spark.campusday.student.model.Student;
import com.spark.campusday.student.service.StudentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@EnabledIfEnvironmentVariable(named = "CAMPUS_DAY_MONGO_TEST_URI", matches = ".+")
@SpringBootTest(properties = {
        "spring.mongodb.database=CampusDayPart9Test",
        "spring.data.mongodb.auto-index-creation=true"
})
class InventoryPersistenceIntegrationTest {
    @DynamicPropertySource
    static void mongo(DynamicPropertyRegistry registry) {
        registry.add("spring.mongodb.uri", () -> System.getenv("CAMPUS_DAY_MONGO_TEST_URI"));
    }

    @Autowired IngredientRepository ingredients;
    @Autowired StockMovementRepository movements;
    @Autowired MongoTemplate mongoTemplate;

    @Test
    void openingIssueAndAdjustmentRemainTraceableWithoutNegativeStock() {
        String registrationId = "MANAGER-" + UUID.randomUUID();
        StudentService students = mock(StudentService.class);
        Student manager = mock(Student.class);
        MealManagerAccess access = mock(MealManagerAccess.class);
        when(students.getByStudentId(registrationId)).thenReturn(manager);
        when(manager.getId()).thenReturn("manager-db");
        when(access.hasCurrentAccess("manager-db")).thenReturn(true);
        InventoryService service = new InventoryService(ingredients, movements, students, access,
                Clock.system(ZoneId.of("Asia/Dhaka")));

        String ingredientId = null;
        try {
            var ingredient = service.createIngredient(registrationId,
                    new IngredientRequest("Flour " + UUID.randomUUID(), IngredientUnit.KG));
            ingredientId = ingredient.id();
            LocalDate today = LocalDate.now(ZoneId.of("Asia/Dhaka"));
            var opening = service.open(registrationId,
                    new OpeningStockRequest(ingredientId, IngredientUnit.KG, new BigDecimal("20")));
            var issue = service.issue(registrationId,
                    new KitchenIssueRequest(today, MealType.BREAKFAST, ingredientId,
                            IngredientUnit.KG, new BigDecimal("7.5"), "Kitchen issue"));

            assertEquals(new BigDecimal("12.500"), issue.balanceAfter());
            assertEquals(new BigDecimal("12.500"), service.getIngredient(ingredientId).currentStock());
            assertEquals(new BigDecimal("12.500"), service.listIngredients().stream()
                    .filter(row -> row.id().equals(ingredient.id())).findFirst().orElseThrow().currentStock());
            assertEquals(2, service.history(ingredientId, 0, 20).totalElements());
            assertEquals(1, service.issuesForDate(today, 0, 20).totalElements());
            assertNotNull(movements.findById(opening.id()).orElseThrow().getCreatedAt());

            final String id = ingredientId;
            assertThrows(InvalidBusinessStateException.class, () -> service.issue(registrationId,
                    new KitchenIssueRequest(today, MealType.LUNCH, id, IngredientUnit.KG,
                            new BigDecimal("13"), null)));
            assertThrows(InvalidBusinessStateException.class, () -> service.adjust(registrationId,
                    new StockAdjustmentRequest(id, IngredientUnit.KG, StockMovementType.ADJUSTMENT_OUT,
                            new BigDecimal("13"), "Correction")));
            assertEquals(2, service.history(id, 0, 20).totalElements());
            assertThrows(ConflictException.class, () -> service.open(registrationId,
                    new OpeningStockRequest(id, IngredientUnit.KG, BigDecimal.ONE)));
            assertThrows(ConflictException.class, () -> service.createIngredient(registrationId,
                    new IngredientRequest(ingredient.name().toUpperCase(java.util.Locale.ROOT), IngredientUnit.KG)));
            var adjustment = service.adjust(registrationId, new StockAdjustmentRequest(id, IngredientUnit.KG,
                    StockMovementType.ADJUSTMENT_IN, new BigDecimal("2.25"), "Count correction"));
            assertEquals(new BigDecimal("14.750"), adjustment.balanceAfter());
            assertEquals(3, service.history(id, 0, 20).totalElements());
            assertTrue(mongoTemplate.indexOps(StockMovement.class).getIndexInfo().stream()
                    .anyMatch(index -> index.isUnique() && index.getName().equals("ingredient_sequence_unique")));
            assertTrue(mongoTemplate.indexOps(Ingredient.class).getIndexInfo().stream()
                    .anyMatch(index -> index.isUnique() && index.getName().equals("nameKey")));

            when(access.hasCurrentAccess("manager-db")).thenReturn(false);
            assertThrows(InvalidBusinessStateException.class, () -> service.issue(registrationId,
                    new KitchenIssueRequest(today, MealType.LUNCH, id, IngredientUnit.KG,
                            BigDecimal.ONE, null)));
            assertEquals(3, service.history(id, 0, 20).totalElements());
        } finally {
            if (ingredientId != null) {
                movements.deleteAll(movements.findByIngredientIdOrderBySequenceDesc(
                        ingredientId, PageRequest.of(0, 100)).getContent());
                ingredients.deleteById(ingredientId);
            }
        }
    }
}
