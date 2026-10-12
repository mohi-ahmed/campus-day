package com.spark.campusday.meal.inventory.model;

import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Locale;

@Getter
@Document(collection = "meal_ingredients")
public class Ingredient {
    @Id
    private String id;
    private String name;
    @Indexed(unique = true)
    private String nameKey;
    private IngredientUnit unit;
    @CreatedDate
    private Instant createdAt;
    @LastModifiedDate
    private Instant updatedAt;

    protected Ingredient() { // Spring Data MongoDB
    }

    public Ingredient(String name, IngredientUnit unit) {
        revise(name, unit);
    }

    public void revise(String name, IngredientUnit unit) {
        this.name = name.trim();
        this.nameKey = name.trim().toLowerCase(Locale.ROOT);
        this.unit = unit;
    }
}
