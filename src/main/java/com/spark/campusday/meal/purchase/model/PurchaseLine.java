package com.spark.campusday.meal.purchase.model;

import com.spark.campusday.meal.inventory.model.IngredientUnit;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;
import java.math.BigDecimal;

public record PurchaseLine(String ingredientId, String ingredientName, IngredientUnit unit,
                           BigDecimal quantity,
                           @Field(targetType = FieldType.DECIMAL128) BigDecimal unitCost,
                           @Field(targetType = FieldType.DECIMAL128) BigDecimal lineTotal) { }
