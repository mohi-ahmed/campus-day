package com.spark.campusday.meal.absence.model;

import com.spark.campusday.common.exception.InvalidBusinessStateException;
import com.spark.campusday.meal.model.MealType;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Getter
@Document(collection = "meal_absence_notices")
@CompoundIndex(name = "absence_student_dates", def = "{'studentId': 1, 'startDate': 1, 'endDate': 1}")
public class MealAbsenceNotice {
    @Id
    private String id;
    private String studentId;
    private LocalDate startDate;
    private LocalDate endDate;
    private List<MealType> meals;
    private String note;

    @CreatedDate
    private Instant createdAt;
    @LastModifiedDate
    private Instant updatedAt;

    protected MealAbsenceNotice() {
        // Spring Data MongoDB
    }

    public MealAbsenceNotice(String studentId, LocalDate startDate, LocalDate endDate,
                             List<MealType> meals, String note) {
        if (studentId == null || studentId.isBlank() || startDate == null || endDate == null
                || startDate.isAfter(endDate) || meals == null || meals.isEmpty()
                || meals.stream().anyMatch(meal -> meal == null)
                || meals.stream().distinct().count() != meals.size()
                || (note != null && note.length() > 1000)) {
            throw new InvalidBusinessStateException("A valid date range and distinct selected meals are required");
        }
        this.studentId = studentId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.meals = List.copyOf(meals);
        this.note = note == null || note.isBlank() ? null : note.trim();
    }
}
