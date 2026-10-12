package com.spark.campusday.meal.inventory;

import com.spark.campusday.common.exception.InvalidBusinessStateException;
import com.spark.campusday.meal.access.MealManagerAccess;
import com.spark.campusday.meal.inventory.dto.KitchenIssueRequest;
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
import org.springframework.dao.DuplicateKeyException;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InventoryConcurrencyTest {
    @Test
    void concurrentIssueCollisionRechecksLatestBalanceBeforeRetry() {
        IngredientRepository ingredients = mock(IngredientRepository.class);
        StockMovementRepository movements = mock(StockMovementRepository.class);
        StudentService students = mock(StudentService.class);
        MealManagerAccess access = mock(MealManagerAccess.class);
        Student manager = mock(Student.class);
        Ingredient ingredient = mock(Ingredient.class);
        when(students.getByStudentId("M-1")).thenReturn(manager);
        when(manager.getId()).thenReturn("manager-db");
        when(access.hasCurrentAccess("manager-db")).thenReturn(true);
        when(ingredients.findById("flour")).thenReturn(Optional.of(ingredient));
        when(ingredient.getId()).thenReturn("flour");
        when(ingredient.getUnit()).thenReturn(IngredientUnit.KG);
        LocalDate date = LocalDate.of(2026, 9, 25);
        StockMovement opening = new StockMovement("flour", 1, StockMovementType.OPENING,
                new BigDecimal("10.000"), new BigDecimal("10.000"), IngredientUnit.KG,
                date, Instant.parse("2026-09-24T00:00:00Z"), null, null, "manager-db");
        StockMovement concurrent = new StockMovement("flour", 2, StockMovementType.KITCHEN_ISSUE,
                new BigDecimal("7.000"), new BigDecimal("3.000"), IngredientUnit.KG,
                date, Instant.parse("2026-09-24T01:00:00Z"), MealType.BREAKFAST, null, "manager-db");
        when(movements.findFirstByIngredientIdOrderBySequenceDesc("flour"))
                .thenReturn(Optional.of(opening), Optional.of(concurrent));
        when(movements.save(any(StockMovement.class))).thenThrow(new DuplicateKeyException("sequence collision"));

        InventoryService service = new InventoryService(ingredients, movements, students, access,
                Clock.fixed(Instant.parse("2026-09-24T02:00:00Z"), ZoneId.of("Asia/Dhaka")));
        assertThrows(InvalidBusinessStateException.class, () -> service.issue("M-1",
                new KitchenIssueRequest(date, MealType.LUNCH, "flour", IngredientUnit.KG,
                        new BigDecimal("7"), null)));
        verify(movements, times(1)).save(any(StockMovement.class));
    }
}
