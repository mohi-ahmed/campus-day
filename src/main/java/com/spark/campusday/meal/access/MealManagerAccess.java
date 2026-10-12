package com.spark.campusday.meal.access;

/**
 * Implemented by the management-period feature when assignments exist.
 * Callers pass Student.id (the central MongoDB document ID); academic year alone never grants access.
 */
public interface MealManagerAccess {
    boolean hasCurrentAccess(String studentDocumentId);
}
