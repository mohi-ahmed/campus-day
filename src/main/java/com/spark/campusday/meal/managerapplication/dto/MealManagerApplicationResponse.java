package com.spark.campusday.meal.managerapplication.dto;

import com.spark.campusday.meal.managerapplication.model.MealManagerApplication;
import com.spark.campusday.meal.managerapplication.model.MealManagerApplicationStatus;

import java.time.Instant;
import java.time.LocalDate;

public record MealManagerApplicationResponse(
        String id,
        String studentId,
        String studentName,
        String studentEmail,
        String managementPeriodId,
        LocalDate periodStartDate,
        LocalDate periodEndDate,
        MealManagerApplicationStatus status,
        Instant submittedAt,
        Instant expiresAt,
        Instant decidedAt
) {
    public static MealManagerApplicationResponse from(MealManagerApplication application) {
        return new MealManagerApplicationResponse(
                application.getId(),
                application.getStudentId(),
                application.getStudentName(),
                application.getStudentEmail(),
                application.getManagementPeriodId(),
                application.getPeriodStartDate(),
                application.getPeriodEndDate(),
                application.getStatus(),
                application.getSubmittedAt(),
                application.getExpiresAt(),
                application.getDecidedAt());
    }
}
