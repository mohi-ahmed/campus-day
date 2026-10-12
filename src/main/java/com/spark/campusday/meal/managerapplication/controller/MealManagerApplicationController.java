package com.spark.campusday.meal.managerapplication.controller;

import com.spark.campusday.auth.security.CampusUserPrincipal;
import com.spark.campusday.meal.managerapplication.dto.MealManagerApplicationOverview;
import com.spark.campusday.meal.managerapplication.dto.MealManagerApplicationResponse;
import com.spark.campusday.meal.managerapplication.service.MealManagerApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/meal-manager-applications")
@RequiredArgsConstructor
public class MealManagerApplicationController {
    private final MealManagerApplicationService service;

    @GetMapping("/me")
    MealManagerApplicationOverview current(@AuthenticationPrincipal CampusUserPrincipal principal) {
        return service.currentOverview(principal);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    MealManagerApplicationResponse apply(@AuthenticationPrincipal CampusUserPrincipal principal) {
        return service.apply(principal);
    }
}
