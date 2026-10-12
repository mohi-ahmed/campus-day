package com.spark.campusday.meal.managerapplication.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MealManagerApplicationExpiryScheduler {
    private final MealManagerApplicationService applications;

    @Scheduled(
            initialDelayString = "${app.meal.manager-application.expiry-initial-delay:PT30S}",
            fixedDelayString = "${app.meal.manager-application.expiry-fixed-delay:PT15M}")
    void expirePendingApplications() {
        int expired = applications.expirePendingApplications();
        if (expired > 0) {
            log.info("Expired {} pending Meal Manager application(s)", expired);
        }
    }
}
