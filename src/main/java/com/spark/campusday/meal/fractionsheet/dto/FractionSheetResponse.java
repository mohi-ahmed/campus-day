package com.spark.campusday.meal.fractionsheet.dto;

import com.spark.campusday.meal.order.model.MealChoiceKind;

import java.time.LocalDate;
import java.util.List;

public record FractionSheetResponse(LocalDate mealDate, List<StudentRow> students, Totals totals) {
    public enum RowStatus { SUBMITTED, LOCKED, CANCELLED, NOT_EATING }

    public record StudentRow(String studentId, String name, RowStatus status, boolean breakfast,
                             MealSelection lunch, MealSelection dinner) {}

    public record MealSelection(MealChoiceKind kind, List<ItemQuantity> items) {}

    public record ItemQuantity(String itemId, String name, int quantity) {}

    public record Totals(int finalYearStudents, int submitted, int cancelled, int notEating, int locked,
                         int breakfastPackages, int lunchFullMeals, int dinnerFullMeals,
                         List<ItemQuantity> lunchItems, List<ItemQuantity> dinnerItems) {}
}
