package com.spark.campusday.meal.managementperiod.service;

import com.spark.campusday.meal.access.MealManagerAccess;
import com.spark.campusday.meal.managementperiod.model.ManagementPeriodStatus;
import com.spark.campusday.meal.managementperiod.repository.MealManagementPeriodRepository;
import com.spark.campusday.meal.managerapplication.service.MealManagerApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class AssignedMealManagerAccess implements MealManagerAccess {
    private final MealManagementPeriodRepository periods;
    private final MealManagerApplicationService applications;
    @Qualifier("mealClock")
    private final Clock mealClock;

    @Override
    public boolean hasCurrentAccess(String studentDocumentId) {
        if (studentDocumentId == null || studentDocumentId.isBlank()) {
            return false;
        }
        LocalDate today = LocalDate.now(mealClock);
        boolean assignedForCurrentPeriod = periods.findFirstByStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                        ManagementPeriodStatus.ACTIVE, today, today)
                .map(period -> period.getManagerStudentIds().contains(studentDocumentId))
                .orElse(false);
        return assignedForCurrentPeriod || applications.hasApprovedAccess(studentDocumentId);
    }
}
