package com.spark.campusday.meal.cookingplan.controller;

import com.spark.campusday.meal.cookingplan.dto.CookingPlanResponse;
import com.spark.campusday.meal.cookingplan.service.CookingPlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/** Development API: explicit manager identity is replaced when Security is added. */
@RestController
@RequestMapping("/api/meal/cooking-plan")
@RequiredArgsConstructor
public class CookingPlanController {
    private final CookingPlanService service;

    @GetMapping
    public CookingPlanResponse get(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                   @RequestParam String managerStudentId) {
        return service.get(date, managerStudentId);
    }
}
