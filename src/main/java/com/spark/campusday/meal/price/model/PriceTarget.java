package com.spark.campusday.meal.price.model;

public enum PriceTarget {
    BREAKFAST_PACKAGE,
    LUNCH_FULL_MEAL,
    DINNER_FULL_MEAL,
    LUNCH_MENU_ITEM,
    DINNER_MENU_ITEM;

    public boolean isIndividualItem() {
        return this == LUNCH_MENU_ITEM || this == DINNER_MENU_ITEM;
    }
}
