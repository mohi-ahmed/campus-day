package com.spark.campusday.meal.managerapplication.model;

import com.spark.campusday.common.exception.InvalidBusinessStateException;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.time.LocalDate;

@Getter
@Document(collection = "meal_manager_applications")
@CompoundIndexes({
        @CompoundIndex(name = "student_period_unique", def = "{'studentDocumentId': 1, 'managementPeriodId': 1}", unique = true),
        @CompoundIndex(name = "pending_expiry_lookup", def = "{'status': 1, 'expiresAt': 1}")
})
public class MealManagerApplication {
    public static final String ROLE_ACCESS_SCOPE = "ROLE_ACCESS";

    @Id
    private String id;

    private String studentDocumentId;
    private String studentId;
    private String studentName;
    private String studentEmail;
    private String managementPeriodId;
    private LocalDate periodStartDate;
    private LocalDate periodEndDate;
    private MealManagerApplicationStatus status;

    @Indexed(name = "decision_token_unique", unique = true)
    private String tokenHash;

    private Instant expiresAt;
    private Instant decidedAt;
    private String decidedBy;

    @CreatedDate
    private Instant submittedAt;

    @LastModifiedDate
    private Instant updatedAt;

    @Version
    private Long version;

    protected MealManagerApplication() {
        // Spring Data MongoDB
    }

    public MealManagerApplication(
            String studentDocumentId,
            String studentId,
            String studentName,
            String studentEmail,
            String managementPeriodId,
            LocalDate periodStartDate,
            LocalDate periodEndDate,
            String tokenHash,
            Instant expiresAt) {
        if (isBlank(studentDocumentId) || isBlank(studentId) || isBlank(studentName)
                || isBlank(studentEmail) || isBlank(managementPeriodId) || isBlank(tokenHash)
                || expiresAt == null || hasInvalidPeriodDates(
                        managementPeriodId, periodStartDate, periodEndDate)) {
            throw new InvalidBusinessStateException("Meal Manager application data is incomplete");
        }
        this.studentDocumentId = studentDocumentId;
        this.studentId = studentId;
        this.studentName = studentName;
        this.studentEmail = studentEmail;
        this.managementPeriodId = managementPeriodId;
        this.periodStartDate = periodStartDate;
        this.periodEndDate = periodEndDate;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.status = MealManagerApplicationStatus.PENDING;
    }

    public boolean expireIfDue(Instant now) {
        if (status == MealManagerApplicationStatus.PENDING && !now.isBefore(expiresAt)) {
            status = MealManagerApplicationStatus.EXPIRED;
            decidedAt = now;
            decidedBy = "SYSTEM";
            return true;
        }
        return false;
    }

    public void approve(Instant now, String actor) {
        requirePending();
        status = MealManagerApplicationStatus.APPROVED;
        decidedAt = now;
        decidedBy = actor;
    }

    public void reject(Instant now, String actor) {
        requirePending();
        status = MealManagerApplicationStatus.REJECTED;
        decidedAt = now;
        decidedBy = actor;
    }

    private void requirePending() {
        if (status != MealManagerApplicationStatus.PENDING) {
            throw new InvalidBusinessStateException("This Meal Manager application has already been decided");
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static boolean hasInvalidPeriodDates(
            String managementPeriodId, LocalDate startDate, LocalDate endDate) {
        if (ROLE_ACCESS_SCOPE.equals(managementPeriodId)) {
            return startDate != null || endDate != null;
        }
        return startDate == null || endDate == null || startDate.isAfter(endDate);
    }
}
