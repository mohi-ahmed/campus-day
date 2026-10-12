package com.spark.campusday.meal.managementperiod.model;

import com.spark.campusday.common.exception.InvalidBusinessStateException;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Getter
@Document(collection = "meal_management_periods")
@CompoundIndexes({
        @CompoundIndex(name = "period_month_unique", def = "{'year': 1, 'month': 1}", unique = true),
        @CompoundIndex(name = "active_date_lookup", def = "{'status': 1, 'startDate': 1, 'endDate': 1}")
})
public class MealManagementPeriod {
    @Id
    private String id;

    private int year;
    private int month;

    @Indexed
    private LocalDate startDate;

    private LocalDate endDate;
    private List<String> managerStudentIds;
    private ManagementPeriodStatus status;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    protected MealManagementPeriod() {
        // Spring Data MongoDB
    }

    public MealManagementPeriod(int year, int month, LocalDate startDate, LocalDate endDate,
                                List<String> managerStudentIds) {
        reviseDetails(year, month, startDate, endDate, managerStudentIds);
        this.status = ManagementPeriodStatus.ACTIVE;
    }

    public void reviseDetails(int year, int month, LocalDate startDate, LocalDate endDate,
                              List<String> managerStudentIds) {
        if (status == ManagementPeriodStatus.CLOSED) {
            throw new InvalidBusinessStateException("Closed management periods cannot be changed");
        }
        validateDetails(year, month, startDate, endDate, managerStudentIds);
        this.year = year;
        this.month = month;
        this.startDate = startDate;
        this.endDate = endDate;
        this.managerStudentIds = List.copyOf(managerStudentIds);
    }

    public void close() {
        if (status != ManagementPeriodStatus.ACTIVE) {
            throw new InvalidBusinessStateException("Management period is already closed");
        }
        this.status = ManagementPeriodStatus.CLOSED;
    }

    private static void validateDetails(int year, int month, LocalDate startDate, LocalDate endDate,
                                        List<String> managerStudentIds) {
        if (year < 1 || month < 1 || month > 12 || startDate == null || endDate == null
                || startDate.isAfter(endDate)) {
            throw new InvalidBusinessStateException("Invalid management period dates or month");
        }
        YearMonth periodMonth = YearMonth.of(year, month);
        if (!YearMonth.from(startDate).equals(periodMonth) || !YearMonth.from(endDate).equals(periodMonth)) {
            throw new InvalidBusinessStateException("Management period dates must fall within its month");
        }
        if (managerStudentIds == null || managerStudentIds.size() != 2
                || managerStudentIds.stream().anyMatch(id -> id == null || id.isBlank())) {
            throw new InvalidBusinessStateException("Exactly two manager Student IDs are required");
        }
        if (managerStudentIds.get(0).equals(managerStudentIds.get(1))) {
            throw new InvalidBusinessStateException("Manager Student IDs must be distinct");
        }
    }
}
