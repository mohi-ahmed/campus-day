package com.spark.campusday.meal.managerapplication.controller;

import com.spark.campusday.meal.managerapplication.dto.MealManagerApplicationResponse;
import com.spark.campusday.meal.managerapplication.dto.MealManagerDecisionRequest;
import com.spark.campusday.meal.managerapplication.service.MealManagerApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/meal-manager-applications")
@RequiredArgsConstructor
public class MealManagerApplicationDecisionController {
    private final MealManagerApplicationService service;

    @GetMapping("/review")
    MealManagerApplicationResponse review(@RequestParam String token) {
        return service.review(token);
    }

    @PostMapping("/decision")
    MealManagerApplicationResponse decide(@Valid @RequestBody MealManagerDecisionRequest request) {
        return service.decide(request.token(), request.decision());
    }
}
