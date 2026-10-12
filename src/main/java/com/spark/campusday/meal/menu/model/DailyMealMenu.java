package com.spark.campusday.meal.menu.model;

import com.spark.campusday.common.exception.InvalidBusinessStateException;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Getter
@Document(collection = "meal_menus")
public class DailyMealMenu {
    @Id
    private String id;

    @Indexed(unique = true)
    private LocalDate mealDate;

    private Breakfast breakfast;
    private MainMeal lunch;
    private MainMeal dinner;
    private MenuStatus status;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    protected DailyMealMenu() {
        // Spring Data MongoDB
    }

    public DailyMealMenu(LocalDate mealDate, Breakfast breakfast, MainMeal lunch, MainMeal dinner) {
        revise(mealDate, breakfast, lunch, dinner);
        status = MenuStatus.DRAFT;
    }

    public void revise(LocalDate mealDate, Breakfast breakfast, MainMeal lunch, MainMeal dinner) {
        if (mealDate == null || breakfast == null || lunch == null || dinner == null) {
            throw new InvalidBusinessStateException("Menu date and all three meals are required");
        }
        this.mealDate = mealDate;
        this.breakfast = breakfast;
        this.lunch = lunch;
        this.dinner = dinner;
    }

    public void publish() {
        if (status != MenuStatus.DRAFT) {
            throw new InvalidBusinessStateException("Menu is already published");
        }
        status = MenuStatus.PUBLISHED;
    }

    public record Breakfast(String packageDescription, boolean available, boolean orderingEnabled) {}

    public record MainMeal(String description, boolean available, boolean orderingEnabled,
                           boolean fullMealAvailable, String fullMealDescription, List<Item> items) {
        public MainMeal {
            items = List.copyOf(items);
        }
    }

    public record Item(String id, String name, String nameBn, String quantity, boolean available) {}
}
