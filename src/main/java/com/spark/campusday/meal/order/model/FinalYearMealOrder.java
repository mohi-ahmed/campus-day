package com.spark.campusday.meal.order.model;

import com.spark.campusday.common.exception.InvalidBusinessStateException;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Getter
@Document(collection = "final_year_meal_orders")
@CompoundIndex(name = "student_meal_date_unique", def = "{'studentId': 1, 'mealDate': 1}", unique = true)
public class FinalYearMealOrder {
    @Id
    private String id;
    private String studentId;
    @Indexed private LocalDate mealDate;
    private String menuId;
    private boolean breakfast;
    private MealChoice lunch;
    private MealChoice dinner;
    private OrderStatus status;

    @CreatedDate
    private Instant createdAt;
    @LastModifiedDate
    private Instant updatedAt;

    protected FinalYearMealOrder() {
        // Spring Data MongoDB
    }

    public FinalYearMealOrder(String studentId, LocalDate mealDate, String menuId, boolean breakfast,
                              MealChoice lunch, MealChoice dinner) {
        if (studentId == null || studentId.isBlank() || mealDate == null || menuId == null || menuId.isBlank()) {
            throw new InvalidBusinessStateException("Student, meal date and published menu are required");
        }
        this.studentId = studentId;
        this.mealDate = mealDate;
        this.status = OrderStatus.SUBMITTED;
        revise(menuId, breakfast, lunch, dinner);
    }

    public void revise(String menuId, boolean breakfast, MealChoice lunch, MealChoice dinner) {
        if (status != OrderStatus.SUBMITTED) {
            throw new InvalidBusinessStateException("Cancelled orders cannot be edited");
        }
        if (lunch == null || dinner == null || (!breakfast
                && lunch.kind() == MealChoiceKind.NOT_EATING && dinner.kind() == MealChoiceKind.NOT_EATING)) {
            throw new InvalidBusinessStateException("Select at least one meal or leave the date as Not Eating");
        }
        this.menuId = menuId;
        this.breakfast = breakfast;
        this.lunch = lunch;
        this.dinner = dinner;
    }

    public void cancel() {
        if (status != OrderStatus.SUBMITTED) {
            throw new InvalidBusinessStateException("Order is already cancelled");
        }
        status = OrderStatus.CANCELLED;
    }

    public record MealChoice(MealChoiceKind kind, List<SelectedItem> items) {
        public MealChoice {
            if (kind == null || items == null) {
                throw new InvalidBusinessStateException("Meal choice is required");
            }
            items = List.copyOf(items);
        }
    }

    public record SelectedItem(String itemId, String name, int quantity) {}
}
